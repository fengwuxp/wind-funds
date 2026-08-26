# 资金生命周期 State 命名统一 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Current project instructions do not authorize subagent delegation.

**Goal:** 将生命周期字段在数据库、Entity、Mapper、face 和 JSON 中统一为 `state`，将结果、方向和状态变更时间字段改为真实业务名，并修复 FrozenOrder 自定义查询丢失状态的问题。

**Architecture:** 先用源码、DDL 和真实 H2 扫描构造精确 RED，再按 core/transaction/wallet、reconciliation、governance 三个物理分区完成同一 breaking 版本的原子切换。迁移不保留别名、桥接 setter、双写或新旧列共存；已有宿主通过独立 forward/rollback SQL 在维护窗口与应用版本一起切换。

**Tech Stack:** Java 21、Spring Boot 3.x、MyBatis-Flex、MySQL 8、H2、JUnit 5、AssertJ、Maven、Justfile。

**Project Rules:** 遵循根 `AGENTS.md`、`wind-coding-conventions` 和 OpenSpec；自定义 `@Select` 保留，不改为 MyBatis-Flex API。当前工作树为共享脏工作树，所有写入按本计划精确 path/hunk 执行。Git stage、commit、push、PR 未授权，不进入实施步骤。

**Task ID:** `ENG-STATE-NAMING-001`。

**Owner:** 当前主 Agent 是执行 owner；Human Owner 已确认命名设计；验证 owner 由实施后的独立 Review/Checker 结论承担。

**阶段切片:** `MIG-STATE-RED -> MIG-STATE-CORE -> MIG-STATE-RECON-GOV -> MIG-STATE-CLOSURE`，严格串行，不发布中间态。

**写入范围 / 写入文件:** 仅限下方 File Map 的 create/modify 路径和为通过既定格式检查所需的同文件 import；其他路径只读。

**只读范围 / 只读参考:** 根 `AGENTS.md`、本 change spec、相关产品/DSL/系分/TDD、当前调用方、生成 `NameRefs`、现有测试结果和 Git diff。

**验收场景 / 完成条件:** 27 个物理列和全部生命周期契约统一，FrozenOrder 扫描输出 HELD/RELEASED，48 个 Entity/schema 差集为 0，命名差异只剩 3 个批准布尔映射；验证命令、测试、编译、PMD 和 CAD 结果按 Task 8 分层报告。

**TDD / Review / 编码红线:** 先 RED 后 GREEN；保留自定义 Mapper SQL；不新增别名、桥接、双写或兼容层；每个阶段审查资金不变量、公共契约、Wind 约规和 AI 产物是否出现幻觉 API、范围漂移或无主复杂度。

**Execution Grant:** 用户已确认书面规格并要求推进，授权范围限本计划的代码、测试、DDL、迁移资产和权威文档。Git 策略为 `summary_only`；禁止事项包括真实数据库执行、联网、部署、发布、stage/commit/push/PR、删除非目标资产和覆盖并行修改。撤销方式为用户暂停或任一停止条件命中后立即停止写入。

**人工确认 / 停止条件:** 需要兼容双写、真实宿主无法原子切换、目标 hunk 并行漂移、出现非目标资金行为变化或范围超出 File Map 时中断并请求人工确认。

**交接 / 恢复入口:** 每个阶段回写本 plan checkbox、实际验证证据和残余风险；恢复入口始终是最后一个已完成 MIG task 与当前 `git status/diff` 双读结果，不以计划中的预期结果冒充执行证据。

---

## File Map

### Create

- `database/mysql/migration/20260826_state_naming_forward.sql`：已有宿主的 27 个物理列重命名、索引重命名和执行后校验。
- `database/mysql/migration/20260826_state_naming_rollback.sql`：停止新应用后执行的反向列与索引重命名。
- `database/mysql/migration/README.md`：维护窗口、前置检查、执行顺序、验证和回滚边界。

### Modify: Canonical Schema

- `database/mysql/core/001_create_core_tables.sql`
- `database/mysql/reconciliation/001_create_reconciliation_tables.sql`
- `database/mysql/reconciliation/001_verify_reconciliation_tables.sql`
- `database/mysql/governance/001_create_governance_tables.sql`
- `tests/src/test/resources/jdbc-schema.sql`

### Modify: Persistence Models

- `ledger/impl/src/main/java/com/wind/funds/ledger/dal/entities/Ledger.java`
- `wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/{FundingAccount,CreditAccount,SpendControlScope,PaymentInstrument,PaymentInstrumentBinding,SpendRuleDefinition,SpendRuleVersion,SpendRuleBinding}.java`
- `transaction/impl/src/main/java/com/wind/funds/transaction/dal/entities/{FundsTransaction,FundsTransactionDetail,FundsFrozenOrder}.java`
- `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities/{ClearingSplittableDetail,ClearingSplitBatch,ClearingCandidate,ClearingBatch,ReconciliationBatch,ReconciliationRunResult,ReconciliationDifference,SettlementOrder,PayoutOrder,PayoutReceipt,RecoveryOrder}.java`
- `reconciliation/face/src/main/java/com/wind/funds/reconciliation/model/{dto/ClearingCandidateDTO,query/ClearingCandidateQuery}.java`
- `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/clearing/impl/ClearingCandidateApplicationServiceImpl.java`
- `governance/impl/src/main/java/com/wind/funds/governance/dal/entities/ProjectionReplayTask.java`

### Modify: Custom SQL And Runtime Consumers

- `transaction/impl/src/main/java/com/wind/funds/transaction/dal/mapper/{FundsTransactionMapper,FundsFrozenOrderMapper}.java`
- `transaction/impl/src/main/java/com/wind/funds/transaction/projection/impl/DefaultFundsTransactionProjectionExplainApplicationService.java`
- `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/mapper/{ClearingSplittableDetail,ClearingSplitBatch,ClearingCandidate,ClearingBatch,ReconciliationBatch,ReconciliationRunResult,ReconciliationDifference,SettlementOrder,PayoutOrder,PayoutReceipt,RecoveryOrder}Mapper.java`
- `governance/impl/src/main/java/com/wind/funds/governance/dal/mapper/ProjectionReplayTaskMapper.java`
- `governance/impl/src/main/java/com/wind/funds/governance/projection/FundsProjectionReplayService.java`
- `governance/face/src/main/java/com/wind/funds/governance/projection/FundsProjectionReplayTaskDTO.java`

### Modify: Tests And Authority

- `AGENTS.md`
- `tests/src/test/java/com/wind/funds/architecture/FundsModuleDependencyBoundaryTests.java`
- `tests/src/test/java/com/wind/funds/reconciliation/schema/ReconciliationMysqlDdlContractTests.java`
- `tests/src/test/java/com/wind/funds/reconciliation/schema/ReconciliationMysqlMigrationIntegrationTests.java`
- `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsHostCompositionContractTests.java`
- Direct SQL test consumers under `tests/src/test/java/com/wind/funds/{ledger,wallet,transaction,reconciliation}` that reference one of the 27 renamed columns; only SQL column tokens change, JSON payload keys and explanatory display status remain.
- `docs/产品设计/02-交易路由钱包账目与投影.md`
- `docs/产品设计/支付资金公共能力层-产品设计.md`
- `docs/系分设计/02-交易路由钱包账目与投影系分设计.md`
- `docs/系分设计/03-清结算与对账系分设计.md`
- `docs/系分设计/04-归档重放与指标治理系分设计.md`
- `docs/系分设计/06-SpendRule支出规则系分设计.md`
- `docs/系分设计/支付资金公共能力层-系分设计.md`
- `docs/TDD设计/支付资金底座测试驱动设计.md`
- `openspec/changes/funds-state-naming-normalization/spec.md`

---

### Task 1: Freeze The Dirty-Tree Baseline

**Files:** Read-only inspection of every path in the File Map.

- [x] **Step 1: Capture the current worktree and target diffs**

Run:

```bash
git status --short
git diff --name-status
git diff --cached --name-status
```

Expected: staged index remains empty; existing unrelated and overlapping changes are recorded without being reverted.

- [x] **Step 2: Verify the Java/Maven baseline**

Run:

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just mvn-version
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
```

Expected: Java 21 / Maven 3.6.3 and reactor compile `21/21` success. If a target hunk changes during the task, stop and re-read that file before writing.

### Task 2: Add The Naming And FrozenOrder RED

**Files:**

- Modify: `tests/src/test/java/com/wind/funds/architecture/FundsModuleDependencyBoundaryTests.java`
- Modify: `tests/src/test/java/com/wind/funds/reconciliation/schema/ReconciliationMysqlDdlContractTests.java`
- Modify: `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsHostCompositionContractTests.java`

- [x] **Step 1: Replace the Projection Replay status guard with a global naming guard**

In `FundsModuleDependencyBoundaryTests`, add `java.util.LinkedHashMap`, then replace `testProjectionReplayTaskShouldUseStatusAcrossPersistenceAndContract` with a test that scans all production Entity source roots. Parse explicit `@Column("...")` followed by its field and assert that the only cross-language mismatches are the three approved booleans:

```java
@Test
void testPersistencePropertiesShouldUseCanonicalBusinessNames() throws IOException {
    Pattern explicitColumn = Pattern.compile(
            "@Column\\(\\\"([^\\\"]+)\\\"\\)\\s+private\\s+[^;=]+\\s+(\\w+)\\s*;");
    Map<String, String> mismatches = new LinkedHashMap<>();
    for (Path entity : javaSourceFiles(List.of(
            "ledger/impl/src/main/java/com/wind/funds/ledger/dal/entities",
            "wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities",
            "transaction/impl/src/main/java/com/wind/funds/transaction/dal/entities",
            "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities",
            "governance/impl/src/main/java/com/wind/funds/governance/dal/entities"))) {
        Matcher matcher = explicitColumn.matcher(Files.readString(entity));
        while (matcher.find()) {
            String column = matcher.group(1);
            String field = matcher.group(2);
            if (!column.equals(toSnakeCase(field))) {
                mismatches.put(workspaceRoot().relativize(entity) + "#" + field, column);
            }
        }
    }

    assertThat(mismatches).containsExactlyInAnyOrderEntriesOf(Map.of(
            "ledger/impl/src/main/java/com/wind/funds/ledger/dal/entities/Ledger.java#allowNegative",
            "is_allow_negative",
            "wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/FundingAccount.java#platform",
            "is_platform",
            "wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities/PaymentInstrumentBinding.java#defaultBinding",
            "is_default"));
}
```

Add the exact helper matching the generated MyBatis-Flex convention:

```java
private String toSnakeCase(String value) {
    return value.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
            .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
            .toLowerCase(Locale.ROOT);
}
```

Add `java.util.Locale`. Extend the same test with deterministic bridge-setter collection:

```java
List<String> bridgeSetters = new ArrayList<>();
Pattern bridgeSetter = Pattern.compile("\\bvoid\\s+(setStatus|setReconciliationDecisionStatus)\\s*\\(");
for (Path entity : javaSourceFiles(List.of(
        "ledger/impl/src/main/java/com/wind/funds/ledger/dal/entities",
        "wallet/impl/src/main/java/com/wind/funds/wallet/dal/entities",
        "transaction/impl/src/main/java/com/wind/funds/transaction/dal/entities",
        "reconciliation/impl/src/main/java/com/wind/funds/reconciliation/dal/entities",
        "governance/impl/src/main/java/com/wind/funds/governance/dal/entities"))) {
    if (bridgeSetter.matcher(Files.readString(entity)).find()) {
        bridgeSetters.add(workspaceRoot().relativize(entity).toString());
    }
}
assertThat(bridgeSetters).isEmpty();
```

- [x] **Step 2: Lock Projection Replay back to `state`**

Add a focused assertion in the same architecture test:

```java
assertThat(Stream.of(ProjectionReplayTask.class.getDeclaredMethods()).map(Method::getName))
        .contains("getState", "setState")
        .doesNotContain("getStatus", "setStatus");
assertThat(Stream.of(FundsProjectionReplayTaskDTO.class.getRecordComponents())
        .map(component -> component.getName()))
        .contains("state")
        .doesNotContain("status");
```

Read `ProjectionReplayTaskMapper.java` and use these exact assertions:

```java
String mapperSource = Files.readString(workspaceRoot().resolve(
        "governance/impl/src/main/java/com/wind/funds/governance/dal/mapper/ProjectionReplayTaskMapper.java"));
assertThat(mapperSource)
        .contains("checkpoint_value, state, success_count")
        .contains("AND state IN ('CREATED', 'RUNNING')")
        .doesNotContain("status AS state", "checkpoint_value, status, success_count", "AND status IN");
```

- [x] **Step 3: Add physical-column RED assertions**

In `ReconciliationMysqlDdlContractTests`, reuse `extractCreateTable`. Add one test with these exact target groups:

```java
private static final List<String> LIFECYCLE_CORE_TABLES = List.of(
        "t_funding_account", "t_credit_account", "t_spend_control_scope", "t_payment_instrument",
        "t_payment_instrument_binding", "t_spend_rule_definition", "t_spend_rule_version",
        "t_spend_rule_binding", "t_funds_transaction", "t_funds_transaction_detail",
        "t_funds_frozen_order", "t_ledger");

private static final List<String> LIFECYCLE_RECONCILIATION_TABLES = List.of(
        "t_clearing_split_batch", "t_clearing_candidate", "t_clearing_batch",
        "t_reconciliation_batch", "t_reconciliation_difference", "t_settlement_order",
        "t_payout_order", "t_payout_receipt", "t_recovery_order");
```

For each lifecycle table, assert the MySQL and H2 table DDL contains `` `state` `` and does not contain `` `status` ``. Also assert:

```java
assertThat(extractCreateTable(reconciliationDdl, "t_clearing_splittable_detail"))
        .contains("`admission_result`", "`reconciliation_decision_result`")
        .doesNotContain("`status`", "`reconciliation_decision_status`");
assertThat(extractCreateTable(reconciliationDdl, "t_reconciliation_run_result"))
        .contains("`outcome`")
        .doesNotContain("`status`");
assertThat(extractCreateTable(coreDdl, "t_payment_instrument"))
        .contains("`flow_direction`")
        .doesNotContain("`instrument_direction`");
assertThat(extractCreateTable(governanceDdl, "t_projection_replay_task"))
        .contains("`state`")
        .doesNotContain("`status`");
```

- [x] **Step 4: Expose the FrozenOrder mapping defect through public projection output**

Extend `testBoundedProjectionScanShouldUseStableCursorForTransactionFreezeAndUnfreezeFacts`:

```java
assertThat(facts)
        .filteredOn(fact -> fact.eventType() == FundsTransactionEventType.FREEZE)
        .extracting(FundsTransactionProjectionExplanation::factStatus)
        .containsOnly("HELD");
assertThat(facts)
        .filteredOn(fact -> fact.eventType() == FundsTransactionEventType.UNFREEZE)
        .extracting(FundsTransactionProjectionExplanation::factStatus)
        .containsOnly("RELEASED");
```

- [x] **Step 5: Run RED and verify failure classes**

Run:

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-slice FundsModuleDependencyBoundaryTests,ReconciliationMysqlDdlContractTests,FundsHostCompositionContractTests tests
```

Expected: failures identify legacy `status`/semantic physical columns, Projection Replay `status`, bridge setters, and FrozenOrder projection `PROCESSING`; no Spring context or unrelated business error.

### Task 3: Rename Core, Wallet And Transaction Persistence Columns

**Files:** Core canonical DDL, H2 schema, the 12 core lifecycle Entity files, `PaymentInstrument.java`, two transaction Mapper files, and `DefaultFundsTransactionProjectionExplainApplicationService.java`.

- [x] **Step 1: Rename core physical columns and index semantics**

Apply these exact canonical/H2 replacements inside the named tables only:

| Table | Column rename |
| --- | --- |
| `t_funding_account` | `status -> state` |
| `t_credit_account` | `status -> state` |
| `t_spend_control_scope` | `status -> state` |
| `t_payment_instrument` | `status -> state`, `instrument_direction -> flow_direction` |
| `t_payment_instrument_binding` | `status -> state` |
| `t_spend_rule_definition` | `status -> state` |
| `t_spend_rule_version` | `status -> state` |
| `t_spend_rule_binding` | `status -> state` |
| `t_funds_transaction` | `status -> state` |
| `t_funds_transaction_detail` | `status -> state` |
| `t_funds_frozen_order` | `status -> state` |
| `t_ledger` | `status -> state` |

Rename index names containing `_status` to `_state`; update all unnamed composite index column lists from `status` to `state`. Keep `idx_funds_frozen_order_expire` name but change its column list to `expire_time, state` because its business name is not status-specific.

- [x] **Step 2: Remove obsolete Entity mappings**

For the 12 lifecycle Entity files, remove `@Column("status")` and keep the existing `state` field. In `PaymentInstrument`, remove `@Column("instrument_direction")` and keep `flowDirection`. In `FundsTransaction`, delete the package-private bridge:

```java
void setStatus(FundsTransactionState state) {
    this.state = state;
}
```

Do not remove the three approved boolean mappings or same-name explicit mappings such as `context_variables`.

- [x] **Step 3: Update custom transaction SQL**

In `FundsTransactionMapper`, replace selected and filtered lifecycle column `status` with `state`. In `FundsFrozenOrderMapper#scanProjectionFacts`, select `state` directly. Preserve every method signature, tenant predicate, lock, dynamic condition, order and limit.

- [x] **Step 4: Close FrozenOrder replay completion semantics**

Update only the completed-state predicate:

```java
private boolean isFrozenOrderCompleted(FundsFrozenOrder order, FundsTransactionEventType eventType) {
    if (eventType == FundsTransactionEventType.UNFREEZE) {
        return order.getState() == FundsFrozenOrderState.RELEASED
                || order.getState() == FundsFrozenOrderState.CLOSED;
    }
    return order.getState() == FundsFrozenOrderState.FROZEN
            || order.getState() == FundsFrozenOrderState.PARTIALLY_RELEASED
            || order.getState() == FundsFrozenOrderState.RELEASED
            || order.getState() == FundsFrozenOrderState.CLOSED;
}
```

This preserves the established rule that a committed freeze remains a completed `HELD` fact after a later partial release.

- [x] **Step 5: Compile and run the core/transaction evidence**

Run:

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-slice FundsHostCompositionContractTests,FundsTransactionProjectionExplainApplicationServiceTests,DefaultRoutedFundsInstructionOrchestratorProjectionTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-transaction
```

Expected: compile and transaction behavior Green. Global naming RED may remain until reconciliation/governance are migrated.

### Task 4: Rename Reconciliation State And Result Columns

**Files:** Reconciliation canonical/verification DDL, H2 schema, 11 Entity files, 11 custom Mapper files, and `ReconciliationMysqlDdlContractTests`.

- [x] **Step 1: Apply the exact reconciliation column map**

| Table | Column rename |
| --- | --- |
| `t_clearing_splittable_detail` | `status -> admission_result`, `reconciliation_decision_status -> reconciliation_decision_result` |
| `t_clearing_split_batch` | `status -> state` |
| `t_clearing_candidate` | `status -> state`, `status_changed_time -> state_changed_time` |
| `t_clearing_batch` | `status -> state` |
| `t_reconciliation_batch` | `status -> state` |
| `t_reconciliation_run_result` | `status -> outcome` |
| `t_reconciliation_difference` | `status -> state` |
| `t_settlement_order` | `status -> state` |
| `t_payout_order` | `status -> state` |
| `t_payout_receipt` | `status -> state` |
| `t_recovery_order` | `status -> state` |

Update canonical MySQL, H2, all unique/index column lists, status-specific index names, and `001_verify_reconciliation_tables.sql` column signatures/index expectations. Rename `idx_clearing_splittable_detail_status` to `idx_clearing_splittable_detail_admission_result`; lifecycle index names use `_state`.

- [x] **Step 2: Remove annotations and bridge setters**

Remove the affected `@Column` annotations from all 11 Entity files. Delete these mapping-only methods:

```java
void setStatus(... value) { ... }
void setReconciliationDecisionStatus(ReconciliationGateDecisionResult value) { ... }
```

Keep the existing fields `state`, `admissionResult`, `outcome`, and `reconciliationDecisionResult`; do not change their enum types or business accessors.

- [x] **Step 3: Update all custom reconciliation SQL**

In the 11 listed Mapper files, replace every selected, filtered, ordered and updated affected column with its target name. Preserve custom `@Select`/`@Update`, tenant predicates, `FOR UPDATE`, ordering, batch limits and method signatures. Rename SQL parameter names only when they contain `Status`; use `currentState`/`targetState`, `outcome`, or the specific result name consistently in both annotation and method parameters.

- [x] **Step 4: Run reconciliation schema and behavior verification**

Run:

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-slice ReconciliationMysqlDdlContractTests,ReconciliationRunResultApplicationServiceTests,ClearingSplittableDetailApplicationServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-reconciliation
```

Expected: MySQL/H2 DDL contract and reconciliation behavior Green; no bridge setter remains.

### Task 5: Restore Governance Projection Replay To `state`

**Files:** Governance canonical DDL, H2 schema, ProjectionReplay Entity/Mapper/Service/DTO, Host test and architecture test.

- [x] **Step 1: Rename the governance physical column**

In `t_projection_replay_task`, rename `status` to `state` in production/H2 DDL, backlog index columns, all four custom `@Select` projections and the backlog predicate.

- [x] **Step 2: Restore Entity, DTO and service accessors**

Use this final shape:

```java
// ProjectionReplayTask
private ProjectionReplayTaskState state;

// FundsProjectionReplayTaskDTO record component
@NonNull ProjectionReplayTaskState state,
```

Replace `getStatus/setStatus/.status(...)` with `getState/setState/.state(...)` only along the Projection Replay chain. Do not add `@Column`, alias accessors or JSON compatibility annotations.

- [x] **Step 3: Update Projection Replay tests**

Replace `completedVerify.status()` and `unchanged.status()` with `.state()`. Keep the existing state transition assertions and persistent replay behavior unchanged.

- [x] **Step 4: Run the complete focused RED-to-GREEN slice**

Run:

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-slice FundsModuleDependencyBoundaryTests,ReconciliationMysqlDdlContractTests,FundsHostCompositionContractTests,FundsProjectionReplayServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-governance
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-boundary
```

Expected: all naming, schema, FrozenOrder and Projection Replay focused tests Green.

### Task 6: Add Host Migration Assets

**Files:** Create the three files under `database/mysql/migration/`.

- [x] **Step 1: Write the forward migration**

The forward SQL must first query `information_schema.columns` for all source/target names, then execute these exact rename specs:

```sql
ALTER TABLE `t_funding_account` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_credit_account` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_control_scope` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payment_instrument`
    RENAME COLUMN `instrument_direction` TO `flow_direction`,
    RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payment_instrument_binding` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_rule_definition` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_rule_version` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_spend_rule_binding` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_funds_transaction` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_funds_transaction_detail` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_funds_frozen_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_ledger` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_clearing_splittable_detail`
    RENAME COLUMN `status` TO `admission_result`,
    RENAME COLUMN `reconciliation_decision_status` TO `reconciliation_decision_result`;
ALTER TABLE `t_clearing_split_batch` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_clearing_candidate`
    RENAME COLUMN `status` TO `state`,
    RENAME COLUMN `status_changed_time` TO `state_changed_time`;
ALTER TABLE `t_clearing_batch` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_reconciliation_batch` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_reconciliation_run_result` RENAME COLUMN `status` TO `outcome`;
ALTER TABLE `t_reconciliation_difference` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_settlement_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payout_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_payout_receipt` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_recovery_order` RENAME COLUMN `status` TO `state`;
ALTER TABLE `t_projection_replay_task` RENAME COLUMN `status` TO `state`;
```

Add `ALTER TABLE ... RENAME INDEX ...` for every index whose name contains `_status`; the target uses `_state`, except `idx_clearing_splittable_detail_status -> idx_clearing_splittable_detail_admission_result`. End with `information_schema` queries proving 27 target columns exist, 27 source columns are absent, and state/outcome/admission distributions are unchanged by rename.

- [x] **Step 2: Write the exact reverse migration**

Reverse the index names first and then every column rename above in reverse table order. The rollback file must state that the new application is stopped before execution and that old application startup happens only after validation.

- [x] **Step 3: Document host execution**

`database/mysql/migration/README.md` must specify: MySQL 8 prerequisite, dedicated backup, maintenance window, old instance drain, precheck, forward SQL, post-check, new application startup, monitoring, and reverse rollback order. It must state that repository tests do not execute this script and do not prove online DDL lock duration.

- [x] **Step 4: Verify migration assets statically**

Run:

```bash
rg -n 'RENAME COLUMN|RENAME INDEX|information_schema' database/mysql/migration
git diff --check -- database/mysql/migration
```

Expected: 27 forward and 27 reverse `RENAME COLUMN` specs, matching index renames, pre/post checks, no data update/delete/drop statement.

Actual rework evidence (2026-08-26): Task 6 首轮独立 Checker=`NOT PASS / P0=0 / P1=4 / P2=0`，指出缺索引前后检、缺表级行数、部分 DDL 失败无恢复路径和预检不 fail-closed。forward/rollback 各含 `27 RENAME COLUMN + 20 RENAME INDEX`，新增 27 列与 20 索引名称/列顺序 pre/post guard、24 表精确行数、双向分布证据和非 `--force` client 约束；README 明确部分提交后的矩阵化续跑/回退。第二轮独立 Checker 在脚本头统一为 MySQL 8.0.4+ 后最终=`PASS / P0=0 / P1=0 / P2=0`。静态计数与 `git diff --check` PASS；当前环境无 MySQL client，未执行真实数据库。

### Task 7: Synchronize Authority Documents

**Files:** The eight authority documents listed in File Map plus this OpenSpec.

- [x] **Step 1: Update only physical/API naming statements**

For each table field list, index list, SQL example, DTO/Query field list and TDD contract affected by the 27 renames, use the target name. Preserve legitimate qualified statuses such as `displayStatus`, `publishStatus`, external provider status and historical migration evidence.

- [x] **Step 2: Record the final naming taxonomy**

Each authoritative entry that defines conventions must state:

```text
领域生命周期当前值统一使用 state，枚举类型使用 XxxState；一次运行或准入结果使用 outcome/result，外部或展示状态必须带限定词，数据库布尔列 is_* 与 Java 布尔属性按各自语言约规命名。
```

- [x] **Step 3: Update OpenSpec state**

Set `funds-state-naming-normalization/spec.md` status to `IMPLEMENTED_AWAITING_VERIFICATION` only after source, schema and focused tests are Green. Record actual validation outputs; do not copy planned counts as evidence.

- [x] **Step 4: Verify documentation**

Run:

```bash
rg -n 'status AS state|@Column\("status"\)|instrument_direction|reconciliation_decision_status' docs openspec/changes/funds-state-naming-normalization
git diff --check -- docs openspec/changes/funds-state-naming-normalization
```

Expected: no current positive instruction uses legacy names; explicitly labeled historical evidence may remain and must not be mechanically rewritten.

### Task 8: Full Verification And Review

**Files:** No new source files; inspect the complete target diff.

- [x] **Step 1: Verify generated MyBatis-Flex metadata**

After clean compile, inspect generated `*NameRefs.java` and assert the deterministic audit result:

```text
entities=48
schema_set_diffs=0
naming_mismatches=3
approved_boolean_mismatches=3
custom_select_unmapped_columns=0
```

- [x] **Step 2: Run project quality gates**

Run sequentially:

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-transaction
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-reconciliation
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-governance
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-boundary
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just pmd
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad
git diff --check
```

Expected: focused and module gates Green. If `verify-cad` still reports the separately authorized MIG-09 FrozenOrder CRUD RED, report it as a distinct expected state and do not modify that immutable test.

- [x] **Step 3: Review the diff against Wind rules**

Check explicitly:

- custom Mapper methods and `@Select/@Update` semantics remain;
- no `LambdaQueryWrapper`, bare string QueryWrapper field or replacement wrapper was added;
- Entity never leaks into face/public contracts;
- MapStruct stays `ReportingPolicy.ERROR` and only converts models;
- no alias, bridge setter, dual field, view, trigger, fallback or compatibility accessor was added;
- Swagger annotations and code comments remain Chinese;
- no unrelated formatting or metadata churn entered the diff.

- [x] **Step 4: Report delivery without Git mutation**

Report changed files, TDD RED/GREEN evidence, compile/tests/PMD/CAD results, migration assets, expected unrelated RED, residual host migration risk and next owner. Do not stage or commit until the user gives an explicit Git grant.
