# 支付资金底座 DSL 契约验收矩阵

## 文档信息

| 项目 | 内容 |
| --- | --- |
| 文档名称 | 支付资金底座 DSL 契约验收矩阵 |
| 日期 | 2026-05-13 |
| 设计口径 | 支付产品与资金系统专家、资深架构师 |
| 上游输入 | `v5 支付底座完整产品 PRD.md`、`v5 DSL 规范设计.md`、产品用例、产品层 TDD、核心概念审查、`系分设计/API 契约测试与编码实施计划.md` |
| 文档定位 | 面向研发、测试和评审的 DSL 契约验收清单，用于验证目标态 DSL 是否正确表达资金事实、路由、账务计划、分录、余额和回放边界。 |

## 一、验收口径

本矩阵不讨论历史兼容，不保留旧命名包袱。验收只看目标态 DSL 是否满足支付资金底座的核心目标：

```text
FundsInstruction
  -> Route / RouteSnapshot
  -> PostingPlan
  -> LedgerEntry
  -> BalanceProjection
```

验收分级：

| 分级 | 含义 | 处理 |
| --- | --- | --- |
| 必须通过 | 不满足会导致资损、账不平、回放错误或架构边界污染。 | 代码实现前必须有契约测试。 |
| 应该通过 | 不满足会降低可审计、可维护或可扩展能力。 | 本轮设计和系分阶段补齐。 |
| 不纳入 DSL | 属于产品单据、运营流程、通道状态、交易视图或报表。 | 不进入 DSL 枚举、RouteLeg 或 LedgerPhase。 |

## 二、DSL 边界验收

| 验收项 | 等级 | 预期 | 失败表现 |
| --- | --- | --- | --- |
| 业务单不等于资金指令 | 必须通过 | 订单、出款单、清算单、结算单、对账单、争议单只作为业务身份、引用或未来来源事实引用，不成为 DSL 主对象。 | 把订单状态、审批状态、通道状态塞进 `FundsInstruction`、`RouteLegType` 或 `LedgerPhaseCode`。 |
| 工具不是账户 | 必须通过 | VCC、共享卡、VA、银行卡、外部账户只作为引用和快照。 | 工具或外部账户进入 `LedgerEntry.subjectType`。 |
| 可入账主体受控 | 必须通过 | `LedgerEntry` 只允许资金账户、信用账户、预算组等可入账主体。 | 用户、商户经营主体、平台角色、外部账户直接入账。 |
| 冻结不是资金交易 | 必须通过 | 冻结/解冻使用 `FrozenOrder` 来源事实，生成账本交易和分录，不创建 `FundsTransaction`。 | 冻结写入资金交易主表或交易明细。 |
| 交易视图不是事实 | 必须通过 | 用户账单、商户账单、运营时间线、财务报表只读投影，不反写账务事实。 | 用交易视图或报表修正余额。 |
| 清结算流程不污染 DSL | 必须通过 | 清算批次、结算审核、出款处理中、回单核验属于产品对象。 | 增加 `RECONCILING`、`PAYOUT_PROCESSING` 等 phase 或 leg。 |

## 三、Instruction 契约验收

| 验收项 | 等级 | 预期 | 测试建议 |
| --- | --- | --- | --- |
| 业务身份和引用明确 | 必须通过 | 指令必须能追溯 `businessScene/businessSn`，后续事件必须通过 `reference` 定位原事实。 | 构造资金交易、冻结、调账、退款、拒付指令，断言业务身份和 reference 边界。 |
| 业务标识明确 | 必须通过 | `tenantId`、`businessScene`、`businessSn` 在进入 route 前必定可用。 | 缺任一字段应失败。 |
| 金额口径明确 | 必须通过 | `amount` 是账务主金额，`originalAmount/exchangeRate` 是原始币种和汇率快照。 | 错币种交易必须保存两套金额。 |
| 指令类型准确 | 必须通过 | 直接交易、授权交易、余额控制分开。 | 冻结不得使用直接交易；授权结算不得使用普通支付事件。 |
| 引用边界明确 | 必须通过 | 退款、撤销、结算、拒付、费用退回、解冻必须引用原事实。 | 缺引用或引用无法定位原快照时失败。 |
| 幂等摘要稳定 | 必须通过 | 请求摘要不包含数据库 ID、持久化流水、审计时间或易变展示字段。 | 同一事实重建后摘要一致。 |
| 操作者快照独立 | 应该通过 | 操作者使用 DSL 内部稳定 actor，不依赖业务域 operator 类型。 | 公共契约耦合业务域类。 |
| 交易层服务能力有 JSON 验证结构 | 必须通过 | 直接交易、逆向交易、授权交易、余额控制、查询与重放都必须有 JSON 样例，样例能覆盖 instruction、route、posting、validation 或只读边界。 | 只有文字矩阵，没有可序列化结构，无法转契约测试。 |

## 四、Route 与 Snapshot 契约验收

| 验收项 | 等级 | 预期 | 测试建议 |
| --- | --- | --- | --- |
| Route 只表达资金路径 | 必须通过 | `RouteLeg` 只描述主体、账目、金额、阶段、约束和 replay 策略。 | 不允许通道处理、审批、证据提交成为 leg。 |
| Snapshot 结构有版本 | 必须通过 | `snapshotSchemaVersion` 与 `routeVersion` 同时存在且含义分离。 | 未知 schema version replay 失败。 |
| Snapshot 保留平台账户 | 必须通过 | 使用平台现金、预收、费用、挂账、准备金时，必须保存具体平台账户快照。 | replay 时读取当前默认平台账户。 |
| Snapshot 保留工具与外部端点 | 必须通过 | 工具和外部账户只作为快照，不入账。 | replay 后工具、外部流水丢失。 |
| Replay 使用原路径 | 必须通过 | 后续事件基于原 snapshot，不重新 route。 | 原绑定关系变化后 replay 使用新主体。 |
| RouteParticipant 保留实际影响主体 | 应该通过 | replay 部分金额时只保留本次实际影响主体。 | 部分退款携带无关主体导致审计混乱。 |
| RouteCode 语义稳定 | 应该通过 | routeCode 表达资金路径，不表达页面流程。 | 出现 `WITHDRAW_APPLY`、`DISPUTE_CREATED` 等流程名。 |

## 五、Posting 与 Ledger 契约验收

| 验收项 | 等级 | 预期 | 测试建议 |
| --- | --- | --- | --- |
| 每组计划独立平衡 | 必须通过 | 每个 `PostingPlan` 同币种借贷相等。 | 任意 route leg 生成不平衡 plan 应失败。 |
| 整笔账本交易平衡 | 必须通过 | `LedgerTransaction` 下所有 plan 汇总平衡。 | 多 plan 汇总借贷不一致失败。 |
| 缺账本失败 | 必须通过 | 入账路径不自动创建 ledger。 | 主体未初始化账本时失败。 |
| 借贷方向系统推导 | 必须通过 | 业务方不传 `DEBIT/CREDIT`，由 profile normal balance 和 route 推导。 | 业务请求直接指定借贷方向。 |
| Entry 摘要稳定 | 必须通过 | 摘要不包含 entry sn、数据库 ID、ledger transaction sn、plan sn、审计时间。 | 同一事实重建后 entry 摘要变化。 |
| Entry 不暴露展示语义 | 必须通过 | Entry 不作为用户账单行或商户账单行。 | Entry 出现账单标题、展示原因、国际化文案。 |
| Phase 不表达业务状态 | 必须通过 | phase 只表达资金动作阶段。 | 加入审核中、处理中、举证中等状态。 |
| Intent 表达账务原因 | 应该通过 | intent 表达为什么记账，phase 表达资金阶段，二者不混用。 | intent 和 phase 同义重复或使用页面动作。 |

## 六、余额桶与 Profile 契约验收

| 验收项 | 等级 | 预期 | 测试建议 |
| --- | --- | --- | --- |
| Profile 决定账目规则 | 必须通过 | 账目 normal balance、可用账目、负数策略由 profile 决定。 | 业务代码硬编码 normal balance。 |
| `AVAILABLE` 可受控为负 | 必须通过 | 资金账户、信用账户和预算组的 `AVAILABLE` 可在明确策略下为负，并记录来源、上限、账龄、主体类型和治理路径。 | 负余额静默产生或被当作可继续消费、授权或出款余额。 |
| `CONSUMED` 不入账 | 必须通过 | 信用和预算已消费进入报表口径，不新增账务 `CONSUMED`。 | 创建 `CONSUMED` ledger account 或 route leg。 |
| `SETTLEMENT` 独立存在 | 必须通过 | 商户出款中锁定使用 `SETTLEMENT`，不能被 `AVAILABLE` 替代。 | 出款中资金仍可重复出款或自动退款。 |
| 商户订单款先进 `CLEARING` | 必须通过 | 商户订单收款默认用户 `AVAILABLE -> 商户 CLEARING`。 | 订单款直入商户 `AVAILABLE` 或 `SETTLEMENT`。 |
| 冻结只迁移同主体余额 | 必须通过 | 冻结是同一主体 `AVAILABLE -> FROZEN`。 | 冻结改变资金归属或跨主体转移。 |

## 七、Replay 契约验收

| 场景 | 等级 | 预期 | 失败红线 |
| --- | --- | --- | --- |
| 原交易退款 | 必须通过 | 基于原 snapshot 或当前持仓桶规则生成退款路径，累计退款不超额。 | 缺快照重新选路。 |
| 授权撤销 | 必须通过 | 原主体 `AUTHORIZATION -> AVAILABLE`。 | 读取当前绑定关系释放到新主体。 |
| 授权结算 | 必须通过 | 结算金额不超过剩余授权，控制主体和真实资金主体分别按规则关闭占用。 | 部分主体成功、部分失败。 |
| 授权链退款 | 必须通过 | 基于原授权/结算路径回补。 | 把授权拒绝当退款。 |
| 争议拒付 | 必须通过 | 使用 `CHARGEBACK`，共同上限为 `refundedAmount + chargebackAmount <= settledAmount`。 | 与授权拒绝混用或累计到错误字段。 |
| 手续费退回 | 必须通过 | 只回放费用 leg，普通退款不默认退手续费。 | 普通退款自动退所有 fee。 |
| 解冻 | 必须通过 | 引用冻结单，解冻金额不超过剩余冻结金额。 | 超额解冻或未引用冻结单。 |
| `REPLAY_ONCE` | 必须通过 | 成功回放一次后不能再次消费同一 leg。 | 重复回放导致重复退款或重复释放。 |

## 八、场景到账务契约矩阵

### 8.0 交易层服务能力 JSON 样例验收

| 服务能力 | 必备 JSON 样例 | 必须验证 | 失败红线 |
| --- | --- | --- | --- |
| 直接交易服务 | 平台内部付款、手续费收取、商户收款、系统内转账。 | `instructionType=DIRECT_TRANSACTION`；平台角色解析成具体 `FundingAccount`；本金和费用拆分；posting plan 平衡。 | 平台角色未固化、费用混入本金、业务侧直接传 entry。 |
| 逆向交易服务 | 直接交易退款、手续费退回、外部退回、退汇或冲正。 | 必须引用原事实和原 route snapshot；只回放相关 leg；累计金额不超上限。 | 缺快照重新选路、普通退款默认退手续费、超额退款。 |
| 授权交易服务 | 授权批准、授权拒绝、授权结算、授权撤销、授权链退款、争议拒付。 | 授权批准整体成功或整体失败；授权拒绝无 route/entry；授权结算和退款基于原快照。 | 授权拒绝写 `CHARGEBACK`；信用或预算写 `CONSUMED`；结算超过剩余授权。 |
| 余额控制服务 | 冻结、解冻、资金调账、信用额度调整、预算额度调整。 | 冻结来源为 `FrozenOrder`；冻结不创建 `FundsTransaction`；调额不是现金流；信用/预算调额通过 `LIMIT_ADJUST` 受控表达；受控负余额有策略；不承接 FX，金额必须是账户或账本币种。 | 冻结跨主体转移；无审批调账；普通交易把 `LIMIT` 当 source/target；预算调减被表达成入金；余额控制自动换汇。 |
| 查询与重放服务 | 交易视图有界重放、余额重建。 | 交易视图重放只写只读投影；余额重建只读账本分录、检查点、水位和归档清单；请求必须有范围。 | 无范围全量重放；从报表反推余额；视图重放生成 route 或 entry。 |

JSON 样例必须通过以下机械校验：

1. 所有 `json` 代码块可被标准 JSON parser 解析。
2. 每个 `expectedPosting.postingPlans[*]` 同币种借贷平衡。
3. `sourceNode/targetNode.subjectType` 只能是 `FUNDING_ACCOUNT`、`CREDIT_ACCOUNT`、`BUDGET_GROUP`。
4. 平台账户角色只能出现在 `platformAccounts` 或上下文快照中，入账时必须是具体 `FundingAccount`。
5. `QUERY_AND_REPLAY` 样例不得包含 `expectedRoute` 或 `expectedPosting`。

### 8.0.1 契约样例文件清单

以下文件名作为后续测试资源建议命名，最终路径按模块测试目录约定确认。当前已先在 `core/src/test/resources/dsl/transaction-layer/` 落地交易层 P0 关键样例，并由 `TransactionServiceAbilityDslJsonContractTests` 校验可解析、枚举合法、服务能力覆盖、posting plan 平衡和无入账场景边界。

| 样例文件 | 服务能力 | 覆盖场景 | 必须断言 |
| --- | --- | --- | --- |
| `direct-platform-transfer.json` | 直接交易 | 平台内部付款或转账。 | route snapshot 固化，posting plan 平衡。 |
| `direct-wallet-payment-with-fee.json` | 直接交易 | 钱包支付并收取手续费。 | 本金和手续费分账，费用归集账户入账。 |
| `reverse-refund-original-route.json` | 逆向交易 | 退款、退回或冲正。 | 引用原 route snapshot，不重新选路，累计金额不超上限。 |
| `authorization-approve-multi-subject.json` | 授权交易 | 资金账户、信用账户、预算组组合授权。 | 多主体整体成功或整体失败。 |
| `authorization-decline-no-posting.json` | 授权交易 | 授权拒绝。 | 无 route、无 entry，不累计 `chargebackAmount`。 |
| `authorization-settlement-partial-release.json` | 授权交易 | 授权部分结算并释放差额。 | 结算金额不超过剩余授权，差额处理可解释。 |
| `authorization-chargeback-dispute.json` | 授权交易 | 争议拒付或强制扣回。 | 争议拒付独立事实，和授权拒绝分离。 |
| `balance-freeze-order.json` | 余额控制 | 风控或运营冻结。 | 来源事实为 `FrozenOrder`，`AVAILABLE -> FROZEN`。 |
| `balance-unfreeze-replay.json` | 余额控制 | 解冻。 | 引用冻结单，解冻金额不超过剩余冻结。 |
| `clearing-settlement-payout.json` | 清结算 | 清算确认、结算锁定、出款成功。 | `CLEARING -> AVAILABLE -> SETTLEMENT` 链路清晰。 |
| `reconciliation-exception-adjustment.json` | 对账差错 | 差错阻断、调账、核销。 | 有差错来源、审批和审计引用。 |
| `archive-balance-rebuild-watermark.json` | 余额重建 | 检查点、水位和热分录拼接。 | 无 gap、无 overlap，不使用交易视图反推余额。 |
| `transaction-view-replay-range.json` | 交易视图重放 | 用户账单、商户账单或运营时间线修复。 | 必须限定范围，只写读模型和差异报告。 |

### 8.1 入金、出金和转账

| 场景 | DSL 映射 | 账务结果 | 验收 |
| --- | --- | --- | --- |
| 用户入金成功 | `DIRECT_TRANSACTION / FUND_IN / FUND_IN_STANDARD` | 平台现金或预收路径可解释，用户 `AVAILABLE` 增加。 | 外部账户不生成 entry；重复通知幂等。 |
| 用户提现申请 | `BALANCE_CONTROL / FREEZE / BALANCE_FREEZE_STANDARD` | 用户 `AVAILABLE -> FROZEN`。 | 生成冻结单，不创建资金交易。 |
| 用户提现成功 | `DIRECT_TRANSACTION / FUND_OUT / FUND_OUT_STANDARD` | 消耗用户 `FROZEN`，平台外部出金路径可解释。 | 成功通知不重复扣。 |
| 用户提现失败 | `BALANCE_CONTROL / UNFREEZE` | 用户 `FROZEN -> AVAILABLE`。 | 只释放一次。 |
| 系统内转账 | `DIRECT_TRANSACTION / TRANSFER / INTERNAL_TRANSFER_STANDARD` | 付款方 `AVAILABLE` 减少，收款方 `AVAILABLE` 增加。 | 同主体、币种不一致失败。 |

### 8.2 商户订单收款、清算和结算

| 场景 | DSL 映射 | 账务结果 | 验收 |
| --- | --- | --- | --- |
| 商户订单收款 | `DIRECT_TRANSACTION / PAY / MERCHANT_ORDER_COLLECTION_STANDARD` | 用户 `AVAILABLE -> 商户 CLEARING`。 | 不允许业务方指定商户 `AVAILABLE/SETTLEMENT`。 |
| 商户清算确认 | `DIRECT_TRANSACTION / SETTLEMENT / MERCHANT_CLEARING_COMPLETE` | 商户 `CLEARING -> AVAILABLE`。 | 清算批次重跑不重复入账。 |
| 商户结算锁定 | `DIRECT_TRANSACTION / SETTLEMENT / MERCHANT_SETTLEMENT_LOCK` | 商户 `AVAILABLE -> SETTLEMENT`。 | 出款中不可再次结算。 |
| 商户出款成功 | `DIRECT_TRANSACTION / FUND_OUT / MERCHANT_PAYOUT_SUCCESS` | 消耗商户 `SETTLEMENT` 并完成平台外部出金路径。 | 回单可核对。 |
| 商户出款失败 | `DIRECT_TRANSACTION / SETTLEMENT / MERCHANT_PAYOUT_FAIL_RESTORE` | 商户 `SETTLEMENT -> AVAILABLE`。 | 失败原因、通道引用和处理人可审计；不得混用退款或解冻事件。 |

### 8.3 授权交易

| 场景 | DSL 映射 | 账务结果 | 验收 |
| --- | --- | --- | --- |
| 授权批准 | `AUTHORIZATION_TRANSACTION / AUTHORIZE / AUTHORIZATION_STANDARD` | 一个或多个主体 `AVAILABLE -> AUTHORIZATION`。 | 多主体整体成功或整体失败。 |
| 授权拒绝 | 授权结果事实，不进入 posting | 无 route、无 entry。 | 不影响 `chargebackAmount`。 |
| 授权撤销 | `AUTHORIZATION_TRANSACTION / AUTH_REVERSAL / AUTHORIZATION_REVERSAL_REPLAY` | `AUTHORIZATION -> AVAILABLE`。 | 基于原 snapshot。 |
| 授权结算 | `AUTHORIZATION_TRANSACTION / AUTH_SETTLEMENT / AUTHORIZATION_SETTLEMENT_REPLAY` | 授权占用关闭或减少，真实资金进入结算路径。 | 结算不超过剩余授权。 |
| 授权退款 | `AUTHORIZATION_TRANSACTION / AUTH_REFUND / AUTHORIZATION_REFUND_REPLAY` | 按原结算路径回补。 | 累计退款和拒付不超过已结算。 |
| 争议拒付 | `AUTHORIZATION_TRANSACTION / CHARGEBACK / CHARGEBACK_REPLAY` | 争议资金结果入账。 | 与授权拒绝严格分离。 |

### 8.4 冻结、调账和控制账户

| 场景 | DSL 映射 | 账务结果 | 验收 |
| --- | --- | --- | --- |
| 风控冻结 | `BALANCE_CONTROL / FREEZE / BALANCE_FREEZE_STANDARD` | 主体 `AVAILABLE -> FROZEN`。 | 冻结单有原因、期限、操作者和审批引用。 |
| 解冻 | `BALANCE_CONTROL / UNFREEZE / BALANCE_UNFREEZE_REPLAY` | 主体 `FROZEN -> AVAILABLE`。 | 不超过剩余冻结金额。 |
| 资金余额调账 | `BALANCE_CONTROL / BALANCE_ADJUST / FUNDING_BALANCE_ADJUST_STANDARD` | 平衡调账分录。 | 必须有差错、凭证、审批和财务复核。 |
| 信用额度调整 | `BALANCE_CONTROL / LIMIT_ADJUST / LIMIT_ADJUST_STANDARD` | `LIMIT` 和 `AVAILABLE` 按规则调整；`LIMIT` 不开放给普通交易迁移。 | 不作为现金流。 |
| 预算额度调整 | `BALANCE_CONTROL / LIMIT_ADJUST / LIMIT_ADJUST_STANDARD` | 预算 `LIMIT` 和 `AVAILABLE` 调整；调减受控负数必须有预算周期、审批、上限、账龄和治理路径。 | 不新增 `CONSUMED`。 |

## 九、不纳入 DSL 的产品对象

| 对象 | 不纳入原因 | DSL 只提供 |
| --- | --- | --- |
| 清算批次 | 运营批处理对象。 | 清算确认后的 `CLEARING -> AVAILABLE` 入账事实。 |
| 结算单 | 出款申请、审批和金额组成对象。 | 结算锁定和出款结果的入账事实。 |
| 出款单 | 外部通道处理和回单对象。 | 成功、失败或退回的资金结果。 |
| 对账批次 | 匹配和差错发现对象。 | 差错核销调账事实。 |
| 争议单 | 证据、原因码、责任和时限对象。 | 拒付、争议费用、追偿等资金结果。 |
| 交易视图 | 展示投影。 | 来源事实、route、entry 作为只读输入。 |
| 报表 | 汇总投影。 | 账务事实、批次事实和规则版本作为只读输入。 |
| 余额归档任务 | 运维治理对象。 | 水位、检查点、归档清单和分录事实。 |

## 十、归档、余额重建和视图重放验收

| 验收项 | 等级 | 预期 | 失败红线 |
| --- | --- | --- | --- |
| 余额冷热边界 | 必须通过 | 使用 `BalanceProjectionWatermark`，不是 180 天或自然日。 | 出现 gap 或 overlap。 |
| 水位推进顺序 | 必须通过 | 先计算、写入、校验，再推进水位。 | 先更新水位导致漏算。 |
| 归档预检查 | 必须通过 | cutoff 不晚于热保留边界且不晚于 watermark。 | 归档未汇总区间。 |
| ArchiveManifest | 必须通过 | 记录范围、数量、借贷汇总、游标、摘要、检查点和审批。 | 冷归档后无法核对。 |
| 余额重建 | 必须通过 | 水位前检查点或冷汇总 + 水位后增量分录。 | 从交易视图或报表反推余额。 |
| 交易视图重放 | 必须通过 | 指定租户、视图域、时间窗口、主体、批次或单笔来源。 | 无范围全量在线重放。 |

## 十一、结构测试清单

| 测试类 | 测试重点 |
| --- | --- |
| `FundsInstructionSpecContractTests` | 业务标识、金额、引用、操作者和上下文字段。 |
| `TransactionServiceAbilityDslJsonContractTests` | 交易层服务能力 JSON 样例可解析、字段完整、场景覆盖完整。 |
| `FundsTransactionServiceApiContractTests` | 交易层 Request、Query、DTO、错误码、幂等键和审计字段。 |
| `RouteDslContractTests` | route、snapshot、platform accounts、tool/external refs、replay policy。 |
| `LedgerPostingPlanContractTests` | 每组 plan 平衡、币种一致、entry 绑定 ledger。 |
| `LedgerEntryDigestContractTests` | 摘要稳定字段和排除字段。 |
| `RouteReplayContractTests` | 缺快照失败、未知 schema 失败、binding 漂移不影响 replay。 |
| `BalanceProjectionArchiveContractTests` | watermark、checkpoint、archive manifest、冷热拼接。 |
| `SettlementPolicySpecTests` | 结算策略表达式解析和边界日期。 |

测试方法中文注释必须写在方法上方，说明场景、输入、输出和预期。

## 十二、本轮结论

目标态 DSL 可以支撑支付资金底座的核心账务能力：资金事实接入、路由快照、账务计划、不可变分录、余额投影、后续事件 replay、结算策略和归档后的余额重建。

本矩阵后续作为代码实现和测试设计的验收基线。凡是不属于资金事实、路由、账务计划、分录、余额投影或回放治理的对象，都不应进入 DSL。
