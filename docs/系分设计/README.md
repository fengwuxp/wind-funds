# 支付资金底座系分设计

## 目录定位

本目录把已确认产品事实和 DSL 契约转成可编码、可测试的系统约束，重点说明模块职责、依赖方向、服务契约、数据模型、状态、不变量、事务、一致性、安全和观测。

正式系分只保留当前有效设计、真实取舍、风险、待确认和验证方向。测试类、执行命令、任务状态和历史争论由 TDD、Goal 或 Git 历史承接。

## 设计依据与裁决顺序

1. 产品目标、业务对象和验收边界以 [产品设计](../产品设计/README.md) 为准。
2. 结构化资金事实和不变量以 [DSL 设计](../DSL设计/README.md) 为准。
3. 当前可用接口、字段和状态以源码、公共 face 契约和枚举为准。
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
| `reconciliation-face/impl` | 对账批次、来源快照、运行结果、差异和准入门禁。 | 反写交易、钱包或账本历史事实。 |
| `governance-face/impl` | 归档、重放、差异报告和治理控制面。 | 把治理结果当资金事实或余额来源。 |
| `fx-impl` | 来源汇率选择和金额换算。 | 创建报价、换汇执行或资金交易。 |

公共调用依赖 `core` 和 `*-face`；Entity、Mapper、Repository、MyBatis 查询对象和实现类不得穿透模块边界。

## 系统事实链

```text
已确认业务事实
  -> FundsInstruction
  -> ResolvedRoute / RouteSnapshot
  -> PostingPlan
  -> LedgerTransaction / LedgerEntry
  -> BalanceProjection
  -> TransactionProjection / Reconciliation / Replay
```

主链写入必须保持原子性和可追溯性。投影、对账和治理只能消费或追加新事实，不能修改原交易、原分录或余额投影来“修正”历史。

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
| [01-系分设计总览.md](01-系分设计总览.md) | 系统边界、分层、模块、事实链和统一非功能约束。 |
| [02-交易路由钱包账目与投影系分设计.md](02-交易路由钱包账目与投影系分设计.md) | 钱包、交易、路由、账本和投影主链详细设计。 |
| [03-清结算与对账系分设计.md](03-清结算与对账系分设计.md) | 清分、清算、结算、出款、对账和差异闭环。 |
| [04-归档重放与指标治理系分设计.md](04-归档重放与指标治理系分设计.md) | 归档、重放、检查点、水位和治理边界。 |
| [05-测试观测安全与金融红线.md](05-测试观测安全与金融红线.md) | 跨模块测试、观测、安全和金融红线。 |
| [06-SpendRule支出规则系分设计.md](06-SpendRule支出规则系分设计.md) | Spend Rule、决策记录、控制流水和预算投影。 |

## 系分准出

设计进入编码前至少能够回答：

1. 入口服务、输入前提、业务输出、副作用和失败语义是什么。
2. 数据事实归哪个模块和表，唯一键与幂等键是什么。
3. 状态不明、重启、重放和重复请求如何处理。
4. 金额、币种、周期、主体和借贷平衡由什么不变量保护。
5. 哪些路径必须失败且无资金副作用。
6. 哪些约束已经有测试或静态门禁，哪些仍需 owner 或生产变更确认。
