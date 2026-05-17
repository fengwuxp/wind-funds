# Clearing and Reconciliation Spec Delta

## ADDED Requirements

### Requirement: Settlement lock must reserve payout amount before external payout

P1 settlement implementation MUST lock payable amount from merchant `AVAILABLE` into merchant `SETTLEMENT` before any external payout is submitted.

#### Scenario: Settlement lock succeeds

- GIVEN an approved settlement order with sufficient merchant `AVAILABLE`
- WHEN settlement lock is executed
- THEN the system MUST create a `MERCHANT_SETTLEMENT_LOCK` ledger posting
- AND it MUST move the settlement net amount from merchant `AVAILABLE` to merchant `SETTLEMENT`
- AND it MUST preserve settlement order number, order version, policy code, policy version, operator and approval reference
- AND it MUST create or link a payout order for the locked amount

#### Scenario: Settlement lock rejects insufficient balance

- GIVEN an approved settlement order whose net amount exceeds merchant `AVAILABLE`
- WHEN settlement lock is executed
- THEN the system MUST reject the lock
- AND it MUST NOT create a payout order
- AND it MUST NOT change merchant `AVAILABLE` or `SETTLEMENT`

### Requirement: External payout acceptance must not be treated as payout success

P1 payout implementation MUST distinguish external acceptance from actual payout success.

#### Scenario: External payout accepted

- WHEN a payout request is accepted or submitted by an external channel
- THEN the payout order MAY move to `SUBMITTED`
- AND it MUST NOT create `MERCHANT_PAYOUT_SUCCESS`
- AND it MUST NOT consume merchant `SETTLEMENT`

#### Scenario: Payout success receipt

- GIVEN a payout order with locked settlement amount
- WHEN a trusted receipt confirms success with matching amount, currency, receiving account and external reference
- THEN the system MUST create a `MERCHANT_PAYOUT_SUCCESS` ledger posting
- AND it MUST consume merchant `SETTLEMENT` according to the payout path
- AND it MUST preserve receipt reference, external reference and verification result

### Requirement: Payout failure restore must be single-use and receipt-driven

P1 payout failure restore MUST release locked amount only when the failure or return result is trustworthy.

#### Scenario: Payout failure restore succeeds

- GIVEN a payout order with locked settlement amount and trustworthy failure or return result
- WHEN failure restore is executed
- THEN the system MUST create a `MERCHANT_PAYOUT_FAIL_RESTORE` ledger posting
- AND it MUST move the original locked amount from merchant `SETTLEMENT` back to merchant `AVAILABLE`
- AND it MUST preserve failure reason, receipt reference, operator and audit data

#### Scenario: Duplicate payout failure restore

- GIVEN a payout order has already restored its locked amount
- WHEN the same failure restore is retried
- THEN the system MUST return the original restore result or reject the duplicate action
- AND it MUST NOT release merchant `SETTLEMENT` more than once

#### Scenario: Payout receipt mismatch

- WHEN payout receipt amount, currency, receiving account or external reference does not match the payout order
- THEN the payout order MUST enter manual review or a separately modeled reconciliation exception path
- AND it MUST NOT be marked succeeded
- AND it MUST NOT automatically restore locked amount
