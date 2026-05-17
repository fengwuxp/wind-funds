# Tasks: P1 Clearing Batch

> 2026-05-18 作废说明：本 change 已从当前 CAD 任务队列移除。清算、清结算相关内容仅作为历史设计草稿保留，不作为下一轮实现、测试或 DDL 的有效依据；如后续重新启动，必须重新设计并创建新的 OpenSpec change。

## 1. Change Scaffolding

- [x] Create `p1-clearing-batch` OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real Harness pipeline.

## 2. Voided Historical Draft Scope

- [x] Mark the previous `ClearingCandidate`, `ClearingBatch`, `ClearingItem`, state machine, idempotency key and ledger posting notes as historical draft only.
- [x] Remove the previous `clearing-reconciliation` and `transaction-layer` spec delta work from the effective task queue.
- [x] Keep settlement order, payout, reconciliation exception, report, FX and archive work out of this voided change.

## 3. Voided Implementation Tasks

- [x] Void the previous implementation queue for `p1-clearing-batch`.
- [x] Remove `MerchantClearingBatchServiceTests`, clearing candidate DDL, clearing confirmation posting and rerun compensation from the current effective backlog.
- [x] Require a fresh product/system design review before any future clearing or clearing-batch task is reintroduced.

## 4. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
