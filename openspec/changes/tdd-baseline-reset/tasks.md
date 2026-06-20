# TDD 基线重置 Harness Plan

业务目标：把支付资金底座的产品验收、业务对象、状态、业务流程、规则矩阵和测试红线拆成可执行的单一 MVP 任务；用户价值是让产品、运营、财务和研发能清楚看到每个 Execution Grant 的范围、非目标、风险、待确认项和验收证据。

## 0. 当前状态

- [x] 作废旧 OpenSpec specs。
- [x] 作废旧 OpenSpec changes。
- [x] 删除旧测试源码。
- [x] 保留测试 resources。
- [x] 重建 OpenSpec 项目上下文。
- [x] 重建支付资金底座开发基线规格。
- [x] 校准 TDD 文档中“旧测试资产复用”表述。
- [x] 冻结当前设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果，作为进入编码前的独立检查点；上一已冻结设计交付提交点为 30b1a00 docs: 冻结权益快照设计基线，上一冻结点为 620b5a5 docs: 精简并加固资金底座设计交付文档。
- [x] 完成设计、代码、任务三方基线对齐：`eabd672` 纳入编码准备包，`6be9c99` 完成 B1-10 权益快照 DSL 契约承载，`75b46ef` 和 `9db3eba` 收敛请求摘要与稳定摘要支撑，`434f8a2` 校准账务计划装配器长 ID 测试规范，`5bad4b7` 对齐权益资金流编码基线，`42100dc` 固化权益生产准入门禁，`555a943` 至 `db766fa` 完成权益准入任务基线、架构师工程约规、CAD 自动推进约规、测试最佳实践、预算组默认周期 `LIFETIME`、账务事实断言、钱包/交易/治理边界、CAD 完整验证门禁和出款准入规则核验证据收敛，`f386ed0a` 至 `5901265` 完成编码准入设计冻结、出款准入设计冻结、提现/解冻红线、余额日志证据、路由事实边界、交易投影解释、权益回放摘要和治理重放范围/来源/差异项校验收敛，`5901265..77bc9f4` 完成资金事实红线、敏感上下文阻断、上下文不可变、防御性拷贝、外部账户原文阻断、MVP/归档边界和设计交付口径收敛；任务以本 Harness Plan 和 OpenSpec spec 为准。
- [x] 2026-05-31 完成 PRD、DSL、系分、TDD、OpenSpec、代码和任务基线的只读对齐：该轮已提交设计和任务对齐输入推进至 `8e4a801 docs: 对齐最新设计任务基线`；当时的代码能力基线为 `77bc9f4`，现仅作为历史局部保护证据。本轮把历史“资金来源关系”统一校准为资金责任解析关系，明确支付工具只承载交易投影和 route 快照，BudgetGroup 与 Spend Rule 只作为预算 scope、控制规则、规则快照和审计上下文；`3ef522c`、`a7d3fc9` 和 `8e4a801` 分别固化授权支付工具外层入口、钱包 application facade、兼容缺口 CR 和最新设计任务基线索引。既有测试文件变更保持隔离，未纳入本轮设计基线。
- [x] 2026-05-31 完成核心设计骨架修复：该轮已提交设计和任务对齐输入推进至 `5a78f02 docs: 明确核心设计骨架`；`8e4a801 docs: 对齐最新设计任务基线` 继续作为上一轮任务索引证据。核心设计骨架进一步明确可入账主体、产品能力、系统模块、架构分层和能力地图，作为本轮代码 CR 差异复核输入。
- [x] 2026-05-31 完成代码 CR 任务基线对齐：当前已提交设计和任务对齐输入推进至 `f99800b docs: 对齐代码 CR 任务基线`；`5a78f02 docs: 明确核心设计骨架` 继续作为核心设计骨架修复证据。
- [x] 2026-05-31 完成本轮编码准入复核：当轮已提交设计和任务对齐输入推进至 `4a7ef12 docs: 固化代码准入 CR 基线`；`f99800b docs: 对齐代码 CR 任务基线` 保留为历史代码 CR 任务基线证据。复核未发现新代码事实变化，A1 当时是单一 Execution Grant 候选；该裁决已降级为历史记录，未重新确认前不写生产代码、测试代码、DDL/H2 schema 或运行时配置。
- [x] 2026-05-31 完成 B2/B4 编码准入口径修复：系分已明确外部支付工具和工具快照才使用 `PaymentInstrumentRef`，内部余额钱包、信用额度、返利钱包和商户钱包等业务入口先解析为 `SubjectRef`、`BenefitSnapshot`、`FundingAllocationDecision` 或等价不可变快照；`PaymentInstrumentCapabilityApplicationService` 只做工具能力准入、读取工具与绑定历史、生成准入快照，不注册工具、不变更绑定；TDD 映射表已显式纳入 `AC-AUTH-000` 授权入口分层。该修复只关闭 B2/B4 进入独立 Round 0 或单一 Execution Grant 前的文档阻断，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。
- [x] 2026-05-31 完成钱包入口二次收敛：PRD、DSL、系分、TDD、VCC 分册和 OpenSpec 已把内部余额钱包、平台钱包、商户钱包、返利钱包、信用额度入口与外部钱包端点、通道 token、卡、VA 等支付工具分开；内部入口只作为 `SubjectRef`、`BenefitSnapshot`、`FundingAllocationDecision` 或等价不可变快照解析来源，外部工具才使用 `PaymentInstrumentRef` / `ExternalAccountRef`。该修复仍为文档准入口径，不授权新增 facade、Request/DTO、DDL/H2 schema 或测试资产。
- [x] 2026-05-31 完成 CAD Round 0 准入刷新：上一完整 CAD 验证证据提交为 `270122e docs: 刷新 CAD 准入基线`，工作树在刷新前为 clean；`77bc9f4` 保留为上一冻结代码能力基线，`98ec7cc..81a7ecb` 的直接交易、授权后继、失败无副作用、费用幂等、外部账户主体阻断、支付工具绑定约束和 B2/B4/钱包入口文档收敛纳入当前准入复核输入，`270122e` 后已执行 `just verify-cad` 完整门禁并通过。该刷新只更新基线、授权卡和任务计划引用，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入；后续 Execution Grant 必须以确认时 Git HEAD 作为 `authorityBaseline`。
- [x] 2026-06-01 固化支付工具入口交付基线：`FundsAuthorizationTransactionService.authorize`、直接交易和余额控制的 canonical 请求继续以已解析账户主体为入参；支付工具入口只位于 application facade，例如 `authorizeByInstrument` 或等价入口，负责支付工具准入、绑定快照、资金责任解析、Spend Rule 决策和账户能力校验，再委派账户主体型内核。VCC 预付卡充值、系统内余额钱包充值、共享卡调额、VA 收款、全球账户付款和 ACH/银行转账事件不抽象为统一支付工具交易服务，必须先由 P2 业务能力包解释为归一资金事实，再映射到 P1/P0 直接交易、授权交易、余额控制、清结算、对账调账或归档能力。本条只记录 PRD、DSL、系分、TDD 和任务基线，不授权新增 Java 包、公共契约、测试代码、DDL/H2 schema 或运行配置。
- [x] 2026-06-01 完成本轮编码准入对齐：产品和架构口径一致，A1 直接交易事实红线当时是单一 Execution Grant 候选；B2 钱包 application facade / 资金责任目标字段 / BudgetGroup 兼容策略、B4 授权支付工具 application facade、B5/B6 预算控制和路由回放、B7/B8/P2 均不得作为本轮附带写入。该裁决已降级为历史记录；后续开工必须引用确认时 Git HEAD 或在 Execution Grant 的 `authorityBaseline` 中显式纳入允许的未提交文档变更；未授权前不写生产代码、测试代码、DDL/H2 schema 或运行时配置。
- [x] 2026-06-01 完成支付工具、资金账户和信用账户重定性后的 DSL、路由、账务、余额投影和交易投影 CR：DSL 主体只允许资金账户、信用账户和平台角色解析后的平台资金账户入账；支付工具、外部账户、预算组、Spend Rule 和交易投影只作为输入、快照、控制视图、查询维度或审计上下文。route 可以消费工具和规则快照，但 route leg、posting、LedgerEntry 和账本余额投影必须落到已解析可入账主体；交易投影只读，不反写 route、posting、entry 或 balance。该 CR 关闭设计口径歧义，不授权公共契约、测试资源、DDL/H2 schema 或生产代码写入。
- [x] 2026-06-01 完成支付工具与 Spend Rule 生产可用性 CR：支付工具资源管理和绑定历史可作为 B2 局部代码基线，但支付工具应用准入、资金责任目标主体、Spend Rule 规则定义/版本/决策日志/控制活动、预算控制投影和授权准入组合仍未达到生产可用闭环。当前结论为设计可用、生产条件不足；下一步只能准备 B2/B4/B5/B6/B8 Round 0 或单一 Execution Grant，不新增统一 `InstrumentTransactionService`，不替换 canonical 交易请求，不把预算组或 Spend Rule 写成资金交易事实或账本主体。
- [x] 2026-06-01 补齐 B2/B4 支付工具与 Spend Rule Round 0 准入卡：新增 `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md`，把 `R0-PI-001`、`R0-FR-001`、`R0-AUTH-001`、`R0-SR-001`、`R0-SR-002`、`R0-PI-002` 拆成 B2-PI-CAP、B2-FR、B4-AUTH-PI、B5-SR-CONTROL、B6/B8-PI-VIEW 五个候选切片。该卡只作为 Round 0 和独立 Execution Grant 输入，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。
- [x] 2026-06-02 固化 Highnote 参考模式对 wind-funds 的确认点：Highnote financial account / ledger / payment card / financial account activity 分层只作为外部设计参考，用于确认账户入账、工具归因、控制留痕、投影查询。wind-funds 不照搬对象名，不新增卡账本、支付工具账务主体或统一支付工具交易内核；共享卡或多卡共享账户通过同一内部责任主体、多工具绑定、控制快照和交易投影过滤区分卡账单，只有独立资金池、独立授信、独立账期、独立还款或独立资金责任成立时才创建独立资金账户或信用账户。本条只更新 PRD、DSL、系分、TDD 和 OpenSpec 设计基线，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。
- [x] 2026-06-02 完成 B4-TRX-EXPIRE 授权过期释放 canonical 能力：`b0666ba feat: 补齐授权过期释放 canonical 能力` 新增 `EXPIRE` 事件、`EXPIRED` 状态、`FundsAuthorizationTransactionExpireRequest`、`FundsAuthorizationTransactionService#expire`、授权过期转换、route replay、ledger posting 复用 release 路径和生命周期金额校验；`FundsAuthorizationTransactionFlowTests` 覆盖 `TDD-AUTH-011`、`TDD-ROUTE-008`、`TDD-RED-016`，`DefaultRouteReplayServiceTests` 覆盖 `AUTHORIZATION_EXPIRE_REPLAY`。已验证 `just test-one DefaultRouteReplayServiceTests tests`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-boundary`、`just compile`、`git diff --check`、`just pmd` 全部通过。该提交关闭 B4-03 授权过期基础缺口；强制完成、无授权直接退款、拒付承接、授权支付工具 facade、VCC 生命周期、Spend Rule 控制和并发竞争仍需独立 Execution Grant。
- [x] 2026-06-02 补齐 B4 授权后继能力 Round 0 准入卡：新增 `docs/TDD设计/B4-授权后继能力Round0准入卡.md`，把 B4-TRX-EXPIRE 后剩余的 `settle` 强制完成、`settleRefund` 无授权退款、拒付/争议承接和授权并发竞争拆成 B4-FORCE-SETTLE、B4-NO-AUTH-REFUND、B4-DISPUTE-CHARGEBACK、B4-AUTH-RACE 四个候选切片。该卡只作为账户主体型 canonical 授权内核的 Round 0 和独立 Execution Grant 输入，不授权生产代码、测试代码、DDL/H2 schema、支付工具 facade、VCC 生命周期、Spend Rule 控制、清结算对账或治理写入。
- [x] 2026-06-02 完成 B4-FORCE-SETTLE 只读覆盖扫描：现有普通 `settle` 请求强制携带 `authorizationTransactionSn`，converter 无条件构造 `AUTHORIZATION` reference 并查询原授权账本交易，route resolver 依赖原授权主体解析；`FundsAuthorizationTransactionFlowTests` 已覆盖普通完成、部分完成、完成后过期、完成幂等和拒付幂等，但没有 FORCE 模式、强制完成策略、上限、原因、凭证或无前置授权外部事实引用字段。扫描结果已回填 `docs/TDD设计/B4-授权后继能力Round0准入卡.md#71-existingcoveragescan2026-06-02`；该扫描当时只作为 B4-FORCE-SETTLE Execution Grant 输入，后续已由 `616dac1` 和 `3825466` 消费为实现和回归证据。
- [x] 2026-06-02 收敛 B4-FORCE-SETTLE 首轮候选契约字段：`docs/TDD设计/B4-授权后继能力Round0准入卡.md` 已补充 `forceSettleContractCandidate`，把首轮字段限定为 `settleMode`、`forceSettlePolicyCode`、`forceSettleLimitAmount`、`forceSettleReason`、`externalOriginalFactRef`、`forceSettleVoucherRef`、`operator/contextVariables` 或后续 Grant 明确确认的等价命名。该收敛当时只降低 B4-FORCE-SETTLE Execution Grant 歧义，后续已纳入 `616dac1` 和 `3825466` 的实现基线；未覆盖的策略引擎、规则表、processor 生命周期、支付工具 facade、VCC、拒付增强、清结算对账或治理写入仍需独立授权。无授权退款后续已由 `006bcaa` 独立闭合首轮 canonical 能力。
- [x] 2026-06-02 完成 B4-FORCE-SETTLE 编码前契约边界加固：PRD、DSL、系分、OpenSpec spec/design 和 `docs/TDD设计/B4-授权后继能力Round0准入卡.md` 已统一补强普通完成与 FORCE 完成的差异。普通完成继续依赖 `authorizationTransactionSn` 和原授权账本交易；首轮 FORCE 模式必须声明 `settleMode=FORCE` 或等价模式，且不得依赖内部原授权流水、不得构造 `AUTHORIZATION` reference 或查询原授权账本交易。强制完成策略和上限必须来自 Execution Grant 声明的内部白名单、审批结果或受信策略快照；审计最小集为 `WindOperator`、`forceSettleReason`、`externalOriginalFactRef`、`forceSettleVoucherRef` 和受信策略/审批快照引用，`contextVariables` 只能作为白名单补充。该加固当时为 docs-only 准入对齐，后续已被 B4-FORCE-SETTLE 代码基线消费；剩余扩展仍需单独授权。
- [x] 2026-06-02 完成 B4-FORCE-SETTLE 授权强制完成 canonical 能力：`616dac1 feat: 补齐授权强制完成能力` 新增 FORCE 完成请求字段、转换和路由分支，普通完成仍走原授权流水，FORCE 不构造 `AUTHORIZATION` reference、不查询原授权账本交易，路由从 `AVAILABLE` 直接进入 `SETTLEMENT`。目标测试覆盖强制完成成功、普通完成回归、幂等和失败无副作用；已验证 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile` 和 `git diff --check` 通过。
- [x] 2026-06-02 完成 B4-FORCE-SETTLE 策略红线加固：`3825466 fix: 收紧授权强制完成策略红线` 将 `forceSettlePolicyCode` 和 `forceSettleLimitAmount` 收敛到内部受信策略校验，补齐未知策略、上限不匹配、缺原因、缺外部事实、缺凭证、携带内部授权流水等失败路径无资金副作用断言；已验证 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd` 和 `git diff --check` 通过。B4-FORCE-SETTLE 首轮闭合后，B4-NO-AUTH-REFUND、B4-DISPUTE-CHARGEBACK 和 B4-AUTH-RACE 仍需独立 Execution Grant。
- [x] 2026-06-02 完成 B4-NO-AUTH-REFUND 只读覆盖扫描：现有 `settleRefund` 已覆盖已完成授权后的全额退款、争议类退款、超已完成金额失败无副作用和幂等摘要冲突；但 `FundsAuthorizationTransactionRefundRequest` 仍要求 `authorizationTransactionSn`，converter 无条件构造 `AUTHORIZATION` reference 并写入 `AUTHORIZATION_TRANSACTION_SN`，route resolver 和 route replay 依赖原授权/原完成路径。扫描结果已回填 `docs/TDD设计/B4-授权后继能力Round0准入卡.md#711-noauthrefundcoveragescan2026-06-02`；该扫描只作为后续 B4-NO-AUTH-REFUND Execution Grant 输入，不授权 Java 代码、测试代码、DDL/H2 schema、支付工具 facade、VCC、拒付增强、清结算对账或治理写入。
- [x] 2026-06-02 收敛 B4-NO-AUTH-REFUND 首轮候选契约字段：`docs/TDD设计/B4-授权后继能力Round0准入卡.md` 已补充 `noAuthRefundContractCandidate`。后续 CR 将资金层请求契约收缩为 `authorizationTransactionSn` 空值语义、`externalReferenceSn`、`refundReason`、`operator/contextVariables` 或等价命名，`NO_AUTH` 只作为资金指令内部上下文标签；普通授权链退款继续要求 `authorizationTransactionSn`，NO_AUTH 模式不得携带内部授权流水、不得查询原授权账本交易。该收敛只作为 B4-NO-AUTH-REFUND Execution Grant 输入，不授权 Java 代码、测试代码、DDL/H2 schema、支付工具 facade、VCC、force settle 返工、拒付增强、清结算对账或治理写入。
- [x] 2026-06-02 对齐 B4-NO-AUTH-REFUND 主文档口径：PRD、DSL、系分、TDD、B4 准入卡和 OpenSpec 已统一到 `authorizationTransactionSn` 空值判定、`NO_AUTH` 内部归类标签、`externalReferenceSn` 外部追溯引用、退款原因、操作者/审计、普通授权链退款兼容和无授权退款不得携带或查询内部授权流水；`TDD-RED-017A` 作为缺外部引用、缺原因、缺审计、携带内部授权流水和失败无副作用的红线入口。该对齐仍是 docs-only 准入，不授权代码、测试、DDL/H2 schema 或运行时配置。
- [x] 2026-06-02 完成 B4-NO-AUTH-REFUND GSD-CAD 准入复核：最初以 `e937395 docs: 对齐 B4 无授权退款主文档口径` 为已提交基线，后续 docs-only 索引、恢复入口和确认基线校准提交以用户确认 Execution Grant 时的 Git HEAD 自然纳入；当时 B4-NO-AUTH-REFUND 进入待确认态。GSD 层确认下一候选是单一 B4-NO-AUTH-REFUND 切片；CAD 层只有在用户确认 `Execution Grant：B4-NO-AUTH-REFUND` 后，才允许从 `B4-NAR-RED-001` 开始写目标 Red。该准备态后续已由 `006bcaa feat: 补齐无授权退款 canonical 能力` 消费并闭合。
- [x] 2026-06-02 补齐 B4-NO-AUTH-REFUND Grant 可执行包：`docs/TDD设计/B4-授权后继能力Round0准入卡.md#82-grantexecutionpackagecandidate2026-06-02` 已把下一轮候选收敛为 `B4-NAR-CAD-001` 原子任务包，当时仍为待确认态。该包明确首轮只写 `B4-NAR-RED-001`，必要时再补 `B4-NAR-RED-002`；允许范围限定为授权退款 flow 测试、`FundsAuthorizationTransactionRefundRequest` 显式列名兼容字段、transaction converter/command/lifecycle/route replay/request summary 最小修复；`tests/src/test/resources/jdbc-schema.sql`、ledger 公共契约、core 枚举状态、支付工具 facade、钱包 application facade、VCC、Spend Rule、chargeback case、清结算追偿、治理、生产配置、外部协议和敏感数据处理均保持禁止。该补强后续已由 `006bcaa` 消费为代码闭环；不再作为新的自动编码授权。
- [x] 2026-06-03 完成 B4-DISPUTE-CHARGEBACK 只读准入裁决：现有代码已有 `FundsAuthorizationTransactionService#chargeback`、`CHARGEBACK` eventType、route replay chargeback phase 和授权交易 flow 成功/超额失败/幂等冲突测试；但 PRD、DSL、系分和 B4 准入卡目标语义仍要求拒付/争议默认通过 `settleRefund` 携带原因、凭证、外部引用和审计上下文承接，不强制把独立 `chargeback` 服务入口作为目标态主入口。本轮裁决为 `SEMANTIC_DECISION_REQUIRED_NOT_CODE_AUTHORIZED`，只同步任务和准入状态，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置；已验证 `just test-one FundsAuthorizationTransactionFlowTests tests` 通过 24 tests。
- [x] 2026-06-03 补齐 B4-DISPUTE-SEMANTIC-ALIGNMENT Grant 候选包：`docs/TDD设计/B4-授权后继能力Round0准入卡.md#810-disputesemanticalignmentgrantcandidate2026-06-03` 当时把下一轮候选收敛为 `B4-DISPUTE-SEMANTIC-ALIGNMENT` 原子任务包；该候选后续已由 `949b24a fix(transaction): 对齐授权争议退款审计语义` 消费并闭合。该包默认以 `settleRefund` 为拒付/争议目标态主入口，既有 `chargeback` 入口只作为兼容、显式事件或内部适配资产；首批 Red 聚焦争议退款与普通授权链退款、NO_AUTH 退款、授权拒绝的查询、投影、审计和幂等摘要可区分性。本记录只作历史 provenance，不再作为新的自动编码授权。
- [x] 2026-06-03 对齐 B4-DISPUTE-SEMANTIC-ALIGNMENT 主文档入口：PRD、DSL、系分和 TDD 主文档已统一为 `settleRefund / AUTH_REFUND` 默认承接拒付/争议语义，独立 `chargeback` 只保留兼容、显式事件、内部适配或后续专项候选口径；同步新增 `TDD-RED-017B` 作为后续授权后的争议退款可区分性红线。本记录仍是文档准入闭环，不授权 Java、测试、DDL/H2 schema、公共契约或运行时配置写入。
- [x] 2026-06-11 完成授权争议 / chargeback 语义再裁决：dispute / chargeback 定性为案件过程，不是授权交易层默认资金结果。资金底座只消费裁决后的退款结果：用户胜诉或需退款时通过 `settleRefund / AUTH_REFUND` 沿原完成路径承接；用户败诉或无需资金处理时不得生成 route、posting、LedgerEntry、余额变化或新的交易事实。既有 `FundsAuthorizationTransactionService#chargeback` 只能视为历史兼容或内部适配资产；移除公共 API、迁移测试、调整 `CHARGEBACK` event 入口或建立完整 dispute case 必须另起 `B4-DISPUTE-OUTCOME-API-CLEANUP` 或等价新的单一 Execution Grant，不能混入账本 004A、VCC、清结算或其他切片。本轮仅同步 PRD/DSL/系分/TDD/OpenSpec 交付口径，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-03 刷新 B4 GSD-CAD 候选结构校验入口：`818da34 fix(transaction): 移除授权退款请求模式字段` 已把 no-auth refund 请求契约进一步收口为“请求无 `refundMode`、`NO_AUTH` 仅为内部上下文标签”，同轮 `just test-one FundsAuthorizationTransactionFlowTests tests` 通过 25 tests；B4 准入卡第 8.10 已补充标准 Harness `harnessScopeIndex`，用于让 CAD 候选结构校验识别写入范围、写入文件、只读范围和只读参考。本记录只刷新 docs/OpenSpec 任务入口，不授权 Java、测试、DDL/H2 schema、公共契约或运行时配置写入。
- [x] 2026-06-04 回写 B4-NO-AUTH-REFUND 路由回退基线：`967586c fix: 按外部引用推断无授权退款路由` 已补齐请求侧无 `refundMode` 后的 route resolver 行为，内部 `REFUND_MODE` 标签缺失时可从 `EXTERNAL_TRANSACTION` reference 推断 no-auth refund 路由，显式 `DISPUTE` 或其他退款归类不被覆盖；已验证 `just verify-slice AuthorizationFundsInstructionRouteResolverTests tests`、`just test-boundary`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just pmd` 和 `git diff --check` 通过。本记录只回写 OpenSpec/任务账本基线，不新增 Java、测试、DDL/H2 schema、公共契约或运行时配置授权。
- [x] 2026-06-12 建立 GSD-2 新基线工作流：以 `b3b9712 feat: 对齐资金底座GSD基线与交易回放能力` 作为 GSD-2 初始重置点，新增 `docs/TDD设计/GSD-2-新基线工作流规划.md`，并把旧 GSD + Goal 未完成计划从当前活跃执行队列移除。旧 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`、`QUEUED_AFTER_P0_P1`、`PARTIAL_*_NOT_DONE` 候选只作为 backlog reference 和历史准入材料；后续必须通过 GSD-2 重新选择 Task ID、Goal 映射、写入范围、验证命令和单一 Execution Grant。本记录只更新文档、索引和状态载体，不授权 Java、测试、公共契约、DDL/H2 schema、运行时配置或 Git 操作。
- [x] 2026-06-12 完成 `GSD2-W1-BASELINE-GAP-AUDIT` 低风险基线差距审计：当前实际 Git/code baseline 已校准为 `da7d2ea test: 阻断契约夹具承载资金流断言`，`b3b9712` 只保留为 GSD-2 初始重置点和历史证据；`a56bfbc`、`3c71e29`、`a3c2e21` 和 `4b63996..da7d2ea` 的账户层级、信用账户路由闭合、GSD2 路由快照和 DSL 契约门禁强化被登记为局部前置证据。新增 `docs/TDD设计/GSD-2-W1-基线差距审计.md`，状态为 `W1_GAP_AUDIT_DONE_READY_FOR_W2_SINGLE_GRANT_SELECTION`，下一步推荐进入 `GSD2-W2-SINGLE-GRANT-SELECTION`，优先评估 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`。本记录只更新文档、索引和 OpenSpec tasks，不授权 Java、测试、公共契约、DDL/H2 schema、运行时配置或 Git 操作。
- [x] 2026-06-12 完成 `GSD2-W2-SINGLE-GRANT-SELECTION` 草案并推进 W3A 只读定位：新增 `docs/TDD设计/GSD-2-W2-单一Grant选择卡.md`，把下一推荐候选收敛为 `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`；新增 `docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md`，确认账户层级 DSL / value object / route snapshot JSON / route replay 已有局部证据，并把首批 Red 从“新增账户层级契约”调整为真实服务流生成、生命周期保存、原快照回放和失败无副作用。当前状态为 `W3_READONLY_SOURCE_POSITIONING_DONE_WAITING_EXECUTION_GRANT`；未获用户确认前不进入 Red/Green/CAD Loop，不写 Java、测试、公共契约、DDL/H2 schema、运行时配置或 Git 操作。
- [x] 2026-06-12 完成 `GSD2-W4-B2-AH-EXECUTION-GRANT-PACK` 确认包：新增 `docs/TDD设计/GSD-2-W4-B2账户层级ExecutionGrant确认包.md`，提供可复制确认文本、授权范围、首个 Red `B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT`、验证命令和停止条件。该 Grant 已在 2026-06-15 被用户确认并消费到首个 Red，观察到授权服务流缺少合法账户层级来源；下一步改为确认 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 或等价来源契约 Grant。
- [x] 2026-06-15 完成 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 本地 Green：新增账户层级来源 port、wallet-face 账户层级服务契约、wallet-impl 账户关系持久化与快照解析、H2 schema、授权 route snapshot 接入和服务流目标测试，并按评审结论移除 `AccountHierarchyBinding` 与 `AccountHierarchySnapshot` 中无实质闭环的 `hierarchyVersion`、`changeReason`、`requestSn` 和 `description` 字段。`B2-AH-RED-001-SERVICE-FLOW-SNAPSHOT` 已转 Green，授权 route snapshot 能携带 `routingDecision.fundingAllocations[].accountHierarchySnapshot`，LedgerEntry 主体仍落在授权账户而非父账户。已验证 `just compile`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-one AuthorizationFundsInstructionRouteResolverTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests` 和 `just test-one RouteSnapshotJsonSupportTests tests` 通过；收口追加 `test-boundary`、`pmd` 和 `git diff --check`。下一候选转为 `GSD2-B2-FR-TARGET-001`，不得重复消费账户层级来源契约 Grant。
- [x] 2026-06-16 完成 `GSD2-B2-FR-TARGET-001` 首轮服务流 Green：资金责任关系从只依赖 `fundingAccountId` 扩展为 `targetSubjectType + targetSubjectId`，保留 `fundingAccountId` 作为资金账户目标兼容字段；同步 wallet Request/DTO/Query、wallet 实体与服务校验、H2 schema 目标主体列、目标主体唯一键和索引，并补 `SpendSubjectFundingRelationServiceImplTests` 覆盖旧资金账户兼容归一、信用账户目标创建与查询、不可用信用账户拒绝、预算组目标拒绝和失败无账务副作用。已验证 `just test-one SpendSubjectFundingRelationServiceImplTests tests` 通过；完整边界、compile、PMD 和 diff 收口仍需本轮继续执行。下一候选转为 `GSD2-B2-WALLET-APPLICATION-FACADE-001`，不得把本轮等同为支付工具准入、平台角色解析、完整 route snapshot 回放或 VCC facade 完成。
- [x] 2026-06-16 完成 `GSD2-B2-WALLET-APPLICATION-FACADE-001` 首轮资金责任解析 facade Green：新增 `FundingResponsibilityResolutionApplicationService`、`ResolveFundingResponsibilityRequest`、`FundingResponsibilityDecisionDTO` 和 wallet-impl application 实现，通过现有 `SpendSubjectFundingRelationService` 解析当前 ACTIVE 默认资金责任关系，输出 `targetSubjectType + targetSubjectId` 决策，且不写交易事实、route、LedgerEntry、余额投影或账务投影。新增 `FundingResponsibilityResolutionApplicationServiceTests` 覆盖资金账户目标、信用账户目标和缺失默认关系失败的无账务副作用。已验证 `just test-one FundingResponsibilityResolutionApplicationServiceTests tests` 通过；支付工具能力准入、钱包账户聚合、平台角色解析、完整预交易快照、VCC facade 和 route snapshot 回放仍为 Not Done。下一候选转为 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` 或另行确认 wallet facade 子切片。
- [x] 2026-06-16 完成 `GSD2-B2-WALLET-APPLICATION-FACADE-002` / `B2-PI-CAP-CAD-001` 首轮支付工具能力准入 facade Green：新增 `PaymentInstrumentCapabilityApplicationService`、`ResolvePaymentInstrumentCapabilityRequest`、`PaymentInstrumentCapabilityDecisionDTO`、`PaymentInstrumentAction` 和 wallet-impl application 实现，基于支付工具资源服务只读校验工具状态、方向、动作、币种、生效窗口、默认绑定、绑定角色和绑定版本，并输出不可变工具准入快照；不注册工具、不变更绑定、不写交易事实、route、LedgerEntry、余额投影或账务投影。新增 `PaymentInstrumentCapabilityApplicationServiceTests` 覆盖通过准入、方向不匹配拒绝和绑定版本失效拒绝，已验证 `just test-one PaymentInstrumentCapabilityApplicationServiceTests tests` 通过；账户能力、资金责任组合、授权 admission、Spend Rule、VCC facade、route snapshot 回放和完整预交易快照仍为 Not Done。
- [x] 2026-06-17 完成 GSD2 P0/P1 Ledger-Wallet-Transaction 完整门禁收口：当前 Git/code baseline 已推进至 `e81a8a25 feat: 完善账务钱包交易基线`，账户层级来源契约、资金责任目标主体、资金责任解析 application facade 和支付工具能力准入 application facade 已完成目标回归、边界测试、治理测试、账本、交易、余额控制、业务流和 PMD 完整门禁。已验证 `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad` 通过，`git diff --check` 通过。当前状态迁移为 `W5_P0P1_LWT_FULL_GATE_VERIFIED_NEXT_B4_TRANSACTION_PROJECTION_EXPLAIN`；下一候选为 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001`。本记录只回写状态基线，不授权 Java、测试、DDL/H2 schema、公共契约、Git、VCC、清结算或 P2 业务写入。
- [x] 2026-06-19 消费 `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001 / scopeDecision=object-scope-schema-backed`：差错事实新增 `blockingObjectType / blockingObjectSn`，差错 Request/DTO、阻断 DTO、Entity、H2 schema 和 Mapper 支持对象级阻断；gate 查询演进为对象精确命中并兼容历史类型级差错保守阻断。新增目标测试覆盖同类型不同对象不误阻断、结算对象精确阻断、复合阻断范围下对象类型精确匹配、对象级差错幂等、对象字段成对校验和查询无账务副作用。该记录当时只代表对象级 Gate 基座阶段，随后已由 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 消费并随 `0d3f68dc feat: 补齐清算结算对账准入消费` 提交固化。下一候选不得复用本 Grant，需在 B7 差异报告、wallet 完整预交易快照或其他单一 Grant 中重新确认。
- [x] 2026-06-19 补齐 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 确认包：新增 `docs/TDD设计/GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md`，把下一轮 B7 候选从泛化“清算 / 结算消费方”收敛为只读 gate consumer 服务切片。该包要求复用已完成的对象级 `ReconciliationGate` 基座，首批 Red 覆盖清算对象阻断、结算对象阻断、同类型不同对象不误阻断、处理后重跑对平条件放行和缺对象字段拒绝；写入范围限 reconciliation-face 契约、reconciliation-impl 只读 consumer、目标服务测试和状态文档，不授权完整清分、清算、结算、出款、补事实、生产迁移、Java 编码或 Git。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。
- [x] 2026-06-19 对清算 / 结算真实消费方确认包补做只读源码 Gap Audit：确认当前没有 `ClearingSettlementGateConsumerService` 或等价 consumer；`PayoutOrderServiceImpl#checkPayoutPreflight` 是真实消费 gate 样板，`PayoutPreflightServiceTests` 是无账务副作用断言样板；对象级 Gate 基座已经具备 `blockingObjectType / blockingObjectSn` 和 `CheckReconciliationGateRequest.gateObjectType / gateObjectSn`。本轮已把推荐测试类、首个 Red、候选 face/impl/request/dto 落点、准出条件和停止条件写入确认包第 12 节，并同步 LWT Goal；不授权 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git。
- [x] 2026-06-19 消费 `GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`：新增 `CheckClearingSettlementGateRequest`、`ClearingSettlementGateResultDTO`、`ClearingSettlementGateConsumerService`、`ClearingSettlementGateConsumerServiceImpl` 和 `ClearingSettlementGateConsumerServiceTests`，把清算 / 结算 gate consumer 从确认包落为只读服务流。目标测试覆盖 CLEARING 对象阻断、SETTLEMENT 对象阻断、同类型不同对象不误阻断、处理后重跑对平条件放行、非法或空请求拒绝，并断言无账本事实副作用。沙箱内目标 Spring 测试因 embedded Redis 本地端口限制失败，已在非沙箱环境重跑通过；`just test-one ClearingSettlementGateConsumerServiceTests tests` 5 tests 通过，`just test-reconciliation` 26 tests 通过，`just compile` 通过，`just pmd` 通过。当前状态已随 `0d3f68dc feat: 补齐清算结算对账准入消费` 固化；下一轮不得复用 B7 对象级 Gate 或 consumer Grant，需重新确认 B7 差异报告、wallet 完整预交易快照、账户能力来源组合或其他单一 Grant。
- [x] 2026-06-19 补齐 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 确认包：新增 `docs/TDD设计/GSD-2-B7-对账差异报告ExecutionGrant确认包.md`，把下一轮 B7 候选收敛为单笔只读差异报告查询能力，解释差错状态、阻断对象、处理动作、原始事实引用、重跑结果、gate 决策和证据引用。该记录是当时的准备态，随后已由本文件第 63 条消费并随 `a1397ddf feat: 补齐对账差异报告只读查询` 提交固化；当前不得复用该准备态作为待授权入口。
- [x] 2026-06-19 对 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 确认包补做只读源码落点审计：确认当前差错写服务位于 `application/difference`，gate 服务位于 `application/gate`，清算 / 结算 consumer 位于 `service` / `services/impl`；请求、DTO 和枚举落点分别是 `model/request`、`model/dto`、`reconciliation/enums`；`ReconciliationDifferenceMapper` 目前只有加锁差错查询和对象级 gate 查询，缺少报告所需的不加锁单笔查询。确认包已把候选接口落点校准为 `application/difference/report`，把查询入参改为 Request 风格，并补目标测试包路径、只读 mapper 缺口和首个 Red 组合方式。本记录只增强确认包可消费性，不授权 Java、测试、DDL/H2 schema、公共契约、Mapper 实现、运行时配置、Git 或生产发布。
- [x] 2026-06-19 为 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 确认包补齐 Grant 消费预检清单和运行卡：确认包第 10 / 11 节已明确授权文本、工作树、当前基线、首个 Red、写入范围、验证顺序、Git 策略，以及 Pick / Red / Green / Review / Verify / Handoff 的通过口径和停止条件。确认后默认从 `B7-REPORT-RED-001` 开始，只证明单笔报告解释对象级未闭环差错且无资金副作用。本记录只增强确认包可执行性；未确认 Grant 前仍不授权 Java、测试、DDL/H2 schema、公共契约、Mapper 实现、运行时配置、Git 或生产发布。
- [x] 2026-06-19 消费 `Execution Grant：GSD2-B7-RECON-DIFFERENCE-REPORT-001`：新增 reconciliation 差异报告 application 契约、查询 Request、报告 DTO、完整性枚举、只读 mapper 查询、report impl 和目标 Spring 服务流测试；`test-reconciliation` 已纳入报告测试。Red 先证明缺少报告契约 / DTO / 枚举 / 实现，Green 证明对象级未闭环差错可解释差错状态、阻断对象、处理动作、重跑结果、gate 决策、证据引用、报告视图开关、处理动作证据不完整、缺重跑结果和历史类型级差错缺 gate 决策，且查询不写 ledger / route / funds 事实。目标测试 5 tests、对账分组 31 tests、compile、PMD 和 `git diff --check` 通过。当前 Goal 只推进服务层能力，本 Grant 不新增 Controller、HTTP/RPC、页面、导出端点或外部适配入口。当前状态迁移为 `B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED`；本轮已提交到 `a1397ddf`。
- [x] 2026-06-19 消费 `GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`：新增 `FundsAccountCapabilityApplicationService`、`ResolveFundsAccountCapabilityRequest`、`FundsAccountCapabilityDecisionDTO`、wallet-impl 只读实现和账户能力来源解析器；`FundsAccount` 增加 `capabilitySource`，`DefaultFundsAccountQueryServiceImpl` 从 ledger profile 与账户 `contextVariables.fundsAccountCapabilities` 解析 Funding/Credit Account 能力。显式账户能力只能收窄 profile 安全能力，不得扩出信用账户等 profile 安全基线；账户状态继续决定当前是否可收款、付款或提现。新增 `FundsAccountCapabilityApplicationServiceTests` 覆盖资金账户显式只收款、信用账户越权扩收款拒绝、冻结资金账户状态门禁关闭和查询无账本事实副作用。沙箱内 wallet application 组合回归因 embedded Redis 本地端口限制失败，已在非沙箱环境复跑 12 tests 通过；`just compile`、`just pmd` 和 `git diff --check` 通过。当前状态迁移为 `ACCOUNT_CAPABILITY_SOURCE_GREEN_VERIFIED`；本 Grant 不新增 Controller、HTTP/RPC、完整预交易快照、Spend Rule、VCC facade、支付工具交易内核、DDL/H2 schema 或生产迁移。
- [x] 2026-06-19 消费 `GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`：新增 `PaymentInstrumentPreTransactionSnapshotApplicationService`、`ResolvePaymentInstrumentPreTransactionSnapshotRequest`、`PaymentInstrumentPreTransactionSnapshotDTO` 和 wallet-impl 只读聚合实现，组合支付工具能力、资金责任和账户能力三个 admission decision，输出最终资金账户或信用账户目标主体快照。新增 `PaymentInstrumentPreTransactionSnapshotApplicationServiceTests` 覆盖授权前成功聚合和工具方向拒绝，证明成功或失败均不创建资金交易、route、posting、LedgerEntry 或余额投影事实。沙箱内目标测试因 embedded Redis 本地端口限制失败，已在非沙箱环境复跑 2 tests 通过；wallet application 组合回归在非沙箱环境复跑 14 tests 通过，`just compile`、`just pmd` 和 `git diff --check` 通过。当前状态迁移为 `WALLET_PRE_TRANSACTION_SNAPSHOT_GREEN_VERIFIED`；本 Grant 不新增 Controller、HTTP/RPC、Spend Rule、VCC facade、支付工具交易内核、DDL/H2 schema 或生产迁移。
- [x] 2026-06-19 消费 `GSD2-B2-SPEND-CONTROL-ADMISSION-001`：新增 `SpendControlAdmissionApplicationService`、`ResolveSpendControlAdmissionRequest`、`SpendControlAdmissionDecisionDTO`、`SpendControlDecisionResult` 和 wallet-impl 只读实现，把外部 Spend Rule 决策证据与支付工具预交易快照组合为支出控制准入结论。Red 首次证明缺少支出控制准入服务、Request、DTO、枚举和实现；Green 覆盖规则通过、规则拒绝和缺少决策证据三类路径，证明成功和拒绝均不创建资金交易、route、posting、LedgerEntry、余额投影或账本事实。沙箱内目标 Spring 测试因 embedded Redis 本地端口限制失败，已在非沙箱环境复跑 `just test-one SpendControlAdmissionApplicationServiceTests tests` 3 tests 通过；收口追加 wallet application 组合回归、`compile`、`pmd` 和 `git diff --check`。当前状态迁移为 `SPEND_CONTROL_ADMISSION_GREEN_VERIFIED`；本 Grant 不新增 Spend Rule 规则定义、决策日志持久化、Spend Control Activity、预算控制投影、VCC facade、交易内核支付工具入参、Controller、HTTP/RPC、DDL/H2 schema 或生产迁移。
- [x] 2026-06-19 准备 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 确认包：新增 `docs/TDD设计/GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md`，把 wallet 下一步候选从泛化“完整 Spend Rule 控制活动 / 预算控制投影”收敛为服务层单一 Grant。确认包明确默认推荐 `schemaDecision=ddl-backed`，首个 Red 落在 `SpendControlActivityApplicationServiceTests`，目标是记录准入、拒绝、占用、释放等控制活动并派生只读预算控制投影，同时证明无资金交易、route、posting、LedgerEntry、余额投影或账本事实副作用。当时状态迁移为等待授权准备态；后续已在 2026-06-20 按 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001 / schemaDecision=ddl-backed` 进入首轮实现，当前状态以第 2026-06-20 执行记录和下方状态表为准。
- [x] 2026-06-19 对 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 确认包补做只读源码 Gap Audit：确认当前 wallet 仅有 `SpendControlAdmissionApplicationService`、准入 Request / DTO、只读 impl 和目标测试，能证明外部 Spend Rule 决策证据参与准入且无资金事实副作用；源码、H2 schema 和测试均未发现 `SpendControlActivityApplicationService`、`RecordSpendControlActivityRequest`、`SpendControlActivityDTO`、`BudgetControlProjectionDTO`、`t_spend_control_activity` 或 `t_budget_control_projection`。确认包已把首个 Red 收敛为复用既有准入结论记录控制活动，MVP 入参限定为活动流水、业务流水、目标账户、金额币种、规则证据、预算范围和活动摘要，并明确预算控制投影不得包含 ledger balance、available balance 或 frozen balance。本记录只增强确认包可消费性；未确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` 与 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-19 对齐 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 的 PRD、系分、DSL 和 TDD 目标口径：PRD 已明确 Spend Rule 控制活动和预算控制投影的产品验收；系分已补 `SpendControlActivityApplicationService` 职责、推荐包落点和 B5 最小目标态；DSL 已补 Spend Control Activity 控制事实字段、目标主体和反写资金事实红线；TDD 已补 `TDD-WALLET-021` 至 `TDD-WALLET-023`、`SR-CONTROL-002` 和 `SR-CONTROL-003`。本记录只完成设计对齐和测试意图固化；未确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` 与 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-19 为 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 补齐三卡交接和 Coding Loop Contract：确认包新增 Product Context Card、Engineering Handoff Card、Production Loop Card 和 Coding Loop Contract，明确产品目标、核心对象、业务不变量、验收种子、工程写入范围、首个 Red、验证命令、Maker / Checker、生产非目标、人工接管点、失败回写、最大轮次和提交切片。本记录只提升下一轮编码准入可消费性；未确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` 与 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-19 为 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 补齐 `schemaDecision` 决策矩阵：确认包已明确 `ddl-backed`、`contract-only`、`defer` 三种选择的适用判断、授权写入范围、可声明交付结果和禁止外推范围，并把推荐授权收敛为 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` + `schemaDecision：ddl-backed`。首轮 `ddl-backed` 最小存储边界限定为 `t_spend_control_activity`，预算控制投影由控制活动聚合派生，不单独新增 `t_budget_control_projection`。本记录只消除下一轮编码授权歧义；未确认 Execution Grant 和 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-19 对 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 补齐编码样板和首轮切片边界：确认包第 6.4 节已明确 face 契约、Request/Query/DTO、impl、Entity/Mapper、H2 schema 和目标服务流测试的推荐落点；`ddl-backed` 首轮只允许新增 `t_spend_control_activity`，预算控制投影由活动聚合派生；测试必须证明记录活动、拒绝活动、幂等重放、摘要冲突和预算投影查询均无资金交易、route、posting、LedgerEntry、ledger transaction 或余额投影副作用。本记录仍为 docs-only 交接增强；未确认 Execution Grant 和 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-19 复核重复确认的 `Execution Grant：GSD2-B7-RECON-DIFFERENCE-REPORT-001`：源码、目标测试、确认包、Git log 和本 tasks 已证明该 Grant 已完成 Red / Green / Verify，并随 `a1397ddf feat: 补齐对账差异报告只读查询` 固化。本轮只做状态账本回写，不重新授权 Java、测试、DDL/H2 schema、公共契约、Mapper、Controller、HTTP/RPC、运行时配置或 Git。若后续要扩批次报告、导出、运营后台、完整清结算、补事实或生产迁移，必须另起新的单一 Grant；当前默认下一候选仍为 `GSD2-B5-SR-CONTROL-ACTIVITY-001`，进入编码前需确认 `schemaDecision`。
- [x] 2026-06-19 对 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 做控制活动字段命名准入复核：源码确认既有 `ResolveSpendControlAdmissionRequest` 和 `SpendControlAdmissionDecisionDTO` 使用 `spendRuleId`、`spendRuleVersion`、`spendDecisionSn`、`spendDecisionDigest`；确认包已把 `RecordSpendControlActivityRequest` MVP 字段同步为相同命名，`ruleId`、`ruleVersion`、`decisionSn`、`decisionDigest` 仅作为产品概念简写，不作为代码字段建议。本记录只提升下一轮编码交接一致性；未确认 Execution Grant 和 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-19 为 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 补齐首轮编码 Runbook：确认包第 8.1 节已按 Pick / Red / Contract / Green / Extend / Review / Verify / Handoff 固化后续获得授权后的执行顺序；最小 Green 判定限定为记录通过和拒绝控制活动、按活动流水和业务维度查询、从 `RESERVED` / `RELEASED` 派生预算控制投影、`tenantId + activitySn` 幂等稳定且摘要冲突拒绝，并通过目标服务流证明无资金交易、route、posting、LedgerEntry、ledger transaction 或余额投影副作用。本记录仍为 docs-only 交接增强；未确认 Execution Grant 与 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-19 对 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 做 `ddl-backed` 可实现性审计：只读源码确认 wallet-face 已有 `application/spend` 和 model request/query/dto 包，wallet-impl 已有 `application/spend/impl`、DAL Entity / Mapper 样板和 H2 schema 落点；确认包第 6.3 节已记录首轮可沿用现有结构补 `t_spend_control_activity`、Entity、Mapper、服务实现和目标服务流测试，预算控制投影仍从活动聚合派生，不新增 `t_budget_control_projection`。本记录只增强后续 `schemaDecision=ddl-backed` 的可消费性；未确认 Execution Grant 与 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-20 对 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 做确认包可消费性复核：当前工作树只存在文档差异，未发现 Java、SQL、POM 或 Justfile 未提交变更；源码仍未发现 `SpendControlActivityApplicationService`、`RecordSpendControlActivityRequest`、`SpendControlActivityDTO`、`BudgetControlProjectionDTO`、`t_spend_control_activity` 或 `t_budget_control_projection`，既有支出控制准入 Request / DTO 字段口径仍与确认包一致。B5 确认包已通过 Harness、产品架构、系统架构结构检查和 `git diff --check`。本记录只证明确认包仍可消费；未确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` 与 `schemaDecision` 前，不授权 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移。
- [x] 2026-06-20 校准 `GSD-2-LWT-生产可用能力Goal.md` 第 2 节影响范围：将影响范围从上一轮支出控制准入实现口径改为当前真实的 B5 Spend Rule 控制活动确认包、PRD、DSL、系分、TDD、README 和 OpenSpec 状态同步口径。本记录仍为 docs-only 状态维护；未确认 `Execution Grant：GSD2-B5-SR-CONTROL-ACTIVITY-001` 与 `schemaDecision` 前，不授权 Java、测试、公共契约、DDL/H2 schema、Entity、Mapper、运行时配置、Git 或生产迁移。
- [x] 2026-06-20 校准 W5 LWT 推进计划的下一候选裁决：`GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已随 `a1397ddf feat: 补齐对账差异报告只读查询` 成为已消费证据，不再作为下一轮默认候选；当时默认确认项为 `GSD2-B5-SR-CONTROL-ACTIVITY-001`，现已随 `78f7f008 feat: 补齐支出控制活动与预算投影` 消费并提交。后续若扩新 Java、测试、公共契约、DDL/H2 schema、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移，必须另起新的单一 Grant。
- [x] 2026-06-20 执行 `GSD2-B5-SR-CONTROL-ACTIVITY-001 / schemaDecision=ddl-backed` 首轮实现并完成本地 Green / Verify / Commit：已新增 Spend Rule 控制活动 application 契约、Request / Query / DTO、活动类型枚举、wallet-impl Entity / Mapper / Service、H2 `t_spend_control_activity` 和目标服务流测试。目标测试覆盖准入活动、拒绝活动、幂等重放、摘要冲突、预算控制投影和查询侧拒绝非资金 / 信用账户目标主体，并证明无资金交易、route、posting、LedgerEntry、账本交易或余额投影副作用。已验证 `just test-one SpendControlActivityApplicationServiceTests tests` 6 tests 通过、`just test-one SpendControlAdmissionApplicationServiceTests tests` 3 tests 通过、`just compile` 通过、`just pmd` 通过、`git diff --check` 通过；代码与文档变更已提交到 `78f7f008 feat: 补齐支出控制活动与预算投影`，状态更新为 `SR_CONTROL_ACTIVITY_GREEN_VERIFIED_COMMITTED`。本记录只声明服务层最小能力完成，不外推 Spend Rule 规则引擎、决策日志持久化、交易消费、VCC facade、Controller、HTTP/RPC、生产迁移、运营 UI 或灰度监控已完成。
- [x] 2026-06-19 同步 B7 对账差异报告恢复入口：`docs/TDD设计/GSD-2-新基线工作流规划.md` 已从“无待消费确认包”修正为“B7 对账差异报告确认包已准备但未授权编码”；`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 已把本轮影响范围校准为 B7 报告 docs-only 准入材料，不再误写为 B5 route snapshot 审计摘要或清算 / 结算 gate 消费缺口。该轮仍为 docs-only，不授权 Java、测试、DDL/H2 schema、公共契约、Mapper 实现、运行时配置、Git 或生产发布。
- [x] 2026-06-19 复刷 B7 对象级 Gate 验证证据：沙箱内目标 Spring 测试因 embedded Redis 本地端口探测 `SocketException: Operation not permitted` 失败，已按项目约规在非沙箱环境复跑 `just test-one ReconciliationGateApplicationServiceTests,ReconciliationDifferenceApplicationServiceTests tests` 20 tests 通过，并复跑 `just test-reconciliation` 26 tests 通过。该记录只刷新验证证据和状态文档，不授权 Java、测试、DDL/H2 schema、公共契约、运行时配置、Git 或清算 / 结算真实 consumer 编码。
- [x] 2026-06-17 完成 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` 首轮交易投影解释 Green：新增 transaction-face 只读投影解释查询契约、最小查询条件、统一解释来源和 transaction-impl 持久化事实解释实现，基于资金交易、RouteSnapshot 和交易明细解释 posted pay、declined authorization 和 missing RouteSnapshot fail-fast，不写交易事实、route、LedgerEntry、余额投影或投影存储。新增 `FundsTransactionProjectionExplainApplicationServiceTests` 覆盖成功付款解释、授权拒绝解释和缺 RouteSnapshot 不重建投影；原有 `DefaultRoutedFundsInstructionOrchestratorProjectionTests` 回归通过，证明主写投影发布口径未被破坏。已验证 `just test-one FundsTransactionProjectionExplainApplicationServiceTests tests`、`just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests`、`just compile`、`just pmd` 和 `git diff --check` 通过；退款、no-auth refund、释放、拒付、projection store、治理重放和差异报告仍为 Not Done。下一候选转为 `GSD2-B5-BALANCE-ADJUST-AUDIT-001` 或 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`。
- [x] 2026-06-17 完成 `GSD2-B5-BALANCE-ADJUST-AUDIT-001` 首轮余额调账审计 Green：新增外部余额异常来源类型、balance adjust 请求一等审计字段、受控负可用策略字段和 instruction context key，转换器在 `EXTERNAL_BALANCE_ANOMALY` 场景强制校验来源流水、原因、外部终局事件、外部余额快照、对账差错引用和责任引用，并把审计事实透传到资金交易明细上下文。新增 `FundsBalanceAdjustAuditFlowTests` 覆盖外部余额异常纠偏可生成受控负可用余额、平台调账账务事实、交易明细审计上下文，以及缺必要审计证据时失败无资金事实和账务副作用。已验证 `just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-one FundsBalanceControlFailureFlowTests tests`、`just test-balance-control`、`just test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check` 通过；B7 对账差错闭环、独立审计表、运营审批流、route snapshot 审计回链和泛化运营补账当时仍为 Not Done；当时建议候选为 `GSD2-B7-RECON-DIFFERENCE-MVP-001` 或 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`，当前 B7-001 已由后一条记录消费。
- [x] 2026-06-17 完成 `GSD2-B7-RECON-DIFFERENCE-MVP-001` 首轮对账差错闭环 Green：新增 reconciliation-face 差错 application service、create/link/rerun 请求、差错 DTO、差错类型、严重等级、状态、来源质量和匹配强度枚举；新增 reconciliation-impl `t_reconciliation_difference` Entity、Mapper 和服务实现，H2 schema 增加差错表并纳入测试 Mapper 扫描。服务支持按 `tenantId + differenceSn` 创建幂等、差错默认阻断、处理动作或调账结果回链、重新对账结果登记和关闭前必须已有处理动作引用；同差错流水、同处理动作或同重跑流水的事实漂移必须拒绝。新增 `ReconciliationDifferenceApplicationServiceTests` 覆盖差错登记阻断且无账务副作用、处理回链与重跑关闭幂等、创建幂等冲突拒绝、无处理动作直接关闭拒绝和重跑幂等冲突拒绝。已验证 `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd` 和 `git diff --check` 通过；目标测试在沙箱内因 embedded Redis 本地端口绑定限制失败，已按权限规则在非沙箱环境重跑通过。完整清分、清算、结算、出款、追偿、运营后台、账龄升级、清算/出款阻断消费、生产迁移脚本、对账任务运行记录、差异报告、补事实白名单、独立运营审批和泛化运营补账仍为 Not Done。下一候选转为 `GSD2-B7-RECON-DIFFERENCE-MVP-002` 或 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`。
- [x] 2026-06-17 完成 `GSD2-B7-RECON-DIFFERENCE-MVP-002` 首轮对账差错处理动作守卫 Green：新增 `ReconciliationDifferenceActionType`，把补事实、冲正、调账、挂账、追偿和核销收敛为受控处理动作类型；`LinkReconciliationDifferenceAdjustmentRequest`、`ReconciliationDifferenceDTO`、`ReconciliationDifference` 和 H2 `t_reconciliation_difference` 增加 `actionType`、处理动作幂等键和原始事实引用。服务在处理回链时强制校验动作类型、幂等键和原始事实引用，重复回链时若动作、幂等键或原始事实引用漂移必须拒绝。新增 `ReconciliationDifferenceApplicationServiceTests` 覆盖缺少动作上下文失败、处理回链保存动作上下文和动作上下文幂等冲突拒绝。已验证 `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd` 和 `git diff --check` 通过；目标 Spring 测试在沙箱内受 embedded Redis 本地端口限制，已按权限规则在非沙箱环境重跑。完整补事实命令执行服务、交易层/账本层委派、运营审批流、职责分离、清算/结算/出款消费、生产迁移脚本、账龄升级和完整差异报告仍为 Not Done。下一候选转为清算/结算/出款准入消费差错状态和阻断范围，或 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`。
- [x] 2026-06-18 完成 `GSD2-B7-RECON-GATE-CONSUME-001` 首轮对账准入消费 Green：新增 reconciliation-face `ReconciliationGateApplicationService`、`CheckReconciliationGateRequest`、`ReconciliationGateDecisionDTO`、`ReconciliationGateBlockingDifferenceDTO`、`ReconciliationGateDecisionStatus` 和 `ReconciliationGateObjectType`，reconciliation-impl 基于 `t_reconciliation_difference` 只读查询阻断范围并输出清算、结算或出款准入决策。服务在未闭环差错、处理后重跑未对平或动作上下文漂移被拒绝时返回 `BLOCKED`，在已处理且重跑对平时返回 `CONDITIONALLY_PASSED`，不创建清算候选、确认清算批次、锁定结算单、提交出款、写交易事实、route、posting、LedgerEntry、余额投影或交易投影。新增 `ReconciliationGateApplicationServiceTests` 覆盖未解决差错阻断、已处理但重跑未对平继续阻断、重跑对平条件放行和动作上下文漂移拒绝后仍阻断；`test-reconciliation` 分组已纳入 Gate 测试。已验证 `just test-one ReconciliationGateApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check` 通过；目标 Spring 测试在沙箱内受 embedded Redis 本地端口限制，已按权限规则在非沙箱环境重跑。完整清分、清算、结算、出款、追偿、运营后台、生产迁移脚本、差异报告、补事实命令执行服务、运营审批流和外部规则确认仍为 Not Done。下一候选转为 `GSD2-B7-RECON-GATE-CONSUME-002`，把准入服务接入具体清算、结算或出款消费方；或转为 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`。
- [x] 2026-06-18 完成 `GSD2-B7-RECON-GATE-CONSUME-002` 首轮出款准入消费 Green：`PayoutOrderService#checkPayoutPreflight` 已接入 `ReconciliationGateApplicationService`，对 `PAYOUT` 阻断范围内的未闭环差错返回 `RECONCILIATION_BLOCKED`，对已处理且重跑对平的差错条件放行并保留差错、处理动作和重跑证据引用；创建前无 `payoutSn` 时以 `settlementSn` 作为 gate 消费对象流水。新增 `PayoutPreflightServiceTests` 覆盖 PAYOUT 差错阻断、条件放行和失败无账务事实副作用。已验证 `just test-one PayoutPreflightServiceTests tests` 和 `just test-reconciliation` 通过；目标 Spring 测试在沙箱内受 embedded Redis 本地端口限制，已按权限规则在非沙箱环境重跑。完整出款生命周期、外部回单、金额不一致处理、清算/结算消费方接入、运营审批流、生产迁移脚本、差异报告和补事实执行服务仍为 Not Done。下一候选转为清算/结算消费方接入、B7 差异报告，或 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`。
- [x] 2026-06-18 建立 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 生产可用能力 Goal：新增 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`，把 ledger、wallet、transaction 和 reconciliation 当前能力拆成 Ready / Conditional Ready / Not Done / Blocker，并把当前 Git/code baseline 校准为 `a38776c5 feat: 接入出款准入对账门禁`。同步更新 GSD-2 工作流、P0/P1 LWT 推进计划、TDD README 和 docs README，把 `GSD2-B7-RECON-GATE-CONSUME-002` 从下一候选调整为已消费证据；下一候选收敛为 `GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001`、`GSD2-B2-WALLET-APPLICATION-FACADE-002`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002`、`GSD2-LD-LEDGER-GUARD-REGRESSION-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 或 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`。本记录只更新文档、索引和任务状态，不授权 Java、测试、DDL/H2 schema、公共契约、Git、VCC、清结算生产闭环或 P2 业务写入。
- [x] 2026-06-18 补强 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 生产可用基线判定矩阵：在 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 第 5.5 节新增 `CONDITIONAL_DELIVERABLE_BASELINE / PARTIAL_BASELINE / NOT_DELIVERABLE_AS_FULL_PRODUCTION_DONE` 判定口径，明确当前 ledger、wallet、transaction 和 reconciliation 可作为上层 MVP 后续单一 Grant 的被依赖能力继续消费，但不得外推为三模块或 B7 清结算全量生产 Done。同步回写 `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` 的可消费证据和不可外推范围；本记录只更新文档和任务状态，不授权 Java、测试、DDL/H2 schema、公共契约、Git、生产迁移、灰度、告警、VCC、全球账户、收单或清结算生产闭环。
- [x] 2026-06-18 补强 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` Completion Audit 与三卡交接：在 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 第 11 节新增 Goal 要求逐项审计和 Product Context Card / Engineering Handoff Card / Production Loop Card 交接口径。该记录当时裁决为状态载体、完备性矩阵、条件基线和下一 Grant 确认包已完成 docs-only 收口；后续 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 已由 `AUTH-COMPAT-005` 消费并完成 Green 验证，三模块全量生产可用仍为 `NOT_DONE`，Goal 必须保持 active。同步回写 `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`；本记录只更新文档和任务状态，不授权 Java、测试、DDL/H2 schema、公共契约、Git、生产发布、真实资金或上线审批。
- [x] 2026-06-18 补强 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` Evidence Anchor Matrix：在 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 第 11.3 节把账户层级/资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade、交易投影解释、余额调账审计、对账差错、对账 gate、出款 preflight 和 Ledger 护栏映射到当前仓库 face / impl / test 锚点和已记录验证命令。同步回写 `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`；本记录只提供恢复和准入消费证据索引，不授权 Java、测试、DDL/H2 schema、公共契约、Git，也不能替代后续目标测试、compile、PMD、diff 和 Not Done 回写。
- [x] 2026-06-18 补强 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 单一 Grant 决策账本：在 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 第 8.1 节把默认推荐 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、B4 投影解释扩展、wallet facade 补强、ledger guard 回归、B7 清算/结算 gate 消费和 B5 审计扩展的进入条件、切换条件、停止条件和状态回写位置固化为可交接账本。同步回写 `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`；本记录只消除下一轮授权歧义，不授权 Java、测试、DDL/H2 schema、公共契约、Git、生产迁移、真实资金、外部规则或专业合规确认。
- [x] 2026-06-18 校准 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 活跃入口结构门禁命令：将 LWT Goal、P0/P1 LWT 推进计划、AUTH Chargeback 目标语义任务卡和 AUTH Chargeback 兼容入口确认包中的裸 `check_*` 命令改为可复跑的 Skill 绝对路径。已验证 `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py` 的 `gsd-wave` / `cad-candidate`、`python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture` 和 `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan` 对上述四个活跃入口均通过；本记录只修复文档验证可复现性，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金或生产发布。
- [x] 2026-06-18 回写 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 结构门禁验证证据账本：在 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 第 11.4 节记录 LWT Goal、P0/P1 LWT 推进计划、AUTH Chargeback 目标语义任务卡和 AUTH Chargeback 兼容入口确认包的 Harness、产品和架构结构 checker 通过结果，并同步 `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` 的 Structure Gates 行。该证据只证明活跃文档入口可消费，不替代 Java 编译、目标测试、分组测试、PMD、Git 授权、生产迁移、灰度、告警或上线准出。
- [x] 2026-06-18 将 GSD-2 总入口纳入 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 结构门禁证据：把 `docs/TDD设计/GSD-2-新基线工作流规划.md` 纳入当前 LWT Goal 结构门禁，并在 LWT Goal 第 11.4 节补充 GSD-2 总恢复入口的 Harness、产品和架构结构 checker 通过记录；同步 W5 Structure Gates 行。本记录只完善恢复入口和结构证据，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 补齐 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` Grant 消费预检清单：在 `docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 11 节新增用户授权、工作树状态、当前基线、首个 Red 收窄、写入范围、验证顺序、状态回写和 Git 策略预检；同步 LWT Goal Completion Audit 与 W5 推进计划。该预检只在用户复制确认后帮助资深架构师开工前收窄首个 Red，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金或生产发布。
- [x] 2026-06-18 收敛 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 活跃状态卫生：将 W5 Coding Loop Contract、GSD-2 总入口残余风险和 OpenSpec 活跃工作流表中的旧 B7 / B2-AH 活跃口径先校准为 AUTH 确认包可消费状态；该状态后续已由 `AUTH-COMPAT-005` 推进为 `AUTH_CHARGEBACK_COMPAT_ADAPTER_GREEN_VERIFIED_SUMMARY_ONLY`。本记录只修正文档状态一致性，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金或生产发布。
- [x] 2026-06-18 补齐 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` Goal 层级恢复映射：在 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 第 1.1 节明确父 Goal、LWT 子 Goal、当前可执行状态和历史证据状态的关系，并同步 W5 推进计划和 GSD-2 总入口恢复规则。该记录当时以 AUTH 确认包可消费状态作为当前状态；后续已推进为 `AUTH_CHARGEBACK_COMPAT_ADAPTER_GREEN_VERIFIED_SUMMARY_ONLY`，历史 B7 / B2 状态只作为 Evidence Anchor，不作为默认授权。本记录只改善交接可消费性，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金或生产发布。
- [x] 2026-06-18 校准 README 恢复导航口径：将 `docs/TDD设计/README.md` 中 W2/W3/W4 描述改为历史记录和已消费证据，将 `docs/README.md` 的 GSD + Goal 一页导航从旧 `da7d2ea` / B2 下一候选口径校准为当前 `a38776c5` baseline、LWT Goal 主恢复入口和 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 待确认状态。本记录只改善入口可读性和恢复路径，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金或生产发布。
- [x] 2026-06-18 补齐 `GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` contract/design-only 任务卡：新增 `docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md`，把 dispute / chargeback 明确定性为案件过程而不是默认独立资金结果；目标态主入口为 `settleRefund` 携带争议字段承接有资金影响的裁决结果，用户败诉或无资金影响时不得生成 route、posting、LedgerEntry、余额变化或新的资金交易事实；现有 `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest`、`CHARGEBACK` event / replay 分支和 `declinedAmount` 只作为历史兼容、显式事件或内部适配资产，后续是否 deprecated、adapter 或 guard 必须另起 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 或等价单一 Execution Grant。同步更新 LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README 和 docs README；本记录只更新文档、索引和任务状态，不授权 Java、测试、DDL/H2 schema、公共契约、Git、完整 dispute case、清结算追偿或 P2 业务写入。
- [x] 2026-06-18 补齐 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` Execution Grant 确认包：新增 `docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md`，推荐 `COMPAT_GUARD_NO_BEHAVIOR_BREAK`，把下一步限定为现有 `chargeback` 入口的兼容说明、最小 guard、兼容测试和状态回写；不删除 `FundsAuthorizationTransactionService#chargeback`，不破坏公共契约，不新增 DDL/H2 schema，不引入完整 dispute case，不把 `chargeback` 整体迁移为 `settleRefund` 委派实现。同步更新 LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README 和 docs README；本记录只更新文档、索引和任务状态，不授权 Java、测试、DDL/H2 schema、公共契约、Git、真实资金、完整争议运营、清结算追偿或 P2 业务写入。
- [x] 2026-06-18 补齐 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` README 恢复导航验证锚点：在 `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 第 9、10、11.1 节新增 README 旧口径扫描命令、handoff 验证命令和 Completion Audit 行，要求后续改 README 或入口导航时复跑扫描，避免把历史 baseline、历史账户层级候选或历史授权状态误当作当前可执行状态。已验证 LWT Goal 的 Harness、产品、架构结构 checker 通过，`git diff --check` 通过，README 旧口径扫描无残留匹配。本记录只更新文档验证锚点和任务状态，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 收敛 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 活跃状态命名：将 GSD-2 总入口、W5 P0/P1 LWT 推进计划和 OpenSpec active table 的当前状态统一到 AUTH 确认包可消费口径，并把“活跃未完成计划”收窄为“活跃未完成编码计划”；该状态后续已由 `AUTH-COMPAT-005` 消费并推进为 `AUTH_CHARGEBACK_COMPAT_ADAPTER_GREEN_VERIFIED_SUMMARY_ONLY`。已验证 GSD-2 总入口和 LWT Goal 的 Harness、产品、架构结构 checker 通过，README 旧口径扫描和旧状态扫描无残留匹配，`git diff --check` 通过。本记录只更新文档状态命名、恢复入口和任务账本，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 补齐 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` Grant 消费运行卡：在 `docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 12 节新增 Red 选择、Red 范围、Green 实现、Review、Verify、Handoff 和最小断言清单，明确确认后优先从兼容入口边界、授权拒绝不误计、缺原事实 fail-fast 三类 Red 中选择最小失败样例，并要求回写 LWT Goal、W5 和 OpenSpec tasks。同步 LWT Goal Completion Audit。本记录只更新确认包可消费性和任务状态，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 对齐 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` Grant 消费恢复入口：将 W5 P0/P1 LWT 推进计划、LWT Goal 验证矩阵、GSD-2 总入口、TDD README 和 docs README 统一到“确认包 + 第 11 节预检清单 + 第 12 节运行卡”口径。下一轮用户确认 Grant 后，资深架构师应先消费预检清单和运行卡，再选择 `AUTH-CB-COMPAT-RED-001/002/003` 的最小失败样例；未确认前仍只允许 docs-only 状态维护。本记录只更新文档恢复路径和验证锚点，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 收敛 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 三卡 handoff 口径：将 LWT Goal 当前任务、Completion Audit、Engineering Handoff Card、三卡裁决、W5 交接要求和 GSD-2 总入口统一为“确认包 + 第 11 节预检清单 + 第 12 节运行卡”可消费状态。下一轮若用户复制 `Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`，架构师先消费预检和运行卡再写最小 Red；未确认前仍不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 补齐 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` Loop Progress Ledger：在 LWT Goal 第 1.2 节登记 `LWT-GOAL-001`、`AUTH-HANDOFF-002` 和 `LWT-PROGRESS-LEDGER-003`，明确 docs-only 轮次必须产生新增证据、状态变化或缺口收敛；连续两轮没有新增进展时停止扩写并等待用户确认单一 Grant 或重新选择候选。同步 GSD-2 Agent Loop 契约和 TDD README。本记录只更新状态载体和无进展检测口径，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 回写 `GSD2-LWT-PRODUCTION-CAPABILITY-GOAL-001` 首轮无新 Grant 状态：当前运行时 Goal 仍 active，工作树仍为 docs-only 范围，未获得新的单一 Execution Grant，也没有新的事实差异、验证证据或状态缺口收敛；LWT Goal 第 1.2 节新增 `LWT-NO-GRANT-004`，将无进展计数记为 1。下一轮若仍未确认 Grant 且没有新事实，应停止 docs-only 扩写并等待用户选择单一 Grant 或重新排序候选。本记录只更新停止条件证据，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出。
- [x] 2026-06-18 完成 LWT 角色协作 Loop Round 0 基线校准：重新加载 AI Native、产品架构专家和资深架构师规约后，复核当前 Git、docs、OpenSpec 和 wallet / transaction 代码锚点，确认真实 HEAD 已推进到 `ca603eab feat: 补齐交易投影解释剩余矩阵`，`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 和 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING` 均为已消费证据；wallet 侧已有资金责任解析 facade 和支付工具能力准入 facade，但当时仍缺 `AuthorizationAdmissionApplicationService` 或等价授权准入入口。同步回写 LWT Goal、W5 推进计划、GSD-2 总入口和 docs README，把当时恢复候选收敛为 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`：在 wallet application 层组合支付工具准入、绑定快照、资金责任、账户能力和账户主体型授权内核委派；准入失败必须无 route、posting、LedgerEntry、projection 或敏感上下文副作用，`approved=false` 授权拒绝按标准拒绝交易事实处理。本记录只更新状态载体、恢复入口和候选草案，不授权 Java、测试、DDL/H2 schema、公共契约、Git、联网、真实资金、生产迁移或上线准出；该候选已由下一条任务消费，当前不再作为下一 Grant。
- [x] 2026-06-18 消费 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`：新增 `AuthorizationAdmissionApplicationService`、`AuthorizeByPaymentInstrumentRequest`、wallet-impl 最小实现和 `AuthorizationAdmissionApplicationServiceTests`。本切片证明支付工具授权入口先完成工具动作准入、绑定主体校验、资金责任解析、账户租户/币种/能力校验，再构造账户主体型 `FundsAuthorizationTransactionAuthorizeRequest` 委派 `FundsAuthorizationTransactionService#authorize`；准入失败不生成资金交易、route、posting、LedgerEntry 或余额投影，`approved=false` 授权拒绝只生成标准拒绝交易事实，route legs 为空且无 posting、LedgerEntry 或余额影响。已验证目标测试 3 tests、wallet application 组合回归 9 tests、授权交易回归 32 tests、`just compile`、`just pmd` 和 `git diff --check` 均通过。当时下一候选收敛为 `GSD2-LD-LEDGER-GUARD-REGRESSION-001`，该候选已由后续 ledger guard 切片消费；不得沿用本 Grant 扩 VCC facade、Spend Rule 策略引擎、完整预交易快照或统一支付工具交易内核。
- [x] 2026-06-19 完成 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 恢复核验：本轮按运行时请求优先复核 B2 授权准入，源码和任务账本均确认该 Grant 已消费，不是当前默认编码切片。沙箱内目标测试出现 Spring ApplicationContext 环境失败后，按项目约规在沙箱外重跑 `just test-one AuthorizationAdmissionApplicationServiceTests tests`，3 tests 通过；随后重跑 `just test-one AuthorizationAdmissionApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,RouteSnapshotJsonSupportTests,FundsAuthorizationTransactionFlowTests tests`，40 tests 通过。结论：支付工具授权准入 application 服务流、工具能力、route snapshot 回链和账户主体型授权内核仍为健康被依赖基线；本轮不新增 Java、测试、DDL/H2 schema、公共契约或 Git 授权。后续账户能力来源、预交易快照和支出控制准入均已消费；若继续 wallet，必须在完整 Spend Rule 控制活动 / 预算控制投影或 VCC facade 中重新确认单一 Execution Grant。
- [x] 2026-06-18 完成 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 只读准入：确认 core/route 已支持 `paymentInstrumentRef` 承载、序列化和回放，授权 route resolver 已透传 `instruction.instrumentRef`；当前缺口是 wallet 授权准入到 `FundsAuthorizationInstructionConverter` 之间尚未构造 `PaymentInstrumentRefSpec`，因此下一 Red 应断言支付工具授权批准路径 persisted route snapshot 的 `paymentInstrumentRef` 缺失。后续 Green 只允许非破坏性新增可选支付工具快照承载、converter 透传和 wallet 准入快照构造；不替换账户主体型 canonical 入参，不改 DDL/H2 schema，不混入 VCC、Spend Rule 或统一支付工具交易内核。已验证 `check_harness_plan.py --kind cad-candidate`、LWT Goal 的 `gsd-wave` / 产品 / 架构结构检查、`rg` 一致性检索和 `git diff --check` 通过；本轮未运行编译和业务测试，原因是只更新 docs-only 准入包。
- [x] 2026-06-18 消费 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`：新增 `FundsAuthorizationTransactionAuthorizeRequest.paymentInstrumentRef` 可选快照承载，`FundsAuthorizationInstructionConverter` 透传到 `ImmutableFundsInstructionSpec.instrumentRef`，`AuthorizationAdmissionApplicationServiceImpl` 基于支付工具能力准入结果构造敏感字段安全的 `PaymentInstrumentRefSpec` 和绑定快照；`AuthorizationAdmissionApplicationServiceTests` 断言支付工具授权批准路径 persisted route snapshot 顶层 `paymentInstrumentRef` 能回链工具号、工具类型、币种、状态、绑定号、绑定版本、绑定角色、内部主体、准入动作和准入决策。交易内核仍以账户主体 `FundsAccountId` 为 canonical 入参，支付工具只作为 route snapshot 和审计回链，不作为 route leg、posting、LedgerEntry 或余额投影主体。已验证目标测试 3 tests、组合回归 40 tests、`just compile`、`just pmd`、`git diff --check` 和结构检查均通过。本 Grant 已消费；当时下一候选切换为 `GSD2-LD-LEDGER-GUARD-REGRESSION-001`，该候选已由后续 ledger guard 切片消费。
- [x] 2026-06-18 消费 `GSD2-LD-LEDGER-GUARD-REGRESSION-001`：新增 `LedgerNormalBalanceGuard`，并接入 `LedgerServiceImpl`、`DefaultLedgerTransactionPostingServiceImpl` 和 `LedgerBalanceProjectionServiceImpl`，要求固定账目类别的 `normalBalanceSide` 与类别默认方向一致；`ASSET / EXPENSE / COST` 等借方类账本不得被历史脏数据或错误入参改成贷方正常余额，`LIABILITY / EQUITY / INCOME / REVENUE` 等贷方类账本不得改成借方正常余额。目标测试覆盖创建非法账本、历史异常账本入账和余额投影三条路径，均证明失败发生在持久化、posting、LedgerEntry、余额投影和事件发布前；已验证目标 ledger 测试 21 tests、ledger 分组 28 tests、`just compile` 和 `just pmd` 通过。本 Grant 不授权 DDL/H2 schema、ledger profile 重构、治理重放、清结算补事实或 GSD1 大包回滚；下一候选默认切换为 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`，若优先清结算验收则从清算/结算 gate 消费或 B7 差异报告中重新确认单一 Grant。
- [x] 2026-06-19 消费 `GSD2-B5-BALANCE-ADJUST-AUDIT-002` route snapshot 审计回链子切片：`BalanceControlFundsInstructionRouteResolver` 已把余额调账安全审计摘要写入 persisted route snapshot 顶层 contextVariables，覆盖来源类型、来源流水、原因、调账证据、审批、外部终局事件、外部余额快照、对账差错、重新对账、责任引用和受控负可用策略；敏感 `EXTERNAL_ACCOUNT_REF` 和任意 request context 不进入 route snapshot。`FundsBalanceAdjustAuditFlowTests` 新增 route snapshot 断言，Red 阶段证明 route snapshot 缺审计上下文，Green 后目标测试通过；本 Grant 不授权独立审计表、运营审批流、泛化运营补账、绕过 B7 差错、DDL/H2 schema 或公共契约变更。下一候选默认切换为 `GSD2-B5-BALANCE-ADJUST-AUDIT-003`，若优先清结算验收则从清算/结算 gate 消费或 B7 差异报告中重新确认单一 Grant。
- [x] 2026-06-19 建立 `GSD2-B5-BALANCE-ADJUST-AUDIT-003` 确认包：新增 `docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md`，把下一轮首个子切片收敛为余额调账独立审计查询最小服务流，查询只读聚合交易事实、交易明细上下文、route snapshot、ledger transaction 和 LedgerEntry，不创建或修复资金事实；同时明确不选择运营审批闭环，不授权 DDL/H2 schema、Entity、Mapper、公共契约之外的写入、运营后台、补事实执行、泛化运营补账、真实资金或 Git。该确认包已补齐 Product Context Card、Engineering Handoff Card、Production Loop Card、源码锚点、候选落点、首个 Red 设计提示、可复制确认文本、Grant 消费预检清单和 Grant 消费运行卡。本记录只更新确认包、索引和状态账本；用户确认 `Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-003` 后才可进入 Red / Green。
- [x] 2026-06-19 复核 `GSD2-B5-BALANCE-ADJUST-AUDIT-003` 源码锚点：确认候选契约应落在 `transaction-face` application service、`model/query` 和 `model/dto`，实现优先落在 `transaction-impl/application/impl`；ledger 查询锚点校准为 `ledger/ledger-face/src/main/java/com/wind/funds/ledger/service/LedgerTransactionService.java`，只允许调用查询方法，不得调用 posting、更新、删除、replay 或补事实入口。`FundsBalanceAdjustAuditFlowTests` 的外部余额异常纠偏请求构造当前为私有方法，首个 Red 需在同类测试或抽取测试支撑之间收窄。本记录仍是 docs-only 准入证据，不授权 Java、测试、DDL/H2 schema、公共契约或 Git。
- [x] 2026-06-19 消费 `GSD2-B5-BALANCE-ADJUST-AUDIT-003`：新增 `FundsBalanceAdjustmentAuditApplicationService`、`FundsBalanceAdjustmentAuditQuery`、`FundsBalanceAdjustmentAuditDTO`、`FundsBalanceAdjustmentAuditCompleteness` 和 `DefaultFundsBalanceAdjustmentAuditApplicationService`，补齐余额调账独立审计查询最小服务流；查询以资金交易主事实为定位轴，只读聚合交易明细上下文、route snapshot、ledger transaction 和 LedgerEntry，返回完整性状态、账本事实摘要、entry 摘要和安全审计上下文，不调用 posting、更新、删除、replay、生命周期 saver 或补事实入口。`FundsBalanceAdjustAuditFlowTests` 证明可按业务流水和交易流水查询外部余额异常纠偏调账审计视图，且查询前后余额、账本交易和分录数量不变，不泄露外部账户敏感上下文；同一测试已覆盖交易事实和 route snapshot 存在但账本事实缺失时返回 `INCOMPLETE_LEDGER`，不得误判为未找到或补写账本事实；`FundsBenefitSpecValidators` 同步允许 contextVariables 中精确 Money value object `{amount,currency}`，同时继续阻断顶层核心权益金额字段。已验证 `just test-one FundsBalanceAdjustAuditFlowTests tests` 非 sandbox 通过，sandbox 内同命令因 embedded Redis 端口限制失败并已复跑确认；`just test-one DefaultRouteReplayServiceTests tests`、`just test-one LedgerDtoContextVariablesContractTests tests`、`just test-balance-control`、`just test-boundary`、`just test-transaction`、`just compile` 和 `just pmd` 通过；本 Grant 未授权 DDL/H2 schema、Entity、Mapper、运营审批流、独立审计表、泛化运营补账、补事实执行、B7 差错创建、生产权限模型或 Git 提交。下一轮不得复用 B5-003，需重新确认 B7 清算/结算 gate、B7 差异报告、wallet 完整预交易快照或其他单一 Grant。
- [x] 2026-06-19 建立 `GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 确认包：新增 `docs/TDD设计/GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md`，把 B7 清算/结算 gate 进入前决策收敛为 `scopeDecision=type-scope-no-schema` 和 `scopeDecision=object-scope-schema-backed` 两个选项。只读审计确认当前 `gateObjectSn` 已存在于请求和决策解释，出款 preflight 也会传入消费对象流水，但差错命中仍按 `tenantId + blockingScope` 类型级查询；若选择对象级阻断，需要显式授权公共契约、DDL/H2 schema、Entity、Mapper、兼容口径和 no-false-block Red。本记录只更新确认包、索引和状态账本，不授权 Java、测试、DDL/H2、公共契约、Entity、Mapper、运行时配置或 Git。
- [x] 2026-06-03 完成 B4-DISPUTE-SEMANTIC-ALIGNMENT 首轮 CAD 闭环：`949b24a fix(transaction): 对齐授权争议退款审计语义` 已让 `settleRefund / AUTH_REFUND` 通过 `disputeMode`、`disputeReason`、`disputeVoucherRef`、`externalDisputeRef` 一等字段承接争议/拒付语义，请求侧仍不恢复 `refundMode`，`DISPUTE` 只作为资金指令内部上下文标签；route replay 只在 `AUTH_REFUND + DISPUTE` 场景传播争议审计上下文，避免普通退款、NO_AUTH 退款和 fee refund 被请求上下文污染。已验证 `just compile`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just pmd` 和 `git diff --check` 通过。本闭环只关闭 `B4-CB-RED-001A / TDD-RED-017B` 首个 canonical 可区分性切片，不声明完整 dispute case、独立 chargeback 一等 API、清结算追偿、VCC processor、DDL/H2 schema、core 枚举状态或 ledger 公共契约完成。
- [x] 2026-06-03 完成 B4-AUTH-RACE GSD-CAD Round 0：`docs/TDD设计/B4-授权后继能力Round0准入卡.md#813-authraceround0scan2026-06-03` 已记录授权后续事件并发竞争的只读扫描，现有顺序金额闭合、幂等摘要和失败无副作用覆盖充分，但当时未发现同一授权 settle / reversal / expire / settleRefund 并发竞争专用 Red；`#814-authracegrantcandidate2026-06-03` 已把候选收敛为 `B4-AUTH-RACE` 原子任务包，历史状态为 `ROUND0_READY_NOT_CODE_AUTHORIZED`。该准备态后续已由 `47c5269 fix(transaction): 串行化授权后继并发竞争` 消费并闭合，不再作为新的自动编码授权。
- [x] 2026-06-03 完成 B4-AUTH-RACE 首轮 CAD 闭环：`47c5269 fix(transaction): 串行化授权后继并发竞争` 已把 `B4-RACE-RED-001` 转为回归基线，新增同一授权 settle / expire / reversal 并发竞争测试，证明只有一个合法金额迁移获胜，失败方不生成 route、posting、ledger entry、projection 或余额副作用；实现层对授权后继命令增加事务完成前持有的 JVM 锁，并在读取授权原交易时使用 `FOR UPDATE` 行锁，同时保留完成、撤销和过期金额上限校验。已验证 `git diff --check`、`just compile`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary` 和 `just pmd` 通过。本闭环不声明 DDL/H2 schema、公共契约、core 枚举状态、ledger 公共契约、支付工具 facade、钱包 application facade、VCC、Spend Rule、完整 dispute/chargeback case、清结算对账或治理完成。
- [x] 2026-06-03 完成 B4-AUTH-PI GSD-CAD Round 0 只读准入：本轮把下一候选收敛为 `B4-AUTH-INSTRUMENT-APPLICATION` / `B4-AUTH-PI` 授权支付工具应用入口；代码扫描确认 `FundsAuthorizationTransactionService#authorize` 和 `FundsAuthorizationTransactionAuthorizeRequest.accountId` 仍是账户主体型 canonical 内核，`wallet-face` 只有 `PaymentInstrumentService`、`SpendSubjectFundingRelationService` 等资源服务，未发现 `AuthorizationAdmissionApplicationService` 或 `authorizeByInstrument` 生产入口。当前状态为 `ROUND0_READY_NOT_CODE_AUTHORIZED`，后续若确认新的单一 Execution Grant，首批 Red 为 `R0-AUTH-001`；未确认前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-03 补齐 B4-AUTH-PI Grant 可执行包：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#82-authinstrumentgrantcandidate2026-06-03` 已把下一轮候选收敛为 `B4-AUTH-PI-CAD-001` 原子任务包；该包明确首轮只写授权 application facade 目标 Red，Red 证明缺口后才允许 wallet-face application facade 契约、Request/DTO、wallet-impl 最小实现、委派适配和必要授权 flow 回归。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B4-AUTH-PI` 前仍不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 回写 B4-AUTH-PI 恢复入口确认基线：`88d80c7 docs: 收敛授权后继索引基线` 已把已闭合 B4 授权后继能力和下一候选索引串入 TDD / OpenSpec 入口；本轮同步把 `B4-AUTH-PI-CAD-001` 的 authority baseline 上调为确认时 Git HEAD 且至少包含 `88d80c7` 及本次基线校准提交。当前状态仍为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B4-AUTH-PI` 前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 完成 B4-AUTH-PI CAD 候选结构门禁复核：资深架构师 Harness checker 以 `cad-candidate` 模式检查 `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md` 和本 Harness tasks，结果均为 `OK harness plan check: kind=cad-candidate`。该复核只证明任务包字段结构完整，不等于用户已确认 Execution Grant、编码授权、测试通过或生产审批。
- [x] 2026-06-04 同步 B4-AUTH-PI 索引入口：`be3df9f docs: 同步授权工具候选索引` 和 `c58431e docs: 同步 TDD 授权工具候选索引` 已把 `B4-AUTH-PI-CAD-001` 的只读 Round 0、Grant 候选包和 `cad-candidate` 结构门禁证据同步到根 docs 与 TDD 索引。当前状态仍为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；该索引同步不新增编码授权，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 收敛 B4-AUTH-PI 确认基线提交集合：`dc88ae2 docs: 收敛授权工具候选确认基线` 已把 `B4-AUTH-PI-CAD-001` 的 authority baseline 从“后续基线校准提交”收敛为 `88d80c7`、`7b49684`、`be3df9f`、`c58431e`、`226dfc2` 和确认时 Git HEAD 的可审计集合。当前状态仍为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；该收敛只服务恢复和确认，不新增编码授权，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 补齐 B2-PI-CAP Round 0 与 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#83-b2-pi-cap-round-0-扫描2026-06-04` 和 `#84-paymentinstrumentcapabilitygrantcandidate2026-06-04` 已把下一基础候选收敛为 `B2-PI-CAP-CAD-001` 原子任务包。代码扫描确认 `PaymentInstrumentService` 仍是资源服务，当前未发现 `PaymentInstrumentCapabilityApplicationService` 或等价 application facade；首批 Red 为 `R0-PI-001`。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B2-PI-CAP` 前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-06 补齐 B2-ACCOUNT-HIERARCHY Round 0 与 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#80-b2-account-hierarchy-round-0-扫描2026-06-06` 和 `#801-accounthierarchygrantcandidate2026-06-06` 已把资金账户 / 信用账户父子结构、VCC 关联资金/信用子账户、父账户默认只读聚合、`PostingRole` 和禁止恢复 `VCC_ACCOUNT` 收敛为 `B2-ACCOUNT-HIERARCHY-CAD-001` 原子任务包。代码扫描确认当前只有 `PREPAID_CARD`、`SHARED_CARD`、`CREDIT_CARD` 等账户类型枚举和 `funding_account_id` 资金责任关系局部基线，未形成 `AccountHierarchySnapshot`、`PostingRole`、父账户/根账户快照或层级版本公共承载。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B2-ACCOUNT-HIERARCHY`、implementationDecision、postingRoleDecision、父账户/根账户快照策略和 DDL/H2 范围前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-06 完成账户层级跨 PRD/DSL/系分准入链对齐：PRD `06-VCC` 已把 VCC 关联子账户准入更新为必须具备父账户、根账户、层级版本、资金责任来源和 `B2-ACCOUNT-HIERARCHY`；DSL 已明确 `AccountHierarchySnapshot`、`PostingRole`、父账户 / 根账户快照和回放断言进入 fixture、Spec 或公共字段变更前必须先确认账户层级 Grant；系分已把 Round 0 顺序调整为账户层级先于支付工具能力准入、资金责任解析、Spend Rule 和授权准入组合。本条只更新交付设计和任务基线，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。
- [x] 2026-06-04 补齐 B2-FR funding-account-only Round 0 与 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#85-b2-fr-round-0-扫描2026-06-04` 和 `#86-fundingresponsibilitygrantcandidate2026-06-04` 已把资金责任解析低风险切片收敛为 `B2-FR-FAO-CAD-001` 原子任务包。代码扫描确认 `SpendSubjectFundingRelationService` 仍是资源服务，`CreateSpendSubjectFundingRelationRequest`、DTO、Query、Entity 和 H2 schema 仍以 `fundingAccountId` / `funding_account_id` 为主要目标字段；本候选只允许 `funding-account-only`，目标主体迁移必须另起 `B2-FR-TARGET`。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B2-FR-FAO` 前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 补齐 B2-FR-TARGET 资金责任目标主体迁移 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#861-fundingresponsibilitytargetgrantcandidate2026-06-07` 已把 VCC、信用账户和平台责任来源所需的 `targetSubjectType + targetSubjectId` 策略收敛为 `B2-FR-TARGET-CAD-001` 原子任务包。该候选依赖账户层级和子账户快照策略，明确 `fundingAccountId` 只能作为兼容读取、派生字段或历史查询字段，不能继续作为唯一写入事实并声明 VCC 子账户、信用账户或平台角色责任生产可用。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B2-FR-TARGET`、schemaGate、写入范围、首批 Red 和验证命令前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 补齐 B5-SR-CONTROL Round 0 与 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#87-b5-sr-control-round-0-扫描2026-06-04` 和 `#88-spendrulecontrolgrantcandidate2026-06-04` 已把 Spend Rule 控制切片收敛为 `B5-SR-CONTROL-CAD-001` 原子任务包。代码扫描确认当前只有 BudgetGroup、`BUDGET_GROUP` ledger profile、余额控制调账和预算组余额查询等兼容路径，未发现 `SpendRuleDecisionLog`、`SpendControlActivity`、Spend Rule application facade 或预算控制投影生产模型；本候选必须在 `contract-only` 与 `ddl-backed` 中二选一。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B5-SR-CONTROL` 和 schemaDecision 前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 补齐 B6/B8-PI-VIEW Round 0 与 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#89-b6b8-pi-view-round-0-扫描2026-06-04` 和 `#810-paymentinstrumentviewgrantcandidate2026-06-04` 已把支付工具解释视图和治理重放切片收敛为 `B6-B8-PI-VIEW-CAD-001` 原子任务包。代码扫描确认交易正常投影发布、route replay 和交易投影治理重放已有局部边界基线，但未形成支付工具维度流水 query DTO、预算控制视图、规则命中时间线或完整 B8 Manifest/余额快照/指标水位闭环；本候选必须在 `query-contract-only` 与 `projection-store-backed` 中二选一。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：B6-B8-PI-VIEW` 和 schemaDecision 前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 补齐 P2-VCC-PREPAID Round 0 与 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#811-p2-vcc-prepaid-round-0-扫描2026-06-04` 和 `#812-vccprepaidfundinggrantcandidate2026-06-04` 已把 VCC 预付资金确认切片收敛为 `P2-VCC-PREPAID-CAD-001` P2 业务专项候选包。代码扫描确认当前只有支付工具资源服务、资金责任关系资源服务、账户主体型直接交易/授权交易、route replay 和交易投影局部基线，未发现 `VccPrepaidFundingApplicationService`、VCC prepaid funding 请求模型、资金子账户/父账户快照契约或 VCC 专项资金流实现；本候选必须在 `contract-only` 与 `funding-flow-backed` 中二选一，并显式确认资金子账户、父账户快照、背后资金来源和外部规则核验状态。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：P2-VCC-PREPAID`、implementationDecision、责任主体策略和外部规则核验状态前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 补齐 P2-VCC-LIFECYCLE Round 0 与 Grant 候选：`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#813-p2-vcc-lifecycle-round-0-扫描2026-06-04` 和 `#814-vcclifecyclegrantcandidate2026-06-04` 已把共享卡和预付卡授权后生命周期回放切片收敛为 `P2-VCC-LIFECYCLE-CAD-001` P2 业务专项候选包。代码扫描确认交易层已有账户主体型授权生命周期、route replay 和交易投影局部能力，但未发现 `InstrumentTransactionLifecycleApplicationService`、`VccSharedCardTransactionApplicationService` 或 VCC lifecycle request model；本候选必须在 `contract-only` 与 `canonical-lifecycle-backed` 中二选一，默认 `original-snapshot-required` 和 `settleRefund-dispute-semantic`。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：P2-VCC-LIFECYCLE`、implementationDecision、snapshotDecision、disputeDecision、外部规则核验状态和 DDL/H2 范围前不写 Java、测试、公共契约、表结构或运行时配置。
- [x] 2026-06-04 补齐 P2-GA-INBOUND Round 0 与 Grant 候选：`docs/TDD设计/P2-业务能力包Round0准入卡.md#4-p2-ga-inbound-round-0-扫描2026-06-04` 和 `#5-p2-ga-inbound-grant-候选2026-06-04` 已把全球账户入金匹配与外部受理在途切片收敛为 `P2-GA-INBOUND-CAD-001` P2 业务专项候选包。代码扫描确认当前只有 `ExternalAccountRefSpec`、外部账户敏感值校验、FX 端口和出款前准入局部基线，未形成 `GlobalAccountInboundApplicationService`、全球账户入金 Request/DTO、VA/银行流水匹配 facade、外部非终态不入账 Red 或入金幂等摘要；本候选必须在 `contract-only` 与 `canonical-funds-backed` 中二选一，默认 `funding-account-only` 和 `same-currency-only`。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：P2-GA-INBOUND`、implementationDecision、账户解析决策、FX 决策、外部规则核验状态和 DDL/H2 范围前不写 Java、测试、公共契约、表结构或运行时配置。
- [x] 2026-06-04 补齐 P2-GA-OUTBOUND Round 0 与 Grant 候选：`docs/TDD设计/P2-业务能力包Round0准入卡.md#10-p2-ga-outbound-round-0-扫描2026-06-04` 和 `#11-p2-ga-outbound-grant-候选2026-06-04` 已把全球账户出款在途、成功回单和退汇边界切片收敛为 `P2-GA-OUTBOUND-CAD-001` P2 业务专项候选包。代码扫描确认当前已有 `PayoutOrderService#checkPayoutPreflight`、preflight Request/DTO 和 `PayoutPreflightServiceTests` 局部候选基线，但未形成 `GlobalAccountOutboundApplicationService`、全球账户出款 Request/DTO、外部非终态不误展示、成功回单终态、退汇关联原出金和费用责任处理目标 Red；本候选必须在 `preflight-contract-only` 与 `canonical-transit-backed` 中二选一，默认 `reuse-reconciliation-preflight-candidate`、`no-ledger-in-transit`、`return-as-difference-only` 和 `same-currency-only`。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：P2-GA-OUTBOUND`、implementationDecision、preflightDecision、transitDecision、returnDecision、FX 决策、外部规则核验状态和 DDL/H2 范围前不写 Java、测试、公共契约、表结构或运行时配置。
- [x] 2026-06-04 补齐 P2-GA-FX-FEE Round 0 与 Grant 候选：`docs/TDD设计/P2-业务能力包Round0准入卡.md#12-p2-ga-fx-fee-round-0-扫描2026-06-04` 和 `#13-p2-ga-fx-fee-grant-候选2026-06-04` 已把全球账户 FX quote 引用、费用分离和错币种阻断切片收敛为 `P2-GA-FX-FEE-CAD-001` P2 业务专项候选包。代码扫描确认当前已有 `FxService` / `DefaultFxServiceImpl`、`FundsInstruction.originalAmount`、`FundsInstruction.exchangeRate` 和直接交易手续费/退费局部能力，但未形成 `GlobalAccountFxFeeApplicationService`、全球账户 FX quote approval snapshot、费用组件归因、FX P&L 专业确认、错币种无 quote 阻断、费用净额混淆阻断或不执行 FX 的目标 Red；本候选必须在 `contract-only` 与 `attribution-backed` 中二选一，默认 `no-fx-execution`、`fee-attribution-only` 和 `external-rules-incomplete-blocking`。当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认 `Execution Grant：P2-GA-FX-FEE`、implementationDecision、fxExecutionDecision、feeDecision、externalRuleDecision、专业确认状态和 DDL/H2 范围前不写 Java、测试、公共契约、表结构或运行时配置。
- [x] 2026-06-04 收单 P2-ACQ-CAPTURE 降级为设计-only：`docs/TDD设计/P2-业务能力包Round0准入卡.md#14-p2-acq-capture-round-0-扫描2026-06-04` 只保留收单 capture 归一、商户 `CLEARING`、敏感数据边界、外部规则和 PCI 边界设计复核；此前实现候选不再作为当前可确认编码 Grant。当前状态为 `DESIGN_ONLY_NOT_CODE_CANDIDATE`；不得确认或消费 P2-ACQ-CAPTURE 实现授权，不得写 Java、测试、公共契约、DDL/H2 schema、运行时配置或收单 facade 实现，除非用户后续明确重新打开收单实现优先级。
- [x] 2026-06-04 重排任务优先级：用户确认当前任务优先级按模块或能力排列为账本账目 > 钱包 > 交易层，支付工具、VCC、全球账户支持放到最后，收单能力仅做设计不做实现。后续 GSD-CAD 自动模式必须按该队列选择恢复入口；若越级选择支付工具、VCC 或全球账户，Grant 必须显式说明业务原因和 P0/P1 依赖已满足；收单只能做 design-only 文档、边界复核和差距登记。
- [x] 2026-06-04 完成 GSD-1 账本账目 Round 0 准入卡：`docs/TDD设计/GSD-1-账本账目Round0准入卡.md` 已收敛账本账目产品语义、系统落点、源码入口、既有测试资产、Red 候选、写入范围、禁止范围、验证命令和停止条件。当时候选 `GSD1-LEDGER-FACTS-CAD-001` 状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；001A 获用户确认前不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。
- [x] 2026-06-04 收敛 GSD-1 首批 Red 推荐入口：只读扫描确认 `DefaultLedgerTransactionPostingServiceImpl` 缺少独立目标测试类，`LedgerTransactionServiceImplTests` 主要保护持久层上下文红线，`LedgerBalanceProjectionServiceImplTests` 已覆盖事件非事实源和整批失败无半截投影。首轮推荐 `GSD1-LD-RED-001A`，优先补 `DefaultLedgerTransactionPostingServiceImplTests`，证明非法账本交易、非法 posting plan 或非法 entry 在持久化和余额投影前被拒绝；该结论仍为准入建议，不授权编码。
- [x] 2026-06-04 进入 GSD Wave 1 编排：`docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` 已形成 `GSD1-LD-RED-001A` 原子任务包、验证矩阵、Execution Grant 草案、恢复入口和停止条件。当时状态为 `WAVE_1_READY_TO_CONFIRM` / `CANDIDATE_BLOCKED_BY_EXECUTION_GRANT`；001A 授权前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-04 补齐 GSD-1 状态账本：`docs/TDD设计/GSD-1-账本账目状态账本.md` 已记录当时状态、source of truth、决策日志、写入/只读范围、验证矩阵、handoff 和 Execution Grant 阻塞点。当时恢复入口为状态账本第 2 节和 Wave1 执行计划第 5 节；001A 授权前仍只允许 GSD 文档和索引同步。
- [x] 2026-06-04 补齐 GSD-1 代码库理解结论包：`docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` 已把 `DefaultRoutedFundsInstructionOrchestrator`、`DefaultLedgerPostingAssembler`、`DefaultLedgerTransactionPostingServiceImpl`、`LedgerTransactionServiceImpl`、`LedgerBalanceProjectionServiceImpl` 和既有测试资产接入同一恢复入口，确认 `GSD1-LD-RED-001A` 的首批 Red 应证明非法账本交易、非法 posting plan 或非法 entry 在持久化和余额投影前失败。当时状态仍为 `CANDIDATE_BLOCKED_BY_EXECUTION_GRANT`；该理解包不授权 Java、测试、DDL/H2 schema、公共契约或运行时配置写入。
- [x] 2026-06-04 完成 GSD-1 轻量验证回写：`docs/TDD设计/GSD-1-账本账目状态账本.md#10-verificationevidence2026-06-04` 已记录 001A 授权前验证证据。已通过 `check_harness_plan.py --kind lightweight`（代码库理解结论包、状态账本）、`check_harness_plan.py --kind gsd-wave`（Wave1 执行计划）、`check_harness_plan.py --kind cad-candidate`（Wave1 执行计划）、`git diff --check` 和 GSD/OpenSpec 相关 Markdown 行尾空白扫描。由于当时仅修改文档且尚未进入 001A 授权消费，未运行 Maven 编译、测试或 PMD。
- [x] 2026-06-04 补齐 GSD-1 Execution Grant 确认卡：`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` 已把 `Execution Grant：GSD1-LEDGER-FACTS` 收敛为可评审授权文本，包含允许执行、允许写入、条件写入、只读参考、禁止写入、验证命令、`summary_only` Git 策略、停止条件、撤销方式、首轮执行顺序、验收种子、人工确认点和 handoff。该确认卡仍不是授权本身；未获用户明确确认前，不写 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git 提交。
- [x] 2026-06-04 补齐 `GSD1-LD-RED-001A` 测试驱动设计口径：`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md#7-testdesignbeforegrant` 已明确被保护业务事实、服务层流程测试层级、真实链路、替身边界、前置数据、核心断言、断言禁区、命名建议和最小首例。授权后首轮 Red 应优先继承 `AbstractFundsServiceTest`，使用真实 H2 schema、真实 Mapper 和 `FundsBalanceAssertionSupport.ledgerFactSnapshot` / `assertLedgerTransactionFactsUnchanged` 证明无半截账务事实和无余额投影副作用；该测试设计口径不授权测试代码写入。
- [x] 2026-06-04 同步 GSD-1 AI Native 一页式准入包：`docs/TDD设计/GSD-1-账本账目状态账本.md#91-ainativeadmissionpackage` 已把 GSD/CAD 编排准入结论、输入成熟度、Atomic Task、CAD 候选缺口、Execution Grant 缺口、质量门禁、下一步 owner、停止条件和路由固化为恢复入口。当时结论为 GSD 可继续、CAD 候选可保持、CAD 执行缺用户确认授权；001A 授权前不写 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git 提交。
- [x] 2026-06-04 标记 GSD-1 授权前材料饱和：`docs/TDD设计/GSD-1-账本账目状态账本.md#92-pregrantsaturation` 已记录 Round 0、Wave 1、代码库理解、Execution Grant 确认卡、OpenSpec/Harness/TDD 索引均已齐备。当时状态为 `PRE_GRANT_SATURATED_BLOCKED_BY_EXECUTION_GRANT`；后续在 001A 授权前，只允许修正事实错误、同步索引漂移、补充验证证据或响应用户新增约束，不再新增新的 GSD 计划、候选卡、测试设计表或代码库扫描包。
- [x] 2026-06-04 完成 `GSD1-LD-RED-001A` 账本入账编排覆盖补齐：用户确认并消费 `Execution Grant：GSD1-LEDGER-FACTS` 后，当前工作树新增 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`，覆盖非 `POSTED`、posting plan 缺 entry、entry 金额为空/非正、plan/entry 流水不一致、entry 缺 `ledgerId` 和账本绑定不一致等失败无半截账务事实场景。该轮未修改生产代码；已验证 `just mvn-version`、`just compile`、`just test-one DefaultLedgerTransactionPostingServiceImplTests tests` 通过，其中目标测试 6 tests / 0 failures / 0 errors。沙箱内目标测试曾因 embedded Redis 本地端口绑定受限失败，提升权限后通过，判定为工具/沙箱限制而非代码失败。2026-06-07 工作树审计确认该目标测试文件当前未被 Git 跟踪，因此该记录只作为当前工作树证据和后续编辑基底。
- [x] 2026-06-04 迁移 GSD-1 下一候选到 `GSD1-LD-RED-001B`：`docs/TDD设计/GSD-1-账本账目状态账本.md`、`docs/TDD设计/GSD-1-账本账目Wave1执行计划.md`、`docs/TDD设计/GSD-1-账本账目代码库理解结论包.md`、`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md`、`docs/TDD设计/README.md` 和 `openspec/project.md` 已同步为 001A 当前工作树 Done、001B `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。下一 Grant 候选为 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY`，只允许在 `DefaultLedgerTransactionPostingServiceImplTests` 中补同一 ledger transaction 重复 post 后账务事实和余额投影不重复的目标 Red；Red 证明真实缺口后才允许 `DefaultLedgerTransactionPostingServiceImpl` 最小修复。未确认前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-05 完成 `GSD1-LD-RED-001B` 账本重复入账幂等覆盖补齐：用户确认并消费 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY` 后，已在当前工作树 `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java` 中新增同一 `LedgerTransactionSpec` 连续 post 两次的服务层流程测试，断言第一次新增 ledger transaction、posting plan、ledger entry 和余额变化，第二次账务事实快照和 ledger balance 均保持不变。该轮未修改生产代码；已验证 `just mvn-version`、`just compile`、`just test-one DefaultLedgerTransactionPostingServiceImplTests tests`、`git diff --check` 通过，其中目标测试 7 tests / 0 failures / 0 errors。当前状态迁移为 `WAVE_1_001B_DONE_READY_FOR_LEDGER_NEXT_DECISION`；2026-06-07 审计确认目标测试文件当前未被 Git 跟踪，后续不得沿用 001B 授权继续写代码，也不得把该文件写成已冻结 Git 基线。
- [x] 2026-06-07 完成 GSD + Goal 运行时授权前验证回写：`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#32-runtimeevidence2026-06-07`、`docs/TDD设计/GSD-1-账本账目状态账本.md#14-verificationevidence2026-06-07-runtimegoalpregrantvalidation`、`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md#11-nextgrantcandidate2026-06-05` 和 `docs/TDD设计/GSD-1-账本账目Wave1执行计划.md#8-handoff` 已同步运行时证据。已验证 `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version`、`WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` 和 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests`；目标测试在沙箱内因 embedded Redis 本机端口绑定受限失败，提权复跑后当前工作树 7 tests / 0 failures / 0 errors。该证据只证明 001A/001B 既有覆盖仍可运行，不授权 002A，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置；目标测试文件当前未被 Git 跟踪，不构成冻结 Git 基线。
- [x] 2026-06-07 补齐 `GSD1-LD-RED-002A` 一页式确认入口：`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md#12-nextgrantonepageconfirmation2026-06-07` 已把 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 的允许执行、业务问题、当前判断、首批 Red、写入范围、条件写入、只读参考、禁止范围、验证命令、Git 策略、停止条件和准出记录收束为一页；该入口随后已被用户确认并由 002A 消费，当前只作为历史授权文本和执行证据，不再作为可继续确认或复用的编码入口。
- [x] 2026-06-07 完成 002A 授权前只读准入刷新：`docs/TDD设计/GSD-1-账本账目状态账本.md#15-verificationevidence2026-06-07-pregrantreadonlyrefresh` 已记录当前 Harness checker 的可复跑路径为 `/Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py`，并复跑 `lightweight`、`gsd-wave`、`cad-candidate` 结构校验通过；同步修正 `docs/TDD设计/GSD-1-账本账目Wave1执行计划.md` 中旧 `#11` 恢复入口到 `#12-nextgrantonepageconfirmation2026-06-07`。第 16 节同步记录目标测试文件未被 Git 跟踪的工作树审计。该刷新只证明 002A 材料可恢复、结构完整和锚点一致，不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 完成 GSD + Goal 授权前 Git 基线口径收口：`docs/TDD设计/GSD-1-账本账目状态账本.md#17-verificationevidence2026-06-07-pregrantgitbaselinewording` 已记录本轮 docs-only 口径修正和验证结果。`GSD-Goal-生产可用MVP推进计划.md`、`GSD-1-账本账目Round0准入卡.md`、`GSD-1-账本账目Wave1执行计划.md`、`GSD-1-账本账目ExecutionGrant确认卡.md` 和本 tasks 已补充 001A/001B 只属于当前工作树证据、目标测试文件未被 Git 跟踪、不是已冻结 Git 基线的边界。已验证 Harness checker、`git diff --check` 和行尾空白扫描通过；本轮未获新的 Execution Grant，未运行 Maven、目标测试、PMD，未写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 完成 GSD + Goal 跨候选授权前准入复核：`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#81-pregrantadmissionevidence2026-06-07` 已记录 Goal 总计划、账本 002A、钱包账户/支付工具/Spend Rule、交易内核 B4 和清结算对账 B7 的 Harness、产品、架构和外部规则字段检查结果。已验证 `GSD-Goal-生产可用MVP推进计划.md` 的 `gsd-wave`、`cad-candidate`、产品结构和外部规则字段；`GSD-1-账本账目ExecutionGrant确认卡.md`、`B2B4-支付工具与SpendRule生产可用性Round0准入卡.md`、`B4-交易内核生产可用性Round0准入卡.md`、`B7-清结算与对账Round0准入卡.md` 的对应准入检查均通过。该记录只证明跨候选材料具备可确认单一 Grant 的结构条件，不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 完成 PRD、系分、DSL、TDD 权威入口授权前门禁复核：`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#82-authorityentrygateevidence2026-06-07` 已记录上游设计入口结构检查结果。已验证 `docs/产品设计/01-PRD总览.md` 的 PRD 结构检查，`docs/产品设计/02-交易路由钱包账目与投影.md`、`docs/产品设计/03-清结算与对账.md`、`docs/产品设计/06-VCC发卡业务资金底座PRD.md` 的产品架构结构检查，`docs/系分设计/02-交易路由钱包账目与投影系分设计.md`、`docs/系分设计/03-清结算与对账系分设计.md` 的系分结构检查，`docs/DSL设计/支付资金底座DSL承载层设计.md` 的架构计划结构检查，以及 `docs/TDD设计/支付资金底座测试驱动设计.md` 的 lightweight Harness 检查。该记录只证明权威入口可支撑下一单一 Grant 确认，不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 完成 GSD + Goal 编码准入裁决收口：`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#83-codingadmissiondecision2026-06-07` 已明确 002A 消费后当前状态为“可以重新选择下一单一 `Execution Grant`，但尚不可继续编码”。`Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 已由 `GSD1-LD-RED-002A` 消费；当时下一步先进入账本下一决策，在 `GSD1-LD-RED-003` 投影强化回归和 `GSD1-LD-RED-004` 的 `BUDGET_GROUP` 兼容策略人工决策之间收敛。该裁决随后已由 003 消费记录更新为预算组决策入口，并由 004 策略准备进一步更新为 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` / `GSD1-LD-RED-004A`。目标测试文件当前未被 Git 跟踪，未确认新的单一 Grant 前不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 完成 `GSD1-LD-RED-003` 余额投影强化回归登记：用户确认账本下一决策后，本轮低风险选择 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`，只复核既有 `LedgerBalanceProjectionServiceImplTests`，不触碰 `BUDGET_GROUP`。既有测试已覆盖事件发布失败不回滚余额事实、事件携带来源分录证据、嵌套上下文不可被外部回写污染、核心权益字段投影前阻断、后一个余额桶失败时整批不写；已验证 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one LedgerBalanceProjectionServiceImplTests tests` 通过，5 tests / 0 failures / 0 errors，未修改生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。003 完成时状态迁移为预算组决策入口；该入口随后已由 004 策略准备更新为 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` / `GSD1-LD-RED-004A`，仍未授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 完成 `GSD1-LD-RED-004` 预算组兼容策略准备：产品 + 架构只读扫描确认目标态 BudgetGroup 不是核心资金记账主体，但现有代码仍保留 `FundsSubjectType.BUDGET_GROUP`、posting 白名单、route participant、预算组 control ledger 初始化、预算额度调账和测试夹具兼容路径。策略收敛为 `COMPAT_CONTROL_LEDGER_WITH_FREEZE`：迁移期保留预算组控制账本兼容路径，但冻结为控制语义，不允许扩大为直接交易、授权交易、出入金、退款、清结算或对账补事实的资金价值主体。下一候选授权文本为 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` / `GSD1-LD-RED-004A`，状态 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；未确认前不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-11 完成 `GSD1-LD-RED-004A` BudgetGroup 兼容 guard：用户确认并消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 后，本轮补齐直接交易 topup / transfer / pay 和授权交易的 `BUDGET_GROUP` 拒绝用例，并在 transaction converter 层做最小 guard；余额控制兼容路径通过 `FundsBalanceControlFailureFlowTests` 回归保持可用。已验证 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsDirectTransactionFlowTests tests` 47 tests 通过，`just test-one FundsAuthorizationTransactionFlowTests tests` 29 tests 通过，`just test-one FundsBalanceControlFailureFlowTests tests` 19 tests 通过，`just compile` 通过，`git diff --check` 通过。未写 DDL/H2 schema、公共契约、Entity、Mapper、钱包、支付工具、VCC、清结算对账或运行时配置；Git 策略仍为 `summary_only`。004A 已消费，后续不得沿用本 Grant。
- [x] 2026-06-07 补齐 B4 交易内核生产可用 Round 0 准入卡：新增 `docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md`，把交易层下一候选从“继续泛化 B4 授权后继”收敛为账户主体型 canonical 内核补强，默认首切片为 `B4-CANONICAL-REPLAY-FAILFAST-CAD-001` / `Execution Grant：B4-CANONICAL-REPLAY-FAILFAST`。该卡只证明原路径回放、缺原 route snapshot 或原事实 fail-fast、当前支付工具/资金责任/预算/Spend Rule 绑定变化不重选路和失败无副作用；不授权支付工具 facade、VCC、清结算对账、治理、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-07 落地权益营销账户设计基线：PRD、DSL、系分、TDD 和 OpenSpec 已把“营销账户”重新定性为已决策权益资金影响的账户化承接，MVP 先按资金账户、平台责任资金账户或等价账户 profile 表达，不新增支付工具主体，不接管营销规则、券包库存、活动生命周期或最优券计算。该日追加交易入口裁决曾把 `FundsBenefitFundingApplicationService` 定义为权益资金责任应用门面；该旧口径已由 2026-06-16 `GSD2-BENEFIT-FUNDING-TRANSACTION-REBASE-001` 覆盖：`FundsBenefitFundingApplicationService` 目标态为交易级 application service，提供 `apply/refund/reverse` 并返回资金交易流水号，不再停留在 prepare/decision DTO 或重型权益快照固化。
- [x] 2026-06-16 消费 `Execution Grant：GSD2-BENEFIT-FUNDING-TRANSACTION-REBASE-001`：按产品和架构结论将权益让利资金入口从“资金责任准备门面”重基线为交易级 application service。允许写入 `transaction-face` 公共契约、请求模型、契约测试和 PRD/DSL/系分/TDD/OpenSpec；禁止写 transaction-impl 业务实现、route/posting/ledger 运行时实现、DDL/H2 schema、清结算对账实现和旧 core DSL 大规模物理删除。首轮公共契约删除 `FundsBenefitFundingPrepareRequest` 与 `FundsBenefitFundingDecisionDTO`，新增 `FundsBenefitFundingApplyRequest`、`FundsBenefitFundingRefundRequest`、`FundsBenefitFundingReverseRequest`，`FundsBenefitFundingApplicationService` 暴露 `apply/refund/reverse` 并返回资金交易流水号。随后按接口 CR 收缩请求模型：正向以 `businessSn` 作为统一幂等入口，保留原订单或原交易引用、承担方、受益方、金额、资金性质、账务效果和来源引用；退款和撤销只引用原权益资金交易、业务流水、金额和原因，不再暴露专用 `benefitRefundSn` / `benefitReverseSn`、专业确认状态、确认凭证、事实摘要或描述字段。专业确认和摘要属于生产准入、审计证据或实现侧校验。来源引用使用 `FundsBenefitFundingSourceDTO` 和 `FundsBenefitFundingSourceType`，避免与支付工具语义混淆；`ruleSn` 收敛为 `ruleId`。旧重型权益 DSL 清理由后续 `GSD2-BENEFIT-LEGACY-SNAPSHOT-REMOVE-001` 单独消费。
- [x] 2026-06-16 消费 `Execution Grant：GSD2-BENEFIT-FUNDING-TRANSACTION-IMPL-001`：在 `transaction-impl` 落地 `FundsBenefitFundingApplicationServiceImpl`，把 `apply` 委派到标准 `FundsDirectTransactionService#pay`，把 `refund/reverse` 先读取原权益资金交易 route snapshot 后委派到 `FundsDirectTransactionService#refund`，从而生成标准 route snapshot、交易事实、posting plan、ledger transaction、ledger entry 和余额影响。MVP 仅放行 `POSTING_REQUIRED` 权益让利资金事实，`NO_LEDGER`、`HOLD_ONLY`、`RELEASE_ONLY`、`REVERSAL_REQUIRED` 暂 fail-fast 且不得产生资金或账务副作用；退款和撤销必须引用原权益资金交易，不按当前营销规则重算。本轮新增 `FundsBenefitFundingApplicationServiceFlowTests` 覆盖正向、部分退款、撤销、账本分录、余额变化、route/ledger 对齐和非入账失败无副作用。未写 DDL/H2 schema、独立权益事实表、清结算对账实现、交易投影实现、旧重型权益 DSL 物理删除或公共请求字段扩张。
- [x] 2026-06-16 消费 `Execution Grant：GSD2-BENEFIT-LEGACY-SNAPSHOT-REMOVE-001`：单独清理旧权益快照 DSL。删除 core 旧 `FundsBenefitSnapshotSpec`、组件、引用、退款策略、稳定摘要对象、旧权益枚举、JSON support、`FundsInstructionSpec#getBenefitSnapshot`、不可变模型、旧契约测试和 `DSL-BENEFIT-SNAPSHOT-001.json`；`FundsDslJsonContractVerifier` 拒绝 `instruction.benefitSnapshot`，仍允许历史 `benefitSnapshotId + stableDigest` 作为 route、投影、对账或归档只读摘要；Route Replay 不再依赖当前请求携带旧权益 DSL，只有历史摘要残缺时 fail-fast。同步 PRD、DSL、系分、TDD 和 OpenSpec：目标态以 `FundsBenefitFundingApplicationService` 和权益资金事实为准，旧快照 DSL 只作为历史记录，不作为当前编码资产。
- [x] 2026-06-20 消费 `Execution Grant：GSD2-BENEFIT-FUNDING-SETTLE-REFUND-REBASE-001`：按产品和架构 CR 将 `FundsBenefitFundingApplicationService` 从 `apply/refund/reverse` 收敛为 `settle/refund`。`settle` 表达已确认入账的权益让利资金交易，`refund` 统一承接退款、业务取消、人工纠错或反向冲销；删除独立 `FundsBenefitFundingReverseRequest`，用 `FundsBenefitFundingSettleRequest` 替代旧 `FundsBenefitFundingApplyRequest`。实现仍委派标准直接交易、route、posting 和账本链路，不新增 `FundsMarketingTransactionService`、不写 DDL/H2 schema、不扩展清结算对账或独立权益事实表。本轮契约测试证明公共方法仅包含 `settle/refund`，流程测试覆盖 `POSTING_REQUIRED` 结算、部分退款、业务取消冲回、账本分录、余额变化、route/ledger 对齐和 `NO_LEDGER` 失败无副作用；PRD、DSL、系分、TDD 和 OpenSpec 同步移除旧交付态 `apply/reverse` 口径。
- [x] 2026-06-20 消费 `Execution Grant：GSD2-B4-TRANSACTION-PROJECTION-PI-EXPLAIN-001`：补齐支付工具授权入口的交易投影解释快照证据。`FundsTransactionProjectionExplanationSource` 从已持久化 route snapshot 读取 `paymentInstrumentRef`，在解释 payload 中输出工具号、工具类型、脱敏展示号、绑定快照和准入决策，并在 `evidenceRefs` 中输出 `paymentInstrument:*` 与 `paymentInstrumentBinding:*:v*`；测试证明该解释不输出外部 token 或敏感原文，且查询只读、不新增资金、route、posting、LedgerEntry、账本交易或余额事实。本轮只触碰服务层解释能力和测试，不新增 Controller、HTTP/RPC、统一支付工具交易服务、DDL/H2 schema 或 projection store；支付工具换绑后全链路 replay、完整多维查询索引和投影重放仍需独立 Execution Grant。
- [x] 2026-06-20 消费 `Execution Grant：GSD2-B2-BUDGET-GROUP-NON-LEDGER-SUBJECT-001`：收敛预算组创建自动建账缺口，已随 `a5b12a3f feat: 收敛资金服务层交付基线` 提交固化。`BudgetGroupService#createBudgetGroup` 不再调用 `SubjectLedgerInitializer`，预算组创建只保存预算 scope、周期、profile 和展示元数据，不初始化 ledger bucket，不生成 LedgerTransaction、LedgerEntry 或余额投影。目标测试先以 Red 证明旧实现会在 LIFETIME、MONTHLY 和 CUSTOM_CYCLE 预算组创建时初始化控制账本，Green 后 `ControlAccountLedgerInitializationTests` 覆盖预算组创建无账本事实副作用，并保留显式 `SubjectLedgerInitializer` 兼容测试；相邻回归 `FundsBalanceControlFailureFlowTests`、`FundsDirectTransactionFlowTests`、`FundsAuthorizationTransactionFlowTests` 通过显式兼容初始化保持既有预算组拒绝和余额控制场景。已验证 `just test-one ControlAccountLedgerInitializationTests tests`、`just test-one FundsBalanceControlFailureFlowTests tests`、`just test-one FundsDirectTransactionFlowTests tests`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just compile`、`just pmd` 和 `git diff --check` 均通过。该 Grant 只处理服务层预算组创建语义，不删除 `FundsSubjectType.BUDGET_GROUP`，不改 route/posting/余额查询/余额控制兼容路径，不新增 Controller、HTTP/RPC、DDL/H2 schema、Entity、Mapper 或生产迁移。
- [x] 2026-06-08 完成 PRD、系分、DSL、TDD 权威交付态整理：`docs/产品设计/README.md`、`docs/产品设计/01-PRD总览.md`、`docs/产品设计/02-交易路由钱包账目与投影.md`、`docs/产品设计/03-清结算与对账.md`、`docs/产品设计/05-产品验收与TDD用例矩阵.md`、`docs/产品设计/06-VCC发卡业务资金底座PRD.md`、`docs/系分设计/README.md`、`docs/系分设计/01-系分设计总览.md`、`docs/系分设计/02-交易路由钱包账目与投影系分设计.md`、`docs/系分设计/03-清结算与对账系分设计.md`、`docs/系分设计/04-归档重放与指标治理系分设计.md`、`docs/DSL设计/README.md`、`docs/DSL设计/支付资金底座DSL承载层设计.md`、`docs/TDD设计/README.md` 和 `docs/TDD设计/支付资金底座测试驱动设计.md` 已移除或收敛讨论过程、迭代版本、Grant 消费、当前 Git、工作树状态和历史提交证据；最终设计文档只保留稳定产品口径、系统边界、DSL 契约和 TDD 断言要求。过程性内容继续沉淀在本 tasks、GSD/Round0/Grant 卡和状态账本；本轮为 docs-only，不授权写 Java、测试代码、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-08 明确路由层与清分、清算、结算、出款和对账层能力边界：路由层只做资金路径解析、主体/账目/周期定位、路径快照和回放衔接，不创建清分批次、清算候选、清算批次、结算单、出款单、对账批次或差错单；清结算与对账层负责批次、单据、候选、审批、阻断、外部证据和差错闭环，只有清算批次确认、结算锁定、出款成功/失败/退回、已审批补事实或调账等标准资金事实成立后，才通过交易层和路由层追加账务影响。同步修正 DSL 和收单补充分册中“清分确认入账”的旧口径为“清算批次确认 / 清算确认”入账；本轮为 docs-only，不授权写 Java、测试代码、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-08 固化 B7 两级代理收益分润生产可用验收用例：平台员工邀请用户代理并可配置分润用户代理获得的佣金，作为 `B7-REVSHARE-ACCEPTANCE` 验收包进入 PRD、DSL、系分、TDD 和 B7 Round 0 基线。设计结论是资金底座只消费上层业务已固化的收益应得项、收益参与方、两级归因快照、GMV 阶梯或利润口径、规则版本、审批和账户解析；不实现代理关系、无限级分销、GMV 统计、利润计算、KPI 引擎、薪税处理、税务会计确认或营销规则引擎。该验收包排在 `B7-RECON-DIFFERENCE-MVP`、`B7-CLEARING-GATE`、`B7-PAYOUT-EXPLAIN` 和 `B7-OPS-AUDIT` 之后；本轮为 docs-only，不授权写 Java、测试代码、DDL/H2 schema、公共契约或运行时配置。
- [x] 2026-06-10 固化 `FundsBalanceControlService` 外部余额异常纠偏设计：第三方钱包、VCC 发卡行、发卡处理商或外部余额系统出现终局余额事实与我侧同主体余额不一致时，余额控制只承接已终局或经差错单确认的受控纠偏，不打开普通消费透支。纠偏必须具备外部终局事件、外部余额快照、差错单或审批、凭证、责任方、操作者和重新对账引用；允许同主体目标账目进入受控负可用，但负余额只能作为风险、追偿、抵扣或人工处理状态，后续支付、授权、冻结、提现、出款和清算放行必须重新读取运行时负余额策略事实。该条只同步 PRD、DSL、系分、TDD 和任务基线，不授权 Java、测试代码、DDL/H2 schema、公共契约或运行时配置。

## 1. MVP 任务写入范围

后续开发只能按 MVP 任务切片写入，不能按整章目标态或整张任务表一次性授权。任务开始前必须先写清 `mvpScenario`、覆盖 AC/DSL/系分/TDD/RED、写入范围、禁止范围、资金不变量、验证命令和停止条件。

当前执行优先级按模块或能力划分：账本账目 > 钱包 > 交易层 > 清结算对账。支付工具、VCC 和全球账户只作为最后一组支持能力；收单能力仅做设计、边界复核和差距登记，不做实现。B1 至 B8 只保留为 TDD 覆盖索引，不表示任一编号优先于上述队列，也不表示任何索引可以绕过独立 Execution Grant。

| 覆盖索引 | 能力优先级 | 可写入范围的上限 |
| --- | --- | --- |
| B1 | P0 共享承载 | `tests/src/test/java/com/wind/funds/dsl`、`tests/src/test/resources/dsl-contract-cases`、必要的 `core/src/main/java` DSL/枚举/Spec、Route DSL、PaymentInstrument DSL、RoutingDecision/FundingAllocation DSL、Posting/Ledger DSL、SettlementPolicy。 |
| B2 | P0 基础事实 | `wallet-*`、`ledger-*`、`tests/src/test/java` 中账户、支付工具、绑定关系、支出主体资金责任解析关系、账本、投影相关测试和最小实现。 |
| B3 | P1 直接交易 | `transaction-*`、`tests/src/test/java` 中直接交易测试和最小实现；`B3-DIRECT-REFUND-REFERENCE-REPLAY` 已消费并补齐直接退款原交易引用回放、独立退款事实、缺原交易失败无副作用和累计超额阻断。后续 B3 扩展仍需新的单一 Execution Grant。 |
| B4 | P1 授权交易 | `transaction-*`、`core`、`tests/src/test/java` 中授权交易测试和最小实现；授权过期、强制完成、无授权直接退款、争议退款可区分性和授权并发竞争首轮账户主体型 canonical 能力已闭合，2026-06-11 Agent Loop Plan Grant 已补授权后继缺原授权事实 fail-fast 覆盖；授权支付工具应用入口、授权权益生命周期和完整 dispute/chargeback case 的候选授权输入见 `docs/TDD设计/B4-授权后继能力Round0准入卡.md`。 |
| B5 | P1 余额控制 | `transaction-*`、`tests/src/test/java` 中余额控制测试和最小实现。 |
| B6 | P1 路由回放与交易投影 | `transaction-*`、`ledger-*`、`tests/src/test/java` 中 Route Replay、余额日志、交易投影测试和最小实现。 |
| B7 | P0 运营账务闭环 | 清结算、对账相关模块或包，需先确认；冻结基线已有 `reconciliation-*` 模块骨架和出款前准入候选实现，已完成专项验证，但未纳入清结算、对账或出款生命周期 Done 基线。当前 Round 0 已收敛到 `docs/TDD设计/B7-清结算与对账Round0准入卡.md`，默认首切片为 `B7-RECON-DIFFERENCE-MVP`，先证明对账差错闭环、阻断、重跑和补事实白名单准入。 |
| B8 | P0/P1 治理闭环 | 归档、余额重建、账本余额快照、交易投影重放相关模块或包，需先确认；当前已有 `governance-*` 交易投影重放骨架，但归档、余额快照和指标水位隔离未闭环；指标仅保留普通指标快照边界测试。进入编码前必须在 Execution Grant 中确认治理物理落点、依赖方向、是否新增公共契约、DDL/H2 schema、边界测试和指标水位隔离测试。 |

### 1.1 当前任务优先级队列

| 顺位 | 队列 | 任务范围 | 当前口径 |
| --- | --- | --- | --- |
| 1 | 账本账目 | ledger account、账目、posting、ledger entry、余额投影、清结算对账对象、账本余额快照和治理证据。 | 最高优先级；当前恢复入口为 `docs/TDD设计/GSD-1-账本账目状态账本.md`、`docs/TDD设计/GSD-1-账本账目Wave1执行计划.md`、`docs/TDD设计/GSD-1-账本账目代码库理解结论包.md` 和 `docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md`，状态 `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY`。2026-06-07 已消费 `GSD1-LD-RED-002A` / `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`，当前工作树目标测试 10 tests / 0 failures / 0 errors，未修改生产代码；已消费 `GSD1-LD-RED-003` / `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`，投影回归 5 tests / 0 failures / 0 errors，未修改生产代码或测试代码；2026-06-11 已消费 `GSD1-LD-RED-004A` / `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`，direct 47 tests、auth 29 tests、balance-control failure 19 tests 和 compile 通过。目标测试文件当前未被 Git 跟踪，因此该证据不构成已冻结 Git 基线。下一步必须确认新的单一 Grant，不得沿用 004A。 |
| 2 | 钱包 | 钱包账户、内部钱包入口、账户层级、资金责任解析、钱包 application facade、账户能力和失败无副作用。 | 排在交易层之前；当前可选前置入口为 `B2-ACCOUNT-HIERARCHY-CAD-001`，资金责任可选 `B2-FR` / `B2-FR-TARGET`；支付工具动作能力只作为后置支持能力，不代表钱包主优先级。 |
| 3 | 交易层 | A1 直接交易、账户主体型授权交易、余额控制、route replay、交易投影和交易投影重投影。 | 已闭合 B4 授权后继任务只作为回归基线；当前交易内核 Round 0 已收敛到 `B4-CANONICAL-REPLAY-FAILFAST-CAD-001`。2026-06-11 Plan Grant 已补授权后继缺原事实覆盖；随后用户确认并消费 `B3-DIRECT-REFUND-REFERENCE-REPLAY`，直接退款原交易引用回放切片已闭合；随后已验证纯 route replay 边界下当前支付工具、外部账户和资金责任变化不会覆盖原快照，`DefaultRouteReplayServiceTests` 9 tests 通过；随后已补原交易存在但 route snapshot 缺失时直接退款全链路失败无副作用覆盖，`FundsDirectTransactionFlowTests` 50 tests 通过；本轮已补直接退款交易 flow 下原支付快照固化旧支付工具和旧资金责任后仍沿历史快照回放，`FundsDirectTransactionFlowTests` 51 tests 通过。B4 剩余交易投影解释、调账审计和授权/争议/VCC lifecycle 更大组合 replay flow 仍需新的单一 Execution Grant。 |
| 4 | 清结算对账 | `B7-RECON-DIFFERENCE-MVP-CAD-001`，后续再拆 `B7-CLEARING-GATE`、`B7-PAYOUT-EXPLAIN`、`B7-OPS-AUDIT` 和 `B7-REVSHARE-ACCEPTANCE`。 | 排在支付工具、VCC 和全球账户之前；当前只形成 Round 0 候选，状态 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。两级代理收益分润仅作为生产可用验收包，不改变首切片仍先做对账差错闭环的顺序；未确认前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |
| 5 | 权益让利资金交易 | `GSD2-BENEFIT-FUNDING-TRANSACTION-REBASE-001`、`GSD2-BENEFIT-FUNDING-TRANSACTION-IMPL-001`、`GSD2-BENEFIT-LEGACY-SNAPSHOT-REMOVE-001` 与 `GSD2-BENEFIT-FUNDING-SETTLE-REFUND-REBASE-001` 已消费；下一候选为 `GSD2-BENEFIT-NON-POSTING-FACT-001` 或 `GSD2-BENEFIT-RECON-PROJECTION-001`。 | `POSTING_REQUIRED` 结算和退款型逆向处理已通过标准直接交易、route、posting、ledger 链路生成资金事实；旧重型权益 DSL 已从目标契约移除；非入账权益事实、清结算对账投影消费和独立权益事实持久化仍需新的单一 Execution Grant。仍不新增 `FundsMarketingTransactionService`。 |
| 6 | 支付工具支持 | `B2-PI-CAP-CAD-001`、`B4-AUTH-PI-CAD-001`、`B5-SR-CONTROL-CAD-001`、`B6-B8-PI-VIEW-CAD-001`。 | 放到最后的支持队列；只处理工具动作能力准入、授权工具 application facade、Spend Rule 控制和只读解释，不替换交易 canonical 请求。 |
| 7 | VCC 与全球账户支持 | `P2-VCC-PREPAID-CAD-001`、`P2-VCC-LIFECYCLE-CAD-001`、`P2-GA-INBOUND-CAD-001`、`P2-GA-OUTBOUND-CAD-001`、`P2-GA-FX-FEE-CAD-001`。 | P2 业务专项支持队列；不属于默认编码入口。 |
| 8 | 收单 | `P2-ACQ-CAPTURE`、`P2-ACQ-DISPUTE`、商户清结算和 PCI/外部规则边界。 | `DESIGN_ONLY_NOT_CODE_CANDIDATE`；不写实现、不写 Red 测试、不确认 Execution Grant。 |

## 2. 全局只读范围

| 路径 | 用途 |
| --- | --- |
| `docs/产品设计` | 产品目标、流程、状态、验收和风险边界。 |
| `docs/DSL设计` | DSL 语义、JSON 契约和场景矩阵。 |
| `docs/系分设计` | 模块边界、服务契约、表设计、观测和金融红线。 |
| `docs/TDD设计` | 测试用例、测试分层和红线。 |
| `openspec/specs/payment-funds-foundation/spec.md` | 当前开发规格基线。 |
| `tests/src/test/resources`、`core/src/test/resources` | 测试配置、schema 和数据资源。 |

## 3. 全局禁止事项

1. 不恢复旧测试源码或历史过渡断言。
2. 不把测试通过建立在旧 OpenSpec 或旧 Harness 计划上。
3. 不绕过 TDD 直接大范围改生产代码。
4. 不把清结算、对账、归档、指标实现混入交易、钱包、账本主链路。
5. 不引入真实外部调用、生产配置、敏感数据或无主依赖。
6. 不在缺少人工确认时修改公共契约、枚举、表结构和状态机。
7. 不在设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果未冻结时进入生产代码实现。

## 4. 每个 MVP 任务基础验证命令

后续每个 MVP 编码任务默认先执行基础验证，再执行任务表中的专项验证命令：

```bash
just mvn-version
just compile
```

如因 Java runtime、私有 Maven 仓库、网络、凭据或本地缓存导致无法执行，交付记录必须区分环境问题与代码问题。

进入 CAD 自动模式或需要声明完整本地基线复核时，优先执行：

```bash
just verify-cad
```

`verify-cad` 聚合运行时确认、编译、核心契约、账本、交易、余额控制、业务流、架构边界、治理重放和 PMD。仅修改文档或说明性材料时可不执行完整门禁，但交付记录必须说明原因。

## 5. 基线对齐结论

- [x] OpenSpec 已重新对齐：以 `project.md` 和 `payment-funds-foundation/spec.md` 作为当前目标态规格，不再引用历史 specs 或历史 changes。
- [x] Superpowers 已重新对齐：以 TDD、增量、可回退、先写用例再改实现为执行纪律，不复用旧测试源码和旧断言。
- [x] Harness 已重新对齐：以本文件作为 MVP 任务计划、覆盖索引、写入范围、只读范围、禁止事项、人工确认点和交付记录入口。
- [x] 旧测试源码已移除，测试 resources 保留。
- [x] 当前基线支持在用户明确授权后进入 CAD 自动提交模式；进入生产编码前仍需用户按 MVP 任务切片授予 Execution Grant，且每轮交付必须声明专项验证或 `just verify-cad` 完整门禁结果。
- [x] 已冻结设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果已作为独立检查点冻结；上一设计冻结点为 30b1a00 docs: 冻结权益快照设计基线。最新已提交设计和任务对齐输入以后续确认时 Git HEAD、OpenSpec 和 Harness 最新任务账本为准；`270122e`、`81a7ecb`、`4a7ef12`、`f99800b`、`9456ab6` 和 `77bc9f4` 保留为历史准入与局部保护证据；后续生产编码仍需用户按 MVP 任务切片单独授予 Execution Grant。
- [x] 代码基线已从上一冻结点 `77bc9f4` 复核到 `81a7ecb`，并在 `270122e` 后完成 CAD 准入刷新和完整门禁复核：B1 覆盖索引的 DSL 契约测试已存在；B2 覆盖索引下支付工具、绑定历史、资金责任解析关系（历史代码命名仍可能为 funding relation）、显式建账、预算组默认周期 `LIFETIME`、账务计划装配器长 ID 追溯、余额日志证据、余额投影、上下文敏感字段阻断、防御性拷贝、支付工具绑定对象约束和钱包入口文档口径已有局部基线；B3 至 B6 覆盖索引已有部分直接交易、授权、余额控制、Route Replay、交易投影、稳定摘要、解释摘要、权益回放摘要、路由事实边界、主链路事实红线、敏感上下文阻断、投影重放差异校验、失败无副作用、费用幂等和外部账户主体阻断测试；B7 已有 reconciliation-* 模块骨架和出款前准入候选实现，已通过专项服务测试验证，可作为后续候选输入但未纳入清结算、对账或出款生命周期 Done 基线；B8 已有 governance-* 交易投影重放骨架、范围/来源事实/差异项校验和局部边界测试。上述都只作为局部代码和验证门禁基线，不表示对应覆盖索引全量完成或可跳过 Execution Grant。
- [x] 完成 A0 只读核验证据登记：Java 21 运行时、编译门禁、A1 直接交易主链路、posting 装配和余额投影回归测试已通过；embedded Redis 由测试基础设施自动启动和使用，未触发额外人工确认。A0 通过只作为下一步单一 MVP Execution Grant 输入，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。
- [x] 完成 CAD 完整门禁验证登记：`WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad` 已通过，覆盖 Java 运行时、编译、核心契约、账本、交易、余额控制、业务流、架构边界、治理重放和 PMD；该结果仍只作为下一步 Execution Grant 输入，不替代生产 Done。
- [x] 补齐 A1 直接交易事实红线候选准入卡：`docs/TDD设计/A1-直接交易事实红线准入卡.md` 已整理候选授权、覆盖验收、写入边界、Red 集合、验证命令和停止条件；该卡仍需用户确认后才成为实际 Execution Grant。
- [x] 完成 A1 现有覆盖扫描登记：`52f116f docs: 记录 A1 现有覆盖扫描` 已记录 `A1-RED-001`、`A1-RED-002` 既有回归覆盖和 `FundsDirectTransactionFlowTests` 22 个用例通过证据；该结果只作为 Execution Grant 输入，不替代生产 Done。
- [x] 完成 A1 证据链同步后完整门禁复核：`97bc25d docs: 同步 A1 覆盖扫描证据` 后执行 `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad` 并通过，覆盖编译、测试和 PMD；该结果只证明当前候选准入证据链与工程基线一致，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。
- [x] 完成 2026-05-31 代码 CR 和设计差异复核：该轮以 `f99800b docs: 对齐代码 CR 任务基线` 为设计任务输入，重新对比 PRD、DSL、系分、TDD 与代码事实。结论是交易内核仍应保持账户主体型 canonical 入参，支付工具型入口只允许落 application facade；主要差异集中在 B2/B4/B5/B6 任务基线：钱包 application facade 尚未落地，资金责任关系目标字段仍偏 `fundingAccountId`，预算组仍有 ledger subject、ledger profile、route leg、posting 和余额查询兼容路径，部分 DSL/route 测试仍在保护旧预算组主体语义。本轮只更新任务基线，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。
- [x] 完成 2026-05-31 代码准入 CR：当前准入裁决为 A1 可作为下一步最小编码授权候选；B2/B4/B5/B6 必须先拆成独立 Round 0 或单一 Execution Grant，不能一次性写入 wallet application facade、资金责任目标字段、BudgetGroup 兼容策略、授权支付工具入口和预算控制投影；B7/B8/P2 继续不开放默认编码准入。
- [x] 完成 2026-06-11 Agent Loop Plan Grant 首轮执行登记：用户明确要求进入 Agent Loop Engineering 并使用 GSD + Goal 按任务计划推进后，本轮选择 `B4-CANONICAL-REPLAY-FAILFAST` 的低风险 `R0-TRX-REPLAY-001` 子场景，新增 `FundsAuthorizationTransactionFlowTests#testAuthorizationSuccessorsMissingOriginalFactShouldRejectAndLeaveNoSideEffects`，验证授权撤销、授权完成和已完成授权退款缺原授权交易事实时 fail-fast 且无资金、账本事实副作用；`just test-one FundsAuthorizationTransactionFlowTests tests` 30 tests 通过。该记录只是覆盖补齐，不代表 B4 完整 Done，不授权公共契约、DDL/H2、生产代码、运行时配置或 Git。
- [x] 完成 2026-06-11 Agent Loop 直接退款契约缺口登记：同一 Plan Grant 继续筛选交易内核低风险切片时，复核 `FundsTransactionRefundRequest` 与 `FundsDirectTransactionInstructionConverter#convertToRefundInstruction`，确认直接退款当时没有原支付交易、原 route snapshot 或原 ledger transaction 引用；若要求直接退款严格按原交易 / 原路径 replay，必须新增公共契约或等价 reference DTO。本轮停止在 `DIRECT_REFUND_REFERENCE_CONTRACT_GAP`，只回写 GSD 计划、B4 准入卡和 OpenSpec，不写 Java、不写测试、不改 DDL/H2、不执行 Git。该历史缺口已被 2026-06-11 `B3-DIRECT-REFUND-REFERENCE-REPLAY` 消费关闭。
- [x] 完成 2026-06-11 `B3-DIRECT-REFUND-REFERENCE-REPLAY`：用户确认并消费单一 Grant 后，直接退款新增 `referenceTransactionSn` 内部原交易引用，converter 转为 `ORIGINAL_TRANSACTION`，直接付款本金 leg 和费用 leg 支持部分回放，lifecycle 保存独立退款交易事实并更新原交易累计已退摘要。已补目标测试覆盖原交易引用回放、缺原交易失败无副作用和累计超额阻断；已验证 `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one FundsDirectTransactionFlowTests tests` 49 tests 通过、`just test-transaction` 102 tests 通过、`just test-boundary` 152 tests 通过。未写 DDL/H2 schema、支付工具 facade、VCC、清结算对账、治理或运行时配置；Git 策略仍为 `summary_only`。
- [x] 完成 2026-06-11 B4 route replay 原快照复用边界核验：在 Plan Grant 下复跑 `DefaultRouteReplayServiceTests`，9 tests 通过，确认 replay resolver 使用原 `RouteSnapshot` 中的支付工具快照、外部账户快照和资金责任决策，不被当前请求工具或账户上下文覆盖。本轮只回写 GSD 计划、B4 准入卡和 OpenSpec，不写 Java、不写测试、不改公共契约、不改 DDL/H2、不执行 Git；该证据不是 B4 完整 Done。

### 5.1 设计、代码、任务对齐矩阵

| 设计域 | 代码现状 | 任务基线 |
| --- | --- | --- |
| `02-交易路由钱包账目与投影` | DSL 契约、支付工具、钱包账户、部分直接交易、授权、余额控制、Route Replay、余额投影和交易投影已有局部测试与实现。 | 按 P0/P1 拆解补齐覆盖索引：B1、B2 对应 DSL、钱包、账本、账目和余额投影；B3 至 B6 对应直接交易、授权交易、余额控制和交易投影。已存在能力只作为局部基线，进入编码时必须再裁剪成 A0 至 A4 或等价 MVP 任务，按 TDD 证明目标 AC/DSL/RED。 |
| `03-清结算与对账` | 冻结基线中 `reconciliation-face`、`reconciliation-impl` 已有模块骨架和出款前准入候选实现，已覆盖准入解释状态、完整外部规则核验证据和只读无账务事实断言，但仍缺完整出款生命周期、表结构和数据库级服务闭环。 | 清结算与对账继续编码前必须另起独立 OpenSpec change，并确认模块、表、状态机、接口、是否扩展出款前准入候选能力和验证命令。 |
| B8 资金数据治理边界 | `governance-face`、`governance-impl` 已有交易投影重放骨架和局部边界测试；产品 04 已降级为拆分索引。 | B8 是 P0/P1 治理闭环覆盖索引，不因编号排在 B7 后而降低优先级；Manifest、账本余额快照、普通指标快照、交易投影重放和水位隔离仍需独立 Execution Grant，并以产品 02、03、05 和已确认事实边界为输入。 |
| 2026-05-31 代码准入 CR | 账户主体型交易内核请求与最新设计一致；`FundsTransactionPayRequest`、`FundsAuthorizationTransactionAuthorizeRequest`、`FundsBalanceFreezeRequest` 仍以 `FundsAccountId` 入参，`ImmutableResolvedRouteSpec` 已有 `PaymentInstrumentRefSpec` 快照位。代码现状不阻断 A1 直接交易事实红线按单一 Execution Grant 开工，但阻断“泛化地直接编码钱包/授权/预算组目标态”。主要缺口在钱包应用层 facade、资金责任目标字段和 BudgetGroup 账务主体化兼容路径；代码证据包括 `FundsSubjectType.BUDGET_GROUP`、`CreateSpendSubjectFundingRelationRequest.fundingAccountId`、`SpendSubjectFundingRelationServiceImpl#getFundingAccount` 只校验真实资金账户、`DefaultFundsAccountQueryServiceImpl` 解析预算组、`BudgetGroupServiceImpl` 初始化预算组 ledger、`DefaultLedgerProfileServiceImpl` 的 `BUDGET_BASIC` profile、`DefaultLedgerTransactionPostingServiceImpl` 允许 `BUDGET_GROUP` 入账、`RouteSubjectSupport` 和 `DefaultRouteReplayService` 将预算组解析成 route participant，以及 `FundsBalanceControlInstructionConverter` 把预算组当额度调整对象。 | 这些差异必须进入后续单一 Execution Grant；下一步若要编码，应按当前队列优先从账本账目、钱包基础能力或交易层选择一个最小切片。支付工具、VCC 和全球账户支持排到最后；未确认前不得把预算组 route leg、posting、LedgerEntry、ledger 初始化、余额查询或旧 DSL 测试通过作为目标态 Done。 |
| 2026-06-01 DSL/路由/账务/投影 CR | `SubjectRef`、route leg participant、posting subject、LedgerEntry subject 和账本余额投影主体必须统一到资金账户、信用账户或平台角色解析后的平台资金账户；`PaymentInstrumentRef`、`ExternalAccountRef`、预算组、Spend Rule 和交易投影不得被解释为账务主体；交易投影输入只能来自交易事实、冻结单、route snapshot、`paymentInstrumentRef`、`FundingAllocationDecision`、`SpendRuleDecisionLog`、`SpendControlActivity`、账本摘要、授权拒绝事实、清结算和对账差错。 | A1 可以继续按账户主体型直接交易事实红线准备确认；B2/B4/B5/B6/B8 若要触碰支付工具 application facade、预算控制视图、route replay、交易投影或 BudgetGroup 兼容策略，必须独立 Execution Grant。B6/B8 不得把交易投影通过写成账务事实、余额事实或生产 Done。 |
| 2026-06-01 支付工具与 Spend Rule 生产可用性 CR | 当时 `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests` 和 `PaymentInstrumentRouteDslContractTests` 只能证明资源服务、现有资金责任关系和 DSL 契约局部基线，尚未证明工具能力准入、授权准入、Spend Rule 决策日志、Spend Control Activity、预算控制投影或 `authorizeByInstrument` 生产链路；后续已补齐 `PaymentInstrumentCapabilityApplicationService`、`AuthorizationAdmissionApplicationService`、`FundsAccountCapabilityApplicationService`、`PaymentInstrumentPreTransactionSnapshotApplicationService` 和 `SpendControlAdmissionApplicationService` 首轮 Green，当前仍缺完整 Spend Rule 控制活动、预算控制投影和完整业务策略准入生产链路。 | B2 的账户层级属于钱包账户前置，B2 的支付工具能力准入属于后置支持；资金责任目标主体已按 `targetSubjectType + targetSubjectId` 首轮落地，`fundingAccountId` 仅保留为资金账户目标兼容字段。B4 做授权准入组合；B5 做预算预留释放控制；B6/B8 做只读投影和重放。未授权前不得新增统一支付工具交易服务、Spend Rule 表或投影表，也不得把资源服务测试通过等同于生产可用。 |
| 2026-06-04 B2-PI-CAP 后置支持候选 | 支付工具资源服务已能证明工具、绑定、绑定历史、状态、方向、币种、生效窗口、敏感字段阻断和无账务副作用局部基线；2026-06-16 已完成首轮 `PaymentInstrumentCapabilityApplicationService`，固化 RECEIVE、PAY、AUTHORIZE、REFUND、WITHDRAW 五类动作能力，并输出只读工具准入快照。 | `B2-PI-CAP-CAD-001` 已消费为首轮 Green，后续只作为回归基线维护；若继续扩展工具能力配置、授权 admission、资金责任组合、Spend Rule、预算控制、投影、治理或 P2，需要另起新的单一 Execution Grant。工具能力通过不得替代账户能力、余额、额度、账本周期、资金责任或授权准入校验。 |
| 2026-06-06 B2-ACCOUNT-HIERARCHY 钱包账户候选 | 当前设计已确认 VCC 不新增 `VCC_ACCOUNT`，每张卡绑定资金/信用子账户，父账户默认只做约束和汇总；代码侧尚未形成账户层级快照、父账户 / 根账户快照、层级版本、`PostingRole`、父账户控制分录和父子账户防双算的公共承载。 | `B2-ACCOUNT-HIERARCHY-CAD-001` 只作为钱包账户/账户层级队列的单一 Grant 确认输入，且是 VCC prepaid funding 和 lifecycle 的前置候选。未确认前不得写 application facade、Request/DTO、core DSL value object、DDL/H2 schema、测试或实现；确认时必须选择 `contract-only` 或 `ledger-snapshot-backed`，并显式确认 postingRoleDecision、父账户 / 根账户快照策略和 DDL/H2 范围。 |
| 2026-06-04 B2-FR-FAO Grant 候选 | 资金责任关系资源服务已能证明真实资金账户存在、可借记、币种一致、生效窗口、默认关系唯一、优先级不冲突、敏感字段阻断和无账务副作用局部基线；当前字段和表结构仍偏 `fundingAccountId` / `funding_account_id`。 | `B2-FR-FAO-CAD-001` 可作为下一次单一 Execution Grant 确认输入；首批 Red 为 `R0-FR-001A`，只处理 `funding-account-only` 策略下唯一资金账户责任主体裁决和不可变 `FundingAllocationDecision`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现；不得声明信用账户或平台角色责任主体生产可用，目标主体迁移必须另起 `B2-FR-TARGET`。 |
| 2026-06-07 B2-FR-TARGET Grant 候选 | VCC、信用账户和平台责任来源需要把最终责任目标从 `fundingAccountId` 演进为 `targetSubjectType + targetSubjectId` 或等价主体引用；否则共享卡、预付卡、信用账户和平台责任无法声明生产可用。 | `B2-FR-TARGET-CAD-001` 可作为钱包资金责任目标主体迁移的单一 Execution Grant 确认输入；首批 Red 为 `R0-FR-TARGET-001`，次批 Red 为 `R0-FR-TARGET-002`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、Entity、Mapper、route snapshot、fixture、测试或实现；确认时必须列明账户层级依赖、schemaGate、兼容字段、迁移校验、回放断言和验证命令。 |
| 2026-06-04 B5-SR-CONTROL Grant 候选 | 当前可定位的是 BudgetGroup、`BUDGET_GROUP` ledger profile、余额控制调账和预算组余额查询等兼容路径；未发现 `SpendRuleDecisionLog`、`SpendControlActivity`、Spend Rule application facade、规则版本决策模型或预算控制投影生产模型。 | `B5-SR-CONTROL-CAD-001` 可作为下一次单一 Execution Grant 确认输入；首批 Red 为 `R0-SR-001A`，次批 Red 为 `R0-SR-002A`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现；确认时必须选择 `contract-only` 或 `ddl-backed`，且不得把 BudgetGroup ledger 兼容路径写成目标态 Done。 |
| 2026-06-04 B6/B8-PI-VIEW Grant 候选 | `FundsTransactionProjectionPublisher`、`DefaultRouteReplayServiceTests` 和 `FundsProjectionReplayServiceTests` 已证明正常交易投影发布、原 route snapshot 回放、有界交易投影重放和差异报告局部边界；尚未形成支付工具流水、预算控制视图、规则命中时间线和面向运营/财务的统一解释查询闭环。 | `B6-B8-PI-VIEW-CAD-001` 可作为下一次单一 Execution Grant 确认输入；首批 Red 为 `R0-PI-002A`，次批 Red 为 `R0-PI-002B`。未确认前不得写 Query/DTO、DDL/H2 schema、测试或实现；确认时必须选择 `query-contract-only` 或 `projection-store-backed`，且不得把交易投影或治理重放结果反写成资金事实、账本事实或余额事实。 |
| 2026-06-04 P2-VCC-PREPAID Grant 候选 | VCC、虚拟卡、卡 token 和 prepaid virtual card 的目标态是支付工具快照与资金模式，不是账本主体；当前未发现 `VccPrepaidFundingApplicationService`、VCC prepaid funding 请求模型、资金子账户/父账户快照契约或专项资金流实现，已有账户主体型直接交易/授权交易只能作为 P0/P1 可复用能力基线。 | `P2-VCC-PREPAID-CAD-001` 可作为 P2 业务专项 Execution Grant 确认输入；首批 Red 为 `R0-VCC-PREPAID-001A`，次批 Red 为 `R0-VCC-PREPAID-001B`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现；确认时必须选择 `contract-only` 或 `funding-flow-backed`、资金子账户、父账户快照、背后资金来源策略和外部规则核验状态，且不得把卡工具、共享卡、父账户汇总、预算组或 Spend Rule 写成资金账户、ledger subject、route leg、posting subject、LedgerEntry subject 或余额投影主体。 |
| 2026-06-04 P2-VCC-LIFECYCLE Grant 候选 | 账户主体型授权生命周期、route replay 和交易投影已有局部基线，但 VCC shared/prepaid card 的清算、释放、退款和争议事件尚未形成产品侧 application facade、原授权引用、原 route snapshot 引用、原工具快照引用、原资金责任决策引用、绑定版本和差错/人工入口闭环。 | `P2-VCC-LIFECYCLE-CAD-001` 可作为 P2 业务专项 Execution Grant 确认输入；首批 Red 为 `R0-VCC-LC-001A`，次批 Red 为 `R0-VCC-LC-001B`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现；确认时必须选择 `contract-only` 或 `canonical-lifecycle-backed`、原快照策略、disputeDecision 和外部规则核验状态，且不得按当前绑定/规则重新选路或把普通退款与争议/拒付合并为不可区分事实。 |
| 2026-06-04 P2-GA-INBOUND Grant 候选 | 全球账户入金、VA/银行流水匹配和外部受理在途属于 P2 业务 capability pack；当前只有外部账户引用、敏感值阻断、FX 端口和出款前准入局部基线，尚未形成全球账户入金 facade、Request/DTO、匹配确认、外部非终态不入账和错币种阻断目标 Red。 | `P2-GA-INBOUND-CAD-001` 可作为 P2 业务专项 Execution Grant 确认输入；首批 Red 为 `R0-GA-IN-001A`，次批 Red 为 `R0-GA-IN-001B`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现；确认时必须选择 `contract-only` 或 `canonical-funds-backed`、账户解析决策、FX 决策和外部规则核验状态，且不得把 VA、银行账户、Nostro、Vostro、外部银行流水或完整敏感账号写成资金账户、ledger subject、route leg、posting subject、LedgerEntry subject 或余额投影主体。 |
| 2026-06-04 P2-GA-OUTBOUND Grant 候选 | 全球账户出款、外部受理在途、成功回单和退汇属于 P2 业务 capability pack；当前只有出款前准入候选、外部账户引用、敏感值阻断和 FX 端口局部基线，尚未形成全球账户出款 facade、Request/DTO、外部非终态不误展示、成功回单终态、退汇关联原出金、费用和责任处理目标 Red。 | `P2-GA-OUTBOUND-CAD-001` 可作为 P2 业务专项 Execution Grant 确认输入；首批 Red 为 `R0-GA-OUT-001A`，次批 Red 为 `R0-GA-OUT-001B` 和 `R0-GA-OUT-001C`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现；确认时必须选择 `preflight-contract-only` 或 `canonical-transit-backed`、preflightDecision、transitDecision、returnDecision、FX 决策和外部规则核验状态，且不得把外部提交、message sent、processing 或银行退汇误写成成功付款或普通退款。 |
| 2026-06-04 P2-GA-FX-FEE Grant 候选 | 全球账户 FX quote 引用、费用分离和错币种阻断属于 P2 业务 capability pack；当前只有 FX 端口、原币金额/汇率字段和直接交易手续费/退费局部基线，尚未形成全球账户 FX/费用 facade、Request/DTO、quote approval snapshot、费用归因、FX P&L 专业确认和不执行 FX 的目标 Red。 | `P2-GA-FX-FEE-CAD-001` 可作为 P2 业务专项 Execution Grant 确认输入；首批 Red 为 `R0-GA-FX-001A`，次批 Red 为 `R0-GA-FX-001B` 和 `R0-GA-FX-001C`。未确认前不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现；确认时必须选择 `contract-only` 或 `attribution-backed`、`no-fx-execution` 或 `quote-validation-only`、`fee-attribution-only` 或 `fee-ledger-backed`、外部规则和专业确认状态，且不得静默换汇、净额混淆费用或把资金服务写成 FX 执行系统。 |
| 2026-06-04 P2-ACQ-CAPTURE 设计-only | 收单 capture 或支付成功属于 P2 业务 capability pack；当前只有商户信息、商户上下文、清算账目、授权 capture 概念和敏感上下文阻断局部基线，尚未形成收单 capture facade、Request/DTO、capture 幂等、商户资金账户解析、merchant CLEARING 断言、支付成功不等于可提现和 PCI 原文阻断目标 Red。 | 当前状态为 `DESIGN_ONLY_NOT_CODE_CANDIDATE`。本行只保留收单设计差距、外部规则和 PCI 边界，不作为历史收单 capture 实现候选的授权输入；不得确认或消费 P2-ACQ-CAPTURE 实现授权，不得写 application facade、Request/DTO、DDL/H2 schema、测试或实现。 |
| 2026-06-02 支付工具接口落包与交易层能力裁决 | 支付工具型入口统一落钱包 application facade：`com.wind.funds.wallet.application.instrument` 承接工具准入、授权准入和授权后生命周期，`com.wind.funds.wallet.application.funding` 承接资金责任解析，`com.wind.funds.wallet.application.vcc` 承接 VCC 预付资金和共享卡场景编排；Request/DTO 默认落 `com.wind.funds.wallet.model.request` 和 `com.wind.funds.wallet.model.dto`。交易层已补齐账户主体型授权过期释放、强制完成、B4-NO-AUTH-REFUND、争议退款可区分性和授权并发竞争首轮 canonical 能力；2026-06-11 已补授权后继缺原授权事实 fail-fast 覆盖；余额控制调账、剩余原路径回放、支付工具授权应用入口、授权权益生命周期和完整 dispute/chargeback case 仍需后续独立补强。交易层不新增统一 `InstrumentTransactionService`，不新增顶层 `com.wind.funds.instrument`，不让 `transaction-impl` 反向依赖钱包资源服务。 | 后续编码必须拆成两类 Execution Grant：B2/B4/P2 的 wallet application facade 切片，以及 B3/B4/B5/B6 的 transaction canonical 内核补强切片。两类切片可以通过接口委派协作，但不得在同一轮混合改包、改交易请求、改状态机、改 DDL/H2 和改投影。 |
| 2026-06-01 B2/B4 Round 0 准入卡 | 已新增 `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md`，作为支付工具应用准入、资金责任解析、授权支付工具入口、Spend Rule 控制和只读投影的共同准入输入。 | 后续只能选择其中一个候选切片进入 Execution Grant；不得一次性授权 B2、B4、B5、B6 和 B8 的全部目标态。 |
| 导出附件 | 若工作树存在 `docs/*.zip` 等导出包，只能作为评审附件。 | 导出包不作为规格、任务或验收 Source of Truth；是否纳入版本库需用户单独确认。 |

### 5.2 生产交付判定

本 Harness Plan 的任务结论只表示设计、覆盖索引和 MVP 任务规则已经对齐，不表示生产交付已经完成。后续每个 MVP 任务必须在交付记录中单独说明 Done 证据。

| 覆盖索引 | 当前判定 | Done 证据要求 |
| --- | --- | --- |
| B1/B2 覆盖索引 | P0 共享承载、账本账目、钱包基础事实可按单一 MVP 任务 Execution Grant 进入编码。 | 对应 DSL、钱包、账本、账目、余额投影 AC/DSL/TDD/RED 映射闭合，相关测试通过，资金变化断言覆盖主体、账目、posting、entry、projection、幂等和审计。 |
| B3 至 B6 覆盖索引 | P1 交易入口、余额控制、路由回放和交易投影可按单一 MVP 任务 Execution Grant 进入编码。 | 对应直接交易、授权交易、余额控制、Route Replay、交易投影和权益资金流 AC/DSL/TDD/RED 映射闭合，相关测试通过，资金变化断言覆盖状态、route、posting、entry、projection、幂等和审计；含权益任务还需覆盖使用者解释视图、证据最小化和外部规则核验状态。 |
| B7 | P0 清结算与对账只保留边界、计划和设计评审输入；整体编码准入未打开，出款前准入候选实现只作为差距复核输入。 | `CLS-GATE-*`、独立 OpenSpec change、模块/表/接口确认、DDL/H2、服务级 H2 测试、对账阻断和清结算幂等测试通过。 |
| B8 | P0/P1 资金数据治理边界只保留边界、计划和设计评审输入；交易投影重放只是局部基线，编码准入未打开。 | `GOV-GATE-*`、Manifest、checkpoint、watermark、余额快照、指标水位隔离、范围锁、差异报告、异常人工处理、大数据消费边界、回滚/续跑测试通过。 |

### 5.3 MVP 任务交付证据包

后续每个 MVP 任务开始和完成时，分别补齐申请卡和验收记录。字段不允许留空；不适用时必须写 `N/A + 原因`。字段留空、用“待补”占位或无法说明不适用原因时，视为该任务仍未达到 Done。

任务开始前先填写申请卡：

```text
任务切片：
mvpScenario：
设计基线提交点：
申请目标：
设计域：
覆盖产品验收：
覆盖 DSL caseId：
覆盖 TDD 用例：
覆盖红线：
写入范围：
只读范围：
非目标：
允许修改公共契约：是/否
公共契约允许修改范围：
允许新增枚举或事件：是/否
允许新增服务入口：是/否
允许扩展 Request/Query/DTO：是/否
允许修改状态机：是/否
允许修改表结构：是/否
允许新增模块：是/否
是否影响架构 ADR：是/否
受影响 ADR：
是否触碰能力域边界：是/否
是否触碰事实端口层：是/否
架构边界测试范围：
治理物理落点：复用 governance-face/governance-impl / 扩展既有包 / 新增独立模块 / 不涉及；依赖方向 / 公共契约 / 表结构 / H2 schema / Mapper Entity 归属 / 边界测试 / 指标水位隔离测试：
运营补事实命令白名单：允许命令或等价事件 / 不允许 / 不涉及；来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因和可撤销边界：
退款分摊确定性规则：规则版本 / 分摊依据 / 稳定组件顺序 / 舍入模式 / 尾差归属 / 组件剩余额度版本 / 幂等摘要字段 / 并发保护 / 不涉及
先写或先恢复的测试：
验证命令：
人工确认点：
停止条件：
工作树状态：
允许纳入本任务的未提交变更：
必须排除的未提交变更：
NFR 假设：
观测告警：
回滚或补偿：
```

任务完成时再填写验收记录：

```text
任务切片：
mvpScenario：
Execution Grant：
覆盖产品验收：
覆盖 DSL caseId：
覆盖 TDD 用例：
覆盖红线：
修改范围：
公共契约变化：
DDL/H2 schema 变化：
服务契约证据：
状态机和幂等证据：
资金断言证据：
运营补事实白名单证据：
治理物理落点和 DDL/H2 证据：
观测和审计证据：
架构边界证据：
外部规则和待确认项：
验证命令：
验证结果：
未覆盖项：
残余风险：
完成结论：Done / Conditional Done / Not Done
```

设计准入评审可以使用“通过、带条件通过、阻断”；MVP 编码任务验收只能使用 `Done / Conditional Done / Not Done`。只有文档、设计或计划闭合时不得写 Done；本任务生产目标必需的 DDL/H2、服务级测试、资金断言、幂等或审计证据缺失时必须写 Not Done。

### 5.4 当前编码准入裁决

| 检查点 | 裁决 | 处理口径 |
| --- | --- | --- |
| CAD Round 0/A0 基线与验证证据 | 已提交为下一轮编码准备输入。 | 后续 Execution Grant 必须引用已提交设计基线和 A0 只读核验证据；若产生新的未提交设计变更，必须先提交或明确列为本任务基线附件。 |
| A0 至 A4 | P0/P1 条件准入。 | 仅在 Execution Grant 写清 `mvpScenario`、AC/DSL/系分/TDD/RED、写入范围、测试和验证命令后逐任务进入；不得一次性授权整个 02 目标态。B1 至 B6 只作为覆盖索引。 |
| B7 | P0 未准入。 | 出款前准入候选实现只能作为差距复核输入；清结算、对账和出款生命周期必须独立 OpenSpec change 后再授权，并确认 `CLS-GATE-*`、首批 Red、模块/表/接口、DDL/H2 schema、服务级测试、运营补事实白名单和职责分离边界。 |
| B8 | P0/P1 未准入。 | governance 交易投影重放局部基线不能替代归档、余额快照、差异报告、异常人工处理和指标水位隔离；进入编码前必须另补 Execution Grant，并确认 `GOV-GATE-*`、治理物理落点、依赖方向、DDL/H2 schema、边界测试、指标水位隔离测试和大数据消费边界。 |
| P2 业务能力包 | 不开放默认编码准入。 | VCC 和全球账户必须按业务专项 PRD、DSL 准入卡、系分承接卡和 `TDD-P2-*` 用例另起 Execution Grant；VCC 预付/共享卡还必须声明 `PaymentInstrument`、绑定快照、资金责任解析关系、原授权和原 route snapshot 回放策略、禁止新增账户/账本主体和 P0/P1 回归范围；授权前只能做设计、契约草案或 contract-only 验证，不得把业务 pack 当作 P0/P1 默认实现范围。收单当前例外，只允许 design-only 设计、边界复核和差距登记，不进入实现授权。 |
| 本轮生产代码、测试代码、DDL/H2 schema 和运行时配置 | 不授权写入。 | 本轮代码准入 CR 只允许完善设计和任务基线；编码开始前另行确认单一 Execution Grant、Git 策略、人工确认点和停止条件。 |

### 5.5 编码阶段 Execution Grant 对齐模板

进入编码阶段时，Execution Grant 必须作为 PRD、DSL、系分、TDD、OpenSpec、任务基线和当前 Git 状态之间的唯一写入授权单。缺少任一必填项时，本 change 只能作为 TDD 分析和设计基线，不得写生产代码、测试代码、DDL/H2 schema 或运行时配置。

| 必填项 | 填写要求 | 缺失时裁决 |
| --- | --- | --- |
| MVP 场景和任务切片 | 指明 `mvpScenario`、A0 至 A4、B7、B8 或 P2 业务专项；B1 至 B8 细项只作为覆盖索引，不能一次性授权整个目标态。 | Not Done。 |
| 业务驱动准入页 | 写清 `productGoal`、`businessQuestion`、`moneyFact`、使用者可见结果、成功/失败终态、产品验收 ID、DSL caseId、系分章节和产品不做范围。 | 回到 PRD 或 A0，不进入 Red 写入。 |
| 基线引用 | 引用 PRD、DSL、系分、TDD、OpenSpec、Harness Plan、任务文档和 Git 提交点；未提交变更必须列为本任务基线附件。 | 不得把草稿当冻结基线。 |
| 验收与红线 | 列出 AC-*、RED-*、DSL caseId、TDD-*、首批 Red 和 Not Done 条件；B7 必填 `CLS-GATE-*`，B8 必填 `GOV-GATE-*`。 | 只能继续 TDD 分析。 |
| 写入范围 | 列出允许修改的模块、包、公共契约、枚举、Request/Query/DTO、状态机、表结构、H2 schema、fixture、生产代码和测试目录。 | 不得写入未列范围。 |
| 禁止范围 | 列出不得触碰的模块、接口、表、运行配置、外部协议、历史事实和用户未提交变更。 | 越界即停止。 |
| 首批 Red | 指定先失败的测试名称、断言事实、夹具等级、目标验证命令和失败无副作用检查。 | 不得进入 Green 实现。 |
| 金融红线 | 明确金额、幂等、余额桶、posting 平衡、ledger transaction 可追溯、审计、权限、外部规则核验和专业确认状态。 | 只能做契约或设计验证。 |
| 验证命令 | 指定最小目标测试、边界测试、PMD/compile/verify-cad 取舍，以及无法执行时的环境限制说明。 | 不得声明 Done。 |
| 停止条件 | 明确公共契约冲突、表结构冲突、资金口径不一致、外部规则未确认、测试无法闭合、权限不足和工作树冲突时的暂停规则。 | 触发即停止并回到 CR。 |

字段化授权建议：

| 字段 | 承接内容 |
| --- | --- |
| `mvpScenario` | 本任务服务的使用者、资金事实、最小闭环和不得声明的扩展能力。 |
| `businessAdmission` | 产品目标、业务问题、资金事实、使用者可见结果、成功/失败终态、验收 ID、DSL caseId、系分章节和明确不做范围。 |
| `abilityBatch` | A0 至 A4、B7、B8 或 P2 业务专项、能力域、AC/DSL/系分/TDD/RED 和 Not Done 条件。 |
| `authorityBaseline` | PRD、DSL、系分、TDD、OpenSpec、Harness Plan、Git 提交点、工作树状态和允许读取文件。 |
| `writeScope` | 允许写入的生产代码、测试代码、公共契约、枚举、Request/Query/DTO、状态机、表结构、H2 schema、fixture 和运行时配置。 |
| `noWriteScope` | 禁止写入范围、不得触碰的模块、对象、资金语义、外部协议、历史事实和用户未提交变更。 |
| `physicalLanding` | 模块、包、face/impl、端口、依赖方向、DTO、Entity、Mapper、DDL/H2 和边界测试范围。 |
| `firstRedSet` | 必须先失败的 Red、目标测试资产、失败断言、测试层级和验证命令。 |
| `moneyInvariant` | 主体、账目、币种、周期、route、posting、entry、projection、余额、幂等、失败无副作用和审计证据。 |
| `operationGovernanceGate` | 清结算、对账、出款、差错、白名单、归档、Manifest、checkpoint、watermark、差异报告、人工处理和指标水位隔离。 |
| `externalRuleStatus` | 规则来源、版本或发布日期、生效日期、适用主体或范围、适用法域、核验日期、确认方和确认状态。 |
| `verificationAndStop` | 目标验证命令、边界测试、PMD/完整验证取舍、失败停止条件和 Not Done 判定。 |

## 6. 里程碑拆解

| 里程碑 | 目标 | 完成标志 | 阻塞关系 |
| --- | --- | --- | --- |
| M0 基线重置 | 作废历史基线、删除旧测试源码、保留 resources、重建 OpenSpec/Harness。 | 本文件第 0 节全部完成。 | 已完成。 |
| M1 P0 资金底座内核 | 先把账本、账目、posting、余额投影、账本余额快照和清结算/对账证据的不变量证明清楚，再承接钱包账户和资金责任基础。对应 B1、B2、B7、B8 的 P0 覆盖索引。 | P0 任务按各自 Execution Grant 通过，真实服务层测试、余额断言、账务平衡、清结算对账阻断、归档 Manifest、余额快照和失败隔离闭合。 | 当前最高优先级先落到账本账目；可按独立授权分任务推进，不要求把 03/04 视为低优先级后置能力。 |
| M2 P1 交易与读模型扩展 | 在 P0 主体、账目、账本、余额和治理边界上，落直接交易、授权交易、余额控制、Route Replay、交易投影和权益资金流。对应 B3 至 B6 覆盖索引。 | P1 任务通过，交易入口具备状态、route、posting、entry、projection、幂等和审计断言；含权益场景补齐解释视图、证据最小化和规则核验断言。 | 依赖已确认的 P0 基础事实；不得反向改变 P0 资金口径。 |
| M3 P2 业务模式能力包 | 按专项 PRD/DSL/系分/TDD 支撑后置的 VCC 发卡和全球账户收付款；收单只保留设计能力包。 | VCC/全球账户专项能证明归一资金事实、外部引用脱敏、外部规则核验、轨道边界、`TDD-P2-*` 专项用例和 P0/P1 回归闭合，不把业务协议沉入资金内核。VCC 专项必须证明 VCC 卡/虚拟卡/卡 token 是 `PaymentInstrument`，prepaid virtual card 是资金子账户模式，shared card 是信用子账户绑定/使用模式，必须固化父账户快照，并回挂 `VCC-AC-007`、`VCC-AC-008`、`DSL-VCC-HIERARCHY-001`、`DSL-PAYMENT-INSTRUMENT-PREPAID-CARD-001`、`DSL-PAYMENT-INSTRUMENT-SHARED-CARD-001`、`TDD-RAIL-001A`、`TDD-P2-VCC-004` 至 `TDD-P2-VCC-011`。ACH 或银行转账仅作为上层业务或外部轨道输入，资金底座只承接归一资金事实、外部引用、对账差错、追偿、调账核销和审计；若专项使用 ACH 或银行转账，必须回挂 `ACH-BOUNDARY-001` 至 `ACH-BOUNDARY-006`、`AC-RAIL-002A` 至 `AC-RAIL-007`、`TDD-RAIL-002` 至 `TDD-RAIL-007`。收单专项当前只证明 capture/dispute、商户清结算和 PCI/外部规则设计边界。 | VCC/全球账户依赖 P0/P1 可复用能力和业务专项 Execution Grant；未授权前不得进入生产代码、测试代码、DDL/H2 schema 或运行配置写入。收单不得进入实现授权，除非用户后续明确重新打开优先级。 |

## 7. 覆盖索引明细

本节保留 B1 至 B8 的 TDD 覆盖索引和历史任务编号，用于反查设计、测试和既有局部基线；它不是当前默认授权计划。进入编码前，仍必须从 A0 至 A4、B7、B8 或 P2 中选择一个 MVP 任务切片，并由 Execution Grant 明确 `mvpScenario`、写入范围和验证命令。

### B1 覆盖索引：DSL 契约与枚举红线

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B1-01 | 重建核心测试骨架和命名规范。 | `tests/src/test/java/com/wind/funds/dsl` | TDD 设计、DSL 设计。 | 测试目录为空时先建立最小失败样例。 | 建立按 DSL/枚举/JSON 分层的测试包，不迁回旧测试源码。 | `just test-core` |
| B1-02 | 固化 `FundsInstructionSpec` 语义。 | `tests/src/test/java/com/wind/funds/dsl`、必要的 `core/src/main/java` | DSL 语义结构、JSON 契约。 | 构造最小合法指令、缺字段、非法金额、非法主体用例。 | 补齐或校准 Spec 校验行为。 | `just test-core` |
| B1-03 | 证明 `transactionType` 不承载生命周期事件。 | `tests/src/test/java/com/wind/funds/dsl`、必要枚举。 | DSL 第九章、第十章。 | 将授权生命周期事件误塞入交易类型应失败。 | 保持交易类型与事件类型分离。 | `just test-core` |
| B1-04 | 固化授权 `eventType` 生命周期边界。 | `tests/src/test/java/com/wind/funds/dsl`、必要枚举。 | DSL 授权交易用例。 | `PENDING`、`SETTLE`、`REVERSAL`、`REFUND`、`EXPIRE` 的合法/非法组合，以及拒付不强制落独立 `CHARGEBACK` 事件。 | 必要时补 `EXPIRE` 枚举或先形成失败基线；拒付通过退款承接口径表达。 | `just test-core` |
| B1-05 | 重建 JSON 契约样例解析测试。 | `tests/src/test/java/com/wind/funds/dsl`、`tests/src/test/resources/dsl-contract-cases` | DSL JSON 契约用例。 | JSON 样例无法解析、字段含义不一致、枚举不匹配。 | 让 DSL 样例能推导业务流程和 TDD 断言。 | `just test-core` |
| B1-06 | 固化 Route DSL 和 Route Replay 类型契约。 | `tests/src/test/java/com/wind/funds/dsl`、必要的 `core/src/main/java` | DSL Route、Route Replay、不变量。 | 外部账户或工具进入 ledger node、缺快照 Route Replay 仍成功、route code 漂移。 | 补 `RouteDslContractTests` 或等价契约测试，明确 Route Replay 不等同于交易投影重放、余额重建或归档续跑。 | `just test-core` |
| B1-06A | 固化 PaymentInstrument Route DSL 契约。 | `tests/src/test/java/com/wind/funds/dsl`、必要的 `core/src/main/java` | `DSL-PAYMENT-INSTRUMENT-ROUTE-001`、`DSL-PAYMENT-INSTRUMENT-FAIL-001`、`DSL-PAYMENT-INSTRUMENT-REPLAY-001`。 | 支付工具、外部账户或通道 token 被写成 ledger subject；routing decision 缺资金责任决策、优先级或选择原因；敏感字段进入快照；工具换绑后按当前绑定重路由。 | 补 `PaymentInstrumentRouteDslContractTests` 或等价契约测试，固化 `PaymentInstrumentRef`、`ExternalAccountRef`、`RoutingDecision`、`FundingAllocationDecision` 和 route snapshot 不变量。 | `just test-core` |
| B1-07 | 固化 Posting/Ledger DSL 契约。 | `tests/src/test/java/com/wind/funds/dsl`、必要的 `core/src/main/java` | DSL Posting、LedgerEntry、账务不变量。 | posting plan 不平衡仍可构造、entry 缺主体/账目/币种/周期仍通过。 | 补 posting、entry、digest 和账本周期契约测试。 | `just test-core` |
| B1-08 | 固化 SettlementPolicy DSL 契约。 | `tests/src/test/java/com/wind/funds/dsl`、必要的 `core/src/main/java` | DSL SettlementPolicy、结算策略边界。 | 策略解析失败被静默按实时处理。 | 补 `SettlementPolicySpecTests` 或等价策略解析红线。 | `just test-core` |
| B1-09 | 固化金额临界值契约。 | `tests/src/test/java/com/wind/funds/dsl`、必要的 `core/src/main/java` | TDD 5.1 金额临界值通用矩阵。 | 0 金额、负金额、超币种精度、超系统上限或多 leg 合计不闭合仍可入账。 | 补 `FundsAmountBoundaryContractTests` 或等价契约测试，统一最小单位、精度、上限和累计闭合校验。 | `just test-core` |
| B1-10 | 固化权益快照 DSL 契约。 | `tests/src/test/java/com/wind/funds/dsl`、`tests/src/test/resources/dsl-contract-cases`、必要的 `core/src/main/java` | `FundsBenefitSnapshotSpec`、`DSL-BENEFIT-SNAPSHOT-001`、产品 `AC-BEN-001`、`AC-BEN-012`、`AC-BEN-013`、红线 `RED-050` 至 `RED-059`、`TDD-BEN-001` 至 `TDD-BEN-007`、`TDD-BEN-ENTRY-005`；新增准入项 `AC-BEN-014` 至 `AC-BEN-019`、`RED-060` 至 `RED-066`、`TDD-BEN-RED-020` 至 `TDD-BEN-RED-030` 只作为后续 Phase 2/3 门禁，不倒灌为 B1-10 已完成范围。 | 权益核心字段藏入 `contextVariables`、无权益交易空值语义不成立、金额不闭合、闭合角色缺失或混用、组件重复、稳定摘要未覆盖权益快照差异、用户侧不返券和资金侧不冲补贴无法分开表达、请求态快照被误判为生产 Done；B1-10 越权修改交易服务入口、Request/DTO、route/posting/replay 或主链路实现。 | 补 `FundsBenefitSnapshotSpec`、组件、闭合角色、引用、退款策略、退款决策来源字段、稳定摘要边界和权益枚举的契约测试；Phase 1 只证明 DSL 承载和请求摘要可区分，不宣称 route/posting/replay 已闭合，不改直接交易或授权交易服务入口；`RED-058` 只作为生产 Done 门禁登记；交付结论只能写“契约承载 Done”。后续 Phase 2/3 必须另起 Execution Grant，并补权益准入证明。 | `just test-core` |

### B2 覆盖索引：钱包账户、账本和余额投影基础

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B2-01 | 重建账户角色与建账规则。 | `wallet-*`、`ledger-*`、`tests/src/test/java` | 产品账户/钱包/账本概念、系分表设计。 | 资金账户、信用账户、预算组、平台账户角色缺失或混用。 | 校准账户创建、角色标识、缺账本失败行为。 | `just test-ledger` |
| B2-02 | 固化显式建账红线。 | `ledger-*`、`tests/src/test/java` | 账本与账本周期设计。 | 未建账本直接交易应失败。 | 保持建账动作可追溯，不隐式创建关键账本。 | `just test-ledger` |
| B2-03 | 固化账本周期和 posting 平衡。 | `ledger-*`、`tests/src/test/java` | 账本周期语义矩阵、账务规则矩阵。 | 周期混用、借贷不平、entry 字段缺失。 | 补齐周期隔离、posting plan 平衡和分录完整性。 | `just test-ledger` |
| B2-04 | 固化余额投影边界。 | `ledger-*`、`tests/src/test/java` | 余额投影、余额日志。 | 投影失败误回滚事实、余额日志被当事实源。 | 保持事实追加、投影可重建、日志只读辅助。 | `just test-boundary` |
| B2-05 | 建立余额断言支撑。 | `tests/src/test/java` | TDD 资金断言红线。 | 只断言状态、不断言余额桶。 | 补公共断言支撑，后续 MVP 任务复用。 | `just test-boundary` |
| B2-06 | 固化支付工具、绑定和资金责任解析关系。 | `wallet-*`、`tests/src/test/java` | 产品 `AC-PI-*`、系分支付工具状态与路由准入、`GAP-PI-001`、`GAP-WALLET-001`、`GAP-WALLET-001A`、`GAP-WALLET-002`。 | 工具状态不可用、方向不匹配、RECEIVE/PAY/AUTHORIZE/REFUND/WITHDRAW 动作能力缺失、资金责任缺失或不唯一、账户能力不足仍进入 route；内部余额钱包、信用额度账户、返利钱包或商户钱包被强制包装为 `PaymentInstrument` 后丢失账户主体语义；敏感信息未脱敏；绑定历史被覆盖；预算组或 Spend Rule 被输出为最终资金责任主体；调用方绕过 application facade 拼装资源服务完成工具准入或资金责任解析；继续把关系目标长期限制为 `fundingAccountId`，却在产品或 DSL 中声明可解析到信用账户或平台账户角色。 | 补 `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentCapabilityApplicationService` / `FundingResponsibilityResolutionApplicationService` 等价测试，证明工具只做引用、工具动作能力只做准入、绑定只做候选、资金责任解析关系不扣款不写账，最终只解析到资金账户、信用账户或平台账户角色解析后的平台资金账户；若应用入口接收内部余额、信用额度、权益或外部工具等业务入口参数，必须证明其只在 application facade 解析，输出 `SubjectRef`、`PaymentInstrumentRef`、`BenefitSnapshot` 或 `FundingAllocationDecision`，而不是替代交易内核主体；若支持信用账户或平台账户角色作为目标责任主体，必须把 `fundingAccountId` 兼容字段演进为 `targetSubjectType + targetSubjectId` 或在 Execution Grant 中声明只支持资金账户，并同步 DTO、Entity、H2 schema、摘要和测试。 | `just test-boundary` |
| B2-07 | 收敛预算组账务主体化兼容缺口。 | `wallet-*`、`ledger-*`、`transaction-*`、`tests/src/test/java`，具体写入范围由 Execution Grant 决定 | 产品预算组 / Spend Rule 边界、DSL BudgetGroup 上下文、系分账户服务边界、`GAP-WALLET-003`、本轮代码 CR。 | `FundsSubjectType.BUDGET_GROUP`、预算组余额查询或预算组 ledger 初始化被当作目标态可记账主体；预算组成为 route leg、posting 或 LedgerEntry 主体；预算组额度调整绕过 Spend Rule 控制视图；`FundsAmountBoundaryContractTests`、`PaymentInstrumentRouteDslContractTests`、`ControlAccountLedgerInitializationTests` 或交易 flow support 继续把预算组入账路径当目标态保护。 | 明确 `BUDGET_GROUP` 是兼容枚举、迁移别名、只读查询过滤条件还是待删除目标；补预算组不初始化 ledger bucket、不作为 LedgerEntry 主体、预算控制只走 Spend Rule 控制视图的 Red/Green 证据；同步收敛 `DefaultFundsAccountQueryServiceImpl`、`BudgetGroupServiceImpl`、`DefaultLedgerProfileServiceImpl`、`DefaultLedgerTransactionPostingServiceImpl`、`RouteSubjectSupport`、`DefaultRouteReplayService`、`FundsBalanceControlInstructionConverter` 和相关测试中的旧语义，或在 Execution Grant 中明确哪些路径只是兼容验证。 | `just test-ledger`、`just test-boundary` |

### B3 覆盖索引：直接交易

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B3-01 | 重建直接交易指令转换测试。 | `transaction-*`、`tests/src/test/java` | DSL 直接交易、系分交易编排。 | 充值、付款、商户订单付款、转账、提现转换错误。 | 校准 converter、amount、subject、route 快照。 | `just test-transaction` |
| B3-02 | 固化主路径资金流。 | `transaction-*`、`tests/src/test/java` | 产品流程、账务规则矩阵。 | 资金流方向、账本余额桶、平台账户处理错误。 | 逐步骤断言交易、route、entry、余额。 | `just test-business-flow` |
| B3-03 | 固化退款、手续费、退费和原路径 replay。 | `transaction-*`、`tests/src/test/java` | DSL 场景账务规则矩阵。 | 退款超额、退费方向错误、replay 丢快照。 | 保持原路径、原费项和幂等 replay。 | `just test-transaction` |
| B3-04 | 固化异常与红线。 | `transaction-*`、`tests/src/test/java` | TDD 异常路径。 | 错币种、受控负余额、重复请求、缺主体。 | 补失败原因、幂等键和余额不变断言。 | `just test-business-flow` |
| B3-05 | 固化含权益直接交易资金流。 | `transaction-*`、`tests/src/test/java` | `DSL-BENEFIT-MERCHANT-DISCOUNT-001`、`DSL-BENEFIT-PLATFORM-SUBSIDY-001`、`DSL-BENEFIT-PLATFORM-NO-SETTLEMENT-001`、`DSL-BENEFIT-PREPAID-VOUCHER-001`、`DSL-BENEFIT-PARTIAL-REFUND-001`、`TDD-BEN-ENTRY-001`、`TDD-BEN-ENTRY-002`。 | 商户让利生成 ledger entry、平台补贴与本金净额混记、储值券按普通优惠券入账、退款按当前营销规则重算、权益累计超额、零实付选型未确认仍入主链路、同一业务流水不同权益资金事实复用幂等结果；为优惠券、代金券或补贴新增直接交易权益专用服务入口。 | 不新增直接交易权益专用服务入口；`pay` 承接含权益支付时只能引用权益资金事实、历史摘要或等价不可变事实；含权益生产链路必须固化 route snapshot、交易事实、独立权益资金事实或等价不可变存储之一；幂等摘要纳入权益资金事实摘要；`refund` 承接含权益退款，优先引用原交易、原 route snapshot、原权益资金事实和本次退款决策引用；route/posting 只消费原权益资金事实、历史摘要或受控补充事实；`NO_LEDGER` 只入展示和对账解释；平台补贴或储值券必须形成可追溯资金影响或被专业口径阻断。 | `just test-business-flow` |

### B4 覆盖索引：授权交易

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B4-01 | 重建授权批准与授权拒绝。 | `transaction-*`、`core`、`tests/src/test/java` | DSL 9.2 授权交易、VCC 场景。 | 授权批准不冻结、授权拒绝写 route/entry。 | 保持批准冻结、拒绝仅记录原因且无资金扣划。 | `just test-transaction` |
| B4-02 | 重建撤销、完成、退款和拒付承接。 | `transaction-*`、`tests/src/test/java` | 授权组合场景、账务矩阵。 | 撤销后错误完成、完成后错误释放、多次清算/退款累计错误、拒付被当成授权拒绝或普通退款。 | 校准状态机、授权释放、实际扣款、退款/拒付承接和累计金额闭合；即使拒付底层复用退款终态，也必须保留拒付原因、凭证、外部引用、审计上下文和投影可区分性。 | `just test-business-flow` |
| B4-03 | 补授权过期独立语义。 | `transaction-*`、`core`、`tests/src/test/java` | `GAP-AUTH-001`、`TDD-AUTH-011`、`TDD-ROUTE-008`、`TDD-RED-016`。 | `EXPIRE` 与 `REVERSAL` 混用，过期释放已完成金额。 | 已由 `b0666ba` 完成：新增 `EXPIRE` 事件、`EXPIRED` 状态、`expire` 服务入口、请求模型、route replay、生命周期金额校验和授权流程测试；后续只做回归维护或另行授权扩展。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-one DefaultRouteReplayServiceTests tests`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd` |
| B4-04 | 校准 `settle` 强制完成模式。 | `transaction-*`、`tests/src/test/java` | `GAP-AUTH-002`、force settle 设计。 | 无前置授权完成缺少 FORCE 模式、受信策略或审批快照、原因、上限、外部原事实、凭证或审计；FORCE 模式回退普通授权完成路径。 | 已由 `616dac1` 和 `3825466` 完成首轮：普通完成继续要求 `authorizationTransactionSn`，FORCE 模式不依赖内部原授权流水，走外部原事实引用和受信策略校验；后续只做回归维护或另行授权扩展策略引擎、审批快照、额度窗口或 overcapture。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd` |
| B4-05 | 校准 `settleRefund` 无授权退款模式。 | `transaction-*`、`tests/src/test/java`，必要时涉及请求契约 | `GAP-AUTH-004`、`AC-AUTH-012`、`TDD-AUTH-006`、`TDD-RED-017A`、B4 准入卡 `noAuthRefundContractCandidate`。 | 无前置授权但有可追溯外部引用时无法退款；缺外部引用、缺原因、缺审计、携带内部授权流水或敏感上下文仍静默退款；实现为了退款补造授权占用或按当前绑定重选路。 | 已完成首轮并经 CR 收缩为资金层最小契约：以 `authorizationTransactionSn` 为空判定 no-auth refund，请求携带 `externalReferenceSn`、原因和操作者/审计，请求契约不恢复 `refundMode`，`NO_AUTH` 只作为内部归类标签；普通授权链退款继续要求 `authorizationTransactionSn`，NO_AUTH 模式不得携带内部授权流水、不得查询原授权账本交易；route resolver 在内部 `REFUND_MODE` 缺失时可由 `EXTERNAL_TRANSACTION` reference 推断 no-auth refund 路由，显式 `DISPUTE` 或其他退款归类不被覆盖；独立退款事实、route snapshot、ledger transaction、posting、entry、余额变化和失败无副作用已进入目标测试。后续运营审批、人工差错、累计退款跨请求聚合控制、查询投影解释或外部规则需另起 Grant。 | `just verify-slice AuthorizationFundsInstructionRouteResolverTests tests`、`just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-boundary`、`just pmd` |
| B4-06 | 固化 VCC / Spend Controls 扩展边界。 | `transaction-*`、`tests/src/test/java` | Spend Controls 为扩展能力。 | 将 spend controls 当成资金主链路 P0。 | 默认只保留边界测试；未明确启用发卡产品时不做实现。 | `just test-boundary` |
| B4-07 | 明确 chargeback 不落 `FundsAuthorizationTransactionService#chargeback`。 | `transaction-*`、`tests/src/test/java`，后续完整 case 仍需独立授权 | `GAP-AUTH-003`、拒付业务事实、B4 准入卡 `disputeSemanticAlignmentGrantCandidate`。 | 测试或任务强制要求调用 `chargeback` 服务入口，或把拒付结果只保存成无法区分的普通退款。 | 已由 `949b24a` 完成首轮：默认以 `settleRefund / AUTH_REFUND` 为拒付/争议承接目标态主入口，既有 `chargeback` 入口只作为兼容、显式事件或内部适配资产；争议退款的原因、凭证、外部引用、内部 `DISPUTE` 标签、审计上下文、请求摘要和 route/ledger/posting/entry 事实可与普通退款、NO_AUTH 退款和授权拒绝区分。完整 dispute case、独立 chargeback 一等 API、清结算追偿和外部规则仍需另起 Grant。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd` |
| B4-08 | 固化授权并发竞争红线。 | `transaction-*`、`core`、`tests/src/test/java` | TDD 13.5、授权生命周期。 | 同一授权的完成、撤销、过期、退款并发导致重复入账、重复释放或剩余为负。 | 已由 `47c5269` 完成首轮：同一授权的 settle / expire / reversal 并发竞争只允许一个赢家，失败方无 route、posting、ledger entry、projection 或余额副作用；后续跨节点锁、数据库唯一约束、版本字段或更深退款并发扩展需另起 Grant。 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd` |
| B4-09 | 固化授权占券和权益生命周期边界。 | `transaction-*`、`core`、`tests/src/test/java` | `DSL-BENEFIT-AUTH-HOLD-001`、`TDD-BEN-AUTH-*`、`TDD-BEN-ENTRY-003`、`TDD-BEN-ENTRY-004`、`RED-053`。 | 授权拒绝、工具准入失败或余额不足后仍核销权益；授权完成和过期并发导致同一权益重复核销或释放；完成时按当前券规则重算；新增授权权益专用服务入口或授权后续事件接收当前权益结果。 | 不新增授权权益专用服务入口；`authorize` 如需表达授权占券，应在对应 Execution Grant 中新增权益占用引用、原权益资金事实引用或等价一等字段，不恢复旧 `benefitSnapshot`；`reversal`、`expire`、`settle` 和 `settleRefund` 读取原授权事实、原权益资金事实、占用引用或历史摘要；授权阶段只固化占用引用，完成按原事实核销，撤销或过期释放剩余占用；失败事件无 route/posting/entry 副作用。 | `just test-business-flow` |
| B4-10 | 固化授权支付工具应用入口。 | `transaction-face`、`wallet-face`、`transaction-impl`、`wallet-impl`、`tests/src/test/java`，具体写入范围由 Execution Grant 决定 | 产品 `AC-PI-010`、`AC-AUTH-000`、DSL 支付工具入口到账户主体内核转译、系分 4.1.2、`GAP-AUTH-005`。 | 外部业务必须传内部 `FundsAccountId` 才能授权；工具状态、绑定、预算组、Spend Rule 或资金责任缺失仍进入内核；工具准入失败生成 route/posting/entry；批准后 route leg 使用支付工具、预算组或 Spend Rule 作为主体；直接把现有 canonical `authorize` 请求改成支付工具引用字段。 | 新增 `authorizeByInstrument` 或等价 application facade，先完成工具、绑定、使用主体、预算组、Spend Rule、资金责任和账户能力校验；批准后委派账户主体型授权内核；拒绝只记录拒绝事实和原因；PaymentInstrumentRef、BindingSnapshot、FundingAllocationDecision 和拒绝原因进入快照或审计；保留账户主体型授权内核作为 canonical 入口。 | `just test-transaction`、`just test-boundary` |

B4-10 边界：后续 Execution Grant 只能选择新增外层 `authorizeByInstrument`、`AuthorizationAdmissionApplicationService` 或等价 facade，并必须保留账户主体型授权内核；不得用支付工具引用替换 canonical 请求字段。

人工确认点：授权占券字段和权益 hold/release/write-off 外部引用，授权支付工具应用入口的服务命名 / Request / DTO / 幂等摘要和快照字段，钱包 application facade 命名，资金责任目标字段是否从 `fundingAccountId` 演进为目标主体引用，`FundsSubjectType.BUDGET_GROUP` 兼容策略；`EXPIRE` 枚举和 `expire` 服务入口已由 B4-TRX-EXPIRE 完成，强制完成首轮请求契约、`authorizationTransactionSn` 条件化规则、策略/上限受信来源和审计最小集已由 B4-FORCE-SETTLE 完成，`settleRefund` 无授权退款的空原授权流水判定、`NO_AUTH` 内部归类标签、`externalReferenceSn`、退款原因、操作者/审计字段和内部授权流水禁用规则已由 B4-NO-AUTH-REFUND 首轮完成并经 CR 收缩；后续只在扩展策略引擎、审批快照、额度窗口、overcapture、累计退款控制、并发、投影解释或 facade 生命周期时重新确认。VCC / Spend Controls 默认只做边界测试，只有明确启用发卡产品才进入实现范围。

### B5 覆盖索引：余额控制

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B5-01 | 固化冻结/解冻控制语义。 | `transaction-*`、`tests/src/test/java` | 余额控制 DSL、资金红线。 | 冻结表达消费、跨主体冻结、多次解冻超额。 | 保持同主体 `AVAILABLE <-> FROZEN` 控制。 | `just test-balance-control` |
| B5-02 | 固化冻结后提现与组合路径。 | `transaction-*`、`tests/src/test/java` | 产品验收用例。 | 提现忽略冻结、冻结释放后余额桶错误。 | 逐步骤断言可用、冻结、实际扣划。 | `just test-balance-control` |
| B5-03 | 固化资金账户余额调整、信用账户额度、预算组额度。 | `transaction-*`、`tests/src/test/java` | 账户/额度/预算概念边界。 | 余额调整缺来源、审批、凭证或审计，跨主体价值转移误用 adjust，额度与余额混用。 | 保持资金账户余额调整、信用账户额度和预算组额度分离；adjust 必须有准入来源和审计；完整对账差错闭环在B7 验证。 | `just test-balance-control` |
| B5-04 | 固化 FX 与余额控制边界。 | `transaction-*`、`tests/src/test/java` | TDD 红线。 | 余额控制承接 FX 或隐式换汇。 | 余额控制只处理同币种余额桶。 | `just test-boundary` |
| B5-05 | 固化冻结、解冻和提现并发竞争红线。 | `transaction-*`、`tests/src/test/java` | TDD 13.5、余额控制组合场景、冻结动作明细。 | 同一冻结来源被解冻和提现确认并发关闭，导致重复释放、重复关闭或 `FROZEN` 为负。 | 通过冻结动作明细、剩余金额校验、动作幂等键和唯一约束保证同一冻结来源只被关闭或释放一次。 | `just test-balance-control` |
| B5-06 | 固化外部余额异常纠偏和受控负可用。 | `transaction-*`、`tests/src/test/java`；若落表则需独立确认 DDL/H2 范围 | `AC-CTRL-013`、`DSL-BALANCE-CONTROL-EXTERNAL-DEFICIT-ADJUST-001`、`TDD-CTRL-012`、`TDD-CTRL-ERR-007`、`TDD-RECON-016` | 上游未终局、无外部余额快照、无差错单或审批、无凭证、跨主体转移损失、纠偏后绕过运行时负余额策略继续交易。 | 首轮已通过 `BALANCE_CONTROL / BALANCE_ADJUST` 固化外部余额异常审计字段、终局外部事件、余额快照、对账差错引用、责任引用和受控负可用策略透传；B7 差错单闭环、独立审计表和运营审批仍后续承接。 | `just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-balance-control`、`just test-reconciliation` |

### B6 覆盖索引：Route Replay、交易投影和余额日志

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B6-01 | 固化 route resolver 选择和无副作用。 | `transaction-*`、`tests/src/test/java` | 路由系分、路由能力边界。 | `supports` 写事实或改变状态。 | 保持 resolver 只判定路径，不写交易/账本事实。 | `just test-boundary` |
| B6-02 | 固化缺快照 Route Replay 失败。 | `transaction-*`、`tests/src/test/java` | Route Replay 设计。 | 缺 route/amount/subject 快照仍按当前绑定重放成功。 | Route Replay 必须依赖原始快照并输出明确失败原因；不得退化为重新路由。 | `just test-business-flow` |
| B6-03 | 固化交易投影只读和有界重放。 | `transaction-*`、`tests/src/test/java` | 交易投影重放。 | 无范围重放、投影水位复用余额水位或归档 checkpoint。 | 引入范围、模式、checkpoint 和差异报告红线；交易投影重放不重新入账、不改余额。 | `just test-boundary` |
| B6-04 | 固化余额日志边界。 | `ledger-*`、`tests/src/test/java` | 余额日志设计。 | 余额日志失败导致事实回滚，或被用于修复余额。 | 保持日志为观测与审计辅助，不作事实源。 | `just test-boundary` |
| B6-05 | 固化支付工具换绑后原路径回放。 | `transaction-*`、`tests/src/test/java` | `DSL-PAYMENT-INSTRUMENT-REPLAY-001`、TDD 支付工具回放红线。 | 原交易后工具换绑、解绑、暂停或默认资金责任变化，退款、撤销、退费或拒付按当前绑定重新选路。 | Route Replay 必须使用原 route snapshot、原工具快照和原 funding allocation；当前绑定只作为审计上下文，不参与资金路径。 | `just test-business-flow` |
| B6-06 | 固化权益资金事实回放和缺事实处理。 | `transaction-*`、`tests/src/test/java` | `DSL-BENEFIT-REFUND-NO-COUPON-001`、`DSL-BENEFIT-REFUND-RETAIN-SUBSIDY-001`、`DSL-BENEFIT-MISSING-FUNDING-FACT-REPLAY-001`、`TDD-BEN-ENTRY-002`、`TDD-BEN-ENTRY-004`、`TDD-BEN-REFUND-004`、`TDD-BEN-REFUND-005`、`TDD-BEN-REPLAY-002`。 | 退款、撤销、过期或拒付按当前营销规则重算；缺原权益资金事实、历史摘要或受控补充事实仍静默放行；不退券和不冲补贴混成一个布尔值；多次部分退款累计超额或尾差随机分摊；仅用外部渠道流水替代原资金交易或原权益资金事实引用；含权益退款请求承载新的权益计算结果；资金底座自行判断券是否可退。 | replay 使用原权益资金事实、历史摘要、组件摘要、退款策略和业务层本次退款决策引用；直接退款 Request 如需扩展，应补原资金交易、原 route snapshot、原权益资金事实、业务决策流水或 replay 策略引用，不承载新的权益计算结果；缺事实失败或进入人工处理；累计金额、补贴冲回、储值恢复和不可退权益都可追溯；多组件部分退款必须有确定性尾差规则且重试结果稳定。 | `just test-business-flow` |
| B6-07 | 固化交易投影和运营时间线可解释性。 | `transaction-*`、`tests/src/test/java` | 产品使用者可解释性矩阵、系分 05 可解释输出、`GAP-OPS-001`。 | 查询或投影只返回技术状态，无法说明金额来源、事实状态、展示状态、操作状态、失败阶段、不可操作原因、下一步动作、证据脱敏引用或外部规则核验状态；授权占用、冻结、待清算或出款受理被展示为完成资金结果；解释视图反写资金事实。 | 用户账单、商户账单、运营时间线和交易投影必须只读生成解释摘要，至少包含业务键、资金交易、route snapshot 摘要、账务引用、事实状态、展示状态、操作状态、失败原因、不可操作原因、下一步动作、脱敏证据引用和外部规则核验状态；不修改交易、账本或余额事实。 | `just test-boundary` |

### B7 独立任务索引：清结算与对账

B7 在本 change 中只保留边界、计划和设计评审输入，不表示整体编码准入已经打开。冻结基线中 `reconciliation-face` 和 `reconciliation-impl` 已有模块骨架和出款前准入候选实现，已完成专项验证，可作为后续候选输入，但不能作为完整清结算、对账或出款生命周期 Done 证据。继续编码前仍必须另起独立 OpenSpec change，确认模块命名、表设计、状态机、接口、是否扩展出款前准入候选能力、DDL/H2、NFR 假设、观测告警和验证命令。

当前 PRD、DSL、系分和 TDD 对清结算与对账已经达到 TDD 分析输入状态：对象边界、资金事实边界、验收矩阵和首批 Red 方向可评审。该状态只允许进入 TDD 分析、研发拆解和 Execution Grant 准备，不允许直接进入生产代码、测试代码、DDL/H2 schema 或运行时配置写入。

| CLS-GATE | 必须写入 TDD 分析输出或独立 Execution Grant |
| --- | --- |
| CLS-GATE-001 Execution Grant 准入 | 设计基线范围、写入范围、禁止范围、验证命令、人工确认点和停止条件。 |
| CLS-GATE-002 首批 Red 准入 | `TDD-B7-RED-001` 至 `TDD-B7-RED-007` 的失败断言、目标测试层级和目标验证命令。 |
| CLS-GATE-003 数据与服务闭环 | 对账批次、可清分明细、清分批次、清算候选、清算批次、结算单、出款单、差错单、追偿单和审计对象的表、状态机、幂等键、DTO、Entity、Mapper、服务和 H2 测试范围。 |
| CLS-GATE-004 运营补事实白名单 | 允许生成资金事实的命令或等价事件、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因、可撤销边界和失败无副作用测试。 |
| CLS-GATE-005 NFR 与可观测性 | 批处理容量、重跑窗口、并发锁、对账文件延迟、回单乱序、告警指标、Runbook 信号、敏感导出和审计留存。 |
| CLS-GATE-006 专业确认状态 | 财务、税务、会计、合规、法务、银行、通道、卡组织、KYC/KYB/AML、客户资金、跨境或外汇规则来源和确认状态。 |
| CLS-GATE-007 使用者解释和安全操作 | 事实状态、展示状态、操作状态、不可操作原因、下一步动作、责任方、到期重查和脱敏证据引用。 |
| CLS-GATE-008 职责分离和证据最小化 | 高危动作发起、复核、审批分离，敏感证据查看、导出、脱敏、水印和审计。 |

| B7 准入门禁 | 必须写入独立 Execution Grant |
| --- | --- |
| 基线引用 | PRD、DSL、系分、TDD、OpenSpec、任务和代码基线提交点。 |
| 写入范围 | 是否复用 `reconciliation-face`、`reconciliation-impl`，以及允许新增或修改的 API、DTO、Entity、Mapper、DDL/H2 schema、fixture、服务和测试目录。 |
| 首批 Red | `TDD-B7-RED-001` 至 `TDD-B7-RED-007` 必须先落地，且先于 B7-01 至 B7-07 任一 Green。 |
| 运营补事实白名单 | 允许生成资金事实的命令或等价事件、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因、可撤销边界和失败无副作用测试。 |
| NFR 与观测 | 批处理容量、重跑窗口、并发锁、对账文件延迟、回单乱序、告警指标、Runbook 信号、敏感导出和审计留存。 |
| 专业确认状态 | 财务、税务、会计、合规、法务、银行、通道、卡组织、KYC/KYB/AML、客户资金、跨境或外汇规则来源和确认状态。 |
| 使用者解释与职责分离 | 事实状态、展示状态、操作状态、不可操作原因、下一步动作、责任方、到期重查、脱敏证据引用、高危动作发起/复核/审批分离、敏感导出水印和审计。 |

| CLS-00 | 纳入出款前准入候选实现并固化候选基线。 | `reconciliation-face`、`reconciliation-impl`、`tests/src/test/java/com/wind/funds/reconciliation` | `AC-SET-006` 至 `AC-SET-009`、`TDD-SETTLE-004`、`TDD-SETTLE-005`、`GAP-CLS-002`。 | 只有 `ruleEvidenceRef` 仍被视为已核验、创建前检查强制要求 `payoutSn`、准入结果缺事实/展示/操作状态、准入检查写入账务事实。 | `checkPayoutPreflight` 候选实现必须同步纳入结构化规则核验证据、预创建检查、服务端解释状态、阻断原因和只读事实断言；随后再补出款表、回单、金额不一致和查询解释模型。 | `just test-one PayoutPreflightServiceTests tests` |

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B7-01 | 确认独立模块、表和状态机。 | 待确认的 `reconciliation-*` 扩展或独立包 | 系分 03、产品清分/清算/对账。 | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、对账批次、差错单或追偿单混成一个对象。 | 人工确认后建立独立对象边界。 | 待确认 |
| B7-02 | 固化可清分准入、清分批次和清算候选排除。 | 待确认 | 可清分规则、清分批次、清算候选。 | 未完成交易入可清分明细、清分批次确认即入账、冻结或重大差错交易入清算候选。 | 可清分只来自成功交易、已过账 `CLEARING` 分录和完整来源快照；清分批次只固化归类和规则版本；候选只来自已确认清分结果且满足可清算规则。 | 待确认 |
| B7-03 | 固化清算批次确认、结算锁定、出款成功/失败和追偿。 | 待确认 | 清算批次、结算单、出款单、追偿单边界。 | 候选入池即清算、清算前置对账缺失、出款失败直接改历史分录，或重复出款。 | 清算批次确认才触发 `CLEARING -> AVAILABLE`；结算锁定、出款单、失败原因和追偿链路各自独立。 | 待确认 |
| B7-04 | 固化对账批次、差错单和核销。 | 待确认 | 对账差错闭环。 | 对账差异直接改分录或余额，或绕过差错闭环直接调用 adjust。 | 强制差错单、审批、凭证、调账/冲正、核销、重新对账；差错类资金账户余额调整由闭环触发余额控制 adjust 或批次授权直接交易调账事实。 | 待确认 |
| B7-04A | 固化外部来源标准化、匹配强度和账龄升级。 | 待确认 | PRD 8.3.1、8.4、8.4.1；B7 Round0 准入卡。 | 未验签、解析失败、重复或缺主体映射的来源进入自动对平；候选匹配或人工确认被当作自动对平；等待数据、有条件放行或挂账长期悬挂。 | 来源记录、标准化明细、来源质量结论、`EXACT_MATCH/RULE_MATCH/CANDIDATE_MATCH/MANUAL_CONFIRMED/UNMATCHED` 匹配强度、账龄桶、SLA、到期升级和阻断对象必须可测；首轮补事实白名单默认关闭。 | 待确认 |
| B7-05 | 固化清结算与对账并发竞争红线。 | 待确认 | TDD 13.5、清结算退款时序、对账重跑。 | 清算候选锁定与退款并发、结算锁定与出款回单/退款并发、对账重跑与差错核销并发导致重复扣减、重复出款或证据覆盖。 | 建立对象级锁定、批次唯一键、候选状态版本、重跑运行记录和差错核销互斥；失败方必须可审计且无副作用。 | 待确认 |
| B7-06 | 固化含权益交易的清结算和对账拆分。 | 待确认 | `DSL-BENEFIT-CLEARING-RECONCILIATION-001`、`AC-BEN-011`、`AC-BEN-013`、`AC-BEN-015`、`TDD-BEN-CLS-*`、`TDD-BEN-RECON-*`、`RED-057`、`RED-059` 至 `RED-062`。 | 营销核销、订单金额、用户实付、商户应收、平台补贴、手续费和退款冲回不一致时被静默补平；商户券被误当平台成本；补贴冲回、不可退权益、负债恢复或展示项混入订单正向闭合；`CONTRACT_ONLY` 夹具或专业确认缺失被当作清结算 Done。 | 清分、清算、结算和对账明细拆出权益金额项；闭合角色决定金额项进入订单、退款、补贴冲回、负债恢复或展示解释；差异进入差错单和审计，不直接改历史资金事实；进入 B7 前必须确认夹具级别、权益事实源和专业确认状态。 | 待确认 |
| B7-07 | 固化清结算、对账和出款工作台可解释性。 | 待确认 | 产品使用者可解释性矩阵、系分 03 运营后台、系分 05 告警到 Runbook 动作矩阵、`GAP-OPS-001`。 | 重大差错或前置对账未完成仍展示为可清算、可结算或可出款；出款受理展示为成功；差错缺责任方、阈值、账龄、审批或到期重查；证据原文进入导出或告警；外部规则未核验仍展示可操作；缺事实状态、展示状态或操作状态仍进入商户账单、运营台或审计导出。 | 清分、清算、结算、出款、对账差错和追偿视图必须能解释金额来源、事实状态、展示状态、操作状态、阻断原因、放行依据、审批链、下一步动作、脱敏证据引用、外部规则核验状态和恢复验收。 | 待确认 |
| B7-08 | 固化两级代理收益分润生产可用验收用例。 | 待确认 | PRD `AC-CLR-007` 至 `AC-CLR-009`、`AC-SET-010`、`CLS-GATE-009`，DSL 收益分润清算确认，系分收益分润与激励结算验收对象，TDD-B7-REVSHARE-*。 | 收益应得项清分确认直接入账；平台员工分润用户代理佣金但缺两级归因、规则版本、GMV 阶梯或利润口径、审批、账户解析或专业确认仍清算；退款、争议或规则重跑覆盖旧批次和旧审批。 | 将用户代理佣金、平台员工二级分润和 KPI 激励拆成收益金额项；清分和候选阶段不改变余额；清算确认才委派交易层生成标准资金事实；缺证据时进入差错、排除或阻断，不实现代理/KPI/税务/营销规则引擎。 | 待确认 |

B7 首批 Red 集必须在 B7-01 至 B7-08 任一 Green 实现前完成：`TDD-B7-RED-001` 清分候选直接入账、`TDD-B7-RED-002` 出款前准入未知或失败仍提交、`TDD-B7-RED-003` 非白名单运营动作生成资金事实、`TDD-B7-RED-004` 外部非终态被当成功、`TDD-B7-RED-005` 差错未重新对账即关闭、`TDD-B7-RED-006` 缺使用者解释仍可放行、`TDD-B7-RED-007` 单人绕过职责分离或查看敏感原文。当前 `B7-RECON-DIFFERENCE-MVP` 首切片还必须以 Round0 准入卡中的 `R0-B7-RECON-004` 至 `R0-B7-RECON-006` 覆盖外部来源未验证、候选匹配误对平和账龄超期升级。若进入收益分润验收包，还必须补 `TDD-B7-REVSHARE-001` 至 `TDD-B7-REVSHARE-003`，证明收益应得项清分不入账、收益清算确认才入账、退款争议和规则缺失会阻断。首批 Red 未落地时，B7 只能继续做设计或契约草案，不能声明清结算、对账、出款或收益分润生命周期生产可用。

人工确认点：是否新建模块、模块命名、表命名、状态机、是否扩展出款前准入候选能力、权益清分字段、收益应得项对象、收益参与方账户解析、两级归因快照、权益/收益对账差错类型、运营补事实命令白名单、使用者解释字段、职责分离策略、证据最小化策略、NFR 假设、外部规则确认状态、是否进入本轮开发范围。

### B8 独立任务索引：资金数据治理边界

B8 在本 change 中只保留边界、计划和设计评审输入，不表示编码准入已经打开。当前代码已有 `governance-face` 和 `governance-impl` 交易投影重放骨架，可作为交易投影治理重放的候选落点；进入编码前仍必须另起独立 OpenSpec change，确认资金数据治理任务模型、表设计、审批、回滚、NFR 假设、证据最小化、外部规则核验、观测告警和生产门禁；指标只保留边界，不实现报表指标模块。

当前 PRD、DSL、系分和 TDD 对资金数据治理已经达到 TDD 分析输入状态：产品 02 承接余额检查点、watermark、余额快照和交易投影重放，产品 03 承接清结算批次视图重放和差异报告，产品 05 承接 `GOV-GATE-*`、指标水位隔离和大数据消费边界；产品 04 仅保留拆分索引。该状态只允许进入 TDD 分析、研发拆解和 Execution Grant 准备，不允许直接进入生产代码、测试代码、DDL/H2 schema 或运行时配置写入。

| GOV-GATE | 必须写入 TDD 分析输出或独立 Execution Grant |
| --- | --- |
| GOV-GATE-001 Execution Grant 准入 | 设计基线范围、写入范围、禁止范围、验证命令、人工确认点和停止条件。 |
| GOV-GATE-002 首批 Red 准入 | `TDD-B8-RED-001` 至 `TDD-B8-RED-005` 的失败断言、目标测试层级和目标验证命令。 |
| GOV-GATE-003 治理物理落点 | 复用 `governance-face/governance-impl`、扩展既有包或新增独立模块的取舍、依赖方向、公共契约、DTO、Entity、Mapper 和边界测试范围。 |
| GOV-GATE-004 Manifest 与 H2 范围 | 归档任务、运行记录、Manifest、checkpoint、watermark、冷热对象清单、余额快照、差异报告、人工处理、导出快照、消费方登记和审计对象的表、状态机、幂等键和 H2 测试范围。 |
| GOV-GATE-005 dry-run/apply 边界 | dry-run 不推进 checkpoint、watermark、Manifest 或正式投影；apply 需要范围锁、幂等键、审批、差异报告、失败无副作用和回滚/续跑策略。 |
| GOV-GATE-006 只读事实边界 | 归档、余额重建、交易投影重放、差异报告、普通指标快照和大数据消费承接不得创建 route、posting、entry、交易明细、清结算对象或余额修复事实。 |
| GOV-GATE-007 水位隔离和指标边界 | 余额水位、归档水位、交易投影 checkpoint、指标水位和大数据导出消费水位独立；普通指标快照不能替代账本余额快照。 |
| GOV-GATE-008 使用者解释、导出和 Runbook | 差异报告、人工处理、治理导出、Runbook 告警和审计导出必须包含范围、模式、状态、阻断原因、影响范围、责任方、下一步动作、恢复验收和脱敏证据引用。 |
| GOV-GATE-009 大数据消费承接 | 报表数仓、离线指标或经营分析只能通过治理读取、导出快照、Manifest 摘要、脱敏、digest、消费方登记和审计承接。 |

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B8-01 | 确认归档和重放任务模型。 | 待确认的 `governance-*` 扩展或独立包 | 系分 04、归档重放设计。 | 无审批、无范围、无 Manifest 即执行归档/重放。 | 人工确认后建立任务、审批、范围和 Manifest 边界。 | 待确认 |
| B8-02 | 固化归档门禁和水位推进顺序。 | 待确认 | 归档流程。 | 先推水位再写结果、缺差异报告。 | checkpoint、watermark、Manifest、差异报告作为上线门禁。 | 待确认 |
| B8-03 | 固化余额重建和交易投影重放。 | 待确认 | Balance Rebuild、Transaction Projection Replay。 | 余额重建从交易投影、余额日志或报表反推余额；交易投影重放重新入账。 | 保持事实只读、投影可重建、差异可解释；余额重建只从账本分录、检查点、水位和 Manifest 出发。 | 待确认 |
| B8-04 | 固化账本余额快照覆盖模式。 | 待确认 | 系分 04 账本余额快照、`DSL-GOVERNANCE-BALANCE-SNAPSHOT-001`。 | `HOT_ONLY` 强制要求 Manifest、`COLD_MANIFEST` 缺 Manifest 仍通过、`MIXED` 跳过冷热合并摘要校验。 | 建立 `HOT_ONLY`、`COLD_MANIFEST`、`MIXED` 三种覆盖模式校验；冷区和混合覆盖缺 Manifest 不得进入 `VERIFIED`；失败不得推进余额水位。 | 待确认 |
| B8-05 | 固化指标只读和指标水位隔离。 | 待确认 | 指标治理仅列指标项、`DSL-GOVERNANCE-METRIC-SNAPSHOT-BOUNDARY-001`、`TDD-METRIC-003`、`TDD-METRIC-004`。 | 在本模块实现报表指标计算，或指标失败推进余额水位、修改归档 Manifest；普通指标快照成功替代账本余额快照。 | 仅提供业务关心的指标口径输入，具体实现交给报表指标模块；指标水位独立于余额水位、归档 Manifest 和交易投影 checkpoint；普通指标快照不能证明余额正确。 | 待确认 |
| B8-06 | 固化归档、重放和快照范围互斥。 | 待确认 | TDD 13.5、归档门禁、重放差异报告和普通指标快照并发边界。 | 同一范围重复正式 apply、dry-run 推进 checkpoint、水位或 Manifest 被并发任务重复推进；普通指标快照覆盖余额快照状态。 | 建立范围锁、任务幂等键、dry-run/apply 分离、成功后单次推进规则和指标/余额快照状态隔离；失败任务不得推进水位。 | 待确认 |
| B8-07 | 固化归档、重放和指标边界 Runbook 信号。 | 待确认 | 系分 05 告警到 Runbook 动作矩阵、生产门禁、`GAP-OPS-001`。 | 归档或重放告警只有异常堆栈，没有范围、模式、checkpoint、Manifest、差异报告、止血动作或恢复验收。 | 归档、余额重建、交易投影重放和指标边界告警必须输出任务范围、dry-run/apply 模式、checkpoint、watermark、Manifest、差异报告、负责人、止血动作和恢复验收。 | 待确认 |
| B8-08 | 固化归档重放异常人工处理闭环。 | 待确认 | 产品 `AC-ARCH-008`、系分 04 差异报告模型、`TDD-ARCH-009`。 | 缺范围、缺 Manifest、冷热摘要不一致、检查点不连续、重放差异、权限不足或外部规则待确认时，后台人工处理直接修改交易、账目、余额或投影事实。 | 异常必须进入差异报告、阻断原因、影响范围、责任归属、证据引用、人工处理入口和可重跑条件；人工处理只能审批、补证据、调整范围、重跑或关闭差异。 | 待确认 |
| B8-09 | 固化大数据消费边界。 | 待确认 | 产品 `AC-ARCH-009`、`DSL-GOVERNANCE-BIG-DATA-ARCHIVE-BOUNDARY-001`、系分 04 治理读取或导出快照、`TDD-ARCH-010`。 | 报表数仓、离线指标或经营分析绕过治理边界直接扫描在线资金冷归档，或反写交易、账目、余额、清结算、对账、投影事实，或推进资金水位、替代余额快照和交易重放 checkpoint。 | 大数据消费只能通过治理读取、导出快照、Manifest 摘要、脱敏、digest、消费方和审计边界承接；资金冷归档只作为事实留存和重放证据，不作为在线报表库。 | 待确认 |

B8 首批 Red 集必须在 B8-01 至 B8-09 任一 Green 实现前完成：`TDD-B8-RED-001` 缺 Manifest 或 checkpoint 仍归档成功、`TDD-B8-RED-002` 治理任务反写资金事实、`TDD-B8-RED-003` 普通指标快照替代余额确认、`TDD-B8-RED-004` 无范围或无差异报告的正式重放、`TDD-B8-RED-005` 大数据消费绕过治理读取。首批 Red 未落地时，B8 只能继续做设计、契约或 dry-run，不得进入 apply、正式水位推进或生产修复控制面。

人工确认点：生产重放范围、审批策略、回滚策略、账本余额快照覆盖模式字段、Manifest 覆盖策略、指标模块接口边界。

## 8. 依赖、门禁与设计反馈

1. 设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果必须先冻结为独立检查点；当前任务准入以确认时 Git HEAD、OpenSpec 和 Harness 最新任务账本为准；30b1a00、270122e、81a7ecb、4a7ef12、f99800b、9456ab6 和 77bc9f4 只保留为历史准入与局部保护证据；后续具体 MVP 编码任务仍必须通过 Execution Grant 明确授权。
2. 当前执行优先级固定为：P0 资金底座内核优先，P1 交易与读模型扩展次之，P2 业务模式能力包按专项授权接入；文档编号和覆盖索引编号不代表能力优先级。
3. B1、B2 承接 P0 的 DSL、钱包、账本、账目和余额投影基础；B3 至 B6 承接 P1 的直接交易、授权交易、余额控制、Route Replay 与交易投影；B7、B8 分别承接 P0 清结算对账和 P0/P1 治理闭环。
4. B1 覆盖索引是后续所有交易、账本和授权测试的前置门禁。
5. B2 覆盖索引是后续业务流余额断言、账务平衡和组合测试的前置门禁。
6. B1 已有 DSL 契约测试基线，但仍未替代后续变更授权；如继续修改 `core` 公共枚举、Spec 或值对象，Execution Grant 必须显式确认是否允许修改公共契约和枚举，以及允许修改的范围。
7. B4 涉及公共契约、枚举、服务入口和 Request/Query/DTO，必须先经人工确认再改生产代码；强制完成首轮已闭合，后续扩展策略引擎、审批快照、额度窗口或 overcapture 时必须单独授权；`settleRefund` 无授权退款以空原授权流水进入 no-auth 语义，必须有 `externalReferenceSn`、原因和操作者/审计，且不得携带或查询内部授权流水；`settleRefund / AUTH_REFUND` 已能通过争议字段承接首轮拒付/争议可区分性，`DISPUTE` 只作为内部上下文标签；完整 dispute/chargeback case 或独立 `chargeback` 一等目标 API 仍需另起 Grant。
8. B7、B8 属于独立授权域，不得在 A0 至 A4 或 B1 至 B6 覆盖索引中顺手落入交易、钱包或账本入口；进入编码前必须另起独立 OpenSpec change。
9. B7 若允许清结算、对账、出款、差错、补事实、冲正、调账或追偿通过交易层追加资金事实，Execution Grant 必须列出运营补事实命令白名单、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因、可撤销边界和失败无副作用测试；未列入白名单的运营动作只能生成处理单、差异报告或审计记录。
10. 资金数据治理是否先于、并行或后于清结算与对账编码，取决于独立 Execution Grant 和事实边界是否已确认；B8 只能消费 02 和 03 已确认的事实边界、批次摘要、差异报告和只读投影输入，不得用事实留存或重放补造上游事实。
11. B8 进入编码前必须确认治理物理落点：复用既有 `governance-face` / `governance-impl`、扩展已有包，还是新增独立模块；同时确认依赖方向、是否新增公共契约、表结构和 H2 schema、Mapper/Entity 归属、边界测试、指标水位隔离测试和普通指标只读边界。
12. 任一 MVP 任务发现产品、DSL、系分或 TDD 口径冲突时，先记录“设计错漏”，同步修正设计文档和 OpenSpec，再继续编码。
13. 每个 MVP 任务必须先提交 Red 用例，再做最小 Green 实现，最后补充重构、观测、边界和回归验证。
14. 金额临界值按 TDD 5.1 作为所有资金变化测试的公共前置矩阵；并发竞争按 TDD 13.5 分散到授权、余额控制、清结算、对账和归档相关 MVP 任务承接，不得只用顺序用例代替。
15. 权益资金事实按三段落地：B1 只做 `core` DSL 契约、JSON 空值语义和旧字段拒绝；B3、B4、B6 才进入直接交易、授权和 replay 的 route/posting 消费；B7 才进入清结算与对账拆分；B8 才进入归档、冷热读取和治理重放消费。未完成后段前，不得宣称含权益资金流已完整闭合；涉及独立伴随指令、补充权益事实、审计证据包、使用者解释视图、证据最小化或外部规则核验时，还必须在对应 MVP 任务的 Execution Grant 中登记原子性、补录载体、最终重校验、视图防误导、脱敏边界、规则核验和失败处理。
16. 直接交易和授权交易不因权益资金事实新增权益专用服务方法；后续如需扩展 `FundsTransactionPayRequest`、`FundsTransactionRefundRequest`、`FundsAuthorizationTransactionAuthorizeRequest` 或授权后续事件 Request，必须由对应 MVP 任务的 Execution Grant 明确允许。授权过期 `expire` 已作为 B4-03 既有授权生命周期缺口完成，不属于权益设计新增入口；后续不得借权益任务再次扩大 `expire` 公共契约。
16A. 授权支付工具入口允许新增 application facade，例如 `authorizeByInstrument` 或等价入口；直接交易若后续需要支付工具型业务入口，必须由独立 Execution Grant 再确认服务命名、公共契约、Request/DTO、幂等摘要和测试资产。不得把现有账户主体型内核请求直接替换为业务入口参数或工具引用字段。Execution Grant 必须声明是否新增 application 包、是否调整 transaction-* 对 wallet-face 的依赖、业务入口解析结果、PaymentInstrumentRef / BindingSnapshot / FundingAllocationDecision / BenefitSnapshot / 拒绝原因的快照位置、敏感上下文回归和目标测试资产。
16B. 钱包模块 application facade 进入编码前，必须先确认 `PaymentInstrumentCapabilityApplicationService`、`FundingResponsibilityResolutionApplicationService`、`WalletAccountApplicationService` 或等价命名，以及资源服务能否继续作为管理接口暴露；调用方不得长期绕过 application facade 拼装 `FundingAccountService`、`PaymentInstrumentService`、`SpendSubjectFundingRelationService` 完成交易准入。支付工具能力控制首批只授权 RECEIVE、PAY、AUTHORIZE、REFUND、WITHDRAW 五类工具动作准入；工具能力通过不得替代账户能力、余额、额度、账本周期和资金责任校验。
16C. 支出主体资金责任解析关系进入编码前，必须确认目标责任字段策略：迁移为 `targetSubjectType + targetSubjectId`，或保留 `fundingAccountId` 兼容字段且仅支持资金账户。若声明支持信用账户或平台账户角色作为最终责任主体，DTO、DDL/H2 schema、Entity、Mapper、service、摘要、fixture、route snapshot 和回放测试必须同步；不得在 `fundingAccountId` 仍是唯一写入字段时宣称多责任主体生产可用。
16D. 预算组兼容缺口进入编码前，必须确认 `FundsSubjectType.BUDGET_GROUP` 是兼容枚举、迁移别名、只读查询过滤条件还是待删除目标。未确认前不得把预算组 ledger 初始化、预算组 route leg、预算组 posting 或预算组 LedgerEntry 作为目标态 Done 证据。
16E. 交易内核 canonical 请求继续以已解析账户主体作为入参。直接交易、授权内核、余额控制、退款、撤销、重放和余额查询不得直接替换为业务入口参数或支付工具引用；业务入口参数和支付工具引用只允许进入 wallet/transaction application facade、route snapshot、审计和只读投影。若 B4 新增 `authorizeByInstrument` 或等价入口，必须先解析业务入口参数、支付工具、绑定、Spend Rule、预算上下文和资金责任，再委派账户主体型授权内核。
16F. 本轮代码 CR 发现的预算组旧语义必须作为 B2-07 的首要准入风险处理：`FundsSubjectType.BUDGET_GROUP` 枚举、`BUDGET_BASIC` profile、预算组 ledger 初始化、预算组余额查询、route participant、route replay、posting postable subject、余额控制 limit adjust 和测试 fixture 中的预算组主体断言都不得在未确认兼容策略时继续扩大。若因兼容保留，Execution Grant 必须写明兼容场景、禁止新写入、迁移退出条件和回归测试。
16G. 支付工具 application facade 的新增接口默认落 `wallet-face` 的 `com.wind.funds.wallet.application.instrument`、`com.wind.funds.wallet.application.funding` 或 `com.wind.funds.wallet.application.vcc`，实现落 `wallet-impl` 对应 `application/*/impl` 包，Request/DTO 默认落 `com.wind.funds.wallet.model.request` 和 `com.wind.funds.wallet.model.dto`。不得新增顶层 `com.wind.funds.instrument`，不得让 `transaction-impl` 依赖钱包资源服务。交易层若要完善无授权退款、拒付/争议、余额控制调账和原路径回放，必须作为账户主体型 canonical 内核补强切片独立授权，不得包装成统一支付工具交易内核；授权过期释放已作为 B4-03 基础能力闭合，强制完成首轮 canonical 能力已作为 B4-FORCE-SETTLE 闭合。
17. 券能不能退由业务层、订单层、营销权益系统或运营审批链路决策；资金底座只承接原权益资金事实、历史摘要、受控补充事实和本次退款决策引用，不调用当前营销规则、不读取当前券包状态、不自行判断退券可行性。
18. 平台补贴、储值券、预付券、礼品卡、客户资金、商户待结算资金、负债、备付或收入成本口径进入 B3/B7 生产资金流前，必须在 Execution Grant 中登记财务、税务、会计、合规或法务确认状态；未确认时只能保留为契约或设计验证，不得进入生产 Done 结论。
19. 含权益 MVP 任务进入 Phase 2 或 Phase 3 前，Execution Grant 必须选择 Phase 能力边界、JSON 夹具级别、权益资金事实源、零实付表达、平台补贴表达、独立伴随指令原子性、储值/预付口径、退款分摊粒度、退款分摊确定性规则版本、分摊依据、稳定组件顺序、舍入模式、尾差归属、组件剩余额度版本、历史无权益资金事实处理策略、补充权益事实模型、专业确认状态、审计证据包、使用者解释视图、证据最小化和外部规则核验状态；缺任一项时只能执行 contract-only、设计验证或失败用例，不得实现生产资金流。
20. `contextVariables` 只允许作为短期迁移通道，且只能承载 `benefitSnapshotId`、`stableDigest`、`benefitGroupSn`、`componentSn` 摘要、`ruleVersion`、`refundDecisionId`、历史 `externalDecisionId` 等追溯引用；`externalDecisionId` 不作为 `FundsBenefitFundingSourceDTO` 的一等字段，外部决策来源通过 `sourceType=EXTERNAL_DECISION` 或 `sourceId` 表达；组件金额、资金责任、退款处置完整内容和当前营销规则不得放入 `contextVariables` 作为生产事实。
21. `com.wind.funds.transaction.model.request` 下的交易 Request 只能以 `ReadonlyContextVariables` 承载扩展上下文；不得恢复 `WritableContextVariables` 字段或 `FundsRequestContextVariables` request 专属快照工具。后续若扩展核心资金语义，必须使用一等字段或不可变事实快照，并在 Execution Grant 中列明公共契约范围、请求摘要和敏感上下文回归测试。

## 9. 覆盖索引到设计 ID 映射

| 覆盖索引 | 产品验收 ID | DSL 契约或场景 | TDD 用例入口 |
| --- | --- | --- | --- |
| B1 | `RED-001`、`RED-003`、`RED-009`、`RED-020`、`RED-022`、`RED-023`、`RED-046` 至 `RED-049`、`AC-BEN-001`、`AC-BEN-012`、`AC-BEN-013`、`AC-BEN-015`、`RED-050` 至 `RED-059`、`RED-061`、`RED-063` | Route DSL、PaymentInstrument Route DSL、Posting/Ledger DSL、SettlementPolicy、金额临界值、权益资金事实契约、JSON 契约、`DSL-BENEFIT-FUNDING-SETTLE-001`、旧 `instruction.benefitSnapshot` 拒绝和 `CONTRACT_ONLY` 夹具边界 | `TDD-RED-001`、`TDD-RED-003`、`TDD-RED-004`、`TDD-RED-031`、`TDD-RED-032`、`TDD-RED-034` 至 `TDD-RED-037`、`TDD-LEDGER-001` 至 `TDD-LEDGER-011`、`TDD-ROUTE-011` 至 `TDD-ROUTE-013`、`TDD-BEN-001` 至 `TDD-BEN-009`、`TDD-BEN-RED-001`、`TDD-BEN-RED-002`、`TDD-BEN-RED-012`、`TDD-BEN-RED-017`、`TDD-BEN-RED-018`、`TDD-BEN-RED-020`、`TDD-BEN-RED-023` |
| B2 | `AC-PI-001` 至 `AC-PI-010`、`AC-CTRL-009` 至 `AC-CTRL-011`、`AC-BALLOG-001`、`RED-036`、`RED-046`、`RED-047`、`RED-049`、`RED-067` | 支付工具、绑定关系、绑定历史审计、支付工具能力应用服务、支付工具动作能力控制、资金责任解析关系、资金责任目标主体引用、钱包应用 facade、预算组兼容收敛、账本周期、余额投影、余额日志 | `TDD-WALLET-*`、`TDD-WALLET-018`、`TDD-WALLET-019`、`TDD-ROUTE-011`、`TDD-ROUTE-012`、`TDD-LEDGER-*`、`TDD-VIEW-003`、`B2-RED-003A` |
| B3 | `AC-IN-*`、`AC-OUT-*`、`AC-PAY-*`、`AC-MER-*`、`AC-FEE-*`、`AC-BEN-002` 至 `AC-BEN-005`、`AC-BEN-009`、`AC-BEN-013`、`AC-BEN-014`、`AC-BEN-016`、`AC-BEN-018`、`AC-BEN-019`、`RED-050` 至 `RED-052`、`RED-054` 至 `RED-060`、`RED-062`、`RED-064` 至 `RED-066` | `DSL-DIRECT-*`、`DSL-REVERSE-REFUND-FEE-001`、`DSL-BENEFIT-MERCHANT-DISCOUNT-001`、`DSL-BENEFIT-PLATFORM-SUBSIDY-001`、`DSL-BENEFIT-PLATFORM-NO-SETTLEMENT-001`、`DSL-BENEFIT-PREPAID-VOUCHER-001`、`DSL-BENEFIT-PARTIAL-REFUND-001`、`DSL-BENEFIT-COMPANION-INSTRUCTION-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、零实付权益表达、外部规则核验状态 | `TDD-DIR-*`、`TDD-DIR-FLOW-*`、`TDD-DIR-ERR-*`、`TDD-BEN-DIR-*`、`TDD-BEN-DIR-006`、`TDD-BEN-DIR-007`、`TDD-BEN-REFUND-003`、`TDD-BEN-RED-003`、`TDD-BEN-RED-004`、`TDD-BEN-RED-006`、`TDD-BEN-RED-008`、`TDD-BEN-RED-017` 至 `TDD-BEN-RED-019`、`TDD-BEN-RED-021`、`TDD-BEN-RED-022`、`TDD-BEN-RED-024`、`TDD-BEN-RED-025`、`TDD-BEN-RED-027` 至 `TDD-BEN-RED-030`、`TDD-RACE-012` |
| B4 | `AC-AUTH-001` 至 `AC-AUTH-012`、`AC-RAIL-001`、`AC-RAIL-002`、`AC-BEN-006`、`AC-BEN-013`、`RED-025` 至 `RED-027`、`RED-035`、`RED-053`、`RED-054`、`RED-059` | `DSL-AUTH-LIFECYCLE-001`、`DSL-AUTH-FORCE-CAPTURE-001`、`DSL-AUTH-REFUND-001`、`DSL-BENEFIT-AUTH-HOLD-001`、`AUTHORIZATION_TRANSACTION / SETTLE` 强制完成模式、`AUTHORIZATION_TRANSACTION / AUTH_REFUND` 无授权退款模式、VCC 授权归一边界和授权控制扩展 | `TDD-AUTH-*`、`TDD-AUTH-FLOW-*`、`TDD-AUTH-ERR-*`、`TDD-AUTH-EXT-*`、`TDD-BEN-AUTH-*`、`TDD-BEN-RACE-002`、`TDD-BEN-RED-005`、`TDD-BEN-RED-014`、`TDD-BEN-RED-017`、`TDD-ROUTE-003`、`TDD-ROUTE-005`、`TDD-ROUTE-008`、`TDD-ROUTE-009`、`TDD-RACE-001` 至 `TDD-RACE-003`、`TDD-RAIL-001`、`TDD-RED-003`、`TDD-RED-005`、`TDD-RED-008`、`TDD-RED-016`、`TDD-RED-017`、`TDD-RED-017A`、`TDD-RED-033`、`TDD-RED-036` |
| B5 | `AC-CTRL-001` 至 `AC-CTRL-008`、`AC-CTRL-013`、`AC-ADJ-001` 的 adjust 入口和红线；其中 `AC-CTRL-004` 为资金账户余额调整，`AC-CTRL-013` 为外部余额异常纠偏 | `DSL-BALANCE-CONTROL-FREEZE-001`、`DSL-BALANCE-CONTROL-ADJUST-001`、`DSL-BALANCE-CONTROL-EXTERNAL-DEFICIT-ADJUST-001`、`DSL-BALANCE-CONTROL-LIMIT-BUDGET-001`、`DSL-SETTLEMENT-RECONCILIATION-ADJUST-001` 的 adjust 红线 | `TDD-CTRL-*`、`TDD-CTRL-FLOW-*`、`TDD-CTRL-ERR-*`、`TDD-CTRL-009`、`TDD-CTRL-012`、`TDD-CTRL-ERR-005`、`TDD-CTRL-ERR-007`、`TDD-RECON-016`、`TDD-RACE-004`、`TDD-RED-006`、`TDD-RED-011`、`TDD-RED-012`、`TDD-RED-015`、`TDD-RED-033` |
| B6 | `AC-ROUTE-*`、`AC-PI-005`、`AC-VIEW-*`、`AC-BALLOG-001`、`AC-REPLAY-*`、`AC-BEN-007` 至 `AC-BEN-010`、`AC-BEN-012`、`AC-BEN-016` 至 `AC-BEN-019`、使用者可解释性矩阵、`RED-003`、`RED-016`、`RED-017`、`RED-036`、`RED-043`、`RED-044`、`RED-048`、`RED-049`、`RED-054`、`RED-056`、`RED-058`、`RED-060`、`RED-062` 至 `RED-066` | Route Replay、支付工具换绑后原路径回放、权益资金事实回放、权益资金事实源一致性、补充权益事实、交易投影、运营时间线、余额日志、证据脱敏、规则核验、`DSL-BENEFIT-REFUND-NO-COUPON-001`、`DSL-BENEFIT-REFUND-RETAIN-SUBSIDY-001`、`DSL-BENEFIT-MISSING-FUNDING-FACT-REPLAY-001`、`DSL-BENEFIT-SUPPLEMENTAL-FACT-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、外部规则核验状态 | `TDD-ROUTE-*`、`TDD-ROUTE-013`、`TDD-RACE-009`、`TDD-RACE-012`、`TDD-VIEW-*`、`TDD-REPLAY-*`、`TDD-BEN-REFUND-*`、`TDD-BEN-REPLAY-001` 至 `TDD-BEN-REPLAY-004`、`TDD-BEN-RACE-001`、`TDD-BEN-RED-002`、`TDD-BEN-RED-007` 至 `TDD-BEN-RED-009`、`TDD-BEN-RED-018`、`TDD-BEN-RED-021`、`TDD-BEN-RED-023`、`TDD-BEN-RED-026` 至 `TDD-BEN-RED-030`、`FundsOperationExplainabilityTests`、`FundsOperationPermissionBoundaryTests`、`TDD-RED-003`、`TDD-RED-010`、`TDD-RED-013`、`TDD-RED-014`、`TDD-RED-029`、`TDD-RED-034` 至 `TDD-RED-037` |
| B7 | `AC-CLR-*`、`AC-SET-*`、`AC-SET-006` 至 `AC-SET-009`、`AC-REC-*`、`AC-ADJ-001`、`AC-BEN-011`、`AC-BEN-013`、`AC-BEN-015` 至 `AC-BEN-019`、使用者可解释性矩阵、`RED-030` 至 `RED-033`、`RED-037` 至 `RED-039`、`RED-057`、`RED-059` 至 `RED-066` | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、出款前准入门禁、外部非终态、金额不一致、出款解释状态、对账批次、差错单、追偿单独立对象、权益清分和权益对账拆分、伴随指令清分合并、补充事实对账解释、清结算工作台解释输出、证据脱敏、规则核验、`DSL-BENEFIT-CLEARING-RECONCILIATION-001`、`DSL-BENEFIT-COMPANION-INSTRUCTION-001`、`DSL-BENEFIT-SUPPLEMENTAL-FACT-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、`DSL-SETTLEMENT-RECONCILIATION-001`、`DSL-SETTLEMENT-PAYOUT-RESULT-001`、`DSL-SETTLEMENT-RECONCILIATION-ADJUST-001` 完整差错闭环 | `TDD-CLS-*`、`TDD-CLS-FLOW-*`、`TDD-SETTLE-*`、`TDD-SETTLE-004`、`TDD-SETTLE-005`、`TDD-RECON-*`、`TDD-BEN-CLS-*`、`TDD-BEN-RECON-*`、`TDD-BEN-OPS-*`、`TDD-BEN-DIR-007`、`TDD-BEN-REPLAY-004`、`TDD-BEN-RED-010`、`TDD-BEN-RED-017`、`TDD-BEN-RED-020` 至 `TDD-BEN-RED-030`、`FundsOperationExplainabilityTests`、`FundsOperationPermissionBoundaryTests`、`FundsRunbookSignalTests`、`PayoutPreflightTests`、`PayoutReceiptMismatchTests`、`PayoutExplainabilityTests`、`TDD-RACE-005` 至 `TDD-RACE-007`、`TDD-RACE-012`、`TDD-RED-020` 至 `TDD-RED-025`、`TDD-RED-033` |
| B8 | `AC-ARCH-*`、`AC-ARCH-008`、`AC-ARCH-009`、`AC-REPLAY-*`、`AC-RPT-*`、`AC-BEN-016` 至 `AC-BEN-019`、使用者可解释性矩阵、`RED-016` 至 `RED-019`、`RED-029`、`RED-034`、`RED-040` 至 `RED-042`、`RED-060`、`RED-062`、`RED-064` 至 `RED-066` | `DSL-GOVERNANCE-ARCHIVE-MANIFEST-001`、`DSL-GOVERNANCE-BALANCE-SNAPSHOT-001`、`DSL-GOVERNANCE-PROJECTION-REPLAY-001`、`DSL-GOVERNANCE-METRIC-SNAPSHOT-BOUNDARY-001`、`DSL-GOVERNANCE-BIG-DATA-ARCHIVE-BOUNDARY-001`、`DSL-BENEFIT-COMPANION-INSTRUCTION-001`、`DSL-BENEFIT-SUPPLEMENTAL-FACT-001`、`DSL-BENEFIT-AUDIT-EVIDENCE-001`、`DSL-BENEFIT-EXPLAINABLE-VIEW-001`、归档、余额重建、交易投影重放、差异报告、异常人工处理、大数据消费边界、指标只读、指标水位隔离边界、证据脱敏、规则核验和 Runbook 信号 | `TDD-ARCH-*`、`TDD-ARCH-009`、`TDD-ARCH-010`、`TDD-REPLAY-*`、`TDD-METRIC-*`、`FundsOperationExplainabilityTests`、`FundsOperationPermissionBoundaryTests`、`FundsRunbookSignalTests`、`TDD-RACE-008`、`TDD-RACE-010`、`TDD-RACE-012`、`TDD-BEN-RED-025` 至 `TDD-BEN-RED-030`、`TDD-RED-018`、`TDD-RED-019`、`TDD-RED-026` 至 `TDD-RED-028`、`TDD-RED-033` |
| P2 VCC 证据与对账 | `VCC-AC-010`、`VCC-AC-011`、`VCC-RED-001`、`AC-REC-*`、`RED-030`、`RED-034` | `DSL-P2-EXTERNAL-EVIDENCE-PACK-001`、`DSL-VCC-RECON-SOURCE-OBJECT-001`、外部适配证据包、VCC 对账来源对象、证据脱敏、规则核验、对账差错、匹配结果、处理动作和审计链路；不新增 VCC 对账内核，不实现供应商账单或财务凭证系统。 | `TDD-P2-EVIDENCE-001`、`TDD-P2-EVIDENCE-RED-001`、`TDD-P2-VCC-013`、`TDD-P2-VCC-014`、`TDD-P2-VCC-RED-001`、`TDD-RECON-*`、`TDD-RED-030`、`TDD-RED-034` |
| P2 ACH 边界 | `ACH-BOUNDARY-001` 至 `ACH-BOUNDARY-006`、`AC-RAIL-002A` 至 `AC-RAIL-007` | ACH 边界文档、P2 DSL 准入卡、ExternalAccountRefSpec、外部规则核验字段、文件摘要、回单引用、对账差错和调账核销引用；不新增 ACH DSL 内核对象。 | `TDD-RAIL-002` 至 `TDD-RAIL-007`、`TDD-RED-030`、`TDD-RED-034`、`TDD-P2-GA-*`、`TDD-P2-ACQ-*`、`TDD-RECON-*`、`TDD-OPS-*` |

### 9.0 B7 当前首切片恢复口径（2026-06-07）

上表中的 B7 行是清结算与对账目标态覆盖索引，不等同于当前编码授权。当前可恢复的首个候选只限定为 `B7-RECON-DIFFERENCE-MVP` / `B7-RECON-DIFFERENCE-MVP-CAD-001`，目标是先证明对账来源标准化、对账任务、匹配强度、差错单、阻断、账龄升级、重跑和补事实白名单准入。

`contract-only` 只能交付契约字段、DTO 或目标 Red，不得声明 B7 生产可用；`ddl-backed` 只能说明最小对账表结构、Entity、Mapper 和 H2 schema 可落地；只有 `service-flow-backed` 加 `schemaDecision=minimal-reconciliation-ddl-h2-required`，并覆盖真实 Spring Bean、来源质量阻断、匹配强度约束、差错阻断、账龄升级、重跑幂等、白名单补事实准入和失败无副作用测试，才可作为 B7 首切片生产交付证据。

当前恢复口径不授权完整清分、内部清算、结算锁定、出款提交、追偿、VCC clearing、全球账户外部流水匹配、银行/卡组织/通道协议解析或生产调度平台。VCC 和全球账户后续只能消费 B7 差错闭环作为阻断、解释、补事实或出款前准入输入，不得在 P2 业务包中平行实现清结算与对账对象。

### 9.1 GSD-1 账本账目当前候选（2026-06-05）

本节记录当前最高优先级账本账目 GSD 线的可恢复入口，不替代用户确认的 Execution Grant。`GSD1-LD-RED-001A` 已消费 `Execution Grant：GSD1-LEDGER-FACTS`，`GSD1-LD-RED-001B` 已消费 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY`；二者专项验证已通过且未修改生产代码。2026-06-07 工作树审计确认目标测试文件当前未被 Git 跟踪，因此这些证据只作为当前工作树证据和后续编辑基底，不是已冻结 Git 基线。

| 字段 | 内容 |
| --- | --- |
| 当前状态 | `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY` |
| 已消费候选 | `GSD1-LD-RED-002A` / `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`；`GSD1-LD-RED-003` / `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`；`GSD1-LD-RED-004A` / `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` |
| 消费记录入口 | `docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md#12-consumedgrantonepageconfirmation2026-06-07` |
| 002A 结果 | 已补入账编排层绑定账本账目、币种和负余额约束目标测试，证明 entry 与绑定 ledger 不一致时，在账本事实持久化和余额投影前失败；Red 直接 Green，未修改生产代码。 |
| 实际写入 | `tests/src/test/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImplTests.java`；该文件当前存在于工作树但未被 Git 跟踪，后续 Grant 必须先保护既有 001A/001B/002A 覆盖。 |
| 源码锚点复核 | 2026-06-07 只读复核确认 `DefaultLedgerTransactionPostingServiceImpl#assertEntryMatchesLedger` 已校验 entry 与 bound ledger 的 subject、账目 code/category、currency，以及 `ALLOW_NEGATIVE` 与 ledger `allowNegative` 的兼容性；002A 作为测试覆盖缺口闭合。 |
| 后续禁止范围 | 不得沿用 002A 授权继续写 Java、测试、DDL/H2 schema、公共契约或运行时配置；002A 不处理 `BUDGET_GROUP` 兼容策略、钱包、交易层新业务语义、支付工具、VCC、全球账户、收单、清结算对账或 governance apply。 |
| 下一恢复入口 | 无可沿用 Grant；必须先确认新的单一 Execution Grant，可在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核、清结算对账等范围中选择一个切片。 |
| 最近运行时证据 | 2026-06-07 002A 提权复跑 `DefaultLedgerTransactionPostingServiceImplTests` 10 tests 通过；003 运行 `LedgerBalanceProjectionServiceImplTests` 5 tests 通过；2026-06-11 004A 运行 direct 47 tests、auth 29 tests、balance-control failure 19 tests 和 `just compile` 均通过。目标测试文件当前未被 Git 跟踪，因此这些证据不代表 Git 基线已冻结。 |
| 人工确认点 | `GSD1-LD-RED-004A` 已按 `COMPAT_CONTROL_LEDGER_WITH_FREEZE` 完成兼容 guard；预算组 control ledger 退出条件、Spend Rule 控制投影、历史兼容迁移或删除仍需新的单一 Grant，不得混入 004A。 |

### 9.2 GSD + Goal 生产可用 MVP 推进基线（2026-06-07）

本节记录上一轮 GSD + Goal 推进基线。2026-06-12 起，它已被 GSD-2 新基线工作流取代为历史证据，不再承载当前活跃未完成计划。新的活跃状态载体见 `docs/TDD设计/GSD-2-新基线工作流规划.md`。

| 字段 | 内容 |
| --- | --- |
| Goal | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-2026-06-07` |
| 权威文档 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` |
| 业务目标 | 以金融创业公司 MVP 为目标，服务 VCC 发卡、VCC 交易处理和全球收付款；但实现顺序按依赖关系优先完整被依赖方能力。 |
| 当前依赖顺序 | 账本账目 -> 钱包账户/账户层级 -> 交易内核 -> 清结算对账 -> 支付工具/Spend Rule 支持 -> VCC 支持 -> 全球账户支持 -> 收单 design-only。 |
| 下一默认入口 | 无可沿用已消费 Grant；Agent Loop 可优先选择低风险 Plan Grant 切片。若继续交易层，应回到 `B4-CANONICAL-REPLAY-FAILFAST` 剩余缺口，优先选择交易投影解释、余额调账审计或授权/争议/VCC lifecycle 更大组合 replay flow 中的单一低风险切片；完整公共契约、DDL/H2、生产代码或运行时配置仍必须确认新的单一 Execution Grant。 |
| VCC 优先时入口 | 不直接打开 `P2-VCC-*`，先确认 `B2-ACCOUNT-HIERARCHY`，完成资金/信用子账户、父账户快照、账目 profile 和卡绑定摘要准入；随后确认 `B2-FR-TARGET`，用 `targetSubjectType + targetSubjectId` 或等价主体引用固化共享卡、预付卡和平台责任来源。VCC 优先只允许 `contract-only/no-ddl` 账户层级准入 Red 先行准备，不豁免账本账目、资金责任、交易内核、清结算对账和支付工具准入依赖；一页式确认入口分别为 `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#802-accounthierarchyonepageconfirmation2026-06-07` 和 `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#861-fundingresponsibilitytargetgrantcandidate2026-06-07`。 |
| 全球账户优先时入口 | 不直接打开 outbound/inbound facade，先补钱包账户、交易内核和对账差错闭环 Round 0。 |
| 交易内核入口 | 已在 `docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md#151-canonicalreplayonepageconfirmation2026-06-07` 补齐一页式确认入口，默认首切片为 `B4-CANONICAL-REPLAY-FAILFAST`，状态 `PARTIAL_COVERAGE_ADDED_B3_AND_ROUTE_REPLAY_VERIFIED_NOT_DONE`；已补授权后继缺原授权事实 fail-fast 覆盖；直接退款原交易引用回放已由 B3 独立 Grant 闭合；纯 route replay resolver 边界已验证当前工具、外部账户或资金责任上下文不会覆盖原快照；原交易存在但 route snapshot 缺失的直接退款 flow 已证明 fail-fast 且无新资金或账务副作用；直接退款交易 flow 已证明当前绑定和资金责任变化后仍沿原支付快照回放；仍需证明交易投影解释、调账审计和授权/争议/VCC lifecycle 更大组合 replay flow，不替换交易 canonical 入参。 |
| 清结算对账入口 | 已在 `docs/TDD设计/B7-清结算与对账Round0准入卡.md#151-reconciliationdifferenceonepageconfirmation2026-06-07` 补齐一页式确认入口，默认首切片为 `B7-RECON-DIFFERENCE-MVP`，状态 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`；生产可用路径要求服务级 H2 闭环和最小 DDL/H2 范围，先证明对账差错、阻断、重跑和补事实白名单，不一次性打开完整清分、清算、结算、出款或追偿。 |
| 交付雷达 | 已在 `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#51-生产可用-mvp-交付雷达` 补齐，逐域标明当前状态、下一生产证据、下一 Grant 入口和不能算 Done 的情况。 |
| Goal 完成度审计 | 已在 `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#52-goal-完成度审计2026-06-07` 补齐。当前结论为 `PARTIAL_LEDGER_EVIDENCE_NOT_PRODUCTION_DONE`；除 001A/001B/002A 当前工作树账本目标测试有运行证据外，其余能力仍是设计、Round 0 或候选入口，不能声明生产可用。 |
| 生产可用红线 | 真实 Spring Bean、真实 H2/fixture、账务事实、余额投影、幂等、失败无副作用、审计和验证命令齐备；禁止 mock/内存版/空 facade/只断言状态数量的样子货。 |
| 当前状态 | `SUPERSEDED_BY_GSD2_BASELINE_RESET_HISTORY_ONLY` |
| 运行时 Goal | 已按用户要求在当前会话开启；该状态用于持续推进、状态交接和停止条件管理。Plan Grant 可执行低风险目标测试覆盖补齐，但不替代任何公共契约、DDL/H2、生产代码、运行时配置或 Git 授权。 |
| 最近运行时证据 | 2026-06-07 `mvn-version` PASS、`compile` PASS、当前工作树 `DefaultLedgerTransactionPostingServiceImplTests` PASS_AFTER_ESCALATION；仅作为运行时 Goal 和当前工作树既有测试资产证据，不作为已冻结 Git 基线。 |
| 最新准入补齐 | 2026-06-07 已补 `B2-ACCOUNT-HIERARCHY`、`B2-FR-TARGET`、`B4-CANONICAL-REPLAY-FAILFAST` 和 `B7-RECON-DIFFERENCE-MVP` 一页式确认入口；2026-06-11 已补 B4 授权后继缺原事实低风险目标测试覆盖，已消费 `B3-DIRECT-REFUND-REFERENCE-REPLAY` 关闭直接退款原交易引用回放切片，已验证 B4 route replay 原快照复用纯边界，已补原交易存在但 route snapshot 缺失时直接退款全链路失败无副作用覆盖，并已补直接退款交易 flow 在当前绑定和资金责任变化后仍沿原支付快照回放覆盖。后续新的公共契约、DDL/H2、生产代码、运行时配置和 Git 仍需新的单一 Execution Grant。 |
| 授权前收口 | 当前 Goal 卡、交付雷达、Wave 队列、Grant 队列、默认入口、切换规则和验证矩阵已作为历史证据冻结；后续不再为同一批候选重复扩写 Round 0 文档，只在发现事实错误、索引漂移或用户新增约束时修正文档。新的计划和优先级在 GSD-2 维护。 |
| 禁止范围 | 旧 Plan Grant 已关闭为历史；未在 GSD-2 下确认新的单一 Execution Grant 前，不得写测试代码、生产代码、DDL/H2 schema、公共契约或运行时配置；`B3-DIRECT-REFUND-REFERENCE-REPLAY` 已消费的公共契约例外只包括 `FundsTransactionRefundRequest.referenceTransactionSn`，不得继续扩展等价 reference DTO 或其他交易请求字段；不得把旧 Goal、GSD Wave、Round 0、Plan Grant 覆盖、B3 局部闭环或文档 CR 写成生产 Done。 |

#### 9.2.1 Agent Loop Engineering 恢复口径（2026-06-11）

本节记录上一轮 `GSD + Goal + Loop` 的历史恢复口径。该 Loop 已于 2026-06-12 被 GSD-2 取代，不再作为当前活跃 Loop。

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-LOOP-2026-06-11` |
| Loop 状态 | `CLOSED_AS_HISTORY_BY_GSD2_BASELINE_RESET` |
| 状态载体 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md`、`docs/TDD设计/GSD-1-账本账目状态账本.md`、`docs/TDD设计/GSD-1-账本账目Wave1执行计划.md`、`openspec/project.md` 和本 Harness tasks。 |
| 允许动作 | 修正事实错误、索引漂移、恢复入口、验证矩阵、handoff 和下一 Grant 候选说明；在无公共契约、无 DDL、无生产代码、无跨域写入时补目标测试覆盖；运行文档结构、空白、目标测试和 diff 校验。 |
| 禁止动作 | 未确认新的单一 Execution Grant 前，不改公共契约、DDL/H2 schema、生产代码、运行时配置、Git add/commit/push、联网、依赖安装或不可逆操作；若目标测试暴露必须改生产代码的真实缺口，先停在 Red 证据并重新确认 Grant。 |
| 反馈和验证 | Harness checker、产品/架构 deliverable checker、外部规则字段 checker、`rg` 一致性扫描、`git status --short`、`git diff --check` 和用户确认。 |
| 预算与停止 | 每轮最多 1 个低风险本地任务；连续 2 轮没有新增验证证据、状态变化或缺口收敛时暂停，并回到用户确认新的单一 Grant。 |
| 失败回写 | Goal Ledger、状态账本第 8/9 节、本节、OpenSpec project 和对应候选准入卡。 |
| Git 策略 | `summary_only`；不执行本地提交。 |

### 9.2.2 GSD-2 新基线工作流（2026-06-12）

本节记录新的活跃工作流入口。它只建立状态载体、旧计划出队、新 Workflow 和下一候选优先级，不替代任何单一编码授权。

| 字段 | 内容 |
| --- | --- |
| Goal ID | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| Loop ID | `GSD2-LOOP-BASELINE-PLANNING-2026-06-12` |
| 本轮新增权威补充 | `docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md` 已新增 `GSD2-B2-SPEND-CONTROL-ADMISSION-001` 消费记录，并把 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 从确认包准备态推进到本地 Green / Verify；B7 差异报告已提交到 `a1397ddf`，支出控制准入快照已提交到 `021ee2ce`。 |
| 权威文档 | `docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-W1-基线差距审计.md`、`docs/TDD设计/GSD-2-W2-单一Grant选择卡.md`、`docs/TDD设计/GSD-2-W3-B2账户层级CAD准入草案.md`、`docs/TDD设计/GSD-2-W4-B2账户层级ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-AUTH-Chargeback目标语义对齐任务卡.md`、`docs/TDD设计/GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` |
| 当前状态 | `SR_CONTROL_ACTIVITY_GREEN_VERIFIED_COMMITTED` |
| 本轮新增写入补充 | Spend Rule 控制活动与预算控制投影 application 契约、Request / Query / DTO、活动类型枚举、wallet-impl Entity / Mapper / Service、H2 schema、目标服务流测试、TDD README、docs README、LWT Goal、W5 推进计划、GSD-2 工作流入口和本 Harness tasks 的状态同步；不授权 Spend Rule 规则定义、决策日志持久化实现、交易消费控制活动、VCC facade、支付工具交易内核、Controller、HTTP/RPC、生产迁移或 Git 历史重写。 |
| Git / code baseline | 当前已提交 Git/code baseline 为 `78f7f008 feat: 补齐支出控制活动与预算投影`；`GSD2-B5-SR-CONTROL-ACTIVITY-001 / schemaDecision=ddl-backed` 的服务层最小实现、H2 schema、目标测试和状态文档已完成本地 Green / Verify 并提交。`GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已提交到 `a1397ddf`，`021ee2ce`、`4ef64275`、`da3b4f19`、`0b251593`、`dd442888`、`ea8f8800`、`632bd2f6`、`ca603eab`、`873e5f8c`、`a38776c5`、`bc7ffc0f`、`10853e2d`、`e81a8a25` 和 `ae8cb8a6` 保留为已消费能力证据。 |
| 活跃未完成编码计划 | 当前没有活跃未完成编码计划；`GSD2-B5-SR-CONTROL-ACTIVITY-001` 已完成首轮本地 Green / Verify 并提交到 `78f7f008`。旧候选不再是当前编码计划，只能作为 backlog reference；`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、B4 投影解释、B2 wallet 准入、ledger guard、B5 余额调账、B7 gate / consumer / report、支出控制准入和 B5 控制活动均已消费；下一轮不得复用 B5-003、B7 对象级 Gate、B7 consumer、B7 report、account capability、pre-transaction snapshot、spend control admission 或 B5 控制活动 Grant。 |
| 写入范围 | `docs/TDD设计/GSD-2-新基线工作流规划.md`、W5 P0/P1 LWT 推进计划、LWT Goal、B5 余额调账审计扩展确认包、B7 清算结算 Gate 确认包、AUTH Chargeback 两个入口、TDD README、docs README、OpenSpec project 和本 Harness tasks。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、源码、测试、Justfile、最近 Git 提交和旧 GSD/Grant 历史材料。 |
| 下一 Workflow | `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`、`GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002`、`GSD2-B2-FR-TARGET-001`、`GSD2-B2-WALLET-APPLICATION-FACADE-001`、`GSD2-B2-WALLET-APPLICATION-FACADE-002 / B2-PI-CAP-CAD-001`、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`、`GSD2-LD-LEDGER-GUARD-REGRESSION-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002` 首轮争议退款、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`、`GSD2-B5-BALANCE-ADJUST-AUDIT-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-DIFFERENCE-MVP-001`、`GSD2-B7-RECON-DIFFERENCE-MVP-002`、`GSD2-B7-RECON-GATE-CONSUME-001`、`GSD2-B7-RECON-GATE-CONSUME-002` 和 `GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 已完成本地 Green 和门禁收口；`GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` 已完成 contract/design-only 对齐。 |
| 当前默认确认包 | 当前没有默认确认包；`docs/TDD设计/GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md` 已从确认包进入首轮实现并完成本地 Green / Verify / Commit，状态 `SR_CONTROL_ACTIVITY_GREEN_VERIFIED_COMMITTED`；`docs/TDD设计/GSD-2-B7-对账差异报告ExecutionGrant确认包.md` 已记录 `GSD2-B7-RECON-DIFFERENCE-REPORT-001` 消费结果。 |
| 下一候选 | 当前不是新候选选择阶段；`GSD2-B5-SR-CONTROL-ACTIVITY-001 / schemaDecision=ddl-backed` 已完成首轮服务层最小能力并提交到 `78f7f008`。若要扩展 B7 报告、VCC facade、交易消费、生产迁移或其他 wallet 后续能力，必须另起新的单一 Grant。 |
| 禁止范围 | 未确认新的单一 Execution Grant 前，不继续扩展 wallet 授权 route snapshot 回链、交易投影、余额调账查询、Spend Rule、VCC facade、清结算、P2 业务、运行时配置、下一 Grant 的 Git add/commit/push、联网、依赖安装、生产配置或不可逆操作；B5-003 不得复用为审批、补事实、清结算或生产权限授权。 |
| 验证命令 | 已完成代码基线验证包括 `verify-cad`、B4/B5/B7 目标测试、`test-reconciliation`、`compile`、`pmd` 和 `git diff --check`；AUTH 兼容 adapter 已执行授权目标测试、transaction 分组、compile、pmd 和 diff；B4 remaining 已执行目标解释矩阵、主写投影回归、compile、pmd 和 diff；wallet 授权准入与 route snapshot 回链已执行 `just test-one AuthorizationAdmissionApplicationServiceTests tests`、`just test-one AuthorizationAdmissionApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,RouteSnapshotJsonSupportTests,FundsAuthorizationTransactionFlowTests tests`、`just compile`、`just pmd`、`git diff --check` 和结构检查；账户能力来源已执行 `just test-one FundsAccountCapabilityApplicationServiceTests tests`、`just test-one FundsAccountCapabilityApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,FundingResponsibilityResolutionApplicationServiceTests,AuthorizationAdmissionApplicationServiceTests tests`、`just compile`、`just pmd` 和 `git diff --check`；ledger guard 已执行 `just test-one DefaultLedgerTransactionPostingServiceImplTests,LedgerBalanceProjectionServiceImplTests,LedgerServiceImplTests tests`、`just test-ledger`、`just compile`、`just pmd` 和结构检查；B5-002 已执行目标 Red/Green、`just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-balance-control`、`just compile`、`just pmd` 和 `git diff --check`；B5-003 已执行 `just test-one LedgerDtoContextVariablesContractTests tests`、`just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-balance-control`、`just test-boundary`、`just test-transaction`、`just compile` 和 `just pmd`；B7 consumer 已执行 `just test-one ClearingSettlementGateConsumerServiceTests tests`、`just test-reconciliation`、`just compile` 和 `just pmd`；B7 report 已执行 `just test-one ReconciliationDifferenceReportApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just pmd`、`git diff --check` 和结构检查。 |

2026-06-20 运行时 Goal 恢复裁决：若当前会话运行时 Goal objective、旧摘要或历史计划仍写着优先补齐 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、B7 `scopeDecision` 待确认、清算 / 结算真实消费方待确认，或 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 仍等待授权 / 提交，以本节、LWT Goal 和当前 Git/code baseline 为准；该 B2 授权准入、B2 授权 route snapshot、ledger guard、B5-002、B5-003、B7 对象级 Gate、B7 consumer、B7 差异报告和 B5 控制活动均已消费，`GSD2-B5-SR-CONTROL-ACTIVITY-001 / schemaDecision=ddl-backed` 已完成首轮服务层最小能力、本地 Green / Verify 和提交。后续若扩新 Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、Controller、HTTP/RPC、运行时配置、Git 或生产迁移，必须另起新的单一 Grant。

2026-06-17 loop 收口裁决已消费：ledger / wallet / transaction 被依赖能力已在 `e81a8a25` 完成当前切片 full gate，`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` 已完成只读投影解释首轮 Green，`GSD2-B5-BALANCE-ADJUST-AUDIT-001` 已完成外部余额异常纠偏和受控负可用调账审计首轮 Green，`GSD2-B7-RECON-DIFFERENCE-MVP-001` 已完成对账差错登记、处理回链、重新对账幂等和无资金副作用首轮 Green，`GSD2-B7-RECON-DIFFERENCE-MVP-002` 已完成差错处理动作白名单上下文守卫首轮 Green。当前可声明的 Done 包括账户层级来源契约、资金责任目标主体、资金责任解析 application facade、支付工具能力准入 application facade，基于持久化交易事实、RouteSnapshot 和交易明细的 posted pay / declined authorization / missing RouteSnapshot 投影解释，基于 `BALANCE_CONTROL / BALANCE_ADJUST` 的外部余额异常审计字段校验、交易明细上下文透传和失败无副作用，以及 reconciliation 差错运营对象和差错处理动作上下文的最小闭环；route `FundingAllocationDecisionSpec.subjectRef` 仍作为后续预交易快照和 route snapshot 决策承载位。本轮不得声明 wallet 全量生产 Done、平台角色解析、VCC 子账户完整资金流、完整清分清算结算出款、补事实命令执行服务、独立审计表、projection store、治理重放或 P2 业务已生产可用。

2026-06-18 消费 `Execution Grant：GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`：按 `COMPAT_GUARD_NO_BEHAVIOR_BREAK` 保留 `FundsAuthorizationTransactionService#chargeback` 公共兼容入口，不删除公共 API、不改 DDL/H2 schema、不引入完整 dispute case。新增授权结算后兼容 chargeback 缺少 `externalDisputeRef` 的失败无副作用 Red，证明缺最小审计上下文时不得生成资金交易、交易明细、route、ledger transaction、posting、LedgerEntry 或余额投影副作用；Green 在 `FundsAuthorizationInstructionConverter` 中校验 `chargebackReason`、`evidenceRef` 和 `FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF`，并补充 face 契约说明。已验证 `just test-one FundsAuthorizationTransactionFlowTests tests` 32 tests 通过、`just test-transaction` 107 tests 通过、`just compile` 通过、`just pmd` 通过、状态一致性扫描通过和 `git diff --check` 通过。本 Grant 已消费，后续不得沿用它扩完整 dispute case、`chargeback -> settleRefund` 委派、事件语义迁移、`declinedAmount` 字段调整或清结算影响。

2026-06-18 消费 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002` 首轮争议退款解释切片：查询 `FundsTransactionProjectionExplainApplicationService` 时，授权聚合下的 `settleRefund` 争议退款按最新非手续费交易明细作为解释源，展示 `DISPUTE_REFUNDED / DISPUTE_REFUND_POSTED`，payload 透出 `REFUND_MODE=DISPUTE`、争议模式和外部争议引用，`evidenceRefs` 包含资金交易、账本交易、外部争议号和凭证号；查询保持只读，不新增交易、账本或余额事实。实现只修改投影解释契约、解释来源和查询服务选择逻辑，不新增 projection store、DDL/H2 schema、治理重放或历史节点级查询。本轮已验证目标投影解释测试 4 tests 通过、主写投影回归 5 tests 通过。

2026-06-18 消费 `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`：补齐普通退款、无授权退款、授权释放/过期和兼容 chargeback 解释矩阵。`FundsTransactionProjectionExplanationSource` 现在能区分 `REFUNDED / FUNDS_REFUNDED`、`NO_AUTH_REFUNDED / NO_AUTH_REFUND_POSTED`、`RELEASED / FUNDS_RELEASED`、`COMPAT_CHARGEBACK_REFUNDED / COMPAT_CHARGEBACK_POSTED`，并在 `evidenceRefs` 和 payload 中保留外部 capture 引用、退款原因、chargeback evidence 和外部争议引用。目标测试 `FundsTransactionProjectionExplainApplicationServiceTests` 从 4 个用例扩展到 8 个用例，覆盖只读无副作用和已落账事实解释；本轮已验证目标解释矩阵 8 tests、主写投影回归 5 tests、`just compile`、`just pmd` 和 `git diff --check` 均通过；仍不新增 projection store、DDL/H2 schema、治理重放、历史节点级查询、失败态全量解释或运营差异报告。

2026-06-18 消费 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`：新增 `AuthorizationAdmissionApplicationService`、`AuthorizeByPaymentInstrumentRequest`、wallet-impl 最小实现和 `AuthorizationAdmissionApplicationServiceTests`。本切片证明支付工具授权入口先完成工具动作准入、绑定主体校验、资金责任解析、账户租户/币种/能力校验，再构造账户主体型 `FundsAuthorizationTransactionAuthorizeRequest` 委派 `FundsAuthorizationTransactionService#authorize`；准入失败不生成资金交易、route、posting、LedgerEntry 或余额投影，`approved=false` 授权拒绝只生成标准拒绝交易事实，route legs 为空且无 posting、LedgerEntry 或余额影响。已验证目标测试 3 tests、wallet application 组合回归 9 tests、授权交易回归 32 tests、`just compile`、`just pmd` 和 `git diff --check` 均通过；不声明 Spend Rule 策略引擎、VCC facade、完整预交易快照或统一支付工具交易内核完成。

### 9.3 账本账目到钱包账户交接门禁（2026-06-07）

本节记录当前被依赖方交接口径，不替代用户确认的 Execution Grant。`GSD1-LEDGER-BOUND-LEDGER` 已被 002A 消费；其结果只作为当前工作树账本证据，钱包账户、账户层级、VCC 子账户和全球账户钱包进入生产可用声明前仍需新的单一 Grant 证明自身 `contract-only` 或 `service-flow-backed` 范围。

| 字段 | 内容 |
| --- | --- |
| 权威入口 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#712-账本账目到钱包账户交接门禁` |
| 默认账本前置 | 当前工作树 001A/001B/002A 账本证据：已证明 entry 与绑定 ledger 的 subject、账目 code/category、currency 和 `ALLOW_NEGATIVE` 不匹配时，在 ledger transaction、posting plan、ledger entry 和余额投影前失败；该证据未冻结为 Git 基线，也不能替代账户层级或钱包生产 Grant。 |
| 可先行账户层级准入 | `B2-ACCOUNT-HIERARCHY contract-only/no-ddl` 可作为 VCC 快速路径先行准备，只证明账户层级字段、父账户/根账户快照、账目 profile、卡绑定摘要、application facade 命名和目标 Red 可评审。 |
| 不可先行生产声明 | 未具备账本 `service-flow-backed` 证据前，不得声明账户开户真实落账、账本初始化、余额可用、父子账户汇总可生产、VCC funding、共享卡授权或全球账户钱包生产 Done。 |
| 后续 service-flow 要求 | 账户层级若要进入 `ledger-snapshot-backed` 或 `service-flow-backed`，必须证明账户 profile 到 ledger 初始化、父/根账户快照、子账户 posting role、父账户只读聚合不双算、失败无半截账户/账务事实、幂等摘要和审计快照。 |
| VCC/全球账户消费边界 | VCC prepaid/shared card 和全球账户后续只能消费已确认账户层级、资金责任、交易内核和对账差错证据；不得在 P2 业务包里平行实现账本、钱包、清结算或对账对象。 |
| 本轮授权状态 | 文档基线补齐；不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |

### 9.4 外部和兄弟项目来源采纳门禁（2026-06-07）

本节记录 GSD + Goal 对 fincone、fincone-issuing、nobe、Highnote、陈天宇宙公开文章和其他公开资料的采纳边界。它用于后续 Grant 的证据输入，不替代 wind-funds Source of Truth，也不授权编码。

| 字段 | 内容 |
| --- | --- |
| 权威入口 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#21-来源采纳分级门禁` |
| Source of Truth | 仅 wind-funds 当前 PRD、DSL、系分、TDD、OpenSpec、源码、测试、验证命令和确认时 Git HEAD。 |
| advisory-reference | fincone 产品架构、fincone-issuing docs/v3、Highnote 等资料只能校准业务目标、产品域边界、防腐层、命名和场景拆解，不得直接复制对象、表、状态机、接口或通道规则。 |
| scenario-seed | nobe 现有 VCC/全球账户项目和历史业务代码只能提供真实场景种子、异常路径、验收样例和目标 Red 候选，不得把历史实现当目标架构。 |
| semantic-reference | 陈天宇宙公开文章和公开支付语义资料只能用于统一术语和识别概念歧义，不得作为监管、法务、会计、卡组织、银行、ACH、SWIFT、FX 或通道规则结论。 |
| blocked-reference | 当前环境无法核验正文、来源不完整、版本不明或确认方缺失的资料只能登记待确认项和不采纳原因，不得进入产品结论、系统设计、Execution Grant、测试断言或生产 Done。 |
| Grant 要求 | 任一 Grant 引用非 Source of Truth 来源时，必须列明来源路径或 URL、版本或发布日期、核验日期、确认方、采纳级别、采纳字段或场景、Not Done 边界和冲突处理；外部规则仍需法务、合规、财务、通道或持牌机构确认。 |
| 本轮授权状态 | 文档基线补齐；不授权写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |

## 10. Execution Grant 候选模板

进入任一 MVP 编码任务前，由用户确认以下授权信息：

```text
授权任务：
允许写入范围：
禁止写入范围：
必须覆盖的 TDD 用例：
必须覆盖的 AC/DSL ID：
基线是否已冻结：是/否
工作树状态：clean / dirty，若 dirty 必须列出允许纳入和必须排除的变更
Phase 能力边界：Phase 1 contract-only / Phase 2 route-posting-replay / Phase 3 replay-clearing-reconciliation-projection-archive-governance
JSON 夹具级别：CONTRACT_ONLY / 资金流夹具 / 不涉及
权益资金事实源：RouteSnapshotSpec / 交易事实 / 独立权益资金事实 / 等价不可变存储 / 不涉及
零实付表达：零金额主指令 / 伴随权益指令 / 阻断 / 不涉及
平台补贴表达：额外 route leg / 独立伴随指令 / 阻断 / 不涉及
独立伴随指令原子性：同事务 / 同业务组幂等加补偿 / Saga补偿 / 不涉及；失败补偿和投影合并策略：
储值预付口径：纳入并已确认 / 不纳入 / 阻断 / 不涉及
退款分摊粒度：商品行 / 比例 / 现金优先 / 权益优先 / 不可退权益优先 / 不涉及
退款分摊确定性规则：规则版本 / 分摊依据 / 稳定组件顺序 / 舍入模式 / 尾差归属 / 组件剩余额度版本 / 幂等摘要字段 / 并发保护 / 不涉及
历史无权益资金事实处理：失败 / 人工处理 / 迁移补录 / 只读差错 / 不涉及
历史权益补录事实模型：追加 supplemental benefit fact / 不允许补录 / 不涉及；补录来源、复核、digest 和撤销关系：
专业确认状态：财务 / 税务 / 会计 / 合规 / 法务 / 业务 / 不涉及；未确认时的降级处理：
审计证据包：确认方 / 确认时间 / 结论版本 / 适用范围 / 有效期 / 脱敏证据引用 / 撤销或变更处理 / 审计责任人 / 不涉及
使用者解释视图：用户账单 / 商户账单 / 运营时间线 / 财务对账视图 / 审计导出 / 不涉及；事实状态 / 展示状态 / 操作状态；防误导断言：
证据最小化：脱敏摘要 / 文件编号 / 外部 reference / 不涉及；禁止原文字段：
外部规则核验状态：规则来源 / 版本或发布日期 / 生效日期 / 适用主体或适用范围 / 适用法域 / 核验日期 / 确认方 / 确认状态 / 不涉及
出款前准入门禁：结算锁定 / 出款账户 / 收款端点 / 通道额度 / cutoff / 名单筛查 / 外部规则核验 / 负余额 / 准备金 / 对账差错 / 幂等 / 审批 / 不涉及；缺失、失败或未知处理：
外部非终态处理：accepted / submitted / message sent / processing / IN_TRANSIT / 不涉及；防止展示为到账成功的断言：
出款解释状态：事实状态 / 展示状态 / 操作状态 / 不涉及；缺失、待补证据、待人工处理、金额不一致或规则未确认的防误导断言：
运营补事实命令白名单：允许命令或等价事件 / 不允许 / 不涉及；来源单据 / 审批号 / 证据引用 / 幂等键 / 原事实引用 / 操作者 / 原因 / 可撤销边界：
治理物理落点：复用 governance-face/governance-impl / 扩展既有包 / 新增独立模块 / 不涉及；依赖方向 / 公共契约 / 表结构 / H2 schema / Mapper Entity 归属 / 边界测试 / 指标水位隔离测试：
允许修改公共契约：是/否
公共契约允许修改范围：
允许新增枚举或事件：是/否
允许新增服务入口：是/否
允许扩展 Request/Query/DTO：是/否
允许修改表结构：是/否
允许新增模块：是/否
是否影响架构 ADR：是/否
受影响 ADR：
是否触碰能力域边界：是/否
是否触碰事实端口层：是/否
架构边界测试范围：
人工确认点：
NFR 假设：
观测告警：
回滚或补偿：
基础验证命令：just mvn-version、just compile
专项验证命令：
交付方式：
```

## 11. B1-10 历史 Execution Grant 参考（已完成）

本节保留 B1-10 已执行授权的历史参考，不能直接作为新的开工授权。B1-10 已在 `6be9c99 feat: 固化权益快照稳定摘要契约` 完成交付，后续 `75b46ef` 和 `9db3eba` 已收敛请求摘要与稳定摘要支撑，`434f8a2` 已校准账务计划装配器长 ID 用例规范，`42100dc` 已固化权益生产准入门禁；仓库工程治理和测试门禁基线后续已推进至 `81a7ecb`，上一完整 CAD 验证证据提交为 `270122e`，其中 `77bc9f4` 保留为上一冻结代码能力基线。下一轮若继续修改权益契约、请求摘要、route/posting/replay、投影、归档、治理重放或生产主链路，必须另起目标 MVP 任务 Execution Grant。

```text
授权任务：B1 / B1-10 权益快照 DSL 契约
允许写入范围：tests/src/test/java/com/wind/funds/dsl；必要时 tests/src/test/resources/dsl-contract-cases；必要时 core/src/main/java/com/wind/funds/spec/transaction、core/src/main/java/com/wind/funds/model/transaction、core/src/main/java/com/wind/funds/transaction/enums 中 FundsBenefitSnapshotSpec/FundsBenefitComponentSpec/FundsBenefitReferenceSpec/FundsBenefitRefundPolicySpec 和 FundsBenefit* 枚举的最小目标态契约修改
禁止写入范围：transaction-*、wallet-*、ledger-* 业务实现；Route Resolver、Posting Assembler、Route Replay、授权生命周期、清结算、对账、归档、指标实现；生产配置；外部通道适配
必须覆盖的 TDD 用例：TDD-BEN-001 至 TDD-BEN-007、TDD-BEN-RED-001、TDD-BEN-RED-002、TDD-BEN-RED-012、TDD-BEN-RED-017、TDD-BEN-RED-018
必须覆盖的 AC/DSL ID：AC-BEN-001、AC-BEN-012、AC-BEN-013、DSL-BENEFIT-SNAPSHOT-001；登记 RED-050 至 RED-059，其中 RED-058 只作为生产 Done 门禁，本历史授权只做契约层红线，不声明 route/posting/replay 已闭合
后续准入补充：AC-BEN-014 至 AC-BEN-019、RED-060 至 RED-066、TDD-BEN-008、TDD-BEN-009、TDD-BEN-DIR-006、TDD-BEN-DIR-007、TDD-BEN-REPLAY-003、TDD-BEN-REPLAY-004、TDD-BEN-RED-020 至 TDD-BEN-RED-030、TDD-RACE-012 是 B1-10 后新增的 Phase 2/3 准入门禁，不改变 B1-10 历史完成结论；继续实现含权益生产资金流时必须另起 Execution Grant。
基线是否已冻结：已冻结；设计冻结点为 30b1a00；B1-10 契约承载基线为 6be9c99；仓库准入复核输入截至 81a7ecb，上一完整 CAD 验证证据提交为 270122e，上一冻结代码能力基线为 77bc9f4；本节只作历史参考，继续执行或调整 B1 仍需用户确认新的 Execution Grant
工作树状态：执行前必须复核；dirty 时未列入允许纳入范围的变更不得作为 Done 证据
允许修改公共契约：历史授权仅允许 B1-10 最小新增；后续再次修改公共契约必须重新确认，不允许删除或改写既有字段
公共契约允许修改范围：仅限新增 `FundsInstructionSpec#getBenefitSnapshot()` 默认空值方法、权益快照 Spec/Immutable model、权益枚举和必要 JSON 空值语义；不得改变 `amount`、`originalAmount`、`exchangeRate`、`reference`、`instrumentRef`、`externalAccountRef`、`contextVariables` 既有语义
允许新增枚举或事件：历史授权仅允许新增权益枚举，不新增交易事件；后续再次新增枚举或事件必须重新确认
允许新增服务入口：否
允许扩展 Request/Query/DTO：否
允许修改表结构：否
允许新增模块：否
是否影响架构 ADR：否；如新增公共枚举或调整 core 依赖方向，必须重新确认
受影响 ADR：ADR-002 core 作为资金语义内核
是否触碰能力域边界：否
是否触碰事实端口层：否
架构边界测试范围：如改动 core 依赖或公共 Spec 依赖方向，补充 `just test-boundary` 或等效静态检查
人工确认点：`benefitSnapshot` 字段命名和空值语义、权益枚举命名、退款处置枚举、`PLATFORM_DISPLAY_DISCOUNT` 与 `PLATFORM_SUBSIDY` 的语义边界、核心字段不得塞入 `contextVariables`
NFR 假设：本历史授权只做契约承载，不触碰生产并发、容量、外部回调、清结算批次或归档重放；若扩大范围必须重开授权
观测告警：本历史授权不新增生产告警；后续进入 route/posting/replay 或清结算消费时必须补权益快照缺失、摘要冲突和回放失败告警
回滚或补偿：本历史授权不写生产数据；公共契约变更若撤回，必须同步目标态文档、契约测试和版本迁移说明
基础验证命令：just mvn-version、just compile
专项验证命令：just test-one FundsBenefitSnapshotSpecTests tests；just test-one FundsDslJsonContractTests tests；必要时 just test-core
交付方式：每轮说明覆盖用例、修改文件、验证命令、验证结果和残余风险；未获 Git 授权时不自动提交
```

## 12. MVP 任务交付记录模板

## 12.1 准入模拟验证记录（2026-05-21）

本节是正式交付前的模拟记录，最新完成事实以 12.2 实际交付记录和当前 Git 基线为准。

本轮按 `B1-10 权益快照 DSL 契约` 做 TDD 模拟验证，结论如下：

| 检查项 | 结论 | 证据 |
| --- | --- | --- |
| 任务边界 | 只允许进入 Phase 1 契约承载；未进入 route、posting、replay、清结算或对账生产消费。 | 当前写入范围限制在 core DSL/model/enums、DSL JSON verifier、tests DSL 契约和 JSON 夹具；未修改 `transaction-*`、`wallet-*`、`ledger-*` 业务实现。 |
| 设计差异 | 已发现并修复代码缺少 `FundsBenefitAmountClosureRole` 的差异。 | DSL/TDD 要求只有 `ORDER_DISCOUNT_CLOSURE` 参与正向订单闭合，代码已补闭合角色枚举、组件字段、模型校验和 JSON 校验。 |
| TDD 红线 | 已覆盖无权益空值语义、核心字段不得塞入 `contextVariables`、金额闭合、闭合角色缺失或混用、组件唯一性、零实付边界、退款处置和当前营销规则输入拒绝；稳定摘要、请求态快照误判生产 Done 和闭合角色生产消费边界已纳入 B1-10 门禁。 | 模拟阶段 `FundsBenefitSnapshotSpecTests` 10 个用例通过；正式交付见 12.2，已扩展为 `FundsBenefitSnapshotSpecTests` 12 个用例和 `FundsDslJsonContractTests` 8 个用例通过；route/posting/replay 消费断言仍归后续 MVP 任务。 |
| 验证命令 | Java 21 下编译、目标测试和 PMD 均通过。 | `WIND_FUNDS_JAVA_HOME=... just compile`；`just test-one FundsBenefitSnapshotSpecTests tests`；`just test-one FundsDslJsonContractTests tests`；`just pmd`。 |
| 残余边界 | 只能声明 `B1-10 契约承载 Done`；B3/B4/B6/B7/B8 仍需各自 Execution Grant。 | 平台补贴、储值券、退款回放、授权占券、清结算、对账、投影、归档、冷热读取和治理重放尚未进入生产链路消费；解释视图、证据最小化和外部规则核验也只作为后续门禁。 |

准入结论：`B1-10` 已转为历史交付基线；若下一轮进入生产链路，必须重新声明目标 MVP 任务、写入范围、公共契约变更、表结构变更、测试矩阵和验证命令。

```text
任务切片：
覆盖 TDD 用例：
写入范围：
只读范围：
设计错漏：
已补设计：
测试新增：
实现变更：
验证命令：
验证结果：
残余风险：
下一批建议：
```

## 12.2 B1-10 实际交付记录（2026-05-21）

本轮按第 11 节 Execution Grant 完成 `B1-10 权益快照 DSL 契约` 的第一轮编码闭环，形成权益快照契约承载基线。

| 检查项 | 结论 | 证据 |
| --- | --- | --- |
| 任务 | 已完成 `B1-10 权益快照 DSL 契约`。 | 提交 `6be9c99 feat: 固化权益快照稳定摘要契约`。 |
| 写入范围 | 符合第 11 节授权范围。 | 写入 `core/src/main/java/com/wind/funds/spec/transaction`、`core/src/main/java/com/wind/funds/util`、`tests/src/test/java/com/wind/funds/dsl`、`tests/src/test/resources/dsl-contract-cases`；未写入 `transaction-*`、`wallet-*`、`ledger-*` 业务实现。 |
| 契约承载 | `FundsBenefitSnapshotSpec` 增加稳定摘要口径，避免 JSON 字段顺序、时间字段、运行态对象差异影响契约指纹。 | 新增 `FundsBenefitStableDigest`，`FundsBenefitSnapshotSpec#getStableDigest()` 输出稳定摘要。 |
| DSL 红线 | `fixtureLevel=CONTRACT_ONLY` 必须显式登记，避免把请求态样例误判为生产 Done。 | `FundsDslJsonContractVerifier` 增加 `fixtureLevel`、`scenarioCode`、`acceptanceIds`、`tddIds`、`systemDesignRefs` 和 `validation` 元数据校验。 |
| TDD 用例 | 已补稳定摘要、字段顺序、请求态样例、生产 Done 误判、闭合角色边界等契约测试。 | `FundsBenefitSnapshotSpecTests` 12 个用例通过；`FundsDslJsonContractTests` 8 个用例通过。 |
| 验证命令 | Java 21 下编译、目标测试、PMD 和空白检查通过。 | `just mvn-version`；`just compile`；`just test-one FundsBenefitSnapshotSpecTests tests`；`just test-one FundsDslJsonContractTests tests`；`just pmd`；`git diff --check`。 |
| 后续基线收敛 | 请求摘要、稳定摘要支撑、账务计划装配器长 ID、预算组默认周期 `LIFETIME`、账务事实断言、钱包/交易/治理边界、CAD 完整验证门禁、出款准入规则核验证据、编码准入设计冻结、提现/解冻红线、余额日志证据、路由事实边界、交易投影解释、权益回放摘要、治理重放差异校验、资金事实红线、敏感上下文阻断、上下文不可变、防御性拷贝、外部账户原文阻断、MVP/归档边界、设计交付口径、代码 CR 任务基线、代码准入 CR 基线、B2/B4 准入口径和钱包入口收敛已在后续提交中收敛；`9456ab6`、`81a7ecb`、`270122e` 和 `77bc9f4` 只作历史准入与局部保护证据。 | `75b46ef` 至 `5901265` 的历史基线证据，`5901265..77bc9f4` 的资金事实红线、敏感上下文和设计交付口径收敛证据，`9456ab6` 的 A0 准入与代码基线对齐证据，`3ef522c`、`a7d3fc9` 的授权支付工具入口和钱包应用层 CR 证据，`8e4a801` 的设计任务基线索引证据，`5a78f02` 的核心设计骨架修复证据，`f99800b` 的代码 CR 任务基线证据，`4a7ef12` 的代码准入 CR 基线证据，`98ec7cc..81a7ecb` 的后续资金红线、支付工具约束和文档准入口径收敛证据，以及 `270122e` 的 CAD 准入刷新和完整门禁通过证据。 |
| 残余风险 | 只能声明 `B1-10 契约承载 Done`。 | route、posting、replay、授权占券、清结算、对账、投影、归档、冷热读取和治理重放消费仍在 B3/B4/B6/B7/B8 后续 MVP 任务；解释视图、证据最小化和外部规则核验仍在 B6/B7/B8 后续 MVP 任务。 |

## 13. A1 建议 Execution Grant

A0 只读核验通过后，曾建议优先确认 A1 直接交易事实红线。A1 的完整候选授权卡见 `docs/TDD设计/A1-直接交易事实红线准入卡.md`；该卡仍可作为后续确认输入，但不再是当前默认恢复入口，也不因写入本 Harness Plan 而自动授权编码。

| 项 | 候选口径 |
| --- | --- |
| 任务切片 | A1 直接交易事实红线。 |
| 业务问题 | 直接交易成功或失败后，是否能解释状态、金额、route、posting、ledger entry、余额投影、幂等和审计证据。 |
| 首批 Red | `A1-RED-001` 成功链路事实完整性；必要时补 `A1-RED-002` 失败无副作用和幂等冲突。 |
| 目标测试资产 | `FundsDirectTransactionFlowTests`、`DefaultLedgerPostingAssemblerTests`、`LedgerBalanceProjectionServiceImplTests`。 |
| 默认写入范围 | 先写 A1 目标测试资产；Red 证明缺口后，只允许在 `transaction-impl`、`ledger-impl` 做最小修复。 |
| 默认禁止范围 | face/core 公共契约、DDL/H2 schema、A2/A3/A4、B7/B8、P2、生产配置、外部协议和敏感数据处理。 |
| 验证命令 | `just mvn-version`、`just compile`、`just test-one FundsDirectTransactionFlowTests tests`，必要时 `just test-transaction`、`just test-business-flow`、`just test-boundary`，提交前 `just pmd` 和 `git diff --check`。 |
| 当前状态 | 候选准入卡已补齐，并已记录现有覆盖扫描与完整门禁复核；后续如选择 A1，仍需用户重新确认后才成为实际 Execution Grant。 |

### 13.1 本轮编码准入复核（2026-05-31）

本轮按产品专家和架构师口径重新复核 A1、B2、B4、B5、B6、B7、B8 和 P2 的编码准入状态。复核只对齐文档和任务基线，不写生产代码、测试代码、DDL/H2 schema 或运行时配置。

| 能力域 | 本轮结论 | 后续动作 |
| --- | --- | --- |
| A1 直接交易事实红线 | `HISTORICAL_READY_TO_CONFIRM`。A1 曾是当轮最接近可确认的单一 Execution Grant 候选。 | 后续如选择 A1，需按最新 Git HEAD 和当前任务账本重新确认模板；未确认前只保留为准入卡。 |
| B2 钱包账户、支付工具和资金责任基础 | `BLOCKED_UNTIL_GRANT`。钱包 application facade、资金责任目标字段和 BudgetGroup 兼容策略仍需独立 Round 0 或授权卡。 | 不与 A1 混合编码；先确认 facade 命名、`targetSubjectType + targetSubjectId` 或 funding-account-only 策略、BudgetGroup 兼容/迁移策略。 |
| B4 授权支付工具入口 | `BLOCKED_UNTIL_GRANT`。可以设计 `authorizeByInstrument` 或等价 application facade，但不能替换账户主体型授权内核请求。 | 独立确认公共契约、Request/DTO、幂等摘要、route snapshot、拒绝事实和敏感上下文回归。 |
| B5/B6 余额控制和路由回放 | `BLOCKED_UNTIL_GRANT`。预算控制投影、BudgetGroup 兼容路径和交易投影重放仍需单独切片。 | 先决定预算组兼容策略和投影事实边界，再确认 Red 包和写入范围。 |
| B7/B8/P2 | `TDD_ANALYSIS_ONLY`。清结算对账、资金数据治理和业务能力包仍未打开默认编码。 | 继续独立 OpenSpec、Execution Grant 和 TDD 分析准备；不得作为 A1 或 B2 附带写入。 |

### 13.2 本轮编码准入对齐（2026-06-01）

本轮在 PRD、DSL、系分、TDD 和任务基线已固化支付工具入口后，再做一次编码准入裁决。该裁决只更新开工判断和授权边界，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。

| 能力域 | 准入状态 | 对齐结论 |
| --- | --- | --- |
| A1 直接交易事实红线 | `HISTORICAL_READY_TO_CONFIRM`。 | A1 曾是当轮单一 Execution Grant 候选；后续如重新选择 A1，必须按最新基线复核，不包含支付工具 application facade、VCC/VA/ACH/P2 场景交易入口或交易内核入参调整。 |
| B2 钱包账户、支付工具和资金责任基础 | `BLOCKED_UNTIL_GRANT`。 | 可准备独立 Round 0 或授权卡；必须先决定 application facade 命名、资金责任目标字段、BudgetGroup 兼容策略、Request/DTO 和边界测试。 |
| B4 授权支付工具入口 | `BLOCKED_UNTIL_GRANT`。 | 可设计 `authorizeByInstrument` 或等价 application facade；必须保留账户主体型授权内核，禁止把 canonical `authorize` 请求整体改为支付工具引用。 |
| B5/B6 余额控制、预算控制和路由回放 | `BLOCKED_UNTIL_GRANT`。 | 预算组不作为账务主体、预算控制投影、交易投影重放和换绑后原路径回放需要单独 Red 和写入范围。 |
| B7/B8/P2 | `TDD_ANALYSIS_ONLY`。 | 清结算对账、资金数据治理、VCC、全球账户和 ACH 只可继续做 TDD 分析、contract-only 或独立 Execution Grant 准备；收单只允许 design-only 文档、边界复核和差距登记。 |
| 本轮未提交设计变更 | `HISTORICAL_BASELINE_ATTACHMENT_REQUIRED`。 | 该限制属于当轮未提交状态；后续开工统一以确认时 Git HEAD 和最新任务账本为准。 |

### 13.3 B4-NO-AUTH-REFUND GSD-CAD 准入复核（2026-06-02）

本轮按 GSD-CAD 双层协议复核 B4 授权后继能力。复核只更新任务状态和授权边界，不写生产代码、测试代码、DDL/H2 schema 或运行时配置。

| 能力域 | 准入状态 | 对齐结论 |
| --- | --- | --- |
| B4-NO-AUTH-REFUND | `CLOSED_BY_006BCAA`。 | 设计、PRD、DSL、系分、TDD、B4 准入卡、OpenSpec、Red/Green 测试和最小实现已对齐；本任务包不再作为下一轮默认候选。 |
| 原子任务包 | `B4-NAR-CAD-001`。 | 已由 `006bcaa feat: 补齐无授权退款 canonical 能力` 消费；后续只作为回归和 CR 依据。 |
| 首轮 CAD Pick | `B4-NAR-RED-001`。 | 已回归化；成功路径和失败无副作用矩阵已进入 `FundsAuthorizationTransactionFlowTests`。 |
| 必须列名契约 | 请求字段为 `authorizationTransactionSn` 空值语义、`externalReferenceSn`、`refundReason`、`operator/contextVariables`；`NO_AUTH` 为内部上下文标签。 | Grant 必须说明字段名、类型、必填规则、摘要字段、普通授权链退款兼容策略，并说明 NO_AUTH 内部标签不得由请求侧传入，且不得携带或查询内部授权流水。 |
| 允许写入上限 | `CONSUMED_BY_006BCAA`。 | 实际写入范围为退款请求兼容字段、指令上下文 key、converter、authorization route resolver、lifecycle saver、route code、授权交易 flow 和 no-auth refund helper；未扩展 DDL/H2、ledger 公共契约、core 枚举状态、wallet facade 或支付工具 facade。 |
| 禁止混入 | `BLOCKED_BY_SCOPE`。 | 不混入 force settle 返工、chargeback 独立入口、支付工具 facade、VCC 生命周期、Spend Rule、DDL/H2、清结算对账、治理 apply、生产配置、外部协议或敏感数据处理。 |
| 停止条件 | `BLOCKED_BY_SCOPE`。 | 需要 DDL/H2、core 枚举或状态、ledger 公共契约、新依赖、外部规则、支付工具 facade、钱包 application facade、VCC、chargeback case、清结算追偿、敏感数据处理、公有方法超过 5 个参数或工作树冲突时停止。 |
| 验证命令 | `PASSED`。 | `006bcaa` 前已通过 `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check` 和 `git diff --cached --check`；2026-06-03 追加 `just test-business-flow` 通过 106 tests。 |

### 13.4 当前 GSD-CAD 恢复入口（2026-06-02）

本节记录 `51e86e3 docs: 固化 B4 无授权退款恢复入口` 和 `3e5ec76 docs: 对齐 B4 无授权退款确认基线` 之后的历史恢复入口。13.1 和 13.2 中的 A1 直接交易事实红线是 2026-05-31 和 2026-06-01 的历史裁决；B4-NO-AUTH-REFUND 后续已获得用户授权并由 `006bcaa` 闭合，B4-DISPUTE-SEMANTIC-ALIGNMENT 后续已由 `949b24a` 闭合，本节不再作为当前恢复入口。

| 恢复项 | 当前口径 |
| --- | --- |
| 当前 Git 基线 | 确认时 Git HEAD；必须包含 `8e1ec76 docs: 补齐 B4 无授权退款 Grant 执行包`、`51e86e3 docs: 固化 B4 无授权退款恢复入口` 和 `3e5ec76 docs: 对齐 B4 无授权退款确认基线`，后续 docs-only 提交无需逐条追写到本行。 |
| 当前 GSD 候选 | 历史候选 `B4-NO-AUTH-REFUND` 已闭合；后续曾推进的 `B4-DISPUTE-SEMANTIC-ALIGNMENT` 也已由 `949b24a` 闭合；当前没有新的默认编码候选。 |
| 当前 CAD 原子任务包 | 历史任务包 `B4-NAR-CAD-001` 已由 `006bcaa` 消费。 |
| 当前准入状态 | `CLOSED_BY_006BCAA_THEN_949B24A`；下一轮必须重新选择并确认单一 Execution Grant。 |
| 首轮 Pick | 历史 Pick `B4-NAR-RED-001` 已回归化。 |
| 必须确认的 Grant 契约 | 请求字段为 `authorizationTransactionSn` 空值语义、`externalReferenceSn`、`refundReason`、`operator/contextVariables`、普通授权链退款兼容策略；`NO_AUTH` 为内部上下文标签，且不得携带或查询内部授权流水。 |
| 当前禁止范围 | 支付工具 facade、钱包 application facade、VCC 生命周期、DDL/H2 schema、ledger 公共契约、core 枚举状态、Spend Rule、force settle 返工、chargeback case、清结算追偿、治理 apply、生产配置、外部协议、敏感数据处理。 |
| 下一步入口 | `B4-DISPUTE-SEMANTIC-ALIGNMENT` 和 `B4-AUTH-RACE` 不再作为下一步入口；后续可重新确认 B4-AUTH-INSTRUMENT-APPLICATION、B4-BENEFIT-AUTH、完整 dispute/chargeback case 或 A1/B2/B5/B6 中一个单一 Execution Grant。 |

### 13.5 Execution Grant 准备完成记录（2026-06-03）

本节关闭计划内 “为编码做准备，完成 Execution Grant 任务” 的 docs-only 准备项，并记录其后续被 `006bcaa` 消费为代码闭环。该记录不授权新的编码，只作为 B4-NO-AUTH-REFUND 历史授权包、执行边界和回归依据。

| 项 | 当前口径 |
| --- | --- |
| 任务状态 | `CLOSED_BY_006BCAA`。 |
| 单一任务包 | `B4-NAR-CAD-001`，只处理 `settleRefund` 无授权退款模式，已由 `006bcaa` 消费。 |
| 授权正文 | 历史确认文本为 `docs/TDD设计/B4-授权后继能力Round0准入卡.md` 第 11 节 `Execution Grant：B4-NO-AUTH-REFUND`。 |
| 首批 Red | `B4-NAR-RED-001` 和最小 `B4-NAR-RED-002` 失败矩阵已回归化。 |
| 写入上限 | 历史上限为先写授权退款 flow 测试；Red 证明缺口后，仅允许 `FundsAuthorizationTransactionRefundRequest` 兼容字段、transaction converter/command/lifecycle/route replay/request summary 最小修复。实际闭环未新增 DDL/H2、ledger 公共契约、core 枚举状态、wallet facade 或支付工具 facade。 |
| 禁止范围 | 支付工具 facade、钱包 application facade、VCC 生命周期、DDL/H2 schema、ledger 公共契约、core 枚举状态、Spend Rule、force settle 返工、chargeback case、清结算追偿、治理 apply、生产配置、外部协议和敏感数据处理。 |
| 验证命令 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`。 |
| 下一动作 | B4-NO-AUTH-REFUND 不再作为下一动作；需要重新确认一个新的单一 Execution Grant。 |

### 13.6 B4-NAR 首轮 Red 触点扫描（2026-06-03）

本节记录 Grant 前 Git 基线上的只读代码扫描结论，作为 `B4-NAR-CAD-001` 写首轮 Red 的历史执行输入。当前缺口已由 `006bcaa` 关闭；下表保留为执行 provenance，不代表当前代码状态。

| 触点 | Grant 前扫描事实 | 执行含义 |
| --- | --- | --- |
| Request | Grant 前 `FundsAuthorizationTransactionRefundRequest.authorizationTransactionSn` 仍为 `@NotNull`，且无 no-auth 语义、外部引用、原因或审计最小集字段。 | `B4-NAR-RED-001` 应先证明现有 `settleRefund` 无法表达无授权退款。 |
| Converter | `convertToSettleRefundInstruction` 无条件构造 `AUTHORIZATION` reference，并把 `AUTHORIZATION_TRANSACTION_SN` 放入上下文。 | NO_AUTH Green 只能在 Grant 范围内最小切开普通授权链退款和无授权退款，不得让无授权退款携带或查询内部授权流水。 |
| Ledger reference | `authorizationReference(...)` 会查找原授权 `AUTHORIZE` ledger transaction 并要求唯一。 | 首轮 Red 的预期失败点应与内部授权流水和原授权账本依赖有关；若 Red 未失败，先暂停判断已有实现覆盖或 Red 写错。 |
| Command service | `FundsTransactionCommandServiceImpl#settleRefund` 当前只是委派 converter。 | Green 不应优先在 command service 堆叠分支，除非 Red 证明 command 入口必须承载额外 guard。 |
| Test support | `refundSettledAuthorization(...)` 总是传入普通授权链 `authorizationTransactionSn`。 | 获得 Grant 后需新增 no-auth refund 专用测试构造，并保持普通授权链 helper 回归不退化。 |
| 当前状态 | `CLOSED_BY_006BCAA`。 | 后续扩展需重新扫描当前代码并另起 Grant。 |

### 13.7 B4-NAR 最小 Green 地图（2026-06-03）

本节把实现触点压缩成 Grant 后的最小 Green 约束。该地图已服务 `B4-NAR-CAD-001` 并由 `006bcaa` 消费，后续作为 CR 依据保留；其中 resolver selection 的当前代码基线已由 `967586c` 进一步更新为“no-auth `AUTH_REFUND` 支持外部引用回退，普通授权链 `AUTH_REFUND` 仍不走 resolver”。

| 触点 | Grant 前扫描事实 | Grant 后执行约束 |
| --- | --- | --- |
| Resolver selection | Grant 前 `RouteReplaySupport` 不把 `EXTERNAL_TRANSACTION` 当 route snapshot reference，且 `AuthorizationFundsInstructionRouteResolver#supports` 不支持普通 `AUTH_REFUND`；`967586c` 后，no-auth `AUTH_REFUND` 可在内部 `REFUND_MODE` 缺失时由 `EXTERNAL_TRANSACTION` reference 进入 no-auth resolver，显式 `DISPUTE` 或其他退款归类仍不被覆盖。 | NO_AUTH 不能通过伪造内部 authorization reference 进入 replay；普通授权链 `AUTH_REFUND` 仍不走 resolver，no-auth 路径必须证明不按当前绑定关系静默重选路。 |
| Replay guard | `DefaultRouteReplayService` 已有缺 reference、缺 route snapshot、当前工具换绑、当前资金来源变化不影响 replay 的边界测试。 | Green 不得削弱 replay 红线；新增 no-auth 路径时要保留这些测试，并补对应路由解释断言。 |
| Lifecycle aggregate | `DefaultFundsInstructionLifecycleSaver#findReferenceTransaction` 会复用内部交易 reference；`EXTERNAL_TRANSACTION` 不复用内部交易聚合。 | NO_AUTH 应形成独立且可追溯的退款交易事实；不能把外部引用映射成内部授权交易。 |
| Amount control | `AUTH_REFUND` / `REFUND` 默认校验已结算可回退金额，交易类型为 `REFUND` 的聚合会跳过内部 settled reversible amount 校验。 | NO_AUTH 首轮不使用调用方自报金额币种作为资金层上限；金额红线由账户余额、账务平衡和后续累计控制 Grant 继续兜底。 |
| Idempotency summary | `requestHash` 纳入 reference、contextVariables、route summary 和 participant summary。 | `NO_AUTH` 内部标签、`externalReferenceSn` 和原因必须进入摘要或等价不可变事实，避免幂等误合并。 |
| Boundary tests | `DefaultRouteReplayServiceTests` 已覆盖 replay 不依赖当前 route selection port。 | Grant 后除了 flow Red，还应保留或补充 route replay 边界测试；若修改 resolver 支持矩阵，必须跑 `just test-one DefaultRouteReplayServiceTests tests` 或纳入 `just test-transaction`。 |

### 13.8 B4-NAR 首轮 Red 断言包（2026-06-03）

本节把 `B4-NAR-RED-001` 的测试断言收敛成可执行包。该包已被 `006bcaa` 消费，后续作为回归和 CR 依据保留。

| 断言层 | Grant 后 Red 目标 | 执行停止点 |
| --- | --- | --- |
| Request | no-auth refund 以 `authorizationTransactionSn` 为空判定，带 `externalReferenceSn`、原因和操作者，且不带内部授权流水。 | 现有 Request 无法表达字段时，Red 可先失败在编译或构造层；未经 Grant 不提前改契约。 |
| Money fact | 成功路径应证明用户 `AVAILABLE` 增加、平台 `SETTLEMENT` 减少，`AUTHORIZATION` 不增加、不释放、不伪造授权占用。 | 测试只能断状态或返回值时停止，补齐余额、route、posting、entry 和投影断言。 |
| Funds fact | no-auth refund 应形成独立且可解释的退款资金事实，不能复用内部授权交易聚合。 | 如果实现只能通过内部 `AUTHORIZATION` reference 成功，必须停止并修正 Green 方向。 |
| Ledger fact | ledger transaction、posting plan、ledger entry 必须跟 route snapshot 对齐，退款 phase 覆盖 `SETTLEMENT` 和 `AVAILABLE`。 | route snapshot 不可解释或 posting/entry 与 route leg 不一致时不得提交。 |
| Audit/idempotency | `externalReferenceSn`、原因和 `NO_AUTH` 内部归类标签进入摘要或等价不可变事实；同 `businessSn` 不同外部引用失败且无新增账务事实。 | 若摘要无法区分 no-auth 和普通授权链退款，先修摘要，不扩大到 DDL/H2 或治理。 |
| Regression | 普通授权链 full refund、dispute refund、chargeback 和 refund idempotent 场景不退化。 | 任一普通授权链回归失败，先判断是否越界破坏原语义，不继续扩展 no-auth。 |
| Expected Red | 当前基线预期失败在 Request 契约、converter 内部授权 reference、原授权 ledger transaction 查询或 route resolver 不支持 no-auth 路径。 | Red 未失败时暂停，不进入 Green。 |

### 13.9 B4-NAR Grant 前准备饱和度（2026-06-03）

本节记录 `B4-NAR-CAD-001` 在未获 Execution Grant 前的最后恢复入口。该入口已由用户确认并被 `006bcaa` 消费；当前仅作为历史恢复入口和执行 provenance，不授权写 Java、测试、DDL/H2 schema 或运行时配置。

| 准备面 | 状态 | 说明 |
| --- | --- | --- |
| Grant 可执行包 | `DONE`。 | 第 13.5 已确认授权正文、写入上限、禁止范围、验证命令和下一动作。 |
| 首轮触点扫描 | `DONE`。 | 第 13.6 已记录 Request、converter、ledger reference、command service 和 test support 的当前事实。 |
| 最小 Green 地图 | `DONE`。 | 第 13.7 已记录 resolver selection、replay guard、lifecycle aggregate、amount control、idempotency summary 和 boundary tests 的执行约束。 |
| 首轮 Red 断言包 | `DONE`。 | 第 13.8 已记录 Request、Money fact、Funds fact、Ledger fact、Audit/idempotency、Regression 和 Expected Red。 |
| 当前状态 | `CLOSED_BY_006BCAA`。 | Grant 已消费；下一步必须重新确认新的单一 Execution Grant，不能继续沿用 B4-NO-AUTH-REFUND 授权。 |

### 13.10 B4-DISPUTE-CHARGEBACK 只读准入裁决（2026-06-03）

本节记录 B4-NO-AUTH-REFUND 闭环后的 GSD-CAD 下一候选复核。该复核后续已推进为第 13.11 的 `B4-DISPUTE-SEMANTIC-ALIGNMENT` 候选，并由 `949b24a` 消费；本节只保留历史准入裁决，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。

| 检查项 | 当前结论 |
| --- | --- |
| 历史候选 | `B4-DISPUTE-CHARGEBACK-R0`。目标是裁决拒付/争议承接的 canonical 入口语义，而不是立刻实现完整 chargeback case。 |
| 当前状态 | `SUPERSEDED_BY_949B24A`；当时状态为 `SEMANTIC_DECISION_REQUIRED_NOT_CODE_AUTHORIZED`。 |
| 现有代码事实 | 代码已有 `FundsAuthorizationTransactionService#chargeback`、`FundsAuthorizationTransactionChargebackRequest`、`CHARGEBACK` eventType、route replay `CHARGEBACK` phase 和生命周期金额校验。 |
| 现有测试事实 | `FundsAuthorizationTransactionFlowTests` 已覆盖争议退款走 `settleRefund` 并保留审计上下文、独立 `chargeback` 成功、chargeback 超已完成金额失败无副作用、同业务流水不同摘要失败无副作用。 |
| 目标设计事实 | PRD、DSL、系分和 B4 准入卡要求拒付与普通退款、授权拒绝可区分；但目标态不要求强制调用独立 `chargeback` 服务入口，默认可通过 `settleRefund` 携带拒付原因、凭证、外部引用和审计上下文承接。 |
| 差异定性 | 现有 `chargeback` 是比目标态最小语义更强的实现资产，不能直接作为下一轮 canonical API 目标。当轮判断是先证明 `settleRefund` 争议语义在查询、投影、审计和幂等摘要上可区分，而不是先扩展独立 `chargeback` 入口。 |
| 验证证据 | 2026-06-03 执行 `just test-one FundsAuthorizationTransactionFlowTests tests` 通过 24 tests。 |
| 建议下一步 | 已由 `949b24a` 闭合首轮 `B4-DISPUTE-SEMANTIC-ALIGNMENT`；后续完整 dispute/chargeback case 或独立 `chargeback` 一等 API 仍需新的 Execution Grant。 |
| 禁止混入 | 不新增 dispute case、清结算追偿、VCC processor、外部卡组织规则、DDL/H2、core 枚举或状态、ledger 公共契约、支付工具 facade、生产配置、外部协议或敏感数据处理。 |

### 13.11 B4-DISPUTE-SEMANTIC-ALIGNMENT Grant 候选包（2026-06-03）

本节把 B4-DISPUTE-CHARGEBACK 从只读裁决推进到可确认的单一 Execution Grant 候选。该候选后续已由用户确认并被 `949b24a fix(transaction): 对齐授权争议退款审计语义` 消费；本节只作为历史候选包和授权 provenance 保留，不再授权新的写入。

| 项 | 当前口径 |
| --- | --- |
| 任务状态 | `CONSUMED_BY_949B24A`。 |
| 单一任务包 | `B4-DISPUTE-SEMANTIC-ALIGNMENT`，只处理已完成授权后的争议/拒付承接语义与可区分性。 |
| canonical 决策 | 首轮默认 `settleRefund` 为拒付/争议目标态主入口；既有 `FundsAuthorizationTransactionService#chargeback` 保留为兼容、显式事件或内部适配资产，不在本轮扩展为目标态主入口。 |
| 首批 Red | `B4-CB-RED-001A`：争议退款通过 `settleRefund` 承接时，查询、投影、审计上下文和幂等摘要必须能区分普通授权链退款、NO_AUTH 退款、拒付承接和授权拒绝。 |
| 可选第二 Red | `B4-CB-RED-001B`：授权拒绝不得生成拒付事实；缺拒付原因、缺凭证、缺外部引用或超已完成可回退金额时失败且无资金副作用。 |
| 写入上限 | 先写授权交易 flow 目标 Red；Red 证明缺口后，仅允许授权退款请求兼容字段、transaction converter/lifecycle/route replay/request summary、交易投影解释和 TDD tests 最小修复。 |
| Harness 范围索引 | 标准 Harness 字段已回填到 B4 准入卡第 8.10：写入范围和写入文件先限授权交易 flow 目标 Red，Red 证明缺口后才进入 transaction-face 授权退款请求兼容字段、transaction-impl converter/lifecycle/route replay/request summary 和交易投影解释；只读范围和只读参考为 PRD、DSL、系分、TDD、OpenSpec、既有 `transaction-*`、`ledger-*` 和 H2 schema。 |
| 禁止范围 | 完整 dispute case、chargeback case 生命周期、清结算追偿、VCC processor、支付工具 facade、钱包 application facade、Spend Rule、DDL/H2 schema、core 枚举状态、ledger 公共契约、治理 apply、生产配置、外部协议和敏感数据处理。 |
| 验证命令 | `just test-one FundsAuthorizationTransactionFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`。 |
| 下一动作 | 已由第 13.12 回写首轮 CAD 闭环；后续必须重新选择单一 Execution Grant。 |

### 13.12 B4-DISPUTE-SEMANTIC-ALIGNMENT CAD 闭环（2026-06-03）

本节回写 GSD-CAD state、verification matrix 和 handoff。`B4-DISPUTE-SEMANTIC-ALIGNMENT` 已完成首轮自动推进并提交；该完成态只覆盖 `B4-CB-RED-001A / TDD-RED-017B` 的 canonical 可区分性，不扩大为完整 dispute/chargeback case、清结算追偿或外部规则能力。

| 项 | 当前口径 |
| --- | --- |
| 当前状态 | `CLOSED_BY_949B24A`。 |
| 提交 | `949b24a fix(transaction): 对齐授权争议退款审计语义`。 |
| 完成内容 | `FundsAuthorizationTransactionRefundRequest` 新增 `disputeMode`、`disputeReason`、`disputeVoucherRef`、`externalDisputeRef` 一等字段，请求仍不暴露 `refundMode`；converter 只在争议字段出现时写入内部 `DISPUTE` 上下文并校验原因、凭证、外部引用和模式完整；route replay 只对 `AUTH_REFUND + DISPUTE` 传播争议审计上下文。 |
| 执行纪律 / Superpowers | 已按 TDD 先 Red 后 Green 推进；Review 覆盖资金语义、公共契约、route replay 污染边界、编码红线和测试断言质量；Refactor 仅限授权争议退款最小触点；AI 产物复核通过验证矩阵和 `git diff --check` 收口。 |
| 写入范围 | `transaction/transaction-face`、`transaction/transaction-impl` 和 `FundsAuthorizationTransactionFlowTests` 的授权争议退款最小触点；未修改 DDL/H2 schema、core 枚举状态、ledger 公共契约、wallet、payment instrument、Spend Rule、治理或生产配置。 |
| 验证矩阵 | `just compile` 通过；`just test-one FundsAuthorizationTransactionFlowTests tests` 通过 26 tests；`just test-transaction` 通过 94 tests；`just test-business-flow` 通过 108 tests；`just test-boundary` 通过 126 tests；`just pmd` 通过；`git diff --check` 通过。Spring 测试在沙箱内因 embedded Redis 端口绑定触发 `Operation not permitted`，已按权限规则非沙箱重跑并通过。 |
| 残余风险 | 完整 dispute case、独立 `chargeback` 一等目标 API、清结算追偿、VCC processor、外部卡组织规则、运营审批、查询投影解释增强和合规/会计最终口径仍未完成。 |
| handoff | 下一轮不能继续消费第 13.11；`B4-AUTH-RACE` 后续也已由第 13.14 闭合；必须在授权支付工具应用入口、授权权益生命周期、完整 dispute/chargeback case 或其他经过 Round 0 的单一任务包中重新确认 Execution Grant。 |

### 13.13 B4-AUTH-RACE Round 0 与 Grant 候选（历史，2026-06-03）

本节记录 B4-DISPUTE-SEMANTIC-ALIGNMENT 闭环后的下一轮 GSD-CAD 只读准入。该准入只读取当时的代码、测试和任务材料，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置；后续已由 `47c5269` 消费为第 13.14 的首轮 CAD 闭环。

| 项 | 当前口径 |
| --- | --- |
| 当前状态 | `CONSUMED_BY_47C5269`；历史状态曾为 `ROUND0_READY_NOT_CODE_AUTHORIZED`。 |
| 单一任务包 | `B4-AUTH-RACE`，只处理同一授权后续事件的并发竞争红线。 |
| 现有覆盖 | `FundsAuthorizationTransactionFlowTests` 已覆盖部分完成后过期、过期超剩余失败、撤销超剩余失败、退款/chargeback 超已完成失败，以及 reversal、settle、refund、chargeback 幂等摘要冲突；这些只证明顺序路径和幂等，不证明并发竞争。 |
| 只读实现观察 | 当时扫描观察到 `DefaultFundsInstructionLifecycleSaver#markSucceeded` 以普通读取、内存累计和 `update(transaction)` 更新授权聚合金额，未见同一原授权后续事件的状态版本、行锁、唯一约束或串行化测试证据；该缺口后续已由 `47c5269` 首轮闭合。 |
| 首批 Red | `B4-RACE-RED-001`：同一授权的 settle 与 expire、settle 与 reversal，必要时 settle 与 settleRefund 并发竞争时，只能有一个合法金额迁移获胜，失败方无 route、posting、ledger entry、projection 或余额变化；该 Red 已回归化。 |
| 允许写入建议 | 历史建议为用户确认后先写 `FundsAuthorizationTransactionFlowTests` 或同包新增 `FundsAuthorizationTransactionRaceFlowTests`；Red 证明缺口后只允许在 `transaction-impl` 生命周期保存、编排串行化或等价最小锁策略内修复。该范围已由 `47c5269` 消费。 |
| 禁止混入 | 公共请求字段、core 枚举/状态、ledger 公共契约、DDL/H2 schema、支付工具 facade、钱包 application facade、VCC、Spend Rule、完整 dispute/chargeback case、清结算追偿、治理 apply、生产配置、外部协议或敏感数据处理。 |
| 验证命令 | `just test-one FundsAuthorizationTransactionFlowTests tests` 或 `just test-one FundsAuthorizationTransactionRaceFlowTests tests`、`just test-transaction`、`just test-business-flow`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`。 |
| handoff | `B4-AUTH-RACE` 不再作为下一轮默认恢复入口；若后续要扩展更深并发语义、数据库约束、版本字段或跨节点锁，需要新的单一 Execution Grant。 |

### 13.14 B4-AUTH-RACE CAD 闭环（2026-06-03）

本节回写 GSD-CAD state、verification matrix 和 handoff。`B4-AUTH-RACE` 已完成首轮自动推进并提交；该完成态只覆盖账户主体型授权内核的同一授权后继事件并发竞争，不扩大为跨节点分布式锁、数据库唯一约束、完整 dispute/chargeback case 或支付工具 application facade。

| 项 | 当前口径 |
| --- | --- |
| 当前状态 | `CLOSED_BY_47C5269`。 |
| 提交 | `47c5269 fix(transaction): 串行化授权后继并发竞争`。 |
| 完成内容 | 新增 `B4-RACE-RED-001` 对应的授权后继并发 flow 覆盖，证明同一授权的 settle / expire / reversal 并发竞争只有一个赢家；失败方无 route、posting、ledger entry、projection 或余额变化。实现层在授权后继命令进入编排前按原授权流水串行化，并在事务完成前持有 JVM 锁；读取授权原交易时使用 `FOR UPDATE` 行锁，完成、撤销和过期继续校验剩余可迁移金额。 |
| 执行纪律 / Superpowers | 已按 TDD 先 Red 后 Green 推进；Review 覆盖资金语义、事务边界、锁生命周期、边界方向、公共契约不变、测试断言质量和无关修改；Refactor 仅限授权并发最小触点；AI 产物复核通过验证矩阵和 `git diff --check` 收口。 |
| 写入范围 | `FundsAuthorizationTransactionFlowTests`、`FundsTransactionCommandServiceImpl` 和 `FundsTransactionMapper` 的授权并发最小触点；未修改 DDL/H2 schema、core 枚举状态、ledger 公共契约、wallet、payment instrument、Spend Rule、治理或生产配置。 |
| 验证矩阵 | `git diff --check` 通过；`just compile` 通过；`just test-one FundsAuthorizationTransactionFlowTests tests` 通过 28 tests；`just test-transaction` 通过 96 tests；`just test-business-flow` 通过 110 tests；`just test-boundary` 通过 126 tests；`just pmd` 通过。 |
| 残余风险 | 仅证明当前单 JVM/H2/Spring 事务测试边界下的首轮授权后继并发串行化；跨节点分布式锁、数据库唯一约束、版本字段、完整 dispute/chargeback case、支付工具授权 application facade、授权权益生命周期、清结算追偿、VCC processor、Spend Rule 和治理 apply 仍未完成。 |
| handoff | 下一轮不能继续消费第 13.13 或本节；必须在授权支付工具应用入口、授权权益生命周期、完整 dispute/chargeback case、余额控制调账、原路径回放或其他经过 Round 0 的单一任务包中重新确认 Execution Grant。 |

### 13.15 B4-AUTH-PI Round 0 与 Grant 候选（2026-06-03）

本节记录 B4-AUTH-RACE 闭环后的 GSD-CAD 自动推进结果。该轮只读复核 PRD、DSL、系分、TDD、OpenSpec、当前代码和既有测试资产，把下一候选收敛为授权支付工具应用入口；本节不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 项 | 当前口径 |
| --- | --- |
| 当前状态 | `CONSUMED_BY_GSD2_B2_WALLET_AUTHORIZATION_ADMISSION_001`。 |
| 单一任务包 | `B4-AUTH-INSTRUMENT-APPLICATION` / `B4-AUTH-PI`，只处理 `authorizeByInstrument` 或等价 application facade 的授权前准入、拒绝无副作用和委派账户主体型授权内核。 |
| 业务问题 | 业务方以卡、VCC、共享卡、外部钱包端点、通道 token 或等价工具引用发起授权时，系统能否解释工具、绑定、使用主体、Spend Rule、资金责任和账户能力为什么通过或拒绝，并保证资金影响仍落到已解析账户主体。 |
| 代码扫描事实 | `FundsAuthorizationTransactionService#authorize` 接收 `FundsAuthorizationTransactionAuthorizeRequest`；该请求以 `FundsAccountId accountId` 作为账户主体型 canonical 入参。`PaymentInstrumentService` 只管理工具和绑定，`SpendSubjectFundingRelationService` 只维护资金责任关系。2026-06-03 当时未发现 `AuthorizationAdmissionApplicationService`、`PaymentInstrumentCapabilityApplicationService` 生产实现或 `authorizeByInstrument` 入口；后续已由 `GSD2-B2-WALLET-APPLICATION-FACADE-002 / B2-PI-CAP-CAD-001` 补齐 `PaymentInstrumentCapabilityApplicationService` 首轮 Green，并由 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 补齐 `AuthorizationAdmissionApplicationService` 最小服务流。 |
| 现有测试证据 | `AuthorizationAdmissionApplicationServiceTests`、`PaymentInstrumentCapabilityApplicationServiceTests`、`FundingResponsibilityResolutionApplicationServiceTests`、`FundsAccountCapabilityApplicationServiceTests`、`PaymentInstrumentPreTransactionSnapshotApplicationServiceTests`、`SpendControlAdmissionApplicationServiceTests`、`RouteSnapshotJsonSupportTests` 和 `FundsAuthorizationTransactionFlowTests` 可作为授权准入、工具能力准入、资金责任解析、账户能力来源、预交易快照、支出控制准入快照、route snapshot 支付工具回链和账户主体型授权内核委派的最小服务流证据；仍不证明完整 Spend Rule 控制活动、预算控制投影或 VCC facade 已可用。 |
| 首批 Red | `R0-AUTH-001`：绕过工具准入、绑定快照、Spend Rule、资金责任或账户能力直接调用授权内核必须失败；拒绝路径不得生成 route、posting、LedgerEntry、projection 或敏感上下文副作用；批准路径只能构造账户主体型 `FundsAuthorizationTransactionAuthorizeRequest` 并委派 `FundsAuthorizationTransactionService#authorize`。 |
| 候选写入上限 | 只有在用户确认新的单一 Execution Grant 后，才允许新增 wallet application facade 契约、Request/DTO、最小 impl、授权准入组合测试和必要的授权 flow 回归。任何公共契约、幂等摘要、route snapshot、敏感上下文或 DTO 字段都必须在 Grant 中列名。 |
| 当前禁止范围 | 不替换 `FundsAuthorizationTransactionAuthorizeRequest.accountId`；不新增统一 `InstrumentTransactionService`；不让支付工具、预算组或 Spend Rule 成为 route leg、posting、LedgerEntry 或账本余额主体；不混入完整 VCC 发卡、Spend Rule 引擎、B5/B6/B8、清结算对账、治理 apply、DDL/H2 schema、外部协议或敏感原文。 |
| 建议验证命令 | Round 0 / docs-only：`git diff --check`。获得 Grant 后建议按写入范围运行 `just test-one PaymentInstrumentServiceImplTests tests`、`just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests`、`just test-transaction`、`just test-boundary`、`just compile` 和 `just pmd`。 |
| handoff | `B4-AUTH-PI` 的授权准入最小服务流已由 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 消费，route snapshot 回链已由 `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 消费，账户能力来源、预交易快照和支出控制准入已分别由 `GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001` 消费；下一步若继续 wallet，应在完整 Spend Rule 控制活动 / 预算控制投影或 VCC facade 中重新确认单一 Grant，不得沿用本节扩展 VCC、Spend Rule 或统一支付工具交易内核。 |

### 13.16 B4-AUTH-PI Grant 可执行包（2026-06-03）

本节把第 13.15 的 Round 0 候选推进为可确认的单一 Execution Grant 包。该包仍处于待确认状态；用户确认 `Execution Grant：B4-AUTH-PI` 前，不授权生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置写入。

| 项 | 当前口径 |
| --- | --- |
| 当前状态 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。 |
| Task ID | `B4-AUTH-PI-CAD-001`。 |
| Owner / 角色 | 资深架构师负责 Red、Green、Review、Refactor、Verify 和提交；产品架构专家负责支付工具、资金主体、Spend Rule、拒绝原因和 Not Done 语义复核。 |
| authority baseline | 确认时 Git HEAD；至少包含 `88d80c7 docs: 收敛授权后继索引基线`、`7b49684 docs: 记录授权支付工具候选门禁`、`be3df9f docs: 同步授权工具候选索引`、`c58431e docs: 同步 TDD 授权工具候选索引`、`226dfc2 docs: 回写授权工具索引流水` 和 `dc88ae2 docs: 收敛授权工具候选确认基线`，确保已闭合授权后继回归基线、无授权退款路由回退、`B4-AUTH-PI-CAD-001` 结构门禁、索引同步和恢复入口均已纳入确认语境。未提交文档变更必须先提交或列入 Grant 附件。 |
| 写入范围 | 先写 `AuthorizationAdmissionApplicationServiceTests` 或等价授权 application facade 目标 Red；Red 证明缺口后，仅允许 `wallet-face` 新增 application facade 契约、Request/DTO，`wallet-impl` 新增最小实现和委派适配，并按需补授权 flow 回归。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、既有 wallet/transaction/core、H2 schema 和资源服务测试。 |
| 首批 Red | `R0-AUTH-001`：支付工具授权入口必须先完成工具、绑定、Spend Rule、资金责任和账户能力准入；批准后委派账户主体型授权内核；准入失败无 route、posting、LedgerEntry、projection 或敏感上下文副作用；`approved=false` 授权拒绝只生成标准拒绝交易事实，route legs 为空且无 posting、LedgerEntry 或余额影响。 |
| 验证命令 | `just test-one AuthorizationAdmissionApplicationServiceTests tests`、`just test-one PaymentInstrumentServiceImplTests tests`、`just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-one PaymentInstrumentRouteDslContractTests tests`、`just test-transaction`、`just test-boundary`、`just compile`、`just pmd`、`git diff --check`。 |
| Superpowers / CAD 纪律 | TDD 先 Red 后 Green；Review 检查资金语义、模块依赖、公共契约和无副作用；Refactor 只限测试保护下最小触点；AI 产物复核必须证明没有替换 canonical 授权请求或新增工具交易内核。 |
| Git 策略 | 若用户确认 Grant 并保持自动模式，验证通过且未触发停止条件时按 `auto_commit` 提交；否则转 `summary_only`。 |
| 禁止事项 | 不替换 `FundsAuthorizationTransactionAuthorizeRequest.accountId`；不新增统一 `InstrumentTransactionService`；不让支付工具、预算组或 Spend Rule 成为 route leg、posting、LedgerEntry 或账本余额主体；不混入 DDL/H2 schema、core 枚举、ledger 公共契约、完整 VCC、Spend Rule 引擎、清结算对账、治理 apply、P2 轨道、外部协议或敏感原文。 |
| 停止条件 / 人工确认 | 需要公共契约越界、表结构、目标主体字段迁移、依赖方向反转、外部规则、PCI/PAN/CVV、敏感数据、P2/清结算/治理越界、验证无法解释失败或工作树冲突时停止。 |
| handoff | 候选包已具备 CAD 候选结构；下一步只能等待用户确认 `Execution Grant：B4-AUTH-PI` 后进入 Red，或继续选择其他 Round 0。 |

## 14. B2 建议 Execution Grant

B2 是后续直接交易、授权交易、余额控制、退款和权益资金流进入真实组合验证前的基础门禁。建议对应 MVP 任务先处理账本、账目、余额投影，再承接钱包账户和资金责任基础；支付工具基础能力归入后置支持队列，不进入直接交易、授权交易或权益生产消费链路。

### 14.1 Round 0 开工核验

B2 开工前先做 Round 0，只核验不写入。Round 0 的输出是“是否建议进入 B2 Execution Grant”，不是代码完成结论。

| 核验项 | 必须确认 | 失败处理 |
| --- | --- | --- |
| 基线 | PRD、DSL、系分、TDD、OpenSpec、Harness Plan 和 Git 提交点已冻结或明确列为本任务附件。 | 未冻结时先提交或纳入 Execution Grant，不能开工。 |
| 工作树 | `git status --short` 中未提交变更已分类为允许读取、允许写入或忽略。 | 未分类变更不得作为 Done 证据；影响 B2 时先停下确认。 |
| 现有测试资产 | `ControlAccountLedgerInitializationTests`、`FundingAccountServiceImplTests`、`PlatformFundingAccountServiceImplTests`、`PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`FundsSubjectBalanceQueryServiceImplTests`、`LedgerBalanceProjectionServiceImplTests`、`WalletLayerBoundaryTests` 可定位。 | 目标测试资产不存在或命名漂移时，先修正任务清单。 |
| 写入边界 | B2 只允许 wallet、ledger 和 tests 范围；`transaction-*`、权益消费、清结算、对账、归档和指标实现保持禁止写入。 | 需要越界时重新确认 Execution Grant。 |
| 验证环境 | `just mvn-version`、`just compile`、`just test-ledger`、`just test-boundary`、`just pmd` 可作为目标门禁。 | Maven/JDK/私服问题需记录环境限制，不能写成代码失败。 |

### 14.2 B2 最小 Red 包

| Red ID | 目标行为 | 目标测试资产 | 验证 |
| --- | --- | --- | --- |
| B2-RED-001 | 账户角色、显式建账和平台账户解析错误必须失败。 | `ControlAccountLedgerInitializationTests`、`FundingAccountServiceImplTests`、`PlatformFundingAccountServiceImplTests`。 | `just test-ledger`。 |
| B2-RED-002 | 支付工具状态、方向、账户能力和敏感数据治理错误必须失败。 | `PaymentInstrumentServiceImplTests`、`PaymentInstrumentRouteDslContractTests`。 | `just test-one PaymentInstrumentServiceImplTests tests`、`just test-boundary`。 |
| B2-RED-003 | 支出主体资金责任解析缺失、不唯一、币种不一致、优先级不确定，或预算组、Spend Rule 被当成最终责任主体时必须失败；若目标态支持信用账户或平台账户角色，`fundingAccountId` 兼容字段不足以表达目标主体时必须失败。 | `SpendSubjectFundingRelationServiceImplTests`、`PaymentInstrumentRouteDslContractTests`。 | `just test-one SpendSubjectFundingRelationServiceImplTests tests`、`just test-boundary`。 |
| B2-RED-003A | 预算组被当作 route leg、posting、LedgerEntry 主体，或预算组创建自动初始化 ledger bucket 时必须失败，除非 Execution Grant 明确声明这是兼容迁移验证。 | `ControlAccountLedgerInitializationTests`、`DefaultLedgerPostingAssemblerTests`、`WalletLayerBoundaryTests`。 | `just test-ledger`、`just test-boundary`。 |
| B2-RED-004 | 余额查询初始化账本、修复余额、把余额日志当事实源或跨周期余额当可用余额必须失败。 | `FundsSubjectBalanceQueryServiceImplTests`、`FundsBalanceAssertionSupportTests`。 | `just test-one FundsSubjectBalanceQueryServiceImplTests tests`、`just test-boundary`。 |
| B2-RED-005 | posting 不平衡、分录缺周期、投影失败回滚账本事实或日志失败回滚投影必须失败。 | `PostingLedgerDslContractTests`、`DefaultLedgerPostingAssemblerTests`、`LedgerBalanceProjectionServiceImplTests`。 | `just test-one LedgerBalanceProjectionServiceImplTests tests`、`just test-ledger`。 |
| B2-RED-006 | wallet 直接创建资金交易、route snapshot 或 ledger entry 必须失败。 | `WalletLayerBoundaryTests`、`RouteResolverFactBoundaryTests`。 | `just test-boundary`。 |

### 14.3 B2 Done / Not Done 口径

| 项 | Done | Not Done |
| --- | --- | --- |
| 测试顺序 | B2 最小 Red 已先失败，再做最小 Green。 | 只补实现或只跑既有通过测试。 |
| 资金断言 | 账户、账本、账目、周期、posting、entry、projection、幂等和失败无副作用均被断言。 | 只断状态、数量或“不报错”。 |
| 写入范围 | 只改 B2 授权范围。 | 触碰交易编排、权益消费、清结算、对账、归档、指标或未授权公共契约。 |
| 安全边界 | 支付工具敏感信息脱敏、绑定历史审计、wallet 边界和余额查询只读均有证明。 | 敏感字段、审计或只读边界无测试证据。 |
| 验证 | 目标 `test-one`、`just test-ledger`、`just test-boundary`、`just pmd` 和 `git diff --check` 通过，或明确环境限制。 | 验证缺失、失败未解释或被包装成通过。 |

```text
授权任务：B2 / 钱包账户、账本和余额投影基础
允许写入范围：wallet/wallet-face、wallet/wallet-impl、ledger/ledger-face、ledger/ledger-impl、tests/src/test/java；必要时只读 transaction-*、core 和 tests/src/test/resources/jdbc-schema.sql 用于确认既有契约与表结构
禁止写入范围：transaction-* 业务实现；直接交易、授权交易、余额控制交易编排；Route Resolver、Posting Assembler、Route Replay；权益 route/posting/replay 消费；清结算、对账、归档、指标实现；生产配置；外部通道适配
必须覆盖的 TDD 用例：TDD-WALLET-*、TDD-WALLET-018、TDD-WALLET-019、TDD-ROUTE-011、TDD-ROUTE-012、TDD-LEDGER-*、TDD-VIEW-003、B2-RED-003A
必须覆盖的 AC/DSL/GAP ID：AC-PI-001、AC-PI-002、AC-PI-003、AC-PI-004、AC-PI-005、AC-PI-006、AC-PI-007、AC-PI-008、AC-PI-009、AC-PI-010、AC-CTRL-009、AC-CTRL-010、AC-CTRL-011、AC-BALLOG-001、RED-036、RED-046、RED-047、RED-049、RED-067、GAP-WALLET-001、GAP-WALLET-001A、GAP-WALLET-002、GAP-WALLET-003
基线是否已冻结：已冻结；以确认时 Git HEAD、OpenSpec 和 Harness 最新任务账本为准，270122e、81a7ecb、4a7ef12、f99800b、9456ab6 和 77bc9f4 只作为历史准入与局部保护证据；本任务启动前必须复核工作树状态
工作树状态：执行前必须复核；dirty 时未列入允许纳入范围的变更不得作为 Done 证据
允许修改公共契约：待用户确认；建议默认不删除、不改写既有 face/core 字段，只允许为账户、账本、支付工具基础能力做非破坏性新增或校验补齐
公共契约允许修改范围：如确需变更，只限 wallet/ledger face 中账户角色、账本创建、账期、支付工具绑定和资金账户关系的目标态字段；不得调整交易指令、权益快照、直接交易或授权交易请求语义
允许新增枚举或事件：否；如账户角色、账本周期或支付工具状态缺口必须新增，需重新确认
允许新增服务入口：否；优先复用既有 wallet/ledger 服务入口完成用例闭环
允许扩展 Request/Query/DTO：待用户确认；建议默认不扩展交易 Request/DTO，wallet/ledger 基础对象如确需扩展必须保持可选、语义明确并覆盖测试
允许修改表结构：否；如 H2 测试表与生产模型缺口阻断本任务验收，先停下补充表结构授权
允许新增模块：否
是否影响架构 ADR：否；如改变 core、wallet、ledger 依赖方向或事实归属，必须重新确认
受影响 ADR：ADR-002 core 作为资金语义内核；账本事实边界和 wallet 产品门面边界
是否触碰能力域边界：是，仅限 wallet 账户/支付工具门面与 ledger 账本事实/投影边界
是否触碰事实端口层：是，仅限 ledger 账本事实、账期和余额投影；不得反向持有业务交易生命周期状态
架构边界测试范围：`just test-ledger`、`just test-boundary`，以及与本任务修改对象对应的 `just test-one <TestClass> tests`
人工确认点：账户角色命名与创建规则、显式账本创建边界、账期与分录平衡断言、余额投影事务边界、支付工具绑定历史审计、funding relation 与账户主体关系、是否需要表结构授权
NFR 假设：本任务只做本地服务层与 H2 验证，不处理生产并发容量、外部通道回调、清结算批次或权益回放告警
观测告警：本任务不新增生产告警；后续交易主链路和权益消费任务再补缺失快照、余额投影滞后、摘要冲突和回放失败告警
回滚或补偿：本任务如修改公共契约必须同步目标态说明；如新增测试表字段，必须同步说明生产迁移前置条件
基础验证命令：just mvn-version、just compile
专项验证命令：just test-ledger；just test-boundary；必要时 just test-one <相关测试类> tests；提交前 just pmd
交付方式：先补失败用例再实现，逐项声明覆盖的 TDD/AC/RED；完成后按 CAD 自动提交；未确认本授权前不得进入 B2 编码
```
