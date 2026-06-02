# B4 授权后继能力 Round 0 准入卡

## 1. 文档定位

本文档是 B4 授权后继能力的 Round 0 候选准入卡。它把 B4-TRX-EXPIRE 已完成后的剩余授权交易缺口收敛为账户主体型 canonical 内核的候选 Execution Grant 输入页。

本文档不授权修改生产代码、测试代码、DDL/H2 schema 或运行时配置。只有用户确认本页或确认经调整后的单一 MVP Execution Grant 后，才允许把本文档中的 Red 候选转成实际测试写入。

## 2. authorityBaseline

| 基线项 | 当前口径 |
| --- | --- |
| 代码和文档基线 | 以确认时 Git HEAD 为准；当前已提交 B4-TRX-EXPIRE 实现基线为 `b0666ba feat: 补齐授权过期释放 canonical 能力`，证据回填基线为 `f99f3a3 docs: 回填授权过期释放完成证据`，B4-FORCE-SETTLE 候选契约收敛基线为 `107218a docs: 收敛 B4 强制完成契约候选`。 |
| 已关闭能力 | B4-03 授权过期释放：已新增 `EXPIRE` 事件、`EXPIRED` 状态、`FundsAuthorizationTransactionExpireRequest`、`FundsAuthorizationTransactionService#expire`、route replay、ledger posting 释放路径和生命周期金额校验。 |
| 产品入口 | `docs/产品设计/05-产品验收与TDD用例矩阵.md` 的 `AC-AUTH-011` 无授权强制完成、`AC-AUTH-012` 无授权直接退款、授权拒绝与拒付区分、原路径回放和授权并发红线。 |
| DSL 入口 | `docs/DSL设计/支付资金底座DSL承载层设计.md` 的 `DSL-AUTH-FORCE-CAPTURE-001`、`DSL-AUTH-REFUND-001`、`AUTHORIZATION_TRANSACTION / SETTLE` 强制完成模式、`AUTHORIZATION_TRANSACTION / AUTH_REFUND` 无授权退款模式和拒付语义承接口径。 |
| 系分入口 | `docs/系分设计/02-交易路由钱包账目与投影系分设计.md` 的授权交易服务契约、状态流转、route replay、投影解释和资金红线。 |
| TDD 入口 | `docs/TDD设计/支付资金底座测试驱动设计.md` 的授权交易测试矩阵、B4 覆盖索引、`TDD-AUTH-*`、`TDD-ROUTE-*`、`TDD-RACE-*` 和 `TDD-RED-*`。 |
| OpenSpec 入口 | `openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/design.md` 和 `openspec/changes/tdd-baseline-reset/tasks.md` 的 B4-04、B4-05、B4-07、B4-08。 |

## 3. grantCandidate

| 字段 | 候选取值 |
| --- | --- |
| `mvpScenario` | 外部没有前置授权但存在必须入账或必须退款的已确认资金事实，或已完成授权发生拒付/争议，或同一授权后续事件并发竞争。系统必须保持金额、状态、原路径、幂等和审计可解释。 |
| `abilityBatch` | B4 授权交易账户主体型 canonical 内核后继能力；本卡不包含 B4-AUTH-PI 支付工具 application facade、VCC 全生命周期、Spend Rule 生产控制、清结算对账或资金数据治理。 |
| `businessQuestion` | 运营、财务和研发能否解释一笔无授权强制完成、无授权退款、拒付/争议扣回或授权后续事件并发竞争为什么允许、为什么失败、影响哪些资金事实，以及哪些事实绝不能发生。 |
| `moneyFact` | 强制完成是已确认外部消费结果的资金事实；无授权退款是基于外部原消费、原完成或差错凭证的逆向资金事实；拒付是争议/扣回语义，不是授权拒绝，也不能被压缩成不可区分的普通退款。 |
| `userVisibleResult` | 用户或商户看到账单、退款、扣回或失败原因；运营和财务能追溯原事实引用、原因、凭证、策略、上限、route snapshot、ledger transaction、projection 和审计上下文。 |
| `productNotDone` | 不声明完整 VCC 发卡、完整 chargeback case 管理、完整清结算追偿、外部卡组织规则、Spend Rule 引擎、支付工具 facade 或治理重放生产能力。 |
| `firstRedSet` | 建议先选 `B4-FS-RED-001` 强制完成必填策略和无伪造授权；再按授权确认选择 `B4-NAR-RED-001`、`B4-CB-RED-001` 或 `B4-RACE-RED-001`。 |
| `currentEvidence` | `b0666ba` 已证明授权过期释放基础能力闭合；剩余后继能力仍是设计和任务候选，不因 B4-TRX-EXPIRE 通过而自动获得编码授权。 |

### 3.1 architectureReviewMap

| 架构审查项 | 本卡落点 |
| --- | --- |
| 背景、目标、非目标、成功标准 | 背景是 B4-TRX-EXPIRE 已闭合但授权后继能力仍缺 Round 0 输入；目标是拆出可授权的最小 Red；非目标是不混入支付工具 facade、VCC、Spend Rule、清结算对账或治理；成功标准是候选切片能追溯到 AC、DSL、TDD、写入范围和停止条件。 |
| 核心决策、职责边界和取舍 | 核心决策是只推进账户主体型 canonical 授权内核；支付工具、VCC、Spend Rule 和清结算能力保持独立授权；取舍是优先 B4-FORCE-SETTLE，暂不一次性打开全部授权后继状态。 |
| 接口契约、入参、错误码、幂等和兼容 | Execution Grant 必须显式列名 settle 强制完成策略字段、settleRefund 原事实引用、凭证、原因、审计、错误码和幂等摘要；还必须声明 `authorizationTransactionSn` 的条件化规则：普通完成必填并查询原授权，首轮 FORCE 模式不得依赖内部授权流水或查询原授权账本交易，必须改由外部原事实引用和受信策略/审批快照承接；未列名时不得修改公共契约。 |
| 数据方案、事务边界、一致性和补偿 | Red 必须证明 route snapshot、posting plan、ledger transaction、ledger entry、projection、余额桶和失败无副作用；并发切片若需要唯一约束、锁字段、版本字段或补偿路径，必须扩权确认。 |
| 可靠性、安全、权限、审计和告警 | 本卡不授权生产发布、外部协议或敏感数据；所有强制完成、退款和拒付切片都必须保留原因、凭证、外部引用、脱敏审计和失败可解释性；强制完成的审计最小集为 `WindOperator`、`forceSettleReason`、`externalOriginalFactRef`、`forceSettleVoucherRef` 和受信策略/审批快照引用，`contextVariables` 只能作为白名单补充，不承接核心资金事实；权限和告警只作为后续 Execution Grant 待确认项。 |
| 验证方案、测试、静态检查和回归 | 每个切片先写目标 Red，再跑 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check` 中的授权命令。 |
| 发布、灰度、回滚、风险和待确认 | 本卡不进入生产发布；若后续编码触碰公共契约、DDL/H2、外部规则、清结算追偿或敏感数据，必须停止并补待确认项、风险说明、灰度和回滚策略。 |

## 4. acceptanceMap

| 设计锚点 | 本候选覆盖 | 本候选不覆盖 |
| --- | --- | --- |
| 产品验收 | `AC-AUTH-011`、`AC-AUTH-012`、`RED-003`、`RED-005`、拒付与授权拒绝区分、原路径回放和失败无副作用。 | `AC-AUTH-008` 至 `AC-AUTH-010` 发卡 Spend Controls 扩展、完整 VCC 卡处理、清结算出款追偿和外部规则最终确认。 |
| DSL caseId | `DSL-AUTH-FORCE-CAPTURE-001`、`DSL-AUTH-REFUND-001`、授权拒付承接、授权后续事件 route replay。 | 新支付工具 DSL 入口、P2 ACH/收单/全球账户 DSL、治理 apply DSL。 |
| TDD 用例 | `TDD-AUTH-*`、`TDD-AUTH-FLOW-*`、`TDD-AUTH-ERR-*`、`TDD-ROUTE-005`、`TDD-ROUTE-009`、`TDD-RACE-001` 至 `TDD-RACE-003`、`TDD-RED-003`、`TDD-RED-005`。 | B2 支付工具资源服务测试、B5 Spend Rule 控制生产测试、B7/B8 独立能力域测试和 P2 业务能力包测试。 |
| 产品红线 | 不伪造授权占用；无原事实或无凭证不得静默退款；拒付不得计入授权拒绝；并发不得重复释放、重复入账或让授权剩余为负。 | 卡组织时限、dispute representment、retrieval request、完整证据包运营流程和会计/合规最终口径。 |

## 5. writeScopeCandidate

| 范围 | 候选授权 |
| --- | --- |
| 目标测试资产 | 优先允许写 `tests/src/test/java/com/capte/funds/transaction/application/flow/FundsAuthorizationTransactionFlowTests.java`；必要时允许新增 B4 后继能力专用 flow 测试类或补 `DefaultRouteReplayServiceTests`。 |
| 生产实现 | 只有 Red 证明真实缺口后，才允许在 `transaction-face`、`transaction-impl`、route replay 和必要的 ledger posting 装配最小范围修复。 |
| 公共契约 | 默认不允许破坏既有请求；若必须新增 settle 强制完成策略字段、原事实引用、凭证、原因、审计字段或查询解释字段，Execution Grant 必须显式列名并说明兼容策略；若调整 `authorizationTransactionSn`，必须明确只在 `settleMode=FORCE` 或等价强制完成模式下条件化，普通完成的原授权引用语义保持不变。 |
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
| `B4-NAR-RED-001` | 无前置授权但存在外部原消费、原完成或差错凭证时，系统是否能直接退款回补。 | 无授权退款必须引用外部原事实和凭证，不补造授权占用，不按当前绑定重新选路。 | settleRefund 无授权退款请求携带原事实引用、原因、凭证、审计和金额币种；成功后形成退款资金事实和可解释投影。 | 无原事实、无凭证、无原因或无审计不得静默退款；不得创建内部授权占用；不得按当前工具绑定重选路。 | 原事实引用、凭证、原因、审计、金额累计、route replay、ledger entry、projection、幂等、失败无副作用。 | `FundsAuthorizationTransactionFlowTests` 或新 no-auth refund flow 测试。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、必要时 `just test-transaction`。 | 需要扩展 settleRefund Request/DTO 或 route replay 公共契约但未授权。 |
| `B4-NAR-RED-002` | 缺原事实或缺凭证的无授权退款是否失败且无副作用。 | 不存在可追溯来源时不能生成退款资金事实。 | 返回可解释失败或进入差错候选，不产生 ledger transaction。 | 不得静默退款；不得把外部流水字符串当作充分原事实；不得写入不可核对投影。 | 失败原因、无 route/posting/entry/projection、余额不变、审计上下文脱敏。 | `FundsAuthorizationTransactionFlowTests`。 | `just test-one FundsAuthorizationTransactionFlowTests tests`。 | 产品要求转人工差错而不是直接失败，需要先确认状态和数据落点。 |
| `B4-CB-RED-001` | 已完成授权发生争议、拒付或扣回时，系统是否能与普通退款、授权拒绝区分。 | 拒付是争议/扣回语义，累计退款和拒付不得超过已完成金额，且必须沿原完成或原 route snapshot。 | settleRefund 携带拒付原因、外部引用、凭证和审计上下文；查询和投影能区分普通退款与拒付承接。 | 不得把授权拒绝记成拒付；不得要求独立 `chargeback` 服务入口作为目标态；不得把拒付压缩成不可区分的普通退款。 | 拒付原因、凭证、外部引用、原完成引用、累计金额、route replay、projection 可区分、失败无副作用。 | `FundsAuthorizationTransactionFlowTests`、交易投影相关测试。 | `just test-transaction`、必要时 `just test-business-flow`。 | 需要新增 dispute case 模型、清结算追偿或 chargeback 独立状态机。 |
| `B4-RACE-RED-001` | 同一授权的完成、撤销、过期、退款并发竞争是否会重复入账、重复释放或剩余为负。 | 同一授权同一时刻只有合法迁移生效；失败方不得产生资金副作用。 | 通过幂等键、状态版本、唯一约束、锁定策略或等价机制保证完成、撤销、过期、退款金额闭合。 | 不得重复释放 AUTHORIZATION；不得累计完成超授权；不得出现负 remaining；不得生成重复 ledger entry。 | 并发结果、最终状态、授权剩余、ledger entry 唯一性、余额桶、幂等摘要、失败无副作用。 | 授权并发 flow 测试或 service-level 并发测试。 | `just test-business-flow`、必要时 `just test-transaction`。 | 需要新增数据库唯一约束、锁字段、版本字段或 H2 schema。 |

### 7.1 existingCoverageScan（2026-06-02）

本节记录 B4-FORCE-SETTLE 编码授权前的只读覆盖扫描。扫描只读取现有生产代码、测试代码和设计文档，不修改生产代码、测试代码、DDL/H2 schema 或运行时配置；结论只作为下一轮 Execution Grant 的失败点和写入边界输入。

| redId | 既有覆盖资产 | 当前覆盖判断 | 下一轮 Red 预期失败点 |
| --- | --- | --- | --- |
| `B4-FS-RED-001` | `FundsAuthorizationTransactionSettleRequest`、`FundsAuthorizationInstructionConverter#convertToSettleInstruction`、`AuthorizationFundsInstructionRouteResolver#resolveSettle`、`FundsAuthorizationTransactionFlowTests` 中普通 settle、部分 settle、settle 后 expire 和 settle 幂等用例。 | 普通授权完成链路覆盖充分：请求必须有 `authorizationTransactionSn`，converter 无条件构造 `AUTHORIZATION` reference 并查询原授权账本交易，route resolver 依赖原授权主体解析，测试断言 AVAILABLE/AUTHORIZATION/SETTLEMENT、route、ledger transaction、entry、projection 和幂等。当前代码没有 FORCE 模式、`authorizationTransactionSn` 条件化、强制完成策略编码、上限、原因、凭证或无前置授权外部事实引用字段。 | 如果直接写无前置授权强制完成 Red，应先失败在 Request 契约和 converter：`authorizationTransactionSn` 必填且 `authorizationLedgerTransactionSn` 会要求原授权账本交易存在。Red 应证明 FORCE 模式不依赖内部授权流水、缺策略、缺上限、缺原因、缺审计或缺外部事实引用不得入账，且成功路径不得伪造授权占用。 |
| `B4-FS-RED-002` | `FundsAuthorizationTransactionFlowTests#testAuthorizationSettleSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects`、`DefaultFundsInstructionLifecycleSaver` 的请求摘要和状态累计逻辑。 | 同业务流水同摘要重试、不同摘要拒绝、余额和账务事实保持不变已有普通完成覆盖；但覆盖对象仍是基于原授权的普通 settle，不覆盖无策略、超上限、缺审计或缺外部事实引用的强制完成失败路径。 | Red 应沿用现有幂等和失败无副作用断言结构，但需要先扩展目标 Request 字段或等价上下文字段；若未扩权，测试不得落地。 |
| `B4-CB-RED-001` | `FundsAuthorizationTransactionFlowTests` 已有拒付幂等和余额断言，`FundsAuthorizationTransactionService#chargeback` 仍是现有方法。 | 既有测试证明代码可用 `CHARGEBACK` 事件承接一类拒付资金影响；但目标态文档要求拒付不强制落独立 `chargeback` 入口，并能通过 `settleRefund` 的原因、凭证和审计上下文表达。该差异属于后续语义校准，不应混入 B4-FORCE-SETTLE。 | 如选择 B4-DISPUTE-CHARGEBACK，应单独确认是保留兼容方法并补投影区分，还是收敛到 `settleRefund` 语义；不得在 force settle 切片内处理。 |

### 7.2 forceSettleContractCandidate

本节只定义 B4-FORCE-SETTLE 首轮编码候选契约，不等于授权改公共契约。字段名、类型和必填规则必须在后续 Execution Grant 中最终确认；未确认前不得写 Java 代码或测试代码。

| 候选字段 | 语义 | 首轮建议 | Red 断言 | 不纳入本轮 |
| --- | --- | --- | --- | --- |
| `settleMode` | 区分普通授权完成和无授权强制完成。 | 必须显式为 `FORCE` 或等价枚举/字符串；普通完成默认仍走现有 `authorizationTransactionSn`。首轮 FORCE 模式不得携带或依赖 `authorizationTransactionSn`，不得查询原授权账本交易；若后续要支持带原授权的 overcapture 或 late clearing，必须另起 Execution Grant。 | 缺 FORCE 模式不得走无授权完成；普通完成语义不变；FORCE 模式携带内部授权流水时失败或转人工差错，不回退到普通完成。 | 不新增复杂 processor 状态机；不支持带原授权的 overcapture 扩展。 |
| `forceSettlePolicyCode` | 说明为什么允许无授权消费入账。 | 必填，进入请求摘要和审计上下文；必须来自本次 Grant 声明的内部白名单、审批结果或受信策略快照，不得由外部调用方自由声明为充分凭证。 | 缺策略、策略不在白名单或策略来源不可审计时失败且无 route、posting、entry 或 projection。 | 不实现策略引擎、规则表或外部规则计算。 |
| `forceSettleLimitAmount` | 单笔强制完成可接受上限。 | 必填，币种沿 `transactionAmount`；金额不得超过上限；上限必须来自受信策略或审批快照，不能只依赖请求自填值。 | 超上限或上限来源不可审计时失败且无资金副作用。 | 不实现额度窗口、日/月累计控制。 |
| `forceSettleReason` | 业务原因，例如 forced post、late clearing 或差错完成。 | 必填，保存到上下文和审计摘要。 | 缺原因失败。 | 不定义卡组织最终原因码全集。 |
| `externalOriginalFactRef` | 外部已确认消费事实引用。 | 必填，可为脱敏外部流水、clearing/presentment 引用或等价结构。 | 缺引用失败；不得使用内部授权流水伪造。 | 不保存完整原始报文、PAN、CVV 或生产凭证原文。 |
| `forceSettleVoucherRef` | 凭证或审批引用。 | 首轮建议必填，可为摘要、文件编号、审批号或外部 reference。 | 缺凭证失败。 | 不落完整凭证文件，不做运营审批系统。 |
| `operator` / `contextVariables` | 操作者和审计上下文。 | 审计最小集使用现有 `WindOperator`、`forceSettleReason`、`externalOriginalFactRef`、`forceSettleVoucherRef` 和受信策略/审批快照引用；`ReadonlyContextVariables` 只承接白名单补充字段，敏感字段继续由 validator 阻断。 | 缺操作者、缺原因、缺外部引用、缺凭证、缺受信策略来源或敏感上下文按现有规则失败/阻断。 | 不新增权限系统，不引入生产配置；不把核心资金事实塞进普通上下文。 |

## 8. suggestedGrantSlices

| 切片 | 优先级 | 目标 | 首批 Red | 允许写入建议 | 不适合混入 |
| --- | --- | --- | --- | --- | --- |
| B4-FORCE-SETTLE | 1 | 补齐 settle 强制完成模式的策略、上限、原因、审计和无伪造授权占用。 | `B4-FS-RED-001`、`B4-FS-RED-002`。 | `tests` 授权 flow、`transaction-face` 请求契约、`transaction-impl` 编排和 route/posting 最小修复。 | 无授权退款、拒付、支付工具 facade、Spend Rule、VCC。 |
| B4-NO-AUTH-REFUND | 2 | 补齐 settleRefund 无授权退款模式的原事实引用、凭证、原因、审计和失败无副作用。 | `B4-NAR-RED-001`、`B4-NAR-RED-002`。 | `tests` 授权退款 flow、必要请求契约和 route replay 最小修复。 | force settle、chargeback case 全生命周期、清结算追偿。 |
| B4-DISPUTE-CHARGEBACK | 3 | 固化拒付/争议扣回语义与普通退款、授权拒绝的可区分性。 | `B4-CB-RED-001`。 | 交易 flow、投影解释、原因/凭证/外部引用字段的最小补强。 | 独立 dispute system、VCC processor、清结算追偿单。 |
| B4-AUTH-RACE | 4 | 固化授权完成、撤销、过期、退款并发竞争红线。 | `B4-RACE-RED-001`。 | 授权 flow 并发测试、状态迁移保护和必要幂等/锁策略。 | DDL/H2 默认不允许，除非 Execution Grant 显式扩权。 |

推荐下一轮编码起点为 B4-FORCE-SETTLE。它延续 B4-TRX-EXPIRE 的账户主体型 canonical 授权内核，不需要先打开支付工具 facade、VCC 生命周期、Spend Rule 表结构或清结算追偿。

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

```text
Execution Grant：B4-FORCE-SETTLE
确认基线：确认时 Git HEAD（至少包含 b0666ba / f99f3a3 / 107218a）
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
确认基线：确认时 Git HEAD（至少包含 b0666ba / f99f3a3）
允许写入：先写 tests 中 B4-NAR 目标 Red；Red 证明缺口后允许 transaction-face、transaction-impl、route replay、TDD tests 最小修复
禁止写入：支付工具 facade、VCC 生命周期、DDL/H2 schema、Spend Rule 表结构、force settle、chargeback 独立入口、清结算追偿、治理 apply、生产配置、外部协议、敏感数据处理
首批 Red：B4-NAR-RED-001；必要时补 B4-NAR-RED-002
验证命令：just test-one FundsAuthorizationTransactionFlowTests tests；just test-transaction；just test-business-flow；just test-boundary；just compile；提交前 just pmd 和 git diff --check
Git 策略：auto_commit
停止条件：公共契约扩展未列名、表结构、外部规则、清结算对账、P2、敏感数据或工作树冲突越界即停止
```

确认模板前，本文档只作为 Round 0 准入卡和 TDD 分析产物，不进入编码。
