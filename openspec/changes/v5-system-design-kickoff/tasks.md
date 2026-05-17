# Tasks: v5 System Design Kickoff

## 1. Collaboration Scaffolding

- [x] Create OpenSpec project context.
- [x] Create core capability specs for Ledger, Wallets, Transaction Layer, Clearing and Reconciliation.
- [x] Create system design kickoff proposal.
- [x] Create collaboration workbench under `docs/v5/系分设计`.
- [x] Create API design rules for later system design.
- [x] Create Harness verification gate design.
- [x] Create AI coding collaboration execution plan.

## 2. Product and DSL Traceability

- [x] Link `Ledger DSL Posting 系分设计.md` to PRD chapters.
- [x] Link `Ledger DSL Posting 系分设计.md` to DSL chapters.
- [x] Link `Ledger DSL Posting 系分设计.md` to OpenSpec requirements.
- [x] Link `Wallets 账户与余额控制系分设计.md` to PRD chapters.
- [x] Link `Wallets 账户与余额控制系分设计.md` to DSL chapters.
- [x] Link `Wallets 账户与余额控制系分设计.md` to OpenSpec requirements.
- [x] Link `交易层服务能力系分设计.md` to PRD chapters.
- [x] Link `交易层服务能力系分设计.md` to DSL chapters.
- [x] Link `交易层服务能力系分设计.md` to OpenSpec requirements.
- [x] Link `清结算与对账系分设计.md` to PRD chapters.
- [x] Link `清结算与对账系分设计.md` to DSL chapters.
- [x] Link `清结算与对账系分设计.md` to OpenSpec requirements.
- [x] Link `归档投影与指标治理系分设计.md` to PRD chapters.
- [x] Link `归档投影与指标治理系分设计.md` to DSL chapters.
- [x] Link `归档投影与指标治理系分设计.md` to previous system design documents.
- [x] Update DSL contract matrix when a new API or behavior is designed.

## 3. System Design Work Packages

- [x] Design Ledger / DSL / Posting module.
- [x] Design Wallets / Account / Balance Control module.
- [x] Design Transaction Layer service abilities.
- [x] Design Clearing / Reconciliation / Settlement module.
- [x] Design Archive / Projection / Metrics governance.

## 4. API and Contract Work Packages

- [x] Define face-layer service contracts and DTO / Request / Query models.
- [x] Define error codes, idempotency keys, validation rules and audit fields.
- [x] Define JSON contract fixtures for transaction-layer abilities.
- [x] Define OpenAPI or equivalent API review artifacts when Web API is involved.

## 5. Test and Harness Work Packages

- [x] Define unit test matrix for pure ledger and route rules.
- [x] Define application service tests for transaction orchestration.
- [x] Define contract tests for DSL JSON and service APIs.
- [x] Define integration tests for persistence and local transaction boundaries.
- [x] Define architecture tests for module dependency direction.
- [x] Map local verification commands to Harness stages.

## 6. v4 Code CR Carryover

- [x] Refactor or remove low-value long-parameter helpers such as `RouteBuildSupport`, `RouteReferenceFactory`, `FundsInstructionBuildSupport` and `RouteLegFactory`.
- [x] Close P0 Fx and currency-mismatch implementation gaps: transaction converters must not auto-call `FxService`; v5 transaction code paths use explicit `TransactionAmount` facts and reject wrong-currency requests without an FX decision.
- [x] Replace stable RouteResolver protocol literals with constants or enums.
- [x] Re-scope wallet-named transaction services into transaction-layer service abilities.
- [x] Add ledger balance assertions to all funds-changing core tests.
- [x] Add business-combination integration tests for topup-pay-refund, topup-freeze-withdraw and topup-transfer-pay-withdraw.

## 7. P0 Coding Task Split

- [x] Create P0 coding task split document.
- [x] Calibrate first AI collaboration wave and current P0 task status.
- [x] Create P0-A balance assertion and business-flow test implementation design.
- [x] Execute P0-A test protection and balance assertion tasks.
- [ ] Execute P0-B wind-funds / DSL contract tasks.
  - [x] Close RouteReplay naming, DSL JSON fixtures, digest contract and instruction type naming first pass.
  - [ ] Keep source fact boundary as regression protection until clearing, dispute and reconciliation facts are independently modeled.
- [ ] Execute P0-C Ledger Posting main-chain tasks.
  - [x] Close first-pass posting/projection behavior: positive amounts, postable subjects, route-leg traceability, idempotent no-reprojection, missing-ledger failure, missing-balance-bucket failure, and read-side uninitialized bucket semantics.
  - [x] Rename `LedgerTransactionCreateResult` to the posting result main contract with a compatibility alias.
- [x] Execute P0-D Transaction Layer service facade tasks.
- [x] Execute P0-E Wallets account and balance-control tasks.
- [x] Execute P0-F Route/helper/Fx convergence tasks.
- [x] Execute P0-G naming governance tasks.
  - [x] Introduce `LedgerTransactionPostResult` as the ledger posting result main contract with `LedgerTransactionCreateResult` retained as a deprecated compatibility alias.
  - [x] Introduce `FundsInstructionLifecycleRecorder` as the lifecycle write-side main contract with `FundsInstructionLifecycleSaver` retained as a deprecated compatibility alias.
  - [x] Introduce `DelegatingFundsInstructionLifecycleRecorder` as the composite lifecycle-dispatch main implementation with `CompositeFundsInstructionLifecycleSaver` retained as a deprecated compatibility alias.
  - [x] Confirm account-type semantic axes through `ExternalFundsAccountType`, `UserWalletFundsAccountType`, `CreditFundsAccountType`, `FundsSubjectType` and `PlatformFundingAccountRole`, with `DefaultFundsAccountType` retained only as a compatibility umbrella.
- [ ] Execute P0-H test asset governance tasks.

## 8. 2026-05-15 CR Calibration

- [x] Document Wallets as account capability layer rather than transaction-command layer.
- [x] Document that current PMD dependency-resolution issue is non-blocking for the active CAD/Harness rounds.
- [x] Move account, balance-query, ledger-profile, platform-role and payment-instrument contracts into `wallet-face`.
- [x] Decide that corresponding account capability implementations and DAL belong in `wallet-impl`, not transaction-impl adapters.
- [x] Move corresponding account capability implementations, DAL, Mapper and Converter into `wallet-impl`.
- [x] Review and batch-fix module, package, class and method names according to P0-G.
- [x] Review and batch-fix test method naming and key scenario comments according to P0-H first pass.
- [ ] Review balance assertions and split large test classes according to P0-H follow-up.

## 9. 2026-05-16 OpenSpec / Superpowers / Harness Replan

- [x] Close P0-R product-redline regression gate.
  - [x] Transaction converters no longer call `FxService` implicitly.
  - [x] `FundsBalanceControlService` does not support FX or wrong-currency balance control.
  - [x] Ledger balance projection publishes a derived balance-changed observation hook.
  - [x] `SettlementPolicySpec` supports non-RT expressions and fails unsupported expressions instead of falling back to `RT`.
- [x] Close Route replay and fee CR design in the implementation plan.
  - [x] Route replay remains a `RouteResolver` scenario selected by `supports(FundsInstructionSpec)`.
  - [x] Fee collection is initiated by transaction-layer requests through explicit `FeeSpec`, not by account-level fee lookup inside route resolution.
- [x] Align control-account adjustment design across PRD, ADR, DSL and API plan.
  - [x] `FundsBalanceControlService#adjust` covers funding balance adjustment, credit limit adjustment and budget limit adjustment.
  - [x] `LIMIT` is allowed only in `BALANCE_CONTROL / LIMIT_ADJUST`, not as a normal source or target in ordinary transactions.
- [x] Execute P0-CTRL control-account adjustment implementation.
  - [x] Add credit limit increase/decrease service facade tests.
  - [x] Add budget increase/decrease service facade tests.
  - [x] Add `LIMIT` redline tests for ordinary transactions, authorization settlement, refund, chargeback and fee scenarios.
  - [x] Apply minimal request validation, converter, resolver or context changes only after failing tests expose the gap.
- [x] Resume P0-E Wallets account and balance-control follow-up after P0-CTRL.
  - [x] Add `ControlAccountLedgerRulesTests` for CREDIT/BUDGET control buckets and no ledger `CONSUMED` bucket.
- [x] Resume P0-G naming governance after P0-E.
- [ ] Resume P0-H test asset governance after P0-G.
- [ ] Keep P1 clearing/reconciliation, FX operations and archive governance behind separate OpenSpec changes and Harness manual approval gates when they introduce new objects, DDL or high-risk behavior.
