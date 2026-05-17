# Transaction Layer Spec Delta

## ADDED Requirements

### Requirement: Transaction projection must remain a read model

Transaction projection MUST be a read model and MUST NOT be used as an accounting, routing or balance-correction mechanism.

#### Scenario: Transaction projection boundary is reviewed

- WHEN transaction projection design or implementation is proposed
- THEN the change MUST define source facts, projection code, minimal fields, authorization boundary, idempotency key and audit references
- AND it MUST rebuild from funds transaction facts, lifecycle facts, frozen-order facts, route snapshots and ledger references
- AND it MUST NOT rebuild balances from transaction view
- AND it MUST NOT write ledger entries, posting plans, route legs, funds transactions, frozen orders or balance projections

### Requirement: Transaction view replay must stay separate from route replay

Transaction view replay MUST repair or verify read models only and MUST NOT reuse route replay as an accounting mechanism.

#### Scenario: Transaction view replay readiness review

- WHEN transaction view replay implementation is proposed
- THEN the change MUST define tenant, projection code, bounded range, mode, cursor and idempotency key
- AND `VERIFY_ONLY` or shadow mode MUST be available before formal apply mode
- AND replay MUST NOT create route legs, posting plans, ledger entries, funds transactions or frozen orders

### Requirement: Deferred source facts must not be replaced by business serial numbers

Future independently modeled source facts MUST remain distinct from business identity and transaction references.

#### Scenario: Future source fact design is proposed

- WHEN clearing, settlement, payout, reconciliation exception, FX execution, archive or replay tasks initiate a funds action in a future change
- THEN the change MUST define the source fact type, source fact serial number, source fact version and audit reference
- AND it MUST explain how this differs from `businessScene/businessSn` and transaction `reference`
- AND it MUST NOT restore `sourceObjectType/sourceObjectSn` as two unbounded generic fields
