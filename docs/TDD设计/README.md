# 支付资金底座测试驱动设计

## 目录定位

本目录是支付资金底座测试驱动设计入口。完整设计包入口见 [../README.md](../README.md)。它承接产品设计、DSL 设计、系分设计和真实代码模块，把“产品必须证明什么”转成“代码测试必须覆盖哪些真实执行路径”。

本目录不替代产品验收矩阵，也不替代系分设计。它的职责是把测试对象、测试层级、模块服务、用例场景、断言证据和验证命令统一起来。

业务接入方需要把产品验收、DSL case、系分入口和目标测试资产整理成接入验收矩阵时，先读 [../用户接入指南/README.md](../用户接入指南/README.md)。用户接入指南只说明接入方如何准备验收材料，测试权威口径仍以本目录为准。

## 背景和目标

背景：支付资金底座的产品目标、DSL 契约、系分对象和真实代码模块必须被同一套测试证据证明；否则容易出现“文档看似闭环，但代码只证明接口不报错或状态变化”的交付风险。

目标：把 PRD 的验收条件、DSL 的 caseId、系分的服务入口和代码的真实执行路径映射为可执行或可转化的测试资产，证明每个资金变化都满足状态正确、金额闭合、账目清晰、账本可追溯、投影只读、幂等无副作用和审计可解释。

业务目标：让支付资金底座的资金变化具备可解释、可核对、可回归的测试证据；用户价值是让产品、运营、财务和研发能从同一组 TDD 证据判断能力是否具备工程落地条件。

非目标：本目录不定义产品功能，不替代 DSL 字段设计，不决定服务接口或表结构，不授权修改生产代码、测试代码、DDL/H2 schema 或运行时配置。进入编码仍以工程任务边界为准。

成功标准：任一工程任务都能从 `AC-*`、`DSL-*`、`TDD-*`、`RED-*` 反查到目标测试资产、真实执行路径、核心断言、验证命令、未覆盖项和残余风险；无法证明的范围必须写成未覆盖范围、带条件通过或不适用原因。

## 最小交付约规

TDD 默认先证明最小交付切片，而不是一次性铺大全量目标态。每个测试设计任务只覆盖最小闭环所需的正向、逆向、异常、幂等和红线路径；其他业务扩展、外部协议、清结算深水区、资金数据治理、运营后台和报表能力必须写成未覆盖范围或独立能力域。

最小交付测试必须优先证明资金不变量：状态正确、route snapshot 可追溯、posting plan 平衡、LedgerEntry 可核对、余额投影正确、重复请求无副作用、失败不产生半截事实。若一个测试只能证明接口不报错、状态变化或数量变化，不能作为最小交付完成证据。

## 阅读顺序

本 README 只作为 TDD 设计入口。工程规划、状态账本、工程边界确认包和阶段推进材料已清理；当前只保留稳定测试设计口径。

| 顺序 | 文档 | 作用 |
| --- | --- | --- |
| 1 | [支付资金底座测试驱动设计.md](支付资金底座测试驱动设计.md) | 定义测试驱动设计原则、模块测试矩阵、场景用例、红线用例、目标测试资产和执行门禁。 |

## 契约输入

| 输入 | 用途 |
| --- | --- |
| `docs/产品设计` | 产品目标、使用者、能力地图、产品验收和红线。 |
| `docs/DSL设计` | 资金事实、指令、路由、账务计划、分录、投影和 JSON 契约用例。 |
| `docs/系分设计` | 模块边界、服务入口、流程、状态、表设计、观测、安全和测试专项。 |
| `core`、`wallet-*`、`transaction-*`、`ledger-*`、`tests` | 真实代码执行路径、目标测试包名和测试支撑能力。 |

接口契约、入参、出参、错误码、幂等键、兼容边界和替身边界必须从 DSL、系分和现有代码共同确认；TDD 不单独发明公共契约。若测试设计发现契约缺口，只能提出差距、Red 和工程任务写入建议，不能把测试夹具当成生产契约变更。

凡涉及资金变化的测试，参与方账户示例、账户类型、`normalBalanceSide`、借贷平衡、余额桶影响和失败红线必须回到 [../DSL设计/支付资金底座DSL承载层设计.md#51-资金场景借贷平衡与账务期望表](../DSL设计/支付资金底座DSL承载层设计.md#51-资金场景借贷平衡与账务期望表)；TDD 只把该表转成可执行断言，不从 PRD 摘要或系分说明二次推导账务口径。

## 测试基线约束

| 约束 | TDD 处理口径 |
| --- | --- |
| 真实执行路径 | 服务层流程测试优先使用真实 Spring Bean、H2 schema 和现有测试支撑能力；Mock/Fake/Recording 只用于外部系统、不可控依赖或明确端口边界。 |
| 数据方案 | 资金事实、路由、账务计划、账本分录、余额投影、对账对象和治理对象必须有可准备、可查询、可断言的数据形态。 |
| 事务边界 | 测试必须证明同事务内的 route、posting、entry 和投影派生边界，以及失败后的无副作用、补偿或重试结果。 |
| 一致性和对账 | 资金主链路证明账务平衡和投影一致；清结算与对账证明批次、差异、重跑和核销闭环；归档重放证明 Manifest、checkpoint、watermark 和差异报告。 |
| 可靠性 | 重试、并发、乱序、重复请求、外部非终态、任务续跑和范围锁必须有目标测试或明确不适用原因。 |
| 安全审计 | 权限、审批、证据最小化、敏感数据脱敏、规则待确认、导出边界和审计链必须能被 must-fail 或审计断言覆盖。 |
| 发布和回滚 | TDD 只给验证方案、回归范围、风险和待确认项；灰度、发布、回滚和上线审批由工程任务、生产变更和团队流程承接。 |

## 使用原则

1. 新增或修改资金能力前，先在 TDD 设计中找到对应用例；找不到时先补设计和验收口径。
2. 每个资金变化用例必须断言状态、余额桶、route snapshot、posting plan、ledger entry、投影和幂等。
3. 测试要覆盖真实内部执行路径；除外部通道、时间、ID、不可控依赖外，不应 Mock 内部核心组件。
4. 产品验收、DSL 不变量、系分模块边界和代码服务入口必须能互相追溯。
5. 授权测试命名优先使用“授权完成 / 已完成金额”，对应产品侧“授权结算 / 已结算金额”；商户清结算测试仍使用“结算锁定 / 出款 / SETTLEMENT 账目”。

## TDD 准入评估口径

TDD 设计的准入目标是证明设计已经能转成真实测试资产和验证命令。完整跨文档门禁见 [../README.md#设计准入评估总控](../README.md#设计准入评估总控)。

| 评估维度 | TDD 侧必须证明 | 阻断信号 |
| --- | --- | --- |
| 可用性 | 产品场景能转成可执行或可转化的测试用例，包含输入事实、前置数据、动作和期望结果。 | 场景只能人工理解，无法写测试数据和断言。 |
| 最小交付裁剪 | 只选择最小闭环测试集，能证明目标场景成功、失败、幂等和红线；扩展能力有未覆盖说明。 | 为未来扩展提前铺大量测试，或把清结算、归档、P2 业务混进交易主线 Red。 |
| 资金安全 | 有资金变化的用例同时断言状态、DSL 借贷表命中行、账户类型、`normalBalanceSide`、余额桶、route snapshot、posting plan、ledger entry、balance projection、借贷平衡、余额影响和幂等。 | 只断言交易状态、entry 数量或“不报错”。 |
| 金融红线 | 外部账户入账、敏感信息泄露、授权拒绝写账、冻结表达消费、投影反写事实、无审批调账等必须失败。 | 红线只写在文档里，没有 TDD-RED 用例或明确不适用原因。 |
| 易用性 | 用户账单、商户账单、运营时间线、错误原因和审计查询有测试或断言来源。 | 只测后台内部对象，不证明使用者能理解结果。 |
| 可理解性 | 测试命名、用例 ID、DSL 契约、产品验收和系分服务入口能互相反查。 | `AC-*`、`RED-*`、`DSL-*`、`TDD-*` 之间没有显式映射。 |
| 可开发性 | 目标测试包、真实执行路径、替身边界、数据准备和验证命令明确。 | 依赖大范围 Mock 内部核心组件，或无法说明运行哪条验证命令。 |
| 可测试性 | 正向、逆向、异常、边界、并发、幂等、重放和 must-fail 用例覆盖工程任务交付范围。 | 交付范围内存在未覆盖资金变化或未说明不适用原因。 |

## TDD 与 PRD、DSL、系分对齐口径

TDD 评审口径：TDD 入口必须承接 PRD 目标、DSL 契约和系分落点，并能进入测试资产分析和 Red 排序。进入编码前仍需工程任务明确写入范围、禁止范围、目标测试资产、验证命令和未覆盖范围；未确认时只能产出测试设计、契约草案或 dry-run。

Spend Rule 测试设计优先引用独立产品分册 09、系分分册 06 和 DSL README 的 Spend Rule DSL v1.1：TDD 只把规则定义、不可变版本、挂载 scope、决策记录、控制额度变动流水、预算控制投影和只读解释转成可执行断言，不在交易路由主线或测试夹具中重新发明规则模型。当前代码兼容名 `SpendRuleDecisionRecord`、`SpendControlMovement` 分别对应产品语义 `SpendRuleDecisionRecord`、`SpendControlMovement`；预算控制投影必须证明 `availableControlAmount = limitAmount - consumedAmount - remainingControlAmount`，且额度调减不能低于已使用和已占用控制金额之和。新写入测试必须证明 `ADMISSION_RECORDED`、`REJECTED_RECORDED` 不能再通过控制额度变动流水入口记录，准入和拒绝证据应进入 Spend Rule 决策记录。`SpendControlMovementTypeContractTests` 是当前兼容期的最小枚举契约测试资产，必须证明兼容决策类型不参与预算控制投影，控制额度变动类型统一解释为 `SpendControlMovement`，调额类和释放类只作为控制额度变动流水子集；application 实现必须消费枚举分类方法，不得在实现类中重新硬编码类型集合。

当前 Spend Rule 服务层测试证据口径：

| 测试资产 | 当前证明 | 不证明 |
| --- | --- | --- |
| `SpendRuleDefinitionServiceFlowTests` | 规则版本不可变、挂载冲突策略和有效期、支付工具范围一致性、挂载查询解释、单条决策记录幂等、决策记录窄查询、决策事实解释和拒绝 / 查询 / 解释无资金副作用。 | 完整规则表达式执行、多规则冲突合成器、批量规则时间线 Query Service、运营后台。 |
| `SpendRuleDefinitionServiceTests` | 规则定义、版本发布、规则挂载和挂载查询 / 解释已经按目标分层服务执行；覆盖版本同摘要幂等、异摘要拒绝覆盖、挂载幂等、非法挂载拒绝、查询 / 解释只读和无资金副作用。 | 完整规则表达式执行、多规则冲突合成器、运营后台、DDL / 生产迁移。 |
| `SpendControlAdmissionApplicationServiceTests`、`AuthorizationAdmissionApplicationServiceTests` | 上层决策证据可进入准入快照和授权准入组合；拒绝停在交易内核前且无资金事实。 | 支付工具全场景生产可用、事件消费、控制额度自动占用。 |
| `SpendControlMovementServiceFlowTests`、`BudgetControlLimitAdjustmentApplicationServiceTests`、`SpendControlTransactionConsumptionApplicationServiceTests` | 控制额度变动流水、预算额度调额、交易成功消耗、失败释放、退款补偿、目标账户隔离和投影下限守卫。 | 账本余额、资金交易事实、生产迁移或历史脏数据修复。 |
| `SpendControlMovementTypeContractTests` | 兼容期枚举分类集中在枚举本身，历史决策兼容类型不参与预算投影。 | 公共类名、表名、DTO 或历史枚举删除。 |
| `FundsTransactionProjectionExplainApplicationServiceTests` / `AuthorizationAdmissionApplicationServiceTests` | 已固化 Spend Rule 决策快照可被交易投影只读解释，不输出 `ruleSpec` 或敏感原文。 | 完整规则定义、版本、挂载、决策记录和控制额度变动流水的运营时间线。 |

Spend Rule 服务层分层测试口径：

1. 当前带 `ApplicationServiceTests` 后缀的测试资产表达跨对象场景编排，不代表所有规则能力都应放在 application service。
2. 新增测试按服务职责命名：标准基础服务覆盖持久化、读取、查询、幂等、状态守卫、解释和预算控制投影；application service 覆盖准入、交易消费、支付工具生命周期等跨对象编排。
3. 规则定义、版本、挂载、决策记录和控制额度变动流水已收敛到目标标准基础服务；后续不再使用旧式领域服务测试命名。
4. application service 测试不得要求被测对象直接访问 Mapper / Repository；基础服务测试可以覆盖 Mapper-backed 真实服务行为。
5. `SpendRuleDecisionRecordServiceTests`、`SpendRuleDefinitionServiceTests` 和 `SpendRuleDefinitionServiceFlowTests` 是当前服务测试资产，分别覆盖决策记录幂等 / 窄查询 / 失败无资金副作用，以及规则定义 / 版本 / 挂载的标准基础服务和服务流行为。

Spend Rule DSL v1.1 的 JSON 示例当前仍为 `DOC_ONLY`：`ruleVersion`、`assignmentSn`、`decisionSn`、`evaluatedRules` 等字段用于统一产品、系分和测试语言；其中 `evaluatedRules`、`decisionPolicy`、`finalDecision`、`requestDigest` 尚未作为独立机器契约和数据库字段完成落地。后续若将其升级为可执行 DSL 或规则引擎输入，必须新增 fixture、解析器、服务层测试和独立工程变更边界。

Highnote Spend Controls 对齐后的任务源以产品分册 09 的 `SR-HN-*` 为准。`SR-HN-001` 已同步“上游决策、wallet 固化证据、transaction 消费快照”的接入口径；`SR-HN-002` 已落地最小可执行规则 TDD；`SR-HN-003` 至 `SR-HN-005` 分别补齐控制窗口、外部决策证据和多规则最终裁决摘要的测试与契约锚点，并持续断言不得产生越界资金事实。准入 / 授权公共请求优先使用 `controlScopeId` 表达控制范围，`budgetGroupSn` 仅作为兼容字段参与同值校验和历史映射。

SR-HN-002 最小测试卡：

| 输出项 | 本轮结论 |
| --- | --- |
| `deliveryScenario` | 接入方需要一个可选轻量 Spend Rule evaluator，在进入现有准入服务前判断单条已发布规则是否通过；评估通过后仍由上游携带决策证据调用 `SpendControlAdmissionApplicationService` 固化。 |
| `firstRedSet` | `SR-HN-002-RED-001` 至 `SR-HN-002-RED-012`。 |
| `coreAssertions` | evaluator 只返回 `PASSED` / `REJECTED`、拒绝原因和决策摘要候选；不得写决策记录、控制额度变动、资金交易、route、posting、LedgerEntry 或账本投影。 |
| `outOfScope` | 多规则冲突合成、表达式引擎、脚本、运营后台、协同授权 webhook、外部风控协议、rolling window、生产调度重置和 DDL。 |
| `nextGate` | SR-HN-002 当前收口；AVS、多规则冲突和表达式引擎均另拆任务。 |

SR-HN-002 首批 Red 候选：

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SR-HN-002-RED-001 | 单笔授权金额超过单笔限额时，接入方能否在交易内核前得到拒绝结论？ | 规则拒绝不得产生任何资金事实或控制事实。 | evaluator 返回 `REJECTED`，拒绝原因为单笔限额超限，决策摘要可稳定重放。 | 不写 `t_spend_rule_decision_record`、`t_spend_control_movement`、`t_funds_transaction`、route、posting、LedgerEntry。 | 金额 101 USD、单笔限额 100 USD；拒绝原因明确；调用前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | 公共 evaluator 契约未确认，或测试只能通过改准入服务职责实现。 |
| SR-HN-002-RED-002 | 当前周期可用额度不足时，是否能在交易内核前拒绝？ | 周期金额判断只读消费预算控制投影，不扣减余额、不预留额度。 | evaluator 读取 `BudgetControlProjectionDTO` 后返回 `REJECTED`。 | 不新增控制额度变动流水，不修改投影来源流水，不写资金事实。 | 周期限额 100 USD，已占用 80 USD，请求 30 USD；断言可用额度不足拒绝，且同周期控制流水仍为 2 条。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | 需要新增 DDL 或把预算控制投影改成账本余额。 |
| SR-HN-002-RED-003 | 当前周期次数达到上限时，是否能拒绝下一笔授权？ | 次数控制只基于同一 `controlScopeId + periodId + ruleId + ruleVersion` 下的既有控制流水按原始占用流水去重计数，不新增资金事实。 | evaluator 返回 `REJECTED`，拒绝原因为周期次数超限。 | 不写决策记录、控制额度变动、交易或账本事实。 | 周期次数上限 3，已有 3 条同周期消费或占用控制尝试，请求第 4 笔拒绝；同一授权 RESERVED 后 CONSUMED 不重复计数。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | 现有流水无法表达稳定计数口径，或需要新增聚合表。 |
| SR-HN-002-RED-004 | MCC 在黑名单或不在白名单时，是否能拒绝？ | MCC 判断只使用请求事实和规则规格，不触发资金或控制事实写入。 | evaluator 返回 `REJECTED`，拒绝原因说明 MCC 不允许。 | 不写任何资金事实、决策记录或控制流水。 | 请求 MCC 为 `7995` 且 deny list 包含 `7995` 时拒绝；allow list 只包含 `5812` 而请求 MCC 为 `7995` 时拒绝；断言拒绝前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS 需另拆任务。 |
| SR-HN-002-RED-005 | 所有已实现最小规则均通过时，是否能返回可被准入服务消费的通过证据？ | 通过结论仍然不代表交易成功，也不自动写入准入决策记录。 | evaluator 返回 `PASSED`、空拒绝原因和稳定决策摘要候选。 | 不写决策记录、控制额度变动、资金交易、route、posting、LedgerEntry。 | 当前已实现单笔限额满足时摘要重复评估稳定一致；MCC allow list 命中时通过且无资金事实副作用。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | 需要多规则冲突合成或外部风控确认才能判断通过。 |
| SR-HN-002-RED-006 | 商户国家在黑名单或命中白名单时，是否能按请求事实给出可重放评估？ | 国家判断只使用请求事实和规则规格，不触发资金或控制事实写入。 | evaluator 返回 `REJECTED` 或 `PASSED`，拒绝原因为商户国家不允许。 | 不写任何资金事实、决策记录或控制流水。 | 请求国家 `CU` 且 deny list 包含 `CU` 时拒绝；allow list 包含 `US` 且请求 `us` 时大小写归一化后通过；断言拒绝 / 通过前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS 需另拆任务。 |
| SR-HN-002-RED-007 | 企业员工卡禁止磁条降级交易时，是否能在交易内核前拒绝？ | 卡数据输入能力判断只使用请求事实和规则规格，不触发资金或控制事实写入，不保存 PAN/CVV。 | evaluator 返回 `REJECTED` 或 `PASSED`，拒绝原因为卡数据输入能力不允许。 | 不写任何资金事实、决策记录或控制流水。 | 请求 `MAGNETIC_STRIPE` 命中 deny list 时拒绝；allow list 包含 `EMV_CHIP` 且请求 `emv_chip` 时大小写归一化后通过；断言拒绝 / 通过前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS 需另拆任务。 |
| SR-HN-002-RED-008 | 企业卡是否能按指定商户标识 MID 拒绝风险商户或只允许合作商户？ | 商户标识判断只使用请求事实和规则规格，不触发资金或控制事实写入。 | evaluator 返回 `REJECTED` 或 `PASSED`，拒绝原因为商户标识不允许。 | 不写任何资金事实、决策记录或控制流水。 | 请求 `MID-RISK-001` 命中 deny list 时拒绝；allow list 包含 `MID-CONTRACT-001` 且请求同一 MID 时通过；断言拒绝 / 通过前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS 需另拆任务。 |
| SR-HN-002-RED-009 | 企业卡能否按 PAN 录入方式拒绝手工录入或只允许非接触式录入？ | PAN 录入方式判断只使用录入方式枚举事实，不保存 PAN 原文，不触发资金或控制事实写入。 | evaluator 返回 `REJECTED` 或 `PASSED`，拒绝原因为 PAN 录入方式不允许。 | 不写任何资金事实、决策记录或控制流水；不写完整 PAN、CVV 或卡敏感值。 | 请求 `manual` 命中 deny list `MANUAL` 时拒绝；allow list 包含 `CONTACTLESS` 且请求 `contactless` 时通过；断言拒绝 / 通过前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS 需另拆任务。 |
| SR-HN-002-RED-010 | 企业差旅卡能否拒绝 ATM 终端，车队卡能否只允许自助加油终端？ | POS 类别判断只使用终端类别枚举事实，不触发资金或控制事实写入。 | evaluator 返回 `REJECTED` 或 `PASSED`，拒绝原因为 POS 类别不允许。 | 不写任何资金事实、决策记录或控制流水。 | 请求 `automated_teller_machine` 命中 deny list `AUTOMATED_TELLER_MACHINE` 时拒绝；allow list 包含 `AUTOMATED_FUEL_DISPENSER` 且请求 `automated_fuel_dispenser` 时通过；断言拒绝 / 通过前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS 需另拆任务。 |
| SR-HN-002-RED-011 | 电商卡要求 CVV 时，未提供 CVV 事实能否在交易内核前拒绝？ | CVV 判断只使用是否提供 CVV 的布尔事实，不接收、不保存 CVV 原文，不触发资金或控制事实写入。 | evaluator 返回 `REJECTED` 或 `PASSED`，拒绝原因为未提供 CVV。 | 不写任何资金事实、决策记录或控制流水；不写完整 PAN、CVV 或卡敏感值。 | `cvvControl.required=true` 且请求 `cvvProvided=false` 时拒绝；请求 `cvvProvided=true` 时通过；断言拒绝 / 通过前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS 需另拆任务。 |
| SR-HN-002-RED-012 | 企业卡能否按卡交易处理类型拒绝 PIN 变更，或只允许取现类处理类型？ | 卡交易处理类型判断只使用请求事实和规则规格，不触发资金或控制事实写入。 | evaluator 返回 `REJECTED` 或 `PASSED`，拒绝原因为卡交易处理类型不允许。 | 不写任何资金事实、决策记录或控制流水。 | 请求 `pin_change` 命中 deny list `PIN_CHANGE` 时拒绝；allow list 包含 `CASH` 且请求 `cash` 时通过；断言拒绝 / 通过前后资金事实计数不变。 | `SpendRuleEvaluationApplicationServiceTests`，已落地。 | `just test-one SpendRuleEvaluationApplicationServiceTests tests`。 | AVS、conditional rule 或 velocity control 需另拆任务。 |

SR-HN-003 控制窗口测试卡：

| 输出项 | 本轮结论 |
| --- | --- |
| `deliveryScenario` | 周期额度不复用账本周期、不生成预算组账本；接入方直接用 `controlScopeId + periodId` 查询当前或历史控制窗口。 |
| `firstRedSet` | `SR-HN-003-RED-001`。 |
| `coreAssertions` | 当前周期和历史周期只需替换 `periodId`；同周期不同 `controlScopeId`、不同目标账户不得串账；投影查询不得写交易、route、posting、LedgerEntry 或账本余额。 |
| `outOfScope` | 新增 `windowType` 字段、rolling window、cooldown、生产调度、时区计算器、表达式引擎和 DDL。 |
| `nextGate` | 若要支持滚动窗口或调度自动刷新，再拆单独工程任务；当前最小控制窗口以 `periodId` 作为外部已决策窗口标识。 |

SR-HN-003 首批 Red 候选：

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SR-HN-003-RED-001 | 当前周期和历史周期额度是否能用周期标识直接查询，且不串入其他周期、账户或控制范围？ | Spend Rule 控制窗口只派生控制投影，不生成账本余额或预算组账本。 | `BudgetControlProjectionQuery(controlScopeId, periodId, currency, targetAccountId)` 返回指定窗口的额度、占用、剩余和可用控制额度。 | 不写资金交易、route、posting、LedgerEntry；不把其他周期、其他账户、其他 `controlScopeId` 的流水混入。 | 2026-07 和 2026-08 分别能返回各自额度；另一个 `controlScopeId` 的同周期流水不影响主控制范围；查询后资金事实不变。 | `SpendControlMovementServiceFlowTests`，已落地。 | `just test-one SpendControlMovementServiceFlowTests tests`。 | 需要 rolling window、cooldown、时区换算或生产调度刷新。 |

SR-HN-004 外部决策证据接入测试卡：

| 输出项 | 本轮结论 |
| --- | --- |
| `deliveryScenario` | 上游规则服务、外部风控或协同授权服务已经给出 approve / decline，本系统只消费最终决策证据并固化准入事实。 |
| `firstRedSet` | `SR-HN-004-RED-001`、`SR-HN-004-RED-002`。 |
| `coreAssertions` | 外部 decline 停在交易内核前且无 route、posting、LedgerEntry；外部 approve 仍必须先通过支付工具、账户能力和资金责任校验，不能直接代表资金可用或授权成功。 |
| `outOfScope` | webhook endpoint、HMAC、外部风控协议、超时 stand-in、模拟器、多规则裁决和 DDL。 |
| `nextGate` | 若需要平台托管协同授权协议，再拆外部接入工程任务；当前公共契约只保留最终决策流水、结果、摘要和拒绝原因。 |

SR-HN-004 首批 Red 候选：

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SR-HN-004-RED-001 | 外部 decline 后是否能在交易内核前停止？ | 外部拒绝不得创建任何交易或账本事实。 | 返回 `admitted=false`，固化决策流水、规则版本、摘要和拒绝原因。 | 不写资金交易、route、posting、LedgerEntry 或余额投影。 | 支付工具、账户能力和资金责任均可用；上游结果为 `REJECTED`；断言拒绝原因、决策记录和资金事实不变。 | `SpendControlAdmissionApplicationServiceTests`，已落地。 | `just test-one SpendControlAdmissionApplicationServiceTests tests`。 | 需要新增外部回调协议字段或 webhook。 |
| SR-HN-004-RED-002 | 外部 approve 是否会绕过 wallet 准入链？ | 外部通过只是一条规则证据，不代表资金责任、账户能力或授权交易成功。 | 默认资金责任缺失时准入失败，且不固化决策记录。 | 不写决策记录、资金交易、route、posting、LedgerEntry 或余额投影。 | 携带 `PASSED` 决策证据；支付工具和账户存在但默认资金责任关系缺失；断言失败原因和无副作用。 | `SpendControlAdmissionApplicationServiceTests`，已落地。 | `just test-one SpendControlAdmissionApplicationServiceTests tests`。 | 需要把外部 approve 解释成直接授权成功。 |

SR-HN-005 多规则裁决证据契约评审卡：

| 输出项 | 本轮结论 |
| --- | --- |
| `deliveryScenario` | 上游已经完成多规则裁决，本系统当前只需要消费最终决策流水、结果、摘要和拒绝原因，支持准入、幂等、回放和对账追踪。 |
| `contractDecision` | `evaluatedRules`、`decisionPolicy`、`finalDecision` 暂不进入 `ResolveSpendControlAdmissionRequest` 或 `RecordSpendRuleDecisionRecordRequest` 公共字段；如需表达多规则明细，先由上游把完整裁决证据纳入 `decisionDigest` 的摘要源。 |
| `coreAssertions` | 最终摘要不能替代明细可解释性；但在公共契约升级前，本系统不得伪造、截断或重算上游多规则明细。 |
| `outOfScope` | 新增公共 DTO 字段、DDL、历史决策回填、多规则冲突合成器、规则引擎和内部解释 payload 落库。 |
| `nextGate` | 只有接入方明确要求本系统保存并查询多规则明细时，才新增字段、表结构、兼容策略和可执行测试。 |

SR-HN-005 首批 Red 候选：

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SR-HN-005-RED-001 | 当前公共契约是否足以消费上游多规则最终裁决？ | 多规则裁决证据只是准入控制事实，不创建资金交易或账本事实。 | 接入方传入最终 `decisionSn/result/digest/rejectReason`，准入服务只固化最终决策记录。 | 不新增 `evaluatedRules`、`decisionPolicy`、`finalDecision` 公共字段、DDL 或解释 payload 落库。 | 公共 request 注释明确摘要可代表多规则裁决证据；完整明细由上游保存。 | `ResolveSpendControlAdmissionRequest`、`RecordSpendRuleDecisionRecordRequest` 注释，已落地。 | `just compile`。 | 接入方要求本系统查询或回放每条规则明细。 |
| SR-HN-005-RED-002 | 如果未来需要本系统解释多规则明细，应证明什么？ | 历史解释必须读取当时固化证据，不按当前规则重算。 | 能查询每条 evaluated rule、裁决策略和最终决策，并能脱敏展示拒绝原因。 | 不泄露敏感商户原文、卡号、token、外部账户或风控模型细节；不改变既有最终决策幂等。 | 先补公共契约、DDL 兼容、脱敏和历史回放测试，再实现落库。 | 待授权，当前不落地。 | 待授权后定义。 | 涉及破坏性公共契约变更、历史迁移或敏感字段存储。 |

涉及 Spend Rule 或资金主链路的代码切片，`just compile`、`just test-one`、`just test-module`、`just verify-fast` 和 `just verify-cad` 必须通过 classfile 错误桩扫描；`verify-classfiles` 会检查 `target/classes` 和 `target/test-classes` 中是否存在 `Unresolved compilation`，避免 Maven 命令成功但编译产物不可用。

涉及注解生成链路、MapStruct converter、MyBatis-Flex `NameRefs` 或 clean build 风险时，优先执行 `just clean-compile`；该命令会从空 `target` 重新编译 reactor，并校验代表性的生成类已写入 `target/classes`。

| 对齐项 | PRD 输入 | DSL 输入 | 系分输入 | TDD 必须产出 | 阻断信号 |
| --- | --- | --- | --- | --- | --- |
| 稳定口径 | 产品目标、使用者、规则、红线和验收矩阵。 | 稳定 caseId、字段语义、不变量和失败边界。 | 服务入口、状态机、表设计、事务、观测和安全门禁。 | `AC-*`、`DSL-*`、`TDD-*`、`RED-*` 的映射表和目标测试资产。 | 用例来自过程描述，无法追溯到产品验收、DSL case 或系分入口。 |
| 可解释、可核对、可重建 | 每笔金额都要可解释、可核对、可重建。 | 主体、账户类型、`normalBalanceSide`、账目、金额、币种、route、posting、entry、projection、借贷平衡、余额影响和审计引用。 | 交易、路由、账本、投影、对账和治理模块落点。 | 同时断言状态、余额桶、route snapshot、posting plan、ledger entry、projection、借贷平衡、余额影响、幂等和审计。 | 只断言交易状态、entry 数量、接口不报错或日志存在。 |
| P0 资金内核 | 钱包、账本、账目、余额投影、对账、清分、清算、结算、归档和账本余额快照。 | 统一资金事实和账务对象。 | 账户账本、清结算对象、governance 逻辑边界和横切红线。 | 资金主线 Red、清结算与治理独立能力域 Red、余额桶断言、对账差错、归档水位、只读边界和失败无副作用。 | P0 能力未闭合就先验证 P2 业务特殊路径。 |
| P1 交易入口 | 直接交易、授权交易、余额控制和交易投影。 | instruction、event、route snapshot、posting plan 和 projection case。 | 02 分册的服务契约、状态机、表设计和投影任务。 | 直接交易、授权完成/撤销、授权过期无资金副作用、冻结解冻、余额调整、退款、争议裁决资金结果、交易投影和 route replay 测试。 | 授权拒绝、授权过期、余额不足或规则不唯一时仍产生 route、posting、entry 或投影。 |
| P2 业务补充 | VCC、全球账户、收单和 ACH/银行转账边界只作为业务语义和外部轨道输入；VCC 卡、prepaid virtual card、shared card 只验证支付工具、绑定快照、资金责任解析关系和资金动作映射。 | 归一业务事实、外部引用、状态映射、脱敏证据、规则待确认字段、`PaymentInstrumentRef`、`FundingAllocationDecision` 和 route snapshot。 | 业务能力包准入卡、P0/P1 回归范围、外部规则确认、未覆盖红线、`TDD-RAIL-001A`、`TDD-P2-VCC-004` 至 `TDD-P2-VCC-011`、`TDD-WALLET-015` 至 `TDD-WALLET-019`。 | 业务状态映射、乱序重复、外部引用脱敏、规则待确认、P0/P1 回归、敏感数据 must-fail、预付资金责任、共享卡绑定快照、应用 facade 准入和内部主体能力选择。 | 业务专项测试绕过统一钱包应用层、账本、清结算、对账或归档链路，或把卡工具/共享卡/预付模式测试成新的账户余额、信用账户或预算主体。 |
| 清结算与对账 | 清分、清算、结算、出款、对账、差错、调账核销和追偿。 | 批次、来源事实、规则、差异、审批、凭证和处理动作。 | 03 分册对象状态机、服务 API、表设计、补偿和审计。 | `CLS-GATE-*`、`TDD-B7-RED-*`、服务级 H2 流程、重跑幂等、并发锁、权限审计和差异闭环。 | 清结算、对账或出款只有设计，没有 DDL/H2、服务级流程测试或外部规则确认。 |
| 归档、重放和指标边界 | 归档、余额重建、交易投影重放、异常人工处理和指标只读边界。 | Manifest、checkpoint、watermark、差异报告、处理动作和指标输入边界。 | 04 分册 governance 逻辑边界、物理落点候选、只读边界和人工处理入口。 | `GOV-GATE-*`、`TDD-B8-RED-*`、dry-run/apply、范围锁、回滚/续跑、指标水位隔离和治理边界测试。 | 用普通指标快照替代余额确认，或让治理任务反写资金事实。 |

授权支付工具入口只允许测试 application facade 的准入、解析、快照和委派，不允许把账户主体型 `FundsAuthorizationTransactionService.authorize` 请求替换为支付工具引用。支付工具生命周期授权入口已作为服务层最小切片落到 `InstrumentTransactionLifecycleApplicationService#authorizeByInstrument`，该统一入口只委派 `AuthorizationAdmissionApplicationService` 完成工具准入、资金责任、账户能力和 Spend Rule 决策证据校验；支付工具收款入口已作为服务层最小切片落到 `InstrumentTransactionLifecycleApplicationService#receiveByInstrument`：VA、ACH 或外部钱包端点收款先解析工具和账户能力，再委派账户主体型 `FundsDirectTransactionService#topup`，并把脱敏 `PaymentInstrumentRef` 固化到 route snapshot 用于审计和投影解释；收款请求必须携带 `expectedBindingVersion`，缺版本必须在交易内核前失败且无资金事实副作用。不改变交易 canonical 账户主体，不给 VA、卡或外部账户建账。P2 场景如 VCC 预付卡充值、共享卡调额、全球账户付款和 ACH/银行转账事件，必须先经业务能力包解释成归一资金事实，再复用 P1/P0 测试资产。

授权后继能力和支付工具生产可用性需要分开评审。账户主体型 canonical 授权内核通过授权后继准入卡承接强制完成、无授权退款、争议裁决资金结果承接和并发竞争；支付工具与 Spend Rule 的生产可用性通过支付工具准入卡承接工具准入、资金责任解析、授权 application facade、Spend Rule 控制和只读投影。支付工具及周边支持队列整体排在账本账目、钱包基础能力和交易内核之后；未形成独立工程边界前，TDD 只能继续做差距复核或 contract-only，不写生产代码、测试代码、DDL/H2 schema 或运行时配置。

资金责任目标字段已统一为并落地 `targetSubjectType + targetSubjectId`，允许资源关系表达资金账户和信用账户目标主体；平台角色责任主体、完整 `FundingAllocationDecision` 摘要、route snapshot、账户层级快照和回放断言仍需后续工程边界。B6/B8 进入交易投影或重放时，只能消费交易事实、冻结单、route snapshot、`paymentInstrumentRef`、`AccountHierarchySnapshot`、`FundingAllocationDecision`、`SpendRuleDefinition`、`SpendRuleVersion`、`SpendRuleAssignment`、`SpendRuleDecisionRecord` / `SpendRuleDecisionRecord`、`SpendControlMovement` / `SpendControlMovement`、账本摘要、授权拒绝事实、清结算和对账差错；不得把投影测试通过写成账务事实或生产交付完成。

## 生产验证准入口径

TDD 设计区分“目标测试资产”和“执行证据”。目标测试资产只说明生产交付需要哪些测试；只有测试代码存在、数据准备完整、验证命令执行通过，并且交付说明列出覆盖范围后，才能作为生产完成证据。

| 判定对象 | 可接受证据 | 不可接受替代 |
| --- | --- | --- |
| 资金主链路 | 服务层或契约测试通过，且同时断言状态、route snapshot、posting plan、ledger entry、余额投影、幂等和审计。 | 只断言交易状态、entry 数量或接口不报错。 |
| 清结算与对账 | DDL/H2 schema、对象状态机、服务契约和服务级 H2 流程测试同时闭合。 | 只有文档、空模块骨架或策略单元测试。 |
| 归档、重放和指标边界 | 范围、checkpoint、watermark、Manifest、差异报告、dry-run/apply、回滚/续跑和只读边界测试通过。 | 只有交易投影重放局部测试，或用普通指标快照替代账本余额快照。 |
| 外部规则和敏感数据 | 有规则来源、版本或发布日期、生效日期、适用主体或适用范围、适用法域、核验日期、确认方、确认状态、脱敏和审计测试。 | 用公开文档链接、口头约定或人工说明替代验证。 |

### 评审维度到测试证据

做设计评审或工程任务验收时，TDD 需要把“好设计”转成可执行证据。每个任务至少选择与目标范围相关的证据项；无法覆盖时必须写明不适用原因。

| 评审视角 | 测试证据 | 最低断言 |
| --- | --- | --- |
| 可用性 | 正向链路、失败链路、重试/幂等、人工处理或 Runbook 触发用例。 | 成功有终态，失败有原因和处理路径，重复请求不重复产生资金副作用。 |
| 资金安全 | 余额桶、账本周期、route snapshot、posting plan、ledger entry、projection、唯一约束和并发冲突测试。 | 金额不超额、不串周期、不跨主体误转、不半截入账、不重复出款。 |
| 金融红线 | 外部账户入账、敏感信息、权限、审批、KYC/KYB/AML 待确认、外部规则未确认、轨道/FX 未启用 must-fail。 | 未确认或越权路径必须失败，且不生成 route、posting、entry 或敏感导出。 |
| 易用性 | 用户账单、商户账单、运营时间线、错误原因、差错责任、告警和审计查询断言。 | 使用者能看出发生了什么、为什么失败、是否可重试、是否需要人工处理。 |
| 可理解性 | `AC-*`、`DSL-*`、`TDD-*`、`RED-*`、服务入口和测试类命名互相反查。 | 测试失败能定位到产品验收、DSL 契约和系分落点，而不是只看到内部实现名。 |

### Red 选择顺序

编码前的 TDD 不应先铺大全量目标态，而应选择能最大程度证明资金底座不变量的 Red。准入顺序是先证明 P1 交易主链路依赖的 P0 账户、账本、账目和余额投影证据，再推进授权和余额控制，最后再独立打开清结算、对账、归档和治理。

| 切片 | Red 目标 | 最低断言 | 暂不覆盖 |
| --- | --- | --- | --- |
| 基线核验 | 证明测试环境、H2 schema、现有测试资产和验证命令可用。 | `just mvn-version`、目标测试可定位、fixture 可读取、任务范围可解释。 | 不新增生产实现、测试代码或 schema。 |
| 直接交易 | 证明直接交易成功和失败的资金事实链。 | 交易状态、route snapshot、posting plan、ledger transaction、ledger entry、余额投影、幂等和审计；余额不足、规则不唯一或重复请求无副作用。 | 清结算、对账、归档、P2 业务能力包。 |
| 授权交易 | 证明授权完成、部分完成、撤销、过期、退款和拒付的状态与金额边界。 | 授权拒绝不生成 route/entry；累计完成不超授权；撤销/过期释放剩余占用；逆向沿原 route snapshot。 | 商户结算单、出款单、外部卡组织完整规则。 |
| 余额控制 | 证明冻结、解冻、调整和失败路径不改变资金语义。 | 冻结只做同主体 `AVAILABLE <-> FROZEN`；解冻不产生跨主体转移；调整必须有审批、原因、审计和幂等；失败不写 entry。 | 对账差错调账和运营补事实白名单，除非独立任务确认。 |
| DSL 执行化 | 证明交付引用的 caseId 不只是文档样例。 | 每个引用 caseId 有 fixture 级别、路径、目标测试类、核心断言和未覆盖范围；被测试读取后才声明机器契约通过。 | 未纳入任务范围的历史 caseId 全量清理。 |
| 清结算与对账 | 独立证明对账、差错、清分、清算、结算、出款和追偿闭环。 | `CLS-GATE-*`、`TDD-B7-RED-*`、DDL/H2、服务级 H2 流程、并发重跑、权限审计、外部规则和人工处理。 | 不混入交易主线；不把出款前准入设计当完整生命周期。 |
| 资金数据治理 | 独立证明治理只读、归档水位、重放差异和人工处理闭环。 | `GOV-GATE-*`、`TDD-B8-RED-*`、Manifest、checkpoint、watermark、dry-run/apply、范围锁、指标水位隔离和差异报告。 | 不混入交易主线；不让治理任务反写资金事实。 |

任一 Red 都必须先写失败断言和停止条件：如果测试只能证明接口不报错、状态变化或 entry 数量，不能进入实现；如果测试需要新增公共契约、状态机、表结构、H2 schema 或运行时配置，必须先回到工程任务确认写入范围。

### 业务驱动 Red 裁剪规则

首批 Red 不能从模块覆盖率或目标态清单反推，而要从一个业务问题和一个资金不变量开始。每个 Red 都必须能回答：哪个使用者会看到什么结果，哪些资金事实必须发生，哪些事实绝不能发生。

| 裁剪规则 | 必须满足 | 不满足时处理 |
| --- | --- | --- |
| 一个 Red 对应一个业务问题 | 能反查 `businessQuestion`、`deliveryScenario`、产品验收 ID 和 DSL caseId。 | 回到产品或 DSL，不写测试。 |
| 一个 Red 证明一个最小资金不变量 | 至少覆盖适用的主体、账户类型、`normalBalanceSide`、账目、金额、币种、route、posting、entry、projection、幂等和审计。 | 缩小场景或补资金事实表。 |
| 失败路径必须有 forbidden facts | 明确不允许半截 route、posting、entry、投影、外部出款、敏感导出、治理反写或重复资金副作用。 | 先补失败无副作用断言。 |
| 不把能力域混在一起 | A1-A4、B7、B8、P2 只能单独授权；清结算深水区、资金数据治理、业务专项不得混入交易主线 Red。 | 拆成独立 Red 候选和独立工程边界。 |
| 测试资产要真实可落地 | 指定目标测试类、真实执行路径、fixture/H2 数据、替身边界和验证命令。 | 只能做 TDD 分析或 contract-only。 |
| 停止条件先写清 | 红灯不符合预期、公共契约/表结构越界、外部规则未确认、生产风险升高时如何停止。 | 不进入 Green 实现。 |

首批 Red 最小输出表：

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 待填 | 待填 | 待填 | 待填 | 待填 | 待填 | 待填 | 待填 | 待填 |

A0 基线核验阶段不写测试代码，但必须输出 `redCandidateSet`、`targetAssets`、`schemaNeed`、`minimumAssertions` 和 `testStopReasons`。只有工程任务明确写入范围后，才把候选 Red 转成实际测试写入。

### 最小交付任务测试卡

每个最小交付任务进入实现前，TDD 需要输出一张最小测试卡，避免任务被测试范围反向放大。

| 输出项 | 填写口径 |
| --- | --- |
| `deliveryScenario` | 目标场景、入口动作、成功终态、失败终态和不覆盖范围。 |
| `firstRedSet` | 只覆盖最小闭环的 Red；每个 Red 写清失败断言、目标测试类和验证命令。 |
| `coreAssertions` | 状态、route snapshot、posting plan、LedgerEntry、余额投影、幂等、审计和失败无副作用中的适用断言。 |
| `outOfScope` | 清结算深水区、资金数据治理、P2 业务、外部协议、完整运营后台和报表等不进入当前任务的范围。 |
| `nextGate` | 后续扩展需要的产品确认、DSL caseId、系分落点、测试资产和独立授权条件。 |

### 基线核验测试盘点

基线核验不写测试代码，但必须把“要写哪些测试、证明什么、怎么停下来”盘点清楚。盘点完成后才能把交易主线、清结算或治理 Red 写入工程任务。

| 盘点项 | 必须输出 | 未覆盖条件 |
| --- | --- | --- |
| 目标测试类 | 工程任务可能新增、恢复或复用的测试类、所属模块和验证命令。 | 找不到真实执行路径或只能依赖内部大范围 Mock。 |
| 测试层级 | 单元、契约、服务级 H2、业务流程、边界测试、治理测试的适用层级。 | 资金变化只有单元测试或文档样例，没有服务级或契约证据。 |
| fixture 清单 | 需要读取的 DSL fixture、业务数据、H2 基础数据和外部替身边界。 | fixture 未落地、等级不匹配或不被测试读取。 |
| 最小断言 | 状态、余额桶、route snapshot、posting plan、ledger transaction、LedgerEntry、projection、幂等和审计。 | 只断言状态、数量、日志或接口不报错。 |
| 失败无副作用 | 余额不足、拒绝、错币种、规则不唯一、权限不足、重复请求、外部规则未确认的 forbidden facts。 | 失败路径会生成半截 route、posting、entry、外部出款或敏感导出。 |
| 数据和 schema | 是否需要改 H2 schema、DDL、Entity、Mapper 或测试数据准备。 | 需要改 schema 但工程任务未确认写入范围。 |
| 回归范围 | 交易主线、清结算、治理或 P2 业务能力包对既有测试的回归影响。 | 只测新增路径，不保护 P0/P1 资金内核。 |
| 停止条件 | 红灯不符合预期、需要扩公共契约、需要改表、触碰外部规则或跨能力域时如何停止。 | 无停止条件，或把外部规则/资金红线失败当作普通实现缺陷。 |

基线核验输出直接作为 Red 评审页：

| 输出项 | 填写口径 |
| --- | --- |
| `redCandidateSet` | 工程任务建议先写的 Red 编号、目标行为和失败断言。 |
| `targetAssets` | 目标测试类、fixture、H2 数据准备和验证命令。 |
| `minimumAssertions` | 状态、DSL 借贷表命中行、账户类型、`normalBalanceSide`、余额桶、route、posting、entry、projection、借贷平衡、余额影响、幂等、审计和 forbidden facts。 |
| `schemaNeed` | 是否需要 DDL/H2、Entity、Mapper 或测试资源写入授权。 |
| `testStopReasons` | 哪些失败必须停止并回到工程任务、系分或外部规则确认。 |

### Red 卡模板

每个进入编码的 Red 都要先填 Red 卡，避免测试只表达内部实现步骤。Red 卡可以作为工程任务附件，也可以作为 TDD 分析产物。

| 字段 | 填写要求 |
| --- | --- |
| `redId` | 使用 `TDD-*` 或 `RED-*` 编号，能反查 PRD 和 DSL。 |
| `targetBehavior` | 从使用者或资金事实角度描述必须失败或必须通过的行为。 |
| `preconditions` | 主体、账户、账目、余额桶、支付工具、route 规则、历史事实和外部引用。 |
| `action` | 触发的服务入口、指令、事件或任务。 |
| `expectedFacts` | 应产生的交易事实、route snapshot、posting plan、ledger entry、projection、审计或差异报告。 |
| `forbiddenFacts` | 不允许产生的半截 route、posting、entry、投影、外部出款、敏感导出或治理反写。 |
| `assertions` | 状态、金额、账目、币种、周期、余额桶、幂等、审计和失败无副作用断言。 |
| `verificationCommand` | 精确到 `just test-one <TestClass> [module]` 或等价 Maven 命令。 |
| `stopCondition` | 红灯不符合预期、需要扩公共契约、需要改表、外部规则未确认或触碰清结算、治理、P2 时如何停止。 |

Red 卡表头：

| redId | targetBehavior | preconditions | action | expectedFacts | forbiddenFacts | assertions | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

### 测试证据包

每个工程任务的交付说明至少应能列出下列测试证据。无法执行时，必须说明是环境、依赖、私有仓库、测试数据还是实现缺口导致，不能把未执行测试当作通过。

| 证据项 | 必须列出 |
| --- | --- |
| 覆盖映射 | 工程任务覆盖的 `AC-*`、`DSL-*`、`TDD-*`、`RED-*`。 |
| 测试资产 | 新增、恢复或复用的测试类、fixture、H2 schema、测试数据准备和替身边界。 |
| 核心断言 | 状态、route snapshot、posting plan、ledger entry、余额投影、交易投影、幂等、审计和失败无副作用。 |
| 验证命令 | 实际执行的 `just` 或 Maven 命令、模块范围、指定测试类和结果。 |
| 未覆盖项 | 工程任务未覆盖的目标测试资产、未执行原因、承接任务或人工确认点。 |
| 残余风险 | 外部规则、合规、财务、通道、报表指标、性能容量、并发锁或数据迁移风险。 |
