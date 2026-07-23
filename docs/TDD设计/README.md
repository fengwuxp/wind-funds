# 支付资金底座 TDD 设计

## 目录定位

本目录把产品验收、DSL 不变量和系分约束转成可执行测试。测试 backlog、场景矩阵和目标测试资产的权威入口是 [支付资金底座测试驱动设计.md](支付资金底座测试驱动设计.md)。

TDD 文档可以维护测试类、fixture、验证命令和未覆盖项；产品和系分正文不复制这些执行信息。

## 证据分级

| 等级 | 证据 | 能证明什么 |
| --- | --- | --- |
| L1 | 纯 Java 契约和 DSL fixture 测试 | 类型、字段、枚举、序列化和 must-fail 契约。 |
| L2 | 真实 Service + H2 测试 | 服务规则、唯一约束、事务回滚和持久化行为。 |
| L3 | 业务流程集成测试 | 多步骤资金状态、route、posting、entry、投影和逆向链路。 |
| L4 | 重放、对账和治理测试 | 原事实引用、幂等重跑、差异和只读边界。 |
| L5 | 架构和静态门禁 | 模块依赖、core 纯净性、公共契约和实现泄露。 |

文档中的目标测试类不等于测试已经存在；只有测试代码存在、被实际执行并核对报告后，才是完成证据。

## 资金测试最小断言

发生资金或额度变化的测试按适用范围同时断言：

1. 业务状态和累计金额。
2. RouteSnapshot、RouteParticipant、RouteLeg 和原事实引用。
3. PostingPlan 借贷平衡。
4. LedgerTransaction 与 LedgerEntry 可追溯。
5. 每一步余额桶变化，而非只看最终余额。
6. 幂等重放不产生重复副作用。
7. 失败路径不留下半截事实。
8. 敏感数据、租户、权限和审计边界。

## 场景族

| 场景族 | 最低覆盖 |
| --- | --- |
| 直接交易 | topup、transfer、pay、refund、withdraw、fee、fee refund。 |
| 授权交易 | approve/decline、partial completion、reversal、force completion、refund、no-auth refund、timeout no-op。 |
| 余额控制 | freeze、partial unfreeze、withdraw from frozen、funding adjustment、credit limit adjustment。 |
| 钱包关系 | 工具绑定、账户层级、资金责任解析、工具换绑后原路回放。 |
| Spend Controls | 规则评估、决策验真、预留、消耗、可信释放、显式退款补偿和周期隔离。 |
| 清结算与对账 | 来源快照、批次、运行结果、差异、准入门禁、重跑和调账引用。 |
| VCC/全球账户/收单 | 只验证归一资金事实和底座能力；业务专属状态机留在上层。 |

## 三类关系测试矩阵

| 能力 | 必须证明 | 必须失败 |
| --- | --- | --- |
| `PaymentInstrumentBinding` | 联合业务键幂等、状态/版本/生效窗口、历史快照解释。 | 工具不可用仍准入、换绑后按当前关系重算历史。 |
| `AccountHierarchyRelation` | 一个直接父账户、同币种、账户存在且未关闭、顺序建边拒绝环路、参与方固化 `relationSn + parentAccountRef`。 | 自父关系、跨币种、第二父账户、原交易明确依赖父账户路径却缺少必要关系快照、关系自动触发出资。 |
| `SpendSubjectFundingRelation` | 账户主体在指定币种和关系类型下唯一解析资金责任目标。 | 0 条或多条仍放行，或把 SpendControlScope、Spend Rule、支付工具当成关系的源/目标主体。 |

当前 `AccountHierarchyRelation` 没有跨节点全局锁。测试可以证明唯一子账户约束和顺序环路校验，但不能把它包装成并发构建任意拓扑安全；开放并发批量建边前必须另做工程设计和并发验证。

## 业务场景验证路径

| 场景 | 验证路径 | 关键断言 | 当前证据 |
| --- | --- | --- | --- |
| 内部余额钱包 | 充值确认 -> 付款/转账 -> 部分退款 -> 全额退款。 | 每步核对 `AVAILABLE/CLEARING/SETTLEMENT`、route、posting、entry 和原路退回；重试不重复入账。 | L3：`FundsDirectTransactionFlowTests`、`FundsTransactionProjectionBusinessScenarioTests`。 |
| VCC 预付卡 | 内部资金转入卡绑定 `FundingAccount` 子账户 -> 授权占用 -> 完成/撤销 -> 退款/退卡余额回收。 | 卡只是工具；子账户 `AVAILABLE -> AUTHORIZATION -> SETTLEMENT` 或原路释放；内部充值必须是明确的账户间转账。 | 已有 L2/L3：`PaymentInstrumentTransactionAuthorizationTests` 证明支付工具型授权准入，账户级授权、转账与回放已有基线；预付卡工具贯穿完整生命周期的组合验收仍是目标。 |
| VCC 共享卡 | 卡绑定 `CreditAccount` 子账户 -> 解析直接父 `FundingAccount` -> 双主体授权占用 -> 完成时信用账户进入 `OUTSTANDING`、父账户承担一次真实结算 -> 撤销/退款按原快照回放。 | 信用额度与真实资金同时受控，但不重复结算；快照只固化直接父关系。 | 已有 L2/L3：`PaymentInstrumentTransactionAuthorizationTests` 证明支付工具型授权准入，`FundsAuthorizationTransactionFlowTests` 证明显式传入关联父账户的账户级生命周期；支付工具贯穿完成、撤销和退款的端到端组合仍是目标。 |
| 全球账户入金 | 上层验签并归一 -> 已确认的 `CONFIRMED` credit event -> 委派 topup -> 目标资金账户入账 -> 对账。 | `ACCEPTED/PROCESSING` 不得入账；外部账户只作为引用，不作为 LedgerEntry 主体。 | 已有 L2/L3：`ExternalFundsEventApplicationServiceTests` 只证明已确认、已归一事件到 topup/route/ledger；验签、外部来账核验和外部侧对账属于上层或待交付能力。 |
| 全球账户出款 | 付款申请 -> 准入门禁 -> 授权占用/受控扣款 -> 外部提交 -> 成功/失败/退回事实 -> 对账。 | 受理不等于到账；非终态只能挂起和解释，不伪造成功资金事实。 | 局部 L2/L3：已有冻结提现、投影解释和 `PayoutPreflightServiceTests`；真实外部出款生命周期未交付。 |
| 收单 | 支付尝试归一 -> capture 确认 -> 商户 `CLEARING` -> 清分/清算/结算 -> 出款 -> 部分/全额退款或争议资金结果 -> 对账。 | 只消费已确认资金事实；退款和争议资金结果引用原路由；清结算准入 fail-closed。 | 底层等价 L3：`FundsTransactionProjectionBusinessScenarioTests` 当前只证明 `PAY -> merchant SETTLEMENT -> partial refund` 投影；真实 capture -> `CLEARING` 和完整清结算仍是目标验收。 |
| 账务与业务对账 | 冻结两侧来源快照 -> 逐笔匹配 -> 派生摘要和差异 -> 准入门禁 -> 新的调账交易/再对账。 | 完整覆盖、来源不漂移、重跑幂等、头明细同事务；差异不直接修改历史交易、分录或余额。 | L2/L4：`ReconciliationRunResultApplicationServiceTests`、`ReconciliationDifferenceApplicationServiceTests`、`ReconciliationGateApplicationServiceTests`。 |

场景验证以“已有证据”和“目标验收”分开管理。底层等价测试只能证明可复用能力，不能替代发卡、银行、PSP、卡组织或商户业务的专项验收。

## 当前基线

- Route 当前版本为 `v5`，schema 为 `route.snapshot.v5`；旧 schema 必须拒绝。
- `RouteParticipant.subjectRef` 表达子账户，`AccountHierarchySnapshot` 只表达直接父账户证据。
- 信用账户使用 `LIMIT/AVAILABLE/AUTHORIZATION/OUTSTANDING`；`OUTSTANDING` 是已用额度，不是现金或外部清算状态。
- 授权超时是不可信状态，不自动释放资金或控制占用。
- 交易失败或拒绝导致的同事务预留应回滚，不写 `RELEASED` 业务补偿。
- `REFUND_COMPENSATED` 只在产品策略明确要求退款恢复控制额度时使用；未明确时退款只影响资金事实，不自动改变周期控制累计。
- `tests/src/test/resources/jdbc-schema.sql` 是 H2 测试 schema，不是生产迁移 DDL。

## 验证入口

优先使用根目录 `Justfile`：

```bash
just test-core
just test-ledger
just test-transaction
just test-balance-control
just test-business-flow
just test-boundary
just test-governance
just verify-slice <TestClass>[,<TestClass>] [module]
```

纯文档变更至少执行 `git diff --check`、旧符号/错误 schema 扫描和 Markdown 链接检查。测试结论需核对 Surefire 报告中的执行数、失败数和错误数，不能只依据 Maven 末行。
