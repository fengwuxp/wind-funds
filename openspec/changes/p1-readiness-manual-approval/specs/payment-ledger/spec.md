# Payment Ledger Spec Delta

## ADDED Requirements

### Requirement: Balance projection re-check must preserve ledger facts as the source of truth

Balance projection MUST remain derived from ledger facts and MUST NOT be rebuilt or corrected from transaction views, reports, statements or business balance-change logs.

#### Scenario: Balance projection boundary is reviewed

- WHEN balance projection design or implementation is changed
- THEN the change MUST prove the projection is derived from `LedgerTransaction`, `LedgerEntry`, ledger profile, normal balance and controlled-negative rules
- AND `LedgerBalanceChangedEvent` or any business balance-change log MUST remain observational output only
- AND event or log failure MUST NOT roll back a successfully validated balance projection

#### Scenario: Future checkpoint, watermark or rebuild work is proposed

- WHEN `BalanceCheckpoint`, `BalanceProjectionWatermark`, `ArchiveManifest` or balance rebuild implementation is proposed
- THEN the future change MUST define checkpoint granularity, watermark advancement order, digest inputs, verify-only behavior, failure handling and rollback strategy
- AND DDL, archive movement, formal rebuild or historical data migration MUST require a separate OpenSpec change and manual approval

### Requirement: Metric and reporting governance must not be bundled with balance projection

Metric, report and finance-view governance MUST NOT be implemented as part of the current balance projection re-check.

#### Scenario: Metric or report implementation is proposed

- WHEN metric watermark, report snapshot, finance report or report recompute work is requested
- THEN it MUST be redirected to a fresh product/system design and OpenSpec change
- AND it MUST NOT mutate ledger, wallet, transaction or balance projection facts
