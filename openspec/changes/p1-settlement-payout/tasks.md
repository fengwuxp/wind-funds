# Tasks: P1 Settlement and Payout

## 1. Change Scaffolding

- [x] Create `p1-settlement-payout` OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real Harness pipeline, no external payout.

## 2. Product and System Boundary

- [x] Define `SettlementOrder`, `SettlementLine`, `PayoutOrder` and `PayoutReceipt` semantics.
- [x] Define settlement order and payout order state machines.
- [x] Define net settlement amount formula, settlement idempotency key and traceability fields.
- [x] Define settlement lock, payout success and payout failure restore transaction boundaries.
- [x] Clarify external acceptance is not payout success and receipt mismatch enters review or later exception handling.

## 3. Spec Delta

- [x] Add `clearing-reconciliation` requirements for settlement locking, payout success, payout failure restore and receipt mismatch.
- [x] Add `transaction-layer` requirement for `SETTLEMENT_ORDER` and `PAYOUT_ORDER` source fact references.
- [x] Keep clearing candidates, clearing batch, reconciliation exception, report, FX and archive work out of this change.

## 4. Future Implementation Tasks

- [ ] Before coding, confirm Execution Grant for `p1-settlement-payout` implementation.
- [ ] Before coding, create failing `SettlementOrderServiceTests` covering net formula, lock success, insufficient balance, idempotency and duplicate lock rejection.
- [ ] Before coding, create failing `PayoutResultServiceTests` covering external acceptance, success receipt, receipt mismatch, failure restore and duplicate restore.
- [ ] Before DDL, provide table design, unique constraints, migration and rollback plan for manual approval.
- [ ] Before payout integration, provide channel/bank boundary, credential strategy, receipt verification and retry/failure semantics for manual approval.
- [ ] After implementation, run focused settlement and payout tests, transaction tests, compile and PMD according to the final write scope.

## 5. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Run `openspec validate p1-settlement-payout --strict` after this OpenSpec change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
