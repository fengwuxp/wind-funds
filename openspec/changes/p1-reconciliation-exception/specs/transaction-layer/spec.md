# Transaction Layer Spec Delta

## ADDED Requirements

### Requirement: Reconciliation exception adjustments must use reconciliation exception as the source fact

Future P1 reconciliation exception integration MUST use the exception as the independent funds source fact when a handling action creates money movement.

#### Scenario: Reconciliation exception funds instruction

- WHEN supplement posting, reversal, balance adjustment, suspense claim, return funds or write-off initiates a funds instruction from a reconciliation exception
- THEN the instruction MUST reference source fact type `RECONCILIATION_EXCEPTION`
- AND source fact number MUST be the reconciliation exception number
- AND source fact version MUST be the handling action version
- AND the instruction MUST preserve responsible party, reason, evidence reference and approval reference
- AND `businessScene/businessSn` MUST identify the current handling action only
- AND `ReconciliationBatch`, `ReconciliationItem`, `BlockingRule` and `ExceptionEvidence` MUST NOT be used as posting source facts
- AND the design MUST NOT reintroduce unbounded `sourceObjectType/sourceObjectSn` fields
