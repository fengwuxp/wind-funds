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
- [x] Execute P0-B wind-funds / DSL contract tasks.
  - [x] Close RouteReplay naming, DSL JSON fixtures, digest contract and instruction type naming first pass.
  - [x] Keep source fact boundary as regression protection until clearing, dispute and reconciliation facts are independently modeled.
- [x] Execute P0-C Ledger Posting main-chain tasks.
  - [x] Close first-pass posting/projection behavior: positive amounts, postable subjects, route-leg traceability, idempotent no-reprojection, missing-ledger failure, missing-balance-bucket failure, and read-side uninitialized bucket semantics.
  - [x] Rename `LedgerTransactionCreateResult` to the posting result main contract with a compatibility alias.
- [x] Execute P0-D Transaction Layer service facade tasks.
- [x] Execute P0-E Wallets account and balance-control tasks.
- [x] Execute P0-F Route/helper/Fx convergence tasks.
- [x] Execute P0-G naming governance tasks.
  - [x] Introduce `LedgerTransactionPostResult` as the ledger posting result main contract with `LedgerTransactionCreateResult` retained as a deprecated compatibility alias.
  - [x] Introduce `FundsInstructionLifecycleRecorder` as the lifecycle write-side main contract with `FundsInstructionLifecycleSaver` retained as a deprecated compatibility alias.
  - [x] Introduce `DelegatingFundsInstructionLifecycleRecorder` as the composite lifecycle-dispatch main implementation with `CompositeFundsInstructionLifecycleSaver` retained as a deprecated compatibility alias.
  - [x] Confirm account-type semantic axes through `ExternalFundsAccountType`, `FundingAccountType`, `UserWalletFundsAccountType`, `CreditFundsAccountType`, `FundsSubjectType` and `PlatformFundingAccountRole`, with `DefaultFundsAccountType` retained only as a compatibility umbrella.
  - [x] Reclassify `PREPAID_CARD` as a funding account product and `REBATE_ACCOUNT` as a rebate-liability funding account; keep only `SHARED_CARD` and `CREDIT_CARD` in the credit account axis.
- [ ] Execute P0-H test asset governance tasks.
  - [x] Align transaction service facade tests and shared support to `com.capte.funds.transaction.application`.
  - [x] Align routed instruction orchestration tests and shared support to `com.capte.funds.transaction.application.orchestration`.
  - [x] Align transaction API and participant contract tests to `com.capte.funds.transaction.contract`.
  - [x] Align transaction boundary tests to `com.capte.funds.transaction.boundary`.
  - [x] Align transaction accounting semantics tests to `com.capte.funds.transaction.accounting`.
  - [x] Align remaining transaction flow tests to `com.capte.funds.transaction.application.flow` without business logic changes.
  - [x] Align transaction ledger assertion tests to `com.capte.funds.transaction.ledger` and shared transaction test support to `com.capte.funds.support`.

## 8. 2026-05-15 CR Calibration

- [x] Document Wallets as account capability layer rather than transaction-command layer.
- [x] Document that current PMD dependency-resolution issue is non-blocking for the active CAD/Harness rounds.
- [x] Move account, balance-query, ledger-profile, platform-role and payment-instrument contracts into `wallet-face`.
- [x] Decide that corresponding account capability implementations and DAL belong in `wallet-impl`, not transaction-impl adapters.
- [x] Move corresponding account capability implementations, DAL, Mapper and Converter into `wallet-impl`.
- [x] Review and batch-fix module, package, class and method names according to P0-G.
- [x] Review and batch-fix test method naming and key scenario comments according to P0-H first pass.
- [ ] Review balance assertions and split large test classes according to P0-H follow-up.
  - [x] Split ledger posting validation scenarios from `DefaultLedgerTransactionPostingServiceImplTests` into `DefaultLedgerTransactionPostingValidationTests` with shared `LedgerTransactionPostingTestSupport`.
  - [x] Split posting plan structure validation scenarios from `DefaultLedgerTransactionPostingValidationTests` into `DefaultLedgerPostingPlanValidationTests`.
  - [x] Split ledger-entry amount, ledger binding, profile and subject-type validation scenarios from `DefaultLedgerTransactionPostingServiceImplTests` into `DefaultLedgerEntryValidationTests`.
  - [x] Split ledger posting assembler period lookup scenarios from `DefaultLedgerPostingAssemblerTests` into `DefaultLedgerPostingAssemblerPeriodTests` with shared `DefaultLedgerPostingAssemblerTestSupport`.
  - [x] Split route replay orchestration scenarios from `DefaultRoutedFundsInstructionOrchestratorTests` into `DefaultRoutedFundsInstructionOrchestratorReplayTests` with shared `DefaultRoutedFundsInstructionOrchestratorTestSupport`.
  - [x] Split authorization and balance-control command scenarios from `FundsTransactionCommandServiceImplTests` into dedicated command service tests with shared `FundsTransactionCommandServiceImplTestSupport`.
  - [x] Split fee command scenarios from `FundsTransactionCommandServiceImplTests` into `FundsTransactionFeeCommandServiceImplTests` with shared `FundsTransactionCommandServiceImplTestSupport`.
  - [x] Split ledger transaction digest contract scenarios from `LedgerTransactionServiceImplTests` into `LedgerTransactionDigestContractTests` with shared `LedgerTransactionServiceImplTestSupport`.
  - [x] Split posting plan digest contract scenarios from `LedgerTransactionDigestContractTests` into `LedgerPostingPlanDigestContractTests`.
  - [x] Split funds account balance-query scenarios from `DefaultFundsAccountQueryServiceImplTests` into dedicated query tests with shared `DefaultFundsAccountQueryServiceImplTestSupport`.
  - [x] Split ledger profile required-item and controlled-negative policy scenarios from `DefaultLedgerProfileServiceImplTests` into `DefaultLedgerProfileRequiredItemTests`.
  - [x] Split ledger balance projection event, negative-constraint and validation scenarios from `LedgerBalanceProjectionServiceImplTests` into dedicated tests with shared `LedgerBalanceProjectionServiceImplTestSupport`.
  - [x] Split funds transaction route snapshot query scenarios from `DefaultFundsTransactionQueryServiceTests` into `DefaultFundsTransactionRouteSnapshotQueryTests` with shared `DefaultFundsTransactionQueryServiceTestSupport`.
  - [x] Split wallet layer boundary contract, instrument and implementation ownership scenarios from `WalletLayerBoundaryTests` into dedicated boundary tests with shared `WalletLayerBoundaryTestSupport`.
  - [x] Split funds instruction lifecycle before-posting and success-summary scenarios from `DefaultFundsInstructionLifecycleSaverTests` into dedicated lifecycle saver tests.
  - [x] Split route replay authorization, direct refund and replay-policy scenarios from `DefaultRouteReplayServiceTests` into dedicated route replay tests.
  - [x] Split fee business-flow scenarios from `FundsTransactionBusinessFlowIntegrationTests` into `FundsTransactionFeeBusinessFlowTests` with shared `FundsTransactionBusinessFlowTestSupport`.
  - [x] Split authorization business-flow scenarios from `FundsTransactionBusinessFlowIntegrationTests` into `FundsAuthorizationBusinessFlowTests` with shared `FundsTransactionBusinessFlowTestSupport`.
  - [x] Split replay orchestration-flow scenarios from `FundsTransactionOrchestrationFlowTests` into `FundsTransactionOrchestrationReplayFlowTests` with shared `FundsTransactionOrchestrationFlowTestSupport`.
  - [x] Split credit-account and budget-group `LIMIT_ADJUST` route scenarios from `BalanceControlFundsInstructionRouteResolverTests` into `BalanceControlLimitAdjustRouteResolverTests` with shared `BalanceControlFundsInstructionRouteResolverTestSupport`.
  - [x] Split subject ledger initializer validation scenarios from `DefaultSubjectLedgerInitializerTests` into `DefaultSubjectLedgerInitializerValidationTests` with shared `DefaultSubjectLedgerInitializerTestSupport`.
  - [x] Split frozen-order lifecycle validation scenarios from `DefaultFundsFrozenOrderLifecycleSaverTests` into `DefaultFundsFrozenOrderLifecycleValidationTests` with shared `DefaultFundsFrozenOrderLifecycleSaverTestSupport`.
  - [x] Split fee summary scenarios from `DefaultFundsInstructionLifecycleSuccessSummaryTests` into `DefaultFundsInstructionLifecycleFeeSummaryTests`.
  - [x] Split reference-transaction reuse boundary scenarios from `DefaultFundsInstructionLifecycleSaverIdempotencyTests` into `DefaultFundsInstructionLifecycleReferenceTransactionTests`.
  - [x] Split lifecycle recorder compatibility-alias scenario from `DelegatingFundsInstructionLifecycleRecorderTests` into `DelegatingFundsInstructionLifecycleRecorderCompatibilityTests`.

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
- [x] Resume P0-H test asset governance after P0-G.
- [x] Re-scope unfinished current-version tasks to transaction, wallet and ledger layers first.
- [x] Defer clearing, settlement, reconciliation, ledger account/archive, balance snapshot and full FX operation tasks to the next version.
- [x] Align the current-version service test matrix for `FundsDirectTransactionService`, `FundsAuthorizationTransactionService` and `FundsBalanceControlService`.
- [x] Define test package alignment rules for transaction application, business-flow, converter, route, wallet and ledger tests.
- [x] Implement missing current-version service facade unit tests for the three transaction services.
- [x] Implement missing current-version business-combination integration tests for direct transaction, authorization and balance-control flows.
- [x] Migrate test package names in small batches without mixing business-logic changes.
- [ ] Keep P1 clearing/reconciliation, FX operations and archive governance behind separate OpenSpec changes and Harness manual approval gates when they introduce new objects, DDL or high-risk behavior.
