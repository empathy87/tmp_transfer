/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.api.graph;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.util.jackson.JacksonMapperFactory;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PipelineOptions#PIPELINE_DAG_WITH_PARALLELISM} ({@code
 * pipeline.dag.with.parallelism}), which overrides the parallelism of individual operators by
 * <i>transformation id</i>.
 *
 * <p>The transformation id is the id a user reads off the execution plan visualization: {@link
 * StreamExecutionEnvironment#getExecutionPlan()} emits one JSON node per operator carrying {@code
 * "id"} and {@code "parallelism"}, and that {@code "id"} is {@link
 * org.apache.flink.api.dag.Transformation#getId()}, which is also the {@link StreamNode} id.
 *
 * <p>The override is applied in the transformation translators, at the point where each computes
 *
 * <pre>{@code
 * int parallelism = transformation.getParallelism() != ExecutionConfig.PARALLELISM_DEFAULT
 *         ? transformation.getParallelism()
 *         : executionConfig.getParallelism();
 * streamGraph.setParallelism(transformationId, parallelism, transformation.isParallelismConfigured());
 * }</pre>
 *
 * <p>Two consequences drive the tests below, and both are easy to get wrong:
 *
 * <ol>
 *   <li>That snippet is duplicated across <b>seven</b> translators — {@code
 *       AbstractOneInputTransformationTranslator}, {@code
 *       AbstractTwoInputTransformationTranslator}, {@code MultiInputTransformationTranslator},
 *       {@code SourceTransformationTranslator}, {@code LegacySourceTransformationTranslator},
 *       {@code SinkTransformationTranslator} and {@code LegacySinkTransformationTranslator}. An
 *       operator kind whose translator was missed silently ignores the option, so {@link
 *       OperatorKinds} covers each family separately rather than trusting one representative
 *       operator.
 *   <li>The third argument to {@code setParallelism} decides whether the parallelism counts as
 *       explicitly configured. Passing {@code transformation.isParallelismConfigured()} through
 *       unchanged leaves it {@code false} for an operator the job never called {@code
 *       setParallelism()} on — and {@code StreamingJobGraphGenerator} then <i>resets that vertex to
 *       PARALLELISM_DEFAULT</i> on a dynamic graph, throwing the override away. See {@link
 *       AdaptiveParallelism}.
 * </ol>
 *
 * <p>Assertions are made on the generated graph rather than on any particular call site, so they
 * hold however the lookup is threaded into the translators.
 *
 * <p>Transformation ids come from a JVM-global counter and are <b>not</b> stable across runs. Every
 * test therefore reads the real id back from the topology it just built ({@code
 * DataStream#getId()}) instead of hard-coding one — which is exactly what a user does after looking
 * at the plan.
 */
class StreamGraphGeneratorDagParallelismTest {

    private static final int JOB_PARALLELISM = 2;
    private static final int OVERRIDDEN_PARALLELISM = 7;
    private static final int OTHER_OVERRIDDEN_PARALLELISM = 3;

    private static final String MAP_NAME = "my-map";
    private static final String FILTER_NAME = "my-filter";

    // ------------------------------------------------------------------------
    //  Core contract
    // ------------------------------------------------------------------------

    @Test
    void overridesParallelismOfASingleOperator() {
        final TestJob job = buildJob();

        job.env.configure(overrides(job.map.getId(), OVERRIDDEN_PARALLELISM));
        final StreamGraph streamGraph = job.env.getStreamGraph();

        assertThat(parallelismOf(streamGraph, job.map.getId())).isEqualTo(OVERRIDDEN_PARALLELISM);
    }

    /**
     * Generates the graph twice from the same topology — once without the option and once with it —
     * and requires the two to differ in exactly the overridden entry. This pins down the whole
     * contract at once (the override applies, and nothing else moves) without hard-coding any
     * operator's natural parallelism: the {@code fromData} source, for instance, is non-parallel
     * and sits at 1 regardless of the job parallelism.
     */
    @Test
    void changesExactlyTheListedOperatorAndNothingElse() {
        final TestJob job = buildJob();

        final Map<Integer, Integer> before = parallelismById(job.env.getStreamGraph(false));

        job.env.configure(overrides(job.map.getId(), OVERRIDDEN_PARALLELISM));
        final Map<Integer, Integer> after = parallelismById(job.env.getStreamGraph(false));

        final Map<Integer, Integer> expected = new HashMap<>(before);
        expected.put(job.map.getId(), OVERRIDDEN_PARALLELISM);

        assertThat(after).isEqualTo(expected);
    }

    @Test
    void overridesParallelismOfSeveralOperatorsAtOnce() {
        final TestJob job = buildJob();

        final Map<String, String> both = new HashMap<>();
        both.put(String.valueOf(job.map.getId()), String.valueOf(OVERRIDDEN_PARALLELISM));
        both.put(String.valueOf(job.filter.getId()), String.valueOf(OTHER_OVERRIDDEN_PARALLELISM));
        job.env.configure(configurationOf(both));

        final StreamGraph streamGraph = job.env.getStreamGraph();

        assertThat(parallelismOf(streamGraph, job.map.getId())).isEqualTo(OVERRIDDEN_PARALLELISM);
        assertThat(parallelismOf(streamGraph, job.filter.getId()))
                .isEqualTo(OTHER_OVERRIDDEN_PARALLELISM);
    }

    /**
     * The option is an <i>override</i>: it must win over a parallelism the job itself set on that
     * operator, otherwise it could not be used to tune an already-written job. At the translator
     * hook this is the difference between replacing the whole {@code ? :} expression and only
     * substituting its {@code executionConfig.getParallelism()} branch.
     */
    @Test
    void overrideWinsOverProgrammaticallySetParallelism() {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(JOB_PARALLELISM);
        final DataStreamSource<Integer> source = env.fromData(1, 2, 3);
        final SingleOutputStreamOperator<Integer> map =
                source.map(value -> value).name(MAP_NAME).setParallelism(5);

        env.configure(overrides(map.getId(), OVERRIDDEN_PARALLELISM));
        final StreamGraph streamGraph = env.getStreamGraph();

        assertThat(parallelismOf(streamGraph, map.getId())).isEqualTo(OVERRIDDEN_PARALLELISM);
    }

    /**
     * A user writes this option into {@code config.yaml} or via SQL {@code SET}, i.e. as the flat
     * string form {@code id1:p1,id2:p2} shown in the option description — not as a pre-built map.
     */
    @Test
    void overrideIsParsedFromTheConfigFileStringForm() {
        final TestJob job = buildJob();

        final String flatValue =
                job.map.getId()
                        + ":"
                        + OVERRIDDEN_PARALLELISM
                        + ","
                        + job.filter.getId()
                        + ":"
                        + OTHER_OVERRIDDEN_PARALLELISM;
        job.env.configure(
                Configuration.fromMap(
                        Collections.singletonMap(
                                PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM.key(), flatValue)));

        final StreamGraph streamGraph = job.env.getStreamGraph();

        assertThat(parallelismOf(streamGraph, job.map.getId())).isEqualTo(OVERRIDDEN_PARALLELISM);
        assertThat(parallelismOf(streamGraph, job.filter.getId()))
                .isEqualTo(OTHER_OVERRIDDEN_PARALLELISM);
    }

    @Test
    void unsetOptionLeavesTheTopologyUnchanged() {
        final TestJob job = buildJob();

        final StreamGraph streamGraph = job.env.getStreamGraph();

        assertThat(parallelismOf(streamGraph, job.map.getId())).isEqualTo(JOB_PARALLELISM);
        assertThat(parallelismOf(streamGraph, job.filter.getId())).isEqualTo(JOB_PARALLELISM);
    }

    @Test
    void emptyOptionLeavesTheTopologyUnchanged() {
        final TestJob job = buildJob();

        job.env.configure(configurationOf(Collections.emptyMap()));
        final StreamGraph streamGraph = job.env.getStreamGraph();

        assertThat(parallelismOf(streamGraph, job.map.getId())).isEqualTo(JOB_PARALLELISM);
        assertThat(parallelismOf(streamGraph, job.filter.getId())).isEqualTo(JOB_PARALLELISM);
    }

    // ------------------------------------------------------------------------
    //  Per-translator coverage
    // ------------------------------------------------------------------------

    /**
     * One test per translator family. These fail individually, so a red test names the translator
     * that still computes its parallelism without consulting the option.
     */
    @Nested
    class OperatorKinds {

        /** {@code AbstractOneInputTransformationTranslator} — map, filter, process, keyed ops. */
        @Test
        void oneInputOperator() {
            final StreamExecutionEnvironment env = newEnv();
            final SingleOutputStreamOperator<Integer> map =
                    env.fromData(1, 2, 3).map(value -> value).name(MAP_NAME);
            map.print();

            env.configure(overrides(map.getId(), OVERRIDDEN_PARALLELISM));

            assertThat(parallelismOf(env.getStreamGraph(), map.getId()))
                    .isEqualTo(OVERRIDDEN_PARALLELISM);
        }

        /** {@code AbstractTwoInputTransformationTranslator} — connect + co-process. */
        @Test
        void twoInputOperator() {
            final StreamExecutionEnvironment env = newEnv();
            final DataStreamSource<Integer> left = env.fromData(1, 2, 3);
            final DataStreamSource<Integer> right = env.fromData(4, 5, 6);
            final SingleOutputStreamOperator<Integer> coMap =
                    left.connect(right)
                            .map(
                                    new CoMapFunction<Integer, Integer, Integer>() {
                                        @Override
                                        public Integer map1(Integer value) {
                                            return value;
                                        }

                                        @Override
                                        public Integer map2(Integer value) {
                                            return value;
                                        }
                                    })
                            .name("my-co-map");
            coMap.print();

            env.configure(overrides(coMap.getId(), OVERRIDDEN_PARALLELISM));

            assertThat(parallelismOf(env.getStreamGraph(), coMap.getId()))
                    .isEqualTo(OVERRIDDEN_PARALLELISM);
        }

        /** {@code SourceTransformationTranslator} — {@code fromSequence} is a parallel source. */
        @Test
        void source() {
            final StreamExecutionEnvironment env = newEnv();
            final SingleOutputStreamOperator<Long> source =
                    env.fromSequence(1L, 100L).name("my-source");
            source.print();

            env.configure(overrides(source.getId(), OVERRIDDEN_PARALLELISM));

            assertThat(parallelismOf(env.getStreamGraph(), source.getId()))
                    .isEqualTo(OVERRIDDEN_PARALLELISM);
        }

        /** {@code LegacySinkTransformationTranslator} — {@code print()} is a legacy sink. */
        @Test
        void sink() {
            final StreamExecutionEnvironment env = newEnv();
            final DataStreamSink<Integer> sink = env.fromData(1, 2, 3).print().name("my-sink");
            final int sinkId = sink.getTransformation().getId();

            env.configure(overrides(sinkId, OVERRIDDEN_PARALLELISM));

            assertThat(parallelismOf(env.getStreamGraph(), sinkId))
                    .isEqualTo(OVERRIDDEN_PARALLELISM);
        }
    }

    // ------------------------------------------------------------------------
    //  The override must count as an explicitly configured parallelism
    // ------------------------------------------------------------------------

    /**
     * An overridden operator has to be marked {@code parallelismConfigured}, otherwise {@code
     * StreamingJobGraphGenerator#setVertexParallelismsForDynamicGraphIfNecessary} resets it to
     * {@code PARALLELISM_DEFAULT} on a dynamic graph — batch mode with the (default) AdaptiveBatch
     * scheduler and (default-on) {@code execution.batch.adaptive.auto-parallelism.enabled}. The
     * override would then be silently discarded on exactly the workloads where hand-tuning an
     * operator's parallelism is most useful.
     */
    @Nested
    class AdaptiveParallelism {

        @Test
        void overriddenOperatorCountsAsExplicitlyConfigured() {
            final TestJob job = buildJob();

            job.env.configure(overrides(job.map.getId(), OVERRIDDEN_PARALLELISM));
            final StreamGraph streamGraph = job.env.getStreamGraph();

            assertThat(streamGraph.getStreamNode(job.map.getId()).isParallelismConfigured())
                    .as("an overridden operator must not be treated as auto-parallelism material")
                    .isTrue();
        }

        @Test
        void overrideSurvivesAdaptiveBatchParallelismReset() {
            final StreamExecutionEnvironment env = newEnv();
            env.setRuntimeMode(RuntimeExecutionMode.BATCH);
            env.disableOperatorChaining();
            final SingleOutputStreamOperator<Integer> map =
                    env.fromData(1, 2, 3).map(value -> value).name(MAP_NAME);
            map.print();

            env.configure(overrides(map.getId(), OVERRIDDEN_PARALLELISM));
            final StreamGraph streamGraph = env.getStreamGraph();

            // Guard against the test passing vacuously: the reset path only runs on a dynamic
            // graph with auto-parallelism enabled.
            assertThat(streamGraph.isDynamic()).as("graph must be dynamic").isTrue();
            assertThat(streamGraph.isAutoParallelismEnabled())
                    .as("auto parallelism must be enabled")
                    .isTrue();

            final JobGraph jobGraph = StreamingJobGraphGenerator.createJobGraph(streamGraph);

            assertThat(vertexNamed(jobGraph, MAP_NAME).getParallelism())
                    .as("override was reset to PARALLELISM_DEFAULT by the adaptive batch path")
                    .isNotEqualTo(ExecutionConfig.PARALLELISM_DEFAULT)
                    .isEqualTo(OVERRIDDEN_PARALLELISM);
        }
    }

    // ------------------------------------------------------------------------
    //  The override has to survive the rest of the translation
    // ------------------------------------------------------------------------

    /**
     * Overriding the parallelism only helps if it reaches the {@link JobVertex} that is actually
     * deployed. Chaining is switched off so that every operator becomes its own vertex and the
     * mapping stays one-to-one.
     */
    @Test
    void overrideReachesTheJobGraph() {
        final TestJob job = buildJob();
        job.env.disableOperatorChaining();

        job.env.configure(overrides(job.map.getId(), OVERRIDDEN_PARALLELISM));
        final JobGraph jobGraph =
                StreamingJobGraphGenerator.createJobGraph(job.env.getStreamGraph());

        assertThat(vertexNamed(jobGraph, MAP_NAME).getParallelism())
                .isEqualTo(OVERRIDDEN_PARALLELISM);
        assertThat(vertexNamed(jobGraph, FILTER_NAME).getParallelism()).isEqualTo(JOB_PARALLELISM);
    }

    /**
     * The plan visualization is where the option's description tells users to read the ids, so the
     * overridden parallelism has to be visible there too — otherwise the reported plan disagrees
     * with what runs.
     */
    @Test
    void overrideIsVisibleInTheExecutionPlan() throws Exception {
        final TestJob job = buildJob();

        job.env.configure(overrides(job.map.getId(), OVERRIDDEN_PARALLELISM));
        final JsonNode plan =
                JacksonMapperFactory.createObjectMapper().readTree(job.env.getExecutionPlan());

        assertThat(planNode(plan, job.map.getId()).get("parallelism").asInt())
                .isEqualTo(OVERRIDDEN_PARALLELISM);
        assertThat(planNode(plan, job.filter.getId()).get("parallelism").asInt())
                .isEqualTo(JOB_PARALLELISM);
    }

    // ------------------------------------------------------------------------
    //  Validation
    // ------------------------------------------------------------------------

    /**
     * These encode the assumption that malformed input is <b>rejected loudly</b> rather than
     * silently ignored — a silently dropped entry means a job runs at a parallelism the operator
     * never asked for, which is the worst outcome for an option that exists to tune parallelism.
     *
     * <p>Note that a translator-level lookup has no natural place to notice that an id matched
     * nothing: each translator only ever asks about the transformation it is currently translating,
     * so an id nobody claims is invisible without a separate validation pass over the option
     * against the set of transformation ids. {@link #rejectsAnUnknownTransformationId} and {@link
     * #rejectsANonNumericTransformationId} therefore assert that such a pass exists; if the fork
     * deliberately chose a lenient policy, these are the tests to adjust — but see {@link
     * #aRejectedEntryNeverSilentlyRetargetsAnotherOperator}, which holds either way.
     *
     * <p>The exception type is left broad on purpose so that any of {@code
     * IllegalConfigurationException} / {@code IllegalArgumentException} / {@code
     * NumberFormatException} / {@code FlinkRuntimeException} satisfies these.
     */
    @Nested
    class Validation {

        @Test
        void rejectsAnUnknownTransformationId() {
            final TestJob job = buildJob();

            job.env.configure(overrides(Integer.MAX_VALUE, OVERRIDDEN_PARALLELISM));

            assertThatThrownBy(job.env::getStreamGraph).isInstanceOf(RuntimeException.class);
        }

        @Test
        void rejectsAZeroParallelism() {
            final TestJob job = buildJob();

            job.env.configure(overrides(job.map.getId(), 0));

            assertThatThrownBy(job.env::getStreamGraph).isInstanceOf(RuntimeException.class);
        }

        @Test
        void rejectsANegativeParallelism() {
            final TestJob job = buildJob();

            job.env.configure(overrides(job.map.getId(), -1));

            assertThatThrownBy(job.env::getStreamGraph).isInstanceOf(RuntimeException.class);
        }

        @Test
        void rejectsANonNumericParallelism() {
            final TestJob job = buildJob();

            job.env.configure(
                    configurationOf(
                            Collections.singletonMap(
                                    String.valueOf(job.map.getId()), "not-a-number")));

            assertThatThrownBy(job.env::getStreamGraph).isInstanceOf(RuntimeException.class);
        }

        @Test
        void rejectsANonNumericTransformationId() {
            final TestJob job = buildJob();

            job.env.configure(
                    configurationOf(
                            Collections.singletonMap(
                                    "not-an-id", String.valueOf(OVERRIDDEN_PARALLELISM))));

            assertThatThrownBy(job.env::getStreamGraph).isInstanceOf(RuntimeException.class);
        }

        /**
         * Holds under both a strict and a lenient policy: whatever happens to a junk entry, it must
         * not end up applied to some other operator. Only the safety property is asserted, so this
         * one stays green even if the tests above are relaxed.
         */
        @Test
        void aRejectedEntryNeverSilentlyRetargetsAnotherOperator() {
            final TestJob job = buildJob();
            final Map<Integer, Integer> before = parallelismById(job.env.getStreamGraph(false));

            job.env.configure(overrides(Integer.MAX_VALUE, OVERRIDDEN_PARALLELISM));

            Map<Integer, Integer> after = null;
            try {
                after = parallelismById(job.env.getStreamGraph(false));
            } catch (RuntimeException expectedUnderAStrictPolicy) {
                return;
            }
            assertThat(after).isEqualTo(before);
        }
    }

    // ------------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------------

    /** Source -> map -> filter -> sink, with the two middle operators addressable by id. */
    private static final class TestJob {
        private final StreamExecutionEnvironment env;
        private final SingleOutputStreamOperator<Integer> map;
        private final SingleOutputStreamOperator<Integer> filter;

        private TestJob(
                StreamExecutionEnvironment env,
                SingleOutputStreamOperator<Integer> map,
                SingleOutputStreamOperator<Integer> filter) {
            this.env = env;
            this.map = map;
            this.filter = filter;
        }
    }

    private static StreamExecutionEnvironment newEnv() {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(JOB_PARALLELISM);
        return env;
    }

    private static TestJob buildJob() {
        final StreamExecutionEnvironment env = newEnv();

        final DataStreamSource<Integer> source = env.fromData(1, 2, 3);
        final SingleOutputStreamOperator<Integer> map = source.map(value -> value).name(MAP_NAME);
        final SingleOutputStreamOperator<Integer> filter =
                map.filter(value -> true).name(FILTER_NAME);
        filter.print();

        return new TestJob(env, map, filter);
    }

    private static Configuration overrides(int transformationId, int parallelism) {
        return configurationOf(
                Collections.singletonMap(
                        String.valueOf(transformationId), String.valueOf(parallelism)));
    }

    private static Configuration configurationOf(Map<String, String> overrides) {
        final Configuration configuration = new Configuration();
        configuration.set(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM, overrides);
        return configuration;
    }

    private static Map<Integer, Integer> parallelismById(StreamGraph streamGraph) {
        final Map<Integer, Integer> parallelisms = new HashMap<>();
        for (StreamNode node : streamGraph.getStreamNodes()) {
            parallelisms.put(node.getId(), node.getParallelism());
        }
        return parallelisms;
    }

    private static int parallelismOf(StreamGraph streamGraph, int transformationId) {
        final StreamNode node = streamGraph.getStreamNode(transformationId);
        assertThat(node)
                .as(
                        "No StreamNode for transformation id %d; the topology changed shape",
                        transformationId)
                .isNotNull();
        return node.getParallelism();
    }

    private static JobVertex vertexNamed(JobGraph jobGraph, String name) {
        final List<JobVertex> matches = new ArrayList<>();
        for (JobVertex vertex : jobGraph.getVertices()) {
            if (vertex.getName().contains(name)) {
                matches.add(vertex);
            }
        }
        assertThat(matches).as("JobVertices matching '%s'", name).hasSize(1);
        return matches.get(0);
    }

    private static JsonNode planNode(JsonNode plan, int transformationId) {
        for (JsonNode node : plan.get("nodes")) {
            if (node.get("id").asInt() == transformationId) {
                return node;
            }
        }
        throw new AssertionError(
                "No plan node with id " + transformationId + " in plan:\n" + plan.toPrettyString());
    }
}
