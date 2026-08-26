# 资金生命周期 State 命名统一规格

## Change Metadata

| 字段 | 值 |
| --- | --- |
| Change ID | `funds-state-naming-normalization` |
| 状态 | `VERIFIED` |
| 日期 | `2026-08-26` |
| 目标 Owner | Human Owner 已确认生命周期统一使用 `state` |
| Git 策略 | `NO_GIT_MUTATION_WITHOUT_EXPLICIT_GRANT` |
| 数据库执行 | 仓库只提供 DDL 与迁移资产，不执行宿主或生产数据库变更 |

## 1. 背景、当前问题与证据

当前 48 个 MyBatis-Flex Entity 与 MySQL DDL、H2 schema 的列集合一致，但存在 28 处 Java 属性名与物理列名不一致：

- 21 处生命周期属性使用 Java `state`、数据库 `status`。
- 2 处结果事实使用 Java `admissionResult` / `outcome`、数据库 `status`。
- 1 处对账决策使用 Java `reconciliationDecisionResult`、数据库 `reconciliation_decision_status`。
- 1 处支付工具方向使用 Java `flowDirection`、数据库 `instrument_direction`。
- 3 处布尔字段使用数据库 `is_*` 与 Java 业务属性名，属于语言约规差异。

`FundsFrozenOrderMapper#scanProjectionFacts` 已证明双命名会形成实际缺陷：自定义 `@Select` 返回 `FundsFrozenOrder`，结果列 `status` 无法自动映射到属性 `state`，且该 Entity 没有 ResultMap、别名或桥接 setter；投影扫描随后以空状态判断冻结事实是否完成。其他自定义查询通过额外 `setStatus` / `setReconciliationDecisionStatus` 桥接，虽可工作，但继续扩大隐式映射和维护风险。

当前缺陷调用链为 `FundsFrozenOrderMapper#scanProjectionFacts -> DefaultFundsTransactionProjectionExplainApplicationService#explainFrozenOrder -> isFrozenOrderCompleted`；现有扫描测试只断言事件类型和流水号，未断言状态映射与 `completed`。

## 2. 命名决策

### 2.1 统一词汇

1. 领域生命周期当前值统一命名为 `state`。
2. 生命周期枚举类型继续使用 `XxxState`。
3. 数据库列、Entity 属性、DTO / Request / Query / JSON 属性、MyBatis `NameRefs`、SQL 条件和索引语义对同一生命周期统一使用 `state`。
4. 一次运行或准入的结果事实使用 `outcome`、`result` 或更具体的业务名称，不得为了形式统一改成 `state`。
5. 外部通道原始状态、展示状态和发布状态可使用带限定词的 `status`，例如 `displayStatus`；不得与本仓库领域生命周期混用。
6. 数据库布尔列继续使用 `is_*`，Java 属性不使用 `is` 前缀；这是数据库与 Java 各自约规，不视为同义词混用。

### 2.2 物理列目标

本变更包含 27 个物理列重命名：

- 21 个当前仍为 Java `state` / 数据库 `status` 的生命周期列：`status -> state`。
- 1 个本轮已临时统一为 `status` 的 `t_projection_replay_task.status -> state`。
- `t_clearing_candidate.status_changed_time -> state_changed_time`。
- `t_clearing_splittable_detail.status -> admission_result`。
- `t_reconciliation_run_result.status -> outcome`。
- `t_clearing_splittable_detail.reconciliation_decision_status -> reconciliation_decision_result`。
- `t_payment_instrument.instrument_direction -> flow_direction`。

以下布尔映射保持不变：

- `t_ledger.is_allow_negative -> Ledger.allowNegative`。
- `t_funding_account.is_platform -> FundingAccount.platform`。
- `t_payment_instrument_binding.is_default -> PaymentInstrumentBinding.defaultBinding`。

## 3. 范围

### 3.0 重构准入

本变更需要独立重构设计，不能作为局部命名修改处理：它同时改变 27 个物理列、三个生产 DDL、H2 schema、MyBatis 自定义查询、生成 `NameRefs`、Public JSON 字段和宿主升级顺序。若只修改 Entity 或 Mapper，会继续保留双命名并让旧应用与新 schema 交叉失效。

行为变化只限字段名称和访问器名称；状态值、状态转换和资金行为属于非目标。

### 3.1 In Scope

- 更新 `database/mysql/core`、`database/mysql/reconciliation`、`database/mysql/governance` 的 canonical create DDL。
- 更新 H2 `jdbc-schema.sql`、显式 Mapper SQL、Entity `@Column`、桥接 setter 和 MyBatis-Flex 生成引用的源码输入。
- 将 `ProjectionReplayTask`、`FundsProjectionReplayTaskDTO` 和相关服务从 `status` 恢复为 `state`。
- 修复冻结单投影扫描的状态映射，并断言 `FREEZE` / `UNFREEZE` 扫描结果的完成语义。
- 同步权威产品、DSL、系分、TDD 和接入文档中属于本次物理模型或 Public API 的字段名。
- 增加可执行架构守卫，防止生命周期再次出现 `@Column("status") private ... state`、`status AS state`、`setStatus` 桥接或生命周期表 `status` 列。
- 提供现有宿主数据库使用的显式 MySQL 升级资产、预检、执行后校验和回滚说明；仓库不自动执行。

### 3.2 Out Of Scope

- 不修改枚举值、生命周期状态机、允许转换、金额、账本、余额、幂等、路由、清结算或对账业务行为。
- 不把 `outcome`、`admissionResult`、`reconciliationDecisionResult`、`flowDirection` 或布尔属性强行改成 `state`。
- 不保留 SQL 别名、双字段、兼容 getter/setter、ResultMap 双读、数据库 view、触发器或长期双写。
- 不执行真实 MySQL、宿主、生产数据迁移，不部署、不发布、不联网、不操作 Git。
- 不推进 MIG-09 FrozenOrder Public CRUD Green；本变更只覆盖其原 immutable survivor 中已确认的独立映射缺陷和命名治理，其他 MIG-09 范围保持不变。

## 4. 目标结构与行为不变量

- **目标结构**：每个生命周期事实只存在一个 `state` 物理列和一个同名 Java / JSON 属性；结果事实和方向字段使用各自业务名称。
- **行为不变量**：所有枚举值、允许转换、状态推进条件、幂等摘要输入、资金事实、账本分录和余额结果保持不变。
- **公共契约不变量**：已有 47 个 face `state` 字段保持；Projection Replay 恢复为原 `state` 契约。除该恢复外不新增或删除 Public API。
- **保留范围**：`XxxState` 枚举、业务状态机、布尔 `is_*` 数据库列和对应 Java 属性保持。
- **替换范围**：27 个物理列及其 SQL、注解、索引语义和文档引用一次性替换。
- **删除范围**：删除生命周期 `@Column("status")`、自定义 `setStatus` / `setReconciliationDecisionStatus` 映射桥和本轮临时 `status` accessor；不删除业务事实或表。

## 5. 兼容与迁移规则

这是一次明确的 breaking schema 与序列化契约迁移，不提供长期兼容层。

1. Fresh install 的 canonical create DDL 直接使用目标列名。
2. 已有宿主通过独立升级 SQL 原地重命名列，不复制数据、不双写；升级资产必须包含写前列/索引存在性检查、目标列冲突检查、执行后行数与关键状态分布校验，以及反向 rename 回滚语句。
3. 应用版本与数据库迁移必须作为同一宿主发布单元切换；旧应用不得连接新 schema，新应用不得连接旧 schema。
4. 仓库 H2 与静态 DDL 证据只证明契约和 SQL 闭合，不证明真实 MySQL 锁时长、在线 DDL 能力或宿主停机窗口。
5. Public Java accessor 与 JSON 字段只在当前仍为 `status` 的 Projection Replay 契约上发生 `status -> state`；其他 face 生命周期字段本来已经使用 `state`。

迁移规则：

- **主写方**：切换后只有新应用写目标列；旧应用停止后才执行 rename，新应用只在 rename 成功后启动。
- **双写与回填**：禁止双写；列原地 rename，不复制、不回填数据。
- **影子读与灰度切流**：禁止影子读和按流量灰度，因为新旧应用与 schema 二进制不兼容；宿主只能按维护窗口原子切换。
- **共存与下线条件**：新旧列共存时间为零；执行后校验通过即下线旧列名和旧应用，任何旧实例存活都阻断切换。
- **回滚**：新应用未启动前可直接反向 rename；新应用已写入后先停新应用，再反向 rename 并恢复旧应用。状态值不转换，因此不需要业务数据回填。

## 6. MIG 切片

以下切片是同一 breaking 版本的工程实施顺序，不允许把中间态单独发布：

| MIG | 前置条件 | 写入范围 | 验证证据 | 暂停与回退 |
| --- | --- | --- | --- | --- |
| `MIG-STATE-RED` | 本规格经用户复核；写前 manifest 与目标 hunk 冻结 | 仅架构守卫、FrozenOrder 扫描和 Projection Replay 契约测试 | 失败只列旧命名与冻结状态丢失 | 额外 failure/error 或并行漂移立即暂停并恢复测试目标 hunk |
| `MIG-STATE-CORE` | RED 独立可复现 | core/wallet/transaction canonical DDL、H2、Entity、Mapper、Converter/Service | core/transaction focused、schema 差集、冻结投影完成语义 | 任一状态值、金额、Ledger/Balance 或幂等差异立即回退整个切片 |
| `MIG-STATE-RECON-GOV` | CORE focused Green；不得发布中间态 | reconciliation/governance DDL、Entity、Mapper、Projection Replay face/Service | reconciliation/governance focused、Public Contract、schema 差集 | 契约字段或对账结果语义漂移立即回退整个切片 |
| `MIG-STATE-CLOSURE` | 全部源码 Green | 宿主升级资产、权威文档、最终守卫 | 契约测试、回归测试、数据校验、PMD、CAD、diff-check | 迁移资产缺预检/回滚或完整门禁出现非预期失败时不准出 |

## 7. TDD 与验证

### 7.1 RED

先增加以下失败保护，生产代码不同时修改：

1. Entity / DDL 命名守卫：列出仍存在的生命周期 `status`、结果事实通用 `status`、桥接 setter 和 `@Column("status")`。
2. FrozenOrder 自定义扫描测试：插入明确状态的冻结/解冻事实，断言 Mapper 扫描得到非空 `state`，并断言投影 explanation 的 `completed` 语义。
3. Projection Replay 契约测试：要求 record component、Entity accessor 和 SQL 列统一为 `state`。
4. MySQL/H2 schema 对齐测试：按最终物理列名比较三份生产 DDL、H2 schema 与 48 个 Entity 生成列集合。

RED 必须只由预期旧命名和冻结单状态丢失导致；出现业务行为、资金事实、Spring 装配或其他并行切片失败时停止。

### 7.2 GREEN

按数据库 -> Entity -> Mapper -> Converter / Service -> face -> docs 的依赖顺序完成一次性切换。禁止以别名或桥接 setter 让测试提前变绿。

### 7.3 验证命令

至少执行：

```bash
just mvn-version
just compile
just verify-slice FundsModuleDependencyBoundaryTests,FundsHostCompositionContractTests,FundsProjectionReplayServiceTests tests
just test-transaction
just test-reconciliation
just test-governance
just test-boundary
just pmd
git diff --check
```

阶段收口执行 `just verify-cad`。若当前共享工作树仍保留其他已授权 RED，必须把本切片 focused Green、完整套件结果和非本切片预期 RED 分层报告，不得改动其他测试来制造全绿。

监控与告警属于宿主迁移验证：宿主必须观察应用启动失败、未知列 SQL、状态分布差异和关键资金流程错误率；本仓库不把静态 DDL 或 H2 证据冒充宿主监控。

## 8. 验收标准

| ID | 验收条件 |
| --- | --- |
| `AC-STATE-001` | 48 个 Entity 与最终 MySQL/H2 schema 列集合完全一致。 |
| `AC-STATE-002` | 生命周期数据库列、Entity、Mapper、face 属性和 JSON 字段只使用 `state`。 |
| `AC-STATE-003` | `admissionResult`、`outcome`、`reconciliationDecisionResult`、`flowDirection`、`stateChangedTime` 使用各自真实物理列名。 |
| `AC-STATE-004` | 不存在 `status AS state`、生命周期 `@Column("status")`、`setStatus` 映射桥或兼容双字段。 |
| `AC-STATE-005` | FrozenOrder 自定义扫描返回真实 `state`，冻结/解冻投影完成语义正确。 |
| `AC-STATE-006` | 状态值、状态转换、资金事实、账本、余额、幂等和重放行为与命名迁移前一致。 |
| `AC-STATE-007` | Fresh DDL 与宿主升级资产均可被独立审查；未执行真实数据库变更。 |

## 9. Engineering Handoff

- **第一实施切片**：`MIG-STATE-RED`，只写测试和架构守卫，不修改生产代码、DDL 或文档。
- **执行 owner**：wind-funds Maker，仅在用户复核本书面规格并批准实施计划后执行。
- **验证 owner**：独立 Checker 复核 RED failure class、字段清册、dirty overlap 和每个 Green 切片的 focused 证据。
- **写入边界**：实施计划必须列出精确文件和 hunk；未列入路径只读。
- **停止条件**：范围扩大、真实宿主无法原子切换、非目标测试失败、资金事实变化、共享工作区目标 hunk 漂移或需要兼容双写时立即停止。

## 10. 冲突与停止条件

- 当前工作树包含多轮并发文档、Mapper、Converter、测试和 MIG-09 RED 修改。实施计划必须冻结精确路径与 hunk，写前后双读，不覆盖非本变更内容。
- 本规格经用户书面复核前只授权设计文件，不授权 Java、测试、DDL 或其他权威文档写入。
- Git stage、commit、push、PR 仍需用户单独明确授权。
- 若确认存在仍运行旧 schema 的真实宿主且无法原子切换应用与数据库，本 breaking 方案停止，重新评审兼容窗口；不得临时加入别名、双写或 fallback。

## 11. 当前实施证据

- RED：`FundsModuleDependencyBoundaryTests + ReconciliationMysqlDdlContractTests + FundsHostCompositionContractTests` 为 `45 tests / 3 failures / 0 errors / 0 skipped`，三项 failure 分别锁定旧显式映射、旧物理列和 FrozenOrder `PROCESSING` 错误解释。
- GREEN 编译：Java 21 / Maven 3.6.3，`just compile=21/21`。
- GREEN 聚焦：提交候选独立快照中的全局命名、DDL、FrozenOrder 与 Projection Replay 为 `62/0F/0E/0S`；共享工作区扩大后为 `65/0F/0E/0S`；core/transaction 投影为 `16/0F/0E/0S`；reconciliation 聚焦为 `55/0F/0E/0S`。
- GREEN 模块：`test-transaction=185/0F/0E/0S`、`test-reconciliation=246/0F/0E/0S`、`test-governance=24/0F/0E/0S`、`test-boundary=212/0F/0E/0S`。
- 迁移资产：forward/rollback 各 `27 RENAME COLUMN + 20 RENAME INDEX`；索引前后检、24 表行数、部分 DDL 恢复和 fail-closed 预检 rework 已通过独立 Checker，结果为 `PASS / P0=0 / P1=0 / P2=0`；未执行真实 MySQL。
- 最终结构审计：`entities=48`、`schema_set_diffs=0`、`naming_mismatches=3`，仅保留三个批准的数据库 `is_*` / Java 布尔属性映射；`custom_select_unmapped_columns=0`。
- 最终规约复核：未发现生命周期 `@Column("status")`、`status AS state`、映射桥 setter 或 `LambdaQueryWrapper`；`ProjectionReplayTaskMapper` 保留四个自定义 `@Select` 并直接读取 `state`。
- 提交候选独立快照门禁：`just compile=21/21`；`just verify-cad` 以 `exit 0` 完成，其中全量测试 `1205/0F/0E/1S`，后续 PMD/CAD reactor `21/21` 成功；最新 Task 6 SQL/文档 rework 另经静态检查、`git diff --check` 和独立 Checker 准出。
- 完整门禁仅出现上游 `wind-middleware` POM 中 `sentinel-transport-common` 重复声明警告，不属于本仓库变更，也未阻断构建。
