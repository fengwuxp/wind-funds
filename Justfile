set shell := ["zsh", "-uc"]

java_home := env_var_or_default("WIND_FUNDS_JAVA_HOME", env_var_or_default("JAVA_HOME", ""))
test_flags := "-Dmaven.test.skip=false -DskipTests=false -Dsurefire.skip=false -DskipITs=false -Dsurefire.failIfNoSpecifiedTests=false"

# List available project commands.
default:
    @just --list

# Show Maven and Java runtime.
mvn-version:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -version

# Compile the full Maven reactor.
compile:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn compile

# Run PMD checks.
pmd:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn pmd:check

# Run PMD checks and force Maven to refresh snapshots.
pmd-update:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -U pmd:check

# Run one test class in a Maven module. Defaults to the tests module.
test-one tests module='tests':
    @just _run-test-classes "{{tests}}" {{module}}

# Minimal CAD loop for one implementation slice.
verify-slice tests module='tests': mvn-version compile
    @just _run-test-classes "{{tests}}" {{module}}

_assert-test-classes tests:
    @tests="{{tests}}"; missing=0; for test in ${(s:,:)tests}; do test_class="${test%%#*}"; found=$(find . -path "*/src/test/java/*/${test_class}.java" -print -quit); if [[ -z "$found" ]]; then echo "Missing test class: $test_class"; missing=1; fi; done; exit $missing

_assert-surefire-reports tests marker:
    @tests="{{tests}}"; marker="{{marker}}"; failed=0; for test in ${(s:,:)tests}; do test_class="${test%%#*}"; reports=$(find . -path "*/target/surefire-reports/TEST-*${test_class}.xml" -newer "$marker" -print); if [[ -z "$reports" ]]; then echo "Missing fresh surefire report: $test_class"; failed=1; continue; fi; for report in ${(f)reports}; do if grep -Eq 'errors="[1-9][0-9]*"|failures="[1-9][0-9]*"' "$report"; then echo "Failed surefire report: $report"; failed=1; fi; done; done; exit $failed

_run-test-classes tests module='tests':
    @just _assert-test-classes "{{tests}}"
    @marker=$(mktemp /tmp/wind-funds-tests.XXXXXX); touch "$marker"; if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl {{module}} -am test -Dtest="{{tests}}" {{test_flags}}; mvn_status=$?; just _assert-surefire-reports "{{tests}}" "$marker"; report_status=$?; rm -f "$marker"; if (( mvn_status != 0 || report_status != 0 )); then exit 1; fi

# Run all tests in one Maven module.
test-module module='tests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl {{module}} -am test {{test_flags}}

# Core DSL and contract tests.
test-core tests='FundsInstructionDslContractTests,RouteDslContractTests,FundsDslJsonContractTests,FundsBenefitSnapshotSpecTests,PaymentInstrumentRouteDslContractTests,PostingLedgerDslContractTests,SettlementPolicySpecTests,FundsAmountBoundaryContractTests':
    @just _run-test-classes "{{tests}}" tests

# Ledger assembly, transaction, posting, and projection tests.
test-ledger tests='DefaultLedgerPostingAssemblerTests,LedgerBalanceProjectionServiceImplTests,LedgerServiceImplTests,LedgerTransactionServiceImplTests':
    @just _run-test-classes "{{tests}}" tests

# Transaction orchestration and lifecycle tests.
test-transaction tests='FundsDirectTransactionFlowTests,FundsAuthorizationTransactionFlowTests,FundsTransactionFeeFlowTests,DefaultRoutedFundsInstructionOrchestratorProjectionTests,FundsStableHashSupportTests,RouteSnapshotJsonSupportTests':
    @just _run-test-classes "{{tests}}" tests

# Frozen order and balance-control route tests.
test-balance-control tests='FundsBalanceControlFailureFlowTests,FundsWithdrawalSuccessFlowTests,FundsWithdrawalAfterPartialUnfreezeFlowTests,FundsFrozenOrderServiceImplTests':
    @just _run-test-classes "{{tests}}" tests

# Business flow and balance assertion tests.
test-business-flow tests='FundsDirectTransactionFlowTests,FundsAuthorizationTransactionFlowTests,FundsWithdrawalSuccessFlowTests,FundsWithdrawalRejectionFlowTests,FundsTransferPayWithdrawChainFlowTests,FundsTransactionFeeFlowTests,FundsBalanceAssertionSupportTests':
    @just _run-test-classes "{{tests}}" tests

# Contract, route, wallet, and module dependency boundary tests.
test-boundary tests='FundsContextVariablesContractTests,FundsTransactionParticipantContractTests,FundsTransactionRequestContextVariablesContractTests,LedgerBalanceChangedEventContractTests,LedgerDtoContextVariablesContractTests,LedgerRequestContextVariablesContractTests,SensitiveContextVariablesValidatorTests,RouteDslContractTests,PaymentInstrumentRouteDslContractTests,PostingLedgerDslContractTests,DefaultRouteReplayServiceTests,CompositeRouteResolverTests,RouteResolverFactBoundaryTests,FundingAccountServiceImplTests,ControlAccountLedgerInitializationTests,PaymentInstrumentServiceImplTests,SpendSubjectFundingRelationServiceImplTests,PlatformFundingAccountServiceImplTests,FundsSubjectBalanceQueryServiceImplTests,WalletLayerBoundaryTests,FundsModuleDependencyBoundaryTests':
    @just _run-test-classes "{{tests}}" tests

# Governance projection replay boundary tests.
test-governance tests='FundsProjectionReplayServiceTests':
    @just _run-test-classes "{{tests}}" tests

# Reconciliation and payout preflight tests.
test-reconciliation tests='PayoutPreflightServiceTests':
    @just _run-test-classes "{{tests}}" tests

# Fast CAD verification for non-business tooling or test-asset changes.
verify-fast: mvn-version compile test-boundary test-governance test-reconciliation

# Full CAD verification for the rebuilt payment funds test baseline.
verify-cad: mvn-version compile test-core test-ledger test-transaction test-balance-control test-business-flow test-boundary test-governance test-reconciliation pmd

# Install reactor snapshots locally when Maven plugin resolution needs local artifacts.
install-snapshots:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn install -DskipTests -Dmaven.test.skip=true
