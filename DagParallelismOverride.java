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

package org.apache.flink.streaming.runtime.translators;

import org.apache.flink.annotation.Internal;
import org.apache.flink.configuration.IllegalConfigurationException;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.graph.StreamGraph;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Applies {@link PipelineOptions#PIPELINE_DAG_WITH_PARALLELISM} — a {@code transformationId ->
 * parallelism} map that overrides the parallelism of individual operators.
 *
 * <p>The transformation id is the {@code id} of the corresponding node in the execution plan
 * visualization, which is also the {@link StreamGraph} node id.
 *
 * <p>Translators call {@link #setParallelism} instead of {@link StreamGraph#setParallelism(Integer,
 * int, boolean)} so that the override is honoured for every operator kind. An overridden operator
 * is always marked as having an explicitly configured parallelism, otherwise {@code
 * StreamingJobGraphGenerator} would reset it back to the default on a dynamic (adaptive batch)
 * graph and the override would be silently lost.
 */
@Internal
public final class DagParallelismOverride {

    private DagParallelismOverride() {}

    /**
     * Sets the parallelism of the node for {@code transformationId}, preferring a configured
     * override over the parallelism derived from the transformation itself.
     */
    public static void setParallelism(
            final StreamGraph streamGraph,
            final ReadableConfig config,
            final int transformationId,
            final int parallelism,
            final boolean parallelismConfigured) {

        final OptionalInt override = forTransformation(config, transformationId);
        if (override.isPresent()) {
            streamGraph.setParallelism(transformationId, override.getAsInt(), true);
        } else {
            streamGraph.setParallelism(transformationId, parallelism, parallelismConfigured);
        }
    }

    /** Returns the parallelism configured for the given transformation id, if any. */
    static OptionalInt forTransformation(final ReadableConfig config, final int transformationId) {
        final Map<String, String> overrides =
                config.getOptional(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM)
                        .orElse(Collections.emptyMap());
        if (overrides.isEmpty()) {
            return OptionalInt.empty();
        }

        OptionalInt result = OptionalInt.empty();
        // Every entry is validated, not just the one being looked up, so that a typo is reported
        // no matter which operator happens to be translated first.
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            final int id = parseId(entry.getKey());
            final int parallelism = parseParallelism(entry.getKey(), entry.getValue());
            if (id == transformationId) {
                result = OptionalInt.of(parallelism);
            }
        }
        return result;
    }

    private static int parseId(final String key) {
        try {
            return Integer.parseInt(key.trim());
        } catch (NumberFormatException e) {
            throw new IllegalConfigurationException(
                    String.format(
                            "Invalid transformation id '%s' in '%s'. Expected a numeric "
                                    + "transformation id as shown in the execution plan.",
                            key, PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM.key()));
        }
    }

    private static int parseParallelism(final String key, final String value) {
        final int parallelism;
        try {
            parallelism = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalConfigurationException(
                    String.format(
                            "Invalid parallelism '%s' for transformation id '%s' in '%s'. "
                                    + "Expected a positive number.",
                            value, key, PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM.key()));
        }
        if (parallelism <= 0) {
            throw new IllegalConfigurationException(
                    String.format(
                            "Parallelism %d for transformation id '%s' in '%s' must be positive.",
                            parallelism, key, PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM.key()));
        }
        return parallelism;
    }
}
