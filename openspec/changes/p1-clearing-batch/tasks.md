# Tasks: P1 Clearing Batch

## 1. Change Scaffolding

- [x] Create `p1-clearing-batch` OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real Harness pipeline.

## 2. Product and System Boundary

- [x] Define `ClearingCandidate`, `ClearingBatch` and `ClearingItem` semantics.
- [x] Clarify internal clearing context and avoid external clearing institution semantics.
- [x] Define state machine, terminal states and confirmation preconditions.
- [x] Define candidate idempotency key, policy snapshot and exclusion reason set.
- [x] Define clearing confirmation transaction boundary and ledger posting event.

## 3. Spec Delta

- [x] Add `clearing-reconciliation` requirements for candidate generation, batch confirmation, duplicate clearing and rerun versioning.
- [x] Add `transaction-layer` requirement for `CLEARING_BATCH` source fact reference.
- [x] Keep settlement order, payout, reconciliation exception, report, FX and archive work out of this change.

## 4. Future Implementation Tasks

- [ ] Before coding, confirm Execution Grant for `p1-clearing-batch` implementation.
- [ ] Before coding, create failing `MerchantClearingBatchServiceTests` covering candidate idempotency, exclusion reasons, confirmation posting and duplicate confirmation.
- [ ] Before DDL, provide table design, unique constraints, migration and rollback plan for manual approval.
- [ ] Before confirmation posting implementation, provide funds instruction mapping, route event mapping and rollback/compensation plan for manual approval.
- [ ] After implementation, run focused clearing batch tests, transaction tests, compile and PMD according to the final write scope.

## 5. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
