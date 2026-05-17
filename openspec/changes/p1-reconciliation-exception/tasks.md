# Tasks: P1 Reconciliation Exception

## 1. Change Scaffolding

- [x] Create `p1-reconciliation-exception` OpenSpec change.
- [x] Define proposal, design and task boundaries.
- [x] Keep this change documentation-only: no Java code, no DDL, no real Harness pipeline, no external file integration and no adjustment execution.

## 2. Product and System Boundary

- [x] Define `ReconciliationBatch`, `ReconciliationItem`, `ReconciliationException`, `BlockingRule` and `ExceptionEvidence` semantics.
- [x] Define reconciliation batch flow, matching dimensions and exception types.
- [x] Define exception state machine, blocking scopes and release conditions.
- [x] Define action boundary for supplement, reverse, adjustment, suspense, claim, return and write-off.
- [x] Clarify exception handling must not modify historical ledger, balance projection or transaction facts.

## 3. Spec Delta

- [x] Add `clearing-reconciliation` requirements for reconciliation matching, blocking controls and exception handling.
- [x] Add `transaction-layer` requirement for `RECONCILIATION_EXCEPTION` source fact reference.
- [x] Keep clearing batch, settlement payout, report, FX and archive work out of this change.

## 4. Future Implementation Tasks

- [ ] Before coding, confirm Execution Grant for `p1-reconciliation-exception` implementation.
- [ ] Before coding, create failing `ReconciliationMatchingServiceTests` covering standardization, duplicate external success, single-side records, amount/currency mismatch and later matching.
- [ ] Before coding, create failing `ReconciliationExceptionAdjustmentTests` covering approval, source fact mapping, no historical mutation and block release.
- [ ] Before DDL, provide table design, unique constraints, migration and rollback plan for manual approval.
- [ ] Before external file integration, provide file schema, signature/checksum, retention, desensitization and retry/failure semantics for manual approval.
- [ ] Before any adjustment implementation, provide funds instruction mapping, route event mapping, rollback/compensation plan and finance approval evidence.
- [ ] After implementation, run focused reconciliation tests, transaction tests, compile and PMD according to the final write scope.

## 5. Validation

- [x] Run `git diff --check` after this documentation-only change.
- [x] Run `openspec validate p1-reconciliation-exception --strict` after this OpenSpec change.
- [x] Do not run compile for documentation-only changes; record reason in delivery summary.
