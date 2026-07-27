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

package org.apache.flink.test.streaming.runtime;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.v2.DiscardingSink;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.graph.StreamNode;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.apache.flink.types.Row;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PipelineOptions#PIPELINE_DAG_WITH_PARALLELISM} applied to a SQL pipeline.
 *
 * <p>SQL reaches exactly the same translators as the DataStream API: the planner produces {@code
 * Transformation}s and {@code DefaultExecutor#createPipeline} calls {@code
 * executionEnvironment.configure(tableConfiguration)} before handing them to the very same {@code
 * StreamGraphGenerator}. So the option works for planner-generated operators, and a SQL {@code SET}
 * of it is picked up because the table configuration is merged into the environment.
 *
 * <p>The catch is finding the ids. Transformation ids are handed out by the JVM-global counter in
 * {@code Transformation#getNewNodeId()}, and the planner allocates a <em>fresh</em> set on every
 * translation — unlike a DataStream program, whose transformations are built once and then reused.
 * Within a single session an {@code EXPLAIN} therefore reports ids that the following {@code
 * INSERT} will not reuse. These tests consequently read the ids back from the graph they just
 * built, which is also why they use {@link StreamTableEnvironment}: it gives access to the {@link
 * StreamExecutionEnvironment} the planner writes its transformations into.
 */
class SqlDagParallelismOverrideITCase {

    private static final int JOB_PARALLELISM = 2;
    private static final int OVERRIDDEN_PARALLELISM = 4;

    @RegisterExtension
    private static final MiniClusterExtension MINI_CLUSTER =
            new MiniClusterExtension(
                    new MiniClusterResourceConfiguration.Builder()
                            .setNumberTaskManagers(1)
                            .setNumberSlotsPerTaskManager(OVERRIDDEN_PARALLELISM * 2)
                            .build());

    /**
     * Overrides the parallelism of the operator the SQL planner generated for the {@code WHERE} /
     * projection, and runs the job to prove the resulting graph is deployable.
     */
    @Test
    void overrideAppliesToAPlannerGeneratedOperator() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(JOB_PARALLELISM);
        final StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        tEnv.createTemporaryView(
                "src", tEnv.fromDataStream(env.fromSequence(1L, 100L).name("src")));
        final Table result = tEnv.sqlQuery("SELECT f0 FROM src WHERE f0 > 10");
        final DataStream<Row> out = tEnv.toDataStream(result);
        out.sinkTo(new DiscardingSink<>());

        // The ids are only knowable once the planner has translated the query.
        final StreamGraph baseline = env.getStreamGraph(false);
        final StreamNode calc = singleNodeContaining(baseline, "Calc");
        assertThat(calc.getParallelism()).isEqualTo(JOB_PARALLELISM);

        env.configure(overrides(calc.getId(), OVERRIDDEN_PARALLELISM));

        final StreamGraph overridden = env.getStreamGraph(false);
        assertThat(overridden.getStreamNode(calc.getId()).getParallelism())
                .isEqualTo(OVERRIDDEN_PARALLELISM);

        env.execute("sql dag-with-parallelism override");
    }

    /**
     * The same override, but set on the {@link org.apache.flink.table.api.TableConfig} rather than
     * on the environment — this is what SQL {@code SET 'pipeline.dag.with.parallelism' = '...'}
     * does. The table configuration is pushed into the environment by the bridge on every
     * conversion, so a second conversion is what makes the setting visible here.
     */
    @Test
    void overrideSetOnTheTableConfigIsHonoured() {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(JOB_PARALLELISM);
        final StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        tEnv.createTemporaryView(
                "src", tEnv.fromDataStream(env.fromSequence(1L, 100L).name("src")));
        tEnv.toDataStream(tEnv.sqlQuery("SELECT f0 FROM src WHERE f0 > 10"))
                .sinkTo(new DiscardingSink<>());

        final StreamNode calc = singleNodeContaining(env.getStreamGraph(false), "Calc");

        tEnv.getConfig()
                .set(
                        PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM,
                        Collections.singletonMap(
                                String.valueOf(calc.getId()),
                                String.valueOf(OVERRIDDEN_PARALLELISM)));

        // Any further conversion merges the table configuration into the environment.
        tEnv.toDataStream(tEnv.sqlQuery("SELECT f0 FROM src WHERE f0 > 20"))
                .sinkTo(new DiscardingSink<>());

        assertThat(env.getStreamGraph(false).getStreamNode(calc.getId()).getParallelism())
                .isEqualTo(OVERRIDDEN_PARALLELISM);
    }

    // ------------------------------------------------------------------------

    private static Configuration overrides(int transformationId, int parallelism) {
        final Map<String, String> map =
                Collections.singletonMap(
                        String.valueOf(transformationId), String.valueOf(parallelism));
        final Configuration configuration = new Configuration();
        configuration.set(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM, map);
        return configuration;
    }

    private static StreamNode singleNodeContaining(StreamGraph streamGraph, String namePart) {
        final List<StreamNode> matches = new ArrayList<>();
        final List<String> allNames = new ArrayList<>();
        for (StreamNode node : streamGraph.getStreamNodes()) {
            allNames.add(node.getOperatorName());
            if (node.getOperatorName().contains(namePart)) {
                matches.add(node);
            }
        }
        assertThat(matches)
                .as(
                        "nodes whose operator name contains '%s'; all names were %s",
                        namePart, allNames)
                .hasSize(1);
        return matches.get(0);
    }
}
