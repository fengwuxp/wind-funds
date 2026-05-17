# Tasks: P1 Reconciliation Exception

> 2026-05-18 作废说明：本 change 已从当前 CAD 任务队列移除。对账、差错、阻断和差错调账相关内容仅作为历史设计草稿保留，不作为下一轮实现、测试、外部文件接入或 DDL 的有效依据；如后续重新启动，必须重新设计并创建新的 OpenSpec change。

## 1. Change Scaffolding

- [x] Create `p1-reconciliation-exception` OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real Harness pipeline, no external file integration and no adjustment execution.

## 2. Voided Historical Draft Scope

- [x] Mark the previous `ReconciliationBatch`, `ReconciliationItem`, `ReconciliationException`, `BlockingRule`, evidence and exception state-machine notes as historical draft only.
- [x] Remove the previous `clearing-reconciliation` and `transaction-layer` spec delta work from the effective task queue.
- [x] Keep clearing batch, settlement payout, report, FX and archive work out of this voided change.

## 3. Voided Implementation Tasks

- [x] Void the previous implementation queue for `p1-reconciliation-exception`.
- [x] Remove `ReconciliationMatchingServiceTests`, `ReconciliationExceptionAdjustmentTests`, reconciliation DDL, external file integration and exception adjustment from the current effective backlog.
- [x] Require a fresh product/system design review before any future reconciliation, exception or adjustment task is reintroduced.

## 4. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Run `openspec validate p1-reconciliation-exception --strict` after this OpenSpec change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
