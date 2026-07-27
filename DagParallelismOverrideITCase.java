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

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.v2.DiscardingSink;
import org.apache.flink.test.junit5.MiniClusterExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage for {@link PipelineOptions#PIPELINE_DAG_WITH_PARALLELISM}: the parallelism a
 * task actually reports at runtime must be the overridden one, not just the one recorded in the
 * graph.
 */
class DagParallelismOverrideITCase {

    private static final int JOB_PARALLELISM = 2;
    private static final int OVERRIDDEN_PARALLELISM = 4;

    @RegisterExtension
    private static final MiniClusterExtension MINI_CLUSTER =
            new MiniClusterExtension(
                    new MiniClusterResourceConfiguration.Builder()
                            .setNumberTaskManagers(1)
                            .setNumberSlotsPerTaskManager(OVERRIDDEN_PARALLELISM * 2)
                            .build());

    /** Parallelism reported by each subtask of the operator under test. */
    private static final Set<Integer> observedParallelism = ConcurrentHashMap.newKeySet();

    /** Number of distinct subtask indices that actually ran. */
    private static final Set<Integer> observedSubtasks = ConcurrentHashMap.newKeySet();

    @BeforeEach
    void resetObservations() {
        observedParallelism.clear();
        observedSubtasks.clear();
    }

    @Test
    void overriddenOperatorRunsAtTheOverriddenParallelism() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(JOB_PARALLELISM);

        final SingleOutputStreamOperator<Integer> map =
                env.fromData(1, 2, 3, 4, 5, 6, 7, 8)
                        .map(new ParallelismRecorder())
                        .name("recorder");
        map.sinkTo(new DiscardingSink<>());

        env.configure(overrides(map.getId(), OVERRIDDEN_PARALLELISM));
        env.execute("dag-with-parallelism override");

        assertThat(observedParallelism)
                .as("parallelism reported by the overridden operator at runtime")
                .containsExactly(OVERRIDDEN_PARALLELISM);
        assertThat(observedSubtasks).hasSize(OVERRIDDEN_PARALLELISM);
    }

    @Test
    void operatorRunsAtJobParallelismWithoutAnOverride() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(JOB_PARALLELISM);

        env.fromData(1, 2, 3, 4, 5, 6, 7, 8)
                .map(new ParallelismRecorder())
                .name("recorder")
                .sinkTo(new DiscardingSink<>());

        env.execute("no override");

        assertThat(observedParallelism).containsExactly(JOB_PARALLELISM);
    }

    private static Configuration overrides(int transformationId, int parallelism) {
        final Map<String, String> map =
                Collections.singletonMap(
                        String.valueOf(transformationId), String.valueOf(parallelism));
        final Configuration configuration = new Configuration();
        configuration.set(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM, map);
        return configuration;
    }

    private static final class ParallelismRecorder extends RichMapFunction<Integer, Integer> {

        private static final long serialVersionUID = 1L;

        @Override
        public void open(OpenContext openContext) {
            observedParallelism.add(
                    getRuntimeContext().getTaskInfo().getNumberOfParallelSubtasks());
            observedSubtasks.add(getRuntimeContext().getTaskInfo().getIndexOfThisSubtask());
        }

        @Override
        public Integer map(Integer value) {
            return value;
        }
    }
}
