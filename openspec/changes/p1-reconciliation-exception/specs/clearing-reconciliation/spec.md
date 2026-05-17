# Clearing and Reconciliation Spec Delta

## ADDED Requirements

### Requirement: Reconciliation matching must create auditable exceptions

P1 reconciliation implementation MUST standardize internal and external records before matching and MUST create auditable exception facts for mismatches.

#### Scenario: Reconciliation mismatch creates exception

- WHEN internal facts and external records do not match by reference, amount, currency, direction, status or configured time window
- THEN the system MUST create a `ReconciliationException`
- AND it MUST preserve reconciliation batch number, difference type, responsible party when known, evidence reference and source record references
- AND it MUST NOT modify historical ledger entries, balance projections, funds transactions, clearing batches, settlement orders or payout orders

#### Scenario: Duplicate external success creates high-risk exception

- WHEN the same external reference appears as successful more than once
- THEN the system MUST create a `DUPLICATE_EXTERNAL_SUCCESS` exception
- AND it MUST block the related payout, settlement or subject-period scope when configured
- AND it MUST NOT auto-close or overwrite the earlier matched item

### Requirement: Reconciliation blocking must be scoped and releasable

P1 reconciliation blocking implementation MUST block only the affected scope and MUST define release conditions.

#### Scenario: Blocking affected settlement or payout

- GIVEN a high-risk reconciliation exception affects a settlement order, payout order, transaction or subject-period
- WHEN the exception hits blocking rules
- THEN the system MUST create a blocking record with explicit scope
- AND downstream clearing, settlement, payout or automatic adjustment MUST respect that blocking record
- AND unrelated subjects, currencies or periods MUST NOT be blocked

#### Scenario: Releasing block after resolution

- GIVEN a blocking reconciliation exception has been resolved or matched later
- WHEN release conditions are satisfied and approval is recorded if required
- THEN the system MUST release only the affected blocking scope
- AND it MUST preserve before and after status, reason, operator and evidence

### Requirement: Reconciliation exception handling must use new source facts for money changes

P1 reconciliation exception handling MUST create new auditable source facts for money-changing actions.

#### Scenario: Exception adjustment requires approval

- WHEN a reconciliation exception is resolved by supplement posting, reversal, balance adjustment, suspense claim, return funds or write-off
- THEN the action MUST require approval before money-changing execution
- AND the resulting funds instruction MUST reference source fact type `RECONCILIATION_EXCEPTION`
- AND it MUST preserve responsible party, reason, evidence reference, approval reference and before/after values
- AND it MUST NOT mutate historical ledger entries or balances in place

#### Scenario: Manual close is non-money-changing

- WHEN an authorized operator manually closes a false positive or no-action reconciliation exception
- THEN the system MUST preserve close reason, operator and evidence
- AND it MUST NOT create route legs, posting plans, ledger transactions or balance changes
