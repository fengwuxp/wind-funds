# Tasks: P1 Readiness and Manual Approval Gates

## 1. Change Scaffolding

- [x] Create P1 readiness OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real pipeline.

## 2. Clearing and Reconciliation Readiness

- [ ] Split clearing/reconciliation implementation into separate future changes:
  - clearing candidates and clearing batch.
  - settlement order and payout order.
  - reconciliation batch, exception and adjustment.
  - report snapshot and read-only reporting boundary.
- [ ] For each future change, define source facts, state machines, idempotency keys, audit fields and ledger posting events.
- [ ] Define focused tests before implementation:
  - `MerchantClearingBatchServiceTests`.
  - `SettlementOrderServiceTests`.
  - `PayoutResultServiceTests`.
  - `ReconciliationMatchingServiceTests`.
  - `ReconciliationExceptionAdjustmentTests`.
- [ ] Require Harness manual approval before DDL, payout, reconciliation adjustment or blocking-rule implementation.

## 3. FX Operations Readiness

- [ ] Split FX operations into separate future changes:
  - quote and quote lock.
  - execution result and external evidence.
  - fees, gain/loss and reporting.
  - cross-border or regulatory task references.
- [ ] Keep transaction-layer no-auto-FX redline as regression protection.
- [ ] Define focused tests before implementation:
  - `FxQuoteExecutionTests`.
  - `CrossBorderComplianceBoundaryTests`.
  - `RegulatoryReportingRetryTests`.
- [ ] Require Harness manual approval before FX execution, DDL or regulatory reporting implementation.

## 4. Archive, Replay and Metrics Readiness

- [ ] Split archive governance into separate future changes:
  - `BalanceCheckpoint` and `BalanceProjectionWatermark`.
  - `ArchiveManifest` and manual archive precheck.
  - balance rebuild task and difference report.
  - transaction view replay task.
  - `MetricWatermark` and `MetricSnapshot`.
- [ ] Define focused tests before implementation:
  - `BalanceProjectionArchiveContractTests`.
  - `BalanceWatermarkAdvanceTests`.
  - `TransactionViewReplayRangeTests`.
  - `MetricWatermarkTests`.
- [ ] Require Harness manual approval before archive movement, formal rebuild, formal replay, metric backfill or DDL.

## 5. Cross-Cutting Gates

- [x] Define a P1 approval packet template in system design docs or the future implementation change.
- [x] Confirm future P1 changes reference PRD, DSL, OpenSpec, SAD and API contract test plans.
- [x] Confirm future P1 changes state rollback, compensation, replay and audit strategy.
- [x] Do not enter code implementation until the specific future change has its own proposal, design, tasks, spec delta and user-confirmed execution grant.

## 6. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
