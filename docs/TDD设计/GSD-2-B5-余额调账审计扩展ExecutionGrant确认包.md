# GSD-2 B5 余额调账审计扩展 Execution Grant 确认包

## 1. 文档定位

本文最初是 `GSD2-B5-BALANCE-ADJUST-AUDIT-003` 的 Execution Grant 确认包，用于承接 B5-001 外部余额异常纠偏审计字段、B5-002 route snapshot 审计回链之后的下一轮最小可交付切片。用户明确确认 `Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-003` 后，本 Grant 已完成本地 Red / Green / Review / Verify；第 1 至 12 节保留为授权边界、禁止事项和回放审计依据，本轮消费结果见第 13 节。

本文不授权 DDL/H2 schema、Entity、Mapper、运行时配置、Git、联网、生产配置或真实资金操作；本轮实际写入限定在余额调账审计查询公共契约、只读聚合实现、目标服务流测试、Money value object 上下文校验和状态文档回写。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-B5-BALANCE-ADJUST-AUDIT-003` |
| 原子任务 | 补齐余额调账独立审计查询最小服务流，让外部余额异常纠偏能从交易事实、交易明细上下文、route snapshot 和账本事实解释。 |
| 所属阶段 | GSD-2 / B5 balance adjustment audit extension / consumed local Green。 |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| 当前状态 | `B5_BALANCE_ADJUST_AUDIT_QUERY_GREEN_VERIFIED` |
| Git / code baseline | `da3b4f19 feat: 补齐余额调账路由审计回链` 是本 Grant 开工前已提交基线；本轮 B5-003 变更仍在未提交工作树，待用户另行授权 Git 后提交固化。 |
| Owner | AI Native 流程编排负责 Goal、Loop、状态和停止条件；产品架构专家负责审计展示口径、运营验收和 Not Done；资深架构师负责查询契约、源码锚点、TDD、Review、验证命令和后续实现。 |
| Wave 边界 | 本确认包只准备一个 B5 审计扩展原子任务；不得并行推进 B7 清算/结算 gate、B7 差异报告、wallet 预交易快照、VCC facade、Spend Rule、运营审批流或泛化运营补账。 |
| 执行顺序 / 依赖关系 | 依赖 B5-001、B5-002 已完成；本轮已先写独立审计查询 Red，再做最小只读查询实现，最后回写 LWT Goal、W5 和 OpenSpec tasks。 |
| 授权范围 | 已消费范围仅包含余额调账审计查询相关 face 契约、DTO/Query、impl 只读查询、目标服务流测试、Money value object 上下文校验和文档状态。 |
| 写入范围 | 本文、LWT Goal、GSD-2 工作流、P0/P1 LWT 推进计划、TDD README、docs README 和 OpenSpec tasks 的状态同步；确认 Grant 后的候选代码写入范围见第 7 节。 |
| 写入文件 | `docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、transaction-face、transaction-impl、ledger-face、ledger-impl、tests、AGENTS.md、B5-001/B5-002 目标测试和最近 Git 提交。 |
| Git 策略 | `summary_only`。本 Grant 未授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

## 2. 产品裁决

产品裁决：B5-003 首个子切片选择“余额调账独立审计查询”，不选择“运营审批闭环”。

理由：

1. B5-001 已把外部余额异常纠偏的来源、凭证、审批、对账差错、责任和受控负可用策略放入请求和交易明细上下文。
2. B5-002 已把同一组安全审计摘要回链到 route snapshot，且明确敏感外部账户引用不进入 route snapshot。
3. 独立审计查询可以消费现有交易事实、交易明细、route snapshot、ledger transaction 和 ledger entry，不必新增审批状态机或 DDL。
4. 审批闭环涉及运营角色、审批状态、权限、SLA、财务复核和真实纠偏责任，容易越过当前 B5 最小可用边界，应另起专项 Grant。

业务目标：让运营、财务、风控、研发和测试可以按业务流水、交易流水或对账差错引用查询一笔余额调账的审计证据，并确认它是否来自外部余额异常纠偏、是否有 route snapshot 回链、是否有账本事实和余额影响。

非目标：

1. 不新增泛化运营补账入口。
2. 不新增审批流、审批状态机、工单、运营后台页面或权限模型。
3. 不新增独立审计表，除非用户另行确认 schema Grant。
4. 不绕过 B7 对账差错闭环，不替代对账差错、处理动作和重跑结果。
5. 不修改余额调账资金语义、账务主体、route leg、posting plan 或 ledger entry 生成逻辑。

## 3. 能力地图和对象边界

| 能力域 | 目标口径 | 本 Grant 处理 | 不做范围 |
| --- | --- | --- | --- |
| 余额调账审计查询 | 只读解释已发生的余额调账事实。 | 查询交易、交易明细、route snapshot、账本交易和分录，聚合审计证据。 | 不创建或修改调账事实。 |
| 外部余额异常纠偏 | 只允许被证据、审批、对账差错和责任引用支持。 | 展示来源类型、来源流水、证据、外部终局、余额快照、差错、重跑、责任和负余额策略。 | 不替代真实外部钱包、发卡行或通道余额逻辑。 |
| 账务事实解释 | 调账必须能回链账本交易、分录和余额变化。 | 返回 ledger transaction、entry 数量、金额方向和相关主体摘要。 | 不反写 ledger，不重放账务。 |
| route snapshot 解释 | route snapshot 是调账审计回链之一。 | 校验并展示 route context 审计摘要。 | 不把任意 request context 或敏感字段写入 route snapshot。 |

业务对象：

- BalanceAdjustmentAudit：面向运营和财务的调账审计视图，不是新的资金事实。
- FundsTransaction：调账交易事实。
- FundsTransactionDetail：调账明细和 instruction context 审计事实。
- RouteSnapshot：调账路线、平台调整账户和安全审计摘要。
- LedgerTransaction / LedgerEntry：账本事实和余额影响证据。
- ReconciliationDifference：可选对账差错引用，仅用于查询回链，不由本 Grant 创建。

### 3.1 业务流程和状态

主流程：

1. 外部钱包、发卡行或通道出现余额异常，业务侧按 B5-001 的请求字段发起受控余额调账。
2. transaction 生成余额调账交易事实、交易明细上下文和 route snapshot，ledger 生成账本交易和 LedgerEntry。
3. 审计查询按业务流水或交易流水读取已存在事实，聚合交易、明细、route snapshot 和账本证据。
4. 查询结果展示审计完整性、来源证据、审批引用、对账差错引用、责任引用、受控负可用策略和账务影响摘要。

异常流程：

- 交易事实不存在：返回空结果或明确未找到，不创建调账事实。
- route snapshot 缺失：展示 `routeSnapshotPresent=false` 或 fail-fast，不重建 route snapshot。
- ledger transaction 或 LedgerEntry 缺失：展示 `ledgerFactsPresent=false` 和审计不完整，不补写账务事实。
- 敏感外部账户出现在原始上下文：查询结果只返回安全引用，不返回完整外部账户、卡号、密钥、完整银行账号或外部 processor 原始 payload。

状态口径：

- `COMPLETE`：交易事实、交易明细、route snapshot 和账本事实均可追溯。
- `INCOMPLETE_ROUTE`：交易事实存在但 route snapshot 缺失或缺关键审计摘要。
- `INCOMPLETE_LEDGER`：交易事实存在但账本交易或 LedgerEntry 缺失。
- `NOT_FOUND`：按业务流水或交易流水无法定位余额调账事实。

人工兜底：审计不完整只产生查询可见的风险状态，由运营、财务、风控或研发按 B7 差错、治理重放或专项补事实流程处理；本 Grant 不提供审批、修复或补账入口。

### 3.2 规则矩阵

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验收口径 |
| --- | --- | --- | --- | --- | --- |
| 只读查询 | 调用余额调账审计查询服务 | 只能读取交易、明细、route snapshot、ledger transaction 和 LedgerEntry；不得创建、重放、修复或删除事实。 | P0 | B5-003 | 查询前后交易、route、posting、LedgerEntry、余额投影数量和金额不变。 |
| 审计完整性 | 查询命中余额调账交易 | 交易、明细、route snapshot、账本事实均存在时为完整；任一事实缺失必须显式暴露不完整状态。 | P0 | B5-003 | `auditCompleteness`、`routeSnapshotPresent`、`ledgerFactsPresent` 可判断。 |
| 敏感字段安全 | 原始上下文含外部账户或 processor payload | 只允许返回安全引用和摘要，不返回 `EXTERNAL_ACCOUNT_REF`、PAN、CVV、完整银行账号、密钥或原始 payload。 | P0 | B5-003 | 目标测试断言敏感字段缺失，安全引用存在。 |
| 审批与对账引用 | 调账带审批、差错、重跑或责任引用 | 查询只展示引用，不解释审批状态，不替代 B7 差错处理和重跑结果。 | P1 | B5-003 | 返回引用字段，且不新增审批或差错事实。 |
| 兼容边界 | 老交易缺少 B5-001/B5-002 审计字段 | 可以返回不完整或缺失字段，不做隐式补齐和历史事实迁移。 | P1 | B5-003 | 老数据查询不触发 replay、补事实或 DDL。 |

## 4. 推荐服务边界和接口契约

推荐新增或调整的服务形态：

```java
public interface FundsBalanceAdjustmentAuditApplicationService {

    Optional<FundsBalanceAdjustmentAuditDTO> findByBusinessSn(FundsBalanceAdjustmentAuditQuery query);

    Optional<FundsBalanceAdjustmentAuditDTO> findByTransactionSn(FundsBalanceAdjustmentAuditQuery query);
}
```

设计口径：

- 服务接口放在 `transaction-face/src/main/java/com/wind/funds/transaction/application/FundsBalanceAdjustmentAuditApplicationService.java`。
- 查询条件放在 `transaction-face/src/main/java/com/wind/funds/transaction/model/query/FundsBalanceAdjustmentAuditQuery.java`。
- 返回视图放在 `transaction-face/src/main/java/com/wind/funds/transaction/model/dto/FundsBalanceAdjustmentAuditDTO.java`。
- 实现类优先放在 `transaction-impl/src/main/java/com/wind/funds/transaction/application/impl/DefaultFundsBalanceAdjustmentAuditApplicationService.java`，保持和既有 application service 实现包一致。
- 服务是只读 application service，不属于 `FundsBalanceControlService` 写入能力。
- 查询结果只能聚合已有事实，不生成 route、posting、LedgerEntry、余额投影、交易投影或对账差错。
- 若 route snapshot 缺失，应返回明确的 `routeSnapshotPresent=false` 或 fail-fast，具体在 Red 阶段由架构师收窄；不得重建 route snapshot。
- 若 ledger transaction 或 entry 缺失，应返回审计不完整状态，不得补写账务事实。

接口契约：

| 契约项 | 约定 |
| --- | --- |
| 入参 | `FundsBalanceAdjustmentAuditQuery` 至少包含租户、业务流水或交易流水；字段必须能支持幂等查询和审计追踪，不携带外部账户原文。 |
| 出参 | `FundsBalanceAdjustmentAuditDTO` 返回交易摘要、交易明细审计摘要、route snapshot 摘要、账本事实摘要、审计完整性、风险状态和安全引用。 |
| 错误码 | 非法租户、空查询键、非余额调账交易、事实缺失、敏感字段越界应有明确错误或状态；不得用通用空指针或隐式成功吞掉。 |
| 幂等 | 同一查询条件多次调用结果稳定，且查询不产生任何写侧事实。 |
| 兼容 | 老交易缺少 B5-001/B5-002 审计字段时返回不完整状态或缺失字段，不触发迁移、补写或 replay。 |
| 权限 | 后续若接入运营后台，应按租户、主体和角色做权限校验；本 Grant 只定义服务契约和测试口径，不做权限模型。 |

## 5. 角色协作 Loop 三卡交接

本节把 B5-003 从“可确认任务”进一步整理为角色协作 Loop 可消费的三张交接卡。三卡只说明事实、边界、验收和停止条件，不替代 Execution Grant、测试通过、代码 Review 或上线审批。

### 5.1 Product Context Card

| 字段 | 内容 |
| --- | --- |
| 业务目标 | 让运营、财务、风控、研发和测试能只读查询一笔余额调账的审计证据，确认它是否来自外部余额异常纠偏、是否具备 route snapshot 回链、是否有账本事实和余额影响证据。 |
| 目标用户 / 验收方 | 运营、财务、风控、研发、测试；正式接入运营后台、权限模型或财务审批前仍需对应 owner 确认。 |
| 核心对象 | BalanceAdjustmentAudit 视图、FundsTransaction、FundsTransactionDetail、RouteSnapshot、LedgerTransaction、LedgerEntry、ReconciliationDifference 引用。 |
| 关键不变量 | 审计查询只读；查询前后交易事实、route、posting、LedgerEntry、余额投影、对账差错均不得变化；敏感外部账户和 processor 原始 payload 不得返回。 |
| 主流程 | 已发生余额调账后，按业务流水或交易流水读取交易、明细、route snapshot 和账本事实，聚合审计完整性、来源证据、审批引用、差错引用、责任引用和账务影响。 |
| 异常路径 | 交易不存在返回未找到；route snapshot 缺失返回不完整；ledger facts 缺失返回不完整；敏感字段只返回安全引用或摘要。 |
| 验收种子 | `B5-AUDIT-QUERY-RED-001` 完整审计查询、`B5-AUDIT-QUERY-RED-002` 不完整事实暴露、`B5-AUDIT-QUERY-RED-003` 敏感字段不泄露。 |
| 非目标 | 不做运营审批闭环、独立审计表、泛化运营补账、补事实执行、B7 差错创建、后台页面、权限模型、真实资金纠偏或外部通道调用。 |
| 风险和待确认 | 后续若要对运营展示、财务审批、权限隔离、证据附件、真实补事实或生产数据治理做产品化，需要另起产品和工程 Grant。 |

### 5.2 Engineering Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前阶段 | `Consumed / B5_BALANCE_ADJUST_AUDIT_QUERY_GREEN_VERIFIED / summary_only`。 |
| 下一 owner | 用户确认 Grant 后交给资深架构师进入 TDD / 测试设计和编码实现；AI Native 继续维护 Goal、Loop、状态账本和停止条件。 |
| 入口契约 | 候选 `FundsBalanceAdjustmentAuditApplicationService`，提供 `findByBusinessSn` 与 `findByTransactionSn` 两个只读查询方法。 |
| 建议包边界 | `transaction-face` 放 application service、`model/query` Query、`model/dto` DTO；`transaction-impl/application/impl` 做只读聚合实现；`tests` 新增目标服务流测试；优先复用现有交易和账本查询服务，不新增 Mapper、Entity 或 schema。 |
| 只读源码锚点 | `FundsTransactionQueryService` / `DefaultFundsTransactionQueryService` 读取交易、交易明细和 RouteSnapshot；`ledger/ledger-face/src/main/java/com/wind/funds/ledger/service/LedgerTransactionService.java` 通过 `LedgerTransactionQuery`、`LedgerEntryQuery` 读取账本交易和分录；`FundsBalanceControlInstructionConverter`、`BalanceControlFundsInstructionRouteResolver` 和 `DefaultFundsInstructionLifecycleSaver` 是 B5-001/B5-002 已有事实来源；`FundsBalanceAdjustAuditFlowTests` 是首个服务流测试准备锚点。 |
| 写入上限 | 确认 Grant 后仅允许审计查询契约、DTO/Query、只读查询实现、目标测试和文档状态回写；不得新增 DDL、Entity、Mapper 或运行时配置。 |
| TDD 切入 | 先写 `B5-AUDIT-QUERY-RED-001`，证明完整审计查询必须能从已有事实聚合证据且无写侧副作用。 |
| 验证命令 | 目标测试、`just test-balance-control`、`just compile`、`just pmd` 和 `git diff --check`；若触碰公共契约或边界测试，再追加对应分组。 |
| Git 策略 | 当前仍是 `summary_only`；只有用户另行授权提交，且验证通过、工作树未混入用户无关改动时，才可提交。 |
| 停止条件 | 需要 DDL/H2 schema、审批状态、运营工单、权限模型、补事实执行、真实资金操作、外部 processor payload 或敏感字段返回时停止。 |

### 5.3 Production Loop Card

| 字段 | 内容 |
| --- | --- |
| 生产可用能力锚点 | 查询服务能解释已发生的余额调账事实，不承担审批、补账、重放、清结算或真实外部账户修复。 |
| 安全边界 | 不返回完整外部账户、卡号、密钥、完整银行账号、PAN、CVV、processor 原始 payload 或无关 request context。 |
| 可靠性边界 | 查询幂等、只读、无资金副作用；事实缺失必须显式暴露不完整，不通过重算、重放或补写掩盖。 |
| 对账边界 | B7 对账差错、重跑、处理动作和清结算仍是独立事实源；B5-003 只展示引用，不创建或消费 B7 动作。 |
| 可观测和审计 | 最小版本先以查询结果字段和测试证据证明可追溯；若接入运营后台或日志审计，需要另起可观测、权限和审计 Grant。 |
| 发布前门禁 | 目标测试、余额控制分组、编译、PMD、diff 检查通过；CR 需确认 no-ddl、只读查询、无资金副作用和敏感字段不泄露。 |
| 回滚 / 降级 | 最小实现是只读查询契约和实现；若发布后发现展示口径问题，可下线调用方或禁用入口，不影响既有交易、route 和 ledger 写侧链路。 |
| 残余风险 | 未覆盖运营审批闭环、后台权限、审计附件、差错处理消费、生产数据迁移和清结算结算动作；这些不应被 B5-003 完成状态掩盖。 |

### 5.4 源码锚点和候选落点

| 层级 | 已有锚点 | B5-003 使用方式 | 禁止扩张 |
| --- | --- | --- | --- |
| 交易事实查询 | `transaction-face/src/main/java/com/wind/funds/transaction/services/FundsTransactionQueryService.java`；`transaction-impl/src/main/java/com/wind/funds/transaction/services/impl/DefaultFundsTransactionQueryService.java`。 | 复用 `queryFundsTransaction`、`queryFundsTransactionDetails` 和 `findRouteSnapshotByTransactionSn` 获取主交易、明细和 route snapshot。 | 不在查询服务里生成新 route，不重放交易，不补写交易明细。 |
| 交易事实字段 | `FundsTransactionDTO.routeSnapshot/contextVariables`，`FundsTransactionDetailDTO.ledgerTransactionSn/contextVariables`。 | 审计 DTO 只读取安全摘要、业务流水、状态、金额、币种、ledgerTransactionSn 和上下文审计字段。 | 不返回敏感外部账户原文，不把 request context 原样外放。 |
| 余额调账来源 | `FundsBalanceControlInstructionConverter.adjustContext` 和 `BalanceControlFundsInstructionRouteResolver.adjustmentRouteContextVariables`。 | 作为 B5-001/B5-002 已固化的来源字段清单，校验审计查询必须能展示来源、证据、审批、差错、重跑、责任和受控负可用策略。 | 不扩大余额调账写侧语义，不修改 route leg、posting plan 或 LedgerEntry 生成逻辑。 |
| 生命周期保存 | `DefaultFundsInstructionLifecycleSaver` 保存交易、明细、ledgerTransactionSn 和 route snapshot。 | 查询服务以已落库事实为唯一来源，缺失时返回审计不完整。 | 不通过生命周期 saver 反向补事实，不调用 replay。 |
| 账本查询 | `LedgerTransactionService.queryAccountLedgerTransactions`、`LedgerTransactionService.queryLedgerEntries`、`LedgerTransactionQuery.fundsTransactionSn/businessSn/businessScene`、`LedgerEntryQuery.ledgerTransactionSn/businessSn/businessScene`。 | 通过现有 ledger face 查询账本交易和分录，形成账务事实摘要和 entry 计数、金额方向、主体摘要。 | 不新增 ledger Mapper，不跨过 ledger face 直接读 DAL。 |
| 目标测试 | `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsBalanceAdjustAuditFlowTests.java`。 | 复用外部余额异常纠偏数据准备，新增或拆出 `FundsBalanceAdjustmentAuditApplicationServiceTests` 验证查询视图和无写侧副作用。 | 不只断言 DTO 非空；必须断言交易、route、ledger、余额投影数量或金额无变化。 |

源码复核补充：

1. `FundsBalanceAdjustAuditFlowTests` 现有外部余额异常纠偏请求构造方法是测试类私有方法，确认 Grant 后首个 Red 可选择先落在同一测试类，或在不改变业务语义的前提下把最小数据准备抽到测试支撑层；不得为了复用方便放宽外部余额异常、审批、对账差错、责任和受控负余额字段断言。
2. `LedgerTransactionService` 同时存在写入和查询方法，B5-003 实现只允许调用 `queryAccountLedgerTransactions`、`queryLedgerEntries` 等只读方法；不得调用 `postLedgerTransaction`、`updateLedgerTransactionStatus`、`deleteLedgerTransaction` 或任何补事实、重放、修复入口。
3. `LedgerTransactionQuery`、`LedgerEntryQuery` 继承通用查询能力，Green 实现必须给出明确的业务流水、交易流水或账本交易流水查询条件，避免无界扫描；目标测试应覆盖同一业务流水多次查询结果稳定。
4. 交易投影已有 `DefaultFundsTransactionProjectionExplainApplicationService` 作为只读 application service 参考，但 B5-003 不是交易投影解释的扩展；它的输出必须以余额调账审计完整性、账本事实和敏感字段安全为中心。

### 5.5 首个 Red 设计提示

`B5-AUDIT-QUERY-RED-001` 建议按下面顺序构造，不作为代码授权，只作为后续 TDD 交接提示：

1. 使用 `FundsBalanceAdjustAuditFlowTests` 现有外部余额异常纠偏请求准备一笔成功余额调账，保留业务流水。
2. 调用候选 `FundsBalanceAdjustmentAuditApplicationService.findByBusinessSn` 查询审计视图。
3. 断言审计视图包含交易流水、业务流水、调账来源、证据、审批、外部终局事件、外部余额快照、对账差错、重跑引用、责任引用、受控负可用策略、route snapshot present、ledger facts present 和完整状态。
4. 断言审计视图不包含 `EXTERNAL_ACCOUNT_REF`、完整外部账户、PAN、CVV、完整银行账号、密钥或 processor 原始 payload。
5. 查询前后断言交易、交易明细、RouteSnapshot、LedgerTransaction、LedgerEntry 和相关余额投影没有新增、删除或金额变化。

若第 5 步缺少可复用断言支撑，优先在测试支撑层补只读计数或余额断言，不允许为了让 Red/Green 通过而放宽资金副作用断言。

## 6. Red 候选和验收矩阵

| redId | businessQuestion | moneyInvariant | expectedFacts | forbiddenFacts | minimumAssertions | targetAssets | verificationCommand | stopCondition |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `B5-AUDIT-QUERY-RED-001` | 外部余额异常纠偏后，是否能按业务流水查询完整审计视图。 | 查询不得产生资金副作用。 | 返回交易、明细、route snapshot、账本交易、分录、来源、证据、审批、对账差错、责任和受控负可用策略。 | 不得写交易、route、posting、LedgerEntry、余额投影或对账差错。 | 审计字段完整、route snapshot present、ledger facts present、余额影响可解释。 | 新审计查询服务和 `FundsBalanceAdjustAuditFlowTests` 或独立查询测试。 | `just test-one FundsBalanceAdjustmentAuditApplicationServiceTests tests`、`just test-balance-control`、`just compile`、`git diff --check`。 | 需要新增审计表或审批状态时停止。 |
| `B5-AUDIT-QUERY-RED-002` | route snapshot 缺失或账本事实缺失时，审计查询是否明确暴露不完整。 | 查询不得重建事实。 | 返回缺失原因或 fail-fast。 | 不得通过 replay、补事实或隐式重算掩盖缺口。 | routeSnapshotPresent / ledgerFactsPresent / auditCompleteness 可判断。 | 查询服务测试。 | 目标测试 + `just compile`。 | 需要治理重放或补事实执行时停止。 |
| `B5-AUDIT-QUERY-RED-003` | 敏感外部账户引用是否不会出现在审计视图安全摘要中。 | 审计可解释但不泄露敏感账户。 | 展示外部余额快照引用、外部终局事件引用和责任引用。 | 不返回 `EXTERNAL_ACCOUNT_REF`、PAN、CVV、完整银行账号、密钥或无关外部 payload。 | 断言敏感字段缺失，安全引用存在。 | 查询服务测试 + DSL 敏感字段回归。 | 目标测试，必要时 `just test-boundary`。 | 需要引入外部附件或证据原文时停止。 |

## 7. 候选写入范围和禁止事项

确认 `Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-003` 后，候选写入范围上限如下。实际编码前仍需由资深架构师根据工作树状态和首个 Red 再收窄。

| 类型 | 候选范围 |
| --- | --- |
| face 契约 | `transaction-face` 新增审计查询 application service、Query、DTO。 |
| impl 查询 | `transaction-impl` 基于资金交易、交易明细、route snapshot、ledger transaction / entry 的只读聚合。 |
| tests | 新增 `FundsBalanceAdjustmentAuditApplicationServiceTests`，必要时复用 `FundsBalanceAdjustAuditFlowTests` 数据准备。 |
| docs / OpenSpec | 本文、LWT Goal、W5、README、OpenSpec tasks 和 Not Done 回写。 |

禁止事项：

1. 不新增 DDL/H2 schema、Entity、Mapper 或生产迁移脚本。
2. 不新增审批流、审批状态、运营工单、后台页面或权限模型。
3. 不新增泛化运营补账入口。
4. 不让查询服务创建、重放或修复交易事实、route、posting、LedgerEntry、余额投影或对账差错。
5. 不把支付工具、预算组、Spend Rule、父账户、route snapshot 或交易投影写成 ledger subject。
6. 不泄露敏感外部账户、卡号、密钥、完整银行账号或外部 processor 原始 payload。

## 8. 验证矩阵

| 验证层 | 命令或方式 | 完成条件 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md` | Task、Owner、范围、验证、TDD、Review、Execution Grant、人工确认和交接字段齐全。 |
| GSD Wave 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md` | Wave、上下文账本、禁止事项、验证矩阵和 handoff 字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md` | 业务目标、能力地图、对象、流程、规则、数据审计、风险和验收齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md` | 背景目标、现状、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-B5-BALANCE-ADJUST-AUDIT-003|余额调账审计扩展|BalanceAdjustmentAudit" docs openspec` | LWT Goal、W5、README 和 OpenSpec tasks 能追踪到本文。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |

确认后建议验证命令：

```bash
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsBalanceAdjustmentAuditApplicationServiceTests tests
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-balance-control
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just compile
WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just pmd
git diff --check
```

## 9. 可复制确认文本

用户若确认进入本 Grant，建议复制下面文本，避免把 B5-003 扩大成运营审批、补事实或清结算任务：

```text
Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-003

确认按 docs/TDD设计/GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md 执行，只允许处理余额调账独立审计查询最小服务流：新增或调整只读查询契约、DTO/Query、transaction-impl 只读聚合实现、目标服务流测试和状态回写。

保持 no-ddl、只读查询、无资金副作用、敏感字段不泄露；不新增审计表、审批流、运营工单、泛化运营补账、补事实执行、对账差错创建、清算/结算处理、真实资金操作或 Git 提交。
```

确认文本解释：

- `Execution Grant` 只授权 B5-003 一个原子任务，不继承 B5-002、B7、AUTH、wallet 或 ledger guard 的旧授权。
- “只读查询”表示服务可以读取交易事实、交易明细、route snapshot、ledger transaction 和 LedgerEntry，不能创建、重放、补写或修复任何资金事实。
- “最小服务流”表示首个 Red 优先证明完整审计查询、敏感字段不泄露和查询无写侧副作用；不把运营审批闭环放入本 Grant。
- Git 策略仍是 `summary_only`；如需提交，必须用户另行明确授权，且验证通过、工作树未混入无关改动。

## 10. Grant 消费预检清单

本节用于用户复制确认后、资深架构师进入首个 Red 前执行。预检通过只表示可以开始本 Grant 的第一个失败用例，不代表后续 Green、Review、验证、Git 或生产准出已经通过。

| 检查项 | 预检要求 | 不通过处理 |
| --- | --- | --- |
| 用户授权 | 用户明确回复 `Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-003` 或等价确认。 | 未确认前只允许 docs-only 状态维护或只读 Gap Audit。 |
| 工作树状态 | `git status --short` 只包含本 Grant 可解释的变更，且没有影响目标文件的用户未归属改动。 | 先收口或请用户确认保留/隔离策略。 |
| 当前基线 | 以 `da3b4f19 feat: 补齐余额调账路由审计回链` 之后的当前工作树为准。 | 若 HEAD 或工作树已变化，先重新读取 B5-003 确认包、LWT Goal、W5 和 OpenSpec tasks。 |
| 首个 Red 收窄 | 默认选择 `B5-AUDIT-QUERY-RED-001`；若现有代码已满足，则改用 `B5-AUDIT-QUERY-RED-002` 或 `B5-AUDIT-QUERY-RED-003`。 | 不得直接写 Green；先证明缺口或记录现有证据。 |
| 写入范围 | 仅 `transaction-face` 查询契约/DTO/Query、`transaction-impl` 只读聚合、目标测试和状态文档。 | 需要 DDL、Entity、Mapper、审批、运营后台、权限或补事实时停止。 |
| 只读来源 | 复用 `FundsTransactionQueryService`、`LedgerTransactionService`、route snapshot 和 B5-001/B5-002 已有事实来源。 | 不跨过 face 直接读 ledger DAL，不调用生命周期 saver 反向补事实。 |
| 验证顺序 | 目标测试 -> `just test-balance-control` -> `just compile` -> `just pmd` -> `git diff --check`；必要时追加 boundary / governance 分组。 | 验证失败先按本 Grant 修复；越界才暂停确认。 |
| 状态回写 | Green 后必须回写本文、LWT Goal、W5、TDD README、docs README 和 OpenSpec tasks。 | 没有状态回写不得进入下一 Grant。 |
| Git 策略 | 默认 `summary_only`；未获 Git 授权时只给建议提交单元和 commit message。 | 不执行 `git add` / `git commit`。 |

## 11. Grant 消费运行卡

| 阶段 | 执行动作 | 通过条件 | 停止条件 |
| --- | --- | --- | --- |
| Red 选择 | 优先写 `B5-AUDIT-QUERY-RED-001`，证明按业务流水查询外部余额异常纠偏调账审计视图仍缺最小只读服务流。 | 测试能表达业务流水、交易事实、route snapshot、ledger facts、审计字段、敏感字段和无写侧副作用。 | 首个 Red 需要新增审计表、审批流、运营工单、权限模型或补事实执行。 |
| Red 范围 | 目标测试建议落在 `FundsBalanceAdjustmentAuditApplicationServiceTests`，必要时复用 `FundsBalanceAdjustAuditFlowTests` 的数据准备。 | 测试用真实 Spring Bean 和 H2 schema，断言查询前后交易、route、ledger、余额投影无变化。 | 只能靠 Mock 内部核心组件或放宽资金副作用断言才能构造测试。 |
| Green 实现 | 最小实现新增只读 application query service，复用交易查询和 ledger face 聚合审计视图。 | 首个 Red 通过，且不新增 DDL、Entity、Mapper、写侧服务调用或资金事实。 | 必须修改交易写侧、route 解析、posting、LedgerEntry 或余额投影才能变绿。 |
| Review | 资深架构师检查模块边界、公共契约、敏感字段、只读行为、异常状态、幂等查询和测试质量。 | 无跨层 DAL、无敏感字段泄露、无副作用、无泛化运营补账倾向。 | 出现查询服务补事实、重放、审批、B7 动作消费或真实资金修复。 |
| Verify | 执行目标测试、余额控制分组、编译、PMD 和 diff；失败时先定位是否本 Grant 范围内可修。 | 验证通过，状态文档和 OpenSpec 回写完成。 | 验证失败且需要越界修复，或环境/依赖失败无法区分代码问题。 |
| Handoff | 输出文件清单、测试清单、验证结果、Not Done、残余风险和下一候选。 | 下一轮仍需重新确认单一 Grant。 | 不得把 B5-003 完成解释为余额调账审计、运营审批或生产发布全量 Done。 |

最小断言清单：

1. 可以按业务流水或交易流水定位已发生的余额调账交易。
2. 审计视图能说明交易事实、交易明细、route snapshot、ledger transaction 和 LedgerEntry 是否完整。
3. 外部余额异常来源、证据、审批、外部终局事件、外部余额快照、对账差错、重跑、责任和受控负可用策略能被安全展示。
4. 审计视图不返回 `EXTERNAL_ACCOUNT_REF`、PAN、CVV、完整银行账号、密钥或 processor 原始 payload。
5. 查询前后交易、交易明细、route snapshot、LedgerTransaction、LedgerEntry、余额投影和对账差错均无新增、删除或金额变化。

## 12. 交接和停止条件

交接裁决：本文可作为 `GSD2-B5-BALANCE-ADJUST-AUDIT-003` 的用户确认材料。确认后，首个 Red 应优先选择 `B5-AUDIT-QUERY-RED-001`，并保持 no-ddl、只读查询、无资金副作用。

必须停止并重新确认的情况：

1. 需要新增审计表、审批状态、运营工单、权限模型或后台页面。
2. 需要补事实执行、治理重放、对账差错创建或 B7 清结算处理。
3. 查询服务必须修改既有交易、route、ledger 或余额投影事实才能变绿。
4. 需要保存或返回敏感外部账户、卡号、密钥、完整银行账号或外部 processor 原始 payload。
5. 工作树出现影响目标文件的用户未归属变更，或验证失败且无法在本 Grant 范围内修复。

## 13. Grant 消费结果（2026-06-19）

| 字段 | 内容 |
| --- | --- |
| 消费状态 | `B5_BALANCE_ADJUST_AUDIT_QUERY_GREEN_VERIFIED`。 |
| Red 证据 | `FundsBalanceAdjustAuditFlowTests` 新增外部余额异常纠偏调账审计查询用例；Red 阶段证明缺少余额调账独立审计查询 application service、Query、DTO 和完整性状态。 |
| Green 实现 | 新增 `FundsBalanceAdjustmentAuditApplicationService`、`FundsBalanceAdjustmentAuditQuery`、`FundsBalanceAdjustmentAuditDTO`、`FundsBalanceAdjustmentAuditCompleteness` 和 `DefaultFundsBalanceAdjustmentAuditApplicationService`；实现以 `FundsTransactionQueryService.findFundsTransactionByBusiness` 或交易流水定位资金交易主事实，再只读聚合交易明细上下文、route snapshot、ledger transaction 和 LedgerEntry。 |
| 安全边界 | 审计查询过滤 `externalAccountRef`、PAN、CVV、secret、password、token 和 raw payload 等敏感上下文；只返回安全审计摘要，不返回完整外部账户或 processor 原始载荷。 |
| 完整性边界 | 目标测试覆盖交易事实和 route snapshot 存在但 ledger transaction / LedgerEntry 缺失的历史异常数据，按业务流水查询仍返回审计视图并标记 `INCOMPLETE_LEDGER`，不得误判为未找到。 |
| 资金副作用边界 | 查询服务不调用 posting、更新、删除、replay、生命周期 saver 或补事实入口；目标测试断言查询前后余额、账本交易和分录数量不变，不为不完整链路补写账本事实。 |
| 关联修复 | `FundsBenefitSpecValidators` 允许 contextVariables 中的精确 Money value object `{amount,currency}`，用于承载 `negativeAvailableSingleLimit` 等风控阈值，同时继续阻断顶层或非 Money 结构的核心权益金额字段。 |
| 验证命令 | `just test-one FundsBalanceAdjustAuditFlowTests tests` 非 sandbox 通过；sandbox 内该 Spring 测试因 embedded Redis 端口被拒绝失败，已按环境问题复跑确认。`just test-one DefaultRouteReplayServiceTests tests`、`just test-one LedgerDtoContextVariablesContractTests tests` 已通过；其余分组和静态门禁见本轮交付说明。 |
| Git 策略 | 本轮仍为 `summary_only`，未执行 Git 提交；后续如需提交需用户另行授权。 |
| Not Done | 不新增独立审计表、运营审批流、运营工单、权限模型、泛化运营补账、补事实执行、对账差错创建、清算/结算处理、真实资金操作、生产迁移或后台页面。 |
| 下一候选 | 本 Grant 不得复用继续扩审批或补事实；下一轮建议在 B7 清算/结算 gate 消费、B7 差异报告、wallet 完整预交易快照或其他单一 Grant 中重新确认。 |
