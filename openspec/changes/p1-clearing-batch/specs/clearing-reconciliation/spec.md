# Clearing and Reconciliation Spec Delta

## ADDED Requirements

### Requirement: Clearing candidate generation must be read-only and idempotent

P1 clearing candidate generation MUST create a versioned candidate result without posting ledger entries or changing account balances.

#### Scenario: Candidate generation idempotency

- WHEN candidate generation is requested for the same tenant, settlement subject, currency, settlement period, policy version, candidate version and data source version
- THEN the system MUST return the same candidate result
- AND it MUST NOT duplicate candidate items
- AND it MUST NOT create route legs, posting plans, ledger transactions or balance changes

#### Scenario: Candidate exclusion reason

- WHEN a transaction detail cannot enter the current clearing batch
- THEN the candidate result MUST preserve an auditable exclusion reason
- AND blocked, already-cleared, risk-held or policy-mismatched items MUST NOT contribute to confirmable amount

### Requirement: Clearing batch confirmation must be duplicate-safe and traceable

P1 clearing batch confirmation MUST move merchant funds from `CLEARING` to `AVAILABLE` exactly once for each confirmed batch.

#### Scenario: Clearing batch confirmation

- GIVEN a checked or approved clearing batch with confirmable items
- WHEN the batch is confirmed
- THEN the system MUST create a `MERCHANT_CLEARING_COMPLETE` ledger posting
- AND it MUST move the confirmed amount from merchant `CLEARING` to merchant `AVAILABLE`
- AND it MUST preserve clearing batch number, batch version, policy code, policy version, operator and approval reference
- AND each confirmed item MUST be linked to the ledger transaction reference

#### Scenario: Duplicate confirmation idempotency

- GIVEN a clearing batch has already been confirmed
- WHEN the same confirmation request with the same idempotency key is retried
- THEN the system MUST return the original confirmation result
- AND it MUST NOT create another ledger transaction or balance projection

#### Scenario: Duplicate confirmation rejection

- GIVEN a clearing batch is already in a terminal confirmed state
- WHEN a different confirmation request attempts to confirm the same batch again
- THEN the system MUST reject the request
- AND it MUST NOT create another ledger transaction or balance projection

### Requirement: Clearing rerun must version results instead of overwriting history

P1 clearing rerun MUST preserve historical candidate and batch results.

#### Scenario: Rerun after source or rule change

- WHEN candidate source data, policy version or blocking result changes after a previous candidate version or confirmed batch
- THEN the system MUST create a new candidate version or follow-up clearing batch
- AND it MUST NOT overwrite confirmed batch amount, item list, policy snapshot or ledger transaction reference
- AND any adjustment or补差 MUST use a separately approved source fact
