# TDD 基线重置 Harness Plan

## 0. 当前状态

- [x] 作废旧 OpenSpec specs。
- [x] 作废旧 OpenSpec changes。
- [x] 删除旧测试源码。
- [x] 保留测试 resources。
- [x] 重建 OpenSpec 项目上下文。
- [x] 重建支付资金底座开发基线规格。
- [x] 校准 TDD 文档中“旧测试资产复用”表述。
- [x] 冻结当前设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果，作为进入编码前的独立检查点；最新设计交付提交点以包含本 Harness Plan 的最新 docs 提交为准，上一冻结点为 620b5a5 docs: 精简并加固资金底座设计交付文档。
- [x] 完成设计、代码、任务三方基线对齐：设计以 `docs/` 和最新提交点为准，代码能力以当前 HEAD 复核结果为准，任务以本 Harness Plan 和 OpenSpec spec 为准。

## 1. 全局写入范围

后续开发批次允许按批次写入：

当前执行优先级按设计文档域排列：先完成 `02-交易路由钱包账目与投影` 对应的批次 1 至批次 6，再进入 `03-清结算与对账` 对应的批次 7，最后进入 `04-归档重放与指标治理` 对应的批次 8。批次编号仍保留为 TDD 内部拆解编号，不表示 03、04 可以在 02 完成前抢跑。

| 批次 | 写入范围 |
| --- | --- |
| 批次 1 | `core/src/test/java`、必要的 `core/src/main/java` DSL/枚举/Spec、Route DSL、PaymentInstrument DSL、RoutingDecision/FundingAllocation DSL、Posting/Ledger DSL、SettlementPolicy。 |
| 批次 2 | `wallet-*`、`ledger-*`、`tests/src/test/java` 中账户、支付工具、绑定关系、支出主体资金来源关系、账本、投影相关测试和最小实现。 |
| 批次 3 | `transaction-*`、`tests/src/test/java` 中直接交易测试和最小实现。 |
| 批次 4 | `transaction-*`、`core`、`tests/src/test/java` 中授权交易、无授权直接退款测试和最小实现。 |
| 批次 5 | `transaction-*`、`tests/src/test/java` 中余额控制测试和最小实现。 |
| 批次 6 | `transaction-*`、`ledger-*`、`tests/src/test/java` 中 Route Replay、余额日志、交易投影测试和最小实现。 |
| 批次 7 | 清结算、对账相关模块或包，需先确认；当前仅保留 `reconciliation-*` 空模块骨架，目标态对象、表、状态机和测试未闭环。 |
| 批次 8 | 归档、余额重建、账本余额快照、交易投影重放相关模块或包，需先确认；当前已有 `governance-*` 交易投影重放骨架，但归档、余额快照和指标水位隔离未闭环；指标仅保留普通指标快照边界测试。 |

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

## 4. 每批基础验证命令

后续每个编码批次默认先执行基础验证，再执行任务表中的专项验证命令：

```bash
just mvn-version
just compile
```

如因 Java runtime、私有 Maven 仓库、网络、凭据或本地缓存导致无法执行，交付记录必须区分环境问题与代码问题。

## 5. 基线对齐结论

- [x] OpenSpec 已重新对齐：以 `project.md` 和 `payment-funds-foundation/spec.md` 作为当前目标态规格，不再引用历史 specs 或历史 changes。
- [x] Superpowers 已重新对齐：以 TDD、增量、可回退、先写用例再改实现为执行纪律，不复用旧测试源码和旧断言。
- [x] Harness 已重新对齐：以本文件作为批次计划、写入范围、只读范围、禁止事项、人工确认点和交付记录入口。
- [x] 旧测试源码已移除，测试 resources 保留。
- [x] 当前阶段为“任务拆解与准入规划”，尚未进入 CAD 自动提交模式；进入编码前仍需用户按批次授予 Execution Grant。
- [x] 当前工作树的设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果已作为独立检查点冻结；最新设计交付提交点以包含本 Harness Plan 的最新 docs 提交为准，上一冻结点为 620b5a5 docs: 精简并加固资金底座设计交付文档。后续编码仍需用户按批次单独授予 Execution Grant。
- [x] 代码基线已复核至 7b4fe22 fix: 拦截直接付款外部收款主体：批次 1 DSL 契约测试已存在；批次 2 支付工具、绑定历史、资金来源关系、显式建账和余额投影已有局部服务层基线；批次 3 至 6 已有部分直接交易、授权、余额控制、Route Replay 和交易投影测试；批次 7 仅保留 reconciliation-* 空模块骨架；批次 8 已有 governance-* 交易投影重放骨架。上述都只作为局部代码基线，不表示对应批次全量完成或可跳过 Execution Grant。

### 5.1 设计、代码、任务对齐矩阵

| 设计域 | 代码现状 | 任务基线 |
| --- | --- | --- |
| `02-交易路由钱包账目与投影` | DSL 契约、支付工具、钱包账户、部分直接交易、授权、余额控制、Route Replay、余额投影和交易投影已有局部测试与实现。 | 继续按批次 1 至批次 6 补齐覆盖索引；已存在能力只作为局部基线，仍需按 TDD 证明全量 AC/DSL/RED。 |
| `03-清结算与对账` | `reconciliation-face`、`reconciliation-impl` 仅为空模块骨架。 | 批次 7 进入编码前必须另起独立 OpenSpec change，并确认模块、表、状态机、接口和验证命令。 |
| `04-归档重放与指标治理` | `governance-face`、`governance-impl` 已有交易投影重放骨架和局部边界测试。 | 批次 8 不抢跑批次 7；归档 Manifest、账本余额快照、普通指标快照和水位隔离仍需独立 Execution Grant。 |
| 导出附件 | 若工作树存在 `docs/*.zip` 等导出包，只能作为评审附件。 | 导出包不作为规格、任务或验收 Source of Truth；是否纳入版本库需用户单独确认。 |

## 6. 里程碑拆解

| 里程碑 | 目标 | 完成标志 | 阻塞关系 |
| --- | --- | --- | --- |
| M0 基线重置 | 作废历史基线、删除旧测试源码、保留 resources、重建 OpenSpec/Harness。 | 本文件第 0 节全部完成。 | 已完成。 |
| M1 02-交易路由钱包账目与投影 | 先把交易、路由、钱包、账目、余额投影和交易投影主链路打穿。内部顺序为批次 1 至批次 6：DSL/core、钱包账户与账本、直接交易、授权交易、余额控制、Route Replay 与投影。 | 批次 1 至批次 6 通过，资金主链路具备真实服务层测试、余额断言、账务平衡、投影 afterCommit 和失败隔离。 | 阻塞 M2、M3；03、04 不在 02 完成前抢跑。 |
| M2 03-清结算与对账 | 在 02 主链路稳定后，按独立系分落清分、清算、结算、出款、对账、差错和追偿对象。 | 批次 7 通过，且对象、状态机、表和测试不污染交易主链路。 | 依赖 M1；进入编码前需独立 OpenSpec change 和模块确认。 |
| M3 04-归档重放与指标治理 | 在 02 主链路和 03 独立对象边界明确后，落归档、重放、账本余额快照、普通指标快照边界和水位隔离。 | 批次 8 通过，归档 Manifest、余额水位、交易投影 checkpoint 和指标水位互不复用。 | 依赖 M1、M2；除非用户再次调整，否则排在 03 之后。 |

## 7. 详细任务计划

### 批次 1：DSL 契约与枚举红线

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B1-01 | 重建核心测试骨架和命名规范。 | `core/src/test/java` | TDD 设计、DSL 设计。 | 测试目录为空时先建立最小失败样例。 | 建立按 DSL/枚举/JSON 分层的测试包，不迁回旧测试源码。 | `just test-core` |
| B1-02 | 固化 `FundsInstructionSpec` 语义。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL 语义结构、JSON 契约。 | 构造最小合法指令、缺字段、非法金额、非法主体用例。 | 补齐或校准 Spec 校验行为。 | `just test-core` |
| B1-03 | 证明 `transactionType` 不承载生命周期事件。 | `core/src/test/java`、必要枚举。 | DSL 第九章、第十章。 | 将授权生命周期事件误塞入交易类型应失败。 | 保持交易类型与事件类型分离。 | `just test-core` |
| B1-04 | 固化授权 `eventType` 生命周期边界。 | `core/src/test/java`、必要枚举。 | DSL 授权交易用例。 | `PENDING`、`SETTLE`、`REVERSAL`、`REFUND`、`EXPIRE` 的合法/非法组合，以及拒付不强制落独立 `CHARGEBACK` 事件。 | 必要时补 `EXPIRE` 枚举或先形成失败基线；拒付通过退款承接口径表达。 | `just test-core` |
| B1-05 | 重建 JSON 契约样例解析测试。 | `core/src/test/java`、`core/src/test/resources` | DSL JSON 契约用例。 | JSON 样例无法解析、字段含义不一致、枚举不匹配。 | 让 DSL 样例能推导业务流程和 TDD 断言。 | `just test-core` |
| B1-06 | 固化 Route DSL 和 Route Replay 类型契约。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL Route、Route Replay、不变量。 | 外部账户或工具进入 ledger node、缺快照 Route Replay 仍成功、route code 漂移。 | 补 `RouteDslContractTests` 或等价契约测试，明确 Route Replay 不等同于交易投影重放、余额重建或归档续跑。 | `just test-core` |
| B1-06A | 固化 PaymentInstrument Route DSL 契约。 | `core/src/test/java`、必要的 `core/src/main/java` | `DSL-PAYMENT-INSTRUMENT-ROUTE-001`、`DSL-PAYMENT-INSTRUMENT-FAIL-001`、`DSL-PAYMENT-INSTRUMENT-REPLAY-001`。 | 支付工具、外部账户或通道 token 被写成 ledger subject；routing decision 缺资金来源、优先级或选择原因；敏感字段进入快照；工具换绑后按当前绑定重路由。 | 补 `PaymentInstrumentRouteDslContractTests` 或等价契约测试，固化 `PaymentInstrumentRef`、`ExternalAccountRef`、`RoutingDecision`、`FundingAllocationDecision` 和 route snapshot 不变量。 | `just test-core` |
| B1-07 | 固化 Posting/Ledger DSL 契约。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL Posting、LedgerEntry、账务不变量。 | posting plan 不平衡仍可构造、entry 缺主体/账目/币种/周期仍通过。 | 补 posting、entry、digest 和账本周期契约测试。 | `just test-core` |
| B1-08 | 固化 SettlementPolicy DSL 契约。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL SettlementPolicy、结算策略边界。 | 策略解析失败被静默按实时处理。 | 补 `SettlementPolicySpecTests` 或等价策略解析红线。 | `just test-core` |
| B1-09 | 固化金额临界值契约。 | `core/src/test/java`、必要的 `core/src/main/java` | TDD 5.1 金额临界值通用矩阵。 | 0 金额、负金额、超币种精度、超系统上限或多 leg 合计不闭合仍可入账。 | 补 `FundsAmountBoundaryContractTests` 或等价契约测试，统一最小单位、精度、上限和累计闭合校验。 | `just test-core` |

### 批次 2：钱包账户、账本和余额投影基础

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B2-01 | 重建账户角色与建账规则。 | `wallet-*`、`ledger-*`、`tests/src/test/java` | 产品账户/钱包/账本概念、系分表设计。 | 资金账户、信用账户、预算组、平台账户角色缺失或混用。 | 校准账户创建、角色标识、缺账本失败行为。 | `just test-ledger` |
| B2-02 | 固化显式建账红线。 | `ledger-*`、`tests/src/test/java` | 账本与账本周期设计。 | 未建账本直接交易应失败。 | 保持建账动作可追溯，不隐式创建关键账本。 | `just test-ledger` |
| B2-03 | 固化账本周期和 posting 平衡。 | `ledger-*`、`tests/src/test/java` | 账本周期语义矩阵、账务规则矩阵。 | 周期混用、借贷不平、entry 字段缺失。 | 补齐周期隔离、posting plan 平衡和分录完整性。 | `just test-ledger` |
| B2-04 | 固化余额投影边界。 | `ledger-*`、`tests/src/test/java` | 余额投影、余额日志。 | 投影失败误回滚事实、余额日志被当事实源。 | 保持事实追加、投影可重建、日志只读辅助。 | `just test-boundary` |
| B2-05 | 建立余额断言支撑。 | `tests/src/test/java` | TDD 资金断言红线。 | 只断言状态、不断言余额桶。 | 补公共断言支撑，后续批次复用。 | `just test-boundary` |
| B2-06 | 固化支付工具、绑定和资金来源关系。 | `wallet-*`、`tests/src/test/java` | 产品 `AC-PI-*`、系分支付工具状态与路由准入、`GAP-PI-001`。 | 工具状态不可用、方向不匹配、资金来源缺失或不唯一、账户能力不足仍进入 route；敏感信息未脱敏；绑定历史被覆盖。 | 补 `PaymentInstrumentServiceImplTests`、`SpendSubjectFundingRelationServiceImplTests` 或等价测试，证明工具只做引用、绑定只做候选、资金来源关系不扣款不写账；绑定当前态变更必须追加历史审计。 | `just test-boundary` |

### 批次 3：直接交易

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B3-01 | 重建直接交易指令转换测试。 | `transaction-*`、`tests/src/test/java` | DSL 直接交易、系分交易编排。 | 充值、付款、商户订单付款、转账、提现转换错误。 | 校准 converter、amount、subject、route 快照。 | `just test-transaction` |
| B3-02 | 固化主路径资金流。 | `transaction-*`、`tests/src/test/java` | 产品流程、账务规则矩阵。 | 资金流方向、账本余额桶、平台账户处理错误。 | 逐步骤断言交易、route、entry、余额。 | `just test-business-flow` |
| B3-03 | 固化退款、手续费、退费和原路径 replay。 | `transaction-*`、`tests/src/test/java` | DSL 场景账务规则矩阵。 | 退款超额、退费方向错误、replay 丢快照。 | 保持原路径、原费项和幂等 replay。 | `just test-transaction` |
| B3-04 | 固化异常与红线。 | `transaction-*`、`tests/src/test/java` | TDD 异常路径。 | 错币种、受控负余额、重复请求、缺主体。 | 补失败原因、幂等键和余额不变断言。 | `just test-business-flow` |

### 批次 4：授权交易

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B4-01 | 重建授权批准与授权拒绝。 | `transaction-*`、`core`、`tests/src/test/java` | DSL 9.2 授权交易、VCC 场景。 | 授权批准不冻结、授权拒绝写 route/entry。 | 保持批准冻结、拒绝仅记录原因且无资金扣划。 | `just test-transaction` |
| B4-02 | 重建撤销、完成、退款和拒付承接。 | `transaction-*`、`tests/src/test/java` | 授权组合场景、账务矩阵。 | 撤销后错误完成、完成后错误释放、多次清算/退款累计错误、拒付被当成授权拒绝或普通退款。 | 校准状态机、授权释放、实际扣款、退款/拒付承接和累计金额闭合；即使拒付底层复用退款终态，也必须保留拒付原因、凭证、外部引用、审计上下文和投影可区分性。 | `just test-business-flow` |
| B4-03 | 补授权过期独立语义。 | `transaction-*`、`core`、`tests/src/test/java` | `GAP-AUTH-001`、授权过期用例。 | `EXPIRE` 与 `REVERSAL` 混用，过期释放已完成金额。 | 人工确认后补 `EXPIRE` 事件和 `expire` 服务入口；只释放剩余 `AUTHORIZATION`。 | `just test-transaction` |
| B4-04 | 校准 `settle` 强制完成模式。 | `transaction-*`、`tests/src/test/java` | `GAP-AUTH-002`、force settle 设计。 | 无前置授权完成缺少策略、原因、上限或审计。 | 人工确认后扩展 settle 请求契约和策略校验。 | `just test-business-flow` |
| B4-05 | 校准 `settleRefund` 无授权退款模式。 | `transaction-*`、`tests/src/test/java`，必要时涉及请求契约 | `GAP-AUTH-004`、`AC-AUTH-012`、`TDD-AUTH-006`。 | 无前置授权但有外部原消费、原完成或差错凭证时无法退款；无原事实或缺审计仍静默退款；实现为了退款补造授权占用。 | 人工确认后补充或校准 `settleRefund` 引用、原因、凭证和审计字段；不得创建内部授权占用；查询、投影和审计能说明无授权退款来源。 | `just test-business-flow` |
| B4-06 | 固化 VCC / Spend Controls 扩展边界。 | `transaction-*`、`tests/src/test/java` | Spend Controls 为扩展能力。 | 将 spend controls 当成资金主链路 P0。 | 默认只保留边界测试；未明确启用发卡产品时不做实现。 | `just test-boundary` |
| B4-07 | 明确 chargeback 不落 `FundsAuthorizationTransactionService#chargeback`。 | `docs`、`openspec`、后续授权测试 | 用户补充口径、拒付业务事实。 | 测试或任务强制要求调用 `chargeback` 服务入口，或把拒付结果只保存成无法区分的普通退款。 | 拒付作为争议/扣回语义，通过 `settleRefund` 的原因、上下文、凭证和审计承接；如代码已有 `chargeback`，本轮不扩展为目标态主入口；查询、投影和审计必须能区分普通退款与拒付承接。 | `just test-transaction` |
| B4-08 | 固化授权并发竞争红线。 | `transaction-*`、`core`、`tests/src/test/java` | TDD 13.5、授权生命周期。 | 同一授权的完成、撤销、过期、退款并发导致重复入账、重复释放或剩余为负。 | 通过幂等键、状态版本、唯一约束或锁定策略确保同一授权同一时刻只有合法迁移生效，失败方无副作用。 | `just test-business-flow` |

人工确认点：`EXPIRE` 枚举、`expire` 服务入口、`settle` 强制完成请求契约、`settleRefund` 无授权退款引用和审计字段；VCC / Spend Controls 默认只做边界测试，只有明确启用发卡产品才进入实现范围。

### 批次 5：余额控制

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B5-01 | 固化冻结/解冻控制语义。 | `transaction-*`、`tests/src/test/java` | 余额控制 DSL、资金红线。 | 冻结表达消费、跨主体冻结、多次解冻超额。 | 保持同主体 `AVAILABLE <-> FROZEN` 控制。 | `just test-balance-control` |
| B5-02 | 固化冻结后提现与组合路径。 | `transaction-*`、`tests/src/test/java` | 产品验收用例。 | 提现忽略冻结、冻结释放后余额桶错误。 | 逐步骤断言可用、冻结、实际扣划。 | `just test-balance-control` |
| B5-03 | 固化资金调账、信用额度、预算额度。 | `transaction-*`、`tests/src/test/java` | 账户/额度/预算概念边界。 | 调账绕过凭证，额度与余额混用。 | 保持调账凭证、额度口径和预算控制分离。 | `just test-balance-control` |
| B5-04 | 固化 FX 与余额控制边界。 | `transaction-*`、`tests/src/test/java` | TDD 红线。 | 余额控制承接 FX 或隐式换汇。 | 余额控制只处理同币种余额桶。 | `just test-boundary` |
| B5-05 | 固化冻结、解冻和提现并发竞争红线。 | `transaction-*`、`tests/src/test/java` | TDD 13.5、余额控制组合场景、冻结动作明细。 | 同一冻结来源被解冻和提现确认并发关闭，导致重复释放、重复关闭或 `FROZEN` 为负。 | 通过冻结动作明细、剩余金额校验、动作幂等键和唯一约束保证同一冻结来源只被关闭或释放一次。 | `just test-balance-control` |

### 批次 6：Route Replay、交易投影和余额日志

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B6-01 | 固化 route resolver 选择和无副作用。 | `transaction-*`、`tests/src/test/java` | 路由系分、路由能力边界。 | `supports` 写事实或改变状态。 | 保持 resolver 只判定路径，不写交易/账本事实。 | `just test-boundary` |
| B6-02 | 固化缺快照 Route Replay 失败。 | `transaction-*`、`tests/src/test/java` | Route Replay 设计。 | 缺 route/amount/subject 快照仍按当前绑定重放成功。 | Route Replay 必须依赖原始快照并输出明确失败原因；不得退化为重新路由。 | `just test-business-flow` |
| B6-03 | 固化交易投影只读和有界重放。 | `transaction-*`、`tests/src/test/java` | 交易投影重放。 | 无范围重放、投影水位复用余额水位或归档 checkpoint。 | 引入范围、模式、checkpoint 和差异报告红线；交易投影重放不重新入账、不改余额。 | `just test-boundary` |
| B6-04 | 固化余额日志边界。 | `ledger-*`、`tests/src/test/java` | 余额日志设计。 | 余额日志失败导致事实回滚，或被用于修复余额。 | 保持日志为观测与审计辅助，不作事实源。 | `just test-boundary` |
| B6-05 | 固化支付工具换绑后原路径回放。 | `transaction-*`、`tests/src/test/java` | `DSL-PAYMENT-INSTRUMENT-REPLAY-001`、TDD 支付工具回放红线。 | 原交易后工具换绑、解绑、暂停或默认资金来源变化，退款、撤销、退费或拒付按当前绑定重新选路。 | Route Replay 必须使用原 route snapshot、原工具快照和原 funding allocation；当前绑定只作为审计上下文，不参与资金路径。 | `just test-business-flow` |

### 批次 7：清结算与对账

批次 7 在本 change 中只保留边界、计划和准入门禁。当前代码仅保留 `reconciliation-face` 和 `reconciliation-impl` 空模块骨架，可作为后续候选落点；进入编码前仍必须另起独立 OpenSpec change，确认模块命名、表设计、状态机、接口和验证命令。

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B7-01 | 确认独立模块、表和状态机。 | 待确认的 `reconciliation-*` 扩展或独立包 | 系分 03、产品清分/清算/对账。 | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、对账批次、差错单或追偿单混成一个对象。 | 人工确认后建立独立对象边界。 | 待确认 |
| B7-02 | 固化可清分准入、清分批次和清算候选排除。 | 待确认 | 可清分规则、清分批次、清算候选。 | 未完成交易入可清分明细、清分批次确认即入账、冻结或重大差错交易入清算候选。 | 可清分只来自成功交易、已过账 `CLEARING` 分录和完整来源快照；清分批次只固化归类和规则版本；候选只来自已确认清分结果且满足可清算规则。 | 待确认 |
| B7-03 | 固化清算批次确认、结算锁定、出款成功/失败和追偿。 | 待确认 | 清算批次、结算单、出款单、追偿单边界。 | 候选入池即清算、清算前置对账缺失、出款失败直接改历史分录，或重复出款。 | 清算批次确认才触发 `CLEARING -> AVAILABLE`；结算锁定、出款单、失败原因和追偿链路各自独立。 | 待确认 |
| B7-04 | 固化对账批次、差错单和核销。 | 待确认 | 对账差错闭环。 | 对账差异直接改分录或余额。 | 强制差错单、审批、凭证、调账/冲正、核销、重新对账。 | 待确认 |
| B7-05 | 固化清结算与对账并发竞争红线。 | 待确认 | TDD 13.5、清结算退款时序、对账重跑。 | 清算候选锁定与退款并发、结算锁定与出款回单/退款并发、对账重跑与差错核销并发导致重复扣减、重复出款或证据覆盖。 | 建立对象级锁定、批次唯一键、候选状态版本、重跑运行记录和差错核销互斥；失败方必须可审计且无副作用。 | 待确认 |

人工确认点：是否新建模块、模块命名、表命名、状态机、是否进入本轮开发范围。

### 批次 8：归档、重放与指标边界

批次 8 在本 change 中只保留边界、计划和准入门禁。当前代码已有 `governance-face` 和 `governance-impl` 交易投影重放骨架，可作为交易投影治理重放的候选落点；进入编码前仍必须另起独立 OpenSpec change，确认归档/重放任务模型、表设计、审批、回滚和生产门禁；指标只保留边界，不实现报表指标模块。

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B8-01 | 确认归档和重放任务模型。 | 待确认的 `governance-*` 扩展或独立包 | 系分 04、归档重放设计。 | 无审批、无范围、无 Manifest 即执行归档/重放。 | 人工确认后建立任务、审批、范围和 Manifest 边界。 | 待确认 |
| B8-02 | 固化归档门禁和水位推进顺序。 | 待确认 | 归档流程。 | 先推水位再写结果、缺差异报告。 | checkpoint、watermark、Manifest、差异报告作为上线门禁。 | 待确认 |
| B8-03 | 固化余额重建和交易投影重放。 | 待确认 | Balance Rebuild、Transaction Projection Replay。 | 余额重建从交易投影、余额日志或报表反推余额；交易投影重放重新入账。 | 保持事实只读、投影可重建、差异可解释；余额重建只从账本分录、检查点、水位和 Manifest 出发。 | 待确认 |
| B8-04 | 固化账本余额快照覆盖模式。 | 待确认 | 系分 04 账本余额快照、`DSL-GOVERNANCE-BALANCE-SNAPSHOT-001`。 | `HOT_ONLY` 强制要求 Manifest、`COLD_MANIFEST` 缺 Manifest 仍通过、`MIXED` 跳过冷热合并摘要校验。 | 建立 `HOT_ONLY`、`COLD_MANIFEST`、`MIXED` 三种覆盖模式校验；冷区和混合覆盖缺 Manifest 不得进入 `VERIFIED`；失败不得推进余额水位。 | 待确认 |
| B8-05 | 固化指标只读和指标水位隔离。 | 待确认 | 指标治理仅列指标项、`DSL-GOVERNANCE-METRIC-SNAPSHOT-BOUNDARY-001`、`TDD-METRIC-003`、`TDD-METRIC-004`。 | 在本模块实现报表指标计算，或指标失败推进余额水位、修改归档 Manifest；普通指标快照成功替代账本余额快照。 | 仅提供业务关心的指标口径输入，具体实现交给报表指标模块；指标水位独立于余额水位、归档 Manifest 和交易投影 checkpoint；普通指标快照不能证明余额正确。 | 待确认 |
| B8-06 | 固化归档、重放和快照范围互斥。 | 待确认 | TDD 13.5、归档门禁、重放差异报告和普通指标快照并发边界。 | 同一范围重复正式 apply、dry-run 推进 checkpoint、水位或 Manifest 被并发任务重复推进；普通指标快照覆盖余额快照状态。 | 建立范围锁、任务幂等键、dry-run/apply 分离、成功后单次推进规则和指标/余额快照状态隔离；失败任务不得推进水位。 | 待确认 |

人工确认点：生产重放范围、审批策略、回滚策略、账本余额快照覆盖模式字段、Manifest 覆盖策略、指标模块接口边界。

## 8. 依赖、门禁与设计反馈

1. 设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果必须先冻结为独立检查点；当前最新设计交付提交点以包含本 Harness Plan 的最新 docs 提交为准，上一冻结点为 620b5a5 docs: 精简并加固资金底座设计交付文档，之后才能进入具体编码批次。
2. 当前执行优先级固定为：先做 `02-交易路由钱包账目与投影`，再做 `03-清结算与对账`，最后做 `04-归档重放与指标治理`。
3. 批次 1 至批次 6 合并构成 02 阶段，内部仍按 DSL/core、钱包账户与账本、直接交易、授权交易、余额控制、Route Replay 与投影推进。
4. 批次 1 是后续所有交易、账本和授权测试的前置门禁。
5. 批次 2 是后续业务流余额断言、账务平衡和组合测试的前置门禁。
6. 批次 1 已有 DSL 契约测试基线，但仍未替代后续变更授权；如继续修改 `core` 公共枚举、Spec 或值对象，Execution Grant 必须显式确认是否允许修改公共契约和枚举，以及允许修改的范围。
7. 批次 4 涉及公共契约、枚举、服务入口和请求模型，必须先经人工确认再改生产代码；`settle` 强制完成和 `settleRefund` 无授权退款必须有策略、原事实引用、凭证、原因和审计；chargeback 不作为 `FundsAuthorizationTransactionService#chargeback` 目标入口。
8. 批次 7、批次 8 属于独立域，不得在批次 1-6 中顺手落入交易、钱包或账本主链路；进入编码前必须另起独立 OpenSpec change。
9. 除非用户再次调整，批次 8 不抢跑批次 7；04 只能消费 02 和 03 已确认的事实边界、批次摘要、差异报告和只读投影输入。
10. 任一批次发现产品、DSL、系分或 TDD 口径冲突时，先记录“设计错漏”，同步修正设计文档和 OpenSpec，再继续编码。
11. 每批必须先提交 Red 用例，再做最小 Green 实现，最后补充重构、观测、边界和回归验证。
12. 金额临界值按 TDD 5.1 作为所有资金变化测试的公共前置矩阵；并发竞争按 TDD 13.5 分散到授权、余额控制、清结算、对账和归档批次承接，不得只用顺序用例代替。

## 9. 批次到设计 ID 覆盖索引

| 批次 | 产品验收 ID | DSL 契约或场景 | TDD 用例入口 |
| --- | --- | --- | --- |
| 批次 1 | `RED-001`、`RED-003`、`RED-009`、`RED-020`、`RED-022`、`RED-023`、`RED-046` 至 `RED-049` | Route DSL、PaymentInstrument Route DSL、Posting/Ledger DSL、SettlementPolicy、金额临界值、JSON 契约 | `TDD-RED-001`、`TDD-RED-003`、`TDD-RED-004`、`TDD-RED-031`、`TDD-RED-032`、`TDD-RED-034` 至 `TDD-RED-037`、`TDD-LEDGER-001` 至 `TDD-LEDGER-011`、`TDD-ROUTE-011` 至 `TDD-ROUTE-013` |
| 批次 2 | `AC-PI-001` 至 `AC-PI-004`、`AC-PI-006`、`AC-PI-007`、`AC-CTRL-009` 至 `AC-CTRL-011`、`AC-BALLOG-001`、`RED-036`、`RED-046`、`RED-047`、`RED-049` | 支付工具、绑定关系、绑定历史审计、资金来源关系、账本周期、余额投影、余额日志 | `TDD-WALLET-*`、`TDD-ROUTE-011`、`TDD-ROUTE-012`、`TDD-LEDGER-*`、`TDD-VIEW-003` |
| 批次 3 | `AC-IN-*`、`AC-OUT-*`、`AC-PAY-*`、`AC-MER-*`、`AC-FEE-*` | `DSL-DIRECT-*`、`DSL-REVERSE-REFUND-FEE-001` | `TDD-DIR-*`、`TDD-DIR-FLOW-*`、`TDD-DIR-ERR-*` |
| 批次 4 | `AC-AUTH-001` 至 `AC-AUTH-012`、`AC-RAIL-001`、`AC-RAIL-002`、`RED-025` 至 `RED-027`、`RED-035` | `DSL-AUTH-LIFECYCLE-001`、`AUTHORIZATION_TRANSACTION / SETTLE` 强制完成模式、`AUTHORIZATION_TRANSACTION / AUTH_REFUND` 无授权退款模式、VCC 授权控制扩展 | `TDD-AUTH-*`、`TDD-AUTH-FLOW-*`、`TDD-AUTH-ERR-*`、`TDD-AUTH-EXT-*`、`TDD-RACE-001` 至 `TDD-RACE-003`、`TDD-RED-033` |
| 批次 5 | `AC-CTRL-001` 至 `AC-CTRL-008`、`AC-ADJ-001` | `DSL-BALANCE-*`、`DSL-LIMIT-ADJUST-001` | `TDD-CTRL-*`、`TDD-CTRL-FLOW-*`、`TDD-CTRL-ERR-*`、`TDD-RACE-004`、`TDD-RED-033` |
| 批次 6 | `AC-ROUTE-*`、`AC-PI-005`、`AC-VIEW-*`、`AC-BALLOG-001`、`AC-REPLAY-*`、`RED-003`、`RED-016`、`RED-017`、`RED-036`、`RED-043`、`RED-044`、`RED-048`、`RED-049` | Route Replay、支付工具换绑后原路径回放、交易投影、余额日志 | `TDD-ROUTE-*`、`TDD-RACE-009`、`TDD-VIEW-*`、`TDD-REPLAY-*` |
| 批次 7 | `AC-CLR-*`、`AC-SET-*`、`AC-REC-*`、`RED-030` 至 `RED-033` | 可清分明细、清分批次、清算候选、清算批次、结算单、出款单、对账批次、差错单、追偿单独立对象 | `TDD-CLS-*`、`TDD-SETTLE-*`、`TDD-RECON-*`、`TDD-RACE-005` 至 `TDD-RACE-007`、`TDD-RED-033` |
| 批次 8 | `AC-ARCH-*`、`AC-REPLAY-*`、`AC-RPT-*`、`RED-016` 至 `RED-019`、`RED-029`、`RED-034`、`RED-040` 至 `RED-042` | `DSL-GOVERNANCE-ARCHIVE-MANIFEST-001`、`DSL-GOVERNANCE-BALANCE-SNAPSHOT-001`、`DSL-GOVERNANCE-PROJECTION-REPLAY-001`、`DSL-GOVERNANCE-METRIC-SNAPSHOT-BOUNDARY-001`、归档、余额重建、交易投影重放、指标只读和指标水位隔离边界 | `TDD-ARCH-*`、`TDD-REPLAY-*`、`TDD-METRIC-*`、`TDD-RACE-008`、`TDD-RACE-010`、`TDD-RED-033` |

## 10. Execution Grant 候选模板

进入任一批次编码前，由用户确认以下授权信息：

```text
授权批次：
允许写入范围：
禁止写入范围：
必须覆盖的 TDD 用例：
必须覆盖的 AC/DSL ID：
基线是否已冻结：是/否
允许修改公共契约：是/否
公共契约允许修改范围：
允许新增枚举或事件：是/否
允许新增服务入口：是/否
允许扩展请求模型：是/否
允许修改表结构：是/否
允许新增模块：是/否
人工确认点：
基础验证命令：just mvn-version、just compile
专项验证命令：
交付方式：
```

## 11. 批次 1 建议 Execution Grant

批次 1 是 02 阶段的首个可执行批次，建议只做 DSL 契约、枚举红线和 JSON 契约测试，不碰交易主链路实现。02 阶段完成前，不启动 03、04 的编码批次。

```text
授权批次：批次 1 DSL 契约与枚举红线
允许写入范围：core/src/test/java；必要时 core/src/main/java 中 DSL/枚举/Spec、Route DSL、PaymentInstrument DSL、RoutingDecision/FundingAllocation DSL、Posting/Ledger DSL、SettlementPolicy 相关最小兼容修改
禁止写入范围：transaction-*、wallet-*、ledger-* 业务实现；清结算、对账、归档、指标实现；生产配置；外部通道适配
必须覆盖的 TDD 用例：TDD-RED-001、TDD-RED-003、TDD-RED-004、TDD-RED-031、TDD-RED-032、TDD-RED-034 至 TDD-RED-037、TDD-LEDGER-001 至 TDD-LEDGER-011、TDD-ROUTE-007、TDD-ROUTE-010 至 TDD-ROUTE-013
必须覆盖的 AC/DSL ID：RED-001、RED-003、RED-009、RED-020、RED-022、RED-023、RED-046 至 RED-049；Route DSL、Route Replay DSL、PaymentInstrument Route DSL、Posting/Ledger DSL、SettlementPolicy、金额临界值、JSON 契约
基线是否已冻结：已冻结，最新设计交付提交点以包含本 Harness Plan 的最新 docs 提交为准；继续执行或调整批次 1 仍需用户确认 Execution Grant
允许修改公共契约：待用户确认
公共契约允许修改范围：仅限支撑 DSL 契约测试所需的兼容性补齐；不得删除既有公开字段或破坏既有调用方
允许新增枚举或事件：待用户确认；若新增 `EXPIRE` 等事件，必须先有失败测试和兼容说明
允许新增服务入口：否
允许扩展请求模型：否
允许修改表结构：否
允许新增模块：否
人工确认点：公共枚举新增、Spec 字段新增、JSON 契约字段命名、PaymentInstrumentRef/RoutingDecision/FundingAllocationDecision 字段边界、Route Replay 与交易投影重放边界
基础验证命令：just mvn-version、just compile
专项验证命令：just test-core
交付方式：每轮说明覆盖用例、修改文件、验证命令、验证结果和残余风险；未获 Git 授权时不自动提交
```

## 12. 每批交付记录模板

```text
批次：
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
