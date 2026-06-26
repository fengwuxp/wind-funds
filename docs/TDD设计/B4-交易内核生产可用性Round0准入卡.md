# B4 交易内核生产可用性 Round 0 准入卡

## 1. 文档定位

本文档是交易内核进入生产可用 MVP 前的 Round 0 候选准入卡。这里的 B4 是 GSD Wave 编号，不表示只覆盖授权交易；本卡承接 B3 直接交易、B4 账户主体型授权交易、B5 余额控制、B6 route replay 和交易投影解释的最小补强候选。

本文档不授权修改生产代码、DDL/H2 schema、公共契约或运行时配置。用户确认完整 `Execution Grant` 后，才允许进入对应 Red/Green 实现；用户明确要求 Agent Loop / GSD + Goal 按任务计划推进时，可在受控 Plan Grant 下把低风险 Red 候选转成目标测试覆盖，但目标测试若暴露必须改生产代码的缺口，必须先停在 Red 证据并重新确认单一 Grant。2026-06-11 用户已确认并消费 `Execution Grant：B3-DIRECT-REFUND-REFERENCE-REPLAY`，该独立 Grant 只关闭直接退款原交易引用回放切片；本卡剩余 B4 候选仍需新的单一 Grant。

本卡的核心结论是：交易内核继续保持账户主体型 canonical 入参，直接交易、授权交易和余额控制均落在已解析的资金账户或信用账户主体上；支付工具、VCC 卡、VA、外部账户、预算组和 Spend Rule 只能作为 application facade 的准入输入、快照、审计证据或只读投影维度，不替换 canonical 交易请求。

## 2. authorityBaseline

| 基线项 | 当前口径 |
| --- | --- |
| 设计和任务基线 | 最新已提交设计和任务对齐输入以确认时 Git HEAD 为准；当前未提交文档变更若要作为开工依据，必须先提交或在 Execution Grant 的 `authorityBaseline` 中显式列入。 |
| 产品入口 | `docs/产品设计/02-交易路由钱包账目与投影.md` 定义直接交易、授权交易、余额控制、交易投影和支付工具 facade 分层；`docs/产品设计/05-产品验收与TDD用例矩阵.md` 定义 P1 交易入口、失败无副作用、route replay 和投影只读验收。 |
| DSL 入口 | `docs/DSL设计/支付资金底座DSL承载层设计.md` 的资金场景借贷平衡与账务期望表、Route DSL、PaymentInstrument Route DSL、Posting/Ledger DSL 和交易投影 case。 |
| 系分入口 | `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` 的交易服务契约、route replay、余额控制、交易投影、钱包 application facade 和模块边界。 |
| TDD 入口 | `docs/TDD设计/支付资金底座测试驱动设计.md` 的直接交易、授权交易、余额控制、route replay、交易投影和 Red 选择顺序；`A1-直接交易事实红线准入卡.md`、`B4-授权后继能力Round0准入卡.md` 和 `B2B4-支付工具与SpendRule生产可用性Round0准入卡.md` 作为前置拆分依据。 |
| OpenSpec 入口 | `openspec/project.md` 和 `openspec/changes/tdd-baseline-reset/tasks.md` 的当前任务优先级、DoR、B3/B4/B5/B6 覆盖索引和 GSD + Goal 基线。 |
| 当前代码证据 | A1 直接交易已有服务级 H2 回归资产；B3 直接退款原交易引用回放已通过独立 Grant 补齐 `referenceTransactionSn`、原 route snapshot 部分回放、独立退款事实和超额/缺原事实失败无副作用测试；2026-06-11 Plan Grant 已补原交易存在但 route snapshot 缺失时直接退款全链路失败无副作用测试，并补直接退款在原 route snapshot 固化旧支付工具和旧资金责任后沿原快照回放的 flow 覆盖；B4 强制完成、无授权退款、争议退款可区分性和授权后继并发竞争已进入代码基线；授权过期释放只作为历史实现痕迹，2026-06-26 目标态已裁决移除 `expire` 入口、`EXPIRE` 事件和 `EXPIRED` 终态；route replay、交易投影和余额控制已有局部测试。上述证据只能作为回归资产，不能直接声明交易内核所有生产红线已 Done。 |

## 3. grantCandidate

| 字段 | 候选取值 |
| --- | --- |
| `Task ID` | `B4-CANONICAL-REPLAY-FAILFAST-CAD-001` |
| `Execution Grant` | `B4-CANONICAL-REPLAY-FAILFAST`，待用户确认。 |
| `mvpScenario` | 一笔账户主体型交易或授权后继事件在历史绑定、资金责任、route snapshot、账本事实已经固化后，发生退款、撤销、退费、释放、争议扣回、退汇承接或重放；系统必须沿原 route snapshot 和原账户主体回放，缺快照、缺原事实、金额不闭合或当前绑定变化时必须失败且无资金副作用。 |
| `abilityBatch` | P1 交易内核 canonical replay fail-fast。只证明账户主体型直接交易、授权交易、余额控制和交易投影的原路径回放红线，不实现支付工具 application facade、完整 VCC 生命周期、清结算对账、资金数据治理或 P2 业务专项。 |
| `businessQuestion` | 运营、财务、商户和研发能否解释一笔逆向或后继事件为什么沿原资金路径处理、为什么因原快照缺失被阻断、为什么不会因当前卡绑定、当前资金责任或当前规则变化而重新选路。 |
| `moneyInvariant` | 历史资金事实不可被当前绑定、当前默认资金责任、当前预算组或当前 Spend Rule 改写；失败时不得生成 route、posting、ledger transaction、LedgerEntry、余额投影、交易投影或控制活动副作用。 |
| `productNotDone` | 不声明完整交易层、完整 dispute/chargeback case、完整余额控制调账系统、完整交易投影查询、完整 VCC clearing、全球账户退汇、清结算对账或生产发布完成。 |
| `firstRedSet` | `R0-TRX-REPLAY-001`、`R0-TRX-REPLAY-002`、`R0-TRX-PROJ-001`。余额控制调账审计和完整交易投影解释作为次级独立切片。 |
| `currentEvidence` | 既有 A1、B4、route replay、projection 和余额控制测试说明交易内核有局部基线；2026-06-11 Plan Grant 已补授权后继缺原授权交易事实 fail-fast 覆盖，目标测试直接 Green；随后用户确认并消费 `Execution Grant：B3-DIRECT-REFUND-REFERENCE-REPLAY`，已补直接退款原交易引用回放、缺原交易失败无副作用、累计超额阻断和交易分组/边界回归；随后复核 `DefaultRouteReplayServiceTests`，确认纯 route replay 边界会复用原 `RouteSnapshot` 中的支付工具快照、外部账户快照和资金责任决策，当前请求工具或账户上下文不会覆盖原快照；随后补 `FundsDirectTransactionFlowTests#testRefundWithMissingReferenceRouteSnapshotShouldRejectAndLeaveNoSideEffects`，确认原交易存在但 route snapshot 缺失时直接退款全链路 fail-fast 且无新资金或账务副作用；本轮补 `FundsDirectTransactionFlowTests#testRefundWithReferenceTransactionShouldReuseOriginalInstrumentAndFundingSnapshot`，确认直接退款 flow 在原支付快照固化旧支付工具和旧资金责任后仍沿原快照回放并保留归因。Round 0 必须继续证明剩余全链路 replay 缺口或确认已有实现覆盖，不把局部回归资产升级为全域生产 Done。 |

## 4. productReviewMap

| 产品审查项 | 本卡落点 |
| --- | --- |
| 业务目标、用户价值、成功指标和非目标 | 业务目标是让交易内核在 VCC、全球账户和清结算对账接入前，先证明账户主体型资金事实可回放、可阻断、可解释；用户价值是运营、财务和商户能看懂逆向、后继、拒绝和重放原因；成功指标是 Red 能证明原路径回放、失败无副作用、幂等和投影只读；非目标是不做支付工具 facade、完整 VCC、完整清结算或外部协议。 |
| 能力地图、能力域、前台能力、后台能力和数据能力 | 能力域拆为直接交易回归、授权后继回归、余额控制、route replay、交易投影解释、幂等摘要和失败无副作用；前台能力只暴露交易结果和不可操作原因；后台能力只提供查询、解释、差错入口和审计引用；数据能力提供原事实、原 route snapshot、交易投影和余额投影只读证据。 |
| 业务对象、对象模型、字段口径、生命周期和状态 | 业务对象包括资金交易、交易明细、授权交易、冻结单、原 route snapshot、replay request、资金指令、posting plan、ledger transaction、LedgerEntry、余额投影和交易投影；字段口径必须区分账户主体、支付工具快照、外部引用、原事实引用、幂等摘要、拒绝原因和审计字段；状态只表达交易生命周期，不把工具、卡号、预算组或父账户汇总变成账务主体。 |
| 业务流程、主流程、异常流程和人工兜底 | 主流程是接收账户主体型命令、校验原事实、回放原 route、生成资金事实、发布只读投影；异常流程包括缺原快照、缺原事实、金额超额、币种不一致、重复同键不同摘要、当前绑定变化、敏感字段越界和规则待确认；人工兜底只能进入差错、审批或补事实白名单，不直接改历史事实。 |
| 规则矩阵、触发条件、判断逻辑、优先级和版本 | 规则矩阵包含原事实存在性、原 route snapshot 完整性、金额币种闭合、账户主体一致性、幂等摘要、失败无副作用、交易投影只读和余额控制调账审计；触发条件和判断逻辑进入 Red；优先级是先回放红线，再调账审计，再投影解释。 |
| 运营后台、指标、报表、审计和数据口径 | 运营后台只展示交易事实、原路径、失败原因、外部引用、操作者、审批或差错引用；指标和报表消费只读交易投影、余额投影和账本事实；审计包含请求摘要、原事实引用、原 route snapshot、规则版本、操作者和脱敏证据。 |
| 风险、待确认、验收、确认方和发布 | 风险是用当前绑定重选路、把投影当事实、把调账当万能补账、把支付工具当交易主体，或只断言交易状态；待确认项包括公共契约、route replay 契约、投影 DTO、调账白名单、外部规则和 DDL/H2；验收由产品、架构、研发、测试、财务和运营确认；发布、灰度和回滚只在后续编码 Grant 中补齐。 |

## 5. architectureReviewMap

| 架构审查项 | 本卡落点 |
| --- | --- |
| 背景、目标、非目标、成功标准 | 背景是交易内核已有多项局部闭环，但 GSD 雷达仍缺交易内核 Round 0 候选；目标是把下一可确认切片收敛为 canonical replay fail-fast；非目标是不混入支付工具 facade、VCC P2、清结算对账或治理；成功标准是候选 Red 能追溯到 PRD、DSL、系分、TDD、写入范围和停止条件。 |
| 现状、约束、问题和影响范围 | 现状是直接交易、授权后继、route replay 和交易投影有局部资产；约束是未获 Grant 前不可写 Java、测试、DDL/H2 或公共契约；问题是后续 VCC lifecycle、全球账户退汇和清结算补事实都依赖“原路径回放失败即停”的交易内核红线；影响范围覆盖 transaction-face、transaction-impl、core route spec、ledger posting、余额投影、交易投影和 tests。 |
| 核心决策、职责边界和取舍 | 核心决策是交易内核只消费已解析账户主体和不可变快照，钱包 application facade 负责支付工具、VCC 和资金责任解析，ledger 只维护账本事实和投影；取舍是先做 replay fail-fast，不一次性打开余额调账、完整投影查询、清结算补事实或 P2 生命周期。 |
| 接口契约、入参、错误码、幂等和兼容 | 默认不修改 `FundsDirectTransactionService`、`FundsAuthorizationTransactionService`、`FundsBalanceControlService` 的 canonical 请求入参；若 Red 证明需要新增 route replay Query/DTO、错误码、幂等摘要字段或投影解释字段，Execution Grant 必须列名并说明兼容策略；不得用支付工具引用替换账户主体入参。 |
| 数据方案、事务边界、一致性、补偿和对账 | 数据方案依赖现有资金交易、交易明细、route snapshot、ledger transaction、LedgerEntry、余额投影和交易投影；事务边界必须保证失败无 route/posting/entry/projection 半截事实；补偿只能通过标准逆向交易、差错或白名单补事实；对账差错闭环留给 B7 独立 Grant。 |
| 可靠性、安全、权限、审计和告警 | 可靠性关注重复请求、同键不同摘要、并发后继事件、缺快照失败和投影重放幂等；安全关注敏感字段阻断、外部引用脱敏和权限审计；审计关注原事实引用、操作者、规则版本、请求摘要和 traceId；告警和 Runbook 作为后续生产变更待确认。 |
| 验证方案、测试、静态检查和回归 | 每个候选切片必须先写目标 Red，再按 Grant 执行 `just test-one DefaultRouteReplayServiceTests tests`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check` 中的适用命令。 |
| 发布、灰度、回滚、风险和待确认 | 本卡不进入生产发布；若后续触碰公共契约、DDL/H2、调账白名单、外部规则、敏感数据、完整 chargeback case、清结算对账或治理 apply，必须停止并补发布、灰度、回滚、风险和待确认项。 |

## 6. 场景裁剪

| 场景 | 本卡允许进入 Round 0 的内容 | 本卡不允许声明 |
| --- | --- | --- |
| 直接交易逆向 | 退款、退费和撤销必须引用原资金事实、原 route snapshot 和原 ledger transaction；缺原事实失败无副作用。 | 完整清结算追偿、外部通道退款规则或支付工具入口。 |
| 授权后继 | 完成、可信撤销、释放、退款和争议语义只作为账户主体型回归和 replay 红线输入；授权过期不再作为资金交易后继事件。 | 替换授权 canonical 请求、完整 VCC processor lifecycle 或独立 chargeback case。 |
| 余额控制 | 冻结、解冻继续证明同主体余额桶控制；余额调整必须独立证明审批、原因、凭证、幂等和失败无副作用。 | 用支付工具、预算组、父账户汇总或 Spend Rule 作为余额主体。 |
| Route replay | 原路径缺失、原快照缺失、当前绑定变化、当前资金责任变化时 fail-fast。 | 根据当前支付工具、当前预算组、当前 Spend Rule 或当前默认责任重新选路。 |
| 交易投影 | 查询和重放只读解释普通交易、无授权退款、争议退款、拒绝、释放和失败原因。 | 投影反写交易事实、账本事实或余额事实。 |
| VCC / 全球账户 | 只作为后续 P2 application facade 的依赖红线输入。 | 在交易内核中新增 VCC_ACCOUNT、卡号账本、VA 账本主体、SWIFT/ACH/FX 外部协议。 |

## 7. interfacePlacementCandidate

以下落包只作为编码准入候选，不等于授权写入。若进入编码，Execution Grant 必须逐项确认接口名、Request/DTO、错误码、依赖方向和验证命令。

| 能力 | 候选接口或服务 | face 包 | impl 包 | 准入切片 |
| --- | --- | --- | --- | --- |
| 原路径回放 fail-fast | `RouteReplayApplicationService` 或复用 `DefaultRouteReplayService` 测试入口 | `com.wind.funds.transaction.route` 或既有 route 契约 | `com.wind.funds.route` | B4-CANONICAL-REPLAY-FAILFAST |
| 授权后继回归 | 复用 `FundsAuthorizationTransactionService` | `com.wind.funds.transaction.application` | `com.wind.funds.transaction.application.impl` | B4-CANONICAL-REPLAY-FAILFAST |
| 直接交易逆向回归 | 复用 `FundsDirectTransactionService` | `com.wind.funds.transaction.application` | `com.wind.funds.transaction.application.impl` | B4-CANONICAL-REPLAY-FAILFAST 或 B3-REVERSE-REPLAY |
| 余额控制调账审计 | 复用 `FundsBalanceControlService` 或新增调账审计 DTO | `com.wind.funds.transaction.application` | `com.wind.funds.transaction.application.impl` | B5-BALANCE-ADJUST-AUDIT |
| 交易投影解释 | `FundsTransactionProjectionExplainApplicationService` 或查询 DTO | `com.wind.funds.transaction.projection` | `com.wind.funds.transaction.projection.impl` | B6-TRANSACTION-PROJECTION-EXPLAIN |

禁止新增顶层 `com.wind.funds.instrument`，禁止让 `transaction-impl` 反向依赖钱包资源服务。支付工具、VCC 和全球账户业务入口默认落钱包或业务 application facade，交易层只消费账户主体、原事实引用和快照。

## 8. writeScopeCandidate

| 范围 | 候选授权 |
| --- | --- |
| 首批测试资产 | `DefaultRouteReplayServiceTests`、`FundsAuthorizationTransactionFlowTests`、`FundsDirectTransactionFlowTests`、`DefaultRoutedFundsInstructionOrchestratorProjectionTests` 或等价目标 Red。 |
| 生产实现 | 只有 Red 证明真实缺口后，才允许在 `transaction-impl` route replay、instruction converter、command service、lifecycle saver、projection publisher 或必要 ledger posting 装配做最小修复。 |
| 公共契约 | 默认不允许修改 face/core 公共契约；`B3-DIRECT-REFUND-REFERENCE-REPLAY` 已消费的例外范围只包括 `FundsTransactionRefundRequest.referenceTransactionSn`。后续如必须新增 route replay DTO、错误码、投影解释 DTO 或调账审计字段，仍必须在新的 Execution Grant 中列名。 |
| DDL/H2 schema | 默认不允许修改；若调账审计、投影 store 或 replay digest 需要新增表字段、索引、Entity、Mapper 或 H2 schema，立即停止并扩权确认。 |
| 运行时配置 | 默认不允许修改；开关、告警阈值、调账白名单、外部规则和发布策略都必须在独立生产变更或 Grant 中声明。 |

### 8.1 harnessScopeCandidate

| Harness 字段 | 候选范围 |
| --- | --- |
| 写入文件 | 未确认完整 Execution Grant 前默认只允许写本文档、TDD README、GSD + Goal 推进计划、OpenSpec project 和 Harness tasks 索引；用户明确要求 Agent Loop / GSD + Goal 按任务计划推进时，可在 Plan Grant 下写入低风险目标测试覆盖。确认完整 Grant 后写入文件必须按 Grant 中列出的测试、facade、Request/DTO、实现或 schema 范围执行。 |
| 写入范围 | 首轮默认只允许交易内核 replay fail-fast 相关测试和最小实现；若选择 `contract-only` 不允许持久化新表；若选择 `projection-explain-backed` 才允许查询 DTO 或投影解释字段；若选择 `adjust-audit-backed` 才允许调账审计字段和 H2 schema。 |
| 只读范围 | `docs/产品设计/02-交易路由钱包账目与投影.md`、`docs/DSL设计`、`docs/系分设计/02-交易路由钱包账目与投影系分设计.md`、`docs/TDD设计`、`openspec`、`transaction-*`、`ledger-*`、`wallet-*`、`core`、`tests/src/test/resources/jdbc-schema.sql`。 |
| 只读参考 | A1 直接交易、B4 授权后继、B2B4 支付工具与 Spend Rule、B7 对账差错候选和 P2 业务能力包只作为依赖或后续输入，不自动成为本卡写入范围。 |

## 9. noWriteScope

| 禁止范围 | 说明 |
| --- | --- |
| 支付工具交易内核 | 不新增统一 `InstrumentTransactionService`，不把支付工具引用作为直接交易、授权交易或余额控制 canonical 入参。 |
| P2 业务专项 | 不实现 VCC prepaid funding、VCC lifecycle、全球账户入金、全球账户出款、FX/fee 或收单生产能力。 |
| 完整清结算对账 | 不新增对账任务、差错单、清分批次、清算批次、结算单、出款单或追偿。 |
| 资金数据治理 | 不新增 Manifest、checkpoint、watermark、治理 apply、指标水位或大数据归档。 |
| 直接改账 | 不通过投影、replay 或余额控制直接修改历史交易、历史分录或余额投影。 |
| 敏感数据和外部协议 | 不引入完整 PAN、CVV、token secret、银行账号、外部报文原文、SWIFT/ACH/卡组织协议实现或生产配置。 |

## 10. redCandidateSet

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `R0-TRX-REPLAY-001` | 原 route snapshot 或原事实缺失时，逆向或后继交易是否会失败且无资金副作用。 | 缺原路径不得根据当前绑定、当前资金责任或当前规则重算资金路径。 | 可解释失败原因、原业务引用、无账务事实和无投影副作用。 | 不得生成 route、posting、ledger transaction、LedgerEntry、余额投影、交易投影或控制活动。 | 缺快照、缺原事实、错币种、金额超额、失败无副作用、余额不变、幂等不污染原事实；2026-06-11 已补“授权后继缺原授权事实”覆盖；B3 已补直接退款原交易引用回放、缺原交易和累计超额阻断；已补原交易存在但 route snapshot 缺失时直接退款全链路失败无副作用覆盖。 | `DefaultRouteReplayServiceTests`、`FundsAuthorizationTransactionFlowTests`、`FundsDirectTransactionFlowTests`。 | 已执行 `just test-one FundsDirectTransactionFlowTests tests`，51 tests 通过；已执行授权 flow、直接交易 flow、`just test-transaction` 和 `just test-boundary` 的相关回归。 | 需要修改新的 route replay 公共契约、其他 Request/DTO、错误码或 H2 schema 但未授权。 |
| `R0-TRX-REPLAY-002` | 支付工具换绑、资金责任变化、预算或 Spend Rule 变化后，历史退款、释放或争议事件是否仍沿原路径回放。 | 历史资金事实只能消费原 route snapshot、原账户主体和原快照版本。 | 原 route snapshot、原账户主体、原绑定摘要、原责任决策引用、交易事实和审计引用。 | 不得按当前绑定、当前默认资金责任、当前预算组或当前 Spend Rule 重新选路。 | 原快照版本、原主体、金额闭合、重复请求幂等、当前关系变化不影响回放、失败无副作用；2026-06-11 已由 `DefaultRouteReplayServiceTests` 验证纯 route replay 边界会复用原支付工具、外部账户和资金责任快照，不被当前请求上下文覆盖；本轮已由直接退款 flow 验证原支付快照固化旧支付工具和旧资金责任后，后续退款交易仍沿原快照回放并保持余额、分录和 route snapshot 归因一致；授权后继、争议/拒付、VCC lifecycle 和交易投影仍需后续切片证明。 | `DefaultRouteReplayServiceTests`、`FundsAuthorizationTransactionFlowTests`、`FundsDirectTransactionFlowTests`。 | 已执行 `just test-one DefaultRouteReplayServiceTests tests`，9 tests 通过；已执行 `just test-one FundsDirectTransactionFlowTests tests`，51 tests 通过；后续更大组合按 Grant 指定 `just test-transaction` 或相关 flow。 | 需要新增支付工具 application facade 或 VCC lifecycle 契约时停止。 |
| `R0-TRX-PROJ-001` | 普通退款、无授权退款、争议退款、授权拒绝、释放和失败能否在交易投影中可区分且只读。 | 投影只能消费交易事实、route snapshot、ledger fact 和审计摘要，不得反写资金事实。 | 交易投影解释、事实状态、展示状态、操作状态、外部引用脱敏、失败原因和审计摘要。 | 不得把投影写回交易状态、账本分录、余额桶或 route snapshot。 | 投影可区分性、只读边界、重放幂等、敏感字段脱敏、缺事实失败或跳过可解释。 | `DefaultRoutedFundsInstructionOrchestratorProjectionTests`、交易投影查询测试。 | `just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests` 或 Grant 指定命令。 | 需要新增投影 store、查询 DTO 或治理 checkpoint 但未授权。 |
| `R0-TRX-ADJUST-001` | 余额调整是否必须有审批、原因、凭证、操作者、原事实或差错引用。 | 调账不是任意补账；无审批或无来源不得改变余额。 | 调账命令、审批号、凭证引用、原因、操作者、幂等摘要、账务事实和审计。 | 不得无审批调账，不得用支付工具、预算组或 Spend Rule 作为余额主体，不得绕过对账差错白名单。 | 审批、凭证、原因、操作者、主体、账目、金额币种、幂等、失败无 route/posting/entry/projection。 | `FundsBalanceControlFailureFlowTests` 或新增调账审计 flow 测试。 | `just test-one FundsBalanceControlFailureFlowTests tests` 或 Grant 指定命令。 | 需要 DDL/H2、审计字段、差错白名单或外部规则但未授权。 |

## 11. suggestedGrantSlices

| 切片 | 局部顺位 | 目标 | 首批 Red | 允许写入建议 | 不适合混入 |
| --- | --- | --- | --- | --- | --- |
| B4-CANONICAL-REPLAY-FAILFAST | 1 | 原路径回放、缺快照失败、当前绑定变化不重选路和失败无副作用。 | `R0-TRX-REPLAY-001`、`R0-TRX-REPLAY-002`。 | route replay 和交易 flow 目标测试；Red 证明缺口后最小修 transaction route replay 或 converter。 | 支付工具 facade、VCC lifecycle、清结算对账、治理 apply。 |
| B3-DIRECT-REFUND-REFERENCE-REPLAY | 已消费 | 直接退款携带原支付交易引用，并在缺原事实或超额时 fail-fast。 | `R0-DIRECT-REFUND-REF-001` 已落地。 | 已新增 `FundsTransactionRefundRequest.referenceTransactionSn`、converter、route replay policy、lifecycle 和 `FundsDirectTransactionFlowTests` 覆盖；后续不得沿用本 Grant。 | 授权后继、支付工具 facade、VCC lifecycle、清结算对账、DDL/H2。 |
| B6-TRANSACTION-PROJECTION-EXPLAIN | 2 | 普通退款、无授权退款、争议退款、授权拒绝、释放和失败原因在交易投影中可区分。 | `R0-TRX-PROJ-001`。 | 只读 query DTO、projection explanation 或测试；默认不改事实写入。 | 投影反写事实、完整治理重放、支付工具流水全量视图。 |
| B5-BALANCE-ADJUST-AUDIT | 3 | 余额调整审批、原因、凭证、操作者、原事实或差错引用和幂等。 | `R0-TRX-ADJUST-001`。 | balance control 目标测试；Red 证明缺口后最小修请求校验或审计字段，DDL/H2 需扩权。 | 对账差错全流程、运营后台、预算组入账。 |
| B3-REVERSE-REPLAY-REGRESSION | 4 | 直接交易退款、退费和提现后继路径的原事实和失败无副作用回归。 | 可从 `A1-RED-002` 派生。 | `FundsDirectTransactionFlowTests` 回归补强。 | 授权后继、清结算、P2 轨道。 |

## 12. 外部规则核验检查点

| 规则来源 | 版本或发布日期 | 生效日期 | 适用主体或适用范围 | 适用法域 | 核验日期 | 确认方 | 确认状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 银行、卡组织、ACH、SWIFT、本地清算网络、发卡处理商、收单机构、FX、客户资金、备付、财务、税务、会计、合规和数据安全规则 | 待确认 | 待确认 | 交易后继事件、退款、退费、拒付、退汇、调账、外部引用、敏感数据和审计 | 待确认 | 2026-06-07，仅完成本地候选包字段完整性核验 | 待法务、合规、财务、税务、会计、银行、通道、卡组织、安全和数据负责人确认 | 未完成外部规则时效核验和专业口径确认，不作为上线依据。 |

## 13. Round 0 验证计划

| 验证项 | 命令或方式 |
| --- | --- |
| Harness 候选结构检查 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` |
| 产品交付结构检查 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` |
| 架构交付结构检查 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` |
| 外部规则字段完整性检查 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_external_rules.py --file docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` |
| Markdown diff 空白检查 | `git diff --check` |
| 索引一致性 | 检索 `B4-CANONICAL-REPLAY-FAILFAST`、`B4-CANONICAL-REPLAY-FAILFAST-CAD-001`、`R0-TRX-REPLAY-001`、`PARTIAL_COVERAGE_ADDED_BY_PLAN_GRANT_NOT_DONE` 和 `Execution Grant`；不得再把交易内核状态保留为 `NEEDS_ROUND0`。 |

后续若获得 Execution Grant，验证命令按授权范围选择：`just test-one <TargetTest> tests`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd` 和必要的业务流程回归。仅修改文档时不运行编译。

## 14. 自动停止条件

出现以下任一情况，本候选包不得继续推进到代码：

1. 用户未确认单一 `Execution Grant`。
2. 需要 Java、测试、公共契约、状态枚举、DDL/H2 schema、Entity、Mapper、MapStruct 或运行时配置，但 Grant 未授权。
3. 需要把直接交易、授权交易或余额控制 canonical 请求改成支付工具引用。
4. 需要新增统一 `InstrumentTransactionService` 或顶层 `com.wind.funds.instrument`。
5. 需要实现 VCC prepaid funding、VCC lifecycle、全球账户、收单、ACH/SWIFT/FX、完整清结算对账、完整 chargeback case 或治理 apply。
6. 需要让支付工具、卡号、PAN、token、VA、外部账户、预算组、Spend Rule 或父账户汇总成为账本主体、route leg、posting subject、LedgerEntry subject 或余额投影主体。
7. 出现投影反写事实、直接改账、泛化补事实、敏感原文写入、依赖方向反转、公有方法超过 5 个参数、生产配置或合规上线结论。
8. 工作树出现无法区分的用户改动，或验证失败且无法在授权范围内修复。

## 15. 确认模板

后续若要进入交易内核编码，请只确认一个切片：

```text
Execution Grant：B4-CANONICAL-REPLAY-FAILFAST
Task ID：B4-CANONICAL-REPLAY-FAILFAST-CAD-001
实现决策：contract-only / replay-backed（二选一）
首批 Red：R0-TRX-REPLAY-001
次批 Red：R0-TRX-REPLAY-002、R0-TRX-PROJ-001
允许公共契约：不允许 / 允许新增 route replay 错误码或解释 DTO（需列名）
DDL/H2：不允许 / 允许新增指定字段或索引（需列名）
外部规则状态：仅本地字段完整性 / 已完成法务合规财务通道确认
Git 策略：auto_commit / summary_only
撤销方式：用户说“暂停/停止/撤销交易内核”即停止自动推进
```

### 15.1 canonicalReplayOnePageConfirmation2026-06-07

本节是一页式确认入口，用于在账本账目、钱包账户和资金责任基线之后恢复交易内核首切片。它不替代第 3 节至第 15 节的候选明细；未收到用户明确确认前，不授权写生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。

| 确认项 | 内容 |
| --- | --- |
| `Execution Grant` | `B4-CANONICAL-REPLAY-FAILFAST`。 |
| `Task ID` | `B4-CANONICAL-REPLAY-FAILFAST-CAD-001`。 |
| `业务问题` | VCC lifecycle、全球账户退汇、清结算补事实和交易投影解释都依赖交易内核能沿原 route snapshot 与原账户主体回放；缺原快照、缺原交易事实、金额不闭合或当前绑定变化时必须 fail-fast，不能重新选路或生成半截资金事实。 |
| `当前判断` | 直接交易、授权后继、route replay 和交易投影已有局部基线，但交易投影解释、余额调账审计以及授权/争议/VCC lifecycle 等更大组合回放仍需后续单一 Grant。2026-06-11 复核确认授权后继缺原事实已覆盖；B3 已单独补齐直接退款原交易引用回放；纯 route replay 边界已覆盖当前工具、外部账户或资金责任上下文不覆盖原快照；随后补齐原交易存在但 route snapshot 缺失时直接退款全链路失败无副作用；本轮补齐直接退款交易 flow 下原支付快照固化旧支付工具和旧资金责任后仍沿历史快照回放，`FundsDirectTransactionFlowTests` 51 tests 通过。 |
| `默认决策` | `implementationDecision=replay-backed-on-existing-contract`，`publicContractDecision=no-public-contract-change`，`schemaDecision=no-ddl`，`canonicalInputDecision=account-subject-only`。B3 已消费的公共契约例外仅限 `FundsTransactionRefundRequest.referenceTransactionSn`；若要新增错误码、解释 DTO、投影 store、调账审计字段、其他 Request/DTO 或 DDL/H2，必须在确认文本中显式追加。 |
| `允许写入` | 先写 `DefaultRouteReplayServiceTests`、`FundsAuthorizationTransactionFlowTests` 或 `FundsDirectTransactionFlowTests` 中的目标 Red；Red 证明缺口后，仅允许在 `transaction-impl` route replay、instruction converter、command service、lifecycle saver、projection publisher 或必要 ledger posting 装配做最小修复。 |
| `禁止写入` | 不新增统一 `InstrumentTransactionService`；不把支付工具引用、VCC 卡、VA、外部账户、预算组、Spend Rule 或父账户聚合视图作为直接交易、授权交易或余额控制的 canonical 入参；不实现 VCC prepaid funding、VCC lifecycle、全球账户、收单、完整清结算对账、完整 dispute/chargeback case 或治理 apply。 |
| `首批 Red` | `R0-TRX-REPLAY-001`：原 route snapshot 或原事实缺失时，逆向或后继交易必须失败且无资金副作用。授权后继缺原事实已补覆盖；直接退款原交易引用回放已由 B3 消费闭合；原交易存在但 route snapshot 缺失的直接退款 flow 已补覆盖。 |
| `第二 Red` | `R0-TRX-REPLAY-002`：支付工具换绑、资金责任变化、预算或 Spend Rule 变化后，历史退款、释放或争议事件仍只能沿原路径回放；不得按当前关系重新选路。纯 route replay resolver 边界已验证；直接退款交易 flow 子场景已证明余额、账本事实和退款 route snapshot 归因沿原支付快照回放；授权后继、争议/拒付、VCC lifecycle 和交易投影仍需后续切片。 |
| `后续拆分` | `R0-TRX-PROJ-001` 交易投影解释和 `R0-TRX-ADJUST-001` 余额调账审计不混入首批 Red；需要时另起 `B6-TRANSACTION-PROJECTION-EXPLAIN` 或 `B5-BALANCE-ADJUST-AUDIT`。 |
| `验证命令` | 已执行 `just test-one DefaultRouteReplayServiceTests tests`，9 tests 通过；已执行 `just test-one FundsDirectTransactionFlowTests tests`，51 tests 通过；此前已执行授权 flow、直接交易 flow、交易分组和边界分组相关回归。后续全链路切片按触点补 `just test-transaction`、`just test-boundary`、`just compile`、提交前 `just pmd` 和 `git diff --check`。 |
| `Git 策略` | 未确认前 `summary_only`；确认时若用户同时保留 GSD-CAD 自动提交授权，目标验证通过且无停止条件时可 `auto_commit`；未明确 auto_commit 时保持 `summary_only`。 |
| `停止条件` | 需要修改公共契约、状态枚举、DDL/H2 schema、Entity、Mapper、MapStruct、运行时配置、外部协议、敏感数据处理、支付工具 facade、VCC lifecycle、清结算对账或治理 apply 但未授权；出现依赖方向反转、测试无法解释失败或工作树冲突。 |
| `交接` | 用户确认完整 `Execution Grant：B4-CANONICAL-REPLAY-FAILFAST` 后，从交易投影解释、余额调账审计或授权/争议/VCC lifecycle 更大组合 replay flow 继续；B3 直接退款原交易引用回放已消费，不得沿用该 Grant 继续扩展公共契约。若用户明确要求 Agent Loop / GSD + Goal 按任务计划推进，可在 Plan Grant 下继续补低风险目标测试覆盖，但仍不得改新的公共契约、DDL/H2、生产代码、运行时配置或 Git。 |

## 16. handoff

| 项 | 要求 |
| --- | --- |
| 恢复入口 | 优先从 `B4-CANONICAL-REPLAY-FAILFAST` 恢复；若要直接做调账审计、投影解释、支付工具 facade 或 VCC lifecycle，必须说明为什么跳过原路径回放红线。 |
| 回写位置 | `docs/TDD设计/README.md`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md`、`openspec/project.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| TDD / Review / Refactor | 确认后必须先 Red 后 Green；Review 优先检查账户主体型 canonical 请求、原 route snapshot、失败无副作用、幂等、投影只读和敏感字段阻断；Refactor 只在 Red 变绿后做必要收敛。 |
| AI 产物复核 | 不接受空 facade、内存版业务 Service、只 mock 内部核心组件、只断言状态或数量的测试作为生产可用证据。 |
| 残余风险 | 余额控制调账审计、完整交易投影解释、完整 dispute/chargeback case、清结算对账、VCC lifecycle、全球账户、治理 apply、运营后台、外部规则和生产 Runbook 仍是 Not Done。 |
