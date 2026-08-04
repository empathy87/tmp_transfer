# Dynamic Checkpoint Parameters

**What is in the last commit** — branch `feature/dynamic-options`, one commit on top of the
`release-1.20.4` tag (47 files, +5240 / −62 lines).

This document uses simple words. It explains what the feature does, how to use it, and how it works
inside.

---

## 1. What this feature does

You can change some checkpoint settings of a job **while the job is running**.

- The job does **not** stop.
- The job does **not** restart.
- The job keeps all its state.
- You send **one HTTP request** and the new setting is in force right away.

Seven settings can be changed this way. They are listed in section 3.

### Why this is useful

Before this feature, changing the checkpoint interval meant:

1. stop the job with a savepoint,
2. edit the config,
3. start the job again from the savepoint.

That costs downtime every time. It is painful when you only want to make checkpoints a bit slower
because the storage system is busy, or a bit faster before a planned failover test.

Now it is one `PATCH` call and no downtime.

---

## 2. Turning the feature on

The feature is **off by default**. Turn it on in `conf/config.yaml`:

```yaml
jobmanager.execution.dynamic-configuration.enabled: true
```

- Option name: `jobmanager.execution.dynamic-configuration.enabled`
- Defined in: `JobManagerOptions.DYNAMIC_CONFIGURATION_ENABLED`
- Default: `false`
- While it is `false`, the endpoint answers **403 Forbidden**.

This is a JobManager setting. You must restart the cluster after you change it. You do **not** need
to restart the cluster later — only this one switch needs it.

> If you get a 403 and you did not expect it, check this switch first. It is the most common cause.

---

## 3. The seven settings you can change

All keys are normal Flink config keys. They are the same keys you write in `config.yaml` or in a SQL
`SET` statement.

| Key | What it means | Rules |
|---|---|---|
| `execution.checkpointing.interval` | How often a checkpoint starts. | Must be at least `10 ms`. Cannot be set to the "off" value (see section 7.6). |
| `execution.checkpointing.interval-during-backlog` | How often a checkpoint starts while the job is processing backlog. | May be turned off. When it is on, it must be **greater than or equal to** `interval`. `0` means "off". |
| `execution.checkpointing.timeout` | How long one checkpoint may take before Flink gives up on it. | Must be at least `10 ms`. |
| `execution.checkpointing.min-pause` | The smallest pause between the end of one checkpoint and the start of the next. | Must be `0` or more. Values above one year are cut down to one year. |
| `execution.checkpointing.max-concurrent-checkpoints` | How many checkpoints may run at the same time. | Must be `1` or more. Must be exactly `1` when unaligned checkpoints are on. |
| `execution.checkpointing.tolerable-failed-checkpoints` | How many checkpoints may fail in a row before the job fails. | Must be `0` or more. |
| `execution.checkpointing.aligned-checkpoint-timeout` | How long an aligned checkpoint waits before it switches to unaligned. | Must be `0` or more. |

Nothing else can be changed this way. Any other key is refused with **400 Bad Request**.

---

## 4. How to call it

### The endpoint

```
PATCH /jobs/:jobid/checkpoints/configuration
```

The body is one JSON object called `configuration`. Inside it you put the keys you want to change:

```json
{
  "configuration": {
    "execution.checkpointing.interval": "5 s",
    "execution.checkpointing.timeout": "2 min"
  }
}
```

Important points about the body:

- **Values are text, in the same form as `config.yaml`.** Times need a unit, for example `5 s`,
  `500 ms`, `2 min`. Flink's own config parser reads them.
- **Keys you do not send are not changed.** There is one exception, see section 7.7.
- **An empty `configuration` is refused** with 400. You must send at least one key.
- On success you get **200 OK** with an empty body.

### Example with curl

```bash
curl -X PATCH -H 'Content-Type: application/json' \
  -d '{"configuration": {"execution.checkpointing.interval": "5 s"}}' \
  http://localhost:8081/jobs/<jobid>/checkpoints/configuration
```

### Example from Java

`RestClusterClient` has a method for it:

```java
Map<String, String> config = new HashMap<>();
config.put("execution.checkpointing.interval", "5 s");

client.updateCheckpointConfiguration(jobId, new CheckpointConfigUpdateRequestBody(config))
        .get();   // returns Acknowledge
```

### How to see the result

Ask the normal checkpoint config endpoint:

```bash
curl http://localhost:8081/jobs/<jobid>/checkpoints/config
```

It shows the **new** values. The code keeps this reported copy in step with the running
configuration (`ExecutionGraph.refreshCheckpointCoordinatorConfiguration()`), so you do not read
stale numbers.

---

## 5. Answer codes

| Code | When you get it |
|---|---|
| **200** | The update was applied. |
| **400** | The body names a key that is not one of the seven; or a value cannot be parsed; or a value breaks a rule (for example `max-concurrent-checkpoints` is `0`); or the body is empty; or a value is `null`. |
| **403** | `jobmanager.execution.dynamic-configuration.enabled` is `false`. |
| **404** | There is no job with this id. |
| **409** | The job cannot take the update **right now**: it is not in state `RUNNING`, or checkpointing is off for this job, or periodic checkpointing is off for this job. |
| **501** | This job runs on a scheduler that does not support live updates (see section 8). |
| **503** | The job is still starting up. Try again. |

One detail worth knowing: only `InvalidCheckpointConfigurationException` becomes a 400. A plain
`IllegalArgumentException` does **not**. The same is true on the 501 side: only
`UnsupportedSchedulerException` becomes a 501, a plain `UnsupportedOperationException` does not.
This is on purpose. Those other errors mean a bug in Flink, not a bad request from the user, so they
must not look like user errors.

---

## 6. What happens if the update is wrong

The update is **all or nothing**.

1. Flink builds the full new configuration first. It takes the values you sent and fills the rest
   from the values in use now.
2. It **checks** the whole new configuration.
3. Only if the check passes does it **apply** it.
4. If applying fails half way, Flink **rolls back** to the old values and timers.

So a bad request never leaves the job in a half-changed state.

**There is one exception on purpose.** Making the timeout shorter can make checkpoints that are
already running expire at once. Those checkpoints are aborted. If one abort fails, Flink writes an
error to the log and goes on: it keeps the new configuration and it still aborts the other expired
checkpoints. The reason is simple — an aborted checkpoint cannot be brought back, so there is
nothing to roll back to. Do not "fix" this; two tests pin it
(`testFailedAbortOfAnExpiredCheckpointKeepsTheNewConfiguration`,
`testFailedAbortStillAbortsTheRemainingExpiredCheckpoints`).

---

## 7. Rules that surprise people

These are all real, wanted behaviours. Tests pin every one of them.

### 7.1 A longer interval does not push back the next checkpoint

Only a **shorter** interval moves the next checkpoint earlier. If you make the interval longer, the
checkpoint that is already scheduled still runs at its old time. The new, longer interval starts to
count after that one. So after raising the interval you will still see one more checkpoint on the
old rhythm.

### 7.2 `min-pause` can win over `interval`

The interval really used is `max(interval, min-pause)`. If you set the interval to 5 s but
`min-pause` is 30 s, checkpoints come every 30 s. The stored interval is still 5 s; only the
effective one is 30 s.

### 7.3 A timeout change also reaches checkpoints that are already running

When the timeout changes, Flink re-arms the timers of the checkpoints in flight. The time already
spent counts. Example: a checkpoint has been running for 40 s and you set the timeout to 60 s — it
has 20 s left, not 60 s.

### 7.4 Savepoints keep their old timeout

A savepoint that is already running is not touched by a timeout change. It keeps the timeout it was
given when it started.

### 7.5 Changing `tolerable-failed-checkpoints` does not reset the counter

The failures counted so far stay counted. The new limit is applied to that same counter. If 3
checkpoints already failed and you set the limit to 2, the job fails.

### 7.6 You cannot switch periodic checkpointing off through this endpoint

The "off" value for the interval is `Long.MAX_VALUE`, which you can write as
`9223372036854775807 ms`. It parses fine, and then it is refused with **400**.

The reason: if you could switch checkpointing off this way, you could not switch it back on again
through the same endpoint. So the door stays closed.

### 7.7 The backlog interval may move on its own

This is the one case where a key you did not send still changes.

- If you raise `interval` and you do **not** send `interval-during-backlog`, and the backlog interval
  is on and now smaller than the new interval, then Flink raises the backlog interval to the same
  value. Otherwise the rule "backlog interval >= interval" would break.
- If you **do** send `interval-during-backlog` and it is smaller than the interval, the request is
  refused with 400. Flink does not silently fix your own value.
- `0` means "off". It is turned into the "off" value for you.
- Unlike the regular interval, the backlog interval **may** be switched off. Only the regular one is
  protected.

### 7.8 Raising `max-concurrent-checkpoints` wakes up waiting requests

Checkpoint requests that were queued because the limit was reached are looked at again at once. This
happens after the new configuration is in force, because starting a checkpoint cannot be rolled
back. If that wake-up fails, the new configuration still stays in force and a warning is logged.

### 7.9 Two updates at the same time are safe

You do not need any locking. All updates for one job go through the JobMaster main thread one after
the other, and each one is checked and applied under the `CheckpointCoordinator` lock.

---

## 8. Which schedulers support it

| Scheduler | Supported? | What happens |
|---|---|---|
| `DefaultScheduler` (streaming, the normal one) | **Yes** | Works while the job is `RUNNING`. |
| `AdaptiveScheduler` | **Yes** | Works while the job is in the `Executing` state. The update also survives a rescale (see section 9). |
| `AdaptiveBatchScheduler` | **No** | Always **501**. It does not look at the job state or at the coordinator. |
| Any other `SchedulerNG` | **No** | Gets the interface default, which throws the same error → **501**. |

`AdaptiveBatchScheduler` is a subclass of `DefaultScheduler`, so it **inherited** a working
implementation by accident. That was not wanted, so it now overrides the method and always throws.

---

## 9. The update is not saved anywhere

This is the most important limit of the feature. Read it before you rely on it in production.

- Nothing is written to the JobGraph.
- Nothing is written to the JobGraphStore.
- Nothing is written to any other store.

So:

- **If the JobManager fails over, the update is lost.** The job comes back with the checkpoint
  settings it was submitted with.
- If you want the change to be permanent, also change your job's submit-time configuration.

There is **one** piece of memory, and it is process-local:

`AdaptiveScheduler` keeps the last applied configuration in a field called `checkpointingOverrides`.
When that scheduler rebuilds its ExecutionGraph — which it does on rescale and on restart — it
applies the saved values to the new `CheckpointCoordinator`. Without this, a normal rescale would
quietly throw your update away.

`DefaultScheduler` does not need such a field. It builds its ExecutionGraph once, in the
`SchedulerBase` constructor, and never rebuilds it inside one JobManager.

Durable saving was built once and then removed on purpose on 2026-07-31. The design is kept in
`live-update-ha.md` for later.

---

## 10. How it works inside

### The path of one request

```
curl PATCH  (or RestClusterClient.updateCheckpointConfiguration)
  → CheckpointConfigUpdateHandler            checks the 7 keys, parses values, maps errors to codes
  → RestfulGateway / Dispatcher.updateCheckpointConfiguration   checks the feature flag (403), forwards
  → JobMasterGateway.applyCheckpointConfigUpdate               (main thread from here on)
  → SchedulerNG.applyCheckpointConfigUpdate
        · SchedulerBase        → DefaultScheduler path
        · AdaptiveScheduler    → StateWithExecutionGraph (only in state Executing)
        · AdaptiveBatchScheduler → always throws (501)
  → CheckpointCoordinator.applyCheckpointConfigUpdate          under its own lock
  → ExecutionGraph.refreshCheckpointCoordinatorConfiguration() so GET .../checkpoints/config is fresh
```

Everything from `JobMasterGateway` outwards returns `Acknowledge`. Only the coordinator returns the
real, effective configuration — `AdaptiveScheduler` keeps that value for its rebuilds.

**The Dispatcher only forwards.** It checks the feature flag and calls the JobMaster. It does not
save anything, so it also needs no "another update is running" guard. (Compare
`updateJobResourceRequirements`, which does save something and therefore keeps such a guard.)

### The two value objects

People mix these two up. They are different on purpose.

| Class | What it is | Shape |
|---|---|---|
| `CheckpointConfigUpdate` | The **partial change** that came in the request. | Every getter is an `Optional`. Built with `CheckpointConfigUpdate.builder()`. |
| `CheckpointConfigurationState` | The **full, finished** configuration that is really in force. | Plain values, no `Optional`. Built with `fromCheckpointCoordinatorConfiguration(...)`. |

`state.withUpdate(update)` lays the partial change on top of the full state and gives a new full
state. `applyCheckpointConfigUpdate(update)` takes the first kind and returns the second kind.
`AdaptiveScheduler.checkpointingOverrides` holds the second kind.

### Inside the coordinator

`CheckpointCoordinator.applyCheckpointConfigUpdate` does this, all under `lock`:

1. Refuse if periodic checkpointing is not configured (`CheckpointingNotEnabledException` → 409).
2. Build the candidate state with `withUpdate`.
3. Refuse if the coordinator is already shut down.
4. `CheckpointConfigurationStateValidator.validate(candidate, unalignedCheckpointsEnabled)`.
5. Take a rollback snapshot (`snapshotReconfigurationState` → `CheckpointReconfigurationSnapshot`):
   the old state, whether a periodic trigger was scheduled, and when it was due.
6. Put the candidate in force:
   - new min-pause and new max-concurrent go to `CheckpointRequestDecider`,
   - new failure limit goes to `CheckpointFailureManager` (the counter is kept),
   - `reschedulePeriodicTriggerIfEarlier()` — moves the next trigger only if it is now earlier,
   - if the timeout changed, `reArmPendingCheckpointTimeouts(...)` re-arms pending checkpoints and
     aborts the expired ones.
7. If step 6 throws, `restoreReconfigurationState(rollback)` puts everything back, and the original
   error is thrown. If the restore itself fails too, that failure is attached to the original error
   as a suppressed error.
8. If max-concurrent went up, wake the queued requests.

To make the values changeable, three fields that used to be `final` became `volatile`:
`CheckpointFailureManager.tolerableCpFailureNumber`,
`CheckpointRequestDecider.maxConcurrentCheckpointAttempts` and
`CheckpointRequestDecider.minPauseBetweenCheckpoints`.

`PendingCheckpoint` learned to remember when its timeout started (`timeoutBaseTimeMillis`) and to
swap its timer (`replaceCancellerHandle`). That is what makes "time already spent counts" work.

---

## 11. Files in the commit

### New main classes

| File | Job |
|---|---|
| `checkpoint/CheckpointConfigUpdate.java` | the partial change |
| `checkpoint/CheckpointConfigurationState.java` | the full state |
| `checkpoint/CheckpointConfigurationStateValidator.java` | all the range and consistency checks |
| `checkpoint/CheckpointingNotEnabledException.java` | → 409 |
| `checkpoint/InvalidCheckpointConfigurationException.java` | → 400 (extends `IllegalArgumentException`) |
| `scheduler/JobNotExecutingException.java` | → 409 |
| `scheduler/UnsupportedSchedulerException.java` | → 501 |
| `rest/handler/job/checkpoints/CheckpointConfigUpdateHandler.java` | the REST handler + the 7-key list |
| `rest/messages/checkpoints/CheckpointConfigUpdateHeaders.java` | method, URL, description |
| `rest/messages/checkpoints/CheckpointConfigUpdateRequestBody.java` | the `configuration` map |

The seven-key list is the field `CheckpointConfigUpdateHandler.LIVE_UPDATABLE_CONFIG_KEYS`. It is
built from `CheckpointingOptions` constants, not from text, so a renamed option cannot slip through.

### Changed main classes

- `checkpoint/CheckpointCoordinator.java` — apply, validate, roll back, re-arm timers
- `checkpoint/CheckpointFailureManager.java` — the limit can be set, the counter is kept
- `checkpoint/CheckpointRequestDecider.java` — min-pause and max-concurrent can be set
- `checkpoint/PendingCheckpoint.java` — remembers the timeout start, can swap its timer
- `jobgraph/tasks/CheckpointCoordinatorConfiguration.java` — new `toBuilder()`
- `executiongraph/ExecutionGraph.java`, `DefaultExecutionGraph.java` — `refreshCheckpointCoordinatorConfiguration()`
- `dispatcher/Dispatcher.java` — the feature flag and the forwarding
- `jobmaster/JobMaster.java`, `JobMasterGateway.java` — one new method
- `scheduler/SchedulerNG.java` — default method that throws
- `scheduler/SchedulerBase.java` — the DefaultScheduler path
- `scheduler/adaptive/AdaptiveScheduler.java`, `StateWithExecutionGraph.java` — the adaptive path plus rebuild survival
- `scheduler/adaptivebatch/AdaptiveBatchScheduler.java` — always throws
- `webmonitor/RestfulGateway.java`, `WebMonitorEndpoint.java` — gateway method and route
- `configuration/JobManagerOptions.java` (flink-core) — the feature flag
- `client/program/rest/RestClusterClient.java` (flink-clients) — the Java client method

### Generated files (do not edit by hand)

- `docs/layouts/shortcodes/generated/rest_v1_dispatcher.html`
- `docs/static/generated/rest_v1_dispatcher.yml`
- `docs/layouts/shortcodes/generated/all_jobmanager_section.html`
- `docs/layouts/shortcodes/generated/job_manager_configuration.html`
- `flink-runtime-web/src/test/resources/rest_api_v1.snapshot`

Rebuild them with:

```bash
./mvnw package -Dgenerate-rest-docs   -pl flink-docs -am -nsu -DskipTests
./mvnw package -Dgenerate-config-docs -pl flink-docs -am -nsu -DskipTests -Pskip-webui-build
./mvnw test -pl flink-runtime-web -Dtest=RuntimeRestAPIStabilityTest -Dgenerate-rest-snapshot
```

---

## 12. Tests

### New test classes

| Class | What it covers |
|---|---|
| `CheckpointCoordinatorReconfigureTest` | the core: check-before-change, rollback, timer moves, timeout re-arming, backlog rules, failure counter (54 tests) |
| `CheckpointConfigurationStateTest` | the full state object (10 tests) |
| `CheckpointConfigurationStateValidatorTest` | every rule of the validator (15 tests) |
| `CheckpointConfigUpdateHandlerTest` | the key list, parsing, and every status code (25 tests) |
| `DispatcherCheckpointConfigUpdateTest` | the feature flag, forwarding, error passing (4 tests) |
| `DynamicCheckpointConfigITCase` (in `flink-tests`) | a real MiniCluster job: the update works and the job never restarts (2 tests) |

### Changed test classes

`AdaptiveSchedulerTest` (+6 tests: rescale survival, the two refusals, and
`testLiveUpdateLeavesTheJobGraphUntouched` which pins that nothing is saved),
`DefaultSchedulerTest` (+5 tests: the only coverage of the non-adaptive path, including
`testNotRunningIsReportedBeforeCheckpointingIsChecked`, which fixes the order of the two checks),
`AdaptiveBatchSchedulerTest` (+2 tests: it stays unsupported, even while the job runs),
plus the hand-written doubles `CheckpointCoordinatorTestingUtils`, `PendingCheckpointTest`,
`TestingJobMasterGateway`, `TestingJobMasterGatewayBuilder`.

### How to run them

```bash
# the six unit classes in flink-runtime
./mvnw test -pl flink-runtime -Dfast -DfailIfNoTests=false -Dtest='\
CheckpointCoordinatorReconfigureTest,CheckpointConfigurationStateTest,\
CheckpointConfigurationStateValidatorTest,CheckpointConfigUpdateHandlerTest,\
DispatcherCheckpointConfigUpdateTest,AdaptiveBatchSchedulerTest'

# the scheduler suites
./mvnw test -pl flink-runtime -Dfast -Dtest='AdaptiveSchedulerTest,DefaultSchedulerTest'

# the integration test lives in flink-tests and needs `verify`, not `test`
./mvnw install -pl flink-runtime -DskipTests -Dfast     # first, or the ITCase uses an old jar
./mvnw verify  -pl flink-tests -Dfast -Dtest=DynamicCheckpointConfigITCase -DfailIfNoTests=false
```

**Watch out:** `-DfailIfNoTests=false` means a wrong class name is skipped in silence, not reported.
Count the classes in the output — the first command must report **6**.

**Watch out again:** the ITCase takes every module except `flink-tests` from `~/.m2`, not from your
working tree. Run the `install` line above after you touch runtime code, or you will test old code.

### Trying it by hand on a real cluster

The step-by-step runbook is `dynamic-param-testing.md`. Before you start, the dist must contain the
feature:

```bash
unzip -l build-target/lib/flink-dist-1.20.4.jar | grep -c CheckpointConfigUpdate   # 0 means: rebuild
./mvnw install -DskipTests -Dfast -Pskip-webui-build -pl flink-runtime,flink-dist
```

Note that this count only tells you the feature is **there**. It does not tell you the dist is
**current**. A dist built before your latest edit still shows the same count.

---

## 13. What is not done yet

- **Parallelism.** Changing parallelism live (through the FLIP-291 rescale path, AdaptiveScheduler
  only) is designed but not built.
- **The umbrella endpoint.** `GET`/`PUT /jobs/:jobid/dynamic-configuration` and the
  `DynamicJobConfigOverrides` record do not exist in the tree.
- **Durable saving.** Built once, removed on 2026-07-31. Design kept in `live-update-ha.md`.
- **Version / requestId locking.** No optimistic locking. The last writer wins.

### Related documents

| File | What it is |
|---|---|
| `dynamic-parameters.md` | the full design, with file and line references — the source of truth |
| `dynamic-parameters-dd.md` | the same feature written as a formal design document |
| `dynamic-param-testing.md` | the hands-on manual test runbook |
| `live-update-ha.md` | the parked design for durable saving |
| `rewrite-task.md` | why the request body is config-native and not the typed body of FLIP-571 |
| `adaptive-scheduler.md` | notes on the AdaptiveScheduler state machine |
