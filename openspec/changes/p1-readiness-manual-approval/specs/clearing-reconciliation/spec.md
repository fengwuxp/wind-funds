# Clearing and Reconciliation Spec Delta

## ADDED Requirements

### Requirement: Voided clearing, settlement, reconciliation and reporting tasks must not be treated as active backlog

Clearing, settlement, payout, reconciliation exception and reporting work MUST remain historical draft material until a fresh product/system design and a new OpenSpec change are created.

#### Scenario: Current readiness queue is inspected

- WHEN the current CAD task queue is derived from this change
- THEN clearing candidates, clearing batches, settlement orders, payout orders, reconciliation exceptions and report snapshots MUST NOT appear as actionable implementation tasks
- AND their previous test names, DDL work, external integrations and batch jobs MUST be treated as removed from the effective backlog

#### Scenario: Future clearing or reconciliation work is proposed

- WHEN clearing, settlement, payout, reconciliation or reporting work is requested again
- THEN the future change MUST redefine product semantics, source facts, state machines, ledger impact, tests and manual approval gates
- AND it MUST NOT reuse this readiness change as direct implementation authorization
