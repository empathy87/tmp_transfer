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

package org.apache.flink.client.cli;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@code -pd,--dagParallelism} CLI option, which is the command line front end for
 * {@link PipelineOptions#PIPELINE_DAG_WITH_PARALLELISM}.
 */
class DagParallelismOptionTest extends CliFrontendTestBase {

    @Test
    void parsesShortFormIntoTheConfiguration() throws Exception {
        final Map<String, String> expected = new HashMap<>();
        expected.put("1", "4");
        expected.put("2", "8");

        assertThat(configurationFor("-pd", "1:4,2:8"))
                .containsEntry(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM.key(), "1:4,2:8");
        assertThat(parsedOverridesFor("-pd", "1:4,2:8")).isEqualTo(expected);
    }

    @Test
    void parsesLongForm() throws Exception {
        assertThat(parsedOverridesFor("--dagParallelism", "7:3"))
                .containsExactlyEntriesOf(java.util.Collections.singletonMap("7", "3"));
    }

    @Test
    void isAbsentFromTheConfigurationWhenTheFlagIsNotGiven() throws Exception {
        final Configuration configuration = new Configuration();
        programOptions().applyToConfiguration(configuration);

        assertThat(configuration.getOptional(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM))
                .isEmpty();
    }

    /** The flag must not disturb {@code -p}, which sets a different option. */
    @Test
    void coexistsWithTheDefaultParallelismFlag() throws Exception {
        final Configuration configuration =
                configurationOf(programOptions("-p", "2", "-pd", "5:9"));

        assertThat(configuration.get(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM))
                .containsExactlyEntriesOf(java.util.Collections.singletonMap("5", "9"));
        assertThat(
                        configuration.get(
                                org.apache.flink.configuration.CoreOptions.DEFAULT_PARALLELISM))
                .isEqualTo(2);
    }

    @Test
    void isAcceptedByTheInfoCommand() throws Exception {
        final CommandLine commandLine =
                CliFrontendParser.parse(
                        CliFrontendParser.getInfoCommandOptions(),
                        new String[] {"-pd", "1:4", "dummy.jar"},
                        true);

        assertThat(commandLine.hasOption("pd")).isTrue();
    }

    // ------------------------------------------------------------------------

    private static ProgramOptions programOptions(String... args) throws Exception {
        final String[] arguments = new String[args.length + 1];
        System.arraycopy(args, 0, arguments, 0, args.length);
        arguments[args.length] = "dummy.jar";

        final CommandLine commandLine =
                CliFrontendParser.parse(CliFrontendParser.RUN_OPTIONS, arguments, true);
        return ProgramOptions.create(commandLine);
    }

    private static Configuration configurationOf(ProgramOptions programOptions) {
        final Configuration configuration = new Configuration();
        programOptions.applyToConfiguration(configuration);
        return configuration;
    }

    private static Map<String, String> configurationFor(String... args) throws Exception {
        return configurationOf(programOptions(args)).toMap();
    }

    private static Map<String, String> parsedOverridesFor(String... args) throws Exception {
        return configurationOf(programOptions(args))
                .get(PipelineOptions.PIPELINE_DAG_WITH_PARALLELISM);
    }
}
