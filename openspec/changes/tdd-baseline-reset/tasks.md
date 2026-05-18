# TDD 基线重置 Harness Plan

## 0. 当前状态

- [x] 作废旧 OpenSpec specs。
- [x] 作废旧 OpenSpec changes。
- [x] 删除旧测试源码。
- [x] 保留测试 resources。
- [x] 重建 OpenSpec 项目上下文。
- [x] 重建支付资金底座开发基线规格。
- [x] 校准 TDD 文档中“旧测试资产复用”表述。
- [ ] 冻结当前设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果，作为进入编码前的独立检查点。

## 1. 全局写入范围

后续开发批次允许按批次写入：

| 批次 | 写入范围 |
| --- | --- |
| 批次 1 | `core/src/test/java`、必要的 `core/src/main/java` DSL/枚举/Spec、Route DSL、Posting/Ledger DSL、SettlementPolicy。 |
| 批次 2 | `wallet-*`、`ledger-*`、`tests/src/test/java` 中账户、账本、投影相关测试和最小实现。 |
| 批次 3 | `transaction-*`、`tests/src/test/java` 中直接交易测试和最小实现。 |
| 批次 4 | `transaction-*`、`core`、`tests/src/test/java` 中授权交易测试和最小实现。 |
| 批次 5 | `transaction-*`、`tests/src/test/java` 中余额控制测试和最小实现。 |
| 批次 6 | `transaction-*`、`ledger-*`、`tests/src/test/java` 中路由回放、余额日志、交易投影测试和最小实现。 |
| 批次 7 | 清结算、对账相关新模块或包，需先确认。 |
| 批次 8 | 归档、重放相关新模块或包，需先确认；指标仅保留边界测试。 |

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
- [ ] 当前工作树的设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果需要先作为独立检查点冻结，再开始批次 1 编码。

## 6. 里程碑拆解

| 里程碑 | 目标 | 完成标志 | 阻塞关系 |
| --- | --- | --- | --- |
| M0 基线重置 | 作废历史基线、删除旧测试源码、保留 resources、重建 OpenSpec/Harness。 | 本文件第 0 节全部完成。 | 已完成。 |
| M1 DSL 与核心红线 | 先稳定 DSL 语义、枚举边界和 JSON 契约。 | 批次 1 通过，核心 DSL 红线可回归。 | 阻塞 M2-M5。 |
| M2 钱包账户与账本基础 | 账户、钱包、账本、余额投影具备可测试底座。 | 批次 2 通过，余额断言和账务平衡可复用。 | 阻塞 M3-M6 的组合测试。 |
| M3 直接交易链路 | 充值、付款、转账、提现、退款、手续费和 replay 可验证。 | 批次 3 通过。 | 依赖 M1、M2。 |
| M4 授权交易链路 | 授权、拒绝、撤销、过期、完成、退款、拒付承接口径和 VCC 扩展边界可验证。 | 批次 4 通过，并完成人工确认点。 | 依赖 M1、M2。 |
| M5 控制与投影治理 | 余额控制、路由回放、余额日志、交易投影具备红线测试。 | 批次 5-6 通过。 | 依赖 M1-M4 的核心语义。 |
| M6 独立域验证 | 清结算、对账、归档、重放和指标边界作为独立域验证。 | 批次 7-8 通过，且不污染资金主链路。 | 依赖产品/系分独立模块确认。 |

## 7. 详细任务计划

### 批次 1：DSL 契约与枚举红线

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B1-01 | 重建核心测试骨架和命名规范。 | `core/src/test/java` | TDD 设计、DSL 设计。 | 测试目录为空时先建立最小失败样例。 | 建立按 DSL/枚举/JSON 分层的测试包，不迁回旧测试源码。 | `just test-core` |
| B1-02 | 固化 `FundsInstructionSpec` 语义。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL 语义结构、JSON 契约。 | 构造最小合法指令、缺字段、非法金额、非法主体用例。 | 补齐或校准 Spec 校验行为。 | `just test-core` |
| B1-03 | 证明 `transactionType` 不承载生命周期事件。 | `core/src/test/java`、必要枚举。 | DSL 第九章、第十章。 | 将授权生命周期事件误塞入交易类型应失败。 | 保持交易类型与事件类型分离。 | `just test-core` |
| B1-04 | 固化授权 `eventType` 生命周期边界。 | `core/src/test/java`、必要枚举。 | DSL 授权交易用例。 | `PENDING`、`SETTLE`、`REVERSAL`、`REFUND`、`EXPIRE` 的合法/非法组合，以及拒付不强制落独立 `CHARGEBACK` 事件。 | 必要时补 `EXPIRE` 枚举或先形成失败基线；拒付通过退款承接口径表达。 | `just test-core` |
| B1-05 | 重建 JSON 契约样例解析测试。 | `core/src/test/java`、`core/src/test/resources` | DSL JSON 契约用例。 | JSON 样例无法解析、字段含义不一致、枚举不匹配。 | 让 DSL 样例能推导业务流程和 TDD 断言。 | `just test-core` |
| B1-06 | 固化 Route DSL 和 replay 类型契约。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL Route、Replay、不变量。 | 外部账户或工具进入 ledger node、缺快照 replay 仍成功、route code 漂移。 | 补 `RouteDslContractTests` 或等价契约测试。 | `just test-core` |
| B1-07 | 固化 Posting/Ledger DSL 契约。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL Posting、LedgerEntry、账务不变量。 | posting plan 不平衡仍可构造、entry 缺主体/账目/币种/周期仍通过。 | 补 posting、entry、digest 和账本周期契约测试。 | `just test-core` |
| B1-08 | 固化 SettlementPolicy DSL 契约。 | `core/src/test/java`、必要的 `core/src/main/java` | DSL SettlementPolicy、结算策略边界。 | 策略解析失败被静默按实时处理。 | 补 `SettlementPolicySpecTests` 或等价策略解析红线。 | `just test-core` |

### 批次 2：钱包账户、账本和余额投影基础

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B2-01 | 重建账户角色与建账规则。 | `wallet-*`、`ledger-*`、`tests/src/test/java` | 产品账户/钱包/账本概念、系分表设计。 | 资金账户、信用账户、预算组、平台账户角色缺失或混用。 | 校准账户创建、角色标识、缺账本失败行为。 | `just test-ledger` |
| B2-02 | 固化显式建账红线。 | `ledger-*`、`tests/src/test/java` | 账本与账本周期设计。 | 未建账本直接交易应失败。 | 保持建账动作可追溯，不隐式创建关键账本。 | `just test-ledger` |
| B2-03 | 固化账本周期和 posting 平衡。 | `ledger-*`、`tests/src/test/java` | 账本周期语义矩阵、账务规则矩阵。 | 周期混用、借贷不平、entry 字段缺失。 | 补齐周期隔离、posting plan 平衡和分录完整性。 | `just test-ledger` |
| B2-04 | 固化余额投影边界。 | `ledger-*`、`tests/src/test/java` | 余额投影、余额日志。 | 投影失败误回滚事实、余额日志被当事实源。 | 保持事实追加、投影可重建、日志只读辅助。 | `just test-boundary` |
| B2-05 | 建立余额断言支撑。 | `tests/src/test/java` | TDD 资金断言红线。 | 只断言状态、不断言余额桶。 | 补公共断言支撑，后续批次复用。 | `just test-boundary` |

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
| B4-05 | 固化 VCC / Spend Controls 扩展边界。 | `transaction-*`、`tests/src/test/java` | Spend Controls 为扩展能力。 | 将 spend controls 当成资金主链路 P0。 | 默认只保留边界测试；未明确启用发卡产品时不做实现。 | `just test-boundary` |
| B4-06 | 明确 chargeback 不落 `FundsAuthorizationTransactionService#chargeback`。 | `docs`、`openspec`、后续授权测试 | 用户补充口径、拒付业务事实。 | 测试或任务强制要求调用 `chargeback` 服务入口，或把拒付结果只保存成无法区分的普通退款。 | 拒付作为争议/扣回语义，通过 `settleRefund` 的原因、上下文、凭证和审计承接；如代码已有 `chargeback`，本轮不扩展为目标态主入口；查询、投影和审计必须能区分普通退款与拒付承接。 | `just test-transaction` |

人工确认点：`EXPIRE` 枚举、`expire` 服务入口、`settle` 强制完成请求契约；VCC / Spend Controls 默认只做边界测试，只有明确启用发卡产品才进入实现范围。

### 批次 5：余额控制

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B5-01 | 固化冻结/解冻控制语义。 | `transaction-*`、`tests/src/test/java` | 余额控制 DSL、资金红线。 | 冻结表达消费、跨主体冻结、多次解冻超额。 | 保持同主体 `AVAILABLE <-> FROZEN` 控制。 | `just test-balance-control` |
| B5-02 | 固化冻结后提现与组合路径。 | `transaction-*`、`tests/src/test/java` | 产品验收用例。 | 提现忽略冻结、冻结释放后余额桶错误。 | 逐步骤断言可用、冻结、实际扣划。 | `just test-balance-control` |
| B5-03 | 固化资金调账、信用额度、预算额度。 | `transaction-*`、`tests/src/test/java` | 账户/额度/预算概念边界。 | 调账绕过凭证，额度与余额混用。 | 保持调账凭证、额度口径和预算控制分离。 | `just test-balance-control` |
| B5-04 | 固化 FX 与余额控制边界。 | `transaction-*`、`tests/src/test/java` | TDD 红线。 | 余额控制承接 FX 或隐式换汇。 | 余额控制只处理同币种余额桶。 | `just test-boundary` |

### 批次 6：路由回放、交易投影和余额日志

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B6-01 | 固化 route resolver 选择和无副作用。 | `transaction-*`、`tests/src/test/java` | 路由系分、路由能力边界。 | `supports` 写事实或改变状态。 | 保持 resolver 只判定路径，不写交易/账本事实。 | `just test-boundary` |
| B6-02 | 固化缺快照 replay 失败。 | `transaction-*`、`tests/src/test/java` | 路由回放设计。 | 缺 route/amount/subject 快照仍重放成功。 | replay 必须依赖原始快照并输出明确失败原因。 | `just test-business-flow` |
| B6-03 | 固化交易投影只读和有界重放。 | `transaction-*`、`tests/src/test/java` | 交易投影重放。 | 无范围重放、投影水位复用事实水位。 | 引入范围、检查点、水位和差异报告红线。 | `just test-boundary` |
| B6-04 | 固化余额日志边界。 | `ledger-*`、`tests/src/test/java` | 余额日志设计。 | 余额日志失败导致事实回滚，或被用于修复余额。 | 保持日志为观测与审计辅助，不作事实源。 | `just test-boundary` |

### 批次 7：清结算与对账

批次 7 在本 change 中只保留边界、计划和准入门禁。进入编码前必须另起独立 OpenSpec change，确认模块命名、表设计、状态机、接口和验证命令。

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B7-01 | 确认独立模块、表和状态机。 | 待确认的新模块或独立包 | 系分 03、产品清分/清算/对账。 | 清算候选、批次、结算单、出款单、差错单混成一个对象。 | 人工确认后建立独立对象边界。 | 待确认 |
| B7-02 | 固化清算候选与排除。 | 待确认 | 清分规则、清算候选。 | 未完成交易入候选、冻结交易入候选。 | 候选只来自符合条件的交易事实。 | 待确认 |
| B7-03 | 固化结算锁定、出款成功/失败和追偿。 | 待确认 | 清算/结算/出款边界。 | 出款失败直接改历史分录，或重复出款。 | 保持锁定、出款单、失败原因和追偿链路。 | 待确认 |
| B7-04 | 固化对账批次、差错单和核销。 | 待确认 | 对账差错闭环。 | 对账差异直接改分录或余额。 | 强制差错单、审批、凭证、调账/冲正、核销、重新对账。 | 待确认 |

人工确认点：是否新建模块、模块命名、表命名、状态机、是否进入本轮开发范围。

### 批次 8：归档、重放与指标边界

批次 8 在本 change 中只保留边界、计划和准入门禁。进入编码前必须另起独立 OpenSpec change，确认归档/重放任务模型、表设计、审批、回滚和生产门禁；指标只保留边界，不实现报表指标模块。

| 任务 | 目标 | 写入范围 | 设计锚点 | Red 用例 | Green/实现动作 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| B8-01 | 确认归档和重放任务模型。 | 待确认的新模块或独立包 | 系分 04、归档重放设计。 | 无审批、无范围、无 Manifest 即执行归档/重放。 | 人工确认后建立任务、审批、范围和 Manifest 边界。 | 待确认 |
| B8-02 | 固化归档门禁和水位推进顺序。 | 待确认 | 归档流程。 | 先推水位再写结果、缺差异报告。 | checkpoint、watermark、Manifest、差异报告作为上线门禁。 | 待确认 |
| B8-03 | 固化余额投影重建和交易投影重放。 | 待确认 | 余额投影重放、交易投影重放。 | 投影重建污染事实源，或复用错误水位。 | 保持事实只读、投影可重建、差异可解释。 | 待确认 |
| B8-04 | 固化指标只读边界。 | 待确认 | 指标治理仅列指标项。 | 在本模块实现报表指标计算。 | 仅提供业务关心的指标口径输入，具体实现交给指标模块。 | 待确认 |

人工确认点：生产重放范围、审批策略、回滚策略、指标模块接口边界。

## 8. 依赖、门禁与设计反馈

1. 设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果必须先冻结为独立检查点，之后才能进入具体编码批次。
2. 批次 1 是后续所有交易、账本和授权测试的前置门禁。
3. 批次 2 是后续业务流余额断言、账务平衡和组合测试的前置门禁。
4. 批次 1 虽以测试和 DSL 契约为主，但可能涉及 `core` 公共枚举、Spec 或值对象；Execution Grant 必须显式确认是否允许修改公共契约和枚举，以及允许修改的范围。
5. 批次 4 涉及公共契约、枚举、服务入口和请求模型，必须先经人工确认再改生产代码；chargeback 不作为 `FundsAuthorizationTransactionService#chargeback` 目标入口。
6. 批次 7、批次 8 属于独立域，不得在批次 1-6 中顺手落入交易、钱包或账本主链路；进入编码前必须另起独立 OpenSpec change。
7. 任一批次发现产品、DSL、系分或 TDD 口径冲突时，先记录“设计错漏”，同步修正设计文档和 OpenSpec，再继续编码。
8. 每批必须先提交 Red 用例，再做最小 Green 实现，最后补充重构、观测、边界和回归验证。

## 9. 批次到设计 ID 覆盖索引

| 批次 | 产品验收 ID | DSL 契约或场景 | TDD 用例入口 |
| --- | --- | --- | --- |
| 批次 1 | `RED-001`、`RED-003`、`RED-009`、`RED-020`、`RED-022`、`RED-023` | Route DSL、Posting/Ledger DSL、SettlementPolicy、JSON 契约 | `TDD-RED-001`、`TDD-RED-003`、`TDD-RED-004`、`TDD-LEDGER-001` 至 `TDD-LEDGER-011` |
| 批次 2 | `AC-CTRL-009` 至 `AC-CTRL-011`、`AC-BALLOG-001`、`RED-036` | 账本周期、余额投影、余额日志 | `TDD-WALLET-*`、`TDD-LEDGER-*`、`TDD-VIEW-003` |
| 批次 3 | `AC-IN-*`、`AC-OUT-*`、`AC-PAY-*`、`AC-MER-*`、`AC-FEE-*` | `DSL-DIRECT-*`、`DSL-REVERSE-REFUND-FEE-001` | `TDD-DIR-*`、`TDD-DIR-FLOW-*`、`TDD-DIR-ERR-*` |
| 批次 4 | `AC-AUTH-001` 至 `AC-AUTH-010`、`AC-RAIL-001`、`AC-RAIL-002`、`RED-025` 至 `RED-027`、`RED-035` | `DSL-AUTH-LIFECYCLE-001`、VCC 授权控制扩展 | `TDD-AUTH-*`、`TDD-AUTH-FLOW-*`、`TDD-AUTH-ERR-*`、`TDD-AUTH-EXT-*` |
| 批次 5 | `AC-CTRL-001` 至 `AC-CTRL-008`、`AC-ADJ-001` | `DSL-BALANCE-*`、`DSL-LIMIT-ADJUST-001` | `TDD-CTRL-*`、`TDD-CTRL-FLOW-*`、`TDD-CTRL-ERR-*` |
| 批次 6 | `AC-VIEW-*`、`AC-BALLOG-001`、`AC-REPLAY-*`、`RED-016`、`RED-017`、`RED-036` | Route replay、交易投影、余额日志 | `TDD-ROUTE-*`、`TDD-VIEW-*`、`TDD-REPLAY-*` |
| 批次 7 | `AC-CLR-*`、`AC-SET-*`、`AC-REC-*`、`RED-030` 至 `RED-033` | 清结算与对账独立对象 | `TDD-CLS-*`、`TDD-SETTLE-*`、`TDD-RECON-*` |
| 批次 8 | `AC-ARCH-*`、`AC-REPLAY-*`、`AC-RPT-*`、`RED-016` 至 `RED-019`、`RED-029`、`RED-034` | 归档、重放、指标只读边界 | `TDD-ARCH-*`、`TDD-REPLAY-*`、`TDD-METRIC-*` |

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
允许修改表结构：是/否
允许新增模块：是/否
基础验证命令：just mvn-version、just compile
专项验证命令：
交付方式：
```

## 11. 每批交付记录模板

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
