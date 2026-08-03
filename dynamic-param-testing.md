# Manual testing: live checkpoint configuration updates (SQL Client + curl)

Hands-on runbook for exercising the dynamic checkpoint-configuration feature on this machine.
Everything below was verified against the tree at commit `1396830b89f` (the single `tmp` commit on
top of `release-1.20.4`) on 2026-08-03.

Companion docs: `dynamic-parameters.md` (authoritative design, §16.5 has the original demo),
`adaptive-scheduler.md` (state machine), `live-update-ha.md` (the durability design that was
deliberately *not* implemented).

---

## 0. What this feature actually is (read before testing)

`PATCH /jobs/:jobid/checkpoints/configuration` changes **seven** `execution.checkpointing.*`
parameters on a **running** job with **no restart**.

The seven keys — the handler rejects anything else with 400:

| Key | Type |
|---|---|
| `execution.checkpointing.interval` | duration |
| `execution.checkpointing.interval-during-backlog` | duration |
| `execution.checkpointing.timeout` | duration |
| `execution.checkpointing.min-pause` | duration |
| `execution.checkpointing.max-concurrent-checkpoints` | int |
| `execution.checkpointing.tolerable-failed-checkpoints` | int |
| `execution.checkpointing.aligned-checkpoint-timeout` | duration |

Three properties that shape every test below:

1. **The update is runtime-only and deliberately NOT durable.** Nothing is written to the JobGraph or
   JobGraphStore. A JobManager failover loses it and the job returns to its submit-time configuration.
   §12 tests exactly this — a "failure" there would actually be a bug.
2. **The feature is off by default.** Without `jobmanager.execution.dynamic-configuration.enabled:
   true` every call returns **403**. This is the single most common wasted debugging hour.
3. **Raising and lowering an interval are not symmetric.** See §4 — this surprises everyone once.
4. **A live update changes the cadence of *existing* checkpointing; it cannot switch checkpointing
   on.** Submit the job with `execution.checkpointing.interval` already set. A job without periodic
   checkpointing is **rejected with 409** rather than silently accepted — see §8 and §8.1.

---

## 1. One-time setup

### 1.1 Rebuild the distribution — mandatory

`build-target/lib/flink-dist-1.20.4.jar` is dated **2026-07-15**, which predates the whole feature.
`flink-dist` *shades* all `org.apache.flink:*` classes into that one jar, so recompiling
`flink-runtime` alone changes nothing for a running cluster. Check first:

```bash
cd ~/oss/flink
unzip -l build-target/lib/flink-dist-1.20.4.jar | grep -c CheckpointConfigUpdate
```

`0` means the endpoint will answer **404** and you would be debugging a stale jar. Rebuild:

```bash
./mvnw install -DskipTests -Dfast -Pskip-webui-build -pl flink-runtime,flink-dist
```

Re-run the `unzip | grep -c` check; expect a non-zero count. If `-pl flink-dist` fails with a
missing-file error, the assembly copies jars by literal relative path from sibling `target/`
directories — do one full `./mvnw install -DskipTests -Dfast -Pskip-webui-build` at the repo root,
then this incremental form works from then on.

> **Trap that will waste an hour: an incremental `flink-dist` build can silently ship stale runtime
> classes.** `BUILD SUCCESS` and a fresh jar mtime are *not* proof your change is in the dist. Hit
> live during this work: `flink-runtime/target/classes`, `flink-runtime/target/*.jar` and `~/.m2` all
> contained the new code, yet the freshly rebuilt `flink-dist` jar still carried a **hours-old** copy
> of `CheckpointCoordinator.class`. Only `clean` cleared it:
>
> ```bash
> ./mvnw clean install -DskipTests -Dfast -Pskip-webui-build -pl flink-dist
> ```
>
> Verify by the **entry timestamp inside the jar**, not the jar's own mtime, and never by
> `strings | grep` (a substring of your new message may already exist in the class — that produced a
> false "it's there" here). The reliable check:
>
> ```bash
> unzip -l build-target/lib/flink-dist-1.20.4.jar \
>   | grep 'runtime/checkpoint/CheckpointCoordinator.class'
> unzip -l flink-runtime/target/flink-runtime-1.20.4.jar \
>   | grep 'runtime/checkpoint/CheckpointCoordinator.class'
> ```
>
> The two timestamps must match. If the dist one is older, the shade used a stale input — `clean`.

> **`clean` on `flink-dist` resets `build-target/conf/`.** The exploded distribution is a build
> artifact, so a clean rebuild restores the stock `config.yaml` — wiping the feature flag and your
> slot count, which then shows up as a **403** or a job stuck waiting for slots. Re-apply §1.2 after
> every clean, or keep a copy of your `config.yaml` outside `build-target/`.

> When you later patch `flink-streaming-java` (§9), add it to the `-pl` list and rebuild again.
> Every code change needs a `flink-dist` rebuild before the cluster sees it.

### 1.2 Enable the feature flag

Edit `build-target/conf/config.yaml`:

```yaml
jobmanager:
  execution:
    dynamic-configuration:
      enabled: true
```

This is standard YAML in 1.20, so the nested form above is correct; the flat
`jobmanager.execution.dynamic-configuration.enabled: true` also parses. Default is `false` → 403.

While you are in the file, give yourself room for the concurrency and rescale tests:

```yaml
taskmanager:
  numberOfTaskSlots: 4
```

### 1.3 Start the cluster

```bash
./build-target/bin/start-cluster.sh
```

Web UI / REST: <http://localhost:8081>. Logs:
`build-target/log/flink-<user>-standalonesession-0-<host>.log` (JobManager — this is the one that
matters) and `...-taskexecutor-0-...log`.

If a TaskManager refuses to start claiming it is already running, delete the stale `/tmp/flink-*.pid`
files — they live in `/tmp`, not the dist directory.

---

## 2. Submit the test job from the SQL Client

```bash
./build-target/bin/sql-client.sh
```

Checkpointing must be configured **before** the `INSERT`, because the submitted JobGraph carries the
initial configuration:

```sql
SET 'execution.checkpointing.interval' = '30 s';
SET 'execution.checkpointing.timeout' = '10 min';
SET 'execution.checkpointing.min-pause' = '0 ms';
SET 'execution.checkpointing.max-concurrent-checkpoints' = '1';
SET 'execution.checkpointing.tolerable-failed-checkpoints' = '0';
SET 'execution.checkpointing.dir' = 'file:///tmp/flink-checkpoints';

CREATE TABLE src (
  id BIGINT
) WITH (
  'connector' = 'datagen',
  'rows-per-second' = '100',
  'fields.id.min' = '1',
  'fields.id.max' = '1000'
);

CREATE TABLE sink (
  k BIGINT,
  cnt BIGINT
) WITH (
  'connector' = 'blackhole'
);

INSERT INTO sink SELECT id % 10 AS k, COUNT(*) AS cnt FROM src GROUP BY id % 10;
```

The `GROUP BY` gives the job real keyed state, so checkpoints have something to snapshot.

The `INSERT` prints a job ID. If you lose it:

```sql
SHOW JOBS;
```

Keep it in a shell variable in a **second terminal** (leave the SQL Client running):

```bash
JOB=<paste-job-id>
```

### 2.1 Baseline read

```bash
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config | python3 -m json.tool
```

This endpoint reports six of the seven parameters — `interval`, `timeout`, `min_pause`,
`max_concurrent`, `tolerable_failed_checkpoints`, `aligned_checkpoint_timeout`. It does **not**
expose the backlog interval (see §10). The Web UI shows the same data under
**Job → Checkpoints → Configuration**.

> **Read-back is cached for ~3 s.** `/jobs/:jobid/checkpoints/config` is served through
> `ExecutionGraphCache`, whose TTL is `web.refresh-interval` (default **3000 ms**). Query it
> immediately after a PATCH and you will see the *old* value and conclude the update did nothing.
> Wait ~5 s, or re-query twice, before believing a read-back. This bites on every test below.

---

## 3. Smoke test: does the endpoint exist and is it enabled?

```bash
curl -i -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "5 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```

Expect **200 OK** with an empty body.

| You got | Meaning |
|---|---|
| 404 with "Not found" for the *URL* | `flink-dist` not rebuilt → §1.1 |
| 403 | feature flag off → §1.2, then restart the cluster |
| 200 | working; continue |

---

## 4. `interval` — and the raise/lower asymmetry

Values take a unit suffix (`5 s`, `500 ms`, `2 min`) because Flink's own config parser handles them.

**Lowering takes effect immediately.** The coordinator reschedules the pending periodic trigger:

```bash
curl -s -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "5 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```

Watch the cadence change in the JobManager log:

```bash
tail -f build-target/log/flink-*-standalonesession-*.log | grep 'Triggering checkpoint'
```

You should see checkpoints move from every 30 s to every 5 s within one cycle.

**Raising does NOT cancel the already-scheduled trigger.** The relevant code is
`CheckpointCoordinator.reschedulePeriodicTriggerIfEarlier()`, whose comment reads *"Only a shorter
active interval can advance the next trigger."* So after:

```bash
curl -s -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "10 min"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```

…one more checkpoint still fires on the **old** 5 s schedule, and only *after* that does the 10 min
interval govern. This is intended. Verify the new value took effect immediately even though the
*timer* did not:

```bash
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config | python3 -m json.tool | grep -w interval
```

Expect `600000`.

**Side effect worth checking:** raising the interval also raises an enabled backlog interval that
would otherwise fall below it (`CheckpointConfigurationState.intervalDuringBacklogFor`). That keeps
the configuration internally valid; §10 covers it.

---

## 5. `min-pause` — the effective interval

The scheduler uses `max(interval, min-pause)`
(`CheckpointConfigurationState.effectiveCheckpointInterval()`). So a min-pause *above* the interval
silently governs the cadence.

```bash
curl -s -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {
  "execution.checkpointing.interval": "5 s",
  "execution.checkpointing.min-pause": "20 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```

Expect checkpoints roughly every 20 s, not 5 s, in the `Triggering checkpoint` log lines. Both values
read back unchanged from `/checkpoints/config` — the max is applied at scheduling time, not stored.

**Clamp check.** A huge min-pause is normalised to one year (`MAX_MIN_PAUSE_BETWEEN_CHECKPOINTS`).
Use a large-but-sane duration — a value near `Long.MAX_VALUE` milliseconds overflows during parsing
and returns 400 instead of demonstrating the clamp:

```bash
curl -s -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.min-pause": "3650 d"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config | python3 -m json.tool | grep min_pause
```

Expect `31536000000` (365 days in ms). Reset with `"min-pause": "0 ms"` before continuing.

---

## 6. `tolerable-failed-checkpoints` — value only, without failures

Raising the tolerance is safe to check on its own:

```bash
curl -s -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.tolerable-failed-checkpoints": "5"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config \
  | python3 -m json.tool | grep tolerable
```

The runtime counter is *not* reset by an update — the new limit is compared against the failures
already accumulated. Driving actual failures needs the patch in §9.

---

## 7. `aligned-checkpoint-timeout` and `max-concurrent-checkpoints` — value checks

```bash
curl -s -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {
  "execution.checkpointing.aligned-checkpoint-timeout": "30 s",
  "execution.checkpointing.max-concurrent-checkpoints": "3"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config | python3 -m json.tool
```

Both read back changed. Observing them *behaviourally* needs slow checkpoints (§9): concurrency only
shows up when checkpoint N+1 starts before N finishes, and alignment timeout only matters under
backpressure.

Note one validator rule you can trip here: with unaligned checkpoints enabled,
`max-concurrent-checkpoints` must be 1. If you enabled unaligned checkpoints in §2, a value > 1 is
rejected with **400** and the message names both options.

---

## 8. Rejection paths

Run these against the same live job. Add `-o /dev/null -w '%{http_code}\n'` to print just the code.

```bash
code() { curl -s -o /tmp/resp.json -w '%{http_code}' "$@"; echo; cat /tmp/resp.json; echo; }
```

**400 — key not in the whitelist:**
```bash
code -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.mode": "AT_LEAST_ONCE"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```
The message lists all seven allowed keys.

**400 — empty body / unparsable or invalid value:**
```bash
code -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
code -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "not-a-duration"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
code -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.max-concurrent-checkpoints": "0"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```

**400 — cross-field validation.** Backlog interval below the regular interval, both named explicitly
(the auto-raise in §4 only applies when the backlog key is *omitted*):
```bash
code -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {
  "execution.checkpointing.interval": "60 s",
  "execution.checkpointing.interval-during-backlog": "10 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```

**404 — unknown job:**
```bash
code -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "5 s"}}' \
  http://localhost:8081/jobs/00000000000000000000000000000000/checkpoints/configuration
```

**409 — checkpointing not enabled.** There are **two distinct causes**, both 409, with different
messages. Verified on a live cluster.

*(a) No `CheckpointCoordinator` at all* — a **batch** job, whose dynamic graph skips checkpointing
entirely:

```sql
SET 'execution.runtime-mode' = 'batch';
CREATE TABLE bsrc (id BIGINT) WITH (
  'connector'='datagen','rows-per-second'='200','number-of-rows'='200000');
CREATE TABLE bsink (id BIGINT) WITH ('connector'='blackhole');
INSERT INTO bsink SELECT id FROM bsrc;
```

`number-of-rows` makes the source bounded (required for batch) and `rows-per-second` keeps the job
alive long enough to PATCH it. Capture its id from the SQL Client output, then:

```bash
BJOB=<batch-job-id>
code -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "5 s"}}' \
  http://localhost:8081/jobs/$BJOB/checkpoints/configuration
```

**Verified:** returns **409** with
`CheckpointingNotEnabledException: Cannot update the checkpoint configuration because checkpointing
is not enabled for this job.` The JobManager log confirms why, one line per such job:

```
Skip setting up checkpointing for a job with dynamic graph.
```

Batch jobs use the `AdaptiveBatch` scheduler by default, which builds a *dynamic* graph;
`DefaultExecutionGraphBuilder` skips checkpoint setup for those, so `getCheckpointCoordinator()` is
`null` and `SchedulerBase.requireCheckpointCoordinator()` throws.

*(b) A coordinator exists but periodic checkpointing was never configured* — a **streaming** job
submitted without `execution.checkpointing.interval`. Start a fresh SQL Client session and never set
the interval (checkpointing is off by default; there is no "disable" value to pass — the internal
disabled sentinel is `Long.MAX_VALUE`, not `0`):

```sql
CREATE TABLE src2 (id BIGINT) WITH ('connector'='datagen','rows-per-second'='10');
CREATE TABLE sink2 (id BIGINT) WITH ('connector'='blackhole');
INSERT INTO sink2 SELECT id FROM src2;
```

PATCH that job id → **409**:

```
Cannot update the checkpoint configuration because periodic checkpointing is not enabled
for this job. Submit the job with 'execution.checkpointing.interval' set to enable it.
```

Confirm the reported interval is left **untouched** at `9223372036854775807`
(`DISABLED_CHECKPOINT_INTERVAL`) — a rejected update must not mutate anything.

**409 — job not running.** This is `SchedulerBase.requireRunningJob`; the state name appears in the
message. Give yourself a long restart backoff in `config.yaml`:
```yaml
restart-strategy.type: fixed-delay
restart-strategy.fixed-delay.attempts: 10
restart-strategy.fixed-delay.delay: '60 s'
```
Restart the cluster and submit the §2 job. **Don't bother writing a failing UDF** — just kill the
TaskManager, which fails the job deterministically and parks it in `RESTARTING` for the full 60 s:

```bash
./build-target/bin/taskmanager.sh stop
# poll until the state flips, then PATCH
curl -s http://localhost:8081/jobs/$JOB | python3 -c 'import sys,json;print(json.load(sys.stdin)["state"])'
```

**Verified:** the state reaches `RESTARTING` within ~10 s and the PATCH returns **409** —
`… the job is not currently executing (state: RESTARTING). Retry once the job is running.`
Restart the TaskManager afterwards with `./build-target/bin/taskmanager.sh start`.

**403 — flag off.** Set `jobmanager.execution.dynamic-configuration.enabled: false`, restart the
cluster, resubmit, PATCH → 403.

**503** happens only while a job is still initializing — hard to hit deliberately; retrying works.

**501** is mapped from `UnsupportedSchedulerException`, thrown by `SchedulerNG`'s interface default.
In practice every shipped scheduler implements the operation (`AdaptiveBatchScheduler extends
DefaultScheduler extends SchedulerBase`, and `AdaptiveScheduler` implements it directly), so **you
cannot reach 501 from a normal deployment** — it is a guard for future schedulers. Don't waste time
trying.

### 8.1 Why a streaming job "without checkpointing" is rejected too

Cause (b) above is not obvious, because a streaming job *does* have a `CheckpointCoordinator` even
when you never configured an interval:

- `StreamingJobGraphGenerator.configureCheckpointing()` runs **unconditionally** and always calls
  `jobGraph.setSnapshotSettings(...)`. An unset interval is merely normalised to
  `DISABLED_CHECKPOINT_INTERVAL` (`Long.MAX_VALUE`).
- `DefaultExecutionGraphBuilder.isCheckpointingEnabled(jobGraph)` is just
  `getCheckpointingSettings() != null`, so a **`CheckpointCoordinator` is always created** for a
  streaming job, and `requireCheckpointCoordinator()` succeeds.
- But the periodic scheduler was never started: `startCheckpointScheduler()` is only invoked when
  `isPeriodicCheckpointingConfigured()`, which was false at submit time.
  `reschedulePeriodicTriggerIfEarlier()` then bails out immediately at
  `if (!isPeriodicCheckpointingStarted()) return;`.

So the coordinator would happily *store* a new interval that nothing ever acts on.

**This used to return 200.** The endpoint accepted the update, `/checkpoints/config` reported the new
interval, and **no checkpoint ever ran** — measured at `total: 0` well past 25 s with a 5 s interval
and zero `Triggering checkpoint` lines. `CheckpointCoordinator.applyCheckpointConfigUpdate` now
guards on `isPeriodicCheckpointingConfigured()` and throws `CheckpointingNotEnabledException`
instead, so the futile update is refused rather than silently reported as applied. The guard sits in
the coordinator, so both `DefaultScheduler` and `AdaptiveScheduler` get it; the ExecutionGraph-rebuild
re-apply path (`applyCheckpointConfigurationState`) is deliberately not guarded, so AdaptiveScheduler
rescale survival is unaffected.

For contrast, the same PATCH on a job submitted **with** `interval = 30 s` works as designed: the
read-back shows `5000` and 7 checkpoints complete in the following ~30 s.

**Practical rule: only PATCH a job that was submitted with checkpointing already enabled.** Confirm
success with the checkpoint *counts*, not only the config read-back:

```bash
curl -s http://localhost:8081/jobs/$JOB/checkpoints \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["counts"])'
```

---

## 9. Temporary code patches (apply → test → roll back)

Some parameters only *show* themselves when checkpoints are slow or failing. These patches are
throwaway. The feature code is committed in `HEAD`, so `git checkout --` restores any file cleanly.

> **Rebuild after every patch**, otherwise the cluster runs the old bytecode. Use `clean` for
> `flink-dist` — see the stale-shade trap in §1.1 — and re-apply the §1.2 config afterwards, because
> cleaning `flink-dist` resets `build-target/conf/`:
> ```bash
> ./mvnw install -DskipTests -Dfast -Pskip-webui-build -pl flink-runtime,flink-streaming-java
> ./mvnw clean install -DskipTests -Dfast -Pskip-webui-build -pl flink-dist
> # re-apply conf/config.yaml (feature flag + slots), then:
> ./build-target/bin/stop-cluster.sh && ./build-target/bin/start-cluster.sh
> ```

### 9.1 Patch A — make every checkpoint slow

File: `flink-streaming-java/src/main/java/org/apache/flink/streaming/runtime/tasks/SubtaskCheckpointCoordinatorImpl.java`

Insert at the very start of the `checkpointState(...)` body (currently line 278, just before
`checkNotNull(options);`):

```java
        // TEMP TEST PATCH - REMOVE
        Thread.sleep(10000);
```

The method already declares `throws Exception`, so no other change is needed. Every subtask now takes
≥10 s to snapshot.

**What this unlocks:**

*`timeout` shrinking actually expiring a checkpoint.* Set a timeout below the sleep and watch the
in-flight checkpoint die:
```bash
curl -s -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {
  "execution.checkpointing.interval": "30 s",
  "execution.checkpointing.timeout": "3 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```
A timeout change re-arms **pending** checkpoints, not just future ones
(`reArmPendingCheckpointTimeouts`), so a checkpoint already running longer than the new timeout is
expired right away. Look for `Checkpoint N of job ... expired` in the JobManager log and a failed
entry in **Checkpoints → History**.

*`max-concurrent-checkpoints`.* With a 10 s snapshot and a 2 s interval, raising concurrency to 3
produces overlapping checkpoints — visible as several rows simultaneously `IN_PROGRESS` in
**Checkpoints → History**:
```bash
curl -s -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {
  "execution.checkpointing.interval": "2 s",
  "execution.checkpointing.timeout": "10 min",
  "execution.checkpointing.min-pause": "0 ms",
  "execution.checkpointing.max-concurrent-checkpoints": "3"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```
Then drop it back to `1` and confirm the overlap stops for *new* attempts — in-flight ones are not
cancelled ("Concurrency changes affect new attempts only").

*`tolerable-failed-checkpoints` driving a job failure.* Keep the 3 s timeout so checkpoints keep
expiring, set the tolerance low, and watch the job fail once the counter passes it:
```bash
curl -s -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.tolerable-failed-checkpoints": "2"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```
Expect the job to fail after the third expiry, with the failure-manager message in the log. Raising
the tolerance to a large number instead should keep the job alive through many expiries — that is the
more useful direction to verify, since it proves the limit is genuinely live.

**Roll back:**
```bash
git diff --stat                       # should show only this one file
git checkout -- flink-streaming-java/src/main/java/org/apache/flink/streaming/runtime/tasks/SubtaskCheckpointCoordinatorImpl.java
git diff --stat                       # empty
```

### 9.2 Patch B — force "processing backlog" on

Needed for §10 because no SQL connector in this tree reports backlog. Backlog is reported by sources
through `IsProcessingBacklogEvent`, which reaches
`CheckpointCoordinator.setIsProcessingBacklog(OperatorID, boolean)`.

The least invasive fake is in `flink-runtime`, `CheckpointCoordinator.getCurrentCheckpointInterval()`
(line 2162), which currently reads:

```java
    long getCurrentCheckpointInterval() {
        return backlogOperators.isEmpty()
                ? configurationState.effectiveCheckpointInterval()
                : configurationState.getCheckpointIntervalDuringBacklog();
    }
```

Temporarily force the backlog branch:

```java
    long getCurrentCheckpointInterval() {
        // TEMP TEST PATCH - REMOVE
        if (true) {
            return configurationState.getCheckpointIntervalDuringBacklog();
        }
        return backlogOperators.isEmpty()
                ? configurationState.effectiveCheckpointInterval()
                : configurationState.getCheckpointIntervalDuringBacklog();
    }
```

**Roll back:** `git checkout -- flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/CheckpointCoordinator.java`

### 9.3 (not needed) A job that fails on demand

Earlier drafts suggested patching in a `throw` to reach the `RESTARTING` window. **No patch is
needed** — `./build-target/bin/taskmanager.sh stop` fails the job deterministically and is what §8
now uses.

---

## 10. `interval-during-backlog`

Two things make this the awkward one:

- It is **not exposed** by `GET /jobs/:jobid/checkpoints/config` (`CheckpointConfigInfo` has no field
  for it), so you cannot read the value back over REST.
- No bundled SQL connector reports backlog, so the branch is never taken in a normal SQL job.

So test it in two halves:

**Value/validation half (no patch).** The cross-field rule from §8 proves the value is being parsed
and validated. Also verify the auto-raise: with the backlog key *omitted*, raising the regular
interval above an enabled backlog interval silently raises the backlog interval to match, rather than
400-ing. Setting the backlog interval to `0` disables periodic checkpoints during backlog.

**Behaviour half (Patch B).** With §9.2 applied, the backlog interval becomes the *only* interval in
force. Set the two to obviously different values and confirm the cadence follows the backlog one:

```bash
curl -s -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {
  "execution.checkpointing.interval": "60 s",
  "execution.checkpointing.interval-during-backlog": "60 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
# then lower only the backlog interval
curl -s -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval-during-backlog": "5 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
```

Expect the cadence to move to 5 s while `/checkpoints/config` still reports `interval: 60000`. Roll
Patch B back afterwards.

---

## 11. Atomicity / rollback

A rejected update must leave the job exactly as it was. Read the config, send a mixed valid+invalid
update, read again:

```bash
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config | python3 -m json.tool > /tmp/before.json
curl -s -X PATCH -H 'Content-Type: application/json' -d '{"configuration": {
  "execution.checkpointing.interval": "7 s",
  "execution.checkpointing.max-concurrent-checkpoints": "0"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config | python3 -m json.tool > /tmp/after.json
diff /tmp/before.json /tmp/after.json && echo "UNCHANGED - correct"
```

The `0` fails validation, so the valid `7 s` must **not** be applied either — validation runs before
any mutation. Expect an empty diff.

---

## 12. Non-durability (this is a feature, not a bug)

```bash
curl -s -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "5 s"}}' \
  http://localhost:8081/jobs/$JOB/checkpoints/configuration
curl -s http://localhost:8081/jobs/$JOB/checkpoints/config | grep -o '"interval":[0-9]*'
```

Now kill and restart the JobManager while keeping the job. After recovery the interval is back to the
**submit-time** 30 s, not 5 s. Nothing is written to the JobGraph —
`AdaptiveSchedulerTest.testLiveUpdateLeavesTheJobGraphUntouched` pins this in unit-test form.

> **This one cannot be tested on a plain standalone cluster.** A true JobManager failover needs HA,
> and 1.20 ships only ZooKeeper and Kubernetes HA services — there is no filesystem-based HA to fall
> back on. Without HA, stopping the JobManager destroys the job, which demonstrates nothing about the
> update. Left unverified in the local run; rely on the unit test unless you stand up ZooKeeper.

What you *can* observe locally is the opposite — which restarts a live update **survives**:
- **DefaultScheduler, task-failure restart:** survives. The ExecutionGraph (and its
  `CheckpointCoordinator`) is reused, so the update stays in force. Verified: a timeout update was
  still reported after the job failed and restored.
- **AdaptiveScheduler, rescale:** survives, via the process-local `checkpointingOverrides` re-applied
  to the rebuilt graph (§13).

---

## 13. AdaptiveScheduler variant (rescale survival)

The only place a process-local override is kept is `AdaptiveScheduler.checkpointingOverrides`, which
is re-applied after the ExecutionGraph is rebuilt on rescale. `DefaultScheduler` needs nothing
equivalent because it builds its graph once.

```yaml
jobmanager:
  scheduler: Adaptive
```

Restart the cluster and resubmit the §2 job (streaming SQL defaults to the `Default` scheduler, so
this must be set explicitly). Then:

1. PATCH the interval to `5 s`; confirm via `/checkpoints/config`.
2. Rescale — read the current requirements, edit the upper bound, put them back:
   ```bash
   curl -s http://localhost:8081/jobs/$JOB/resource-requirements | python3 -m json.tool > /tmp/rr.json
   # edit parallelism upperBound in /tmp/rr.json, then:
   curl -s -X PUT -H 'Content-Type: application/json' -d @/tmp/rr.json \
     http://localhost:8081/jobs/$JOB/resource-requirements
   ```
3. Wait for the job to reach the new parallelism, then read `/checkpoints/config` again.

Expect `interval: 5000` to **survive**. Without the override field a rescale would silently revert to
30 s — that regression is what the three `...SurviveRescale` cases in `AdaptiveSchedulerTest` guard.

Also worth doing here: PATCH while the scheduler is *not* `Executing` → **409** from
`AdaptiveScheduler.requireExecuting` (a different code path from the `SchedulerBase` check in §8).

**Requesting more slots than the cluster has does *not* work** — AdaptiveScheduler simply runs the
job at reduced parallelism and reports `RUNNING`, so the PATCH succeeds with 200. Verified: a
parallelism-8 job on 4 slots ran happily. You need **zero** free slots:

```bash
./build-target/bin/taskmanager.sh stop     # slots-total -> 0
# submit the job; it parks in WaitingForResources with job state CREATED
```

**Verified:** PATCH then returns **409** — `… (state: WaitingForResources). Retry once the job is
running.` Note the state named is the *scheduler* state, not the JobStatus.

Set `jobmanager.scheduler` back to `Default` when finished.

---

## 14. Cleanup

```bash
./build-target/bin/stop-cluster.sh
git status --porcelain | grep -v '^??'      # must be empty - no leftover test patches
rm -rf /tmp/flink-checkpoints
```

Revert `build-target/conf/config.yaml` if you want the dist back at defaults (it is a build artifact,
not tracked by git, so it will be overwritten by the next full `flink-dist` build anyway).

---

## 15. Troubleshooting

| Symptom | Cause |
|---|---|
| 404 on the endpoint URL | `flink-dist` not rebuilt — §1.1 |
| 403 on every call | feature flag off, or cluster not restarted after editing `config.yaml` |
| 409 "checkpointing is not enabled" | no `CheckpointCoordinator` — a **batch**/dynamic-graph job (§8a) |
| 409 "periodic checkpointing is not enabled" | streaming job submitted without an interval; a live update cannot switch checkpointing on (§8b, §8.1) |
| 409 with `(state: ...)` | job genuinely not RUNNING — expected during RESTARTING/CANCELLING |
| Read-back shows the old value right after a PATCH | 3 s `ExecutionGraphCache` TTL (`web.refresh-interval`) — re-query (§2.1) |
| Code change not in the cluster despite `BUILD SUCCESS` | incremental `flink-dist` shipped a stale shaded class — `clean` rebuild, compare jar **entry** timestamps (§1.1) |
| 403 or "waiting for slots" after a rebuild | `clean` on `flink-dist` reset `build-target/conf/config.yaml` — re-apply §1.2 |
| Interval change has "no effect" | you *raised* it; the already-armed trigger fires once more first (§4) |
| Cadence ignores the interval | `min-pause` is higher — effective interval is the max (§5) |
| Backlog interval not in the config response | it is genuinely not exposed (§10) |
| Code change not visible | `flink-dist` shades everything; rebuild and restart |
| Job vanished after cluster stop | no HA configured — expected |
