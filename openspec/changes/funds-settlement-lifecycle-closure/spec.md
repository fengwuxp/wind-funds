# 清结算分佣资金生命周期闭环 Goal 执行规格

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `funds-settlement-lifecycle-closure` |
| Goal key | `WF-CLR-FND-EXEC-001` |
| Runtime Goal thread | `019fc59d-b771-75b0-b5bc-6f9e366e30d8` |
| Goal state | `ACTIVE` |
| Current stage | `CORE-2D_FIX_001_CLOSED` |
| 架构类型 | 系统架构 |
| Baseline HEAD | `ac9d1565a124` |
| Git strategy | `summary_only`；未授权 stage、commit、push 或 PR |

## 1. Goal Card / 背景、目标与非目标

### Objective

基于 VCC、全球账户、收单、分佣分账和优惠让利五类业务对 `core` 的统一裁决，结合 Fincone 清算、结算、分佣一手设计，闭环以下共享资金底座问题：

1. 分佣在清算前、清算后、结算锁定后和出款后的逆向与追偿路径。
2. 结算收款主体的稳定 Profile 语义。
3. Fincone 经营清结算与 wind-funds 资金执行之间的最小 handoff 边界。

不新增分佣内核、通用 Group DSL 或第二套账本；佣金计算、归因、营销规则和宿主生命周期继续由 Fincone 持有。

### Success Criteria

| ID | 完成条件 | 证据 |
| --- | --- | --- |
| `SC-SLC-001` | Fincone 经营事实与 wind-funds 资金事实的 Owner、状态和完成语义无重叠。 | 决策表、源码与 Fincone 设计锚点、独立 Checker。 |
| `SC-SLC-002` | H2 证明清算后只追加带原资金引用的扣减/调账；原 route snapshot、detail、posting 和 entry 不覆盖，原交易只允许累计逆向摘要按既有契约前滚。 | `AgentCommissionSettlementBusinessFlowTests` 与 adjust 相关测试。 |
| `SC-SLC-002A` | 原分配逆向能从 Fincone handoff 唯一回链到资金交易；业务 allocation 字段不进入 core。 | Fincone 映射契约与现有 direct refund `referenceTransactionSn` H2；不由本仓伪造 Fincone 实现。 |
| `SC-SLC-003` | `SETTLEMENT` 锁定后、出款成功前的逆向有明确 fail-closed 行为；若缺公共动作，以现状证据进入 Owner Gate。 | H2 失败路径、无新增资金/账务事实断言。 |
| `SC-SLC-004` | `gateRef` 不作为授权令牌，最终清算、结算、出款在本地事务内重查权威 Gate。 | Gate 契约、实现锚点和既有 H2 Gate 测试。 |
| `SC-SLC-005` | 分佣、返利、分润不复用 `FundsBenefitContributionTransactionService`。 | 公共契约、实现拒绝和边界测试。 |
| `SC-SLC-006` | 结算收款 Profile 完成 Owner 裁决；未裁决前不修改 public enum 或账户映射。 | Profile 决策记录、API baseline 与迁移矩阵。 |
| `SC-SLC-007` | Wave 收口时编译、相关 H2、PMD 和独立 Checker 无未归属 P0/P1。 | 新鲜命令报告与 Checker 结论。 |

### Non-Goals

- 不在 wind-funds 实现邀请关系、资格、KPI、佣金公式、阶梯、封顶、税务、薪酬、受益人或法域规则。
- 不把 `PAYROLL`、`ACCOUNTS_PAYABLE` 作为 wind-funds canonical action；它们直接进入薪酬/AP 主责系统，不强制转换为资金目标。
- 不新增 `CommissionPlan`、`Entitlement`、`allocationGroup`、`Benefit` 或通用激励 DSL。
- 不把 Fincone `REVIEW_READY / OwnerDecision=PENDING / E1 / NOT_STARTED` 的设计写成已实施或生产可用。
- 不连接独立 MySQL；公共基础设施继续使用 Spring/H2 和静态契约验证。
- 不执行 Git、联网、安装、发布、部署、生产或不可逆操作。

## 2. Role Loop Verdicts

| Role Loop | 状态 | 结论 |
| --- | --- | --- |
| Product Loop | `VERIFIED_READ_ONLY` | `ClearingItem` 是唯一经营层金额应得事实；Calculated 无资金副作用，Confirmed 后才可形成 `SettlementItem`。主付款与分佣不做组原子。 |
| Payment Loop | `VERIFIED_READ_ONLY` | 标准资金承接只接受已解析的 `FUNDS_ACCOUNT` 或 `EXTERNAL_PAYOUT`；分阶段逆向，Unknown 阻断后序，外部 submitted/accepted 不等于完成。 |
| Architecture Loop | `VERIFIED_READ_ONLY` | 现有 pay、clearing、settlement、Gate、adjust、payout、recovery 原语可复用；首缺口是组合 H2 证据和锁定后撤销，不是新 core 模型。 |
| Checker Loop | `PASS_FINAL` | 最终 `P0=0 / P1=0 / P2=0`；确认 Wave 1 可关闭，但不代表整个 Goal、Fincone 实施或生产准出完成。 |
| CORE-2B Checker | `CONDITIONAL_PASS` | additive Profile 包 `P0=0 / P1=0 / P2=1`；直接 rename `NO-GO`。当时提出的命令级 capability admission 已由后续 `CORE-2C` 实现并关闭，不再是当前 P1。 |
| CORE-2C Checker | `PASS_FINAL` | `P0=0 / P1=0 / P2=0`；动作矩阵、重放短路、空 legs、逆向/控制豁免和 payout capability drift 均有 H2 证据。 |
| CORE-2D Checker | `PASS_FINAL` | `CORE-2D-FIX-001` 生产源码只修改既有结算服务实现，测试只增强既有 release 幂等用例；当前 CORE-2D 8 类 58 项、DSL 共存 64 项、compile、PMD 与 `verify-cad` 均通过，独立 Checker 最终 `P0=0 / P1=0 / P2=0`。 |
| Fincone Round 2 Checker | `REOPENED_BUSINESS_PREMISE` | `CLR-GATE-003` 原推荐包没有绑定真实 `ClearingItem/ClearingCandidate/ClearingBatch`；该前提已撤回，先由宿主与 Product/Finance Owner 冻结资金政策。 |
| Fincone Round 3 Payment Loop | `NO_GO_PRODUCTION` | 分段资金原语可复用；初始 Gate、锁定释放的稳定新鲜证据和分层关账证据未关闭，不能据此准出 Fincone。 |
| Fincone Round 3 Architecture Loop | `HOLD_OWNER_GATES` | 当前 1:1 不可变证据链可用；watermark/coverage、聚合匹配、容差、运营工单和 release closure evidence 仍由 Owner 裁决。 |
| Fincone Round 3 Checker | `PASS_DOCUMENT_SCOPE` | 修正事务与关闭证据表述后，文档事实差异 `P0=0 / P1=0 / P2=0`；能力 P1 和 Owner Gate 继续保留。 |

## 3. Decision Register / 核心决策与职责边界

| ID | 决策 | 状态 | Owner Gate |
| --- | --- | --- | --- |
| `D-SLC-001` | Fincone 对象链 `CommissionPlanVersion -> FactSnapshot -> Attribution -> Eligibility -> RuleVersion -> ClearingItem -> SettlementItem -> handoff -> Reconciliation -> Adjustment` 是经营域链路，不进入 funds core。 | `CONFIRMED` | Fincone 仍需关闭自身 `CLR-GATE-*`，不能据此宣布已实现。 |
| `D-SLC-002` | `EligibilityDecision` 只表达资格；确定金额、币种后由 `ClearingItem` 形成应得事实。 | `CONFIRMED` | 不新增 Entitlement 或收益账本。 |
| `D-SLC-003` | wind-funds 只承接 `FUNDS_ACCOUNT` 和 `EXTERNAL_PAYOUT`；Payroll/AP 由上游持有并先完成目标解析。 | `CONFIRMED` | Fincone/HR/财务 Owner 负责映射、审批和合规。 |
| `D-SLC-004` | `allocationGroupRef`、`allocationRole`、`basisAmount`、`commissionPoolAmount`、`creditShare` 全部属于 Fincone 的计算、守恒和多受益人编排事实；`basisAmount` 是经济基数，`commissionPoolAmount` 是待分总额，`sum(ClearingItem.amount)=commissionPoolAmount`。 | `CONFIRMED` | `RETAINED/RESIDUAL` 必须是带单一 `beneficiaryRef` 的显式 ClearingItem role；精度、舍入和尾差接收方由规则版本固定。funds 只消费最终 Money 与资金执行引用。 |
| `D-SLC-005` | `gateRef` 只是证据引用；清算确认、结算锁定、结算释放和出款提交在各自事务内调用 `checkGate`。普通 direct pay、adjust、Recovery 登记自身不消费 Gate。 | `CONFIRMED_EXISTING_FOUR_CONSUMERS` | Fincone 先查 Gate、再跨仓调用普通 pay 不是原子准入；初始 `pay -> CLEARING` 是否必须消费 fresh Gate 仍待业务前提裁决，不得由包装器预设。 |
| `D-SLC-006` | commission、rebate、revenue share 不映射为 Benefit。 | `CONFIRMED` | 保持 `FundsBenefitContributionTransactionService` 的拒绝边界。 |
| `D-SLC-007` | 主付款与分佣不做组原子；多受益人状态由 Fincone `SettlementBatch + FundsHandoffRecord` 聚合。 | `CONFIRMED` | 部分退款范围未闭合时 fail-closed/manual；不新增 core Group。 |
| `D-SLC-008` | 保留 `FUNDING_MERCHANT` 的收单商户语义，以 additive 方式新增显式 `FUNDING_SETTLEMENT_PAYEE`：`CLEARING(false) / AVAILABLE(true) / FROZEN(false) / SETTLEMENT(false)` 四 bucket，能力仅声明 `RECEIVE + WITHDRAW`。`AVAILABLE=true` 只开放受控负余额的静态闸门，不表示默认透支。 | `RECOMMENDED_AWAITING_APPROVAL` | `CORE-2B-A` 待用户批准；不自动映射 `ACCOUNT_PAYABLE`，不改 converter、历史行或 Fincone。外部消费者与历史值清单未知只阻止未来 rename/remove/migration。 |
| `D-SLC-009` | 逆向按资金阶段处理；原 route snapshot、detail、posting、entry 和批次历史不覆盖，原资金交易的累计逆向摘要可按既有契约前滚。 | `CONFIRMED_WITH_P1` | `SETTLEMENT` 锁定后、出款前缺稳定公开撤销/调整动作。 |
| `D-SLC-010` | Fincone 必须在每笔 `FundsHandoffRecord` 保存 `factSnapshotRef + allocationGroupRef + clearingItemRef + settlementItemRef + canonicalAction + fundsTransactionSn` 映射；逆向只把返回的 `fundsTransactionSn` 作为 funds `referenceTransactionSn`。 | `CONFIRMED_HOST_CONTRACT` | 映射缺失、重复、漂移或结果 Unknown 时 fail-closed/manual；不得用 `contextVariables` 代替权威映射。 |
| `D-SLC-011` | 新 Profile 不声明 `PAY`；capability decision 和支付工具预交易准入不能替代 canonical 命令准入。`SETTLEMENT_LOCK`、清算确认及控制/纠错动作不是账户商业能力，保持豁免；出款只在首次 submit 检查 `WITHDRAW`。 | `IMPLEMENTED_WITH_H2` | `CORE-2C-A` 已复用现有账户查询和能力枚举落地；`CORE-2B-A` 仍是独立 Owner Gate，不能据此宣称新 Profile 已完成。 |
| `D-SLC-012` | canonical capability 准入放在 `DefaultRoutedFundsInstructionOrchestrator` 完成重放短路之后、空 legs 成功和账本组装之前；检查对象由 `instruction + resolvedRoute` 的明确动作规则解析，不按所有 PAYER/PAYEE participant 泛化。拒绝保留稳定 `FAILED` 资金审计事实与 `RouteSnapshot`，但不产生 posting、ledger transaction/entry 或余额变化。 | `IMPLEMENTED_VERIFIED` | 原路退款、费用退款、授权 successor、force completion、no-auth refund、余额控制、清算/结算和 payout receipt 按事实边界豁免；首次 payout submit 另查 `WITHDRAW`。 |
| `D-SLC-013` | 首个锁定后逆向只支持引用原 `SETTLEMENT_LOCK` 的全额 `SETTLEMENT_RELEASE`，不接收调用方金额；资金在同一事务 release 后立即进入可追踪 `FROZEN`，原结算单进入独立终态 `RELEASED`，并释放 active source claims 与 active order digest。新结算必须从纠正后的来源事实重新创建、审批、Gate 和锁定。部分释放继续 fail-closed。 | `IMPLEMENTED_PROVIDER_VERIFIED` | 最小公共契约与生产实现已按批准边界落地；fresh build、58 项聚焦 H2、PMD、完整 CAD 和独立 Checker 已通过；不能称为跨仓 L3 或生产准出。 |
| `D-SLC-014` | 初始 `pay -> CLEARING` 已由标准 pay 表达；仓内清分对象形成在资金交易之后，尚不能证明哪个 pay 前对象及其 Gate 精确覆盖 payer/payee/Money/业务动作，也不能阻止普通 pay 绕过专用包装器。 | `PENDING_BUSINESS_PREMISE` | 宿主先冻结不可变来源事实、exact Gate coverage、必须受 Gate 的 canonical action 和 Unknown/并发责任；此前不新增专用 API、execution-result 契约或准入表。 |
| `D-SLC-015` | 当前 reconciliation 是通用证据与 Gate 能力，不是来源采集、匹配策略、运营工单或外部 rail 终态系统。`ReconciliationMatchResultItem` 仅表达 1:1；1:N/N:1、watermark、容差、账龄/SLA/升级、条件放行和独立 close evidence 均未实现。 | `CONFIRMED_EXISTING_SCOPE` | 业务源/Fincone/rail/Finance 分别持有来源真实性、经营规则、外部终态和处置政策；涉及公共字段、枚举或 DDL 时进入 Owner Gate。 |
| `D-SLC-016` | Gate `PASSED` 只证明 exact tenant + object type/SN 下，指定结果是当前 lineage 头的完成态 `BALANCED` 结果且当前 lineage 无 blocking difference。冻结成员覆盖在 RunResult 录制时校验，Gate 当次不重新采集或重算来源；它不证明业务窗口应到数据收齐、业务对象当前摘要、TTL、容差、外部 Paid 或 release 授权。 | `CONFIRMED_EXISTING_SCOPE` | CORE-2D 必须在同一事务 current read Gate，并由独立 `SettlementReleaseAuthority` 校验 release 请求、结算单/原 lock 摘要、来源完整性和审批证据；普通 `PASSED` 不得单独释放。 |

### 3.1 `WF-FIN-CLR-FND-001` 设计裁决

`resolution-r1` 中关于清分、结算、出款和分阶段逆向的边界继续有效；其中 `CLR-GATE-003` 的专用初始 pay 前提已撤回，因为 Provider 请求没有绑定真实清分对象。当前保持 `PENDING_BUSINESS_PREMISE`，不授权 Fincone 写入、Git、目标数据库、跨仓 L3 或生产动作。

| Gate | 当前结论 | Owner 与停止线 |
| --- | --- | --- |
| `CF-001 / CLR-GATE-003` | `PENDING_BUSINESS_PREMISE`；原专用入口把内部创建归属暴露为公共契约，却未绑定真实清分对象，也不能阻止普通 pay 绕过，候选已撤回。 | Fincone/宿主先证明 pay 前不可变来源事实、Gate coverage 和强制动作分类；Provider 只在出现最小真实 RED 后重开。 |
| `CF-002 / settlement release` | 设计与生产实现已落地，fresh build/H2/完整门禁与独立 Checker 已通过。 | 本仓 Provider 证据已关闭；不替代 Fincone adapter、跨仓 L3 或生产准出。 |
| `CF-003 / payout external policy` | in-flight、`RETURNED` 和成功后逆向保持 Difference/manual/Recovery。 | rail/executor、Finance、risk 与 Product 未给出权威终局和责任政策前，不新增自动资金动作。 |

## 3A. 五类业务与跨域治理公共能力成熟度矩阵

成熟度口径：`L2` 为源码存在，`L3-H2` 为公共 Spring/H2 组合已证明；Fincone、issuer、银行通道、Payroll/AP 的设计或适配未实现时仍记 `PENDING`，不能由仓内测试代替。

| 场景 | 可复用公共能力 | 源码 / H2 证据 | 当前缺口与 Owner | 成熟度 |
| --- | --- | --- | --- | --- |
| VCC 发卡 / 卡交易 | `PaymentInstrumentTransactionApplicationService`、预交易快照、授权 hold、completion、reversal/refund、原 `RouteSnapshot` 回放。 | `PaymentInstrumentTransactionApplicationService.java`、`PaymentInstrumentPreTransactionSnapshotApplicationService.java`；`PaymentInstrumentTransactionAuthorizationTests`、`FundsAuthorizationTransactionFlowTests`。 | 卡、路由决策、issuer 事件、强制清算、未关联贷记、乱序归一和负余额处置由 Fincone/issuer 持有；无法证明原交易时人工。 | 公共资金链 `L3-H2`；issuer/PCI/完整 VCC 生命周期 `PENDING`。 |
| 全球账户 / ACH | `ExternalFundsEventApplicationService` 消费已确认入金；withdraw、PayoutOrder、Gate、原交易逆向和资金侧对账。 | `ExternalFundsEventApplicationService.java`；`GlobalAccountAchBusinessFlowTests`、`PayoutOrderApplicationServiceTests`。 | `Submitted/Accepted/Processing`、return、NOC、rail reversal 与外部 `Unknown` 由 Fincone/通道归一；实际到账终态由通道主责。 | 归一资金链 `L3-H2`；真实 ACH rail `PENDING`。 |
| 收单 / 订单交易 | 标准 pay、部分/原路 refund、幂等和交易/路由/账本回链。 | `FundsDirectTransactionService` 与请求契约；`AcquiringSettlementBusinessFlowTests`、`FundsDirectTransactionFlowTests`。 | 订单、账单、收单状态、迟到成功、退款范围、费用和履约由 Fincone/收单 Owner 持有；VCC 开卡费与首次入金必须是两笔业务/资金事实。 | 公共资金链 `L3-H2`；收单/订单/履约 `PENDING`。 |
| 分佣 / 邀请返利 / KPI | 每受益人标准 pay 到 `CLEARING`、内部清算、结算锁定、出款、adjust、Recovery 和原交易引用。 | `AgentCommissionSettlementBusinessFlowTests`、`ClearingBatchApplicationServiceTests`、`SettlementOrderApplicationServiceTests`、`RecoveryOrderApplicationServiceTests`。 | Fact/Attribution/Eligibility/Rule/Pool/allocation/ClearingItem/SettlementItem/handoff 与经营对账由 Fincone；Payroll/AP 终态由其主责。多受益人范围不明时人工。 | funds 分段原语 `L3-H2`；Fincone 实施 `PENDING`。 |
| 优惠让利结算 | 上游已确认出资责任后使用 `FundsBenefitContributionTransactionService` 记录单笔 Benefit contribution 与原路冲回。 | Benefit 公共契约；`FundsBenefitContributionTransactionServiceFlowTests` 与 `TDD-BEN-RED-035`。 | 券、折扣、营销归因、多出资方分摊和规则版本由 Fincone/营销 Owner 持有；commission/rebate/revenue share 不映射为 Benefit。 | 单笔让利资金链 `L3-H2`；完整优惠产品 `PENDING`。 |
| 跨域治理 | tenant/account/profile/capability/action/Money/idempotency/original reference；FundsTransaction -> RouteSnapshot -> Ledger -> Balance -> clearing/settlement/payout -> external reconciliation；投影重放。 | `DefaultRoutedFundsInstructionOrchestrator`、`ReconciliationGateApplicationService`、`FundsProjectionReplayApplicationService`；`FundsAccountCapabilityAdmissionFlowTests`、`FundsHostCompositionContractTests`。 | 初始 pay 的 fresh Gate 业务前提、Fincone adapter、跨仓 L3、目标数据库与外部 `Unknown`、payout 异常政策和部分成功仍由对应 Owner 关闭。 | capability、既有四个事务内 Gate 消费者与 release Provider 能力 `L3-H2`；`CLR-GATE-003` 保持 `PENDING_BUSINESS_PREMISE`。 |

拒绝新增 Commission、Benefit、ACH 或 VCC 场景专用公共 DSL：上述场景均能由已有账户、动作、金额币种、来源交易、RouteSnapshot、清结算和对账原语表达。`allocationGroupRef` 等经营标识保存在 Fincone `FundsHandoffRecord` 并映射到返回的 `fundsTransactionSn`，不进入 core，也不塞入 `contextVariables`。

## 4. Reusable Funds Primitives

| 资金阶段 | 现有原语 | 稳定边界 |
| --- | --- | --- |
| 已审批资金分配 | `FundsDirectTransactionService#pay` 到 `CLEARING` | 每个受益人独立资金交易；Fincone 保留 allocation 组和角色。 |
| 内部清算 | `FundsClearingTransactionService` | `CLEARING -> AVAILABLE`。 |
| 结算锁定 | `FundsSettlementTransactionService` | `AVAILABLE -> SETTLEMENT`。 |
| 清算/结算/出款准入 | `ReconciliationGateApplicationService#checkGate` | 现有三个 ApplicationService 在最终命令事务内 current read；inspect 仅解释。direct pay、adjust 和 Recovery 不因此自动获得 Gate。 |
| 清算后扣减 | `FundsBalanceControlService#adjust` | 独立审计事实，必须带来源、审批、证据和责任引用；不任意扣余额。 |
| 外部出款 | `PayoutOrderApplicationService` | 维护出款事实；submitted/accepted 不是外部成功。 |
| 出款后追偿 | `RecoveryOrderApplicationService` + 独立 `RECOVERY` 交易 | 只登记责任与已完成结果，不选择追偿策略。 |

## 5. Stage Matrix

| 更正到达阶段 | 正确动作 | 当前证据/缺口 |
| --- | --- | --- |
| 资金分配前 | Fincone 取消待提交项并按新输入摘要重算。 | 宿主行为，不调用 funds。 |
| 已入 `CLEARING`、未清算 | Fincone 从 handoff 取得每笔原分配 `fundsTransactionSn`，作为 `referenceTransactionSn` 原路退款并阻断候选。 | 单笔原路退款可复用；映射缺失、Unknown 或多受益人部分退款未闭环时人工。 |
| 已到 `AVAILABLE` | 追加 `SettlementAdjustment`，按原分配引用做受控扣减/调账并重新对账。 | adjust 原语已有；缺分佣组合 H2。 |
| 已锁定 `SETTLEMENT`、未出款成功 | 阻断 payout；释放、调整锁定或形成结算差错后重算。 | `P1-REVERSAL`：缺稳定公开闭环。 |
| 出款已提交/成功 | 等待权威回执；成功后不回滚，追加追偿、后续抵扣或人工收款。 | Recovery 责任/结果登记已有；策略仍由宿主持有。 |

## 5A. Fincone 回填契约校验

### Owner 与事实边界

| Owner | 持有事实、规则和状态 | 不得代判 |
| --- | --- | --- |
| 业务源 | 原业务事实、事件终态、事实版本、更正链、watermark/cutoff。 | 不计算 Fincone 清分结果，不判断资金执行完成。 |
| Fincone | Fact/Attribution/Eligibility 快照，规则版本，`basisAmount`、`commissionPoolAmount`、beneficiary allocation、ClearingItem/SettlementItem、目标策略、审批与 Gate 证据、handoff 映射、经营对账和 SettlementAdjustment。 | 不解释账本完成，不把 submitted/accepted 当 Paid，不替 funds/Payroll/AP 判终态。 |
| wind-funds | 账户/Profile/能力，FundsTransaction/RouteSnapshot，LedgerTransaction/Posting/Entry/Balance，资金 CLEARING/AVAILABLE/SETTLEMENT，payout/recovery 事实，以及通用对账证据链和资金 Gate current read。 | 不计算佣金池、归因、资格、受益人或 Payroll/AP 规则；不拥有来源归一、业务匹配策略、任务调度或 rail 对账关闭。 |
| Finance / Payroll / AP | 财税、薪酬和应付专业规则、审批、单据、付款结果、核销和各自终态。 | 不强行映射为 funds Profile 或资金交易终态。 |

`basisAmount` 是 Fincone 的经济基数，`commissionPoolAmount` 是规则算出的待分总额；Fincone 必须按同一 `fact + planVersion + currency` 保证 `sum(ClearingItem.amount)=commissionPoolAmount`。`allocationGroupRef`、`allocationRole`、`creditShare`、`beneficiaryRef` 和 RETAINED/RESIDUAL 仍由 Fincone 持有。wind-funds 只接收每个已确认受益人的标准 Money、已解析资金目标和 canonical action，不新增业务 allocation 公共字段。

### 分层状态与最小准入

`Source accepted/confirmed -> Fact/Attribution frozen -> Eligibility decided -> Calculated -> Clearing Confirmed -> target resolved -> PendingApproval/PendingGate -> Approved -> funds handoff -> funds CLEARING -> AVAILABLE -> SETTLEMENT -> payout submitted/accepted -> external final success/failure -> reconciled/closed`。

- Fincone `Calculated/Confirmed` 是经营应得与清分确认，不是 wind-funds `CLEARING` 或账务完成。
- wind-funds `AVAILABLE` 是已清算可用责任；`SETTLEMENT` 是已锁定待出款责任；两者都不是外部到账。
- payout `submitted/accepted/processing` 不是 Paid；只有外部轨道权威终态与资金/账本事实一致后，Fincone 才能关闭对应 handoff。
- 最小 handoff 必须含 tenant/handoffRef、事实/规则版本与摘要、`factSnapshotRef`、`allocationGroupRef`、`clearingItemRef`、`settlementItemRef`、beneficiary、已解析 `targetType/ref`、payer/funding source、Money/currency、business period/timezone/cutoff、reserve/holdback/negative/recovery policy、approvalRef、gateRef、canonical action、predecessorHandoffRef、requestVersion/idempotencyKey/requestDigest，以及结果、原交易、后续批次/结算/payout、对账和关闭引用。缺失、Unknown、摘要漂移或币种/目标不一致时不得提交；同一动作重试不得更换 handoffRef、业务号或幂等键。
- `gateRef` 只是历史证据引用，不是授权令牌。当前 clearing confirm、settlement lock、锁定后全额 release、payout submit 会在各自最终写事务内 `checkGate`；direct pay、balance adjust 和 Recovery 登记不消费 Gate。初始 pay 是否需要 fresh Gate 由宿主资金政策决定；未绑定真实清分来源前，`CLR-GATE-003` 保持 `PENDING_BUSINESS_PREMISE`。

### 对账与逆向关联

Fincone 对账链为 `FactSnapshot -> accrual/pool -> ClearingItem -> SettlementItem -> handoff/result -> business adjustment`；wind-funds 资金事实链为 `FundsTransaction -> RouteSnapshot -> LedgerTransaction/Entry -> Balance -> clearing/settlement/payout`，宿主可把标准化内部或 rail 来源证据送入通用 reconciliation。两链只通过显式映射连接，不共享状态机；wind-funds 不因接收 `EXTERNAL_STATEMENT/SETTLEMENT_REPORT` 来源类型而成为 rail 对账任务或外部终态 Owner。

最小关联号为：`tenantId + factSnapshotRef + planVersionRef + allocationGroupRef + clearingItemRef + settlementItemRef + handoffRef + idempotencyKey + businessScene/businessSn + fundsTransactionSn`；进入逆向后再追加 `referenceTransactionSn`，进入批次/出款后追加 clearingBatchSn、settlementOrderSn、payoutOrderSn/external resultRef。LedgerTransaction/Entry 由 wind-funds 使用 `fundsTransactionSn` 追溯，不要求 Fincone复制账本明细。

Fincone 必须把每个原始 ClearingItem/SettlementItem 唯一映射到实际返回的 `fundsTransactionSn`。退款/冲正不得用 `allocationGroupRef` 猜原资金交易，也不得把业务标识塞入 `contextVariables` 代替权威映射。已入 `CLEARING` 的单笔原路退款使用该流水作为 `referenceTransactionSn`；已到 `AVAILABLE` 追加受控 adjustment；已锁 `SETTLEMENT` 先阻断 payout，只有满足 current Gate、来源关闭事实、唯一 `SettlementReleaseAuthority` 且 payout 不存在或仍为 `CREATED` 时才可执行全额 release，否则 fail-closed/manual；出款后追加 Recovery/后续抵扣/人工收款，不回滚历史出款。部分冲正、多受益人原分配、累计冲正或并发退款无法证明唯一范围与守恒时，必须 fail-closed/manual，且不得覆盖历史批次、route、posting 或 entry。

## 5B. 第三轮对账契约审计

### 当前对象链与事务边界

Fincone `ClearingItem.CONFIRMED` 不是 wind-funds `ClearingBatch.CONFIRMED`。标准映射仍为 `Confirmed ClearingItem -> SettlementItem/APPROVED FUNDS_ACCOUNT handoff -> pay 到 CLEARING -> ClearingSplittableDetail/SplitResultSnapshot -> ClearingCandidate -> ClearingBatch.CONFIRMED -> SettlementOrder.LOCKED -> PayoutOrder -> 外部终态`。Fincone 以 `FundsHandoffRecord` 保存每笔经营对象到资金与批次流水号的映射，不共享 wind-funds 状态机。

除 `checkGate` 外，wind-funds 写入口使用本地 Spring `REQUIRED` 事务，查询入口使用 `readOnly` 事务；`checkGate` 为 `MANDATORY`，只能加入最终写事务。清算确认在一个事务内锁候选、重查来源交易/退款/RouteSnapshot 与每笔 CLEARING Gate，再执行 `CLEARING -> AVAILABLE`；结算锁定在一个事务内锁 SettlementOrder、重查来源/摘要和 SETTLEMENT Gate，再执行 `AVAILABLE -> SETTLEMENT`；首次出款提交在一个事务内依次锁 SettlementOrder/PayoutOrder、检查 `WITHDRAW`、PAYOUT Gate 和宿主 authority，只登记 `SUBMITTED`，不调用外部 rail。Fincone handoff、来源采集、wind-funds 与外部 executor 之间没有跨仓事务。

### 能力与缺口证据矩阵

| 维度 | 当前源码/H2 已证实 | 缺口与 Owner | 最小建议 |
| --- | --- | --- | --- |
| 范围、窗口、摘要 | `CreateReconciliationBatchRequest` 有 `reconciliationScopeRef`、可选 Gate 对象、ruleVersion、半开时间窗口和 timezone；SourceSnapshot 固化成员 content digest/evidence，RunResult 派生两侧/source/result digest。`ReconciliationBatchApplicationServiceTests`、`ReconciliationRunResultApplicationServiceTests` 覆盖来源漂移、完整覆盖和幂等。 | 没有独立 watermark、coverage mode、cutoff 完成声明或业务 scene；来源内容摘要由可信 adapter 生成。Owner：业务源/Fincone/rail adapter。 | 现阶段用稳定 scope、窗口、成员摘要和受控 evidenceRef；不能证明收数完整时不记录 BALANCED。新增字段须 Owner Gate。 |
| 匹配基数 | `ReconciliationMatchResultItem` 明确一项只表示 1:1；实现拒绝任一侧引用复用，测试覆盖 incomplete/outside/duplicate/reuse。 | 1:N/N:1 没有聚合匹配组和金额守恒。Owner：Reconciliation + 对应业务规则 Owner。 | 上游先形成不可变聚合事实后以 1:1 比较；不得重复 source ref 模拟聚合。需要通用聚合模型时另行裁决。 |
| 匹配规则与容差 | 已有 `EXACT_MATCH/RULE_MATCH`、来源质量，以及金额、币种、方向、状态、主体、缺失、重复等差异类型；金额差异可带 currency/amount。 | 没有 fee/timing/跨日专用类型，也没有阈值、舍入、状态映射或跨日归属策略字段；`RULE_MATCH` 只说明上游规则版本允许自动匹配。Owner：Fincone/Finance/rail 规则 Owner。 | 规则先在宿主版本化并把结论和证据送入；不得把 `RULE_MATCH` 当成 wind-funds 已执行容差政策。公共枚举/策略变化须 Owner Gate。 |
| 差异责任与运营 | Difference 保存 severity、`responsiblePartyRef`、状态、创建/处理/关闭人时和证据；Action 追加保存 actionType、审批、originalFactRef、资金结果与 evidence。 | 无 owner assignment、SLA、dueAt、aging、escalation、风险承接和工单状态机。Owner：Fincone/Finance/运营平台。 | 当前所有未闭环差异 fail-closed；运营字段留宿主工单，通过 differenceSn/evidenceRef 回链。 |
| 处置、核销与重跑 | Action 支持 `SUPPLEMENT_FACT/REVERSE/ADJUST/SUSPENSE/RECOVER/WRITE_OFF`，reconciliation 只登记已完成结果，不执行资金；重跑必须新建 successor batch，完成批次不覆盖；replace 显式 supersede 并 invalidates 旧差异。相关 batch/difference H2 覆盖并发和幂等。 | write-off 的审批/会计/容差政策不在 core；没有任意 reopen 或覆盖历史。Owner：Finance/业务责任方。 | 先在主责系统完成动作，再回链；只有新批次不可变结果可关闭差异。 |
| 关闭证据 | Difference 只有已回链 action 后，才能消费当前后继持久化 run result；BALANCED 后标记 RESOLVED。DifferenceReport 汇总 action history、rerun/Gate/evidence。 | 无独立 `closeEvidence` 对象或关闭授权服务；report completeness 是展示投影。 | 以 `action evidence + successor batch/run/resultDigest + lineage` 作为复合关闭证据；Gate decision 只是下游即时准入证据，不参与 `RESOLVED`。暂不新增公共对象；需要外部可验证关闭凭证时进入 Owner Gate。 |
| Gate | `checkGate` 为 `MANDATORY`，锁 current lineage/batch；校验 exact tenant/object type/SN、current head、completed batch/run binding、`BALANCED`，并读取该对象当前 lineage 的 blocking differences。冻结成员覆盖由 `recordRunResult` 封版时校验；Gate H2 覆盖旧结果、替代批次、差异上限、血缘循环/断裂和重跑。 | Gate 当次不重新采集或重算来源，不校验业务窗口应到数据收齐、业务 scene、当前 Settlement/Payout 对象摘要、来源真实性、TTL 或外部 Paid；decision 不持久化为独立授权事实。 | 最终资金动作同事务 current read；消费方持久化 run/result digest 和自身 authority 证据。`inspectGate` 只解释。 |
| 清分/清算准入 | Candidate 固化 FundsTransaction/detail、LedgerTransaction/Entry、RouteSnapshot digest、split snapshot、规则、金额和 Gate 证据；ClearingBatch confirm 重查来源、退款、摘要和 CLEARING Gate，并原子 `CLEARING -> AVAILABLE`。 | 标准 pay 与清算确认已有 H2；Fincone 经营确认仍不是资金清算确认，初始 pay 的 fresh Gate 前提、宿主 adapter、目标数据库和跨仓 L3 未验证。Owner：Fincone/宿主。 | 先以标准 pay 形成真实资金事实；若业务要求 pay 前 Gate，必须先证明可覆盖该命令的不可变来源对象和不可绕过动作分类，再重开最小 TDD。 |
| 结算/出款准入 | Settlement 从同主体/币种 CONFIRMED ClearingBatch 创建，审批后 lock 时重查来源/摘要与 SETTLEMENT Gate；Payout 首次 submit 检查 locked external endpoint、WITHDRAW、PAYOUT Gate 和唯一 host authority。 | rail 投递、回单拉取、Unknown、外部对账运行和 Paid 关闭不由 core 自动完成。Owner：宿主/rail/Finance。 | 保存 settlementOrderSn/payoutOrderSn/externalRef/receiptRef 与对账结果映射；非终态不关闭。 |

### CORE-2D 依赖裁决

CORE-2D 可以复用 current Gate 和 Difference lineage，但不能把普通 `PASSED` 当作 release authorization。当前 Gate 没有 TTL，也不直接重查 SettlementOrder 当前 `orderDigest` 或原 `SETTLEMENT_LOCK` 摘要；所谓 current 只表示指定 run 是 exact Gate 对象当前 lineage 头。当前也没有单独 closeEvidence，Difference 的复合关闭证据只有 action evidence、后继 batch/run/resultDigest 和 lineage；Gate decision 是下游即时准入证据，不参与 `RESOLVED`。

`CORE-2D-G1/G4` 已于 2026-08-06 获 Owner 批准：`SettlementReleaseAuthority` 在同一事务接收完整 current Gate 决策，并独立校验 release request、SettlementOrder 当前摘要、原 lock 交易/RouteSnapshot、审批，以及来源 Owner 给出的 coverage/watermark/cutoff、规则决策摘要、迟到数据和 supersession 状态；release 持久化 Gate run/result digest 与 authority decision digest。由 `reconciliation-face` 的 release 专用强类型请求/决策携带这些结论、摘要和 evidenceRefs，来源事实仍由业务源/Fincone/rail/Finance 持有，不扩展通用 Gate/RunResult。Gate 自身绑定 expected object digest、TTL 或独立 closeEvidence 不在本裁决内；Gate Unknown、来源未收齐、规则不可审计、来源摘要无法证明、payout 已提交或差异未闭环时继续 fail-closed/manual。

## 6. Wave Plan

### Wave 0 - Design And Checker Gate

| Task ID | 原子任务 | Owner | 写入范围 | 只读范围 | 状态 |
| --- | --- | --- | --- | --- | --- |
| `W0-PRODUCT` | 冻结应得、清算、结算、资金完成和多受益人边界。 | Product Loop | 本规格 | Fincone 清结算/分佣/订单设计 | `COMPLETED` |
| `W0-PAYMENT` | 冻结资金责任、正逆阶段、Gate、payout 与追偿边界。 | Payment Loop | 本规格 | Fincone 设计、funds 产品/face | `COMPLETED` |
| `W0-ARCH` | 映射现有原语、源码锚点、P1 与最小实现范围。 | Architecture Loop | 本规格 | funds source/tests | `COMPLETED` |
| `W0-CHECK` | 独立检查无第二内核、无 Benefit 误用、无历史覆盖且首包可执行。 | Independent Checker | 无 | 本规格及上述一手材料 | `PASS_DESIGN_OWNER_GATE_PENDING` |

Wave 0 完成条件：Checker `P0=0`，所有 P1 有 Owner、验证方式和停止线；否则回到对应角色，不进入编码。

### Wave 1 - Commission Reversal Characterization

| Task ID | 原子任务 | Owner | 写入文件 | 依赖关系 | 状态 |
| --- | --- | --- | --- | --- | --- |
| `CORE-2A-COMMISSION-REVERSAL-CHAR` | 在现有代理分佣业务流只增加 `AVAILABLE` 后带原资金引用的审计调账，以及 `LOCKED` 后取消被拒且无副作用两项 H2 特征测试。原路退款与出款后 Recovery 复用已有专测。 | Architecture Maker | `tests/src/test/java/com/wind/funds/transaction/application/flow/AgentCommissionSettlementBusinessFlowTests.java` | 早期 characterization 门禁已满足；新增 `CLR-GATE-003` P1 不追溯否定已完成测试，但继续阻断 Wave 2E 编码。 | `COMPLETED` |
| `CORE-2A-CHECK` | 独立复核测试确实执行、余额逐步变化、posting 平衡、ledger 追溯、幂等和无副作用。 | Independent Checker | 无 | Maker 验证完成后。 | `PASS` |

写入范围只限上述现有测试文件；只读参考为 direct refund、adjust、Gate、settlement、payout、recovery 生产实现与既有测试。任务互不重叠，顺序固定为补 characterization 测试 -> 运行 -> 相关回归 -> Checker；不得人为制造失败或借机修改生产代码。

禁止事项：不得新增公共 API、Profile、Commission/Benefit 服务、负余额默认策略、内存业务 Service 或生产实现；测试若证明必须改变公共契约，立即停止并转 Owner Gate。

验收场景：

1. 已到 `AVAILABLE` 后发生更正，只追加带 `sourceSn=原 fundsTransactionSn`、审批、证据和责任引用的调整事实；原 route snapshot、detail、posting、entry 和清算批次保持不变，同一调整重放不重复。
2. 已锁定 `SETTLEMENT` 且未出款时，调用现有取消入口必须被拒绝，余额、结算单、资金交易、route、posting 和 entry 无新增或覆盖；该证据只确认 fail-closed，并把安全撤销保留为 `P1-REVERSAL` Owner Gate。

Fincone 映射、映射漂移、前序 Unknown、allocation 守恒、部分退款和多受益人回收全部留在 Wave 3 与各自 Fincone Owner Gate，本仓不以测试夹具伪造宿主实现；初始 `pay -> CLEARING` 的原子 Gate 单独归 `CLR-GATE-003`。原路径退款与 Recovery 只运行现有专测做回归，不在代理分佣测试重复实现。

验证命令：

```bash
just mvn-version
just compile
just test-one AgentCommissionSettlementBusinessFlowTests tests
just verify-slice AgentCommissionSettlementBusinessFlowTests,FundsBalanceAdjustAuditFlowTests,RecoveryOrderApplicationServiceTests tests
just test-business-flow
just pmd
just verify-cad
```

Wave 1 完成条件：目标 Surefire 报告可回链到新鲜命令，0 failure / 0 error；Checker `P0=0` 且没有新增未归属 P1。`P1-REVERSAL` 以明确 Owner Gate 保留，不因 characterization 测试转为已完成。

Wave 1 执行证据（2026-08-05）：

| 检查 | 结果 | 证据边界 |
| --- | --- | --- |
| `just test-one AgentCommissionSettlementBusinessFlowTests tests` | `4/4 PASS` | 两项新增 H2 场景实际执行。 |
| 组合 `verify-slice` | `17/17 PASS` | 分佣、调账审计和 Recovery 相关切片。 |
| `just test-business-flow` | `165/165 PASS` | 业务组合回归。 |
| `just compile` | `PASS` | 21 个 Maven 模块编译成功。 |
| `just pmd` | `PASS_WITH_LIMIT` | `tests` 模块提示 `No files to analyze`，不作为本次测试源码静态分析证据。 |
| `just verify-cad` | `1047 tests / 0 failure / 0 error / 1 skipped`，`BUILD SUCCESS` | H2 与架构/契约全量基线；不替代外部轨道和生产验收。 |
| Final Checker | `P0=0 / P1=0 / P2=0` | 只关闭 Wave 1；Owner Gate 继续保留。 |

### Wave 2 - Settlement Payee Profile Owner Gate

| Task ID | 原子任务 | Owner | 写入范围 | 状态 |
| --- | --- | --- | --- | --- |
| `CORE-2B-PROFILE-DECISION` | 裁决中性 Profile 名称、bucket、商户兼容和迁移窗口。 | Product + Core API + Wallet + Ledger + Clearing Owner | 本规格决策记录 | `DECISION_READY_CHECKER_PASS` |
| `CORE-2B-A-PROFILE-IMPLEMENT` | 用户批准后以 additive 方式新增 `FUNDING_SETTLEMENT_PAYEE`，同步 Profile、能力画像、API baseline、H2 和权威文档。 | Architecture Maker + Checker | 见下方原子包 | `AWAITING_USER_APPROVAL` |
| `CORE-2C-A-CAPABILITY-ADMISSION` | 在 canonical 命令入口落实账户动作准入，冻结退款/逆向豁免与 payout submit 检查。 | Core API + Wallet + Transaction Owner | 见下方原子包；不进入 CORE-2B-A | `COMPLETED_CHECKER_PASS` |
| `CORE-2D-1-SETTLEMENT-RELEASE-PRIMITIVE` | 新增独立、带原锁定交易引用的全额 `SETTLEMENT_RELEASE` 资金原语。 | Core API + Transaction + Ledger Owner | 公共契约、生产实现、H2 与 baseline | `IMPLEMENTED_PROVIDER_VERIFIED` |
| `CORE-2D-2-SETTLEMENT-RELEASE-LIFECYCLE` | 在结算单与 payout 生命周期中编排 release、current Gate、专用 authority 和 CREATED payout 原子取消。 | Reconciliation + Transaction + Wallet Owner | face/impl、H2 DDL、文档 | `IMPLEMENTED_PROVIDER_VERIFIED` |
| `CORE-2E-CLR-GATE-003` | 判断初始 `pay -> CLEARING` 是否存在必须 fresh Gate 的真实业务前提。 | Fincone/宿主 + Reconciliation + Transaction Owner | 只读来源事实、Gate coverage 和 canonical action 证据；未冻结前不写公共 API、DDL 或 Provider 实现 | `PENDING_BUSINESS_PREMISE` |

`CORE-2B-A` 推荐原子包：

1. 在 `LedgerProfileCode` additive 新增 `FUNDING_SETTLEMENT_PAYEE` 并同步 stable API baseline；保留 `FUNDING_MERCHANT`。
2. 新 Profile 只含 `CLEARING(CLEARING/CREDIT/false)`、`AVAILABLE(LIABILITY/CREDIT/true)`、`FROZEN(LIABILITY/CREDIT/false)`、`SETTLEMENT(LIABILITY/CREDIT/false)`；不含 `AUTHORIZATION/ADJUSTMENT`。
3. 能力画像只含 `RECEIVE + WITHDRAW`；`AVAILABLE=true` 仅表示 BalanceControl 可在显式审计门禁下申请受控负余额，普通路径继续非负。
4. Agent commission H2 显式选择新 Profile 并断言 RouteSnapshot；收单 H2 继续断言 `FUNDING_MERCHANT`。
5. 补 Profile 精确契约、真实初始化四桶、能力 decision、冻结/解冻或提现、清算/结算/payout、调账和负余额 fail-closed 回归。现有通用测试可复用的行为不重复造新测试。
6. 同步产品、DSL、系分和用户接入说明；不改 converter、`ACCOUNT_PAYABLE` 默认映射、DDL、历史数据、RouteSnapshot 或 Fincone。

停止条件：直接 rename/remove/migrate `FUNDING_MERCHANT`；自动映射 `ACCOUNT_PAYABLE`、Payroll/AP；给新 Profile 加 `PAY/AUTHORIZATION/ADJUSTMENT`；普通动作制造负余额；BalanceControl 缺 policy/approval/reason/risk/limits/aging 仍放行；未更新 API baseline；或任一失败产生部分资金/账务副作用。当前代码只验证负余额限额证据存在、正数和同币种，未证明实际单笔/累计额度已经权威执行，不得扩大声称。

### Wave 2C - Canonical Capability Admission

`CORE-2C-A` 不新增 core/face API、枚举、工厂或第二套准入服务，只复用 `FundsAccountQueryService`：

1. 在 `DefaultRoutedFundsInstructionOrchestrator#execute` 的已完成重放短路之后、空 legs 成功之前做首次命令准入。拒绝按既有生命周期记为稳定 `FAILED` 资金事实并保留 `RouteSnapshot`，不留下 posting、ledger transaction/entry 或余额副作用。
2. 动作矩阵：`TOPUP=target RECEIVE`；`TRANSFER/PAY=payer PAY + payee RECEIVE`；业务确认 direct `REFUND=payer PAY + target RECEIVE`；`WITHDRAW=account WITHDRAW`；独立 `FEE_CHARGE=account PAY`；`AUTHORIZE=account PAY`，存在 linked funding 时再查其 `PAY`。
3. 原交易 `REFUND`、`FEE_REFUND`、授权 `REVERSAL/COMPLETE/AUTH_REFUND` successor、force completion、no-auth refund、附加 fee、`FREEZE/UNFREEZE/ADJUST`、`CLEARING_CONFIRM/SETTLEMENT_LOCK` 和 `PAYOUT_SUCCEEDED/PAYOUT_FAILED` 保持豁免。目标账户状态仍由既有 refund admission 等契约判断。
4. `PayoutOrderApplicationServiceImpl#submitOrder` 只在 `CREATED` 首次提交、幂等短路之后、Gate 和 `PayoutSubmissionAuthority` 之前校验结算主体 `WITHDRAW`；外部回单不重查 capability。

写入白名单：

- `transaction/impl/src/main/java/com/wind/funds/transaction/DefaultRoutedFundsInstructionOrchestrator.java`
- `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/payout/impl/PayoutOrderApplicationServiceImpl.java`
- `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsAccountCapabilityAdmissionFlowTests.java`
- 按需增量更新既有 `FundsAuthorizationTransactionFlowTests.java`、`PayoutOrderApplicationServiceTests.java` 和用户接入说明；不得修改 core/face、Profile、枚举或 DDL。

最小 H2：正反向动作矩阵；payer/payee/linked funding 分别拒绝；拒绝只保留稳定 `FAILED` 事实与 `RouteSnapshot`；成功后撤销 capability 仍可幂等重放；AUTHORIZE 拒绝型空 legs 仍查 `PAY`；原路退款、successor、force/no-auth 和控制动作不因当前 capability 漂移被阻断；payout 首次 submit 缺 `WITHDRAW` 原子失败，提交后 capability 漂移不阻断成功或失败回单。

停止条件：需要遍历所有 participant 推断准入、在完成重放前检查、拒绝产生 posting/ledger/entry/余额副作用、回单重查 capability、或必须修改公共 API。上述任一情况转 Owner Gate。

Wave 2C 执行证据（2026-08-06）：

| 检查 | 结果 | 证据边界 |
| --- | --- | --- |
| `just compile` | `PASS` | 21 个 Maven 模块编译成功。 |
| 聚焦 `verify-slice` | `20/20 PASS` | capability admission `11/11`，payout submit/receipt `9/9`。 |
| `just test-transaction` | `141/141 PASS` | transaction 回归；新增 capability 类由上项聚焦命令实际覆盖。 |
| `just test-business-flow` | `165/165 PASS` | 业务组合回归。 |
| `just test-boundary` | `194/194 PASS` | 模块与契约边界回归。 |
| `just pmd` | `PASS_WITH_LIMIT` | `tests` 模块提示 `No files to analyze`，不作为测试源码静态分析证据。 |
| `just verify-cad` | `1060 tests / 0 failure / 0 error / 1 skipped`，`BUILD SUCCESS` | 全量 H2、架构、契约和静态基线；不替代外部轨道或生产验收。 |
| Final Checker | `P0=0 / P1=0 / P2=0` | 关闭 CORE-2C；CORE-2B、CORE-2D 和 Fincone Gate 不随之关闭。 |

### Wave 2D - Locked Settlement Full Release

`CORE-2D` 拆为两个可独立审查、但不得单独发布的子包：

1. `CORE-2D-1` 在既有 `FundsSettlementTransactionService` 增加原子 `release` 用例：先以 `ORIGINAL_TRANSACTION` 引用原 completed `SETTLEMENT_LOCK`，创建独立 `SETTLEMENT_RELEASE` 资金交易并执行 `SETTLEMENT -> AVAILABLE`；随后立即复用既有 `FundsBalanceControlService#freeze` 执行 `AVAILABLE -> FROZEN`，创建 `freezeType=SETTLEMENT_RELEASE_HOLD` 的受保护 `FundsFrozenOrder`。两步必须加入同一物理事务，返回 `releaseFundsTransactionSn + releaseFreezeOrderSn`，不得暴露或提交只完成第一步的裸 release。该服务只从原 lock 交易及其 RouteSnapshot 派生并校验主体、金额、币种及目标 Profile 同时支持 `SETTLEMENT/AVAILABLE/FROZEN`，不读取 reconciliation 的 `SettlementOrder`；Profile 不满足时 fail-closed，不降级回 `AVAILABLE`。
2. `CORE-2D-2` 在既有 `SettlementOrderApplicationService` 增加 release 用例，不新增第二个 release 应用服务；固定锁序 `SettlementOrder FOR UPDATE -> PayoutOrder by settlementOrderSn FOR UPDATE`。无 payout 可继续；`CREATED` payout 在同一事务转 `CANCELLED` 并保存取消审计；任何已提交、处理中或终态 payout 均拒绝 release。
3. 在上述锁内 fresh `checkGate` 读取当前 lineage/difference/digest，把完整当前决策交给唯一 `SettlementReleaseAuthority`，仅 `RELEASE_ALLOWED` 可继续；普通 Gate `PASSED` 不是 release 授权。该扩展点复用现有 `PayoutSubmissionAuthority` 的装配与失败关闭模式，但不复用其出款语义，也不抽象通用 `Authority<T>`：face 定义专用接口、authority context 请求和强类型决策，impl 通过 `ObjectProvider#getIfUnique()` 取唯一宿主实现；缺失、多实现、拒绝、非法摘要、空证据或过期决策全部阻断。保存 current Gate digest、authority decision digest、approval/evidence、`releaseFundsTransactionSn`、`releaseFreezeOrderSn` 和 `releaseDigest`。
4. `CORE-2D-2` 独立校验原锁定交易的主体、金额、币种与 `SettlementOrder` 一致。release 与 freeze、`CREATED` payout 取消、active source claims/order digest 释放和 SettlementOrder 更新必须在同一本地事务全有或全无；任一步异常（包括 freeze 的 `LedgerPostingRejectedException`）都必须逃逸外层并整体回滚，不能沿用单笔资金指令保留确定性失败事实的 `noRollbackFor` 语义。成功后以独立 `RELEASED` 终态表达锁定后全额释放，`CANCELLED` 只表达草稿期取消；原 netAmount、lock transaction、route、posting、entry、旧 Gate 和来源摘要保持只读。新结算单必须使用新 SN、新来源摘要、新审批和新 Gate。
5. `CORE-2D-G2` 已批准首包最终 disposition 固定为 `FROZEN`，不按自由文本 reason 分叉。`PAYOUT_FAILED` 的 `SETTLEMENT -> AVAILABLE` 表达外部权威确认失败且原责任未被否定，不是纠错、迟到、替代或争议下的 release；后四类场景在责任重算前直接恢复可用会放大重复消费和追偿风险。首包必须在 `FundsTransactionCommandServiceImpl` 的 canonical `FREEZE_ORDER` 引用准入处拒绝任何通用命令消费 `SETTLEMENT_RELEASE_HOLD`，至少覆盖 `unfreeze` 和 `withdraw`，保持其既不可解冻也不可出款；不能通过改变 query 返回值破坏冻结单审计/解释查询。未来只能由独立、带 authority 的结算处置入口引用 `releaseFreezeOrderSn`，不得用 `releaseFundsTransactionSn` 或调用方 `contextVariables` 绕过。部分 release、直接 `AVAILABLE`、后续解冻/扣划入口和 reasonCode 自动分流不进入首包。

`SettlementReleaseAuthority` 最小输入与职责如下；`CORE-2D-G1/G2/G3/G4` 已获批准并按该边界实施。承载位置为 `reconciliation-face` 的 release 专用强类型请求/决策；它只携带来源 Owner 已产出的状态、摘要和证据引用，不把来源采集或业务规则下沉，也不扩展通用 Gate/RunResult。`SettlementOrderApplicationServiceImpl` 在锁定 SettlementOrder 与按 settlementOrderSn 查询的可空 PayoutOrder 后组装 authority context，包含当前 `SettlementOrderDTO`、系统读取的可空 `PayoutOrderDTO`、外部 release 意图/来源证据请求和完整 current `ReconciliationGateDecisionDTO`；外部请求不得提供 payout 状态或 release disposition。Authority 返回包含 `releaseAllowed`、`decisionDigest`、非空 `evidenceRefs`、未来 `expiresAt` 和 release disposition 的专用决策；首包只接受固定值 `FROZEN`：

| 输入/责任 | 最小事实 | Owner 与校验边界 |
| --- | --- | --- |
| 释放对象 | tenantId、settlementOrderSn、orderDigest、原 `lockFundsTransactionSn`、原 RouteSnapshot digest、主体、Money、release request/idempotency digest。 | Settlement 编排锁单并重查；Transaction primitive 只按原 lock 生成新 release 事实，不读取 reconciliation 表。 |
| 当前对账 | `SETTLEMENT + settlementOrderSn`、`reconciliationRunResultSn + reconciliationResultDigest`、`currentLineageBatchSn`（必须等于 Gate decision 的 `reconciliationBatchSn`）、Gate status、blocking differences/evidence。 | Reconciliation `checkGate` 在最终事务内锁定并验证 current lineage、完成绑定、BALANCED 和差异集合；不重算来源。 |
| 来源关闭 | `coverageStatus=COMPLETE + coverageDigest`、`watermark/cutoff`、`ruleVersion + ruleDecisionDigest`、`lateDataStatus`、`resultReplacementStatus/lineageSupersessionStatus` 及 evidenceRefs。 | 业务源/Fincone/rail/Finance 对各自来源完整性和规则真实性负责；authority 只消费可信结论，缺失、Unknown 或不可审计时阻断。 |
| 业务授权 | approvalRef、release reason、authority decision、decisionDigest、authorizedBy/At、evidenceRefs、release disposition。 | 唯一 `SettlementReleaseAuthority` 判定是否允许释放；普通 Gate `PASSED` 不替代该判断。 |
| payout 竞争 | 系统锁内读取的可空 `PayoutOrderDTO`，包含 `payoutOrderSn + currentPayoutStatus`；无单、`CREATED`、已提交/处理中/终态必须区分。 | Settlement 编排按固定锁序读取并组装 authority context；外部请求没有 payout 状态覆盖字段。只有无 payout 或同事务取消 `CREATED` 才可继续，其余全部拒绝。 |

成功时 Settlement 编排在同一事务保存 Gate run/result digest、authority decision digest、approval/evidence、`releaseFundsTransactionSn`、`releaseFreezeOrderSn`、`releaseDigest` 和最终 disposition。两个动作使用由 `settlementOrderSn` 稳定派生且彼此独立的业务幂等键，并由 SettlementOrder 显式回链，不把关联身份塞入 `contextVariables`。`SETTLEMENT_RELEASE_HOLD` 是系统受保护冻结类型，不接受通用 unfreeze、withdraw 或其他 `FREEZE_ORDER` 消费；审计链预期包含两笔账本交易，但事务外只能观察到 `SETTLEMENT -N / AVAILABLE 0 / FROZEN +N` 的最终净变化。这些是 release 审计，不是通用 Difference `closeEvidence`，也不能替 Fincone 关闭经营 SettlementItem。

最小 H2：全额 release 后最终 `SETTLEMENT -N / AVAILABLE 0 / FROZEN +N`，两笔 posting 各自平衡且 SettlementOrder 精确保存两个事实 SN；同摘要重放不新增事实，异摘要、部分金额、错原 lock、错主体/金额/币种拒绝；Gate Unknown、Authority 缺失/多实现/拒绝/非法摘要/空证据/过期无副作用；authority 只能看到系统锁内读取的 payout 当前状态且外部请求无覆盖字段；空或非 `FROZEN` disposition 阻断；release 第一步失败不创建 freeze，freeze 普通异常或 `LedgerPostingRejectedException` 使 release 交易/RouteSnapshot/detail/ledger、freeze order、订单、payout 取消和 active claim 变化全部回滚；CREATED payout 原子取消，已提交或终态 payout 拒绝，release/release 与 release/submit 并发唯一胜者；`releaseFreezeOrderSn` 指向 `SETTLEMENT_RELEASE_HOLD`，直接用通用 `unfreeze` 或 `withdraw` 消费时必须在生命周期事实创建前失败，且冻结余额、冻结单、FundsTransaction/RouteSnapshot/账本事实均无变化，使用 `releaseFundsTransactionSn` 也不得命中冻结来源；受保护冻结单仍可被只读查询和解释；旧事实不变，新结算使用新 SN/来源/Gate。部分 release、累计 release、SettlementItem 级范围、多受益人分摊和后续专用解冻/扣划不在首包；在残余锁定金额、item scope、重新 Gate、处置 authority 与并发模型齐备前保持 fail-closed/manual。

Owner 待裁决：

- `CORE-2D-G1`：`APPROVED 2026-08-06`。在既有 `SettlementOrderApplicationService` 增加 release 用例，并新增专用 `SettlementReleaseAuthority`；复用 `PayoutSubmissionAuthority` 的唯一宿主实现、有效期、摘要和证据校验模式，不复用其出款语义、不新增通用 Authority 框架。current Gate 与 authority 决策证据必须同事务持久化。
- `CORE-2D-G2`：`APPROVED 2026-08-06`。首包最终 disposition 固定 `FROZEN`；内部以同一事务的 `SETTLEMENT_RELEASE: SETTLEMENT -> AVAILABLE` 加既有 `freeze: AVAILABLE -> FROZEN` 创建标准 FundsTransaction 与 `SETTLEMENT_RELEASE_HOLD` 冻结单，不允许提交中间 `AVAILABLE`，也不新增复合 lifecycle recorder。通用 `unfreeze`、`withdraw` 和其他 `FREEZE_ORDER` 消费必须在 canonical command admission 拒绝该受保护类型，后续专用处置另行裁决。VCC、全球账户、收单、分佣分账和优惠让利场景均未发现责任未重算却应立即自由使用的有效反例。
- `CORE-2D-G3`：`APPROVED 2026-08-06 / IMPLEMENTED_PROVIDER_VERIFIED`。最小公共 API、枚举、受保护冻结引用准入、DDL 与 H2 已按下述不可拆包实施；`CORE-2D-FIX-001` 已关闭 release 重放在余额预检前重复执行的问题，fresh gates 与独立 Checker 均通过；不得据此宣称跨仓 L3 或生产准出。
- `CORE-2D-G4`：`APPROVED 2026-08-06`。`coverageStatus/coverageDigest/watermark/cutoff`、`ruleVersion/ruleDecisionDigest`、`reconciliationRunResultSn/reconciliationResultDigest`、`currentLineageBatchSn`、`lateDataStatus` 和 `resultReplacementStatus/lineageSupersessionStatus` 进入 `reconciliation-face` 的 release 专用外部请求/authority context；`currentPayoutStatus` 只能由 Settlement 编排锁内读取并放入 context，外部请求不得提供。来源事实仍由宿主 Owner 持有，不进入通用 Gate/RunResult，普通 Gate `PASSED` 不足以释放。

`CORE-2D-G3` 推荐执行包：

1. `transaction-face/core` 只新增 `FundsSettlementReleaseRequest`、返回两个事实引用的 `FundsSettlementReleaseResultDTO`、`FundsTransactionEventType.SETTLEMENT_RELEASE`，并在既有 `FundsSettlementTransactionService` 增加 `release`；请求只接受原 `lockFundsTransactionSn` 与稳定 `settlementOrderSn`，不接受 account、amount、currency、disposition 或 `contextVariables`。不新增单独 release service、复合 lifecycle、通用 authority 或场景 DSL。
2. `transaction-impl` 在同一 `@Transactional(rollbackFor = Exception.class)` 中锁原 `SETTLEMENT_LOCK`，从原交易与 RouteSnapshot 派生全额 `SETTLEMENT -> AVAILABLE`，随后复用 `FundsBalanceControlService#freeze` 形成 `SETTLEMENT_RELEASE_HOLD`。在现有 canonical `FREEZE_ORDER` 引用准入处统一拒绝受保护 hold 的通用消费；只读冻结单查询不变。该外层不得声明 `noRollbackFor = LedgerPostingRejectedException.class`。
3. `reconciliation-face` 只在既有 `SettlementOrderApplicationService` 增加返回现有 `SettlementOrderDTO` 的 `releaseOrder`，新增 `ReleaseSettlementOrderRequest`、`SettlementReleaseAuthority` 及其 context/decision DTO、单值 `SettlementReleaseDisposition.FROZEN`，并为 `PayoutOrderState` 增加 `CANCELLED`。不新增重复的 release 结果 DTO、第二个应用服务或通用 `Authority<T>`。
4. `reconciliation-impl` 固定锁序并复用既有 mapper/service：`SettlementOrder FOR UPDATE -> PayoutOrder by settlementOrderSn FOR UPDATE -> releaseDigest/completed replay -> 首次执行才 fresh checkGate -> unique SettlementReleaseAuthority -> FundsSettlementTransactionService#release`。锁内已有两个 release 事实引用时，同摘要直接返回已保存结果，不受后续 Gate lineage 漂移、Authority 缺失/拒绝/过期影响；异摘要立即冲突。只有首次执行才检查 payout 状态和调用 Gate/Authority。无 payout 或原子取消 `CREATED` 才继续；其余 payout 状态全部拒绝。SettlementOrder 只增加重放和审计必需字段：两个资金事实 SN、disposition、releaseDigest、release 专用 Gate run/result/lineage/evidence digest、来源关闭 digest、authority decision digest/evidence refs、approvalRef、reason、releasedBy/Time；PayoutOrder 只增加 CREATED 取消的 cancelledBy/Time/reason。同步 H2 与 MySQL 建表基线，但只使用 H2 执行验证，不连接目标 MySQL。
5. 先补 RED H2，再实现；最小切片覆盖最终三 bucket、两笔平衡账本、同/异摘要重放、成功后 Gate 漂移或 Authority 缺失/拒绝/过期时同摘要仍返回且不新增事实、原 lock/主体/Money/Profile 校验、首次 Gate/Authority 失败关闭、普通异常与 `LedgerPostingRejectedException` 整体回滚、payout 状态与 release/submit 竞态、active claim/order digest、受保护 hold 的 unfreeze/withdraw 拒绝及只读可查。发布门禁为 `verify-core-api`、transaction/reconciliation face 公共契约测试、聚焦 H2、transaction/business-flow/boundary/governance 回归、`pmd`、`verify-cad` 全部通过；任何子包缺失即不发布。

`CORE-2D-FIX-001` 推荐修复包（`APPROVED_IMPLEMENTED_FRESH_GATES_PASS`）：

1. fresh `verify-slice` 在 7 类 55 个测试中得到 54 pass / 1 error；单类 8 个测试复跑稳定复现同一错误。第二次 `FundsSettlementTransactionService#release` 已有完整 protected hold，却仍再次进入 freeze 路由余额预检，以 `AVAILABLE=400` 重扣 `600`，尚未到达冻结生命周期幂等识别。
2. 根因限定在 `FundsSettlementTransactionServiceImpl#release` 的两步组合编排。通用 `DefaultRoutedFundsInstructionOrchestrator` 必须先得到 RouteSnapshot 才能执行现有生命周期摘要校验，不能为单一结算场景全局前移或加入特例；`DefaultFundsFrozenOrderLifecycleSaver` 已能按稳定业务键校验并复用完成事实，不是本轮修改对象。
3. 生产源码最小写集只允许修改 `FundsSettlementTransactionServiceImpl`：锁定并验证原 `SETTLEMENT_LOCK` 后，先读取既有 `SETTLEMENT_RELEASE_HOLD`；若存在，严格校验其 `FROZEN` 状态、账户、金额、币种、release transaction 及其对原 lock 的 provenance，完整时直接返回既有 release/freeze 两个事实引用，不再执行 release/freeze；不完整、篡改或歧义事实一律 fail-closed。不得修改 public API、core、route/orchestrator、通用冻结生命周期、DDL 或 Fincone。
4. 复用现有失败测试作为 RED，并在同一既有 release 幂等用例补充账户状态漂移后的重放断言，不新增重复测试类；修复后依次执行 `FundsSettlementTransactionFlowTests`、`SettlementOrderApplicationServiceTests`、`DefaultFundsFrozenOrderLifecycleSaverTests`、原 7 类 55 tests、`compile`、`pmd`、`verify-cad` 和独立 Checker。任何资金事实、账本或余额断言失败即停止，不进入 `CLR-GATE-003`。

执行证据（2026-08-10）：生产源码只修改 `FundsSettlementTransactionServiceImpl`，在原 lock 校验后优先读取并严格校验既有 protected hold、release transaction 和 RouteSnapshot provenance，完整事实直接重放返回，不再执行 release/freeze；测试只增强既有 release 幂等用例，证明首次成功后账户状态漂移仍返回相同事实引用且不新增账务事实。CORE-2D 8 类 `58/58`、迁移后的 DSL verifier/amount/instruction/FeeSpec `64/64`、`just compile`、`just pmd`、`just verify-cad` 均通过。DSL 合约验证器迁移只改变验证器所在测试包及模型包引用，与本修复无公共契约、DDL 或行为冲突；迁移写入期间出现过短暂测试编译/断言窗口，待文件稳定后复跑全绿，本修复未修改迁移文件。Mockito inline agent 在沙箱内不可 attach，允许 agent attach 后被冻结单 3 项及 CORE-2D 58 项均通过，该环境差异不记为业务失败。独立 Checker 最终 `P0=0 / P1=0 / P2=0`。

### Wave 2E - CLR-GATE-003 Reopened

原 Gate-aware 候选已撤回，原因不是实现细节，而是业务前提不成立：

1. 仓内 `ClearingSplittableDetail/Candidate/Batch` 都建立在已有 FundsTransaction/detail/Route/Ledger 事实之上，不能作为初始 pay 前的 Gate 对象。
2. 候选接收任意 Gate SN 与任意 payer/payee/Money，无法证明该 Gate 的来源集合精确覆盖这笔命令。
3. 普通 `FundsDirectTransactionService#pay` 仍可直接进入 `CLEARING`，专用包装器不能形成不可绕过的 Provider 准入。
4. `created` 只是交易生命周期内部执行结果；为单一未落地编排把它提升为 transaction-face DTO、异常和 executor，会把持久化归属泄漏成公共契约。

因此删除专用 application API、execution-result/ownership 载体、准入 Entity/Mapper/H2 表和对应候选测试，恢复既有 `pay` 契约。`reconciliation-impl` 只消费已存在资金事实和 `FundsClearingTransactionService`、`FundsSettlementTransactionService`、`FundsPayoutTransactionService` 三类阶段专用资金原语，不直接调用通用 pay。

只有同时满足以下条件才重开 TDD：宿主提供 pay 前不可变来源事实；证明 exact Gate coverage 包含 payer/payee/Money/业务动作；冻结哪些 canonical action 必须受 Gate 且普通 pay 不可绕过；给出重启、并发和 Unknown 下必须由 Provider 原子持有的真实 RED。未满足前不得新增公共 API、DDL、通用 GateConsumption 或场景 DSL。

### Wave 3 - Fincone Handoff

| Task ID | 原子任务 | Owner | 写入范围 | 状态 |
| --- | --- | --- | --- | --- |
| `FINCONE-CLR-COMMISSION-HANDOFF` | 在 Fincone 落地 `sum(ClearingItem.amount)=commissionPoolAmount`、显式 `RETAINED/RESIDUAL`、SettlementItem、FundsHandoffRecord、outbox、Unknown 阻断和 Adjustment，并持久化每笔经营分配到 `fundsTransactionSn` 的唯一映射。 | Fincone Product/Architecture/Funds/Finance/HR Owners | Fincone 仓库，当前只读 | `PENDING_FINCONE_GATES` |

Fincone 必须先关闭 `CLR-GATE-001~006`。当前 Goal 只提供 handoff 契约与验收种子，不拥有 Fincone 写入或生产授权。

## 7. Grant 历史与当前授权

以下是已消费的历史 Execution Grant，只用于解释当前工作树来源，不构成 `WF-CLR-DOC-001` 或后续 Wave 的当前授权：

- 本规格创建与验证。
- Wave 1 的单一现有 H2 测试文件增量修改和本地验证。
- `CORE-2C-A` 的两个既有实现边界、最小 H2 和用户接入指南；不新增公共 API、Profile、枚举或 DDL。
- `CORE-2D-G3` 冻结的最小公共 API/枚举、transaction/reconciliation 实现、H2 与静态 MySQL DDL 基线；不连接目标 MySQL。

`CORE-2B-A` 仍需单独批准；`CORE-2D-G1/G2/G3/G4` 与 `CORE-2D-FIX-001` 的实现 Grant 已消费，本仓 fresh 验证和独立 Checker 已关闭。`CORE-2E-OWNERSHIP-001` 与 `CLR-GATE-003` 曾形成 Provider 候选，但后续能力提供者边界评审证明其业务前提未冻结且普通 pay 可绕过，候选已整体撤回，不构成当前能力或证据。

`WF-CLR-DOC-001` 与 `CORE-2D-FIX-001` 授权均已消费；本轮只校准已撤回候选的代码、测试和文档事实并完成独立 Checker。`CLR-GATE-003` 重新进入 Owner 业务前提 Gate；不连接目标数据库，不修改 Fincone，不执行 Git stage/commit/push、feature enable、发布或生产动作。

## 8. Verification Matrix / 验证矩阵

| Spec/AC | 测试/检查 | 当前状态 |
| --- | --- | --- |
| `WF-CLR-DOC-001` | 全工作树 `git diff --check`、untracked OpenSpec no-index whitespace 检查、两文件相对链接检查、状态漂移扫描和独立 Checker | `PASS`；相对链接 `checked=0 / missing=0`，Checker `P0=0 / P1=0 / P2=0`。 |
| `CORE-2E / CLR-GATE-003` | 业务对象时序、生产调用者、普通 pay 绕过路径和 transaction-face 扩展必要性评审 | `CANDIDATE_WITHDRAWN / PENDING_BUSINESS_PREMISE`；专用 API、execution-result、准入表与候选测试已删除。 |
| `D-SLC-005` 事务内 Gate | clearing confirm、settlement lock、settlement release、payout submit H2 | `EXISTING_FOUR_CONSUMERS`；初始 pay 不在已证实 Gate 消费者中。 |
| `D-SLC-006` Benefit 排除 | Benefit public contract、实现校验和 `TDD-BEN-RED-035` | `EXISTING_EVIDENCE` |
| `D-SLC-010` 原分配回链 | direct pay 返回值与 direct refund `referenceTransactionSn` 专测；Fincone 持久映射 | `EXISTING_FUNDS_EVIDENCE / FINCONE_GATE_003` |
| `SC-SLC-002` AVAILABLE 后调整 | Agent commission + adjust H2 | `H2_PASS` |
| `SC-SLC-003` SETTLEMENT 后逆向 | Agent commission H2 fail-closed 现状证据 | `H2_PASS / P1_REVERSAL_RETAINED` |
| payout 后追偿 | `RecoveryOrderApplicationServiceTests` | `EXISTING_EVIDENCE` |
| `SC-SLC-006` Profile | Owner 决策 + API baseline +迁移矩阵 | `WAVE_2_OWNER_GATE` |
| canonical capability admission | 动作矩阵、拒绝审计无账务副作用、完成重放、逆向豁免和 payout capability drift H2 | `20/20 H2 PASS / FINAL_CHECKER_PASS` |
| `D-SLC-015/016` 对账证据链与 Gate 边界 | `ReconciliationBatchApplicationServiceTests` 38、`ReconciliationRunResultApplicationServiceTests` 19、`ReconciliationDifferenceApplicationServiceTests` 30、`ReconciliationGateApplicationServiceTests` 28 | `115/115 H2 PASS`，0 failure / 0 error / 0 skipped；不含既有 DifferenceReport 8 项，也不替代来源完整性或 rail 验收。 |
| 锁定后全额释放 | 独立 release 交易 + 既有冻结单、current Gate + authority、payout 竞态、原子回滚和最终 disposition H2 | `CORE-2D 58/58 PASS / DSL COEXISTENCE 64/64 PASS / COMPILE PASS / PMD PASS / VERIFY-CAD PASS / CHECKER P0=0 P1=0 P2=0`。 |
| Fincone allocation/handoff | Fincone 契约与场景测试 | `OUTSIDE_CURRENT_WRITE_SCOPE` |

人工确认方：Product、Core API、Wallet、Ledger、Clearing、Finance/HR Owners。仓内 H2 只证明公共底座能力，不替代 Fincone 宿主集成、外部出款、税务薪酬、合规或生产验收。

## 9. Goal Ledger / 上下文账本

| Field | Current value |
| --- | --- |
| 阶段状态 | `WF-CLR-DOC-001_VERIFIED / CORE-2D-FIX-001_CLOSED / CLR-GATE-003_PENDING_BUSINESS_PREMISE` |
| Latest completed action | 能力提供者、清结算可靠性与独立架构角色完成边界复核；撤回未绑定真实清分对象且可被普通 pay 绕过的 Gate-aware/ownership 候选，保留既有标准 pay 和阶段专用资金原语。 |
| Current task | Provider 本仓执行候选清理后的 compile、聚焦回归、PMD/CAD 与独立 Checker；不宣称 CLR-GATE-003、跨仓 L3 或生产准出。 |
| Next action | Product/Finance/Fincone 宿主先冻结初始占资时点、pay 前不可变来源事实、exact Gate coverage 和不可绕过动作分类；没有新一手事实时不重开 Provider 实现。 |
| Current write set | 当前只允许本规格、TDD、用户指南以及已批准的 Provider 实现/H2 写集；不修改 DSL 迁移文件、Fincone 或目标数据库 DDL，不执行 Git、feature enable、发布或生产动作。 |
| Stop condition | 超出 G3 冻结公共契约、Profile 变化、原分配范围不确定、Gate Unknown、payout 已提交/处理中/终态、测试未实际执行或出现资金不变量失败。 |
| Residual risk | 多受益人部分退款、后续专用解冻/扣划 authority、1:N/N:1 对账、watermark/容差/SLA/close evidence、目标数据库/rail/生产并发验收、外部消费者/历史 Profile 值清单和 Fincone adapter 仍未完成生产闭环。 |
| Handoff / 恢复入口 | 从本规格 `Goal Ledger`、最新 Checker 结论和目标 Surefire 报告恢复；不得从 Agent 自述推断完成。 |

## 10. Source Anchors

- Fincone 一手材料：业务架构、资金内核、VCC 发卡履约/卡交易、全球账户与通道、订单交易，以及清结算基础 v1.1、分佣与邀请返利 v1.2 产品/系分设计；当前仍为 `REVIEW_READY / OwnerDecision=PENDING / ImplementationStatus=NOT_STARTED`，本轮未修改 Fincone。
- wind-funds 产品与测试规格：`docs/产品设计/03-清结算与对账.md`、`docs/TDD设计/支付资金底座测试驱动设计.md`。
- Gate：`reconciliation/face/.../ReconciliationGateApplicationService.java` 与 settlement/payout ApplicationService 实现。
- Benefit 排除：`transaction/face/.../FundsBenefitContributionTransactionService.java` 与对应实现。
- 调整/追偿：`FundsBalanceControlService`、`FundsBalanceAdjustRequest`、`RecoveryOrderApplicationService` 与对应 H2 测试。
- 分佣组合：`tests/src/test/java/com/wind/funds/transaction/application/flow/AgentCommissionSettlementBusinessFlowTests.java`。
