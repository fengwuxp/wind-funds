# Payment Ledger Spec Delta

## ADDED Requirements

### Requirement: P1 archive governance must protect balance rebuild

P1 archive, checkpoint, watermark and metric implementation MUST preserve immutable ledger facts and balance rebuild correctness.

#### Scenario: Archive implementation readiness review

- WHEN `BalanceCheckpoint`, `BalanceProjectionWatermark` or `ArchiveManifest` implementation is proposed
- THEN the change MUST define checkpoint granularity, watermark advancement order, archive precheck, digest inputs and failure handling
- AND it MUST prove watermark is advanced only after calculation, write and verification succeed
- AND DDL, archive movement or historical data migration MUST require manual approval

#### Scenario: Balance rebuild readiness review

- WHEN balance rebuild implementation is proposed
- THEN the change MUST use verified checkpoint or cold summary plus hot ledger entries after watermark
- AND it MUST fail when checkpoint, watermark, manifest or digest validation is missing
- AND it MUST NOT rebuild balances from transaction views, reports or current balance snapshots

### Requirement: P1 metrics must not mutate funds facts

P1 metric implementation MUST keep metric watermarks and snapshots independent from ledger and transaction facts.

#### Scenario: Metric implementation readiness review

- WHEN `MetricWatermark` or `MetricSnapshot` implementation is proposed
- THEN the change MUST define source window, metric version, dimensions, digest and recompute rule
- AND it MUST prove metric differences create alerts, differences or review tasks instead of changing ledger, wallet or transaction facts
