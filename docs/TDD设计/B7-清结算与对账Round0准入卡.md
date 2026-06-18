# B7 清结算与对账生产可用性 Round 0 准入卡

## 1. 文档定位

本文档是 B7 清结算与对账进入生产可用 MVP 前的 Round 0 候选准入卡。它把产品设计、DSL、系分设计、TDD、OpenSpec、当前代码观察和 GSD + Goal 交付雷达收敛成一张可评审、可拆分、可转成单一 Execution Grant 的输入页。

本文档不授权修改生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。只有用户确认本页或确认经调整后的单一 `Execution Grant` 后，才允许把本文档中的 Red 候选转成实际测试写入。

## 2. authorityBaseline

| 基线项 | 当前口径 |
| --- | --- |
| 设计和任务基线 | 最新已提交设计和任务对齐输入以确认时 Git HEAD 为准；当前未提交文档变更若要作为开工依据，必须先提交或在 Execution Grant 的 `authorityBaseline` 中显式列入。 |
| 产品入口 | `docs/产品设计/03-清结算与对账.md` 定义清分、内部清算、结算、出款、对账、差错和追偿的产品对象、状态、规则和验收口径；`docs/产品设计/05-产品验收与TDD用例矩阵.md` 定义 P0 清分清算结算对账的准入裁决。 |
| DSL 入口 | `docs/DSL设计/支付资金底座DSL承载层设计.md` 的资金场景借贷平衡与账务期望表决定 `CLEARING -> AVAILABLE`、`AVAILABLE -> SETTLEMENT`、`SETTLEMENT/IN_TRANSIT -> CASH`、差错调账和补事实的账务语义。 |
| 系分入口 | `docs/系分设计/03-清结算与对账系分设计.md` 定义 `reconciliation` 模块边界、对象关系、子模块、状态机、表设计、前置对账、差错闭环、补事实白名单和工程落地门禁。 |
| TDD 入口 | `docs/TDD设计/支付资金底座测试驱动设计.md` 的 `TDD-CLS-*`、`TDD-SETTLE-*`、`TDD-RECON-*`、`TDD-CLS-FLOW-*`、`TDD-RED-020` 至 `TDD-RED-025` 和 `TDD-RED-033`。 |
| OpenSpec 入口 | `openspec/project.md` 的 P0 清结算与对账准入裁决、`openspec/changes/tdd-baseline-reset/tasks.md` 的 B7 能力优先级和 GSD + Goal 基线。 |
| 生产可用验收用例 | 两级代理收益分润作为 B7 生产可用验收种子：平台员工邀请用户代理，平台员工可配置分润用户代理获得的佣金；清结算只消费收益应得项、归因快照、规则版本、GMV 阶梯或利润口径、审批和账户解析结果，不实现代理关系、KPI、税务或营销规则引擎。 |
| 当前代码证据 | `reconciliation-*` 模块骨架和出款前准入候选只能作为差距复核输入；`LedgerEntry` 已有 `settlement_status`、`settlement_period`、`reconcile_status`、`reconciliation_batch` 等字段；`SettlementPolicySpecTests` 可作为清算策略契约参考；当前暂无完整清分、清算、结算、出款、对账差错和追偿服务级 H2 闭环。 |

## 3. grantCandidate

| 字段 | 候选取值 |
| --- | --- |
| `Task ID` | `B7-RECON-DIFFERENCE-MVP-CAD-001` |
| `Execution Grant` | `B7-RECON-DIFFERENCE-MVP`，待用户确认。 |
| `mvpScenario` | 一笔已成功、已过账且有账本分录的交易进入对账；系统先把外部文件、流水或回单标准化为来源记录、标准化明细和来源质量结论，再按任务范围、规则版本和匹配强度执行匹配；发现金额、状态、漏单、重复、来源未验证或候选匹配不可靠时生成差错单，阻断清算候选或出款提交；差错处理后可以重新对账，首轮补事实白名单默认关闭。 |
| `abilityBatch` | B7 Wave 1 只做对账来源标准化、对账任务、匹配强度、差错单、阻断、账龄升级、重跑和补事实白名单准入；不一次性实现清分批次、清算批次、结算单、出款单和追偿全生命周期。 |
| `businessQuestion` | 财务、运营、商户和研发能否解释“为什么这笔已过账交易不能清算或出款”，并能看到来源质量、匹配强度、差错原因、责任方、账龄、到期动作、审批、凭证、重跑结果、可补事实动作和下一步处理入口。 |
| `moneyInvariant` | 对账差错和处理动作不得直接修改历史交易、历史分录、余额投影或交易投影；资金影响只能通过明确白名单的交易层或账本层追加事实完成；重大差错未闭环时不得生成清算候选、确认清算批次、锁定结算单或提交出款。 |
| `productNotDone` | 不声明完整清分、完整内部清算、完整商户结算、完整外部出款、完整追偿、完整运营后台、完整报表、持牌清算、外部通道规则或生产上线完成。 |
| `firstRedSet` | `R0-B7-RECON-001` 至 `R0-B7-RECON-006` 优先；若选择清算准入再补 `R0-B7-CLS-001`；若选择出款解释再补 `R0-B7-PAYOUT-001`；若选择运营权限再补 `R0-B7-OPS-001`。 |
| `currentEvidence` | 既有 `LedgerEntry` 对账字段、平台 CLEARING/SETTLEMENT 账目、清算策略契约和出款前准入候选实现只能证明局部承载能力，不能直接升级为 B7 生产可用。 |

## 4. productReviewMap

| 产品审查项 | 本卡落点 |
| --- | --- |
| 业务目标、用户价值、成功指标和非目标 | 业务目标是让清结算和出款前的差异可发现、可阻断、可处理、可重跑和可审计；用户价值是财务、运营和商户不再把交易成功、结算审批或外部受理误读成钱已对上；成功指标是对账差错闭环能阻断清算或出款，并能通过重跑或白名单补事实恢复；非目标是不做整条清分清算结算出款追偿生产链路。 |
| 能力地图、能力域、前台能力、后台能力和数据能力 | 能力域拆为对账来源标准化、对账任务、对账匹配、对账差错、清算/出款阻断、账龄升级、重跑、补事实白名单、审计和解释视图；前台能力只展示事实状态和不可操作原因；后台能力只做排查、审批、核销、重跑和补证据；数据能力提供来源质量、匹配强度、差错、审批、凭证、责任方和重跑报告。 |
| 业务对象、对象模型、字段口径、生命周期和状态 | 业务对象包括来源记录、标准化来源明细、来源质量结论、对账任务、匹配结果、匹配强度、差异指纹、差错单、账龄策略、阻断记录、处理动作、审批、凭证、重跑记录和补事实引用；生命周期至少包含创建、收数、等待数据、匹配、差异、阻断、有条件放行、处理中、已核销、重跑中、关闭；状态不得和清分批次、清算批次、结算单或出款单混成一个对象。 |
| 业务流程、主流程、异常流程和人工兜底 | 主流程是创建任务、标准化外部来源、收集内部事实和外部证据、按匹配强度匹配、生成差异、阻断、处理、重跑和关闭；异常流程包括来源缺失、验签失败、解析失败、重复来源、候选匹配、金额不符、状态不符、漏单、重复、外部非终态、规则待确认和凭证缺失；人工兜底只能补证据、审批、核销、重跑或发起白名单补事实，不能直接改账。 |
| 规则矩阵、触发条件、判断逻辑、优先级和版本 | 规则矩阵包含账实、账账、单账、余额一致性、来源质量、匹配强度、差异等级、阻断等级、低风险有条件放行、白名单动作、审批级别、账龄升级、重跑范围和规则版本；重大差错优先级高于清算候选、结算锁定和出款提交。 |
| 运营后台、指标、报表、审计和数据口径 | 运营后台显示任务范围、差异类型、金额币种、责任方、阻断对象、不可操作原因、下一步动作、审批和凭证；指标和报表只消费只读差错结果；审计必须包含操作者、复核人、原因、凭证、traceId、前后状态和脱敏证据引用。 |
| 风险、待确认、验收、确认方和发布 | 风险是把对账字段当作完整对账系统、把未验证外部来源或候选匹配自动对平、把低风险差异静默放行、把外部受理展示为成功、账龄长期悬挂、或用后台直接改账；待确认项包括模块物理落点、表结构、补事实白名单、外部规则、权限审计和生产 Runbook；验收由产品、架构、财务、运营、测试、安全和合规/通道确认方共同复核；发布只在单独 Grant 中补齐。 |

## 5. architectureReviewMap

| 架构审查项 | 本卡落点 |
| --- | --- |
| 背景、目标、非目标、成功标准 | 背景是清结算与对账已具备产品/系分/TDD 目标态，但服务级 H2 闭环缺失；目标是把 B7 第一切片收敛为对账差错闭环；非目标是不混入完整清分、清算、结算、出款、追偿、治理或 P2 业务；成功标准是候选 Red 能追溯到 PRD、DSL、系分、TDD、写入范围和停止条件。 |
| 现状、约束、问题和影响范围 | 现状是 `reconciliation-*` 模块骨架、出款前准入候选、LedgerEntry 对账字段和清算策略契约存在；约束是 B7 未获独立 Grant 前不可写 Java、测试、DDL/H2 或公共契约；问题是没有对账任务、匹配、差错、重跑和白名单补事实的最小服务级证明；影响范围覆盖 reconciliation、transaction、ledger、wallet 查询、测试 H2 schema 和运营解释视图。 |
| 核心决策、职责边界和取舍 | 核心决策是先交付对账差错闭环，再交付清分、清算、结算和出款；`reconciliation` 只维护运营对象、匹配结果、差错和审计，资金变化必须通过交易层或账本层追加标准事实；取舍是第一轮只确认 `B7-RECON-DIFFERENCE-MVP`，不一次性打开 B7 全量对象。 |
| 接口契约、入参、错误码、幂等和兼容 | Execution Grant 必须显式列出 application service、Request/DTO、Query/DTO、错误码、幂等键、来源质量、匹配强度、差异指纹、账龄字段、规则版本、审批字段和兼容策略；未确认前不修改 transaction canonical request、ledger 公共契约、route replay 契约或出款前准入公共契约。 |
| 数据方案、事务边界、一致性、补偿和对账 | 数据方案至少需要来源记录、标准化来源明细、来源质量结论、对账任务、运行记录、匹配结果、差错单、账龄策略、处理动作、审批、凭证、重跑记录和补事实引用；事务边界必须保证差错生成、阻断和审计原子可追溯；补偿只通过重跑、重新对账、白名单调账/冲正/补事实；不得覆盖旧运行记录或历史分录。 |
| 可靠性、安全、权限、审计和告警 | 可靠性关注任务重跑幂等、差异指纹稳定、重复处理不重复补事实、并发核销不覆盖审批；安全关注高危动作职责分离、权限、脱敏、导出审批、敏感字段阻断；审计关注操作者、复核人、原因、凭证和 traceId；告警关注重大差错、重跑失败、阻断超时和规则待确认。 |
| 验证方案、测试、静态检查和回归 | 每个候选切片必须先写目标 Red，再按 Grant 跑对应 `just test-one ...`、`just test-reconciliation` 或相关分组、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`；首轮 Red 必须覆盖外部来源未验证、候选匹配误对平、账龄超期和白名单默认关闭；既有通过测试只能作为回归资产。 |
| 发布、灰度、回滚、风险和待确认 | 本卡不进入生产发布；后续编码若触碰公共契约、DDL/H2、外部规则、敏感数据、补事实白名单、出款提交或运营导出，必须补发布、灰度、回滚、Runbook、风险和待确认项。 |

## 6. 场景裁剪

| 场景 | 本卡允许进入 Round 0 的内容 | 本卡不允许声明 |
| --- | --- | --- |
| 对账来源标准化 | 来源类型、提供方、文件或批次引用、digest、验签状态、解析版本、重复识别键、标准化金额币种方向状态、来源质量和脱敏证据引用。 | 完整外部文件采集、银行/卡组织/通道协议解析、生产调度平台或敏感原文查询。 |
| 对账任务和匹配 | 任务范围、内部事实摘要、外部证据摘要、规则版本、匹配强度、匹配结果和差异指纹。 | 候选匹配自动对平、人工确认直接释放资金、完整银行/卡组织/通道协议解析或生产调度平台。 |
| 对账差错闭环 | 差错单、差异等级、账龄桶、SLA、阻断清算/出款、责任方、处理动作、审批、凭证、重跑和核销。 | 后台直接改账、直接改历史分录、直接改余额投影或绕过交易/账本追加资金事实。 |
| 补事实白名单 | 首轮默认关闭；只定义允许的动作类型、来源单据、审批号、证据引用、幂等键、原事实引用、操作者和失败无副作用断言。 | 泛化 actionType、无审批补账、无原事实引用调账、任意运营动作生成资金事实。 |
| 清算准入阻断 | 重大对账差错阻断清算候选或清算确认。 | 完整清分批次、完整清算批次确认和 `CLEARING -> AVAILABLE` 实现。 |
| 出款准入阻断 | 出款前准入引用差错状态、规则确认状态和不可操作原因。 | 完整出款单生命周期、银行出款提交、回单成功/失败/退回处理。 |
| 收益分润与激励结算验收 | 只允许把收益应得项、收益参与方、两级归因快照、GMV 阶梯或利润口径、规则版本、审批和账户解析作为 B7 生产验收输入。 | 不实现代理系统、无限级分销、GMV 统计、利润计算、KPI 引擎、薪税系统、税务会计最终确认或营销规则引擎。 |
| VCC / 全球账户 / 收单 | 只作为后续 P2 能力依赖的差错闭环输入。 | 卡组织 clearing 文件、VA 银行流水匹配、收单清分清算实现或外部协议栈。 |

## 7. interfacePlacementCandidate

以下落包只作为编码准入候选，不等于授权写入。若进入编码，Execution Grant 必须逐项确认接口名、Request/DTO、错误码、依赖方向、表结构和验证命令。

| 能力 | 候选接口 | face 包 | impl 包 | 准入切片 |
| --- | --- | --- | --- | --- |
| 对账来源标准化 | `ReconciliationSourceApplicationService` | `com.wind.funds.reconciliation.application.source` | `com.wind.funds.reconciliation.application.source.impl` | B7-RECON-DIFFERENCE-MVP |
| 对账任务 | `ReconciliationTaskApplicationService` | `com.wind.funds.reconciliation.application.task` | `com.wind.funds.reconciliation.application.task.impl` | B7-RECON-DIFFERENCE-MVP |
| 对账匹配 | `ReconciliationMatchingApplicationService` | `com.wind.funds.reconciliation.application.matching` | `com.wind.funds.reconciliation.application.matching.impl` | B7-RECON-DIFFERENCE-MVP |
| 差错生命周期 | `ReconciliationDifferenceApplicationService` | `com.wind.funds.reconciliation.application.difference` | `com.wind.funds.reconciliation.application.difference.impl` | B7-RECON-DIFFERENCE-MVP |
| 阻断与放行 | `ReconciliationGateApplicationService` | `com.wind.funds.reconciliation.application.gate` | `com.wind.funds.reconciliation.application.gate.impl` | B7-RECON-GATE |
| 补事实白名单 | `ReconciliationAdjustmentCommandApplicationService` | `com.wind.funds.reconciliation.application.adjustment` | `com.wind.funds.reconciliation.application.adjustment.impl` | B7-RECON-ADJUSTMENT |
| 出款解释 | `PayoutExplainabilityApplicationService` | `com.wind.funds.reconciliation.application.payout` | `com.wind.funds.reconciliation.application.payout.impl` | B7-PAYOUT-EXPLAIN |

Request/DTO 默认落 `com.wind.funds.reconciliation.model.request`、`com.wind.funds.reconciliation.model.query` 和 `com.wind.funds.reconciliation.model.dto`；若模型数量超过单一切片需要，可在 Execution Grant 中允许增加 `task`、`matching`、`difference`、`gate`、`adjustment` 或 `payout` 子包。禁止让 `reconciliation-impl` 直接访问 wallet/transaction/ledger 的 Mapper 或 Entity。

## 8. writeScopeCandidate

| 范围 | 候选授权 |
| --- | --- |
| 首批测试资产 | `ReconciliationSourceStandardizationTests`、`ReconciliationDifferenceLifecycleTests`、`ReconciliationMatchingRuleTests`、`ReconciliationDifferenceAgingTests`、`ClearingSettlementBoundaryTests`、`PayoutPreflightTests` 或等价目标 Red。 |
| 生产实现 | 只有 Red 证明真实缺口后，才允许在 `reconciliation-face`、`reconciliation-impl` 增加 application facade、DTO、最小实现和必要查询；交易层或账本层只在独立列名的白名单补事实场景下被委派。 |
| 公共契约 | 默认不允许破坏既有 face/core 请求字段；如必须新增 reconciliation face 契约、错误码、状态枚举或 DTO，必须在 Execution Grant 明确命名、兼容策略和回归范围。 |
| DDL/H2 schema | 默认不允许修改；若来源记录、标准化来源明细、来源质量结论、对账任务、匹配结果、匹配强度、差错单、账龄字段、处理动作、重跑记录、审批或凭证需要表结构，必须单独扩权到 DDL/H2、Entity、Mapper、MapStruct 和表结构测试。 |
| 运行时配置 | 默认不允许修改；批处理窗口、告警阈值、白名单动作、规则版本、导出策略和开关都必须在生产变更或后续 Grant 中声明。 |

### 8.1 harnessScopeCandidate

| Harness 字段 | 候选范围 |
| --- | --- |
| 写入文件 | 未确认 Execution Grant 前只允许写本文档、TDD README、GSD + Goal 推进计划、OpenSpec project 和 Harness tasks 索引。确认后写入文件必须按 Grant 中列出的测试、facade、Request/DTO、实现、Entity、Mapper 或 schema 范围执行。 |
| 写入范围 | 首轮默认只允许 B7 对账差错闭环相关测试和最小契约；若选择 `contract-only` 不允许真实资金事实委派；若选择 `ddl-backed` 才允许表结构、Entity、Mapper 和 H2 schema；若选择 `service-flow-backed` 才允许真实服务流程、幂等、阻断、重跑和白名单补事实委派。 |
| 只读范围 | `docs/产品设计/03-清结算与对账.md`、`docs/产品设计/05-产品验收与TDD用例矩阵.md`、`docs/DSL设计`、`docs/系分设计/03-清结算与对账系分设计.md`、`docs/TDD设计`、`openspec`、`reconciliation-*`、`transaction-*`、`ledger-*`、`wallet-*`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| 只读参考 | 当前代码中的 `LedgerEntry` 对账字段、`LedgerReconcileStatus`、`SettlementPolicySpecTests`、出款前准入候选测试和平台 CLEARING/SETTLEMENT 账户能力只作为差距复核参考，不等于 B7 生产可用或编码授权。 |

## 9. noWriteScope

| 禁止范围 | 说明 |
| --- | --- |
| 完整 B7 全量实现 | 不一次性实现完整清分、清算、结算、出款、追偿和运营后台。 |
| 直接改账 | 不通过 reconciliation 直接修改历史交易、LedgerEntry、余额投影、交易投影或外部流水。 |
| 泛化补事实 | 不用泛化 actionType 或无审批命令生成资金事实；必须白名单、凭证、幂等和原事实引用齐备。 |
| 外部协议实现 | 不实现银行、卡组织、ACH、SWIFT、PSP、收单、跨境或 FX 执行协议。 |
| P2 业务专项 | 不实现 VCC clearing 文件、全球账户 VA/银行流水匹配、收单 capture/dispute 生产能力。 |
| 代理和绩效系统 | 不实现无限级代理、代理关系图谱、GMV 统计、利润计算、KPI 引擎、薪税处理、税务会计确认或营销规则引擎；收益分润只作为清结算验收输入。 |
| 治理和指标 | 不新增归档、Manifest、checkpoint、watermark、指标水位或正式重投影 apply。 |
| 敏感数据 | 不引入完整 PAN、CVV、token secret、银行账号、外部报文原文、生产配置或跨境证明文件。 |

## 10. redCandidateSet

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `R0-B7-RECON-001` | 已过账交易与外部证据金额或状态不一致时，系统是否能生成差错并阻断清算/出款。 | 重大差错未闭环不得生成清算候选、确认清算批次、锁定结算单或提交出款。 | 对账任务、匹配结果、差错单、阻断记录、责任方、规则版本、审计摘要和不可操作原因。 | 不得直接改历史分录、余额投影、交易投影或把外部受理写成成功。 | 差异类型、金额币种、规则版本、阻断对象、无资金副作用、审计字段、解释状态。 | `ReconciliationDifferenceLifecycleTests`、`ClearingSettlementBoundaryTests`。 | `just test-one ReconciliationDifferenceLifecycleTests tests` 或 Grant 指定命令。 | 需要 DDL/H2、公共契约或状态枚举但未授权。 |
| `R0-B7-RECON-002` | 对账重跑是否生成新运行记录和差异报告，而不是覆盖旧审批和凭证。 | 重跑不得覆盖旧运行记录、旧差异报告、审批、凭证或核销结果。 | 新 runSn、原任务引用、规则版本、差异报告、重跑原因、操作者和审计。 | 不得删除旧运行记录，不得无新证据重开已关闭差错。 | runSn 递增或稳定唯一、旧记录保留、审批凭证保留、重跑幂等、失败无副作用。 | `ReconciliationTaskServiceTests`、`ReconciliationMatchingRuleTests`。 | `just test-one ReconciliationTaskServiceTests tests` 或 Grant 指定命令。 | 需要任务表、运行记录表或差异报告表但未授权。 |
| `R0-B7-RECON-003` | 差错核销或补事实是否必须经过白名单、审批、凭证和重新对账。 | 差错处理不能直接改账；补事实只通过白名单动作追加标准事实。 | 处理动作、审批号、凭证引用、原事实引用、幂等键、补事实命令摘要、重新对账引用。 | 不得无审批核销、不得无原事实引用调账、不得绕过交易层或账本层补账。 | 白名单动作、审批、凭证、幂等、原事实、重跑结果、失败无 route/posting/entry。 | `ReconciliationDifferenceLifecycleTests`、`FundsOperationPermissionBoundaryTests`。 | `just test-reconciliation` 或 Grant 指定命令。 | 补事实需要 transaction/ledger 公共契约但未列名授权。 |
| `R0-B7-RECON-004` | 外部来源未验签、解析失败、重复或缺主体映射时，是否会被误用于自动匹配或自动对平。 | 未验证来源不得生成资金事实、不得标记对平、不得释放清算/出款。 | 来源记录、标准化来源明细、来源质量结论、digest、解析版本、重复识别键、等待或失败原因。 | 不得写入敏感原文，不得绕过来源质量直接生成匹配结果，不得把缺文件当作对账通过。 | sourceQuality、verificationStatus、parserVersion、dedupeKey、DATA_READY 阻断、无 route/posting/entry。 | `ReconciliationSourceStandardizationTests`、`ReconciliationMatchingRuleTests`。 | `just test-one ReconciliationSourceStandardizationTests tests` 或 Grant 指定命令。 | 需要来源表、解析状态枚举、证据脱敏字段或外部协议解析但未授权。 |
| `R0-B7-RECON-005` | 候选匹配、人工确认或未匹配是否会被误当作自动对平。 | 只有 `EXACT_MATCH` 或带规则版本的 `RULE_MATCH` 才允许自动对平；其他匹配强度不得释放资金。 | matchStrength、autoBalance、规则版本、候选原因、人工审批或差错引用。 | 不得让 `CANDIDATE_MATCH`、`MANUAL_CONFIRMED` 或 `UNMATCHED` 进入 BALANCED；不得用人工备注替代规则版本。 | 匹配强度、自动对平标记、规则版本、差错或复核状态、无资金副作用。 | `ReconciliationMatchingRuleTests`、`ReconciliationDifferenceLifecycleTests`。 | `just test-one ReconciliationMatchingRuleTests tests` 或 Grant 指定命令。 | 需要匹配强度枚举、匹配结果表或放行规则引擎但未授权。 |
| `R0-B7-RECON-006` | 等待数据、有条件放行、候选匹配、挂账或恢复处理中是否会长期悬挂。 | 超 SLA 的差异必须升级阻断或人工复核，不得长期停留在可放行状态。 | ageBucket、dueAt、SLA、escalationReason、blockedObject、responsibleParty 和审计。 | 不得无到期动作、不得无限续放、不得到期后继续提交清算/出款。 | 账龄桶、到期动作、升级结果、阻断对象、审计、无资金副作用。 | `ReconciliationDifferenceAgingTests`、`ClearingSettlementBoundaryTests`。 | `just test-one ReconciliationDifferenceAgingTests tests` 或 Grant 指定命令。 | 需要调度器、时间推进框架、状态枚举或清算/出款阻断契约但未授权。 |
| `R0-B7-CLS-001` | 清算缺前置对账时，是否仍生成候选或确认清算。 | 清算候选和清算确认依赖前置对账放行；候选本身不改变余额。 | 清算前置对账引用、阻断原因、候选状态、无余额变化。 | 不得让可清分明细、清分批次或清算候选直接入账。 | `CLEARING` 余额不变、候选状态、阻断原因、审计。 | `ClearingSettlementBoundaryTests`。 | `just test-reconciliation` 或 Grant 指定命令。 | 需要清分/清算表结构但未授权。 |
| `R0-B7-PAYOUT-001` | 外部出款受理、处理中、在途或金额不一致时，是否会被展示为成功。 | 外部非终态不得关闭 `SETTLEMENT/IN_TRANSIT`，金额不一致必须进入差错。 | factStatus、displayStatus、operationStatus、不可操作原因、差错引用和审计。 | 不得展示到账成功、不得自动放行、不得重复回退。 | 三态解释、金额币种、外部引用脱敏、无成功资金事实。 | `PayoutReceiptMismatchTests`、`PayoutExplainabilityTests`。 | `just test-reconciliation` 或 Grant 指定命令。 | 需要出款单生命周期或外部回单契约但未授权。 |
| `R0-B7-OPS-001` | 高危差错处理和敏感导出是否有权限、职责分离、脱敏和审计。 | 无权限、无审批或职责未分离不得处理差错、补事实或导出敏感明细。 | 操作者、复核人、权限、审批、脱敏字段、水印、导出范围和审计结果。 | 不得导出敏感原文，不得单人绕过复核，不得审计失败仍成功。 | 权限失败、审批失败、脱敏、审计、前后值、失败无副作用。 | `FundsOperationPermissionBoundaryTests`。 | `just test-boundary` 或 Grant 指定命令。 | 需要安全/权限框架接入但未授权。 |
| `R0-B7-REVSHARE-001` | 平台员工邀请用户代理并按配置分润用户代理佣金时，收益能否进入可清分但不直接入账。 | 收益应得项、清分明细和清算候选不得增加 AVAILABLE。 | 收益应得项、用户代理金额项、平台员工二级分润金额项、归因快照、规则版本、审批和排除原因。 | 不得从报表反推，不得跳过清分，不得在清分确认时生成 route/posting/entry。 | 来源交易、利润或 GMV 口径、两级归因、账户解析、幂等键、无余额变化。 | `RevenueShareClearingAcceptanceTests` 或等价 B7 验收测试。 | `just test-reconciliation` 或 Grant 指定命令。 | 需要收益对象表、状态枚举或账户解析契约但未授权。 |
| `R0-B7-REVSHARE-002` | 收益清算确认后，用户代理净佣金、平台员工二级分润和 KPI 激励是否分别形成可解释账务事实。 | 只有清算批次确认才允许收益参与方可用或员工应付增加。 | 清算批次、收益候选、金额项、交易层委派结果、账本交易和分录引用。 | 不得把用户代理佣金、员工分润、KPI 激励和留置混成一笔总额。 | DSL 借贷平衡、余额 delta、金额项拆分、审批、审计、失败无副作用。 | `RevenueShareSettlementAcceptanceTests` 或等价 B7 验收测试。 | `just test-reconciliation` 或 Grant 指定命令。 | 需要交易层或账本层委派但未列名授权。 |
| `R0-B7-REVSHARE-003` | 退款、争议、规则缺失、超两级归因或审批缺失时，收益分润是否被阻断或进入差错。 | 逆向和差错不得修改历史分录；清算前阻断，清算后走扣减、追偿、冲正、调账或后续结算抵扣。 | 阻断记录、差错单、退款或争议引用、规则缺失原因、审批缺失原因、重新对账引用。 | 不得静默放行，不得直接改历史批次或历史分录，不得无限级扩展。 | 阻断原因、对账差错、审计、无余额副作用、重跑不覆盖旧记录。 | `RevenueShareReconciliationDifferenceTests` 或等价 B7 验收测试。 | `just test-reconciliation` 或 Grant 指定命令。 | 需要代理/KPI/税务系统实现时停止，本仓库只保留验收输入。 |

## 11. suggestedGrantSlices

| 切片 | 局部顺位 | 目标 | 首批 Red | 允许写入建议 | 不适合混入 |
| --- | --- | --- | --- | --- | --- |
| B7-RECON-DIFFERENCE-MVP | 1 | 对账来源标准化、对账任务、匹配强度、差错单、阻断、账龄升级、重跑和白名单补事实准入。 | `R0-B7-RECON-001` 至 `R0-B7-RECON-006`。 | reconciliation face/impl 的最小 application facade、DTO、状态、表结构和服务测试；具体范围由 Grant 决定。 | 完整清分、清算、结算、出款、追偿、P2 轨道协议。 |
| B7-CLEARING-GATE | 2 | 清分和清算前置对账阻断。 | `R0-B7-CLS-001`。 | 清分/清算候选边界测试和必要契约。 | 清算确认入账、结算锁定和出款提交。 |
| B7-PAYOUT-EXPLAIN | 3 | 出款状态解释和金额不一致差错。 | `R0-B7-PAYOUT-001`。 | 出款解释 DTO、查询和出款回单差异测试。 | 外部银行协议、真实出款执行、完整退汇。 |
| B7-OPS-AUDIT | 4 | 高危处理权限、审批、脱敏和审计。 | `R0-B7-OPS-001`。 | 权限边界测试、审计摘要和导出脱敏测试。 | 运营后台页面和完整报表。 |
| B7-REVSHARE-ACCEPTANCE | 5 | 两级代理收益分润与员工 KPI 激励作为清结算生产可用验收用例。 | `R0-B7-REVSHARE-001`、`R0-B7-REVSHARE-002`、`R0-B7-REVSHARE-003`。 | 收益清分、收益清算、结算金额项、对账差错和解释性测试；具体契约由 Grant 决定。 | 代理关系系统、无限级分销、KPI 引擎、税务薪酬系统、营销规则引擎。 |

## 12. 外部规则核验检查点

| 规则来源 | 版本或发布日期 | 生效日期 | 适用主体或适用范围 | 适用法域 | 核验日期 | 确认方 | 确认状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 银行、通道、卡组织、ACH、SWIFT、本地清算网络、商户结算、出款回单、差错处理、准备金、追偿、财务、税务、会计和数据安全规则 | 待确认 | 待确认 | 清分、内部清算、结算、出款、对账、差错、补事实、追偿、敏感导出和审计 | 待确认 | 2026-06-07，仅完成本地候选包字段完整性核验 | 待法务、合规、财务、税务、会计、银行、通道、卡组织、安全和数据负责人确认 | 未完成外部规则时效核验和专业口径确认，不作为上线依据。 |

## 13. Round 0 验证计划

| 验证项 | 命令或方式 |
| --- | --- |
| Harness 候选结构检查 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` |
| 产品交付结构检查 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` |
| 架构交付结构检查 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` |
| 外部规则字段完整性检查 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_external_rules.py --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` |
| Markdown diff 空白检查 | `git diff --check` |
| 索引一致性 | 检索 `B7-RECON-DIFFERENCE-MVP`、`B7-RECON-DIFFERENCE-MVP-CAD-001`、`B7-REVSHARE-ACCEPTANCE`、`R0-B7-RECON-001`、`R0-B7-RECON-004`、`R0-B7-RECON-006`、`R0-B7-REVSHARE-001`、`READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` 和 `Execution Grant`；不得再出现把 B7 首切片命名为清分、清算、结算全量大包的旧恢复入口。 |

后续若获得 Execution Grant，验证命令按授权范围选择：`just test-one <TargetTest> tests`、`just test-reconciliation`、`just test-boundary`、`just compile`、`just pmd` 和必要的业务流程回归。仅修改文档时不运行编译。

## 14. 自动停止条件

出现以下任一情况，本候选包不得继续推进到代码：

1. 用户未确认单一 `Execution Grant`。
2. 未选择 `contract-only`、`ddl-backed`、`service-flow-backed` 或其他实现决策。
3. 需要 Java、测试、公共契约、状态枚举、DDL/H2 schema、Entity、Mapper、MapStruct 或运行时配置，但 Grant 未授权。
4. 需要修改 transaction、ledger、wallet、route replay、出款前准入或 core 公共契约，但 Grant 未列名。
5. 需要外部银行、卡组织、ACH、SWIFT、本地清算网络、PSP、FX 执行、收单、全球账户、VCC processor 或跨境协议实现。
6. 出现直接改账、泛化补事实、敏感原文写入、依赖反转、公有方法超过 5 个参数、生产配置或合规上线结论。
7. 工作树出现无法区分的用户改动，或验证失败且无法在授权范围内修复。

## 15. 确认模板

后续若要进入 B7 编码，请只确认一个切片：

```text
Execution Grant：B7-RECON-DIFFERENCE-MVP
Task ID：B7-RECON-DIFFERENCE-MVP-CAD-001
实现决策：contract-only / ddl-backed / service-flow-backed（三选一）
首批 Red：R0-B7-RECON-001、R0-B7-RECON-004、R0-B7-RECON-005
次批 Red：R0-B7-RECON-002、R0-B7-RECON-003、R0-B7-RECON-006
补事实白名单：默认关闭 / 调账 / 冲正 / 补事实（若开放需列动作、审批、凭证、幂等和原事实引用）
外部规则状态：仅本地字段完整性 / 已完成法务合规财务通道确认
Git 策略：auto_commit / summary_only
撤销方式：用户说“暂停/停止/撤销 B7”即停止自动推进
```

### 15.1 reconciliationDifferenceOnePageConfirmation2026-06-07

本节是一页式确认入口，用于在账本、钱包账户和交易内核具备可验证边界后，恢复清结算与对账首切片。它不替代第 3 节至第 15 节的候选明细；未收到用户明确确认前，不授权写生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。

| 确认项 | 内容 |
| --- | --- |
| `Execution Grant` | `B7-RECON-DIFFERENCE-MVP`。 |
| `Task ID` | `B7-RECON-DIFFERENCE-MVP-CAD-001`。 |
| `业务问题` | 一笔已成功、已过账、有账本分录的交易进入对账后，若金额、状态、漏单、重复、来源未验证、候选匹配不可靠或账龄超期，系统必须生成差错、阻断清算或出款、保留审批凭证，并能重跑或在白名单明确授权后补事实闭环；不得把交易成功、结算审批或外部受理误展示为钱已对上。 |
| `当前判断` | 首轮已完成 service-flow-backed 差错生命周期 MVP：代码侧具备差错来源质量、匹配强度、差错单、阻断状态、处理动作回链和重新对账幂等的服务级 H2 闭环；后续已补齐差错处理动作上下文守卫，要求回链处理必须声明动作类型、幂等键和原始事实引用。完整对账来源标准化、匹配规则引擎、账龄升级、清算/出款阻断消费和补事实命令执行服务仍未完成。 |
| `默认决策` | `implementationDecision=service-flow-backed`，`schemaDecision=minimal-reconciliation-ddl-h2-required`，`adjustmentWhitelist=closed-first`，`externalRuleDecision=local-field-check-only`。若用户只确认 `contract-only`，只能交付契约/DTO/目标 Red，不得声明 B7 生产可用。 |
| `允许写入` | 先写 `ReconciliationSourceStandardizationTests`、`ReconciliationDifferenceLifecycleTests`、`ReconciliationTaskServiceTests`、`ReconciliationMatchingRuleTests` 或 `ReconciliationDifferenceAgingTests` 目标 Red；Red 证明缺口后，按确认范围允许新增 `reconciliation-face` application facade、Request/Query/DTO、状态、`reconciliation-impl` 最小服务实现、必要 DDL/H2 schema、Entity、Mapper、MapStruct 和表结构测试。 |
| `禁止写入` | 不一次性实现完整清分、清算、结算、出款、追偿和运营后台；不直接修改历史交易、LedgerEntry、余额投影、交易投影或外部流水；不开放泛化补事实；不实现银行、卡组织、ACH、SWIFT、PSP、收单、跨境或 FX 外部协议；不实现 VCC clearing 文件、全球账户 VA/银行流水匹配或收单生产能力。 |
| `首批 Red` | `R0-B7-RECON-001`：已过账交易与外部证据金额或状态不一致时，必须生成差错并阻断清算/出款，且无资金副作用；`R0-B7-RECON-004`：未验证、解析失败、重复或缺主体映射的来源不得自动匹配或对平；`R0-B7-RECON-005`：候选匹配、人工确认或未匹配不得自动对平。 |
| `第二批 Red` | `R0-B7-RECON-002`：对账重跑必须生成新运行记录和差异报告，不覆盖旧审批、凭证、核销结果或旧运行记录；`R0-B7-RECON-003`：差错核销或补事实必须经过白名单、审批、凭证、幂等键和重新对账，首轮默认关闭；`R0-B7-RECON-006`：等待数据、有条件放行、候选匹配、挂账或恢复处理中超 SLA 必须升级阻断或人工复核。 |
| `验证命令` | 首轮 `just test-one ReconciliationDifferenceLifecycleTests tests` 或 `just test-one ReconciliationTaskServiceTests tests`；按触点补 `just test-reconciliation`、`just test-boundary`、`just compile`、提交前 `just pmd` 和 `git diff --check`。 |
| `Git 策略` | 未确认前 `summary_only`；确认时若用户同时保留 GSD-CAD 自动提交授权，目标验证通过且无停止条件时可 `auto_commit`；未明确 auto_commit 时保持 `summary_only`。 |
| `停止条件` | 未确认 implementationDecision、schemaDecision、补事实白名单或 DDL/H2 范围；需要修改 transaction、ledger、wallet、route replay、出款前准入或 core 公共契约但 Grant 未列名；出现直接改账、泛化补事实、敏感原文写入、依赖反转、外部协议实现、测试无法解释失败或工作树冲突。 |
| `交接` | `Execution Grant：GSD2-B7-RECON-DIFFERENCE-MVP-001` 和 `GSD2-B7-RECON-DIFFERENCE-MVP-002` 已完成首轮 Green；下一步若继续 B7，应确认清算/出款门禁如何消费差错状态、阻断范围、处理动作和重跑结果，不得直接跳到完整清分、清算、结算、出款或追偿。 |

### 15.2 reconciliationDifferenceFirstGreen2026-06-17

本节记录 `GSD2-B7-RECON-DIFFERENCE-MVP-001` 的首轮交付结果，只保留最终可交付口径。

| 项 | 结果 |
| --- | --- |
| 已交付能力 | 新增 `ReconciliationDifferenceApplicationService`，支持创建差错、回链处理动作或调账结果、登记重新对账结果。差错默认进入 `BLOCKED`，重跑对平前必须已有处理动作回链。 |
| 数据与契约 | 新增差错类型、严重等级、状态、来源质量和匹配强度枚举；新增 create / link / rerun 请求模型和差错 DTO；新增 `t_reconciliation_difference` H2 测试表、Entity 和 Mapper。 |
| 资金红线 | reconciliation 只登记运营差错对象和处理结果引用，不直接生成交易事实、route、posting、LedgerEntry、余额投影或交易投影；服务测试断言登记、回链和重跑均不改变账本事实。 |
| 幂等红线 | `tenantId + differenceSn` 创建幂等；同差错流水号的来源、金额、币种、来源质量、匹配强度、责任方、阻断范围、规则版本和证据引用不一致必须拒绝；同一处理动作和同一重跑流水号的事实漂移也必须拒绝。 |
| 验证证据 | `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd` 和 `git diff --check` 已通过；目标测试在沙箱内因 embedded Redis 本地端口绑定限制失败，已按权限规则在非沙箱环境重跑通过。 |
| Not Done | 完整清分、清算、结算、出款、追偿、运营后台、账龄升级、清算/出款阻断消费、外部规则确认、生产迁移脚本、对账任务运行记录、差异报告、补事实命令执行服务和运营审批流仍未完成。 |

### 15.3 reconciliationDifferenceActionGuard2026-06-17

本节记录 `GSD2-B7-RECON-DIFFERENCE-MVP-002` 的首轮交付结果，只保留最终可交付口径。

| 项 | 结果 |
| --- | --- |
| 已交付能力 | 差错处理回链新增动作白名单上下文：处理动作必须声明 `actionType`、`idempotencyKey` 和 `originalFactRef`，用于区分补事实、冲正、调账、挂账、追偿和核销等受控处理意图。 |
| 数据与契约 | 新增 `ReconciliationDifferenceActionType`；`LinkReconciliationDifferenceAdjustmentRequest`、`ReconciliationDifferenceDTO`、`ReconciliationDifference` 和 `t_reconciliation_difference` 增加处理动作类型、处理幂等键和原始事实引用。 |
| 资金红线 | 本切片只登记差错处理动作引用和守卫字段，不生成资金指令、route、posting、LedgerEntry、余额投影或交易投影；补事实、冲正、调账、追偿等真实资金影响仍必须通过后续白名单命令和标准交易/账本事实完成。 |
| 幂等红线 | 同一差错和同一处理动作重复回链时，`actionType`、`idempotencyKey` 或 `originalFactRef` 漂移必须拒绝，避免同一个处理流水被替换成另一种资金修正语义。 |
| 验证证据 | `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd` 和 `git diff --check` 已通过；目标 Spring 测试在沙箱内受 embedded Redis 本地端口限制，按权限规则在非沙箱环境重跑。 |
| Not Done | 补事实命令执行服务、交易层/账本层委派、运营审批流、审批职责分离、清算/结算/出款消费、生产迁移脚本、账龄升级和完整差异报告仍未完成。 |

### 15.4 reconciliationGateConsume2026-06-18

本节记录 `GSD2-B7-RECON-GATE-CONSUME-001` 的首轮交付结果，只保留最终可交付口径。

| 项 | 结果 |
| --- | --- |
| 已交付能力 | 新增 `ReconciliationGateApplicationService`，为清算、结算和出款等消费方提供对账差错准入判断，返回 `PASSED`、`CONDITIONALLY_PASSED` 或 `BLOCKED` 决策、阻断差错摘要、证据引用和解释。 |
| 数据与契约 | 新增 `CheckReconciliationGateRequest`、`ReconciliationGateDecisionDTO`、`ReconciliationGateBlockingDifferenceDTO`、`ReconciliationGateDecisionStatus` 和 `ReconciliationGateObjectType`；`ReconciliationDifferenceMapper` 增加按 `blockingScope` 查询命中差错的只读能力。 |
| 资金红线 | gate consumption 只读取对账差错事实，不创建清算候选、确认清算批次、锁定结算单、提交出款，也不写交易事实、route、posting、LedgerEntry、余额投影或交易投影；服务测试断言准入检查不改变账本事实。 |
| 准入红线 | 未闭环差错、已处理但重新对账未对平、动作上下文漂移被拒绝的差错均不得释放清算、结算或出款；已处理且重跑对平的差错只能条件放行，并保留来源、处理和重跑证据。 |
| 验证证据 | `just test-one ReconciliationGateApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check` 已通过；目标 Spring 测试在沙箱内受 embedded Redis 本地端口限制，按权限规则在非沙箱环境重跑。 |
| Not Done | 完整清分、清算、结算、出款、追偿、运营后台、生产迁移脚本、差异报告、补事实命令执行服务、运营审批流和外部规则确认仍未完成。 |

### 15.5 reconciliationGatePayoutPreflightConsume2026-06-18

本节记录 `GSD2-B7-RECON-GATE-CONSUME-002` 的首轮交付结果，只保留最终可交付口径。

| 项 | 结果 |
| --- | --- |
| 已交付能力 | `PayoutOrderService#checkPayoutPreflight` 已接入 `ReconciliationGateApplicationService`，在出款提交前只读消费 `PAYOUT` 阻断范围内的对账差错准入决策。 |
| 消费方边界 | 出款准入继续只返回 preflight 结果、阻断原因、服务端解释状态和证据引用；未创建出款单、未调用通道、未写交易事实、route、posting、LedgerEntry、余额投影或交易投影。 |
| 阻断口径 | 命中 `PAYOUT` 范围的未闭环差错、已处理但重跑未对平差错，都会映射为 `RECONCILIATION_BLOCKED`；已处理且重跑对平的差错允许条件放行，但必须把原差错、处理动作和重跑证据纳入 `evidenceRefs`。 |
| 创建前检查 | 若尚未生成 `payoutSn`，gate 消费对象流水使用 `settlementSn`，不破坏现有“创建出款单前先做准入检查”的契约。 |
| 验证证据 | `just test-one PayoutPreflightServiceTests tests` 和 `just test-reconciliation` 已通过；目标 Spring 测试在沙箱内受 embedded Redis 本地端口限制，按权限规则在非沙箱环境重跑。 |
| Not Done | 完整出款单生命周期、外部受理/处理中/成功/失败回单、金额不一致处理、出款表结构、生产迁移、运营审批流、完整清分清算结算消费方接入和差异报告仍未完成。 |

## 16. handoff

| 项 | 要求 |
| --- | --- |
| 恢复入口 | 优先从 `GSD2-B7-RECON-GATE-CONSUME-002` 的后继切片恢复，继续补清算/结算消费方接入、运营审批或差异报告；若要直接做补事实执行、完整清算、结算、出款或追偿，必须说明为什么跳过准入消费和白名单命令闭环。 |
| 回写位置 | `docs/TDD设计/README.md`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md`、`openspec/project.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| TDD / Review / Refactor | 确认后必须先 Red 后 Green；Review 优先检查资金不变量、模块边界、失败无副作用、补事实白名单和敏感数据；Refactor 只在 Red 变绿后做必要收敛。 |
| AI 产物复核 | 不接受空 facade、内存版业务 Service、只 mock 内部核心组件、只断言状态或数量的测试作为生产可用证据。 |
| 残余风险 | 完整清分、清算、结算、出款生命周期、追偿、运营后台、报表、外部规则和生产 Runbook 仍是 Not Done。 |
