# B4 授权后继能力 Round 0 准入卡

## 1. 文档定位

本文档是 B4 授权后继能力的 Round 0 准入卡和历史执行记录。它最初把 B4-TRX-EXPIRE 和 B4-FORCE-SETTLE 已完成后的剩余授权交易缺口收敛为账户主体型 canonical 内核的候选 Execution Grant 输入页；截至 `967586c fix: 按外部引用推断无授权退款路由`，B4-NO-AUTH-REFUND 已完成首轮编码闭环、请求契约收口和路由回退加固；截至 `949b24a fix(transaction): 对齐授权争议退款审计语义`，B4-DISPUTE-SEMANTIC-ALIGNMENT 已完成首轮编码闭环；截至 `47c5269 fix(transaction): 串行化授权后继并发竞争`，B4-AUTH-RACE 已完成首轮编码闭环。

本文档本身不授权新的生产代码、测试代码、DDL/H2 schema 或运行时配置写入。B4-NO-AUTH-REFUND、B4-DISPUTE-SEMANTIC-ALIGNMENT 和 B4-AUTH-RACE 的授权模板已被消费；后续只能作为历史授权样例、回归依据和 CR 依据。2026-06-26 目标态已重新裁决：`expire` 是不可信错误状态，独立 `chargeback` 入口也进入移除队列；本文中关于 B4-TRX-EXPIRE 已完成、兼容 chargeback 保留或并发包含 expire 的旧文字只作为历史过程记录，不再作为目标态回归或计划依据。下一轮必须重新确认新的单一 MVP Execution Grant。

## 2. authorityBaseline

| 基线项 | 当前口径 |
| --- | --- |
| 代码和文档基线 | B4-TRX-EXPIRE 的 `b0666ba` / `f99f3a3` 只保留为历史实现和证据回填记录，已被 2026-06-26 目标态裁决覆盖；B4-FORCE-SETTLE 首轮实现基线为 `616dac1 feat: 补齐授权强制完成能力`，策略红线加固基线为 `3825466 fix: 收紧授权强制完成策略红线`；B4-NO-AUTH-REFUND 执行基线为用户确认时 `b69dbe5 docs: 标记 B4 无授权退款 Grant 前准备饱和`，闭环提交为 `006bcaa feat: 补齐无授权退款 canonical 能力`，后续请求契约收口基线为 `818da34 fix(transaction): 移除授权退款请求模式字段`，路由外部引用回退基线为 `967586c fix: 按外部引用推断无授权退款路由`；B4-DISPUTE-SEMANTIC-ALIGNMENT 候选入口刷新基线为 `8268ce8 docs: 刷新 B4 CAD 候选校验入口`，闭环提交为 `949b24a fix(transaction): 对齐授权争议退款审计语义`；B4-AUTH-RACE Round 0 基线为 `04865ba docs: 补齐 B4 授权并发准入包`，闭环提交为 `47c5269 fix(transaction): 串行化授权后继并发竞争`。 |
| 已关闭能力 | B4-03 授权过期释放已退出目标态：`EXPIRE` 事件、`EXPIRED` 状态、`FundsAuthorizationTransactionExpireRequest`、`FundsAuthorizationTransactionService#expire`、route replay 过期分支、ledger posting 过期释放路径和生命周期过期终态进入移除队列。B4-FORCE-SETTLE 首轮账户主体型 canonical 能力：已新增 FORCE 完成模式、强制完成策略/上限/原因/外部事实/凭证字段、普通完成与 FORCE 分支隔离、`AVAILABLE -> SETTLEMENT` 路由和受信策略红线。B4-NO-AUTH-REFUND 首轮 canonical 能力经 CR 收缩为资金层最小契约：以空原授权流水进入 no-auth 语义，请求契约不暴露 `refundMode`，保留 `externalReferenceSn`、退款原因、操作者/审计、独立退款 route、外部引用路由回退和失败无副作用测试。B4-DISPUTE-SEMANTIC-ALIGNMENT 首轮 canonical 可区分性已闭合：`settleRefund / AUTH_REFUND` 可通过争议字段承接拒付/争议语义，保留 `DISPUTE` 内部上下文、原因、凭证、外部引用、用户审计上下文、请求摘要和 route/ledger/posting/entry 审计可追溯性；独立 `chargeback` 入口不再保留。B4-AUTH-RACE 首轮并发竞争红线已闭合：同一授权的 settle / reversal 等可信后继事件并发竞争只允许一个赢家，失败方无 route、posting、ledger entry、projection 或余额副作用；expire 不再作为并发竞争事件。 |
| 产品入口 | `docs/产品设计/05-产品验收与TDD用例矩阵.md` 的 `AC-AUTH-011` 无授权强制完成、`AC-AUTH-012` 无授权直接退款、授权拒绝与拒付区分、原路径回放和授权并发红线。 |
| DSL 入口 | `docs/DSL设计/支付资金底座DSL承载层设计.md` 的 `DSL-AUTH-FORCE-CAPTURE-001`、`DSL-AUTH-REFUND-001`、`AUTHORIZATION_TRANSACTION / SETTLE` 强制完成模式、`AUTHORIZATION_TRANSACTION / AUTH_REFUND` 无授权退款模式和拒付语义承接口径。 |
| 系分入口 | `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` 的授权交易服务契约、状态流转、route replay、投影解释和资金红线。 |
| TDD 入口 | `docs/TDD设计/支付资金底座测试驱动设计.md` 的授权交易测试矩阵、B4 覆盖索引、`TDD-AUTH-*`、`TDD-ROUTE-*`、`TDD-RACE-*`、`TDD-RED-*` 和无授权退款红线 `TDD-RED-017A`。 |
| OpenSpec 入口 | `openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/design.md` 和 `openspec/changes/tdd-baseline-reset/tasks.md` 的 B4-04、B4-05、B4-07、B4-08。 |

## 3. grantCandidate

| 字段 | 候选取值 |
| --- | --- |
| `mvpScenario` | 外部没有前置授权但存在必须入账或必须退款的已确认资金事实，或已完成授权发生拒付/争议，或同一授权后续事件并发竞争。系统必须保持金额、状态、原路径、幂等和审计可解释。 |
| `abilityBatch` | B4 授权交易账户主体型 canonical 内核后继能力；本卡不包含 B4-AUTH-PI 支付工具 application facade、VCC 全生命周期、Spend Rule 生产控制、清结算对账或资金数据治理。 |
| `businessQuestion` | 运营、财务和研发能否解释一笔无授权强制完成、无授权退款、拒付/争议扣回或授权后续事件并发竞争为什么允许、为什么失败、影响哪些资金事实，以及哪些事实绝不能发生。 |
| `moneyFact` | 强制完成是已确认外部消费结果的资金事实；无授权退款是基于上游已解析外部引用的逆向资金事实；拒付是争议/扣回语义，不是授权拒绝，也不能被压缩成不可区分的普通退款。 |
| `userVisibleResult` | 用户或商户看到账单、退款、扣回或失败原因；运营和财务能追溯模式、外部引用、原因、操作者、策略、上限、route snapshot、ledger transaction、projection 和审计上下文。 |
| `productNotDone` | 不声明完整 VCC 发卡、完整 chargeback case 管理、完整清结算追偿、外部卡组织规则、Spend Rule 引擎、支付工具 facade 或治理重放生产能力。 |
| `firstRedSet` | B4-FORCE-SETTLE、B4-NO-AUTH-REFUND、B4-DISPUTE-SEMANTIC-ALIGNMENT 和 B4-AUTH-RACE 首轮已闭合；下一轮只能在授权支付工具应用入口、授权占券和权益生命周期、完整 dispute/chargeback case 或其他候选中重新选择一个单一 Execution Grant。 |
| `currentEvidence` | `b0666ba` 只证明曾实现过授权过期释放基础能力，但该能力已被 2026-06-26 裁决移出目标态；`616dac1` 和 `3825466` 已证明 B4-FORCE-SETTLE 首轮 canonical 能力与策略红线闭合；`006bcaa` 已证明 B4-NO-AUTH-REFUND 首轮 canonical 能力闭合，`818da34` 已进一步移除 `FundsAuthorizationTransactionRefundRequest#refundMode` 请求字段并保留 `NO_AUTH` 为内部上下文标签，`967586c` 已证明 no-auth `AUTH_REFUND` 可在内部 `REFUND_MODE` 缺失时由 `EXTERNAL_TRANSACTION` reference 进入专用 route resolver，显式 `DISPUTE` 或其他退款归类不被覆盖；`949b24a` 已证明 `settleRefund / AUTH_REFUND` 争议退款可在不恢复请求 `refundMode` 的前提下保留 `DISPUTE` 内部语义、审计上下文和幂等可区分性；`47c5269` 已证明同一授权可信后继事件并发竞争时只有一个合法迁移获胜，失败方无资金副作用。剩余授权支付工具应用入口、授权权益生命周期和完整 dispute case 仍是设计和任务候选，不因前述提交通过而自动获得编码授权；不得恢复独立 chargeback 交易入口。 |

### 3.1 architectureReviewMap

| 架构审查项 | 本卡落点 |
| --- | --- |
| 背景、目标、非目标、成功标准 | 背景是 B4-TRX-EXPIRE、B4-FORCE-SETTLE、B4-NO-AUTH-REFUND、B4-DISPUTE-SEMANTIC-ALIGNMENT 和 B4-AUTH-RACE 首轮已闭合，但授权支付工具应用入口、授权权益生命周期和完整 dispute/chargeback case 仍缺独立 Execution Grant；目标是保留可授权的最小 Red 选择依据；非目标是不混入支付工具 facade、VCC、Spend Rule、清结算对账或治理；成功标准是候选切片能追溯到 AC、DSL、TDD、写入范围和停止条件。 |
| 核心决策、职责边界和取舍 | 核心决策是只推进账户主体型 canonical 授权内核；支付工具、VCC、Spend Rule 和清结算能力保持独立授权；B4-FORCE-SETTLE、B4-NO-AUTH-REFUND、B4-DISPUTE-SEMANTIC-ALIGNMENT 与 B4-AUTH-RACE 首轮已闭合，下一步取舍必须重新确认一个单一授权后继切片，不一次性打开全部授权后继状态。 |
| 接口契约、入参、错误码、幂等和兼容 | B4-FORCE-SETTLE 的字段和 `authorizationTransactionSn` 条件化规则已作为回归基线；B4-NO-AUTH-REFUND 的请求契约收敛为 `authorizationTransactionSn` 空值语义、`externalReferenceSn`、退款原因、操作者/审计和普通授权链退款兼容策略，`NO_AUTH` 只由 converter 写入资金指令内部上下文；B4-DISPUTE-SEMANTIC-ALIGNMENT 新增 `disputeMode`、`disputeReason`、`disputeVoucherRef` 和 `externalDisputeRef` 一等字段，`DISPUTE` 只作为内部上下文标签，不恢复请求 `refundMode`。后续任何新增或调整接口契约、错误码、幂等摘要、查询解释字段或投影字段，都必须在新 Execution Grant 中显式列名。 |
| 数据方案、事务边界、一致性和补偿 | Red 必须证明 route snapshot、posting plan、ledger transaction、ledger entry、projection、余额桶和失败无副作用；并发切片若需要唯一约束、锁字段、版本字段或补偿路径，必须扩权确认。 |
| 可靠性、安全、权限、审计和告警 | 本卡不授权生产发布、外部协议或敏感数据；退款和拒付切片都必须按各自语义保留原因、外部引用、脱敏审计和失败可解释性；强制完成的审计最小集已作为 B4-FORCE-SETTLE 回归基线，无授权退款审计最小集已收缩为 `externalReferenceSn`、退款原因和操作者；权限和告警只作为后续 Execution Grant 待确认项。 |
| 验证方案、测试、静态检查和回归 | 每个切片先写目标 Red，再跑 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check` 中的授权命令。 |
| 发布、灰度、回滚、风险和待确认 | 本卡不进入生产发布；若后续编码触碰公共契约、DDL/H2、外部规则、清结算追偿或敏感数据，必须停止并补待确认项、风险说明、灰度和回滚策略。 |

## 4. acceptanceMap

| 设计锚点 | 本候选覆盖 | 本候选不覆盖 |
| --- | --- | --- |
| 产品验收 | `AC-AUTH-011`、`AC-AUTH-012`、`RED-003`、`RED-005`、拒付与授权拒绝区分、原路径回放和失败无副作用。 | `AC-AUTH-008` 至 `AC-AUTH-010` 发卡 Spend Controls 扩展、完整 VCC 卡处理、清结算出款追偿和外部规则最终确认。 |
| DSL caseId | `DSL-AUTH-FORCE-CAPTURE-001`、`DSL-AUTH-REFUND-001`、授权拒付承接、授权后续事件 route replay。 | 新支付工具 DSL 入口、P2 ACH/收单/全球账户 DSL、治理 apply DSL。 |
| TDD 用例 | `TDD-AUTH-*`、`TDD-AUTH-FLOW-*`、`TDD-AUTH-ERR-*`、`TDD-ROUTE-005`、`TDD-ROUTE-009`、`TDD-RACE-001` 至 `TDD-RACE-003`、`TDD-RED-003`、`TDD-RED-005`、`TDD-RED-016`、`TDD-RED-017`、`TDD-RED-017A`。 | B2 支付工具资源服务测试、B5 Spend Rule 控制生产测试、B7/B8 独立能力域测试和 P2 业务能力包测试。 |
| 产品红线 | 不伪造授权占用；无授权退款以空原授权流水进入 no-auth 语义，缺 `externalReferenceSn`、原因或操作者/审计不得静默退款；无授权退款不得携带或查询内部授权流水；拒付不得计入授权拒绝；并发不得重复释放、重复入账或让授权剩余为负。 | 卡组织时限、dispute representment、retrieval request、完整证据包运营流程和会计/合规最终口径。 |

## 5. writeScopeCandidate

| 范围 | 候选授权 |
| --- | --- |
| 目标测试资产 | 优先允许写 `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java`；必要时允许新增 B4 后继能力专用 flow 测试类或补 `DefaultRouteReplayServiceTests`。 |
| 生产实现 | 只有 Red 证明真实缺口后，才允许在 `transaction-face`、`transaction-impl`、route replay 和必要的 ledger posting 装配最小范围修复。 |
| 公共契约 | 默认不允许破坏既有请求；B4-FORCE-SETTLE 字段和 `authorizationTransactionSn` 条件化规则已作为回归基线；B4-NO-AUTH-REFUND 当前请求侧只保留 `authorizationTransactionSn` 空值语义、`externalReferenceSn`、原因和操作者/审计，`NO_AUTH` 仅作为内部上下文标签。若后续必须新增运营审批、人工差错、累计控制、查询解释或外部规则字段，Execution Grant 必须显式列名、说明普通授权链退款和无授权退款的兼容策略，并明确无授权退款不得携带内部授权流水、不得查询原授权账本交易。 |
| core 枚举和状态 | 默认不新增独立 `CHARGEBACK` 事件；若需要新增状态、事件或错误码，必须单独确认。 |
| H2 schema | 默认不允许修改 `tests/src/test/resources/jdbc-schema.sql`；若需要新增字段、表、索引或唯一约束，立即停止并扩权确认。 |

## 6. noWriteScope

| 禁止范围 | 说明 |
| --- | --- |
| 支付工具 facade | 不新增 `authorizeByInstrument`、`AuthorizationAdmissionApplicationService` 或钱包 application facade。 |
| VCC 全生命周期 | 不实现 VCC clearing 文件、processor 事件、chargeback case 全流程、卡账单或 PCI 相关能力。 |
| Spend Rule 生产控制 | 不新增规则定义、规则版本、决策日志、控制活动、预算控制投影或 Spend Rule DDL/H2。 |
| 清结算、对账和治理 | 不新增清分、清算、结算、出款、追偿单、对账差错、归档、Manifest、checkpoint、watermark 或正式重投影 apply。 |
| 交易 canonical 主体替换 | 不把账户主体型授权内核替换成支付工具、预算组、Spend Rule 或外部账户主体。 |
| 敏感数据和外部规则 | 不引入完整 PAN、CVV、token secret、银行账户敏感号、生产通道配置或未经核验的外部卡组织规则。 |

## 7. redCandidateSet

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `B4-FS-RED-001` | 外部没有前置授权但确认发生消费结果时，系统是否能按受控强制完成入账。 | 强制完成只能表达已确认外部消费事实，不得伪造授权占用或污染授权生命周期。 | settle 强制完成请求必须携带强制完成模式、受信策略或审批快照、上限、原因、审计和外部事实引用；首轮 FORCE 模式不得依赖 `authorizationTransactionSn`，不得构造 `AUTHORIZATION` reference 或查询原授权账本交易；成功后生成可追溯资金交易、route snapshot、posting plan、ledger transaction、ledger entry 和 projection。 | 不得先创建虚假授权；不得无策略或超上限入账；不得缺原因、缺审计或缺外部事实引用仍成功；不得把普通完成的 `authorizationTransactionSn` 兼容字段当作强制完成凭证。 | 状态、策略来源、上限、原因、审计、外部引用、`authorizationTransactionSn` 条件化、route snapshot、posting plan 平衡、ledger entry、余额投影、幂等和失败无副作用。 | `FundsAuthorizationTransactionFlowTests` 或新 B4 force settle flow 测试。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、必要时 `just test-transaction`、`just test-boundary`。 | 需要新增公共契约字段、错误码、状态或 H2 schema 但未授权。 |
| `B4-FS-RED-002` | 无策略、超上限、缺审计或 FORCE 模式仍携带内部授权流水时是否被阻断。 | 失败必须无资金副作用，不生成半截 route、posting、entry 或 projection。 | 返回可解释失败原因，原授权或外部事实不被污染。 | 不得生成 FAILED 以外的资金事实；不得产生 ledger transaction、posting plan 或 balance projection；不得在 FORCE 模式下回退到普通授权完成路径。 | 失败状态、失败原因、无 route/posting/entry/projection、余额不变、幂等冲突不污染原事实、普通完成与 FORCE 完成请求摘要可区分。 | `FundsAuthorizationTransactionFlowTests`。 | `just test-one FundsAuthorizationTransactionFlowTests tests`。 | 失败语义需要产品重新确认，或与现有失败事实记录口径冲突。 |
| `B4-NAR-RED-001` | 无前置授权但存在可追溯外部引用时，系统是否能直接退款回补。 | 无授权退款以 `authorizationTransactionSn` 为空进入 no-auth 语义，并引用 `externalReferenceSn`、原因和操作者/审计；不补造授权占用，不携带或查询内部授权流水，不按当前绑定重新选路。 | settleRefund 无授权退款请求不携带内部授权流水，携带 `externalReferenceSn`、退款原因和操作者/审计；成功后形成退款资金事实和可解释投影，`NO_AUTH` 只作为资金指令内部归类标签。 | 无外部引用、无原因或无操作者/审计不得静默退款；不得创建内部授权占用；不得按当前工具绑定重选路；不得把内部 `authorizationTransactionSn` 当成 no-auth refund 凭证。 | 原授权流水空值语义、外部引用、原因、操作者/审计、route、ledger entry、projection、幂等、失败无副作用。 | `FundsAuthorizationTransactionFlowTests` 或新 no-auth refund flow 测试。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、必要时 `just test-transaction`。 | 需要扩展 settleRefund Request/DTO 或 route replay 公共契约但未授权。 |
| `B4-NAR-RED-002` | 缺外部引用、缺原因、缺审计或携带内部授权流水的无授权退款是否失败且无副作用。 | 不存在最小可追溯来源或审计最小集时不能生成退款资金事实。 | 返回可解释失败或进入差错候选，不产生 ledger transaction。 | 不得静默退款；不得把普通上下文暗含为核心资金事实；不得写入不可核对投影。 | 失败原因、无 route/posting/entry/projection、余额不变、审计上下文脱敏、敏感上下文阻断。 | `FundsAuthorizationTransactionFlowTests`。 | `just test-one FundsAuthorizationTransactionFlowTests tests`。 | 产品要求转人工差错而不是直接失败，需要先确认状态和数据落点。 |
| `B4-CB-RED-001` / `TDD-RED-017B` | 已完成授权发生争议、拒付或扣回时，系统是否能与普通退款、授权拒绝区分。 | 拒付是争议/扣回语义，累计退款和拒付不得超过已完成金额，且必须沿原完成或原 route snapshot。 | settleRefund 携带拒付原因、外部引用、凭证和审计上下文；查询、投影、审计和幂等摘要能区分普通退款、NO_AUTH 退款、拒付承接和授权拒绝。 | 不得把授权拒绝记成拒付；不得要求独立 `chargeback` 服务入口作为目标态；不得把拒付压缩成不可区分的普通退款。 | 拒付原因、凭证、外部引用、原完成引用、累计金额、route replay、projection 可区分、失败无副作用。 | `FundsAuthorizationTransactionFlowTests`、交易投影相关测试。 | `just test-transaction`、必要时 `just test-business-flow`。 | 需要新增 dispute case 模型、清结算追偿或 chargeback 独立状态机。 |
| `B4-RACE-RED-001` | 同一授权的完成、撤销、过期、退款并发竞争是否会重复入账、重复释放或剩余为负。 | 同一授权同一时刻只有合法迁移生效；失败方不得产生资金副作用。 | 通过幂等键、状态版本、唯一约束、锁定策略或等价机制保证完成、撤销、过期、退款金额闭合。 | 不得重复释放 AUTHORIZATION；不得累计完成超授权；不得出现负 remaining；不得生成重复 ledger entry。 | 并发结果、最终状态、授权剩余、ledger entry 唯一性、余额桶、幂等摘要、失败无副作用。 | 授权并发 flow 测试或 service-level 并发测试。 | `just test-business-flow`、必要时 `just test-transaction`。 | 需要新增数据库唯一约束、锁字段、版本字段或 H2 schema。 |

### 7.1 existingCoverageScan（2026-06-02）

本节记录 B4-FORCE-SETTLE 编码授权前的历史只读覆盖扫描。扫描当时只读取生产代码、测试代码和设计文档，不修改生产代码、测试代码、DDL/H2 schema 或运行时配置；结论已由 `616dac1` 和 `3825466` 消费为实现与回归证据，后续只作为时间线留痕。

| redId | 既有覆盖资产 | 当前覆盖判断 | 下一轮 Red 预期失败点 |
| --- | --- | --- | --- |
| `B4-FS-RED-001` | `FundsAuthorizationTransactionSettleRequest`、`FundsAuthorizationInstructionConverter#convertToSettleInstruction`、`AuthorizationFundsInstructionRouteResolver#resolveSettle`、`FundsAuthorizationTransactionFlowTests` 中普通 settle、部分 settle、settle 后 expire 和 settle 幂等用例。 | 扫描时普通授权完成链路覆盖充分：请求必须有 `authorizationTransactionSn`，converter 无条件构造 `AUTHORIZATION` reference 并查询原授权账本交易，route resolver 依赖原授权主体解析，测试断言 AVAILABLE/AUTHORIZATION/SETTLEMENT、route、ledger transaction、entry、projection 和幂等。扫描时代码没有 FORCE 模式、`authorizationTransactionSn` 条件化、强制完成策略编码、上限、原因、凭证或无前置授权外部事实引用字段；该缺口后续已由 `616dac1` 和 `3825466` 闭合。 | 历史 Red 预期失败点已消费；后续只有返工或扩展 FORCE 策略引擎、审批快照、额度窗口、带原授权 overcapture 时才需要重新打开。 |
| `B4-FS-RED-002` | `FundsAuthorizationTransactionFlowTests#testAuthorizationSettleSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects`、`DefaultFundsInstructionLifecycleSaver` 的请求摘要和状态累计逻辑。 | 扫描时同业务流水同摘要重试、不同摘要拒绝、余额和账务事实保持不变已有普通完成覆盖；但覆盖对象仍是基于原授权的普通 settle，不覆盖无策略、超上限、缺审计或缺外部事实引用的强制完成失败路径。该失败路径后续已由 `3825466` 加固。 | 历史 Red 预期失败点已消费；后续只作为授权交易回归基线。 |
| `B4-CB-RED-001` | 历史扫描时 `FundsAuthorizationTransactionFlowTests` 已有拒付幂等和余额断言，`FundsAuthorizationTransactionService#chargeback` 仍是现有方法。 | 既有测试曾证明代码可用 `CHARGEBACK` 事件承接一类拒付资金影响；但目标态文档要求拒付不强制落独立 `chargeback` 入口，并能通过 `settleRefund` 的原因、凭证和审计上下文表达。2026-06-26 后，独立 `chargeback` 入口已进入移除目标态。 | 后续只以 `settleRefund / AUTH_REFUND` 的争议字段维护资金结果可区分性；不得在 force settle 切片内恢复独立 `chargeback`。 |

### 7.1.1 noAuthRefundCoverageScan（2026-06-02）

本节记录 B4-NO-AUTH-REFUND 编码授权前的只读覆盖扫描。扫描只读取现有生产代码、测试代码和设计文档，不修改生产代码、测试代码、DDL/H2 schema 或运行时配置；结论只作为后续 B4-NO-AUTH-REFUND Execution Grant 的失败点和写入边界输入。

| redId | 既有覆盖资产 | 当前覆盖判断 | 下一轮 Red 预期失败点 |
| --- | --- | --- | --- |
| `B4-NAR-RED-001` | `FundsAuthorizationTransactionRefundRequest`、`FundsAuthorizationInstructionConverter#convertToSettleRefundInstruction`、`AuthorizationFundsInstructionRouteResolver#resolveSettleRefund`、`DefaultRouteReplayService#resolveReplayType`、`FundsAuthorizationTransactionFlowTests#testFundingAuthorizationFullSettleThenFullRefundShouldRestoreAvailableBalance`、`FundsAuthorizationTransactionFlowTests#testAuthorizationDisputeRefundShouldUseSettleRefundAndPreserveAuditContext`。 | 已完成授权后的 `settleRefund` 覆盖充分：请求必须携带 `authorizationTransactionSn`，converter 无条件构造 `AUTHORIZATION` reference 并把 `AUTHORIZATION_TRANSACTION_SN` 写入上下文；route resolver 基于原授权主体和平台 SETTLEMENT 生成退款路径，route replay 使用 `AUTHORIZATION_REFUND` 回放原完成路径；测试已断言余额、ledger transaction、entry、posting plan、projection、幂等和争议上下文。扫描时还没有 no-auth 语义、外部引用、退款原因一等字段或 no-auth refund 审计最小集，也不能在不携带内部授权流水时进入成功路径。 | 如果直接写无前置授权退款 Red，应先失败在 Request 契约和 converter：`authorizationTransactionSn` 必填且 `authorizationReference(...)` 会要求内部授权流水；route replay 也缺少外部引用驱动的退款路径。Red 应证明 no-auth refund 不补造授权占用、不按当前绑定重新选路、以空原授权流水进入 no-auth 语义，并携带 `externalReferenceSn`、原因和操作者/审计，成功后生成可追溯 AUTH_REFUND 或等价退款资金事实。 |
| `B4-NAR-RED-002` | `FundsAuthorizationTransactionFlowTests#testAuthorizationRefundExceedingSettledAmountShouldLeaveNoSideEffects`、`FundsAuthorizationTransactionFlowTests#testAuthorizationRefundSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects`、`DefaultFundsInstructionLifecycleSaver` 的请求摘要和状态累计逻辑。 | 已完成授权后的失败无副作用覆盖充分：超出已完成可退金额、同业务流水不同摘要会失败并保持余额、ledger transaction、posting、entry 和资金事实不变。但覆盖对象仍是“有内部授权和完成事实”的退款失败，不覆盖缺外部引用、缺原因、无操作者/审计或携带内部授权流水的无授权退款失败。 | Red 应沿用现有余额和事实快照断言结构，新增缺外部引用、缺原因、缺操作者/审计、携带内部授权流水和敏感上下文阻断用例；若 Execution Grant 未明确允许扩展 Request/DTO、错误码或 route replay 契约，测试不得落地。 |

### 7.2 forceSettleContractCandidate

本节记录 B4-FORCE-SETTLE 首轮编码候选契约的准入来源。该候选已由 `616dac1` 和 `3825466` 消费并闭合为当前代码基线；后续不得再把本节解释为默认编码授权。若要扩展生产策略引擎、审批快照、额度窗口、带原授权 overcapture、外部清算文件或运营审批系统，必须另起独立 Execution Grant。

| 候选字段 | 语义 | 首轮建议 | Red 断言 | 不纳入本轮 |
| --- | --- | --- | --- | --- |
| `settleMode` | 区分普通授权完成和无授权强制完成。 | 必须显式为 `FORCE` 或等价枚举/字符串；普通完成默认仍走现有 `authorizationTransactionSn`。首轮 FORCE 模式不得携带或依赖 `authorizationTransactionSn`，不得查询原授权账本交易；若后续要支持带原授权的 overcapture 或 late clearing，必须另起 Execution Grant。 | 缺 FORCE 模式不得走无授权完成；普通完成语义不变；FORCE 模式携带内部授权流水时失败或转人工差错，不回退到普通完成。 | 不新增复杂 processor 状态机；不支持带原授权的 overcapture 扩展。 |
| `forceSettlePolicyCode` | 说明为什么允许无授权消费入账。 | 必填，进入请求摘要和审计上下文；必须来自本次 Grant 声明的内部白名单、审批结果或受信策略快照，不得由外部调用方自由声明为充分凭证。 | 缺策略、策略不在白名单或策略来源不可审计时失败且无 route、posting、entry 或 projection。 | 不实现策略引擎、规则表或外部规则计算。 |
| `forceSettleLimitAmount` | 单笔强制完成可接受上限。 | 必填，币种沿 `transactionAmount`；金额不得超过上限；上限必须来自受信策略或审批快照，不能只依赖请求自填值。 | 超上限或上限来源不可审计时失败且无资金副作用。 | 不实现额度窗口、日/月累计控制。 |
| `forceSettleReason` | 业务原因，例如 forced post、late clearing 或差错完成。 | 必填，保存到上下文和审计摘要。 | 缺原因失败。 | 不定义卡组织最终原因码全集。 |
| `externalOriginalFactRef` | 外部已确认消费事实引用。 | 必填，可为脱敏外部流水、clearing/presentment 引用或等价结构。 | 缺引用失败；不得使用内部授权流水伪造。 | 不保存完整原始报文、PAN、CVV 或生产凭证原文。 |
| `forceSettleVoucherRef` | 凭证或审批引用。 | 首轮建议必填，可为摘要、文件编号、审批号或外部 reference。 | 缺凭证失败。 | 不落完整凭证文件，不做运营审批系统。 |
| `operator` / `contextVariables` | 操作者和审计上下文。 | 审计最小集使用现有 `WindOperator`、`forceSettleReason`、`externalOriginalFactRef`、`forceSettleVoucherRef` 和受信策略/审批快照引用；`ReadonlyContextVariables` 只承接白名单补充字段，敏感字段继续由 validator 阻断。 | 缺操作者、缺原因、缺外部引用、缺凭证、缺受信策略来源或敏感上下文按现有规则失败/阻断。 | 不新增权限系统，不引入生产配置；不把核心资金事实塞进普通上下文。 |

### 7.3 forceSettleCompletionEvidence（2026-06-02）

| 证据 | 结论 |
| --- | --- |
| 实现提交 | `616dac1 feat: 补齐授权强制完成能力` 新增 FORCE 完成请求字段、转换和路由分支；普通 settle 继续引用原授权流水，FORCE 不构造 `AUTHORIZATION` reference、不查询原授权账本交易，路由从 `AVAILABLE` 直接进入 `SETTLEMENT`。 |
| 红线加固提交 | `3825466 fix: 收紧授权强制完成策略红线` 把 `forceSettlePolicyCode` 和 `forceSettleLimitAmount` 收敛到内部受信策略校验；未知策略、策略上限不匹配、缺原因、缺外部事实、缺凭证、携带 `authorizationTransactionSn` 等失败路径均要求无资金副作用。 |
| 覆盖 Red | `B4-FS-RED-001` 和 `B4-FS-RED-002` 首轮已转为回归基线；成功路径覆盖 FORCE 入账和普通完成分支隔离，失败路径覆盖缺策略、超上限/上限不匹配、缺原因、缺外部事实、缺凭证、携带内部授权流水和幂等差异。 |
| 验证证据 | 已通过 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`。 |
| 剩余边界 | 未实现生产策略引擎、审批流、额度窗口、带原授权 overcapture、外部清算文件、拒付/争议增强、授权后继事件并发竞争，以及 NO_AUTH 退款的运营审批、人工差错和累计控制扩展；这些剩余能力仍需独立授权。 |

### 7.4 noAuthRefundContractCandidate

本节记录 B4-NO-AUTH-REFUND 首轮编码候选契约的准入来源。该候选已由 `006bcaa` 消费并进入回归基线；后续 CR 将资金层契约收缩为 `authorizationTransactionSn` 空值判定、`externalReferenceSn` 外部追溯引用和 `refundReason` 轻量原因。下表保留为字段语义、必填、摘要、审计和失败无副作用规则的 CR 依据，不授权新的 Java 代码、测试代码、DDL/H2 schema 或外部协议写入。

| 契约项 | 语义 | 首轮建议 | Red 断言 | 不纳入本轮 |
| --- | --- | --- | --- | --- |
| `NO_AUTH` 内部标签 | 资金指令内部归类标签，不是 `FundsAuthorizationTransactionRefundRequest` 请求字段。 | 请求侧按 `authorizationTransactionSn` 是否为空判定无授权退款；converter 在无授权退款指令上下文补充 `NO_AUTH` 标签；普通授权链退款仍要求 `authorizationTransactionSn`。 | 请求不携带退款模式字段但无内部授权流水时仍进入无授权退款；携带内部授权流水时不能进入 no-auth refund。 | 不重构普通退款服务入口；不新增统一退款事件体系；不恢复请求侧 `refundMode` 字段。 |
| `externalReferenceSn` | 外部引用流水号，只用于资金事实追溯。 | 必填，可为脱敏外部流水、presentment/clearing 引用、差错单号或等价引用；进入请求摘要和审计。 | 缺引用失败且无 route、posting、entry 或 projection；不得用内部授权流水伪造外部引用。 | 不保存完整原始报文、PAN、CVV、外部凭证原文或敏感支付数据；不解释外部事实类型。 |
| `refundReason` | 无授权退款的业务原因。 | 必填，进入请求摘要、交易上下文和审计解释。 | 缺原因或空白原因失败。 | 不定义最终争议/拒付 reason code 全集。 |
| `operator` / `contextVariables` | 操作者和白名单审计上下文。 | 审计最小集使用现有 `WindOperator`、`refundReason` 和 `externalReferenceSn`；`ReadonlyContextVariables` 只承接白名单补充字段，敏感字段继续由 validator 阻断。 | 缺操作者、缺原因、缺外部引用或敏感上下文时失败/阻断；不得把核心资金事实塞进普通上下文。 | 不新增权限系统，不引入生产配置；不把外部协议报文、运营凭证或外部事实分类作为资金层一等字段。 |

## 8. suggestedGrantSlices

| 切片 | 优先级 | 目标 | 首批 Red | 允许写入建议 | 不适合混入 |
| --- | --- | --- | --- | --- | --- |
| B4-FORCE-SETTLE | Done | 首轮账户主体型 canonical 能力已闭合，后续只作为授权交易回归基线。 | `B4-FS-RED-001`、`B4-FS-RED-002` 已回归化。 | 仅在返工或扩展 FORCE 策略引擎、审批快照、额度窗口、overcapture 时另起 Grant。 | 无授权退款、拒付、支付工具 facade、Spend Rule、VCC。 |
| B4-NO-AUTH-REFUND | Done | 已补齐 settleRefund 无授权退款模式；当前资金层契约收缩为 `authorizationTransactionSn` 空值判定、`externalReferenceSn` 外部追溯引用、`refundReason` 原因、操作者/审计和失败无副作用。 | `B4-NAR-RED-001`、`B4-NAR-RED-002`、`TDD-RED-017A` 已回归化。 | 后续仅在扩展运营审批、人工差错、累计退款控制、查询投影解释或外部规则时另起 Grant。 | force settle、chargeback case 全生命周期、清结算追偿。 |
| B4-DISPUTE-SEMANTIC-ALIGNMENT | Done | 首轮固化 `settleRefund / AUTH_REFUND` 争议退款与普通退款、NO_AUTH 退款、授权拒绝的可区分性。 | `B4-CB-RED-001A` / `TDD-RED-017B` 已回归化。 | 后续仅在扩展完整 dispute/chargeback case、独立 `chargeback` 一等目标 API、清结算追偿、外部规则或查询投影解释时另起 Grant。 | 独立 dispute system、VCC processor、清结算追偿单、未确认的 `chargeback` 目标态主入口。 |
| B4-AUTH-RACE | Done / regression baseline | 固化授权完成、撤销、过期、退款并发竞争红线。 | `B4-RACE-RED-001` 已回归化。 | `47c5269` 已覆盖授权 flow 并发测试、状态迁移保护、事务完成前 JVM 锁和授权原交易 `FOR UPDATE` 行锁；后续扩展跨节点锁、数据库约束或版本字段必须另起 Grant。 | DDL/H2 默认不允许，除非 Execution Grant 显式扩权。 |

B4-NO-AUTH-REFUND 已在用户确认 Execution Grant 后进入 Red -> Green -> Review -> Verify -> Commit 闭环，并由 `006bcaa` 进入代码基线。B4-DISPUTE-SEMANTIC-ALIGNMENT 已由 `949b24a` 进入代码基线。B4-AUTH-RACE 已由 `47c5269` 进入代码基线。后续完整 dispute/chargeback case、授权支付工具应用入口、授权占券和权益生命周期仍必须各自单独确认 Execution Grant，不能借前述已完成 Grant 自动扩权。

### 8.1 gsdCadAdmissionDecision（2026-06-02）

本节最初记录以 `e937395 docs: 对齐 B4 无授权退款主文档口径` 为已提交基线的 GSD-CAD 准入结论；当前执行基线以用户确认 Execution Grant 时的 Git HEAD 为准，后续 docs-only 索引、恢复入口或确认基线校准提交随 HEAD 自然纳入。GSD 负责确认阶段、切片、上下文和任务状态；CAD 只能在用户确认单一 Execution Grant 后执行 Red -> Green -> Review -> Verify -> Commit。

| 准入项 | 当前结论 | 进入 CAD 编码所需条件 |
| --- | --- | --- |
| 当前状态 | `DONE_BY_006BCAA`。B4-NO-AUTH-REFUND 已按用户确认的 Execution Grant 完成并提交。 | 后续只作为 B4 授权后继能力回归基线；新增能力、扩展字段、表结构、外部规则或运营流程必须另起 Grant。 |
| GSD 切片 | 单一切片为 B4-NO-AUTH-REFUND，只处理 `settleRefund` 无授权退款模式。 | 不与 force settle 返工、chargeback 独立入口、支付工具 facade、VCC、Spend Rule、清结算对账或治理任务混跑。 |
| CAD 首轮 Pick | `B4-NAR-RED-001` 已被消费并回归化，目标缺口已由 `006bcaa` 关闭。 | 若后续补充失败矩阵或查询投影解释，只能在新 Grant 中重新定义 Red 和写入范围。 |
| 必须列名契约 | 请求字段为 `authorizationTransactionSn` 空值语义、`externalReferenceSn`、`refundReason`、`operator/contextVariables`；`NO_AUTH` 为内部上下文标签。 | Grant 必须说明字段名、类型、必填规则、摘要字段、普通授权链退款兼容策略，并说明 NO_AUTH 内部标签不得由请求侧传入，且不得携带或查询内部授权流水。 |
| 首轮禁止事项 | 不改 DDL/H2 schema，不新增支付工具 facade，不新增 chargeback 独立入口，不实现 VCC 生命周期，不接入外部协议或敏感数据处理。 | 任一项成为必要条件时，停止并重新确认授权范围。 |
| 验证闭环 | 本轮编码闭环已执行 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`、`git diff --cached --check`；2026-06-03 追加回归 `just test-business-flow` 通过 106 tests。 | 后续新 Grant 仍需按各自写入范围重新执行目标验证和回归验证。 |

### 8.2 grantExecutionPackageCandidate（2026-06-02）

本节把 B4-NO-AUTH-REFUND 收敛成可确认的 GSD-CAD 原子任务包。该任务包已被用户确认并由 `006bcaa` 消费；下文作为历史授权包和执行边界记录保留。

| 产品准入项 | 口径 |
| --- | --- |
| 业务目标 / 用户价值 | 在没有内部授权流水但存在可追溯外部引用时，支持运营和财务发起可追溯的退款回补；用户价值是退款结果、原因和账务影响可解释。 |
| 非目标 | 不做完整退款运营后台、不做 chargeback case、不做清结算追偿、不做外部卡组织规则实现、不做 VCC 或支付工具 facade。 |
| 业务流程 / 主流程 | 主流程是运营或系统拿到可追溯外部引用 -> 发起无授权退款 -> 系统校验空原授权流水、外部引用、原因和操作者 -> 生成退款交易事实、route snapshot、账务事实和投影。 |
| 异常流程 / 人工兜底 | 缺外部引用、缺原因、缺操作者/审计、携带内部授权流水或敏感上下文时失败且无资金副作用；是否转人工差错候选由后续独立 Grant 确认。 |
| 运营后台 / 数据口径 | 首轮不新增运营后台页面或报表；但交易事实、请求摘要、外部引用、退款原因、操作者、ledger transaction、projection 和审计上下文必须足以支撑后续查询、指标、报表和差错复核。 |

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `B4-NAR-CAD-001`。 |
| `stage` / `wave` | B4 授权后继能力 / Wave 1 账户主体型 canonical 内核补强。 |
| `status` | `DONE_BY_006BCAA`。Execution Grant 已被用户确认并由 `006bcaa feat: 补齐无授权退款 canonical 能力` 消费。 |
| `authorityBaseline` | 用户确认时 Git HEAD 为 `b69dbe5 docs: 标记 B4 无授权退款 Grant 前准备饱和`；闭环代码提交为 `006bcaa feat: 补齐无授权退款 canonical 能力`。 |
| `mvpScenario` | 无前置内部授权流水，但已存在可追溯外部引用，需要在账户主体型交易内核中形成可追溯退款资金事实。 |
| `businessAdmission` | 产品验收锚点为 `AC-AUTH-012` 和 `TDD-RED-017A`；DSL 锚点为 `DSL-AUTH-REFUND-001`；系分锚点为授权交易 `settleRefund`、route replay、账务计划和投影解释。 |
| `firstRedSet` | `B4-NAR-RED-001` 已消费并回归化，成功路径证明 no-auth refund 生成独立可追溯退款资金事实。 |
| `secondRedSet` | `B4-NAR-RED-002` 最小失败矩阵已随目标 flow 覆盖缺外部引用、缺原因、携带内部授权流水等失败无副作用；请求不携带退款模式字段但无内部授权流水时按 no-auth refund 处理。 |
| `gitStrategy` | 仅在用户确认 `auto_commit` 且目标验证通过时提交；验证失败、环境不可判定或越界时转为 `summary_only`。 |

| 边界项 | 候选裁决 |
| --- | --- |
| `writeScope` | 历史授权范围为先写 `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java` 中 B4-NAR 目标 Red；Red 证明缺口后，允许最小修改 `transaction/transaction-face` 的 `FundsAuthorizationTransactionRefundRequest` 兼容字段，以及 `transaction/transaction-impl` 的 converter、command service、lifecycle saver、route replay 和请求摘要。实际闭环未新增独立测试类，未修改 DDL/H2、ledger 公共契约、core 枚举状态或 wallet facade。 |
| `readOnlyScope` | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/*`、现有 `transaction-*`、`ledger-*`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `publicContractGate` | 只有 Grant 显式列名 `authorizationTransactionSn` 空值语义、`externalReferenceSn`、`refundReason` 和 `operator/contextVariables` 时，才允许扩展 `FundsAuthorizationTransactionRefundRequest`；`NO_AUTH` 仅可作为内部上下文标签，不作为请求字段。不得破坏普通授权链退款的 `authorizationTransactionSn` 兼容语义。 |
| `ledgerGate` | 默认不修改 `ledger-face`、`ledger-impl` 公共能力。若 route replay 或 posting 装配证明必须改 ledger 侧公共契约、账务计划语义或 projection 表达，立即停止并扩权确认。 |
| `schemaGate` | `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段和数据库唯一约束均只读；任何表结构需求都触发停止。 |
| `noWriteScope` | 不写支付工具 facade、钱包 application facade、VCC 生命周期、Spend Rule 表、force settle 返工、chargeback 独立入口、dispute case、清结算追偿、治理 apply、生产配置、外部协议、敏感数据处理和 P2 业务能力包。 |

| `B4-NAR-RED-001` 断言包 | 必须证明的事实 |
| --- | --- |
| 请求事实 | 请求不携带 `authorizationTransactionSn` 时进入无授权退款，携带 `externalReferenceSn`、退款原因、操作者和白名单上下文。 |
| 交易事实 | 成功后生成退款资金交易事实，请求摘要能区分普通授权链退款和无授权退款；外部引用可追溯，内部授权流水不被伪造。 |
| 路由事实 | 不按当前绑定重新选路，不构造 `AUTHORIZATION` reference，不查询原授权账本交易；route snapshot 或 replay 结果能解释退款资金路径。 |
| 账务事实 | posting plan 平衡，ledger transaction、ledger entry 和 projection 可追溯；金额、币种和余额桶变化符合退款语义。 |
| 幂等事实 | 同 `businessSn` 同摘要重试幂等；同 `businessSn` 不同摘要拒绝且不污染原事实。 |
| 失败副作用 | 若首轮 Red 同时带最小负向样例，失败时不得产生 route、posting、ledger transaction、ledger entry、projection 或余额变化。 |

| `B4-NAR-RED-002` 失败矩阵 | 停止或失败口径 |
| --- | --- |
| 请求不携带模式字段 | 不影响 no-auth 判定；请求未携带内部授权流水时仍按无授权退款处理，并在内部摘要补充 `NO_AUTH` 归类标签。 |
| 缺外部引用 | 失败且无资金副作用；不得把内部授权流水当外部引用。 |
| 缺退款原因或操作者 | 失败且无资金副作用；不得从普通 `contextVariables` 暗含核心资金事实。 |
| 携带 `authorizationTransactionSn` | `NO_AUTH` 模式必须失败或转人工差错候选；不得查询原授权账本交易。 |
| 退款金额超出可承接资金事实 | 首轮不使用调用方自报金额币种作为资金层上限；金额红线由账户余额、账务平衡和后续累计控制 Grant 继续兜底。 |
| 敏感上下文 | 继续沿用敏感上下文 validator，敏感字段不得进入请求摘要、审计上下文或日志。 |

| 验证与提交 | 候选要求 |
| --- | --- |
| Red 验证 | `just test-one FundsAuthorizationTransactionFlowTests tests` 必须先失败在 B4-NAR 目标缺口；若失败点偏离无授权退款契约，先修 Red，不改生产。 |
| Green 验证 | `just test-one FundsAuthorizationTransactionFlowTests tests` 通过，且普通授权链 authorize、settle、expire、force settle 和 settleRefund 回归不退化。 |
| 回归验证 | `just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`。 |
| 提交条件 | 工作树只包含本 Grant 范围内变更，目标验证和回归验证通过，且未触发停止条件时才允许 `git add` 和 `git commit`。 |
| 停止条件 | 需要 DDL/H2、core 枚举或状态、新依赖、外部规则、支付工具 facade、VCC、chargeback case、清结算追偿、ledger 公共契约扩展、公有方法超过 5 个参数、敏感数据处理或工作树冲突时立即停止。 |

### 8.3 grantReadinessRecord（2026-06-03）

本节记录计划内 Execution Grant 准备任务的完成态。该完成态最初只表示 B4-NO-AUTH-REFUND 的授权包已经达到可确认状态；当前已进一步被 `006bcaa` 消费为闭环记录。

| 检查项 | 结论 |
| --- | --- |
| 任务包 | `B4-NAR-CAD-001` 已选定为单一 GSD-CAD 原子任务包，只处理 `settleRefund` 无授权退款模式。 |
| 授权正文 | 第 11 节 `Execution Grant：B4-NO-AUTH-REFUND` 可直接作为用户确认文本；确认基线以确认时 Git HEAD 为准。 |
| 首轮 Red | `B4-NAR-RED-001`；首轮 Green 后再补 `B4-NAR-RED-002` 失败矩阵。 |
| 写入范围 | 先写授权退款 flow 测试；Red 证明缺口后仅允许 `FundsAuthorizationTransactionRefundRequest` 兼容字段、transaction converter/command/lifecycle/route replay/request summary 最小修复。 |
| 禁止范围 | 支付工具 facade、钱包 application facade、VCC 生命周期、DDL/H2 schema、ledger 公共契约、core 枚举状态、Spend Rule、force settle 返工、chargeback case、清结算追偿、治理 apply、生产配置、外部协议和敏感数据处理。 |
| 验证闭环 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`。 |
| 准入结论 | `CLOSED_BY_006BCAA`；本节从准备态转为历史 handoff 记录，后续只作为 B4-NO-AUTH-REFUND 回归和 CR 依据。 |

### 8.4 noAuthRefundImplementationScan（2026-06-03）

本节记录进入首轮 Red 前的只读代码触点扫描。扫描只读取 Java 和测试资产，不修改生产代码、测试代码、DDL/H2 schema 或运行时配置；当前缺口已由 `006bcaa` 关闭，下表作为 Grant 前事实保留。

| 扫描项 | Grant 前事实 | 对 `B4-NAR-RED-001` 的含义 |
| --- | --- | --- |
| 请求契约 | Grant 前 `FundsAuthorizationTransactionRefundRequest.authorizationTransactionSn` 仍是 `@NotNull`，当时没有 no-auth 空值语义、`externalReferenceSn`、`refundReason` 等请求契约。 | 无授权退款无法通过现有请求表达；首轮 Red 应证明必须新增或兼容扩展显式列名字段。 |
| 指令转换 | `FundsAuthorizationInstructionConverter#convertToSettleRefundInstruction` 无条件调用 `authorizationReference(request.getAuthorizationTransactionSn())`，并把 `AUTHORIZATION_TRANSACTION_SN` 写入上下文。 | NO_AUTH 路径当前会被内部授权流水绑定，不能满足“不携带或查询内部授权流水”。 |
| 原授权账本查询 | `authorizationReference(...)` 会查询原授权 `AUTHORIZE` ledger transaction，并要求唯一。 | 没有内部授权流水时当前路径会失败在原授权账本引用，而不是形成外部原事实退款。 |
| 命令入口 | `FundsTransactionCommandServiceImpl#settleRefund` 直接委派 converter。 | 首轮 Green 若获授权，修复点应优先落在 request、converter、lifecycle、route replay 和请求摘要的最小范围内，不在 command service 扩大业务分支。 |
| 测试资产 | `FundsTransactionFlowTestSupport#refundSettledAuthorization` 只支持普通授权链退款并总是传入 `authorizationTransactionSn`。 | 首轮 Red 需要新增 no-auth refund 专用构造或测试 helper，并证明普通授权链 helper 不退化。 |
| 准入结论 | `CLOSED_BY_006BCAA`。 | 该扫描已完成历史使命；后续新增 no-auth refund 扩展时需重新扫描当前代码。 |

### 8.5 noAuthRefundMinimalGreenMap（2026-06-03）

本节记录 `B4-NAR-RED-001` 获得 Execution Grant 后的最小 Green 判断地图。该地图来自 Grant 前只读扫描；当前实现闭环已由 `006bcaa` 完成，后续请求契约收口由 `818da34` 完成，路由外部引用回退由 `967586c` 完成。下表作为历史最小 Green 依据和当前回归口径保留。

| 触点 | Grant 前事实 | 最小 Green 约束 |
| --- | --- | --- |
| Route resolver 命中 | Grant 前 `DefaultRouteReplayService` 只在 replay event 且 reference type 为 `ORIGINAL_TRANSACTION`、`AUTHORIZATION`、`REFUND`、`FEE` 或 `FREEZE_ORDER` 时命中；`EXTERNAL_TRANSACTION` 明确不是 route snapshot reference，且 `AuthorizationFundsInstructionRouteResolver#supports` 当时只支持 `AUTHORIZE` 和 FORCE `SETTLE`。`967586c` 后，no-auth `AUTH_REFUND` 可在内部 `REFUND_MODE` 缺失时由 `EXTERNAL_TRANSACTION` reference 进入专用 resolver。 | NO_AUTH 退款不能伪装成内部授权 replay；基于外部引用生成 route 时必须显式切出可解释路径，不能让当前绑定关系静默重选路；普通授权链 `AUTH_REFUND` 仍保持原 route replay 口径，不进入 no-auth resolver。 |
| Route replay 红线 | `DefaultRouteReplayService` 缺 reference 或缺 route snapshot 时明确失败，并有边界测试证明不得依赖当前支付工具、资金来源关系或 resolver 重算路径。 | Green 不能为通过 Red 而放宽 replay reference 要求；若 NO_AUTH 使用新路径，应保持原 replay 红线不退化。 |
| 授权退款 route 形态 | 现有 `resolveSettleRefund` 可生成 SETTLEMENT -> AVAILABLE 的退款 leg，但该方法不是当前普通 `AUTH_REFUND` 的主要命中入口，且依赖账户上下文和平台 SETTLEMENT 账户。 | 若复用该 route 形态，必须由 NO_AUTH 明确校验外部引用、原因和账户主体，不得借内部 `AUTHORIZATION_TRANSACTION_SN` 进入。 |
| 生命周期聚合 | `DefaultFundsInstructionLifecycleSaver#findReferenceTransaction` 会在 reference type 为 `AUTHORIZATION`、`ORIGINAL_TRANSACTION`、`REFUND` 或 `FEE` 时复用引用交易；`EXTERNAL_TRANSACTION` 不复用。 | NO_AUTH 若使用外部引用，应避免复用内部授权交易聚合；交易聚合、明细和请求摘要必须能区分普通授权链退款与 no-auth refund。 |
| 可回退金额校验 | `AUTH_REFUND` 和 `REFUND` 成功前会检查可回退金额；只有交易类型为 `DefaultFundsTransactionType.REFUND` 的聚合跳过 settled reversible amount 校验。 | NO_AUTH 首轮不使用调用方自报金额作为资金层上限；可退资金由平台结算账户余额和账务平衡约束，累计退款控制、外部事实登记或清算批次限额另起 Grant。 |
| 请求摘要 | 明细 `requestHash` 当前纳入 instruction reference、contextVariables、route summary 和 participant summary。 | `NO_AUTH` 内部标签、`externalReferenceSn` 和原因必须进入摘要或等价不可变事实，避免同一 `businessSn` 不同外部引用被幂等误合并。 |

### 8.6 firstRedAssertionPack（2026-06-03）

本节记录 `B4-NAR-RED-001` 获得 Execution Grant 后的首轮 Red 断言包。该断言包已被 `006bcaa` 消费并回归化，后续作为 B4-NO-AUTH-REFUND 测试资产 CR 依据。

| 断言层 | 首轮 Red 必须表达 | 可复用测试资产 |
| --- | --- | --- |
| 请求事实 | no-auth refund 请求不携带 `authorizationTransactionSn` 和 `refundMode`，携带 `externalReferenceSn`、原因和操作者；`NO_AUTH` 只作为内部归类标签，不决定分支。 | `FundsAuthorizationTransactionFlowTests` 新增目标用例；`FundsTransactionFlowTestSupport` 新增 no-auth refund 专用请求构造。 |
| 余额事实 | 成功后用户 `AVAILABLE` 增加退款金额，平台 `SETTLEMENT` 减少退款金额；`AUTHORIZATION` 不增加、不释放、不伪造占用。 | `snapshot(...)`、`assertOnlyBalanceDeltas(...)`、`assertBucket(...)`。 |
| 交易事实 | 生成一笔可追溯退款资金事实，能区分普通授权链退款和 no-auth refund；不得把外部引用映射成内部授权交易聚合。 | `fundsTransactionsByBusinessSn(...)`、`fundsTransactionDetailsByBusinessSn(...)`、`assertSingleFundsAndLedgerFactsForBusinessSn(...)`。 |
| 账务事实 | ledger transaction、posting plan、ledger entry 与 route snapshot 对齐，phase 为退款语义，entry 覆盖 `SETTLEMENT` 和 `AVAILABLE`。 | `ledgerTransactionByBusinessSn(...)`、`postingPlansOf(...)`、`entriesOf(...)`、`assertLedgerFactsFollowRouteSnapshot(...)`。 |
| 审计与摘要 | `externalReferenceSn`、原因和 `NO_AUTH` 内部归类标签进入交易上下文、ledger context 或等价不可变摘要；同 `businessSn` 不同外部引用必须失败且无新增账务事实。 | 现有幂等冲突样例、`assertLedgerTransactionFactsUnchanged(...)`、`assertNoFundsOrLedgerFactsForBusinessSn(...)`。 |
| 普通退款回归 | 现有授权链 full refund、dispute refund、chargeback 和幂等退款回归不退化。 | `testFundingAuthorizationFullSettleThenFullRefundShouldRestoreAvailableBalance`、`testAuthorizationDisputeRefundShouldUseSettleRefundAndPreserveAuditContext`、现有 refund idempotent 场景。 |
| 预期 Red 失败点 | Grant 前基线应失败在 request 契约、converter 内部授权 reference、原授权 ledger transaction 查询或缺少 no-auth 专用 route 路径之一；当前基线已由 `006bcaa`、`818da34` 和 `967586c` 转为回归。 | 若后续新 Grant 的 Red 不失败，立即暂停判断已有实现覆盖或 Red 写错，不进入 Green。 |

### 8.7 grantPreflightSaturation（2026-06-03）

本节记录 `B4-NO-AUTH-REFUND` 在未获 Execution Grant 前的准备饱和度结论。该结论用于恢复入口，不授权写 Red 或代码。

| 准备项 | 状态 | 结论 |
| --- | --- | --- |
| 任务包 | `DONE`。 | `B4-NAR-CAD-001` 已收敛为单一原子任务包。 |
| 触点扫描 | `DONE`。 | Request、converter、原授权账本查询、command service 和测试 helper 的首轮阻断点已记录。 |
| 最小 Green 地图 | `DONE`。 | route resolver、route replay、lifecycle 聚合、金额控制和请求摘要的最小修复边界已记录。 |
| 首轮 Red 断言包 | `DONE`。 | 请求事实、余额事实、交易事实、账务事实、审计摘要、普通退款回归和预期 Red 失败点已记录。 |
| 文档门禁 | `DONE`。 | 架构师和产品专家交付检查、`git diff --check` 已在 docs-only 提交中通过。 |
| 当前阻断 | `CLOSED_BY_006BCAA`。 | B4-NO-AUTH-REFUND 已完成编码闭环；后续只能在新 Grant 中推进拒付、并发、支付工具 facade、VCC、Spend Rule、清结算对账或治理任务。 |

### 8.8 implementationClosureRecord（2026-06-03）

本节记录 `B4-NO-AUTH-REFUND` 的 GSD-CAD 执行闭环，作为后续恢复、CR 和回归判断依据。

| 项 | 结论 |
| --- | --- |
| 执行基线 | 用户确认时 Git HEAD 为 `b69dbe5 docs: 标记 B4 无授权退款 Grant 前准备饱和`。 |
| 闭环提交 | `006bcaa feat: 补齐无授权退款 canonical 能力`。 |
| 契约与路由收口提交 | `818da34 fix(transaction): 移除授权退款请求模式字段` 确认 `FundsAuthorizationTransactionRefundRequest` 不再暴露 `refundMode`，`NO_AUTH` 只作为资金指令内部上下文标签；`967586c fix: 按外部引用推断无授权退款路由` 确认内部 `REFUND_MODE` 缺失时可由 `EXTERNAL_TRANSACTION` reference 推断 no-auth refund 路由，显式 `DISPUTE` 或其他退款归类不被覆盖。 |
| 写入范围 | `transaction-face` 的退款请求兼容字段和指令上下文 key，`transaction-impl` 的 converter、authorization route resolver、lifecycle saver 和 route code，`tests` 的授权交易 flow 和 no-auth refund helper。 |
| 未触碰范围 | 未修改 DDL/H2 schema、ledger 公共契约、core 枚举状态、wallet facade、支付工具 facade、VCC、Spend Rule、force settle、chargeback 独立入口、生产配置、外部协议或敏感数据处理。 |
| 核心语义 | 普通授权链退款继续要求 `authorizationTransactionSn`；无授权退款以 `authorizationTransactionSn` 为空作为分支判定，使用 `externalReferenceSn`、退款原因和操作者/审计形成独立退款事实，请求侧不暴露 `refundMode`，`NO_AUTH` 只作为内部归类标签，不构造 `AUTHORIZATION` reference，不查询内部原授权账本交易；当内部 `REFUND_MODE` 缺失但存在 `EXTERNAL_TRANSACTION` reference 时，route resolver 可推断 no-auth refund 路由。 |
| 目标验证 | `006bcaa` 闭环时 `just test-one FundsAuthorizationTransactionFlowTests tests` 通过 24 tests；`818da34` 契约收口后同命令通过 25 tests，并通过 `just compile`、`just pmd`、`git diff --check`、`git diff --cached --check`；`just test-transaction` 通过 92 tests、`just test-boundary` 通过 126 tests。`967586c` 路由回退加固后，`just verify-slice AuthorizationFundsInstructionRouteResolverTests tests`、`just test-boundary`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just pmd` 和 `git diff --check` 通过。 |
| 追加回归 | 2026-06-03 追加执行 `just test-business-flow` 通过 106 tests，覆盖业务流程回归。 |
| 后续 Not Done | no-auth refund 的运营审批、人工差错单、累计退款跨请求聚合控制、查询投影解释和外部规则核验不是 `006bcaa` 的 Done 范围；需要时另起 Execution Grant。 |

### 8.9 disputeChargebackAdmissionDecision（2026-06-03）

本节记录 B4-NO-AUTH-REFUND 闭合后的下一候选只读复核。该复核后续已推进为 8.10 的 `B4-DISPUTE-SEMANTIC-ALIGNMENT` Grant 候选，并由 `949b24a` 消费为首轮代码闭环；本节仅保留历史准入裁决，不授权 Java 代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 准入项 | 当前结论 |
| --- | --- |
| 历史候选 | `B4-DISPUTE-CHARGEBACK-R0`，只处理已完成授权后的争议、拒付或扣回语义裁决。 |
| 准入状态 | `SUPERSEDED_BY_20260626_REMOVAL_DECISION`；当时状态为 `SEMANTIC_DECISION_REQUIRED_NOT_CODE_AUTHORIZED`，随后被 `949b24a` 的首轮争议退款可区分性闭合，再由 2026-06-26 移除裁决覆盖。 |
| 代码证据 | 历史扫描时存在 `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest`、`CHARGEBACK` eventType、route replay `CHARGEBACK` phase 和 `FundsAuthorizationTransactionFlowTests` 中 chargeback 成功、超额失败、同业务流水不同摘要失败无副作用等测试；当前目标态不再保留这些独立入口和分支。 |
| 目标语义证据 | PRD、DSL、系分和本卡 `B4-CB-RED-001` 均要求拒付与普通退款、授权拒绝可区分；但不要求把独立 `chargeback` 服务入口作为目标态主入口，默认可由 `settleRefund` 携带拒付原因、凭证、外部引用和审计上下文承接。 |
| 历史差异 | 现有 `chargeback` 能力强于目标态最小要求，但不能反推出目标态必须以 `chargeback` 为 canonical API。若直接开编码，容易把实现入口当产品语义，和“拒付不强制落独立 `chargeback` 入口”的设计红线冲突。 |
| 本轮测试证据 | 2026-06-03 只读裁决时执行 `just test-one FundsAuthorizationTransactionFlowTests tests` 通过 24 tests；`818da34` 契约收口后同命令通过 25 tests，说明授权交易主流程、无授权退款和既有 chargeback 回归当前未破坏。 |
| 历史推荐裁决 | 下一轮先确认 `settleRefund` 为拒付/争议承接的目标态主入口；后续已按该裁决推进并由 `949b24a` 闭合首轮可区分性。2026-06-26 后，既有 `chargeback` 不再保留为兼容入口；不得把 `chargeback` 升级为一等目标态 API。 |
| 首批 Red 候选 | `settleRefund` 争议退款在查询、投影、审计和幂等摘要上可与普通授权链退款、无授权退款区分的首批 Red 已由 `949b24a` 回归化；后续不应借本节继续新增独立 dispute case、清结算追偿或外部卡组织规则。 |
| 停止条件 | 需要新增 `chargeback` 公共目标态、dispute case、清结算追偿、DDL/H2、core 枚举/状态、ledger 公共契约、支付工具 facade、VCC、外部协议或敏感数据处理时停止并重新确认 Execution Grant。 |

### 8.10 disputeSemanticAlignmentGrantCandidate（2026-06-03）

本节把 B4-DISPUTE-CHARGEBACK 的只读裁决推进到可确认的 Execution Grant 候选。该候选后续已由用户确认并被 `949b24a fix(transaction): 对齐授权争议退款审计语义` 消费；本节只作为授权 provenance 和回归依据保留，不再授权新的写入。

| 授权包字段 | 候选内容 |
| --- | --- |
| `taskId` | `B4-DISPUTE-SEMANTIC-ALIGNMENT`。 |
| `stage` / `wave` | B4 授权后继能力 / Wave 1 账户主体型 canonical 内核补强。 |
| `status` | `CONSUMED_BY_949B24A`。 |
| `mvpScenario` | 已完成授权后发生争议、拒付或扣回，资金底座需要在不引入完整 dispute case 的前提下，保留拒付原因、凭证、外部引用、审计上下文、原路径和金额上限，并能与普通授权链退款、无授权退款和授权拒绝区分。 |
| `businessAdmission` | 产品验收锚点为拒付与授权拒绝区分、`RED-005` 和拒付不压缩成普通退款；DSL 锚点为 `DSL-AUTH-REFUND-001` 和授权拒付承接；系分锚点为授权交易 `settleRefund`、route replay、账务计划和投影解释。 |
| `canonicalDecision` | 首轮默认确认 `settleRefund` 为拒付/争议承接目标态主入口；2026-06-26 后，既有 `FundsAuthorizationTransactionService#chargeback` 不再保留为兼容、显式事件或内部适配入口，不得新建 `chargeback` 一等 API Grant。 |
| `firstRedSet` | `B4-CB-RED-001A` / `TDD-RED-017B`：争议退款通过 `settleRefund` 承接时，查询、投影、审计上下文和幂等摘要必须能区分普通授权链退款、NO_AUTH 退款、拒付承接和授权拒绝。 |
| `secondRedSet` | `B4-CB-RED-001B`：授权拒绝不得生成拒付事实；缺拒付原因、缺凭证、缺外部引用或超已完成可回退金额时失败且无资金副作用。 |
| `writeScope` | 先写 `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java` 中 B4-CB 目标 Red；Red 证明缺口后，仅允许在 `transaction-face` 的授权退款请求兼容字段、`transaction-impl` converter/lifecycle/route replay/request summary 和交易投影解释最小范围修复。 |
| `readOnlyScope` | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md`、既有 `transaction-*`、`ledger-*`、`tests/src/test/resources/jdbc-schema.sql`。 |
| `harnessScopeIndex` | 标准 Harness 字段索引：写入范围为授权交易 flow 目标 Red、transaction-face 授权退款请求兼容字段、transaction-impl converter/lifecycle/route replay/request summary 和交易投影解释；写入文件先限定 `FundsAuthorizationTransactionFlowTests.java`，Red 证明缺口后才进入上列最小生产触点；只读范围和只读参考为 PRD、DSL、系分、TDD、OpenSpec、既有 `transaction-*`、`ledger-*` 和 H2 schema。 |
| `publicContractGate` | 若需要新增或调整 `settleRefund` 上的一等字段，Grant 必须显式列名，例如 `disputeMode`、`disputeReason`、`disputeVoucherRef`、`externalDisputeRef`、`disputeAuditContext` 或等价命名；普通授权链退款和 NO_AUTH 退款兼容语义不得破坏。 |
| `ledgerGate` | 默认不修改 `ledger-face`、`ledger-impl` 公共契约；若投影解释必须扩展 ledger 公共模型，立即停止并扩权确认。 |
| `schemaGate` | 默认不修改 `tests/src/test/resources/jdbc-schema.sql`、生产 DDL、Entity 字段、Mapper 表字段、索引或唯一约束。 |
| `noWriteScope` | 不写完整 dispute case、chargeback case 生命周期、清结算追偿、VCC processor、支付工具 facade、钱包 application facade、Spend Rule、DDL/H2 schema、core 枚举状态、ledger 公共契约、治理 apply、生产配置、外部协议或敏感数据处理。 |
| `verificationCommand` | 首轮 `just test-one FundsAuthorizationTransactionFlowTests tests`；Green 后按触点补 `just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check`。 |
| `gitStrategy` | 已按用户确认的 `auto_commit` 执行并提交；验证失败、环境不可判定或越界时转 `summary_only` 的规则保留为历史策略说明。 |

```text
Execution Grant：B4-DISPUTE-SEMANTIC-ALIGNMENT
确认基线：确认时 Git HEAD；必须包含 B4-TRX-EXPIRE、B4-FORCE-SETTLE、B4-NO-AUTH-REFUND 和 B4-DISPUTE-CHARGEBACK 只读准入裁决相关提交
目标：补齐已完成授权后的争议/拒付承接语义，使其能与普通授权链退款、NO_AUTH 退款和授权拒绝区分；首轮默认以 settleRefund 为目标态主入口，既有 chargeback 入口只作为兼容、显式事件或内部适配资产
允许写入：先写 tests 中 B4-CB 目标 Red；Red 证明缺口后允许 transaction-face 的授权退款请求兼容字段、transaction-impl converter/lifecycle/route replay/request summary、交易投影解释和 TDD tests 最小修复
允许契约字段：disputeMode、disputeReason、disputeVoucherRef、externalDisputeRef、disputeAuditContext 或后续 Grant 明确确认的等价命名；字段名、类型、必填规则、摘要字段和兼容策略以本次 Grant 为准
首批 Red：B4-CB-RED-001A；必要时补 B4-CB-RED-001B
验证命令：just test-one FundsAuthorizationTransactionFlowTests tests；just test-transaction；just test-business-flow；just test-boundary；just compile；提交前 just pmd 和 git diff --check
禁止写入：完整 dispute case、chargeback case 生命周期、清结算追偿、VCC processor、支付工具 facade、钱包 application facade、Spend Rule、DDL/H2 schema、core 枚举状态、ledger 公共契约、治理 apply、生产配置、外部协议、敏感数据处理
Git 策略：auto_commit
停止条件：公共契约扩展未列名、表结构、ledger 公共契约、外部规则、清结算对账、P2、敏感数据、独立 chargeback 一等 API 或工作树冲突越界即停止
```

### 8.11 disputeGrantArchitectureSelfCheck（2026-06-03）

本节用于把 B4-DISPUTE-SEMANTIC-ALIGNMENT 候选包按架构交付物结构显式自检。它不新增编码授权，只说明 Grant 候选已经具备可评审的目标、边界、设计、质量、测试和计划信息。

| 结构项 | 当前口径 |
| --- | --- |
| 需求背景 / 问题 / 不做的风险 | 已完成授权后的争议、拒付或扣回如果只落成普通退款，运营、财务、审计和研发无法解释责任方、外部引用、凭证、金额上限和授权拒绝差异；若直接把既有 `chargeback` 入口当目标态主入口，则会把实现入口误当产品语义。 |
| 目标 / 非目标 / 系统边界 / 数据边界 / 安全边界 | 目标是让 `settleRefund` 争议承接具备可区分语义；非目标是不做完整 dispute case、清结算追偿、VCC processor 或外部卡组织规则；系统边界限定在账户主体型授权交易内核；数据边界不新增 DDL/H2 或 ledger 公共契约；安全边界保持敏感数据和外部协议只读或禁止。 |
| 概要设计 / 核心方案 / 关键依赖 / 同步 / 异步 | 概要设计是以 `settleRefund` 承接争议语义、保留既有 `chargeback` 兼容资产；核心方案是先用 Red 证明查询、投影、审计和幂等摘要可区分；关键依赖为授权退款请求、converter、lifecycle、route replay、交易投影解释和现有授权交易 flow；本轮不引入新的同步外部调用或异步外部事件。 |
| 详细设计 / 模块 / 类设计 / 接口设计 / 数据设计 | 详细设计限定在 `transaction-face`、`transaction-impl` 和目标测试资产；模块不扩展到 wallet、ledger 公共契约、reconciliation 或 governance；类设计和接口设计只在 Red 证明后最小调整授权退款请求兼容字段和转换保存逻辑；数据设计不新增表、索引或实体字段。 |
| 状态机 / 主流程 / 异常流程 / 补偿流程 / 人工介入 | 状态机不新增独立 chargeback case 状态；主流程为已完成授权后通过争议退款承接逆向资金事实；异常流程为缺原因、缺凭证、缺外部引用、超已完成可回退金额或授权拒绝混入拒付时失败无副作用；补偿流程和人工介入只记录为后续 Grant，不在本轮实现。 |
| 非功能 / 性能 / 容量 / 可用性 / 兼容性 / 生产就绪 | 非功能以可追溯、可审计、兼容普通授权链退款和 NO_AUTH 退款为主；性能和容量不作为本轮目标；可用性要求失败无资金副作用；兼容性要求既有 `chargeback` 测试不退化；生产就绪不成立，完整 dispute case、清结算追偿和外部规则仍是 Not Done。 |
| 测试设计 / 单元测试 / 集成测试 / 契约测试 / 回归测试 | 测试设计先写 `FundsAuthorizationTransactionFlowTests` 目标 Red；单元测试和契约测试只有在公共契约或摘要规则变更时按需补充；集成测试以 Spring 服务层真实链路为主；回归测试覆盖普通授权链退款、NO_AUTH 退款、既有 chargeback、授权拒绝和失败无副作用。 |
| 研发计划 / 负责人 / 里程碑 / 验收方式 | 研发计划已由用户确认 Execution Grant 后完成 Red -> Green -> Review -> Verify -> Commit；负责人为本仓库 GSD-CAD 执行线程和用户确认点；里程碑为 Red 失败、最小 Green、专项验证、回归验证和提交；验收方式为 Grant 覆盖范围内测试和规约检查通过。 |

### 8.12 disputeSemanticAlignmentClosure（2026-06-03）

本节记录 B4-DISPUTE-SEMANTIC-ALIGNMENT 获得 Execution Grant 后的首轮 CAD 闭环。该闭环只关闭 `settleRefund / AUTH_REFUND` 对争议/拒付语义的首个 canonical 可区分性切片，不声明完整 dispute case、chargeback case 生命周期、清结算追偿、VCC processor 或外部卡组织规则完成。

| 闭环项 | 结果 |
| --- | --- |
| 执行提交 | `949b24a fix(transaction): 对齐授权争议退款审计语义`。 |
| 已完成语义 | `FundsAuthorizationTransactionRefundRequest` 新增 `disputeMode`、`disputeReason`、`disputeVoucherRef` 和 `externalDisputeRef`；请求侧不恢复 `refundMode`。converter 在争议字段出现时要求原因、凭证、外部引用和争议模式完整，并向资金指令内部上下文写入 `refundMode=DISPUTE`、争议字段和用户审计上下文。route replay 只在 `AUTH_REFUND + DISPUTE` 场景传播争议审计上下文，避免普通退款、NO_AUTH 退款或 fee refund route snapshot 被请求上下文污染。 |
| 实际写入范围 | `transaction/transaction-face/src/main/java/com/wind/funds/transaction/model/request/FundsAuthorizationTransactionRefundRequest.java`、`transaction/transaction-face/src/main/java/com/wind/funds/transaction/constant/FundsInstructionContextKeys.java`、`transaction/transaction-impl/src/main/java/com/wind/funds/transaction/converter/FundsAuthorizationInstructionConverter.java`、`transaction/transaction-impl/src/main/java/com/wind/funds/route/DefaultRouteReplayService.java`、`tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java`。 |
| 测试清单项 | 覆盖 `B4-CB-RED-001A` / `TDD-RED-017B`：争议退款通过 `settleRefund` 承接后，ledger transaction、posting plan、ledger entry、details、route replay context、请求摘要和幂等冲突都能与普通退款、NO_AUTH 退款和授权拒绝区分；补充缺争议原因、凭证、外部引用或模式时失败且无余额和账务副作用。 |
| 验证证据 | `just compile` 通过；`just test-one FundsAuthorizationTransactionFlowTests tests` 通过 26 tests；`just test-transaction` 通过 94 tests；`just test-business-flow` 通过 108 tests；`just test-boundary` 通过 126 tests；`just pmd` 通过；`git diff --check` 通过。Spring 测试在沙箱内触发 embedded Redis 端口绑定 `Operation not permitted`，已按工具权限规则非沙箱重跑并通过，定性为环境权限问题，不是代码失败。 |
| Not Done | 完整 dispute case、独立 chargeback 一等目标 API、清结算追偿、VCC processor、外部卡组织规则、DDL/H2 schema、core 枚举状态、ledger 公共契约、支付工具 facade、钱包 application facade、Spend Rule、治理 apply 和生产配置仍未打开。 |
| 后续入口 | 下一轮必须重新确认单一 Execution Grant。可选候选包括授权支付工具应用入口、授权权益生命周期、完整 dispute/chargeback case，或其他经过 Round 0 的单一任务包；B4-AUTH-RACE 后续已由第 8.15 闭合。 |

### 8.13 authRaceRound0Scan（历史，2026-06-03）

本节记录 B4-DISPUTE-SEMANTIC-ALIGNMENT 闭环后的下一轮 GSD-CAD Round 0。扫描只读取当时的 Java、测试和任务材料，不修改生产代码、测试代码、DDL/H2 schema 或运行时配置；结论当时用于把 `B4-AUTH-RACE` 收敛成可确认的单一 Execution Grant 候选，后续已由 `47c5269` 消费为首轮代码闭环。

| 扫描项 | 当前事实 | 对 `B4-RACE-RED-001` 的含义 |
| --- | --- | --- |
| 现有顺序覆盖 | `FundsAuthorizationTransactionFlowTests` 已覆盖部分完成后过期、过期超剩余失败、撤销超剩余失败、退款超已完成失败、chargeback 超已完成失败，以及 reversal、settle、refund、chargeback 的同业务流水幂等和不同摘要拒绝。 | 顺序金额闭合和幂等摘要已有较强回归资产；下一轮 Red 不应重复证明顺序路径，而要证明并发竞争下仍不重复入账、释放或回退。 |
| 现有并发覆盖 | 未发现专门覆盖同一授权 settle、reversal、expire、settleRefund 并发竞争的授权 flow 或 service-level 并发测试。 | `B4-RACE-RED-001` 首批 Red 应使用真实 Spring Bean、H2 表和授权 flow helper 构造同一原授权的并发后续事件，断言最终状态、剩余金额、ledger entry、posting plan、余额桶和失败无副作用。 |
| 生命周期更新触点 | `DefaultFundsInstructionLifecycleSaver#markSucceeded` 读取 transaction 和 detail 后更新明细，再按内存态累计 `settledAmount`、`reversedAmount`、`refundedAmount`、`declinedAmount` 并普通 `update(transaction)`。当前扫描未见授权生命周期专用状态版本、行锁、唯一约束或同授权后续事件串行化证据。 | 现有实现可能在并发更新时出现 lost update 或双通过校验风险；Red 预期应先失败或暴露不稳定。若最小 Green 需要版本字段、唯一索引、H2 schema 或数据库锁，必须停止并扩展 Grant。 |
| 命令入口和事务 | `FundsTransactionCommandServiceImpl` 的 authorize、reversal、expire、settle、settleRefund 都是单方法事务，最终委派 converter 和 orchestrator。 | 首轮写入应优先落在目标测试；Red 证明缺口后，只允许在 transaction-impl 生命周期、编排串行化或等价最小锁策略内修复，不扩大到支付工具 facade、VCC、清结算或治理。 |
| 测试工具基础 | `AbstractFundsServiceTest` 已提供真实 Spring Bean、H2、`LockTemplate` 测试基础设施；`FundsTransactionFlowTestSupport` 已有 authorize、settle、reversal、expire、refund helper。 | Red 可复用现有 helper，必要时新增专用并发 helper；测试不得只断言“不报错”或交易状态，必须断言余额、route/posting/entry/projection、幂等摘要和失败方无副作用。 |
| 准入裁决 | 历史裁决为 `ROUND0_READY_NOT_CODE_AUTHORIZED`；当前状态为 `CONSUMED_BY_47C5269`。 | `B4-AUTH-RACE` 单一任务包已闭环，不再作为当前默认恢复入口。 |

### 8.14 authRaceGrantCandidate（历史，2026-06-03）

本节把 `B4-AUTH-RACE` 收敛为当时可确认的 GSD-CAD 原子任务包。它只处理账户主体型授权内核的并发竞争红线，不包含授权支付工具 application facade、授权占券和权益生命周期、完整 dispute/chargeback case、VCC、Spend Rule、清结算对账或治理 apply；该候选后续已由 `47c5269` 消费。

| 项 | 当前口径 |
| --- | --- |
| Task ID | `B4-AUTH-RACE`。 |
| Execution Grant 关联 | 已消费；历史状态曾为 `待用户确认`。 |
| mvpScenario | 同一已批准授权在完成、撤销、过期和退款等后续事件并发到达时，资金底座必须保证金额闭合、状态合法、route/ledger/projection 不重复、不遗漏，失败方无资金副作用。 |
| 首批 Red | `B4-RACE-RED-001`：同一授权的 settle 与 expire 或 settle 与 reversal 并发竞争时，只能有一个合法金额迁移获胜，失败方不得产生 route、posting、ledger entry、projection 或余额变化；必要时扩展 settle 与 settleRefund 并发。该 Red 已由 `47c5269` 回归化。 |
| 允许写入范围 | 历史候选曾允许先写 `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java` 或新增同包 `FundsAuthorizationTransactionRaceFlowTests.java` 的目标 Red；Red 证明缺口后，只允许在 `transaction-impl` 的生命周期保存、编排串行化或等价最小锁策略内修复。该范围已消费。 |
| 只读参考范围 | `transaction-face` 请求契约、`FundsAuthorizationInstructionConverter`、`DefaultFundsInstructionLifecycleSaver`、route replay、ledger posting 装配、`tests/src/test/resources/jdbc-schema.sql` 和现有 B4 授权 flow 测试。 |
| 默认禁止范围 | 不改公共请求字段、core 枚举/状态、ledger 公共契约、DDL/H2 schema、支付工具 facade、钱包 application facade、VCC、Spend Rule、完整 dispute/chargeback case、清结算追偿、治理 apply、生产配置、外部协议或敏感数据处理。 |
| 扩权停止条件 | 若最小 Green 需要数据库唯一约束、锁字段、版本字段、H2 schema、公共契约、状态机或错误码变更，立即停止并要求新的 Execution Grant 明确授权。 |
| 验证命令 | `just test-one FundsAuthorizationTransactionFlowTests tests` 或新增并发测试类的 `just test-one <TestClass> tests`；回归 `just test-transaction`、`just test-business-flow`、`just test-boundary`；提交前 `just compile`、`just pmd`、`git diff --check`。 |
| Git 策略 | 历史候选建议 `auto_commit`；已按自动提交完成。 |
| handoff | `B4-AUTH-RACE` 不再作为当前默认恢复入口；若后续选择授权支付工具应用入口、授权权益生命周期或完整 dispute/chargeback case，则必须另起 Round 0，不复用本候选写入范围。 |

### 8.15 authRaceCompletionEvidence（2026-06-03）

| 证据 | 结论 |
| --- | --- |
| 实现提交 | `47c5269 fix(transaction): 串行化授权后继并发竞争` 新增授权后继并发竞争测试，并在授权后继命令进入编排前按原授权流水串行化。 |
| 覆盖 Red | `B4-RACE-RED-001` 首轮已转为回归基线；历史测试曾证明同一授权 settle / expire / reversal 并发竞争时只有一个赢家，失败方没有 route、posting、ledger entry、projection 或余额副作用。2026-06-26 后，expire 路径已退出目标态，当前只保留 settle / reversal / settleRefund 等可信后继事件竞争边界。 |
| 实现边界 | 命令层在事务完成前持有 JVM 锁，Mapper 读取授权原交易时使用 `FOR UPDATE` 行锁；完成和撤销继续以剩余可迁移金额做前置校验。 |
| 验证证据 | 已通过 `git diff --check`、`just compile`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary` 和 `just pmd`。 |
| 剩余边界 | 未新增数据库唯一约束、锁字段、版本字段或 H2 schema；未实现跨节点分布式锁、支付工具 application facade、授权权益生命周期、完整 dispute/chargeback case、清结算追偿、VCC processor、Spend Rule 或治理 apply。 |

## 9. verificationPlan

| 阶段 | 命令 | 通过口径 |
| --- | --- | --- |
| Round 0 | `git status --short`、目标文档索引检查、B4 任务锚点复核。 | 工作树变更已分类；B4-04/05/07/08 可追踪到 AC、DSL、TDD 和 OpenSpec。 |
| Red | `just test-one FundsAuthorizationTransactionFlowTests tests`。 | 新增 Red 必须先按预期失败；若没有失败，先判断已有实现覆盖或 Red 写错。 |
| Green | 目标 `test-one` 命令。 | 只做最小修复，让目标 Red 和既有授权回归通过。 |
| 回归 | `just test-transaction`、`just test-business-flow`；触碰边界时补 `just test-boundary`。 | 授权交易相关范围通过；模块边界未破坏。 |
| 提交前 | `just compile`、`just pmd`、`git diff --check`。 | 编译、规约和空白检查通过；如因私服、网络或环境失败，必须区分环境问题和代码问题。 |

## 10. stopConditions

1. 需要新增或修改公共契约、枚举、状态机、Request/DTO、错误码、表结构或 H2 schema，但 Execution Grant 未授权。
2. Red 目标开始触碰支付工具 facade、VCC 全生命周期、Spend Rule 表、清结算、对账、追偿、治理 apply、P2 轨道协议或敏感数据。
3. 测试只能证明状态变化、entry 数量或接口不报错，不能证明金额、route、posting、ledger entry、projection、幂等和失败无副作用。
4. 拒付语义需要落独立 dispute case、chargeback 服务入口或清结算追偿对象，超出本卡 canonical 授权内核范围。
5. 并发红线需要新增数据库唯一约束、锁字段、版本字段或 H2 schema，但授权未扩展到 DDL/H2。
6. 工作树出现未分类变更，或用户未提交变更与本任务写入范围冲突。

## 11. confirmationTemplate

历史模板，已由 `616dac1` 和 `3825466` 消费；后续不再作为默认下一轮授权模板，除非用户明确要求返工或扩展 FORCE 策略能力：

```text
Execution Grant：B4-FORCE-SETTLE
确认基线：确认时 Git HEAD（至少包含 b0666ba / f99f3a3 / 107218a / 616dac1 / 3825466）
允许写入：先写 tests 中 B4-FS 目标 Red；Red 证明缺口后允许 transaction-face 的 FundsAuthorizationTransactionSettleRequest 兼容字段、transaction-impl converter/command/lifecycle/route replay、TDD tests 最小修复
允许契约字段：settleMode、forceSettlePolicyCode、forceSettleLimitAmount、forceSettleReason、externalOriginalFactRef、forceSettleVoucherRef、operator/contextVariables 或等价命名；允许把 `authorizationTransactionSn` 调整为普通完成必填、首轮 FORCE 模式不携带且不查询原授权账本交易；字段名、类型、必填规则、摘要字段和兼容策略以本次 Grant 为准
策略与审计：forceSettlePolicyCode 和 forceSettleLimitAmount 必须来自本次 Grant 声明的内部白名单、审批结果或受信策略快照；审计最小集为 WindOperator、forceSettleReason、externalOriginalFactRef、forceSettleVoucherRef 和受信策略/审批快照引用，contextVariables 只作为白名单补充
禁止写入：支付工具 facade、VCC 预付卡充值、共享卡调额、DDL/H2 schema、Spend Rule 表结构、策略引擎、规则表、processor 生命周期、no-auth refund、chargeback 增强、清结算对账、治理 apply、生产配置、外部协议、敏感数据处理
首批 Red：B4-FS-RED-001；必要时补 B4-FS-RED-002
验证命令：just test-one FundsAuthorizationTransactionFlowTests tests；just test-transaction；just test-business-flow；just test-boundary；just compile；提交前 just pmd 和 git diff --check
Git 策略：auto_commit
停止条件：公共契约扩展未列名、表结构、外部规则、清结算对账、P2、敏感数据或工作树冲突越界即停止
```

```text
Execution Grant：B4-NO-AUTH-REFUND
确认基线：确认时 Git HEAD；必须包含 B4 过期释放、强制完成、无授权退款主文档口径、Grant 可执行包和恢复入口相关提交，当前已知最小提交集为 b0666ba / f99f3a3 / 616dac1 / 3825466 / e937395 / fe40d4a / 8e1ec76 / 51e86e3 / 3e5ec76；后续 docs-only 索引、恢复入口或确认基线校准提交以确认时 Git HEAD 自然纳入，无需在模板中逐条追写
允许写入：先写 tests 中 B4-NAR 目标 Red；Red 证明缺口后允许 transaction-face 的 FundsAuthorizationTransactionRefundRequest 兼容字段、transaction-impl converter/command/lifecycle/route replay、TDD tests 最小修复
允许契约字段：authorizationTransactionSn 空值语义、externalReferenceSn、refundReason、operator/contextVariables 或等价命名；允许内部补充 NO_AUTH 归类标签；允许把 `authorizationTransactionSn` 调整为普通授权链退款必填、NO_AUTH 模式不携带且不查询原授权账本交易；字段名、类型、必填规则、摘要字段和兼容策略以本次 Grant 为准
审计最小集：WindOperator、refundReason、externalReferenceSn；contextVariables 只作为白名单补充，不承载核心资金事实、外部协议报文、运营凭证或敏感数据
禁止写入：支付工具 facade、钱包 application facade、VCC 生命周期、DDL/H2 schema、ledger 公共契约、core 枚举状态、Spend Rule 表结构、force settle、chargeback 独立入口、清结算追偿、治理 apply、生产配置、外部协议、敏感数据处理
首批 Red：B4-NAR-RED-001；必要时补 B4-NAR-RED-002
验证命令：just test-one FundsAuthorizationTransactionFlowTests tests；just test-transaction；just test-business-flow；just test-boundary；just compile；提交前 just pmd 和 git diff --check
Git 策略：auto_commit
停止条件：公共契约扩展未列名、表结构、ledger 公共契约、外部规则、清结算对账、P2、敏感数据或工作树冲突越界即停止
```

B4-NO-AUTH-REFUND 模板已由 `006bcaa` 消费；后续只作为历史授权样例和回归范围参考，不作为新的自动编码授权。

```text
Execution Grant：B4-AUTH-RACE
确认基线：确认时 Git HEAD；必须包含 B4 授权过期释放、强制完成、无授权退款、争议退款可区分性和本轮 B4-AUTH-RACE Round 0 候选包相关提交
允许写入：先写 tests 中 B4-RACE 目标 Red；Red 证明缺口后允许 transaction-impl 生命周期保存、编排串行化或等价最小锁策略修复
允许测试资产：FundsAuthorizationTransactionFlowTests 或同包新增 FundsAuthorizationTransactionRaceFlowTests；必须使用真实 Spring Bean、H2 表和现有授权 flow helper
禁止写入：公共请求字段、core 枚举或状态、ledger 公共契约、DDL/H2 schema、支付工具 facade、钱包 application facade、VCC、Spend Rule、完整 dispute/chargeback case、清结算追偿、治理 apply、生产配置、外部协议、敏感数据处理
首批 Red：B4-RACE-RED-001
验证命令：just test-one FundsAuthorizationTransactionFlowTests tests 或 just test-one FundsAuthorizationTransactionRaceFlowTests tests；just test-transaction；just test-business-flow；just test-boundary；just compile；提交前 just pmd 和 git diff --check
Git 策略：auto_commit
停止条件：需要 DDL/H2、数据库唯一约束、锁字段、版本字段、公共契约、状态机、错误码、外部规则、清结算对账、P2、敏感数据或工作树冲突越界即停止
```

B4-AUTH-RACE 模板已由 `47c5269` 消费；后续只作为历史授权样例和回归范围参考，不作为新的自动编码授权。
