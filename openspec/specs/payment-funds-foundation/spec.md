# 支付资金底座开发基线规格

## 一、目标

本规格把当前产品设计、DSL 设计、系分设计和 TDD 设计压缩成后续编码的最小稳定基线。它不是历史变更记录，也不是任务完成证明。

后续开发必须围绕一个核心目标推进：

> 每一笔资金事实都能被解释、被核对、被重建，并能通过 TDD 证明状态、账目、route、posting、ledger entry、投影、幂等和红线均成立。

## 二、范围

### 2.1 默认核心范围

| 域 | 规格范围 |
| --- | --- |
| DSL 契约 | `FundsInstruction`、`eventType`、`transactionType`、金额临界值、引用、route、payment instrument、routing decision、funding allocation、posting、entry、`SettlementPolicy`、JSON 契约。 |
| 权益金额组件 | `FundsInstruction.benefitSnapshot`、`FundsBenefitSnapshotSpec`、权益组件、权益引用、退款处置、商户券、平台补贴、储值券、授权占券、不退券、部分退款和缺快照逆向处理。 |
| 直接交易 | 充值、付款、转账、提现、退款、手续费、退费、受控负余额。 |
| 授权交易 | 授权批准、授权拒绝、授权撤销、授权完成、授权过期、授权链退款、无授权直接退款、拒付承接口径、`settle` 强制完成模式。 |
| 余额控制 | 冻结、解冻、资金账户余额调整、信用账户额度调整和预算组额度调整；对账差错调账必须经过差错单、审批、凭证、审计和重新对账闭环，不得被简化成无来源的余额直接修改。 |
| 路由与回放 | route resolver、route snapshot、Route Replay、缺快照失败、账本周期继承。 |
| 账务与投影 | posting plan、ledger transaction、ledger entry、余额投影、余额日志、交易投影只读边界。 |
| 钱包账户 | 资金账户、信用账户、预算组、平台账户角色、支付工具、支付工具绑定、支出主体资金来源关系、主体余额查询。 |

### 2.2 独立批次范围

| 域 | 规格范围 |
| --- | --- |
| 清结算与对账 | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、对账批次、差错单、追偿单和调账核销。 |
| 归档与重放 | 归档门禁、Manifest、checkpoint、watermark、余额重建、账本余额快照覆盖模式和交易投影重放。 |
| 指标项 | 只列产品和业务关心的指标项、使用者、业务问题和建议事实来源；普通指标快照属于报表指标模块，不能替代账本余额快照确认；具体计算、存储和展示由报表指标模块承接。 |
| Spend Controls | 仅作为发卡、VCC、企业卡或员工卡授权前控制扩展；未启用发卡产品时不进入资金底座核心实现。 |

## 三、非目标

1. 不恢复历史过程规格、旧 Harness 计划或旧测试断言。
2. 不在资金底座内实现报表指标模块内部计算、调度、存储和展示。
3. 不把通道协议、卡组织规则、ACH/Nacha 规则、银行协议或合规结论写成默认实现。
4. 不把退款待处理、业务取消、未来时间事件、乱序事件等上层业务编排场景作为资金底座默认 DSL 场景。
5. 不在没有独立系分和 TDD 的情况下把清结算、对账、归档、指标实现混入交易、钱包、账本主链路。
6. 不在资金底座内实现营销规则计算、最优券选择、用户券包生命周期、活动状态查询或权益库存管理。

## 四、统一术语

| 概念 | 基线口径 |
| --- | --- |
| `transactionType` | 稳定资金业务类型：`TOPUP`、`TRANSFER`、`PAY`、`FEE`、`REFUND`、`WITHDRAW`、`ADJUSTMENT`。不得放生命周期事件。 |
| `eventType` | 生命周期事件：直接交易、授权、冻结、调账等具体事件。 |
| `SETTLEMENT_LOCK` | 结算锁定事件。当前基线不新增 `SETTLEMENT` 类 `transactionType`，可通过 `DIRECT_TRANSACTION / ADJUSTMENT` 兼容承载，但必须以 `eventType=SETTLEMENT_LOCK`、清结算上下文和结算操作类型隔离，不得归入人工调账权限、报表或核销口径。 |
| `IN_TRANSIT` | 外部已受理但未最终成功或失败的账本可见在途桶，必须保留外部引用、责任方、账龄和到期重查口径；未启用账本在途桶时，只能保持出款单待确认。 |
| 冻结动作明细 | 冻结单主表只保存聚合金额和当前状态；冻结、解冻、过期释放和后续资金事实关闭冻结来源都必须有动作明细、动作幂等键、前后剩余冻结金额和账本/资金事实引用。 |
| 出款单唯一性 | 当前基线默认一张结算单只生成一张出款单；重复创建必须命中原出款单或失败。若要支持拆分出款，必须先补出款明细和拆分幂等模型。 |
| 授权撤销 | 外部明确撤销或冲正，事件使用 `REVERSAL`，终态可区分为撤销。 |
| 授权过期 | 系统按有效期释放剩余授权，事件使用 `EXPIRE`，终态可区分为过期。 |
| 授权完成 | 使用 `AUTHORIZATION_TRANSACTION / SETTLE`，表示授权占用转实际消费或清算结果。 |
| 强制完成 | 使用 `settle` 的强制完成模式，不新增独立事件，不伪造授权占用。 |
| 无授权直接退款 | 无前置授权但存在外部原消费、外部原完成或差错凭证时，使用 `settleRefund` 的无授权退款模式承接；不得补造内部授权占用或静默退款。 |
| 授权链拒付 | 表达已完成授权后的争议、扣回或追偿语义；资金底座目标态不要求落到 `FundsAuthorizationTransactionService#chargeback`，默认通过 `settleRefund` 携带拒付原因、凭证、上下文和审计承接。即使底层终态复用退款终态，也必须保留可查询、可投影、可审计的拒付语义，不能只留下普通退款结果。 |
| 支付工具 | 卡、VA、外部账户、虚拟卡、钱包标识或通道 token 的路由输入和审计快照；不表达内部余额，不作为账本主体。 |
| 支付工具绑定 | 工具和付款主体、收款主体、信用账户、预算组或真实资金账户之间的候选关系；只用于路由候选和快照，不直接入账。 |
| 支出主体资金来源关系 | 支出主体到资金账户、信用账户、预算组或兜底资金来源的解析关系；不计算 spend rules，不执行扣款，不写分录。 |
| 权益快照 | `FundsBenefitSnapshotSpec`，只承接业务侧、订单侧或营销权益系统已决策的权益结果；不计算券规则，不判断券是否可用，不维护券生命周期。 |
| 权益金额组件 | 单个商户让利、平台补贴、代金券核销、储值券抵扣或合作方补贴金额项；必须标记账务效果、资金性质、承担方、受益方和退款处置。 |
| `NO_LEDGER` 权益 | 商户让利、展示优惠等无独立资金流组件，只能进入快照、清分展示、对账解释或投影辅助，不得生成 route leg、posting 或 ledger entry。 |
| 平台补贴 / 储值代金券 | 平台补贴可能形成平台自有资金支出；储值、预付或礼品卡代金券可能涉及负债、预收待付或用户权益余额，必须有专业口径确认后再入账。 |
| 不退券 | 用户侧处置和资金侧处置必须分开表达：用户可以不返券，资金侧可以冲回补贴、保留补贴、减少商户应收或恢复储值负债。 |
| 账本周期 | `periodType + periodId`，是余额 bucket 隔离键，不是清算账期、结算周期、报表周期、归档水位或 spend-rule window。 |
| 余额日志 | 从分录和余额投影派生的观察记录，不是余额事实源，不得用于修复余额。 |
| 广义指标快照 | 对某个范围、口径、时间边界和事实来源进行批量计算、校验和确认的任务形态。普通指标快照归报表指标模块；账本余额快照是特殊场景，确认的是账本 bucket 余额事实。 |
| 账本余额快照覆盖模式 | `HOT_ONLY` 表示只覆盖热区分录；`COLD_MANIFEST` 表示只覆盖已归档冷区；`MIXED` 表示同时覆盖冷区 Manifest 和热区游标。冷区和混合覆盖缺 Manifest 不得进入 `VERIFIED`。 |
| 普通指标快照 | 报表指标模块的发布和质量上下文，只影响指标查询、看板、导出或订阅；不得推进余额水位、修改归档 Manifest、替代交易投影重放 checkpoint 或证明余额正确。 |

## 五、当前代码差距基线

以下差距用于指导后续 TDD 批次，不在本规格中直接修改代码。

### 5.1 代码能力对齐

| 批次域 | 当前代码和测试基线 | 对齐结论 |
| --- | --- | --- |
| 批次 1 DSL 契约 | 已存在 `FundsInstructionDslContractTests`、`RouteDslContractTests`、`PaymentInstrumentRouteDslContractTests`、`PostingLedgerDslContractTests`、`SettlementPolicySpecTests`、`FundsAmountBoundaryContractTests`、`FundsDslJsonContractTests`。 | DSL、Route、PaymentInstrument Route、Posting/Ledger、金额临界值和 JSON 契约已有测试基线；仍需按后续批次发现的公共契约缺口补 Red 用例。 |
| 权益快照 DSL | 当前代码未见 `FundsBenefitSnapshotSpec`、`FundsBenefitComponentSpec`、`benefitSnapshot` 字段或 `FundsBenefit*` 枚举；当前为设计和 TDD 目标。 | 不能声明权益快照已实现；Phase 1 需先补 core 契约测试、JSON 夹具和兼容字段，Phase 2 才能消费 route/posting/replay。 |
| 批次 2 钱包账户与账本基础 | 已存在 `FundingAccountServiceImplTests`、`ControlAccountLedgerInitializationTests`、`PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests`、`LedgerBalanceProjectionServiceImplTests`。 | 支付工具、绑定历史、资金来源关系、显式账本初始化和投影 afterCommit 已有局部服务层基线；账本周期、全量余额断言和组合交易仍需后续批次继续闭合。 |
| 批次 3 直接交易 | 已存在 `FundsDirectTransactionFlowTests`、`FundsTransferPayWithdrawChainFlowTests`、`FundsTransactionFeeFlowTests`。 | 直接交易主链路、退款、手续费和链式流程已有局部流程基线；仍需按 TDD 覆盖全部 `AC-IN/OUT/PAY/MER/FEE` 和红线失败。 |
| 批次 4 授权交易 | 已存在 `FundsAuthorizationTransactionFlowTests`，覆盖授权拒绝、撤销、部分完成、全额完成、退款超额等局部场景。 | 授权批准、拒绝、撤销、完成和完成后退款已有局部基线；授权过期、强制完成、无授权直接退款、拒付承接口径、原路径回放、发卡控制扩展边界和并发竞争仍需按批次 4 覆盖索引闭合。 |
| 批次 5 余额控制 | 已存在 `FundsBalanceControlFailureFlowTests`、`FundsWithdrawalSuccessFlowTests`、`FundsWithdrawalAfterPartialUnfreezeFlowTests`、`FundsWithdrawalRejectionFlowTests`。 | 冻结、解冻、提现、失败无副作用和部分组合路径已有局部基线；资金账户余额调整、信用账户额度调整、预算组额度调整、adjust 红线、冻结关闭并发和全量金额临界值仍需补齐。 |
| 批次 6 Route Replay 与投影 | 已存在 `DefaultRouteReplayServiceTests`、`CompositeRouteResolverTests`、`DefaultRoutedFundsInstructionOrchestratorProjectionTests`。 | Route Replay、resolver 无副作用、交易投影 afterCommit 和投影失败不回滚事实已有局部基线；支付工具换绑后全链路回放、敏感快照、绑定历史审计、余额日志边界和投影重放全量覆盖仍需按批次 6 覆盖索引闭合。 |
| 批次 7 清结算与对账 | 仅保留 `reconciliation-face`、`reconciliation-impl` 空模块骨架。 | 不能声明清结算和对账已实现；进入编码前必须另起独立 OpenSpec change 和 Execution Grant。 |
| 批次 8 归档重放与指标边界 | 已存在 `governance-face`、`governance-impl` 交易投影重放骨架和 `FundsProjectionReplayServiceTests`。 | 交易投影重放有局部边界基线；归档 Manifest、账本余额快照覆盖模式、余额水位隔离、普通指标快照和指标水位隔离仍需独立落地。 |

### 5.2 差距清单

| 编号 | 设计基线 | 当前代码观察 | 后续落地要求 |
| --- | --- | --- | --- |
| GAP-AUTH-001 | 授权过期必须有 `EXPIRE` 事件和 `expire` 服务入口。 | 授权服务当前只有 `authorize/reversal/settle/settleRefund/chargeback`，事件枚举未见 `EXPIRE`。 | 先补 TDD，再补枚举、请求、服务、转换、路由/回放、生命周期、账务和查询断言。 |
| GAP-AUTH-002 | `settle` 支持强制完成模式，必须有策略、原因、上限和审计。 | `FundsAuthorizationTransactionSettleRequest` 当前以原授权交易号为必填，未表达强制完成策略字段。 | 先补强制完成失败测试，再设计请求契约兼容方案。 |
| GAP-AUTH-003 | 拒付只作为已完成授权后的争议、扣回或追偿语义，不等同于授权拒绝。 | 代码已有 `FundsAuthorizationTransactionService#chargeback` 和 `CHARGEBACK` eventType，请求契约也有 `FundsAuthorizationTransactionChargebackRequest`；当前目标态仍要求保留拒付原因、凭证、上下文、外部引用和审计，不能只留下普通退款结果。 | 后续 TDD 只验证拒付与授权拒绝可区分、拒付原因/凭证/审计可追溯、退款/扣回金额不超过已完成金额；是否继续保留或收敛 `chargeback` 入口需在批次 4 Execution Grant 中明确。 |
| GAP-AUTH-004 | `settleRefund` 无授权退款模式必须可表达外部原消费、外部原完成或差错凭证。 | `FundsAuthorizationTransactionRefundRequest` 当前仍要求 `authorizationTransactionSn`，尚不能表达无前置授权退款。 | 先补无授权直接退款成功、无原事实失败、审计缺失失败测试；必要时扩展请求契约并保持兼容。 |
| GAP-PI-001 | 支付工具、绑定关系、资金来源关系和绑定历史审计必须能支撑 route snapshot、原路径回放和敏感信息治理。 | 当前已有 `PaymentInstrumentService`、绑定当前态、绑定历史审计、资金来源关系、工具方向/状态守卫和脱敏快照相关实现与服务层测试。 | 批次 2 的支付工具基础能力可作为局部基线；后续仍需补支付工具换绑后资金全链路 replay、route snapshot 引用和组合交易断言。 |
| GAP-BEN-001 | `FundsInstruction` 可携带权益快照，且核心字段不能藏入 `contextVariables`。 | 当前代码未见权益快照模型、字段或枚举。 | 批次 1 先补 `FundsBenefitSnapshotSpec`、组件、引用、退款策略和 JSON 契约测试；无权益交易必须兼容。 |
| GAP-BEN-002 | 直接交易需按原权益快照处理商户券、平台补贴、储值券和叠加权益。 | 当前 route/posting 不消费权益金额组件。 | 批次 3 补商户让利 no-ledger、平台补贴独立资金影响、储值券资金性质和退款分摊测试。 |
| GAP-BEN-003 | 授权交易需表达占券、完成核销、撤销或过期释放，以及拒绝不处理权益。 | 当前授权链路未见权益占用、核销或释放语义。 | 批次 4 补授权占券、完成核销、撤销/过期释放、拒绝无副作用和并发竞争测试。 |
| GAP-BEN-004 | 退款、撤销、过期、拒付和 Route Replay 必须使用原权益快照，不得按当前营销规则重算。 | 当前 replay 缺权益快照摘要、组件累计和缺快照人工处理边界。 | 批次 6 补原快照 digest、缺快照失败、部分退款累计闭合、不退券和补贴冲回/保留分离测试。 |
| GAP-BEN-005 | 含权益交易进入清结算与对账时，营销核销、订单金额、资金入账、商户应收和补贴冲回必须可拆分核对。 | 清结算与对账仍为空模块骨架。 | 批次 7 独立 OpenSpec change 中补权益清分、清算、结算、对账差错和人工处理矩阵。 |
| GAP-TDD-001 | 测试必须按最终 TDD 重建。 | 历史过渡测试已作废；当前已有批次 1、批次 2 和批次 3 至 6 的局部目标态测试，但批次 1 至批次 6 尚未按覆盖索引完整闭环。 | 继续按批次补齐测试，不恢复已废弃断言；已重建测试只作为对应批次的局部基线。 |
| GAP-CLS-001 | 清结算与对账对象需独立状态机和表设计落地。 | 当前代码仅保留 `reconciliation-face` 和 `reconciliation-impl` 空模块骨架，未保留对账任务 DTO、Request、Service 或 Impl 代码；尚未落可清分明细、清分批次、清算候选、清算批次、结算单、出款单、差错单、追偿单、表结构和目标态服务层测试。 | 单独 OpenSpec change 和 Harness 批次处理，不混入交易主链路；后续确认是否复用现有 `reconciliation-*` 模块作为落点。 |
| GAP-ARCH-001 | 归档、重放、普通指标快照和账本余额快照边界需按 04 系分独立落地。 | 当前代码已有 `governance-face` 和 `governance-impl` 交易投影重放骨架，已覆盖有界范围、交易投影 checkpoint 和 verify-only 不写投影等局部边界；尚未落归档 Manifest、余额快照覆盖模式、余额水位隔离、普通指标快照边界和指标水位隔离。 | 批次 8 另起独立 OpenSpec change；归档/重放只做事实治理；账本余额快照按 `HOT_ONLY`、`COLD_MANIFEST`、`MIXED` 校验；普通指标快照只输出报表指标模块输入和边界测试。 |

## 六、TDD 落地顺序

当前执行优先级按设计文档域排列：先做 `02-交易路由钱包账目与投影`，再做 `03-清结算与对账`，最后做 `04-归档重放与指标治理`。下表的顺序 1 至 6 是 02 阶段的内部 TDD 子批次，必须整体优先于 03 和 04。

| 顺序 | 批次 | 目标 |
| --- | --- | --- |
| 0 | 基线重置 | 作废旧规格和旧测试；确认 docs、OpenSpec、resources 和代码差距。 |
| 1 | 02 内部：DSL 契约与枚举红线 | 重建 `FundsInstruction`、event、transactionType、金额临界值、route、payment instrument、routing decision、funding allocation、posting、`SettlementPolicy`、权益快照、JSON 契约测试。 |
| 2 | 02 内部：钱包账户与账本基础 | 重建账户、账本初始化、平台账户角色、支付工具、绑定关系、支出主体资金来源关系、账本周期、posting 平衡和余额投影测试。 |
| 3 | 02 内部：直接交易 | 重建充值、付款、转账、提现、退款、手续费、退费、受控负余额，以及商户券、平台补贴、储值券和不退券直接交易测试。 |
| 4 | 02 内部：授权交易 | 重建授权、拒绝、撤销、完成、过期、强制完成、完成后退款、无授权直接退款、拒付承接口径、授权占券和 VCC 扩展边界测试。 |
| 5 | 02 内部：余额控制 | 重建冻结、解冻、资金账户余额调整、信用账户额度、预算组额度、冻结提现组合测试，以及 adjust 绕过差错闭环的红线测试。 |
| 6 | 02 内部：Route Replay 与投影 | 重建 Route Replay、支付工具换绑后原路径回放、缺快照失败、权益快照回放、交易投影只读、余额日志边界测试。 |
| 7 | 03-清结算与对账 | 按独立系分重建可清分准入、清分批次、清算候选、清算批次、结算单、出款单、对账差错、权益拆分、退款时序和追偿测试。 |
| 8 | 04-归档重放与指标边界 | 重建归档门禁、Manifest、水位、余额重建、账本余额快照覆盖模式、交易投影重放、指标只读、指标水位隔离和普通指标快照并发边界测试。 |

## 七、Superpowers 纪律

1. 每个批次先写失败测试或契约测试，再做最小实现。
2. 每个资金变化场景必须断言状态、route snapshot、posting plan、ledger entry、余额投影和幂等。
3. 红线测试必须证明失败无副作用。
4. 不用旧测试通过证明新设计通过。
5. 发现产品、DSL、系分、TDD 不一致时，先补设计，再继续编码。
6. 重构只能在测试保护下发生，不改变外部可观察行为。
7. 设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果必须先形成独立交付检查点；未冻结前不得进入具体生产代码实现。
8. 所有资金变化测试必须覆盖相关金额临界值；同一资金对象存在多事件推进时，必须评估并发竞争用例，不能只用顺序执行证明安全。

## 八、Harness 门禁

每个开发批次必须声明：

| 项 | 要求 |
| --- | --- |
| 写入范围 | 明确模块、包、文件或测试目录。 |
| 只读范围 | 明确可参考的 docs、OpenSpec、源码和 resources。 |
| 禁止事项 | 不越界修改生产代码、不引入无主依赖、不恢复旧测试。 |
| 验证命令 | 至少包含 `just mvn-version`、`just compile` 和本批次相关测试命令；无法运行需说明。 |
| 人工确认点 | 公共契约、表结构、枚举、资金红线、清结算对象、归档重放、外部规则。 |

## 九、必须失败红线

红线以 `docs/TDD设计/支付资金底座测试驱动设计.md` 第十四章为准，后续 OpenSpec change 不得删减红线，只能增加更具体的场景。
