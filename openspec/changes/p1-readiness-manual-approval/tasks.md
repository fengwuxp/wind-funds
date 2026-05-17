# Tasks: P1 Readiness and Manual Approval Gates

> 2026-05-18 再校准：清算、清结算、结算出款、对账差错和报表相关任务已从当前有效 CAD 队列作废或移除。本文中的相关拆分只作为历史准入草稿保留；下一轮如需继续，必须重新设计并创建新的 OpenSpec change。当前只保留余额投影和交易投影的重新查验入口。

## 1. Change Scaffolding

- [x] Create P1 readiness OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real pipeline.

## 2. Voided Clearing, Settlement, Reconciliation and Reporting Readiness

- [x] Void the previously split clearing, settlement/payout, reconciliation and reporting readiness queue from the current effective backlog.
- [x] Keep the old source-fact, state-machine, idempotency, audit and posting notes as historical design drafts only.
- [x] Remove the related focused test names, DDL work, payout integration, reconciliation adjustment and report snapshot work from the actionable task list.
- [x] Require a fresh product/system design review and a new OpenSpec change before any future clearing, settlement, reconciliation or reporting task is reintroduced.

## 3. FX Operations Readiness

- [x] Split FX operations into separate future changes:
  - quote and quote lock.
  - execution result and external evidence.
  - fees, gain/loss and reporting.
  - cross-border or regulatory task references.
- [x] Keep transaction-layer no-auto-FX redline as regression protection.
- [x] Define focused tests before implementation:
  - `FxQuoteExecutionTests`.
  - `CrossBorderComplianceBoundaryTests`.
  - `RegulatoryReportingRetryTests`.
- [x] Require Harness manual approval before FX execution, DDL or regulatory reporting implementation.

## 4. Balance and Transaction Projection Re-check Readiness

- [x] Void archive movement, report/metric governance, metric backfill and related DDL work from the current effective backlog.
- [x] Keep `BalanceCheckpoint`, `BalanceProjectionWatermark`, balance rebuild and transaction view replay notes as historical projection design drafts until the re-check is completed.
- [x] Keep only balance projection and transaction projection design/process re-check as the current effective task.
- [x] Require a separate user-confirmed OpenSpec change before formal archive movement, balance rebuild, transaction view replay implementation or any projection DDL is reintroduced.

## 5. Cross-Cutting Gates

- [x] Define a P1 approval packet template in system design docs or the future implementation change.
- [x] Confirm future P1 changes reference PRD, DSL, OpenSpec, SAD and API contract test plans.
- [x] Confirm future P1 changes state rollback, compensation, replay and audit strategy.
- [x] Do not enter code implementation until the specific future change has its own proposal, design, tasks, spec delta and user-confirmed execution grant.

## 6. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
