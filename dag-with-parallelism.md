# Manual test: `pipeline.dag.with.parallelism`

Per-operator parallelism override, keyed by **transformation id** — the `id` you read off the
execution plan visualization. Format: `pipeline.dag.with.parallelism: <id1>:<p1>,<id2>:<p2>`.

This differs from the existing `pipeline.jobvertex-parallelism-overrides`, which is keyed by
*JobVertexID* (a post-chaining hash) and is applied in the Dispatcher. Transformation ids exist only
before chaining, so this option takes effect during **StreamGraph generation, client-side**.

Works for DataStream jobs and for Flink SQL — both reach the same translators (see §4).

> Implementation in this tree: `DagParallelismOverride` plus six translators
> (`flink-streaming-java`), and the `-pd,--dagParallelism` CLI flag (`flink-clients`). Automated
> coverage: `StreamGraphGeneratorDagParallelismTest`, `DagParallelismOptionTest`,
> `DagParallelismOverrideITCase`, `SqlDagParallelismOverrideITCase` — see §8.

## 0. Build a dist that contains the change

`flink-dist` shades all `org.apache.flink:*` classes into one jar, so recompiling
`flink-core`/`flink-streaming-java` alone does **not** reach a running cluster:

```bash
./mvnw install -DskipTests -Dfast -Pskip-webui-build \
    -pl flink-core,flink-streaming-java,flink-dist
```

(On a cleaned tree `-pl flink-dist` needs `-am` or a prior full reactor build at least once.)

```bash
./build-target/bin/start-cluster.sh          # Web UI + REST on http://localhost:8081
```

## 1. Read the transformation ids off the plan

```bash
./build-target/bin/flink info build-target/examples/streaming/StateMachineExample.jar
```

This prints the JSON execution plan — one node per operator, each with `id`, `type` and
`parallelism`. That `id` is the transformation id and is exactly what the option keys on:

```json
{ "nodes": [
  { "id": 1, "type": "Source: Custom Source", "parallelism": 1 },
  { "id": 2, "type": "Flat Map",              "parallelism": 4 },
  { "id": 4, "type": "Sink: Print to Std. Out", "parallelism": 4 } ] }
```

Paste the same JSON into <https://flink.apache.org/visualizer/> for the graphical view.

**Ids are not stable across code changes** — they come from a counter incremented per transformation
as the job is built. Re-read them after editing the job.

## 2. Run with an override

Any of these three works; `config.yaml` is the safest because both the client and the JobManager
read it.

```bash
# a) per submission
./build-target/bin/flink run \
    -D pipeline.dag.with.parallelism='2:4,4:2' \
    build-target/examples/streaming/StateMachineExample.jar

# b) dedicated CLI flag, same effect as (a)
./build-target/bin/flink run -pd '2:4,4:2' \
    build-target/examples/streaming/StateMachineExample.jar

# c) cluster-wide — build-target/conf/config.yaml, then restart the cluster
#    pipeline.dag.with.parallelism: 2:4,4:2
```

For SQL Client, use `SET` — but read §4 first, the id workflow is genuinely different there.

## 3. Verify

Plan (pre-chaining — the authoritative check, shows every operator individually):

```bash
./build-target/bin/flink info -D pipeline.dag.with.parallelism='2:4' \
    build-target/examples/streaming/StateMachineExample.jar
```

Node `2` must now report `"parallelism": 4`, and **no other node may change**.

Running job (post-chaining, what actually got deployed):

```bash
JOB=$(./build-target/bin/flink list -r | grep -oE '[0-9a-f]{32}' | head -1)
curl -s "http://localhost:8081/jobs/$JOB" | jq '.vertices[] | {name, parallelism}'
```

The Web UI shows the same under each vertex, and the Task Managers tab shows the subtask count.

## 4. Through the Flink SQL Client

SQL reaches the same translators: the planner emits `Transformation`s and `DefaultExecutor`
merges the table config into the environment before handing them to the same `StreamGraphGenerator`.
So the option applies to planner-generated operators, and `SET` is how you set it.

**But transformation ids behave differently in SQL than in a jar job, and getting that wrong is the
main way this workflow fails.**

### The id trap

Ids come from a JVM-global counter. A DataStream program builds its transformations once and reuses
them, so the ids `flink info` prints are the ids that run. The planner allocates a **fresh set on
every translation**, and the SQL Client is a long-lived session. Re-running the *identical* statement
twice in one session:

| translation | source | Calc | sink |
| --- | --- | --- | --- |
| 1st `EXPLAIN` | 1 | **2** | 4 |
| 2nd `EXPLAIN`, same statement | 5 | **6** | 8 |

So you cannot `EXPLAIN`, read id `2`, then `INSERT` in that same session — the insert's Calc is id 6
and your override silently hits nothing.

Two facts make it workable, both verified:

- **DDL and `SET` consume no ids.** `CREATE TABLE`, `SET`, `USE` allocate no transformations, so the
  counter is still at zero when the first query is translated.
- **The counter resets with the client JVM.** In a fresh embedded session the first translated
  statement always starts at id 1.

⇒ **The statement you want to tune must be the first translation of a fresh session.**

### Step 1 — fresh session, read the ids

```bash
./build-target/bin/sql-client.sh
```

```sql
CREATE TABLE src (f0 BIGINT) WITH ('connector'='datagen', 'number-of-rows'='100');
CREATE TABLE snk (f0 BIGINT) WITH ('connector'='blackhole');

EXPLAIN JSON_EXECUTION_PLAN
INSERT INTO snk SELECT f0 FROM src WHERE f0 > 10;
```

The `== Physical Execution Plan ==` block at the end is the same JSON `flink info` prints:

```json
{ "nodes": [
  { "id": 1, "type": "Source: src[1]", "parallelism": 8 },
  { "id": 2, "type": "Calc[2]",        "parallelism": 8 },
  { "id": 4, "type": "snk[3]: Writer", "parallelism": 8 } ] }
```

**`id` is what the option keys on — not the `[n]` suffix inside `type`.** That suffix is the ExecNode
id and the two disagree (`snk[3]` is id `4` above). Keying off the name suffix is a silent no-op.

### Step 2 — restart the client, then SET before anything is translated

Quit (`QUIT;`) and start `sql-client.sh` again, so the counter is back at zero:

```sql
CREATE TABLE src (f0 BIGINT) WITH ('connector'='datagen', 'number-of-rows'='100');
CREATE TABLE snk (f0 BIGINT) WITH ('connector'='blackhole');

SET 'pipeline.dag.with.parallelism' = '2:3';

EXPLAIN JSON_EXECUTION_PLAN
INSERT INTO snk SELECT f0 FROM src WHERE f0 > 10;
```

`Calc[2]` must now report `"parallelism": 3` while the source and sink stay at the default. This is
the whole check, and it needs no cluster — `EXPLAIN` is the SQL equivalent of `flink info`.

### Step 3 — run it for real

The verifying `EXPLAIN` in step 2 has itself consumed ids 1–4, so **restart once more** and issue
DDL + `SET` + `INSERT` with no `EXPLAIN` in between:

```sql
CREATE TABLE src (f0 BIGINT) WITH ('connector'='datagen', 'number-of-rows'='1000000');
CREATE TABLE snk (f0 BIGINT) WITH ('connector'='blackhole');

SET 'parallelism.default' = '4';
SET 'pipeline.dag.with.parallelism' = '2:3';

INSERT INTO snk SELECT f0 FROM src WHERE f0 > 10;
```

Then check the deployed job, since you can no longer use `EXPLAIN` without disturbing the ids:

```bash
JOB=$(./build-target/bin/flink list -r | grep -oE '[0-9a-f]{32}' | head -1)
curl -s "http://localhost:8081/jobs/$JOB" | jq '.vertices[] | {name, parallelism}'
```

The `Calc` vertex must be 3, everything else 4. Note that the differing parallelism also breaks the
source→Calc chain, so you will see more vertices than an unoverridden run — that is expected.

### SQL-specific caveats

- **Gateway mode does not reset the counter.** The planner runs in the long-lived SQL Gateway JVM, so
  restarting the *client* changes nothing and ids depend on everything that gateway has translated
  since it started. The fresh-session recipe above assumes embedded `sql-client.sh`. (Verified that
  the counter is JVM-global and per-process; a gateway deployment was not tested.)
- **Any statement that translates burns ids** — a stray `SELECT` for a quick look, a failed `INSERT`,
  an `EXPLAIN`. If in doubt, restart.
- **Editing the query changes the ids**, exactly as editing a DataStream job does.
- This id instability is a property of keying on transformation id, not of the implementation. If SQL
  is a primary target, keying on something stable (ExecNode id, operator UID) would avoid it — that
  is a design change, not a fix.

## 5. Check more than one operator kind

The override is applied in the transformation translators, and the parallelism computation it hooks
into is duplicated across **seven** of them:

| Translator | Reached by |
| --- | --- |
| `AbstractOneInputTransformationTranslator` | `map`, `filter`, `process`, keyed operators |
| `AbstractTwoInputTransformationTranslator` | `connect(...).map/process` |
| `MultiInputTransformationTranslator` | multi-input operators (mostly Table/SQL) |
| `SourceTransformationTranslator` | `fromSource`, `fromSequence` |
| `LegacySourceTransformationTranslator` | `addSource` |
| `SinkTransformationTranslator` | `sinkTo` (Sink V2) |
| `LegacySinkTransformationTranslator` | `addSink`, `print` |

A translator that was missed ignores the option **silently** — the job runs, just at the old
parallelism. So don't validate on a `map` alone: override one source, one mid-stream operator and
one sink in the same run and confirm all three moved.

## 6. Batch mode: check the override is not reset

This is the trap worth testing by hand. `StreamingJobGraphGenerator` resets any vertex whose
parallelism is *not marked as explicitly configured* back to the default on a dynamic graph — which
is batch mode with the AdaptiveBatch scheduler and `execution.batch.adaptive.auto-parallelism.enabled`
(both defaults). If the override sets the parallelism but leaves that flag alone, it is thrown away
at JobGraph generation and only in batch:

```bash
./build-target/bin/flink run -Dexecution.runtime-mode=BATCH \
    -D pipeline.dag.with.parallelism='2:4' \
    build-target/examples/streaming/WordCount.jar
```

The vertex must show parallelism 4 — not `-1` / an adaptively chosen value. Streaming mode passing
while batch mode ignores the override is the signature of exactly this bug.

## 7. What to watch out for

- **Chaining changes shape.** Operators only chain when their parallelisms match, so overriding one
  operator in a chain *splits the chain* — the running job legitimately shows more vertices than
  before. That is expected, and it is why step 3 checks the plan (pre-chaining) as well as the
  running job.
- **Non-parallel sources are not protected.** The override is applied unconditionally, so pointing it
  at a source that cannot be parallelised (`fromData`/`fromElements`, and some connectors) does raise
  the number in the graph — whether the job then runs is up to that source. Override sources only
  when you know they are parallel.
- **No effect at all** — the dist is stale. Confirm the option really made it into the shaded jar
  (prints `1` if present, `0` if stale):

  ```bash
  unzip -p build-target/lib/flink-dist-1.20.4.jar \
      org/apache/flink/configuration/PipelineOptions.class | grep -ac dag.with.parallelism
  ```

  This is the single most common false alarm — `build-target/` predates the feature until step 0 is
  rerun.
- **An id that matches nothing is a silent no-op.** This is the failure mode to plan around: a typo,
  a stale id, or a SQL id read from the wrong session produces no error and no effect — the job just
  runs at its old parallelism. Malformed *values* are rejected (non-numeric id, non-numeric or
  non-positive parallelism all raise `IllegalConfigurationException`), but an unmatched-yet-well-formed
  id is not, because a translator only ever sees the transformation it is translating. Always confirm
  the parallelism actually changed rather than assuming the override landed.
- Stale `/tmp/flink-*.pid` files cause bogus "TaskManager already running"; logs are under
  `build-target/log/`.

## 8. Automated tests

```bash
# graph-level contract, per operator kind, and the adaptive-batch reset
./mvnw test   -pl flink-streaming-java -Dfast -DfailIfNoTests=false \
    -Dtest=StreamGraphGeneratorDagParallelismTest

# the -pd,--dagParallelism CLI flag
./mvnw test   -pl flink-clients -Dfast -DfailIfNoTests=false -Dtest=DagParallelismOptionTest

# end to end on a MiniCluster: DataStream, and SQL
./mvnw verify -pl flink-tests -Dfast -DfailIfNoTests=false \
    -Dtest='DagParallelismOverrideITCase,SqlDagParallelismOverrideITCase'
```

Current state: everything green except
`StreamGraphGeneratorDagParallelismTest$Validation.rejectsAnUnknownTransformationId`. A
translator-level hook only ever sees the transformation it is translating, so an id matching nothing
is invisible without a separate validation pass over the option against the full id set — which is
also why a typo'd id is a silent no-op in §7.

The nested classes map onto the sections above: `OperatorKinds` fails per missed translator (§5),
`AdaptiveParallelism` covers the batch reset (§6). `SqlDagParallelismOverrideITCase` covers §4,
including the `SET`-on-TableConfig path.
