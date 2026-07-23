# 支付资金底座 DSL 设计

## 目录定位

DSL 是产品语义到系统实现之间的结构化契约。它定义资金指令、主体、路由、账务、周期、回放和控制事实的稳定含义，使产品场景能够被系分、代码、JSON fixture 和 TDD 共同验证。

DSL 不负责产品策略、外部协议、数据库实现、工程任务状态或生产准出。主文档见 [支付资金底座DSL承载层设计.md](支付资金底座DSL承载层设计.md)。

## 事实优先级

| 事实 | 权威来源 |
| --- | --- |
| 产品目标、角色和场景边界 | [产品设计](../产品设计/README.md) |
| Java 公共契约、枚举和值对象 | `core` 与各 `*-face` 源码 |
| 当前路由和重放行为 | `transaction-impl` 与对应测试 |
| 表约束 | 实际迁移 DDL；当前 H2 schema 只作测试证据 |
| DSL 场景和不变量 | DSL 主文档与被测试读取的 fixture |
| 目标测试和执行证据 | [TDD 设计](../TDD设计/README.md) 与测试报告 |

正文样例未被测试读取时只能算说明性契约，不能声明为机器可执行证据。

## 资金事实链

```text
FundsInstruction
  -> ResolvedRoute
  -> RouteSnapshot
  -> PostingPlan
  -> LedgerTransaction
  -> LedgerEntry
  -> BalanceProjection / TransactionProjection
```

这条链只承接已经确认的业务事实。外部账户、卡、VA、token 和支付工具只能作为引用或快照；真正进入 RouteLeg、PostingPlan 和 LedgerEntry 的主体必须是内部资金账户、信用账户或平台角色解析后的平台资金账户。

## 核心对象

| 对象 | 职责 | 不变量 |
| --- | --- | --- |
| `FundsInstruction` | 表达一次资金动作及其业务引用。 | 金额为正、币种明确、类型与引用一致、幂等摘要稳定。 |
| `SubjectRef` | 引用可参与资金或额度账务的内部主体。 | 支付工具、外部账户和 SpendControlScope 不得伪装成主体。 |
| `RouteParticipant` | 固化本次路由参与主体和角色。 | 子账户身份在 `subjectRef`；直接父账户证据在可选层级快照。 |
| `AccountHierarchySnapshot` | 固化参与账户当时使用的直接父账户关系。 | 仅含 `relationSn + parentAccountRef`；不推导出资。 |
| `RouteLeg` | 表达从源主体/账目到目标主体/账目的金额路径。 | 同币种、金额闭合、主体和账目合法。 |
| `RouteSnapshot` | 固化本次真实路由选择。 | 当前只接受 `route.snapshot.v5`；逆向不得重选路。 |
| `PostingPlan` | 把 route 转成可过账计划。 | 每币种借贷平衡、周期明确、分录可追溯。 |
| `LedgerEntry` | 表达不可变分录事实。 | 正金额、明确借贷方向、账本/账目/周期一致。 |
| `SpendControlMovement` | 表达额度调整、预留、消耗、可信释放或显式退款补偿。 | 不是资金事实，不生成 route、posting 或 LedgerEntry。 |

## 路由与关系

DSL 明确区分三类关系：

1. `PaymentInstrumentBinding` 决定新交易可选择的工具绑定，并由工具快照保存历史解释证据。
2. `AccountHierarchyRelation` 表达账户的直接父账户，路由只把其最小证据附着到对应 `RouteParticipant`。
3. `SpendSubjectFundingRelation` 解析资金责任目标；账户层级不得替它自动决定出资主体。

路由快照不保存完整关系表，也不保存 root、自动多级树或资金分配摘要。

## Spend Rule DSL v1.1 规则版本挂载和决策证据

Spend Rule DSL 分为四类事实：

| 事实 | 用途 |
| --- | --- |
| 规则定义与不可变版本 | 描述规则是什么、适用什么条件。 |
| 规则挂载 | 描述哪个版本作用于哪个 scope 及生效窗口。 |
| 决策记录 | 描述一次请求使用哪个规则版本得到什么结果。 |
| 控制额度变动 | 描述额度调整、预留、消耗、可信释放和显式退款补偿。 |

决策记录不是资金交易；控制额度变动不是账本分录。`REFUND_COMPENSATED` 只有在上层产品明确决定退款恢复控制额度时才记录，不能由“发生退款”自动推导。Highnote 的公开口径中退款不影响累计授权额，这说明退款是否恢复额度必须是产品策略，而不是底座默认行为。

## DSL 不变量

1. 金额使用最小货币单位并遵守币种精度。
2. 负数不表达方向；方向由事件、route leg 和借贷侧表达。
3. 每个 PostingPlan 在同币种内借贷平衡。
4. 非 `LIFETIME` 账本周期必须有 `periodId`，且默认禁止跨周期过账和回放。
5. 失败、拒绝和不可信超时不得产生半截 route、posting、LedgerEntry 或余额变化。
6. 退款、撤销、完成、清算和重放必须引用原事实与原快照。
7. 投影、对账和治理不得反写交易、分录或余额事实。
8. 敏感支付工具原文不得进入 DSL、日志、fixture 或快照。

## 评审入口

新增或修改 DSL 时必须同时核对：产品场景、公共 Java 契约、route schema、JSON fixture、系分落点、TDD 用例和 must-fail 红线。缺少任一侧时只能保留为设计候选，不进入编码。
