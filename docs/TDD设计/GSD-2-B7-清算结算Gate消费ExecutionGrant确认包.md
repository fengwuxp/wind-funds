# GSD-2 B7 清算结算 Gate 消费 Execution Grant 确认包

## 1. 文档定位

本文是 `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 的 Execution Grant 确认和消费记录，用于承接 B7 对账差错闭环、准入 gate 和出款 preflight 已完成后的对象级 Gate 基座补强。

本文不是 Git 授权，也不是完整清分、清算、结算、出款或补事实执行服务授权。本轮已按用户推荐选择 `scopeDecision=object-scope-schema-backed` 完成本地 Green；后续清算 / 结算真实消费方、B7 差异报告、补事实、生产迁移或提交仍需新的单一 Grant。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` |
| 原子任务 | 补齐对账 gate 的对象级阻断基座，作为清算 / 结算真实消费方后续接入前置能力。 |
| 所属阶段 | GSD-2 / B7 reconciliation gate consume / object scope green。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| 当前状态 | `OBJECT_SCOPE_SCHEMA_BACKED_GREEN_SUMMARY_ONLY` |
| Git / code baseline | 当前已提交代码基线为 `8ae0f90f docs: 收敛B7对象级阻断交接包`；本轮对象级阻断字段、H2 schema、Mapper 查询和 gate 目标测试已完成本地 Green，但尚未获得 Git 提交授权。 |
| Owner | AI Native 负责 Goal、Loop、状态和停止条件；产品架构专家负责清算 / 结算业务语义、阻断对象和验收边界；资深架构师负责接口、schema 决策、TDD、实现路径和验证命令。 |
| Wave 边界 | 本确认包只消费 B7 对象级 Gate 基座一个原子任务；不得并行推进完整清分、清算确认、结算锁定、出款提交、追偿、补事实执行、B7 差异报告、wallet 预交易快照、VCC、全球账户或 Spend Rule。 |
| 执行顺序 / 依赖关系 | 依赖 `GSD2-B7-RECON-GATE-CONSUME-001`、`GSD2-B7-RECON-GATE-CONSUME-002` 和 B5-003 已完成；本轮已消费 `scopeDecision=object-scope-schema-backed`。 |
| 授权范围 | 已完成差错对象级阻断字段、H2 schema、Mapper、gate 查询和目标测试；未授权完整清结算消费方、补事实或 Git。 |
| 写入范围 | 本文、LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、reconciliation-face、reconciliation-impl、transaction、ledger、wallet、tests、AGENTS.md、B7 历史准入卡和最近 Git 提交。 |
| Git 策略 | `summary_only`。本轮未授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

## 2. 产品裁决

产品裁决：清算 / 结算消费方应接入对账 gate，阻断粒度已按对象级 schema-backed 方案落地为前置基座。

现有 B7 gate 已能按 `blockingScope` 对 `CLEARING`、`SETTLEMENT`、`PAYOUT` 范围内的未闭环差错做阻断，并已被出款 preflight 消费。下一步的业务问题不是“能否调用 gate”，而是“清算候选或结算单应该被全类型阻断，还是只被命中自身对象的差错阻断”。

2026-06-19 源码 Gap Audit 曾确认：`CheckReconciliationGateRequest.gateObjectSn` 已是准入消费对象必填字段，但差错事实侧缺少阻断对象类型和对象流水。用户随后确认推荐方案，本轮已按 `scopeDecision=object-scope-schema-backed` 补齐 `blockingObjectType / blockingObjectSn`、H2 schema、Mapper 对象级查询和 gate 目标测试。

历史方案说明：

1. 若目标是快速复用现有能力，可选择类型级阻断：任一命中 `CLEARING` 或 `SETTLEMENT` 的未闭环差错都会阻断该租户同类型准入。优点是低风险、无需 schema；缺点是误阻断范围大，只适合保守 MVP 或早期内控。
2. 若目标是生产可用，应选择对象级阻断：差错需要能声明它阻断的清算候选、清算批次、结算单或出款单；gate 按消费对象流水命中差错。优点是可解释、可运营、可局部放行；缺点是需要公共契约、DDL/H2 schema、Entity、Mapper、迁移和兼容设计。

业务目标：清算候选生成和结算锁定前，能够消费对账差错状态、阻断范围、处理动作和重新对账结果，给出阻断、条件放行或通过决策，并证明不会生成资金事实或账本事实。

非目标：

1. 不创建清分明细、清算批次、结算单或出款单。
2. 不确认清算、不锁定结算、不提交出款。
3. 不执行补事实、调账、核销、追偿或资金修复。
4. 不新增运营后台、审批流、权限模型或生产迁移脚本，除非后续 Grant 另行确认。
5. 不把对账差错处理动作当作资金事实。

## 3. 能力地图和对象边界

| 能力域 | 目标口径 | 本 Grant 准备处理 | 不做范围 |
| --- | --- | --- | --- |
| 清算准入 | 清算候选生成或清算确认前必须检查未闭环差错。 | 设计清算消费方如何调用 `ReconciliationGateApplicationService` 并解释阻断结果。 | 不生成清算候选或确认清算批次。 |
| 结算准入 | 结算锁定或结算确认前必须检查未闭环差错。 | 设计结算消费方如何调用 gate 并返回可解释状态。 | 不锁定结算单或生成出款。 |
| Gate 决策 | 只读读取对账差错事实并返回阻断或条件放行。 | 明确 `gateObjectSn` 是解释字段还是对象级命中键。 | 不写交易、账本、余额投影、交易投影或治理重放。 |
| 差错事实 | 差错状态、阻断范围、处理动作、重跑结果是准入依据。 | 消费现有 `ReconciliationDifference`，必要时在对象级方案中扩展阻断对象字段。 | 不创建 B7 差异报告，不执行补事实命令。 |

业务对象：

- ReconciliationGateDecision：清算 / 结算消费方的准入判断，不是资金事实。
- ReconciliationDifference：对账差错事实，当前已有 `blockingScope`、状态、处理动作、重跑结果和证据引用。
- Clearing Gate Consumer：清算候选或清算确认前的 gate 消费方。本 Grant 只定义准入边界，不实现完整清算对象。
- Settlement Gate Consumer：结算锁定或结算确认前的 gate 消费方。本 Grant 只定义准入边界，不实现完整结算对象。
- Gate Object：消费对象流水，当前由 `CheckReconciliationGateRequest.gateObjectSn` 承载。

### 3.1 业务流程和状态

主流程：

1. 清算或结算消费方准备执行前置动作，构造 `CheckReconciliationGateRequest`。
2. gate 根据租户、消费对象类型和阻断粒度读取差错事实。
3. 若存在未闭环或重跑未对平差错，返回 `BLOCKED`，并携带差错、责任方、证据和下一步解释。
4. 若命中范围内差错均已处理且重新对账已对平，返回 `CONDITIONALLY_PASSED`，消费方可继续自己的前置流程，但本 Grant 不负责创建后续资金事实。
5. 若没有命中差错，返回 `PASSED`。

异常流程：

- `gateObjectSn` 为空：拒绝请求，不产生任何事实。
- 差错存在但处理动作缺失或重跑未对平：继续阻断。
- `actionType`、幂等键或原始事实引用漂移：以既有 B7 动作守卫结果为准，不能释放准入。
- 对象级阻断字段缺失：在对象级方案下返回不完整或阻断；不得退化为静默通过。

状态口径：

- `BLOCKED`：存在未闭环、未处理、重跑未对平或对象粒度不完整的阻断差错。
- `CONDITIONALLY_PASSED`：命中范围内存在差错，但已处理且重新对账已对平。
- `PASSED`：未发现命中范围内差错。

人工兜底：人工只能补证据、处理差错、发起重新对账或按后续补事实专项流程处理；不得通过清算 / 结算消费方直接改账。

### 3.2 阻断粒度决策

| scopeDecision | 方案 | 写入影响 | 适用场景 | 风险 |
| --- | --- | --- | --- | --- |
| `type-scope-no-schema` | 沿用当前 `blockingScope` 类型级阻断。 | 可只补清算 / 结算消费方测试和最小服务，不改表。 | 保守 MVP、早期内控、差错量低且宁可误阻断。 | 任一 CLEARING / SETTLEMENT 差错会阻断同类型所有对象，可用性差。 |
| `object-scope-schema-backed` | 新增阻断对象类型和对象流水，gate 按对象命中。 | 需要公共契约、DDL/H2 schema、Entity、Mapper、DTO、测试和兼容读取。 | 生产运营、局部放行、多批次并行、需要解释“为什么这张单被阻断”。 | 改动范围更大，需要迁移和兼容策略。 |

消费结论：本轮已选择并完成 `object-scope-schema-backed`；`type-scope-no-schema` 只保留为历史备选，不再作为当前推荐或待确认项。

### 3.3 规则矩阵

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验收口径 |
| --- | --- | --- | --- | --- | --- |
| 未闭环差错阻断 | 清算 / 结算消费方执行前 | 命中阻断范围且状态非 `RESOLVED`，或 `lastRerunBalanced != true` 时阻断。 | P0 | B7-GATE | 返回 `BLOCKED`，且无交易、账本、清算或结算副作用。 |
| 已处理不等于放行 | 差错已有 actionType | 必须同时具备重新对账对平和稳定处理上下文。 | P0 | B7-GATE | 处理动作存在但重跑未对平仍阻断。 |
| 对象粒度准确 | 选择对象级方案 | 某对象的差错不得误阻断另一个清算候选或结算单。 | P0 | B7-GATE-OBJECT | Red 覆盖同类型不同对象不互相阻断。 |
| 只读 gate | 任一 gate check | 只能读取差错事实和返回解释。 | P0 | B7-GATE | 查询前后交易、route、ledger、projection、清算和结算事实数量不变。 |
| 解释可用 | 返回阻断或条件放行 | 必须包含差错流水、责任方、证据引用、阻断原因和下一步动作。 | P1 | B7-GATE | 清算 / 结算调用方可展示或记录可解释状态。 |

## 4. 推荐服务边界和接口契约

现有公共契约：

- `ReconciliationGateApplicationService#checkGate(CheckReconciliationGateRequest, WindOperator)`。
- `CheckReconciliationGateRequest.tenantId`、`gateObjectType`、`gateObjectSn`。
- `ReconciliationGateObjectType.CLEARING / SETTLEMENT / PAYOUT`。
- `ReconciliationGateDecisionDTO` 和 `ReconciliationGateBlockingDifferenceDTO`。

消费后契约口径：

1. `CheckReconciliationGateRequest.gateObjectType / gateObjectSn` 继续表达准入消费对象。
2. `CreateReconciliationDifferenceRequest.blockingObjectType / blockingObjectSn` 表达差错阻断对象；两者必须同时填写或同时为空。
3. `ReconciliationDifferenceDTO` 和 `ReconciliationGateBlockingDifferenceDTO` 返回阻断对象字段，便于运营解释和审计定位。
4. `ReconciliationDifferenceMapper.selectByGateObject` 支持 `blockingScope + blockingObjectType + blockingObjectSn` 精确命中，并兼容历史 `blockingObjectType / blockingObjectSn` 为空的类型级差错。
5. H2 `t_reconciliation_difference` 已补阻断对象字段和对象级索引；生产 DDL、迁移、灰度和回滚仍是后续 Not Done。

当前实现边界：

| 契约项 | 当前实现 | 后续 Not Done |
| --- | --- | --- |
| Check request | 复用现有 `gateObjectType / gateObjectSn` 作为消费对象命中键。 | 清算 / 结算真实消费方服务仍需新 Grant。 |
| Difference request | 已新增 `blockingObjectType / blockingObjectSn`。 | 生产 API 兼容说明和对外接入文档可在真实消费方 Grant 中补齐。 |
| Entity / schema | H2 schema、Entity 和对象级索引已补。 | 生产 DDL、迁移、回滚和数据回填未授权。 |
| Mapper | 已演进为对象级精确命中 + 历史类型级保守命中。 | 大批量查询性能、生产索引和 explain 仍需发布前评审。 |
| DTO | 已返回阻断对象字段。 | 运营后台展示、导出和权限仍需独立设计。 |
| 消费方 | gate service 可按对象精确判断。 | 完整清算候选、结算单、出款生命周期和差异报告仍未实现。 |

## 5. 角色协作 Loop 三卡交接

### 5.1 Product Context Card

| 字段 | 内容 |
| --- | --- |
| 业务目标 | 让清算候选生成和结算锁定前可以消费对账差错状态，防止重大差错未闭环时释放资金或进入后续资金流程。 |
| 目标用户 / 验收方 | 财务、运营、风控、清结算 owner、测试和研发；生产前仍需财务、合规、SRE 或 DBA 按范围确认。 |
| 核心对象 | ReconciliationDifference、ReconciliationGateDecision、Clearing Gate Consumer、Settlement Gate Consumer、Gate Object。 |
| 关键不变量 | Gate 只读；未闭环差错不得释放清算 / 结算；处理动作不等于放行；对象级方案不得误阻断其他对象。 |
| 主流程 | 清算 / 结算消费方调用 gate，读取差错状态、处理动作和重跑结果，返回阻断、条件放行或通过。 |
| 异常路径 | 缺消费对象、差错未闭环、重跑未对平、动作上下文漂移、对象粒度缺失时默认阻断或拒绝。 |
| 验收种子 | `B7-CLSSET-GATE-RED-001` 清算阻断、`B7-CLSSET-GATE-RED-002` 结算阻断、`B7-CLSSET-GATE-RED-003` 对象级不误阻断、`B7-CLSSET-GATE-RED-004` 无资金副作用。 |
| 非目标 | 不做完整清分、清算确认、结算锁定、出款、追偿、补事实执行、差异报告、运营后台或生产迁移。 |
| 风险和待确认 | `scopeDecision` 已确认并消费；生产迁移、数据回填、清算 / 结算真实消费方和运营后台仍需后续独立确认。 |

### 5.2 Engineering Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前阶段 | `OBJECT_SCOPE_SCHEMA_BACKED_GREEN_SUMMARY_ONLY`。 |
| 下一 owner | 用户确认新的单一 Grant 后交给资深架构师进入清算 / 结算真实消费方或 B7 差异报告的 TDD / 编码实现；AI Native 继续维护 Goal、Loop、状态账本和停止条件。 |
| 入口契约 | 首选复用 `ReconciliationGateApplicationService`；是否新增清算 / 结算专用 application consumer 取决于首个 Red。 |
| 推荐决策 | 本 Grant 已按 `scopeDecision=object-scope-schema-backed` 消费；下一轮不再确认阻断粒度，直接从消费方或报告能力中重新开 Grant。 |
| 源码收敛证据 | 差错登记、差错 DTO、阻断 DTO、Entity、H2 schema 和 Mapper 已显式承载阻断对象字段；gate 查询支持对象精确命中和历史类型级保守命中。 |
| 源码锚点 | `ReconciliationGateApplicationServiceImpl`、`ReconciliationDifferenceMapper.selectByGateObject`、`ReconciliationDifference`、`CreateReconciliationDifferenceRequest`、`CheckReconciliationGateRequest`、`ReconciliationGateApplicationServiceTests`、`ReconciliationDifferenceApplicationServiceTests`。 |
| 写入上限 | 本轮只写对象级 Gate 基座；后续清算 / 结算 consumer、差异报告、补事实、生产迁移和 Git 需新 Grant。 |
| TDD 切入 | 已覆盖同类型不同对象不误阻断、结算对象精确阻断、复合阻断范围下对象类型精确匹配、对象级差错幂等和字段成对校验。 |
| 验证命令 | `just test-one ReconciliationGateApplicationServiceTests tests`、必要时新增清算 / 结算目标测试；分组用 `just test-reconciliation`，收口用 `just compile`、`just pmd`、`git diff --check`。 |
| Git 策略 | 当前仍是 `summary_only`；只有用户另行授权提交，且验证通过、工作树未混入无关改动时，才可提交。 |
| 停止条件 | 需要完整清分、清算、结算、出款、补事实执行、生产迁移、外部规则最终确认或对象级 schema 但 Grant 未列明时停止。 |

### 5.3 Production Loop Card

| 字段 | 内容 |
| --- | --- |
| 生产可用能力锚点 | 清算 / 结算消费方必须能在释放后续资金流程前解释是否存在未闭环对账差错。 |
| 安全边界 | gate 决策不返回敏感外部账户原文，不写资金事实，不调用交易或账本写入。 |
| 可靠性边界 | gate check 幂等、只读、可重跑；差错事实缺失或对象粒度不完整时不得静默放行。 |
| 对账边界 | 差错处理、重跑、补事实和差异报告仍属于 B7 独立事实源；本 Grant 只消费差错状态。 |
| 可观测和审计 | 最小版本返回决策、检查人、检查时间、差错流水、证据引用和解释；生产版本需另行补告警、Runbook、权限和导出边界。 |
| 发布前门禁 | 目标测试、reconciliation 分组、编译、PMD、diff 检查通过；对象级方案还需 schema 兼容和迁移评审。 |
| 回滚 / 降级 | 类型级方案可回滚调用侧；对象级方案需数据库迁移回滚和兼容读取策略。 |
| 残余风险 | 完整清结算生命周期、差异报告、补事实命令、运营审批、生产迁移、SLO、告警和外部规则核验仍为 Not Done。 |

## 6. Red 候选和验收矩阵

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `B7-CLSSET-GATE-RED-001` | 清算消费方遇到未闭环 `CLEARING` 差错是否被阻断。 | 未闭环差错不得释放清算。 | 返回 `BLOCKED`、差错流水、责任方、证据和解释。 | 不生成清算候选、交易、route、posting、LedgerEntry、余额或投影副作用。 | 清算 gate 目标测试或 `ReconciliationGateApplicationServiceTests`。 | `just test-one ReconciliationGateApplicationServiceTests tests`。 | 需要完整清算对象时停止。 |
| `B7-CLSSET-GATE-RED-002` | 结算消费方遇到未闭环 `SETTLEMENT` 差错是否被阻断。 | 未闭环差错不得释放结算。 | 返回 `BLOCKED`、差错流水、责任方、证据和解释。 | 不生成结算单、出款单、交易、route、posting 或 LedgerEntry。 | 结算 gate 目标测试。 | 目标测试 + `just test-reconciliation`。 | 需要完整结算锁定时停止。 |
| `B7-CLSSET-GATE-RED-003` | 对象级方案下，某清算候选或结算单差错是否不会误阻断另一个对象。 | 对象级阻断必须准确命中。 | 对象 A 被阻断并返回 `blockingObjectType / blockingObjectSn`，对象 B 不被误阻断或仅受类型级历史差错影响。 | 不用类型级全阻断冒充对象级能力。 | schema-backed gate 测试。 | 目标测试 + `just test-reconciliation`。 | 未授权 DDL/H2 或公共契约时停止。 |
| `B7-CLSSET-GATE-RED-004` | 差错已处理但重跑未对平时是否继续阻断。 | 处理动作不等于放行。 | 返回 `BLOCKED`，解释重跑未对平。 | 不因 actionType 存在就条件放行。 | gate 回归测试。 | `just test-one ReconciliationGateApplicationServiceTests tests`。 | 需要改变差错状态机时停止。 |
| `B7-CLSSET-GATE-RED-005` | 差错已处理且重跑对平后是否条件放行。 | 放行必须基于重新对账证据。 | 返回 `CONDITIONALLY_PASSED` 和证据摘要。 | 不创建后续清算 / 结算事实。 | gate 回归测试。 | `just test-one ReconciliationGateApplicationServiceTests tests`。 | 需要真实清算确认时停止。 |

## 7. 候选写入范围和禁止事项

确认 `Execution Grant：GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 后，必须同时指定 `scopeDecision`。

### 7.1 `scopeDecision=type-scope-no-schema`

允许写入：

1. 清算 / 结算消费方最小 application consumer 或目标测试。
2. `ReconciliationGateApplicationServiceTests` 或新目标测试补充 `CLEARING` / `SETTLEMENT` 阻断和条件放行。
3. 文档和 OpenSpec 状态回写。

禁止写入：

1. 不新增 DDL/H2、Entity、Mapper、公共 DTO 字段。
2. 不宣称对象级阻断生产可用。
3. 不实现完整清算 / 结算生命周期。

### 7.2 `scopeDecision=object-scope-schema-backed`

允许写入上限：

1. `CreateReconciliationDifferenceRequest`、DTO、Entity、H2 schema 和 Mapper 增加阻断对象字段。
2. gate 查询按 `blockingScope + gateObjectSn` 或兼容规则命中。
3. 目标测试覆盖同类型不同对象不误阻断。
4. 文档和 OpenSpec 状态回写。

必须补充：

1. 历史 `blockingScope` 类型级差错的兼容读取规则。
2. 对象字段为空时的保守阻断或兼容行为。
3. 索引、迁移和生产回滚说明；若只改 H2，交付中必须明确生产迁移 Not Done。

禁止事项：

1. 不新增完整清算批次、结算单、出款单或追偿对象。
2. 不让 gate 服务写交易、账本、余额投影或交易投影。
3. 不执行补事实、核销、调账或治理重放。
4. 不把差错处理动作当成资金事实。
5. 不绕过用户确认扩大到 B7 差异报告、运营后台、外部规则或生产迁移。

### 7.3 对象级字段最小口径

对象级方案不引入完整清算批次、结算单或出款对象模型，只在对账差错事实上补“这个差错阻断哪个消费对象”的最小字段。

| 字段 | 建议落点 | 口径 | 兼容规则 |
| --- | --- | --- | --- |
| `blockingObjectType` | `CreateReconciliationDifferenceRequest`、`ReconciliationDifferenceDTO`、`ReconciliationGateBlockingDifferenceDTO`、`ReconciliationDifference`、H2 schema。 | 阻断对象类型，取值对齐 `ReconciliationGateObjectType`，例如 `CLEARING`、`SETTLEMENT`、`PAYOUT`；不是 ledger subject，也不是清算 / 结算实体类型枚举。 | 历史差错为空时视为类型级阻断。新对象级差错若用于清算 / 结算 gate，应显式填写。 |
| `blockingObjectSn` | 同上。 | 阻断对象流水，例如清算候选流水、结算单流水或出款单流水；与 gate request 的 `gateObjectSn` 进行匹配。 | 历史差错为空时视为类型级阻断。新对象级差错若已知消费对象，应显式填写。 |
| 查询索引 | H2 schema 和生产迁移方案候选。 | 建议按 `tenant_id + blocking_scope + blocking_object_type + blocking_object_sn + status` 建立查询能力；本确认包只授权 H2 和代码最小闭环，生产迁移仍是 Not Done。 | 未做生产迁移前，交付说明必须标明生产 DDL / 回滚 / 灰度待补。 |

命名裁决：不把 `gateObjectSn` 直接写入差错表，避免把“消费方请求对象”与“差错阻断对象”混成同一个语义；差错侧使用 `blockingObjectType / blockingObjectSn`，gate 侧继续使用 `gateObjectType / gateObjectSn`。

### 7.4 对象级兼容命中规则

| 场景 | 命中规则 | gate 决策 |
| --- | --- | --- |
| 对象级精确命中 | 同租户、`blockingScope` 包含当前 `gateObjectType`，且 `blockingObjectType == gateObjectType`、`blockingObjectSn == gateObjectSn`。 | 未闭环或重跑未对平时 `BLOCKED`；已处理且重跑对平时 `CONDITIONALLY_PASSED`。 |
| 同类型不同对象 | 同租户、`blockingScope` 包含当前 `gateObjectType`，但 `blockingObjectSn != gateObjectSn`。 | 不阻断当前对象；Red 必须证明不会误阻断。 |
| 历史类型级差错 | `blockingObjectType` 或 `blockingObjectSn` 为空，但 `blockingScope` 命中当前 gate 类型。 | 保守阻断，避免历史差错因升级对象级查询被静默放行；解释中需体现类型级历史阻断。 |
| 空对象请求 | `CheckReconciliationGateRequest.gateObjectSn` 为空。 | 继续拒绝请求，不把缺对象的消费方请求降级成类型级全局检查。 |

Mapper 目标：对象级 Grant 下，原 `selectByBlockingScope` 应演进为能表达“精确对象命中 + 历史类型级保守命中”的只读查询；不得在 gate 查询中写差错、交易、账本或投影事实。

## 8. 验证矩阵

| 验证层 | 命令或方式 | 完成条件 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md` | Task、Owner、范围、验证、TDD、Review、Execution Grant、人工确认和交接字段齐全。 |
| GSD Wave 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md` | Wave、上下文账本、禁止事项、验证矩阵和 handoff 字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md` | 业务目标、能力地图、对象、流程、规则、数据审计、风险和验收齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md` | 背景目标、现状、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001|scopeDecision|object-scope-schema-backed|type-scope-no-schema" docs openspec` | LWT Goal、W5、README 和 OpenSpec tasks 能追踪到本文。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |

确认后建议验证命令：

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one ReconciliationGateApplicationServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-reconciliation
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just pmd
git diff --check
```

## 9. 可复制确认文本

低风险类型级版本：

```text
Execution Grant：GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001
scopeDecision=type-scope-no-schema
确认只补清算/结算消费方接入和目标测试，不新增公共契约、DDL/H2、Entity、Mapper，不声明对象级阻断生产可用。
```

生产可用对象级版本：

```text
Execution Grant：GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001
scopeDecision=object-scope-schema-backed
确认允许在 reconciliation 差错契约、H2 schema、Entity、Mapper 和 gate 查询中补对象级阻断字段与测试；仍不授权完整清分、清算、结算、出款、补事实执行、运营后台、生产迁移或 Git 提交。
```

## 10. Grant 消费预检清单

| 检查项 | 通过条件 |
| --- | --- |
| 用户授权 | 用户复制确认第 9 节其中一个 Grant 文本，且 `scopeDecision` 明确。 |
| 工作树 | `git status --short` 已复核，当前工作树只允许包含本 Grant 可解释的状态维护；B5-003 已提交固化，不再作为工作树冲突来源。 |
| 设计入口 | 已读取 PRD 03、系分 03、B7 Round0、LWT Goal、W5 和 OpenSpec tasks。 |
| 源码锚点 | 已复核 `ReconciliationGateApplicationServiceImpl`、Mapper、Entity、Request、DTO 和目标测试。 |
| 首个 Red | 已按 `scopeDecision` 选择 `B7-CLSSET-GATE-RED-001` 或对象级 `B7-CLSSET-GATE-RED-003`。 |
| 写入范围 | 与第 7 节一致，没有混入完整 B7 或 P2 业务。 |
| 验证顺序 | 目标测试 -> `test-reconciliation` -> `compile` -> `pmd` -> `git diff --check`。 |
| Git 策略 | 未得到单独 Git 授权时只总结，不提交。 |

## 11. Grant 消费运行卡

| 阶段 | 动作 | 通过口径 | 停止条件 |
| --- | --- | --- | --- |
| Red | 写一个清算 / 结算 gate 目标 Red。 | Red 能证明当前缺口，且 forbidden facts 明确。 | Red 需要完整清算 / 结算对象时停止。 |
| Green | 最小实现只满足 Red。 | gate 决策正确，查询只读，无资金副作用。 | 需要扩大到补事实、差异报告或生产迁移时停止。 |
| Refactor | 收敛命名、注释、DTO 字段和测试支撑。 | 不改变授权范围，不引入无主抽象。 | 需要新增通用规则引擎或运营后台时停止。 |
| Verify | 运行目标测试、分组、compile、pmd 和 diff。 | 命令通过，或环境问题被区分并复跑。 | 验证失败且无法在授权范围内修复时停止。 |
| Handoff | 回写 LWT Goal、W5、README、OpenSpec 和确认包结果。 | Done / Not Done / 验证命令 / 下一 owner 清楚。 | 需要 Git、发布、生产或专业确认时停止。 |

## 12. Execution Grant 消费结果

本轮已按用户推荐选择消费 `scopeDecision=object-scope-schema-backed`。落地范围只覆盖对账差错对象级阻断命中键和 gate 查询能力，不实现完整清算候选、清算批次、结算单、出款单、追偿对象或补事实执行服务。

完成内容：

1. `CreateReconciliationDifferenceRequest`、`ReconciliationDifferenceDTO`、`ReconciliationGateBlockingDifferenceDTO` 和 `ReconciliationDifference` 增加 `blockingObjectType / blockingObjectSn`。
2. H2 `t_reconciliation_difference` 增加阻断对象字段和对象级查询索引；生产 DDL、迁移、灰度和回滚仍为 Not Done。
3. `ReconciliationDifferenceMapper` 的 gate 查询从类型级范围查询演进为“对象级精确命中 + 历史类型级保守命中”。
4. `ReconciliationGateApplicationService` 返回阻断对象字段，清算 / 结算 gate 测试覆盖同类型不同对象不误阻断、结算对象精确阻断、复合阻断范围下对象类型精确匹配和无账本副作用。
5. `ReconciliationDifferenceApplicationServiceTests` 覆盖对象级差错创建幂等和阻断对象字段成对校验，避免半截对象命中键落库。

已验证：

1. `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one ReconciliationGateApplicationServiceTests,ReconciliationDifferenceApplicationServiceTests tests`
2. `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-reconciliation`
3. `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile`
4. `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just pmd`

验证说明：目标 Spring 测试在 sandbox 内曾因 embedded Redis 本地端口绑定权限失败；已按项目规则在非 sandbox 环境重跑通过。`git diff --check` 在文档回写后收口执行。

交付边界：本轮不能声明完整清分、清算、结算、出款或 B7 生产闭环 Done；只能声明“对账差错的对象级阻断键和 gate 精确命中能力已本地 Green”。清算 / 结算真实消费方服务、生产迁移脚本、运营审批、差异报告、补事实执行和告警 Runbook 仍需后续单一 Grant。

## 13. Not Done 和残余风险

本确认包完成后仍不能声明以下能力 Done：

1. 完整清分、清算、结算、出款和追偿生命周期。
2. B7 差异报告、补事实命令执行服务、运营审批流和职责分离。
3. 生产 DDL 迁移、回滚、灰度、SLO、告警和 Runbook。
4. 外部卡组织、银行、通道、ACH、SWIFT、FX、税务、会计、法务或合规最终确认。
5. wallet 全量生产 Done、VCC facade、全球账户业务能力和 Spend Rule 控制闭环。

下一推荐确认包：[GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md](GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md) 已把 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 收敛为待确认切片。若继续 B7，应优先确认该服务，用真实清算 / 结算准入消费方证明“对象级 gate 基座可以被生产链路消费”；未确认前不得复用本 Grant 继续写 Java、测试、DDL/H2 schema、完整清结算或 Git。
