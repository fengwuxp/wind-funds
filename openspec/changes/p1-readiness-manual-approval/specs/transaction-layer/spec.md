# Transaction Layer Spec Delta

## ADDED Requirements

### Requirement: P1 source facts must not be replaced by business serial numbers

P1 transaction-layer integration MUST distinguish business identity, reference transactions and independently modeled funds source facts.

#### Scenario: P1 source fact readiness review

- WHEN clearing, settlement, payout, reconciliation exception, FX execution, archive or replay tasks initiate a funds action
- THEN the change MUST define the source fact type, source fact serial number, source fact version and audit reference
- AND it MUST explain how this differs from `businessScene/businessSn` and transaction `reference`
- AND it MUST NOT restore `sourceObjectType/sourceObjectSn` as two unbounded generic fields

### Requirement: P1 FX operations must not reintroduce transaction-layer auto FX

P1 FX operations MUST keep FX decision outside transaction converters and route resolvers.

#### Scenario: FX execution readiness review

- WHEN FX quote, lock or execution implementation is proposed
- THEN the change MUST define quote source, quote expiry, confirmation, execution result, fees and gain/loss facts
- AND transaction-layer converters MUST continue to accept only explicit business or FX-domain amount facts
- AND wrong-currency transaction requests without explicit FX decision MUST fail or enter a separately modeled exception path

### Requirement: P1 transaction view replay must stay separate from route replay

P1 transaction view replay MUST repair read models only and MUST NOT reuse route replay as an accounting mechanism.

#### Scenario: Transaction view replay readiness review

- WHEN transaction view replay implementation is proposed
- THEN the change MUST define tenant, projection code, bounded range, mode and idempotency key
- AND `VERIFY_ONLY` or shadow mode MUST be available before formal apply mode
- AND replay MUST NOT create route legs, posting plans, ledger entries, funds transactions or frozen orders
