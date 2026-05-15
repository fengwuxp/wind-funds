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
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl {{module}} -am test -Dtest={{tests}} {{test_flags}}

# Run all tests in one Maven module.
test-module module='tests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl {{module}} -am test {{test_flags}}

# Core DSL and contract tests.
test-core tests='FundsInstructionSpecContractTests,RouteDslContractTests,TransactionServiceAbilityDslJsonContractTests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl core -am test -Dtest={{tests}} {{test_flags}}

# Ledger assembly, posting, and projection tests.
test-ledger tests='DefaultLedgerPostingAssemblerTests,DefaultLedgerTransactionPostingServiceImplTests,LedgerBalanceProjectionServiceImplTests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl tests -am test -Dtest={{tests}} {{test_flags}}

# Transaction orchestration and lifecycle tests.
test-transaction tests='FundsTransactionCommandServiceImplTests,DefaultRoutedFundsInstructionOrchestratorTests,DefaultFundsInstructionLifecycleSaverTests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl tests -am test -Dtest={{tests}} {{test_flags}}

# Frozen order and balance-control route tests.
test-balance-control tests='FundsFrozenOrderServiceImplTests,DefaultFundsFrozenOrderLifecycleSaverTests,BalanceControlFundsInstructionRouteResolverTests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl tests -am test -Dtest={{tests}} {{test_flags}}

# Business flow and balance assertion tests.
test-business-flow tests='FundsTransactionLedgerBalanceAssertionsTests,FundsTransactionBusinessFlowIntegrationTests,FundsTransactionOrchestrationFlowTests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl tests -am test -Dtest={{tests}} {{test_flags}}

# Architecture boundary tests.
test-boundary tests='LedgerLayerBoundaryTests,RouteLayerBoundaryTests,WalletLayerBoundaryTests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn -pl tests -am test -Dtest={{tests}} {{test_flags}}

# Fast CAD verification for non-business tooling or test-asset changes.
verify-fast: compile test-boundary

# Install reactor snapshots locally when Maven plugin resolution needs local artifacts.
install-snapshots:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; fi; mvn install -DskipTests -Dmaven.test.skip=true
