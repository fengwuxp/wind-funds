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
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; javac_flag="-Dmaven.compiler.executable={{java_home}}/bin/javac"; else javac_flag=""; fi; mvn -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.fork=true $javac_flag compile && just verify-classfiles

# Clean and compile the full Maven reactor, including annotation-generated code.
clean-compile:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; javac_flag="-Dmaven.compiler.executable={{java_home}}/bin/javac"; else javac_flag=""; fi; mvn -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.fork=true $javac_flag clean compile && just verify-classfiles && just verify-codegen

# Verify compiled class files do not contain Eclipse JDT unresolved-compilation stubs.
verify-classfiles:
    #!/usr/bin/env zsh
    set -euo pipefail
    existing=(${(f)"$(find . \( -path "*/target/classes" -o -path "*/target/test-classes" \) -type d -print)"})
    if (( ${#existing[@]} == 0 )); then
        echo "No compiled classes found. Run just compile first."
        exit 1
    fi
    if command -v rg >/dev/null 2>&1; then
        matches=$(rg -a -l "Unresolved compilation" "${existing[@]}" || true)
    else
        matches=$(grep -R -a -l "Unresolved compilation" "${existing[@]}" || true)
    fi
    if [[ -n "$matches" ]]; then
        echo "Found unresolved compilation stubs in class files:"
        echo "$matches"
        exit 1
    fi

# Verify representative MyBatis-Flex and MapStruct generated classes exist after clean compile.
verify-codegen:
    #!/usr/bin/env zsh
    set -euo pipefail
    required=(
        ledger/ledger-impl/target/classes/com/wind/funds/ledger/dal/entities/table/LedgerNameRefs.class
        transaction/transaction-impl/target/classes/com/wind/funds/transaction/dal/entities/table/FundsTransactionNameRefs.class
        wallet/wallet-impl/target/classes/com/wind/funds/wallet/dal/entities/table/PaymentInstrumentBindingNameRefs.class
        wallet/wallet-impl/target/classes/com/wind/funds/wallet/mapstruct/AccountHierarchyBindingConverterImpl.class
    )
    missing=()
    for file in "${required[@]}"; do
        [[ -f "$file" ]] || missing+=("$file")
    done
    if (( ${#missing[@]} > 0 )); then
        echo "Missing generated classes:"
        printf '%s\n' "${missing[@]}"
        exit 1
    fi

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
    @marker=$(mktemp /tmp/wind-funds-tests.XXXXXX); touch "$marker"; if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; javac_flag="-Dmaven.compiler.executable={{java_home}}/bin/javac"; else javac_flag=""; fi; mvn -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.fork=true $javac_flag -pl {{module}} -am test -Dtest="{{tests}}" {{test_flags}}; mvn_status=$?; just _assert-surefire-reports "{{tests}}" "$marker"; report_status=$?; rm -f "$marker"; if (( mvn_status != 0 || report_status != 0 )); then exit 1; fi; just verify-classfiles

# Run all tests in one Maven module.
test-module module='tests':
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; javac_flag="-Dmaven.compiler.executable={{java_home}}/bin/javac"; else javac_flag=""; fi; mvn -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.fork=true $javac_flag -pl {{module}} -am test {{test_flags}} && just verify-classfiles

# Core DSL and contract tests.
test-core tests='FundsInstructionDslContractTests,RouteDslContractTests,FundsDslJsonContractTests,PaymentInstrumentRouteDslContractTests,PostingLedgerDslContractTests,SettlementPolicySpecTests,FundsAmountBoundaryContractTests':
    @just _run-test-classes "{{tests}}" tests

# FX source-price selection and amount-conversion tests.
test-fx tests='DefaultFxAmountConversionServiceImplTests':
    @just _run-test-classes "{{tests}}" tests

# Ledger assembly, transaction, posting, and projection tests.
test-ledger tests='DefaultLedgerPostingAssemblerTests,DefaultLedgerTransactionPostingServiceImplTests,LedgerBalanceProjectionServiceImplTests,LedgerServiceImplTests,LedgerTransactionServiceFactQueryTests,LedgerTransactionServiceImplTests':
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
test-boundary tests='FundsContextVariablesContractTests,FundsTransactionParticipantContractTests,FundsTransactionRequestContextVariablesContractTests,LedgerBalanceChangedEventContractTests,LedgerDtoContextVariablesContractTests,SensitiveContextVariablesValidatorTests,RouteDslContractTests,PaymentInstrumentRouteDslContractTests,PostingLedgerDslContractTests,DefaultRouteReplayServiceTests,CompositeRouteResolverTests,AuthorizationFundsInstructionRouteResolverTests,RouteResolverFactBoundaryTests,FundingAccountServiceImplTests,LedgerProfileContractTests,ControlAccountLedgerInitializationTests,PaymentInstrumentServiceImplTests,SpendSubjectFundingRelationServiceImplTests,PlatformFundingAccountServiceImplTests,FundsSubjectBalanceQueryServiceImplTests,WalletLayerBoundaryTests,FundsModuleDependencyBoundaryTests':
    @just _run-test-classes "{{tests}}" tests

# Governance projection replay boundary tests.
test-governance tests='FundsProjectionReplayServiceTests':
    @just _run-test-classes "{{tests}}" tests

# Reconciliation, difference lifecycle, gate consumption, and payout preflight tests.
test-reconciliation tests='ClearingSplittableDetailApplicationServiceTests,ClearingSettlementGateConsumerServiceTests,PayoutPreflightServiceTests,ReconciliationDifferenceApplicationServiceTests,ReconciliationGateApplicationServiceTests,ReconciliationDifferenceReportApplicationServiceTests,ReconciliationRunResultApplicationServiceTests':
    @just _run-test-classes "{{tests}}" tests

# Fast CAD verification for non-business tooling or test-asset changes.
verify-fast: mvn-version compile test-boundary test-governance test-reconciliation

# Full CAD verification for the rebuilt payment funds test baseline.
verify-cad: mvn-version clean-compile test-core test-fx test-ledger test-transaction test-balance-control test-business-flow test-boundary test-governance test-reconciliation pmd verify-classfiles verify-codegen

# Install reactor snapshots locally when Maven plugin resolution needs local artifacts.
install-snapshots:
    @if [[ -n "{{java_home}}" ]]; then export JAVA_HOME="{{java_home}}"; export PATH="{{java_home}}/bin:$PATH"; javac_flag="-Dmaven.compiler.executable={{java_home}}/bin/javac"; else javac_flag=""; fi; mvn -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.fork=true $javac_flag install -DskipTests -Dmaven.test.skip=true && just verify-classfiles
