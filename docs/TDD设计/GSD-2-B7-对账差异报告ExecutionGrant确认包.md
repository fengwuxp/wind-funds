# GSD-2 B7 对账差异报告 Execution Grant 确认包

## 1. 文档定位

本文是 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 的 Execution Grant 确认包，用于承接已提交的对账差错、对象级 Gate、出款 preflight 和清算 / 结算只读 consumer 证据，补齐面向运营、财务、风控、研发和测试的只读差异报告查询能力。

本文不是新的 PRD，不替代清结算与对账产品设计、DSL、系分或 TDD 正文；也不是生产 DDL 授权、Git 授权或上线发布授权。本文最初作为产品、架构、TDD 和编码准入交接材料；用户已确认 `Execution Grant：GSD2-B7-RECON-DIFFERENCE-REPORT-001` 后，本文件继续记录本 Grant 的消费结果、验证证据和 Not Done。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B7-RECON-DIFFERENCE-REPORT-001` |
| 原子任务 | 新增对账差异报告最小只读查询能力，解释差错状态、阻断对象、处理动作、重跑结果、准入 gate 和证据引用。 |
| 所属阶段 | GSD-2 / B7 reconciliation difference report / Green verified。 |
| 当前状态 | `CONSUMED_GREEN_VERIFIED_SUMMARY_ONLY` |
| 前置证据 | `GSD2-B7-RECON-DIFFERENCE-MVP-001`、`GSD2-B7-RECON-DIFFERENCE-MVP-002`、`GSD2-B7-RECON-GATE-CONSUME-001/002`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 和 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 已完成；当前已提交 Git/code baseline 为 `0d3f68dc feat: 补齐清算结算对账准入消费`。 |
| Owner | AI Native 负责 Loop、Goal、状态回写和停止条件；产品架构专家负责业务目标、对象、能力地图、规则矩阵、验收和风险；资深架构师负责接口契约、边界、TDD、实现建议、Review 和验证；用户确认单一 Grant。 |
| 写入范围 | 本文、LWT Goal、W5 推进计划、GSD-2 工作流入口、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-B7-对账差异报告ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、`reconciliation-*`、`ledger-*`、`transaction-*`、`wallet-*`、tests、Justfile、AGENTS.md 和最近 Git 提交。 |
| 只读参考 | `ReconciliationDifferenceApplicationService`、`ReconciliationGateApplicationService`、`ClearingSettlementGateConsumerService`、`PayoutOrderService#checkPayoutPreflight`、B7 对象级 Gate 确认包、B7 清算 / 结算 consumer 确认包和当前 LWT Goal。 |
| Git 策略 | 本 Grant 当前为 `summary_only`，已完成代码、测试和状态回写，但未授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。若后续需要提交，必须重新确认提交范围。 |

## 2. 产品裁决

业务目标：让对账差错从“可登记、可处理、可阻断准入”进一步变成“可解释、可导出、可复盘、可交接”的只读报告视图，支撑清分、清算、结算、出款和运营差错闭环的人工确认。

用户价值：运营能看到差错为什么阻断、谁处理了什么动作、重跑是否对平；财务能核对差错金额、币种、阻断对象和证据引用；研发和测试能用同一视图定位来源事实、gate 决策和无资金副作用边界。

非目标：

1. 不新增清分、清算、结算、出款、追偿或补事实执行能力。
2. 不新增运营审批流、后台页面、报表发布、生产告警或 Runbook。
3. 不新增生产 DDL、迁移脚本、冷热归档、Manifest、checkpoint 或治理重放能力。
4. 不把差异报告反写成交易事实、route、LedgerEntry、余额投影、清算对象或结算对象。

成功指标：

1. 可按租户和差错流水查询单笔差异报告。
2. 报告可解释差错基础信息、阻断范围、阻断对象、处理动作、原始事实引用、处理事实引用、重跑结果、gate 决策和证据引用。
3. 查询只读，查询前后交易、route、ledger transaction、LedgerEntry、余额投影、清算和结算事实数量不变。
4. 报告不泄露外部账户原文、敏感通道字段、完整卡号、未脱敏对手方或任意上下文原文。

## 3. 能力地图和业务对象

| 能力域 | MVP 目标 | 数据能力 | 后续扩展 |
| --- | --- | --- | --- |
| 差异报告查询 | 按差错流水查询单笔报告。 | 差错、处理动作、重跑、gate 决策和证据引用的只读聚合。 | 批次报告、导出、订阅和运营后台筛选。 |
| 阻断解释 | 说明差错阻断了哪个清算、结算或出款对象。 | `blockingScope`、`blockingObjectType`、`blockingObjectSn`、gate 消费对象和决策状态。 | 多对象聚合报告、账龄升级和责任归属。 |
| 处理链路解释 | 说明处理动作、幂等键、原始事实和处理事实引用。 | action type、action idempotency key、original fact ref、adjustment result 和证据引用。 | 审批单、复核人、职责分离和补事实白名单执行。 |
| 重跑结果解释 | 说明重新对账结果是否对平。 | rerun serial、rerun result、checkedAt、condition pass evidence。 | 多轮重跑时间线、批次运行记录和归档留存。 |

业务对象：

- ReconciliationDifferenceReport：面向使用者的只读差异报告，不是新的资金事实。
- ReconciliationDifference：差错事实来源，承载状态、金额、币种、差错类型、阻断范围和处理动作。
- ReconciliationGateDecision：准入 gate 决策来源，解释是否阻断、条件放行或通过。
- DifferenceEvidenceRef：原始事实、处理事实、重跑事实、gate 消费对象和外部凭证的引用集合。
- ReportViewPolicy：敏感字段过滤、权限边界和展示口径，不改变底层事实。

状态口径：

| 状态 | 生命周期含义 | 报告展示 |
| --- | --- | --- |
| OPEN / BLOCKING | 差错未闭环或仍阻断准入。 | 显示阻断对象、阻断范围和下一步处理建议。 |
| LINKED / ACTIONED | 已有关联处理动作或调账结果。 | 显示处理动作、幂等键、处理引用和原始事实引用。 |
| RERUNNING / RERUN_RECONCILED | 已触发重新对账并获得结果。 | 显示重跑流水、是否对平和 gate 是否条件放行。 |
| CLOSED | 差错已闭环。 | 显示关闭原因、证据链和不可覆盖历史报告提示。 |

## 4. 业务流程和规则矩阵

主流程：

1. 使用者输入租户、差错流水或业务对象引用。
2. 报告服务读取差错事实、处理动作、重跑结果和 gate 决策。
3. 报告服务组装只读视图，按权限和安全策略过滤敏感数据。
4. 使用者查看阻断原因、证据引用、处理状态和下一步建议。

异常流程：

1. 差错不存在时返回明确未找到，不创建差错。
2. 差错存在但处理链不完整时返回 `INCOMPLETE_ACTION_EVIDENCE` 或等价完整性状态，不自动补事实。
3. gate 决策缺失时返回 `MISSING_GATE_DECISION` 或等价提示，不重放或重新执行 gate。
4. 权限不足或敏感字段命中时隐藏敏感字段并保留审计提示。

人工兜底：报告只给出“需要处理、需要复核、需要重跑、需要补证据、可以关闭”的操作建议，不直接执行处理动作、审批或资金调整。

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验收口径 |
| --- | --- | --- | --- | --- | --- |
| 单笔差异报告 | 使用者按差错流水查询。 | 只读聚合差错、处理动作、重跑和 gate 信息。 | P0 | B7-REPORT-MVP | 目标测试证明字段完整且无资金副作用。 |
| 阻断对象解释 | 差错带 `blockingObjectType / blockingObjectSn`。 | 报告展示阻断对象；历史类型级差错展示为保守阻断。 | P0 | B7-REPORT-MVP | 对象级和历史类型级差错均可解释。 |
| 处理链完整性 | 差错有关联动作。 | action type、幂等键、原始事实引用和处理事实引用必须可追踪。 | P0 | B7-REPORT-MVP | 缺关键处理证据时显示不完整，不补写事实。 |
| 重跑解释 | 差错已重跑。 | 展示重跑流水、重跑结果和条件放行证据。 | P1 | B7-REPORT-MVP | 重跑对平和未对平都能解释。 |
| 敏感字段过滤 | 报告包含上下文变量或外部引用。 | 外部账户原文、卡号、通道敏感字段和任意上下文原文不得透出。 | P0 | B7-REPORT-MVP | 目标测试覆盖敏感字段不泄露。 |

## 5. 架构方案

### 5.1 核心决策

核心决策：先做 reconciliation 逻辑边界内的只读 application service，不新增独立报表模块，也不把差异报告落成新的资金事实。

职责边界：

1. `reconciliation-face` 暴露查询契约、Request、DTO 和完整性枚举。
2. `reconciliation-impl` 只读聚合现有差错事实、gate 决策和必要证据引用。
3. `ledger`、`transaction`、`wallet` 只作为后续只读引用来源，不被本 Grant 写入。
4. 运营后台、导出、批量报告和治理归档后置。

取舍：

1. 先做单笔报告，避免一次性做批次、导出和运营后台。
2. 优先复用现有差错表和 gate 决策，不在本切片新增生产 DDL。
3. 报告完整性用 DTO 表达，避免通过补写事实“修复”报告。

### 5.2 推荐接口契约

| 契约 | 建议落点 | 入参 / 出参 | 兼容和幂等 |
| --- | --- | --- | --- |
| `ReconciliationDifferenceReportApplicationService` | `reconciliation-face/src/main/java/com/wind/funds/reconciliation/application/difference/report` | `getReport(GetReconciliationDifferenceReportRequest request, WindOperator operator)` 返回 `ReconciliationDifferenceReportDTO`。 | 查询只读，无写入幂等事实；按 `tenantId + differenceSn` 稳定定位。 |
| `GetReconciliationDifferenceReportRequest` | `reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/request` | 入参包含 `tenantId`、`differenceSn`、可选 `includeGateDecision`、可选 `includeEvidenceRefs`。 | 延续当前 reconciliation 查询/动作入参统一放在 `model/request` 的风格；缺租户或差错流水直接拒绝。 |
| `ReconciliationDifferenceReportDTO` | `reconciliation-face/src/main/java/com/wind/funds/reconciliation/model/dto` | 出参包含差错摘要、阻断摘要、处理摘要、重跑摘要、gate 摘要、完整性和安全过滤提示。 | DTO 兼容已有差错状态，不改旧服务契约。 |
| `ReconciliationDifferenceReportCompleteness` | `reconciliation-face/src/main/java/com/wind/funds/reconciliation/enums` | 出参完整性，例如 `COMPLETE`、`INCOMPLETE_ACTION_EVIDENCE`、`MISSING_RERUN_RESULT`、`MISSING_GATE_DECISION`。 | 枚举延续当前 `com.wind.funds.reconciliation.enums` 包；完整性只用于解释，不驱动资金动作。 |

错误码或断言语义：优先使用现有断言工具和资金域异常口径；本 Grant 不新增完整公共错误码体系。

### 5.3 数据方案和一致性

数据方案：首轮只读聚合已有 `t_reconciliation_difference` 和现有 gate 查询结果；不新增生产表、不修改生产 DDL、不创建 report fact。当前 `ReconciliationDifferenceMapper` 只有 `selectByDifferenceSnForUpdate` 和 `selectByGateObject`，确认后需要新增不加锁的只读查询方法，例如 `selectByDifferenceSn(tenantId, differenceSn)`；不得复用 `FOR UPDATE` 查询来构造报告。

事务边界：查询服务使用只读事务或等价只读语义；不得调用 create、link、rerun、posting、balance projection、clearing、settlement、payout 或补事实入口。

一致性：报告反映查询时刻的当前事实，不覆盖历史运行记录；后续批次报告或归档留存需要独立 Grant 设计不可篡改快照。

补偿：报告发现证据不完整时只返回完整性状态和人工处理建议，不做自动补偿。

对账：差异报告是对账运营视图，不替代原始对账任务、重新对账运行记录或 gate 决策事实。

### 5.4 源码锚点审计

本节是确认前只读源码审计结果，用于减少确认后 Red / Green 的二次翻译成本，不构成编码授权。

| 审计项 | 当前源码事实 | 对本 Grant 的影响 |
| --- | --- | --- |
| 差错应用服务 | 已存在 `ReconciliationDifferenceApplicationService` 和 `ReconciliationDifferenceApplicationServiceImpl`，落点为 `application/difference`。 | 报告能力应放在 `application/difference/report` 子包，避免混入 create / link / rerun 写服务职责。 |
| 准入 Gate 应用服务 | 已存在 `ReconciliationGateApplicationService#checkGate`，返回 `ReconciliationGateDecisionDTO`，实现为只读事务。 | 报告实现可以只读调用 gate service 或复用同等查询逻辑，但不得写 gate 决策事实或重新执行清结算。 |
| 清算 / 结算消费方 | 已存在 `ClearingSettlementGateConsumerService` 和 `ClearingSettlementGateConsumerServiceImpl`，落点为 `reconciliation-face/.../service` 和 `reconciliation-impl/.../services/impl`。 | consumer 是被报告解释的证据来源之一；报告服务不应放到 `service` 包混淆为业务消费方。 |
| 差错 DTO / Request 风格 | 现有请求统一在 `model/request`，DTO 在 `model/dto`，枚举在 `reconciliation/enums`。 | 新增 Query 建议命名为 Request，并进入 `model/request`；完整性枚举进入 `reconciliation/enums`。 |
| 差错 Mapper | 当前有加锁查询和对象级 gate 查询，缺少单笔报告不加锁查询。 | 首个 Green 可只新增只读 mapper 方法；不得修改 Entity 字段、DDL/H2 schema 或现有写入查询。 |
| 目标测试 | 现有差错 lifecycle 测试在 `tests/.../application/difference/impl/ReconciliationDifferenceApplicationServiceTests`，gate 测试在 `application/gate/impl`，consumer 测试在 `services/impl`。 | 新测试建议落在 `tests/src/test/java/com/wind/funds/reconciliation/application/difference/report/impl/ReconciliationDifferenceReportApplicationServiceTests.java`。 |

## 6. Red 候选和验收矩阵

| redId | 验收场景 | moneyInvariant | expectedFacts | forbiddenFacts | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `B7-REPORT-RED-001` | 未闭环对象级差错生成报告。 | 查询不得改变资金、账本、清算或结算事实。 | 报告展示差错、阻断对象、阻断范围、金额、币种和 gate 阻断状态。 | 不创建交易、route、posting、LedgerEntry、余额投影、清算或结算事实。 | `tests/.../application/difference/report/impl/ReconciliationDifferenceReportApplicationServiceTests`。 | `just test-one ReconciliationDifferenceReportApplicationServiceTests tests`。 | 需要新 report table 或批次快照时停止。 |
| `B7-REPORT-RED-002` | 已处理但重跑未对平的差错报告。 | 处理动作不等于自动放行。 | 报告展示 action type、幂等键、原始事实、处理引用和重跑未对平。 | 不改差错状态，不重新执行 rerun。 | 同上。 | 目标测试 + `just test-reconciliation`。 | 需要改变差错状态机时停止。 |
| `B7-REPORT-RED-003` | 处理后重跑对平的差错报告。 | 条件放行必须可追踪到重跑证据。 | 报告展示 `CONDITIONALLY_PASSED` 或等价 gate 摘要和证据引用。 | 不创建清算、结算、出款或补事实。 | 同上。 | 目标测试。 | 需要接清算 / 结算生命周期时停止。 |
| `B7-REPORT-RED-004` | 敏感上下文过滤。 | 报告不得泄露敏感外部账户或通道字段。 | DTO 返回安全过滤提示和脱敏引用。 | 不返回外部账户原文、完整卡号、任意 contextVariables 原文。 | 同上。 | 目标测试 + boundary 视情况。 | 需要权限体系或脱敏框架改造时停止。 |

## 7. 写入范围和禁止事项

授权范围待用户确认后才生效；确认前本文只作为准入材料。

确认后允许写入：

1. `reconciliation-face` 新增报告查询契约、Request、DTO 和必要枚举。
2. `reconciliation-impl` 新增只读 application service 实现和必要的 mapper 只读查询方法。
3. `tests` 新增目标服务流测试，使用真实 Spring Bean、H2 schema 和既有差错、gate、consumer 服务。
4. 文档、LWT Goal、W5、README 和 OpenSpec tasks 状态回写。

禁止事项：

1. 不改交易、钱包、账本写入链路。
2. 不新增生产 DDL、H2 schema 字段、Entity、Mapper 写入路径，除非用户另行确认 schema-backed report。
3. 不新增清分、清算、结算、出款、补事实、运营审批或生产迁移。
4. 不用差异报告驱动自动资金动作。
5. 不泄露外部账户、卡号、通道敏感字段、原始上下文或未授权证据。

撤销方式：用户未确认前直接关闭本文即可；确认后若 Red / Green 过程中发现越界，停止并回滚本 Grant 新增文件或以补偿提交撤销。

## 8. 验证方案和 Review

TDD：确认后先写 `ReconciliationDifferenceReportApplicationServiceTests` 首个 Red，证明当前缺少报告查询服务或服务不能解释对象级阻断差错；再做最小 Green。首个 Red 推荐复用 `ReconciliationDifferenceApplicationService` 创建对象级差错、复用 `ClearingSettlementGateConsumerService` 或 `ReconciliationGateApplicationService` 获取 gate 证据，再调用报告服务断言报告解释完整且无账务事实副作用。

Review：完成 Green 后按问题优先 CR 检查业务语义、模块边界、公共契约、敏感字段、只读事务、失败无副作用、测试断言和 Not Done。

Refactor：只有在报告 DTO 或测试支撑出现真实重复、影响可读性或不符合现有模式时做小范围重构；不得顺手重构差错状态机、gate、清结算或 ledger。

AI 产物复核：检查是否引入不存在的 Mapper、错误包名、未被测试覆盖的字段、过度泛化 DTO、直接读取上下文原文或把报告误做成事实写入。

验证命令：

```bash
just test-one ReconciliationDifferenceReportApplicationServiceTests tests
just test-reconciliation
just compile
just pmd
git diff --check
```

静态检查：PMD 必须通过；若私有仓库、缓存、凭据或网络导致依赖解析失败，需区分环境问题与代码问题。

发布和风险：本 Grant 不发布生产能力；后续若接入运营后台、导出、权限、告警、灰度或 Runbook，必须另起 production-change 评审。回滚策略以 Git revert 或补偿提交为准，不重写历史。

## 9. 可复制确认文本

```text
Execution Grant：GSD2-B7-RECON-DIFFERENCE-REPORT-001
确认新增对账差异报告最小只读查询能力，承接已提交的对象级 Gate 和清算 / 结算 consumer 证据，写入范围限 reconciliation-face 查询契约、reconciliation-impl 只读聚合实现、目标服务流测试和状态文档回写；不授权生产 DDL、完整清结算、出款、补事实、运营审批、治理归档、报表导出、生产发布或 Git 提交。
```

## 10. Grant 消费预检清单

用户确认 `Execution Grant：GSD2-B7-RECON-DIFFERENCE-REPORT-001` 后，资深架构师进入编码前先消费本清单。任一项不满足时停止在预检阶段，不写 Java、测试、DDL/H2 schema 或公共契约。

| 检查项 | 通过口径 | 不通过处理 |
| --- | --- | --- |
| 授权文本 | 用户明确确认第 9 节可复制文本，且未扩大为批次报告、运营后台、导出、补事实或完整清结算。 | 回到用户确认单一 Grant。 |
| 工作树 | `git status --short` 中没有影响 `reconciliation-*`、`tests`、TDD/OpenSpec 状态文件的未归属变更；若有，只能先分类或停止。 | 停止并说明冲突文件。 |
| 当前基线 | 最近提交至少包含 `0d3f68dc feat: 补齐清算结算对账准入消费` 和本确认包。 | 重新读取当前 Git / docs / OpenSpec 后再决策。 |
| 首个 Red | 默认选择 `B7-REPORT-RED-001`，只证明对象级未闭环差错可以生成只读报告且无资金副作用。 | 若当前代码已满足，改选 `B7-REPORT-RED-002` 或 `B7-REPORT-RED-004`。 |
| 写入范围 | 仅允许 reconciliation-face 报告契约、Request、DTO、枚举，reconciliation-impl 只读实现和 mapper 只读查询，目标测试和状态文档回写。 | 任一需求超出范围时另起 Grant。 |
| 验证顺序 | 目标测试先行，再 `test-reconciliation`、`compile`、`pmd`、`git diff --check`。 | 验证失败且无法在授权范围内修复时停止。 |
| Git 策略 | 默认 `summary_only`；如需提交，必须在用户另行确认提交范围后执行。 | 未确认前不得 `git add` / `git commit`。 |

## 11. Grant 消费运行卡

| 阶段 | 动作 | 通过口径 | 停止条件 |
| --- | --- | --- | --- |
| Pick | 选择 `B7-REPORT-RED-001` 作为首个 Red。 | Red 聚焦单笔差异报告，不触碰批次报告、导出或运营后台。 | 需要 report table、生产 DDL 或不可篡改快照时停止。 |
| Red | 新增 `ReconciliationDifferenceReportApplicationServiceTests`。 | 当前缺少报告服务或无法解释对象级阻断差错时失败。 | Red 必须修改差错状态机、gate 语义或清结算事实时停止。 |
| Green | 新增最小报告查询契约、Request、DTO、完整性枚举、只读 mapper 查询和只读实现。 | 能按 `tenantId + differenceSn` 返回报告，并解释差错、阻断对象、处理动作、重跑和 gate 摘要。 | 需要写交易、route、posting、LedgerEntry、余额投影、清算或结算事实时停止。 |
| Review | 做问题优先 CR。 | 检查模块边界、公共契约、DTO 字段、敏感字段过滤、只读事务和失败无副作用。 | 发现报告实现反写事实或泄露敏感上下文时停止。 |
| Verify | 运行目标测试、reconciliation 分组、compile、pmd 和 diff。 | 命令通过，或环境问题被区分并复跑。 | 验证失败且无法在授权范围内修复时停止。 |
| Handoff | 回写本文、LWT Goal、W5、README 和 OpenSpec tasks。 | Done / Not Done / 验证命令 / 下一 owner 清楚。 | 需要 Git、发布、生产迁移或专业确认时停止。 |

最小断言清单：

1. 报告查询前后 ledger transaction、posting、LedgerEntry、余额投影、清算和结算事实数量不变。
2. 对象级差错展示 `blockingObjectType / blockingObjectSn`；历史类型级差错展示保守阻断或完整性提示。
3. 已回链处理动作时展示 action type、幂等键、原始事实引用、处理事实引用和证据引用。
4. 已重跑时展示重跑流水、是否对平、结果摘要和 gate 条件放行或阻断解释。
5. 缺处理证据、缺重跑结果或缺 gate 决策时返回完整性状态，不自动补写事实。
6. 不返回外部账户原文、完整卡号、通道敏感字段或任意 `contextVariables` 原文。

## 12. 停止条件和交接

人工确认点：

1. 需要新增生产 DDL、H2 schema 字段、独立 report table 或不可篡改快照。
2. 需要运营权限、报表导出、批量筛选、后台页面、告警或 Runbook。
3. 需要补事实、调账、冲正、清结算、出款或任何资金写入。
4. 需要外部规则、通道、法务、财务或合规专业确认。
5. 工作树出现未分类变更，或目标文件存在用户未提交变更且无法安全区分。

交接：

| 交接项 | 内容 |
| --- | --- |
| Product Context Card | 业务目标、用户价值、对象、流程、规则矩阵、运营数据、风险和验收已在第 2 至 4 节表达。 |
| Engineering Handoff Card | 推荐接口、Request、DTO、完整性枚举、Red 候选、预检清单、运行卡、验证命令、禁止范围和停止条件已在第 5 至 11 节表达。 |
| Production Loop Card | 本 Grant 不发布生产；发布、灰度、回滚、权限、告警和 Runbook 仍为 Not Done。 |
| 残余风险 | 批次报告、报告留存、导出、运营后台、治理归档、生产权限、完整清结算生命周期、补事实执行和专业规则确认仍未完成。 |

## 13. Grant 消费结果（2026-06-19）

用户已确认并消费 `Execution Grant：GSD2-B7-RECON-DIFFERENCE-REPORT-001`。本轮按 `B7-REPORT-RED-001` 进入 Red / Green：首个 Red 证明当前缺少对账差异报告查询契约、DTO、完整性枚举和只读实现；Green 新增最小只读报告查询能力，并纳入对账分组验证。

写入结果：

1. `reconciliation-face` 新增 `ReconciliationDifferenceReportApplicationService`、`GetReconciliationDifferenceReportRequest`、`ReconciliationDifferenceReportDTO` 和 `ReconciliationDifferenceReportCompleteness`。
2. `reconciliation-impl` 新增 `ReconciliationDifferenceReportApplicationServiceImpl`，按 `tenantId + differenceSn` 只读聚合差错事实、gate 决策、证据引用、完整性和安全过滤提示。
3. `ReconciliationDifferenceMapper` 新增不加锁的 `selectByDifferenceSn`，报告查询不复用 `FOR UPDATE`。
4. `tests` 新增 `ReconciliationDifferenceReportApplicationServiceTests`，覆盖对象级未闭环差错报告、gate 阻断解释、证据引用、报告视图开关、处理动作证据不完整、缺重跑结果、历史类型级差错缺 gate 决策和无账本事实副作用。
5. `Justfile` 的 `test-reconciliation` 已纳入 `ReconciliationDifferenceReportApplicationServiceTests`。

验证结果：

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `just test-one ReconciliationDifferenceReportApplicationServiceTests tests` | PASS | 沙箱内因 embedded Redis 端口绑定限制失败，已在非沙箱环境复跑通过；5 tests passed。 |
| `just test-reconciliation` | PASS | 沙箱内因 embedded Redis 端口绑定限制失败，已在非沙箱环境复跑通过；31 tests passed。 |
| `just compile` | PASS | Maven reactor compile 通过。 |
| `just pmd` | PASS | PMD 通过。 |
| `git diff --check` | PASS | 未发现空白或补丁格式问题。 |

Review 结论：

1. 报告服务定位为 reconciliation application 层只读解释视图，不写交易、route、posting、LedgerEntry、余额投影、清算或结算事实。
2. DTO 只返回事实引用、状态摘要和解释文本；敏感字段以安全提示呈现，不返回外部账户、卡号、通道敏感字段或上下文原文。
3. 完整性状态只服务人工判断和运营解释，不触发补事实、重跑、调账或清结算动作。
4. 本 Grant 不声明批次报告、导出、运营后台、生产权限、完整清结算生命周期、补事实执行、治理归档或生产发布 Done。
5. 本 Grant 只交付服务层 application 能力；不新增 Controller、HTTP/RPC 入口、页面、导出端点或外部适配入口。
