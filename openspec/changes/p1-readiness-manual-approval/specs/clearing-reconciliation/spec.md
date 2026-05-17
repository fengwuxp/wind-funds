# Clearing and Reconciliation Spec Delta

## ADDED Requirements

### Requirement: P1 clearing readiness must pass manual approval gates

P1 clearing and reconciliation implementation MUST NOT begin until the specific future change defines objects, state machines, idempotency, audit, tests and manual approval triggers.

#### Scenario: Clearing implementation readiness review

- WHEN clearing candidate or clearing batch implementation is proposed
- THEN the change MUST define candidate source data, exclusion reasons, batch version, rerun rule and confirmation posting event
- AND it MUST define duplicate-clearing rejection tests
- AND it MUST state whether DDL is required
- AND DDL or confirmation posting MUST require manual approval before implementation

#### Scenario: Settlement and payout implementation readiness review

- WHEN settlement order or payout order implementation is proposed
- THEN the change MUST define settlement net amount formula, approval state, payout evidence, success criteria and failure rollback rule
- AND it MUST prove external acceptance is not treated as payout success
- AND it MUST require manual approval for payout-related ledger effects or DDL

#### Scenario: Reconciliation exception implementation readiness review

- WHEN reconciliation matching or exception adjustment implementation is proposed
- THEN the change MUST define data sources, matching dimensions, exception types, blocking scopes, release conditions and adjustment audit fields
- AND it MUST prove reconciliation differences do not directly modify historical ledger entries or balances
- AND adjustment or blocking-rule implementation MUST require manual approval

### Requirement: P1 reports must remain derived outputs

P1 reporting implementation MUST keep reports derived from facts, batches and reconciliation results.

#### Scenario: Report implementation readiness review

- WHEN report snapshot, statement or finance report implementation is proposed
- THEN the change MUST define source facts, metric口径, versioning and recompute rule
- AND it MUST prove report recomputation does not write ledger, wallet, transaction, clearing or reconciliation facts
