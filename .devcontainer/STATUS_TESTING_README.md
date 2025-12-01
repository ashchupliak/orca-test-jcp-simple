# Devcontainer Scenarios for Status Testing

This directory contains devcontainer configurations designed to test all possible environment statuses in jcp-orca-facade.

## Available Scenarios

### 1. scenario-success
**Purpose:** Test STOPPED status (successful runCmd completion)

**Config:** `.devcontainer/scenario-success/devcontainer.json`

**Command:** `echo 'SUCCESS: Command completed' && exit 0`

**Expected Status Flow:** PENDING → RUNNING → **STOPPED**

**Actual Behavior:** PENDING → RUNNING (stays forever)

**Issue:** orca-compute doesn't send `Finished(exitCode=0)` status

---

### 2. scenario-explicit-exit
**Purpose:** Test STOPPED status with explicit exit 0

**Config:** `.devcontainer/scenario-explicit-exit/devcontainer.json`

**Command:** `exit 0`

**Expected Status Flow:** PENDING → RUNNING → **STOPPED**

**Actual Behavior:** PENDING → RUNNING (stays forever)

**Issue:** Same as scenario-success

---

### 3. scenario-failure
**Purpose:** Test FAILED status via non-zero exit code

**Config:** `.devcontainer/scenario-failure/devcontainer.json`

**Command:** `echo 'FAILURE: Exiting with code 1' && exit 1`

**Expected Status Flow:** PENDING → RUNNING → **FAILED**

**Actual Behavior:** PENDING → RUNNING (stays forever)

**Issue:** Exit codes not propagated, same root cause as STOPPED

---

### 4. scenario-longrunning
**Purpose:** Test normal RUNNING status (long-lived service)

**Config:** `.devcontainer/scenario-longrunning/devcontainer.json`

**Command:** `sleep infinity`

**Expected Status Flow:** PENDING → RUNNING (indefinitely)

**Actual Behavior:** ✅ Works as expected

---

### 5. scenario-crash
**Purpose:** Test FAILED status via runtime crash

**Config:** `.devcontainer/scenario-crash/devcontainer.json`

**Command:** `sleep 5 && kill -9 $$`

**Expected Status Flow:** PENDING → RUNNING → **FAILED**

**Actual Behavior:** PENDING → RUNNING (likely stays forever)

**Issue:** Process termination not detected

---

### 6. scenario-graceful-shutdown
**Purpose:** Test STOPPING status (if reachable)

**Config:** `.devcontainer/scenario-graceful-shutdown/devcontainer.json`

**Command:** `sleep infinity` (then user terminates)

**Expected Status Flow:** RUNNING → **STOPPING** → STOPPED → TERMINATING → TERMINATED

**Actual Behavior:** RUNNING → TERMINATING → TERMINATED (skips STOPPING)

**Issue:** STOPPING status is never set by reconciler (dead code)

---

## How to Use

### With HTTP Files (manual-playground/environment-status-testing.http):

1. Open `manual-playground/environment-status-testing.http` in IntelliJ IDEA
2. Each test is numbered and documented
3. Run the POST request to create environment
4. Run GET requests to poll status
5. Observe status transitions

### With curl:

```bash
# Test 1: Success scenario (should reach STOPPED but stays RUNNING)
curl -X POST 'https://orca-server-nightly.labs.jb.gg/api/environments' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
  "definition": {
    "type": "devcontainer",
    "git": {
      "repositories": [{
        "cloneUrl": "https://github.com/ashchupliak/orca-test-jcp-simple",
        "ref": "andrii/statuses"
      }]
    },
    "workspaceFolder": "orca-test-jcp-simple",
    "config": {
      "type": "path",
      "path": ".devcontainer/scenario-success/devcontainer.json"
    }
  }
}'

# Poll status
ENV_ID="<ID_FROM_RESPONSE>"
curl -X GET "https://orca-server-nightly.labs.jb.gg/api/environments/$ENV_ID" \
  -H "Authorization: Bearer $TOKEN"
```

## Status Reachability Matrix

| Status | Scenario | Reachable? | Notes |
|--------|----------|------------|-------|
| PENDING | All scenarios | ✅ Yes | Initial status on creation |
| ACCEPTED | All scenarios | ⚠️ Too fast | Transitions in <3 seconds |
| STARTING | All scenarios | ⚠️ Too fast | Transitions in <3 seconds |
| RUNNING | 1-6 | ✅ Yes | All scenarios reach RUNNING |
| **STOPPED** | 1, 2 | ❌ No | **Bug: orca-compute doesn't send Finished** |
| **STOPPING** | 6 | ❌ No | **Dead code: no reconciler path sets it** |
| TERMINATING | User action | ✅ Yes | POST /terminate endpoint |
| TERMINATED | User action | ✅ Yes | After termination completes |
| **FAILED** (runCmd) | 3, 5 | ❌ No | **Bug: exit codes not propagated** |
| FAILED (provisioning) | Invalid repo | ✅ Yes | Git clone failures work |
| REJECTED | Invalid config | ✅ Yes | Validation failures work |
| UNSPECIFIED | Proto mismatch | ❌ No | Infrastructure-only |

## Expected vs Actual Behavior

### Working Transitions:
- ✅ PENDING → RUNNING (happy path)
- ✅ RUNNING → TERMINATING → TERMINATED (user termination)
- ✅ PENDING → FAILED (git clone failure)
- ✅ PENDING → REJECTED (validation failure)

### Broken Transitions:
- ❌ RUNNING → STOPPED (runCmd success) - **stays RUNNING**
- ❌ RUNNING → FAILED (runCmd failure) - **stays RUNNING**
- ❌ RUNNING → STOPPING → STOPPED - **STOPPING never set**

## Root Cause

**Status Synchronization Gap:**
- orca-compute doesn't send status updates when runCmd completes
- Facade has correct mapping: `Finished(exitCode) → STOPPED`
- But never receives `Finished` status from compute
- Result: environments stay RUNNING forever

**STOPPING Status:**
- Defined in enum: `OrcaEnvStatusDb.STOPPING`
- Never set by reconciler logic
- Either incomplete feature or should be removed

## Related Files

- **HTTP Tests:** `manual-playground/environment-status-testing.http`
- **Status Enums:** `server/src/main/kotlin/.../orcaEnvironment/db.kt:148-200`
- **Reconciler Logic:** `server/src/main/kotlin/.../reconciler/OrcaEnvironmentReconciler.kt:122-203`
- **Proto Mapping:** `server/src/main/kotlin/.../orcaEnvironment/db_to_grpc.kt:109-121`

## References

- Issue: Status synchronization gap between facade and orca-compute
- State diagram shows STOPPING but code doesn't implement it
- STOPPED and FAILED (via runCmd) are unreachable in current implementation
