# Tasks: P1 Settlement and Payout

> 2026-05-18 作废说明：本 change 已从当前 CAD 任务队列移除。结算、出款和相关清结算内容仅作为历史设计草稿保留，不作为下一轮实现、测试、外部集成或 DDL 的有效依据；如后续重新启动，必须重新设计并创建新的 OpenSpec change。

## 1. Change Scaffolding

- [x] Create `p1-settlement-payout` OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real Harness pipeline, no external payout.

## 2. Voided Historical Draft Scope

- [x] Mark the previous `SettlementOrder`, `SettlementLine`, `PayoutOrder`, `PayoutReceipt`, state machine, net settlement and traceability notes as historical draft only.
- [x] Remove the previous `clearing-reconciliation` and `transaction-layer` spec delta work from the effective task queue.
- [x] Keep clearing candidates, clearing batch, reconciliation exception, report, FX and archive work out of this voided change.

## 3. Voided Implementation Tasks

- [x] Void the previous implementation queue for `p1-settlement-payout`.
- [x] Remove `SettlementOrderServiceTests`, `PayoutResultServiceTests`, settlement/payout DDL, payout integration and receipt handling from the current effective backlog.
- [x] Require a fresh product/system design review before any future settlement, payout or external-funds task is reintroduced.

## 4. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Run `openspec validate p1-settlement-payout --strict` after this OpenSpec change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
