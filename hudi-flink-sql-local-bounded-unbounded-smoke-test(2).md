# Local Flink SQL Client + Hudi Smoke Test

This document verifies the simplest local Flink-to-Hudi workflow in two stages:

1. a **bounded** `datagen` source that writes a finite dataset to Hudi and finishes;
2. an **unbounded** `datagen` source that keeps generating data and continuously commits it to Hudi.

The test intentionally avoids Kafka, Hive Metastore, S3, CDC, partitioning, and external services.

The complete data flow is:

```text
Flink datagen
    |
    v
Flink SQL INSERT
    |
    v
Hudi COPY_ON_WRITE
    |
    v
local filesystem under /tmp
```

## 1. Prerequisites

You need:

- your local Flink distribution;
- a Hudi Flink bundle built from the Hudi fork you want to test;
- a local standalone Flink cluster.

The Hudi bundle should match the Flink version used by your Flink fork.

For example, the bundle name normally looks like:

```text
hudi-flink1.20-bundle-<version>.jar
```

In a Hudi source checkout it is normally built from:

```text
packaging/hudi-flink-bundle
```

Define the paths mentally for the commands below:

```text
FLINK_HOME=/path/to/flink
HUDI_BUNDLE=/path/to/hudi-flink1.20-bundle-<version>.jar
```

No separate dependency is needed for the Flink SQL `datagen` connector.

## 2. Clean local test data

Before starting, remove data from previous runs:

```bash
rm -rf /tmp/hudi-flink-sql-smoke
mkdir -p /tmp/hudi-flink-sql-smoke
```

The test will create two independent Hudi tables:

```text
/tmp/hudi-flink-sql-smoke/bounded
/tmp/hudi-flink-sql-smoke/unbounded
```

Keeping them separate makes it easier to understand which files and commits belong to which test.

## 3. Start the local Flink cluster

From the Flink distribution:

```bash
cd "$FLINK_HOME"
./bin/start-cluster.sh
```

Optionally verify that the processes are running:

```bash
jps
```

You should see a JobManager/standalone cluster process and at least one TaskManager process.

## 4. Start Flink SQL Client with Hudi

Start SQL Client and put the Hudi bundle on its classpath:

```bash
./bin/sql-client.sh embedded \
  -j "$HUDI_BUNDLE" \
  shell
```

If your internal Flink fork uses a slightly different SQL Client command line, use its equivalent form. The important part is that the Hudi bundle is available to the SQL Client and submitted jobs.

## 5. Configure the SQL session

Run the following in SQL Client:

```sql
SET 'execution.runtime-mode' = 'streaming';
SET 'execution.checkpointing.interval' = '2 s';
SET 'parallelism.default' = '1';
```

We deliberately use streaming runtime mode for both tests.

Checkpointing is important because the Hudi streaming sink publishes data through its commit/checkpoint integration.

Parallelism `1` is not required by Hudi. It only makes this smoke test easier to inspect.

---

# Part A: bounded source

## 6. Create a bounded DataGen source

Create a source that generates exactly 100 rows:

```sql
CREATE TABLE generated_users_bounded (
    id BIGINT,
    name STRING,
    age INT
) WITH (
    'connector' = 'datagen',

    'rows-per-second' = '5',
    'number-of-rows' = '100',

    'fields.id.kind' = 'sequence',
    'fields.id.start' = '1',
    'fields.id.end' = '100',

    'fields.name.length' = '8',

    'fields.age.min' = '18',
    'fields.age.max' = '80'
);
```

This source is bounded for two independent reasons:

- `number-of-rows = 100` limits the total number of generated rows;
- the `id` field uses a finite sequence from `1` through `100`.

The generation rate is intentionally slow enough that several checkpoints can complete before the source finishes.

## 7. Check the source itself

Before involving Hudi, you can verify that DataGen works:

```sql
SELECT *
FROM generated_users_bounded;
```

The query should eventually finish after 100 rows.

Example output:

```text
+----+----+----------+-----+
| op | id | name     | age |
+----+----+----------+-----+
| +I |  1 | ...      |  31 |
| +I |  2 | ...      |  54 |
| +I |  3 | ...      |  22 |
...
```

The exact `name` and `age` values are random.

## 8. Create the bounded Hudi table

Create a non-partitioned Copy-on-Write Hudi table:

```sql
CREATE TABLE hudi_users_bounded (
    id BIGINT PRIMARY KEY NOT ENFORCED,
    name STRING,
    age INT
) WITH (
    'connector' = 'hudi',
    'path' = 'file:///tmp/hudi-flink-sql-smoke/bounded',
    'table.type' = 'COPY_ON_WRITE',
    'write.operation' = 'upsert',
    'hoodie.write.lock.provider' =
        'org.apache.hudi.client.transaction.lock.FileSystemBasedLockProvider'
);
```

For this first test we intentionally use:

- no partitions;
- no Hive catalog;
- no metadata service;
- no object storage;
- no Merge-on-Read;
- no CDC.

The test explicitly uses Hudi's filesystem-based lock provider:

```sql
'hoodie.write.lock.provider' =
    'org.apache.hudi.client.transaction.lock.FileSystemBasedLockProvider'
```

This avoids a dependency on ZooKeeper for the local filesystem smoke test.

## 9. Write the bounded dataset to Hudi

For the bounded test it is convenient to make DML synchronous:

```sql
SET 'table.dml-sync' = 'true';
SET 'pipeline.name' = 'hudi-bounded-smoke-test';
```

Now submit the insert:

```sql
INSERT INTO hudi_users_bounded
SELECT id, name, age
FROM generated_users_bounded;
```

Because DML is synchronous here, the SQL Client should wait until the bounded job has finished.

The job should eventually reach `FINISHED`.

You can also inspect jobs with:

```sql
SHOW JOBS;
```

After the insert finishes, reset the custom job name if desired:

```sql
RESET 'pipeline.name';
```

## 10. Inspect the Hudi files

In another terminal:

```bash
find /tmp/hudi-flink-sql-smoke/bounded -maxdepth 3 -type f | sort
```

You should see:

- Hudi metadata under `.hoodie`;
- one or more data files, normally Parquet files.

A rough shape is:

```text
/tmp/hudi-flink-sql-smoke/bounded/
├── .hoodie/
│   └── ...
└── <file-id>_....parquet
```

The exact internal `.hoodie` layout depends on the Hudi version.

## 11. Read the bounded Hudi table

Back in SQL Client:

```sql
SELECT *
FROM hudi_users_bounded
ORDER BY id;
```

Then run a compact verification:

```sql
SELECT
    COUNT(*) AS row_count,
    MIN(id) AS min_id,
    MAX(id) AS max_id
FROM hudi_users_bounded;
```

Expected result:

```text
row_count = 100
min_id    = 1
max_id    = 100
```

If this works, the basic local integration is proven:

```text
bounded DataGen
    -> Flink SQL
    -> Hudi sink
    -> local Hudi dataset
    -> Hudi snapshot read
```

---

# Part B: unbounded source

The second test keeps the same setup but replaces the finite DataGen table with a continuously generating source.

A critical DataGen detail is that a field using `kind = 'sequence'` makes the table bounded when that sequence reaches its end.

Therefore the unbounded test must not use a finite sequence field.

It also must not set `number-of-rows`.

## 12. Create an unbounded DataGen source

Create the source:

```sql
CREATE TABLE generated_users_unbounded (
    id STRING,
    name STRING,
    age INT
) WITH (
    'connector' = 'datagen',

    'rows-per-second' = '5',

    'fields.id.length' = '20',
    'fields.name.length' = '8',

    'fields.age.min' = '18',
    'fields.age.max' = '80'
);
```

Important differences from the bounded source:

```text
no number-of-rows
no sequence generator
```

By default, DataGen random generation continues without a finite row limit.

The random 20-character `id` is used only to make accidental key collisions extremely unlikely during a short smoke test.

Do not expect deterministic IDs in this test.

## 13. Create a separate Hudi table

Create another local Hudi table:

```sql
CREATE TABLE hudi_users_unbounded (
    id STRING PRIMARY KEY NOT ENFORCED,
    name STRING,
    age INT
) WITH (
    'connector' = 'hudi',
    'path' = 'file:///tmp/hudi-flink-sql-smoke/unbounded',
    'table.type' = 'COPY_ON_WRITE',
    'write.operation' = 'upsert',
    'hoodie.write.lock.provider' =
        'org.apache.hudi.client.transaction.lock.FileSystemBasedLockProvider',
    'read.streaming.enabled' = 'true',
    'read.start-commit' = 'earliest',
    'read.streaming.check-interval' = '2'
);
```

This table is intentionally separate from the bounded table.

## 14. Submit the unbounded insert asynchronously

An unbounded insert must not use synchronous DML, because by definition it is not expected to finish by itself.

Switch DML back to asynchronous mode:

```sql
SET 'table.dml-sync' = 'false';
SET 'pipeline.name' = 'hudi-unbounded-smoke-test';
```

Submit the job:

```sql
INSERT INTO hudi_users_unbounded
SELECT id, name, age
FROM generated_users_unbounded;
```

The SQL Client should return after the job has been submitted and print a Job ID.

Save that Job ID.

For example:

```text
Job ID: 0123456789abcdef0123456789abcdef
```

Reset the session job name if desired:

```sql
RESET 'pipeline.name';
```

## 15. Verify that the job stays running

Run:

```sql
SHOW JOBS;
```

The unbounded job should be in state:

```text
RUNNING
```

Unlike the bounded test, it should not transition to `FINISHED` by itself.

This is the first property we want to verify.

## 16. Let several checkpoints complete

The checkpoint interval is:

```text
2 seconds
```

Let the job run through several checkpoints before checking the Hudi dataset.

You can inspect completed checkpoints in the Flink Web UI.

The important property is not the exact number of checkpoints, only that successful checkpoints keep occurring while the job remains `RUNNING`.

## 17. Inspect Hudi while the writer is still running

In another terminal:

```bash
find /tmp/hudi-flink-sql-smoke/unbounded -maxdepth 3 -type f | sort
```

You should see the Hudi table metadata and data files appear while the Flink job is still running.

You can inspect the timeline-related files more specifically with:

```bash
find /tmp/hudi-flink-sql-smoke/unbounded/.hoodie -type f | sort
```

The exact filenames and directory structure depend on the Hudi version.

## 18. Start a second SQL Client for the streaming reader

The writer job from the previous section should still be `RUNNING`.

Start a **second** SQL Client in another terminal:

```bash
cd "$FLINK_HOME"

./bin/sql-client.sh embedded \
  -j "$HUDI_BUNDLE" \
  shell
```

Configure this second SQL Client:

```sql
SET 'execution.runtime-mode' = 'streaming';
SET 'parallelism.default' = '1';
```

Re-create the same Hudi table definition in this SQL Client:

```sql
CREATE TABLE hudi_users_unbounded (
    id STRING PRIMARY KEY NOT ENFORCED,
    name STRING,
    age INT
) WITH (
    'connector' = 'hudi',
    'path' = 'file:///tmp/hudi-flink-sql-smoke/unbounded',
    'table.type' = 'COPY_ON_WRITE',
    'write.operation' = 'upsert',
    'hoodie.write.lock.provider' =
        'org.apache.hudi.client.transaction.lock.FileSystemBasedLockProvider',
    'read.streaming.enabled' = 'true',
    'read.start-commit' = 'earliest',
    'read.streaming.check-interval' = '2'
);
```

The important read-side options are:

```sql
'read.streaming.enabled' = 'true',
'read.start-commit' = 'earliest',
'read.streaming.check-interval' = '2'
```

They make the Hudi source unbounded.

For this smoke test, `read.start-commit = 'earliest'` is useful because the reader first consumes already committed data and then continues waiting for later commits.

## 19. Run an unbounded `COUNT(*)`

In the second SQL Client, run:

```sql
SELECT COUNT(*) AS row_count
FROM hudi_users_unbounded;
```

This query should **not finish after producing the first count**.

It is a second, independent long-running Flink job:

```text
Hudi streaming source
        |
        v
     COUNT(*)
        |
        v
changing result
```

At this point there should be two independent running jobs:

```text
Job 1

unbounded DataGen
       |
       v
   Hudi writer
       |
       v
 local Hudi table


Job 2

 local Hudi table
       |
       v
Hudi streaming reader
       |
       v
    COUNT(*)
```

As Job 1 completes new checkpoints and publishes new Hudi commits, Job 2 should consume those commits and update the aggregate.

Conceptually, the result evolves like this:

```text
137
142
151
...
```

The exact values and timing are not important.

Depending on the SQL Client result mode, the UI may redraw the current aggregate instead of printing one additional line for every update.

The important properties are:

- the writer job remains `RUNNING`;
- the reader job also remains `RUNNING`;
- the reader does not finish after the first snapshot;
- the count changes after additional Hudi commits become visible.

## 20. Verify both jobs in Flink

Use the Flink Web UI or another SQL Client.

You should see two running jobs:

1. `hudi-unbounded-smoke-test` — DataGen -> Hudi writer;
2. the streaming `COUNT(*)` query — Hudi source -> aggregation.

The topology under test is therefore:

```text
unbounded DataGen
       |
       v
  Flink Job 1
       |
       v
  Hudi COW table
       |
       v
  Flink Job 2
       |
       v
 streaming COUNT(*)
```

This is the main unbounded smoke test: Hudi is both the sink of one streaming job and the source of another streaming job at the same time.

## 21. Inspect Hudi while both jobs are running

In another terminal:

```bash
find /tmp/hudi-flink-sql-smoke/unbounded -maxdepth 3 -type f | sort
```

You should see Hudi metadata and data files while both Flink jobs remain alive.

You can inspect the timeline more specifically with:

```bash
find /tmp/hudi-flink-sql-smoke/unbounded/.hoodie -type f | sort
```

Let the writer complete several more checkpoints.

The streaming `COUNT(*)` should keep following newly published commits.

## 22. Stop the streaming reader

The `SELECT COUNT(*)` is intentionally unbounded, so it must be stopped explicitly.

In the SQL Client running the query, the simplest option is usually:

```text
Ctrl+C
```

You can also identify the reader Job ID in the Flink Web UI and cancel it there.

After stopping the reader, verify that the writer is still `RUNNING`.

That confirms that reading and writing are separate Flink jobs.

## 23. Stop the unbounded writer

In the SQL Client that submitted the writer, find its Job ID:

```sql
SHOW JOBS;
```

Then stop it:

```sql
STOP JOB '<writer-job-id>';
```

For example:

```sql
STOP JOB '0123456789abcdef0123456789abcdef';
```

A savepoint is not needed for this smoke test.

## 24. Verify the final snapshot with a bounded read

After both streaming jobs have stopped, create a separate table definition without streaming-read options:

```sql
CREATE TABLE hudi_users_unbounded_snapshot (
    id STRING PRIMARY KEY NOT ENFORCED,
    name STRING,
    age INT
) WITH (
    'connector' = 'hudi',
    'path' = 'file:///tmp/hudi-flink-sql-smoke/unbounded',
    'table.type' = 'COPY_ON_WRITE',
    'hoodie.write.lock.provider' =
        'org.apache.hudi.client.transaction.lock.FileSystemBasedLockProvider'
);
```

Now this query is bounded and should finish normally:

```sql
SELECT COUNT(*) AS row_count
FROM hudi_users_unbounded_snapshot;
```

You can also inspect some rows:

```sql
SELECT *
FROM hudi_users_unbounded_snapshot
LIMIT 20;
```

The dataset remains on disk after both Flink jobs stop.

---

# Expected result

At the end of the tutorial, the following properties should have been verified.

## Bounded case

The source:

```text
generated_users_bounded
```

generates exactly 100 rows.

The Flink job:

```text
bounded DataGen -> Hudi
```

finishes automatically.

The Hudi snapshot contains:

```text
COUNT(*) = 100
MIN(id)  = 1
MAX(id)  = 100
```

The dataset persists under:

```text
/tmp/hudi-flink-sql-smoke/bounded
```

## Unbounded case

The source:

```text
generated_users_unbounded
```

continues generating rows.

The writer job:

```text
unbounded DataGen -> Hudi
```

remains `RUNNING` and publishes new commits as checkpoints complete.

A second, independent job reads the same Hudi table with streaming reads enabled:

```text
Hudi -> COUNT(*)
```

The reader also remains `RUNNING`; it does not finish after the first count and its aggregate changes as new commits arrive.

The reader and writer can be stopped independently.

The final dataset persists under:

```text
/tmp/hudi-flink-sql-smoke/unbounded
```

---

# What this test proves

If both parts work, you have verified a minimal local setup covering both execution shapes needed for later debugging:

```text
                         +-------------------+
                         |   Flink SQL       |
                         +---------+---------+
                                   |
                  +----------------+----------------+
                  |                                 |
                  v                                 v
          bounded DataGen                  unbounded DataGen
                  |                                 |
                  v                                 v
          finite Flink job                  long-running job
                  |                                 |
                  +---------------+-----------------+
                                  |
                                  v
                           Hudi COW sink
                                  |
                     +------------+------------+
                     |                         |
                     v                         v
             bounded local table       unbounded local table
```

This deliberately tests only the foundation.

It does not yet test:

- Hudi Merge-on-Read;
- additional Hudi streaming read modes beyond the tested streaming `COUNT(*)`;
- updates of an existing record key;
- deletes;
- CDC;
- partitions;
- Kafka;
- savepoint/recovery behavior;
- checkpoint failures;
- JobManager failover;
- exactly-once failure scenarios;
- Hive Metastore;
- object storage.

Those can be added one at a time after this smoke test is stable.

# Troubleshooting

## `Could not find any factory for identifier 'hudi'`

The Hudi Flink bundle is not visible to the SQL Client/job classloader.

Verify the bundle passed to:

```bash
./bin/sql-client.sh ... -j "$HUDI_BUNDLE"
```

Also verify that the bundle was built for the Flink version used by the current Flink fork.

## The bounded INSERT returns immediately

Check:

```sql
SET 'table.dml-sync';
```

For Part A it should be:

```text
true
```

Alternatively, use:

```sql
SHOW JOBS;
```

and wait until the bounded job reaches `FINISHED` before validating the Hudi table.

## The unbounded INSERT never gives the SQL prompt back

Check:

```sql
SET 'table.dml-sync';
```

For Part B it must be:

```text
false
```

## The supposed unbounded job finishes

Inspect the DataGen DDL.

The unbounded source must have:

```text
no number-of-rows
no finite sequence field
```

A DataGen sequence makes the source bounded when the sequence reaches its end.

## The Hudi directory exists but no committed data is visible

First verify that checkpoints are completing successfully.

The test enables them with:

```sql
SET 'execution.checkpointing.interval' = '2 s';
```

Inspect:

- Flink Web UI;
- JobManager logs;
- TaskManager logs;
- failed checkpoints.

For streaming Hudi writes, successful checkpoint/commit progression is one of the first things to check.

## `COUNT(*)` does not increase by exactly five rows per second

That is expected.

`rows-per-second = 5` controls source generation rate, not the exact growth rate visible from Hudi snapshot reads.

Visibility depends on completed Hudi commits/checkpoints.

The unbounded source also uses random primary keys; although collisions should be very unlikely in a short test, this smoke test does not require an exact row count.

# References

- Apache Flink DataGen SQL Connector:
  https://nightlies.apache.org/flink/flink-docs-stable/docs/connectors/table/datagen/

- Apache Flink SQL Client:
  https://nightlies.apache.org/flink/flink-docs-master/docs/sql/interfaces/sql-client/

- Apache Flink 1.20 table configuration (`table.dml-sync`):
  https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/table/config/

- Apache Hudi Flink Quick Start:
  https://hudi.apache.org/docs/flink-quick-start-guide/

- Apache Hudi SQL DML:
  https://hudi.apache.org/docs/sql_dml/
