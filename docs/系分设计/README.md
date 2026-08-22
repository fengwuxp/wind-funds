# 支付资金底座系分设计

## 目录定位

本目录把已确认产品事实和 DSL 契约转成可编码、可测试的系统约束，重点说明模块职责、依赖方向、服务契约、数据模型、状态、不变量、事务、一致性、安全和观测。[支付资金公共能力层系统分析设计](支付资金公共能力层-系分设计.md) 是本轮重设计的目标态权威入口；编号文档保留为当前实现和迁移证据，不能覆盖已通过 Checker 的目标章节。

正式系分只保留当前有效设计、真实取舍、风险、待确认和验证方向。测试类、执行命令、任务状态和历史争论由 TDD、Goal 或 Git 历史承接。

## 设计依据与裁决顺序

1. 产品目标、业务对象和验收边界以 [产品设计](../产品设计/README.md) 为准。
2. 结构化资金事实和不变量以 [DSL 设计](../DSL设计/README.md) 为准。
3. 目标工程边界以 [支付资金公共能力层系统分析设计](支付资金公共能力层-系分设计.md) 已通过 Checker 的章节为准；当前接口、字段、状态与编号系分只用于现状取证和迁移清册。
4. 表约束以实际迁移 DDL 为准；当前仓库只有 H2 测试 schema，不能据此声明生产 DDL 已交付。
5. 测试覆盖和验证命令以 [TDD 设计](../TDD设计/README.md) 与实际测试为准。

来源冲突时不得在系分中自行“兼容”两个口径；应标记偏差并修正文档或源码中的错误事实。

## 模块边界

| 模块 | 职责 | 禁止 |
| --- | --- | --- |
| `core` | 资金 DSL、枚举、值对象和端口契约。 | 依赖 DAL、Web、消息或具体实现。 |
| `wallet-face/impl` | 资金账户、信用账户、支出控制、支付工具、关系管理和余额查询。 | 写资金交易、route、posting 或 LedgerEntry。 |
| `transaction-face/impl` | 资金指令、交易生命周期、路由、回放和交易投影编排。 | 依赖 wallet/ledger 的实现层或 DAL。 |
| `ledger-face/impl` | 账本、账本交易、分录、posting 和余额投影。 | 持有业务交易生命周期或反向决定业务状态。 |
| `reconciliation-face/impl` | 对账范围/快照、strict-exact 运行结果、差异/current lineage/Gate，以及规范化清分、清算、结算阶段事实。 | 反写交易、钱包或账本历史事实；解释 raw rail、裁决责任、生成展示或把 Gate 当资金动作。 |
| `governance-face/impl` | 归档、重放、差异报告和治理控制面。 | 把治理结果当资金事实或余额来源。 |
| `fx-impl` | 来源汇率选择和金额换算。 | 创建报价、换汇执行或资金交易。 |

公共调用依赖 `core` 和 `*-face`；Entity、Mapper、Repository、MyBatis 查询对象和实现类不得穿透模块边界。

## 系统事实链

```text
BusinessFactRef / NormalizedExternalFundsFact
  -> FundsIntent / FundsAttempt / FundsActionInstruction
  -> FundsActionFact
  -> internal Route / Posting
  -> LedgerTransaction / LedgerEntry
  -> BalanceProjection / Reconciliation evidence
```

Ledger 本地写链必须在一个本地事务内原子写入 LedgerTransaction、LedgerEntry 与主余额投影；W3-03 进一步要求正资金效果的成功 FundsAction、生命周期累计/逐原事实上限、action-ledger 关联与这条写链共享本地事务。外部 authority/finality、Wallet 控制和 Reconciliation 仍通过稳定引用正交闭合；投影、对账和治理只能消费或追加新事实，不能修改原交易、原分录或余额投影来“修正”历史。

W3-04 已准出：Reconciliation 只按冻结 source/current lineage 强制 normalized 1:1 strict exact，Difference 只追加 action evidence，Gate 只作 exact-object 事务时点准入；raw payout receipt、责任裁决、beneficiary arrival、rail finality 和 recovery case 归上游或 adapter。

## 当前关键设计

### 关系模型

| 模型 | 唯一职责 | 当前数据约束 | 路由使用方式 |
| --- | --- | --- | --- |
| `PaymentInstrumentBinding` | 工具到 `FundingAccount` 或 `CreditAccount` 的绑定。 | 业务联合唯一键、`ACTIVE/SUSPENDED`、版本和生效窗口；`subjectType` 只接受两类账户主体。 | 新交易选择当前有效绑定；历史交易读取原工具快照；控制范围和 Spend Rule 由独立控制上下文承接。 |
| `AccountHierarchyRelation` | 账户到直接父账户的不可换绑结构关系。 | 一个账户最多一个直接父账户；同币种；资金/信用账户；禁止环。 | `RouteParticipant` 固化 `relationSn + parentAccountRef`。 |
| `SpendSubjectFundingRelation` | 资金/信用账户到出资或结算责任账户的唯一解析关系。 | 源端和目标端都只能是 `FUNDING_ACCOUNT` / `CREDIT_ACCOUNT`；`tenant + spendSubject + currency + relationType` 唯一。 | 形成显式资金责任决策，不由工具、Spend Rule 或账户层级自动推导。 |

`AccountHierarchyRelation` 不保存 root、状态、生效期或资金分配，不自动遍历多级树，也不触发父账户出资。当前环路校验没有跨节点全局锁；并发批量建边属于调用方准入和后续工程治理问题。

### 路由快照

- 当前 route 版本为 `v5`，schema 为 `route.snapshot.v5`。
- 子账户身份由 `RouteParticipant.subjectRef` 表达；直接父账户证据由参与方上的 `AccountHierarchySnapshot` 表达。
- `AccountHierarchySnapshot` 只包含 `relationSn` 和 `parentAccountRef`，不复制完整关系实体。
- 逆向交易和重放只接受原快照，不读取当前绑定、当前层级或当前规则重新选路。

### 账务与投影

- `LedgerTransaction` 负责一组分录的原子性、平衡和业务追踪。
- `LedgerEntry` 是不可变账务事实；余额投影是可重建读模型。
- `OUTSTANDING` 当前用于信用账户授权完成后的已用额度，不表示现金或外部结算完成。
- 支出控制范围和 Spend Rule 不作为 LedgerEntry 主体；其周期额度由控制流水和只读投影承接。

## 文档清单

| 文档 | 定位 |
| --- | --- |
| [支付资金公共能力层-系分设计.md](支付资金公共能力层-系分设计.md) | 本轮目标态系统设计；按 W3 章节与 Checker 逐步准出。 |
| [01-系分设计总览.md](01-系分设计总览.md) | 当前实现的系统边界与事实链迁移证据。 |
| [02-交易路由钱包账目与投影系分设计.md](02-交易路由钱包账目与投影系分设计.md) | 当前钱包、交易、路由、账本和投影实现证据。 |
| [03-清结算与对账系分设计.md](03-清结算与对账系分设计.md) | 当前清分、清算、结算、出款和对账实现证据。 |
| [04-归档重放与指标治理系分设计.md](04-归档重放与指标治理系分设计.md) | 当前归档、重放和治理实现证据。 |
| [05-测试观测安全与金融红线.md](05-测试观测安全与金融红线.md) | 当前跨模块验证与治理证据。 |
| [06-SpendRule支出规则系分设计.md](06-SpendRule支出规则系分设计.md) | 当前 Spend Rule 与控制流水实现证据。 |

## 系分准出

设计进入编码前至少能够回答：

1. 入口服务、输入前提、业务输出、副作用和失败语义是什么。
2. 数据事实归哪个模块和表，唯一键与幂等键是什么。
3. 状态不明、重启、重放和重复请求如何处理。
4. 金额、币种、周期、主体和借贷平衡由什么不变量保护。
5. 哪些路径必须失败且无资金副作用。
6. 哪些约束已经有测试或静态门禁，哪些仍需 owner 或生产变更确认。
