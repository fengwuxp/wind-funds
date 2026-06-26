# GSD-2 LD 预算组控制账本兼容退出 Execution Grant 确认包

## 1. 文档定位

本文是 `GSD2-LD-BUDGET-GROUP-CONTROL-COMPAT-EXIT-001` 的角色 Loop 确认包，用于裁决预算组 `BUDGET_GROUP` 是否还能以 `CONTROL` 类 `LIMIT` / `AVAILABLE` / `AUTHORIZATION` 分录进入 ledger。结论不是继续扩大兼容，而是把兼容路径拆成可迁移、可验证、可停止的工程切片。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-LD-BUDGET-GROUP-CONTROL-COMPAT-EXIT-001` |
| 原子任务 | 明确预算组控制账本兼容退出策略，并把下一步服务层编码拆成“控制视图替代优先、ledger 硬禁随后”的 TDD 切片。 |
| 所属阶段 | GSD-2 / LWT role loop / ledger subject guard / budget control compatibility exit。 |
| 当前状态 | `BUDGET_GROUP_LEDGER_COMPAT_CLEANUP_GREEN_VERIFIED` |
| Owner | 产品架构专家负责预算组和 Spend Rule 产品语义、验收和 Not Done；资深架构师负责 ledger 边界、服务契约、迁移顺序、TDD、Review 和验证；AI Native 流程编排负责上下文账本、Grant 串行和交接。 |
| 写入范围 | Phase 1 写 wallet-face application/spend 契约、Request/DTO、wallet-impl application/spend 实现、Spend Control Activity 控制事实字段、H2 测试 schema 和目标测试；Phase 2 写 ledger posting 主体准入、余额控制预算组旧入口拒绝、目标测试和状态文档；Phase 3 写预算组 ledger profile 清理、显式初始化拒绝、预算组余额查询拒绝、route replay 可入账主体守卫、DSL 夹具、H2 schema、目标测试和状态文档。 |
| 写入文件 | `wallet/wallet-face/src/main/java/com/wind/funds/wallet/application/spend/BudgetControlLimitAdjustmentApplicationService.java`、`wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/request/AdjustBudgetControlLimitRequest.java`、`wallet/wallet-face/src/main/java/com/wind/funds/wallet/model/dto/BudgetControlLimitAdjustmentResultDTO.java`、`wallet/wallet-impl/src/main/java/com/wind/funds/wallet/application/spend/impl/BudgetControlLimitAdjustmentApplicationServiceImpl.java`、`SpendControlMovement` 相关 Request/DTO/Entity/投影/枚举、`ledger/ledger-impl/src/main/java/com/wind/funds/ledger/DefaultLedgerTransactionPostingServiceImpl.java`、`ledger/ledger-impl/src/main/java/com/wind/funds/ledger/impl/DefaultLedgerProfileServiceImpl.java`、`ledger/ledger-impl/src/main/java/com/wind/funds/ledger/impl/DefaultSubjectLedgerInitializer.java`、`transaction/transaction-impl/src/main/java/com/wind/funds/transaction/converter/FundsBalanceControlInstructionConverter.java`、`transaction/transaction-impl/src/main/java/com/wind/funds/route/DefaultRouteReplayService.java`、`transaction/transaction-impl/src/main/java/com/wind/funds/route/support/RouteSubjectSupport.java`、`wallet/wallet-impl/src/main/java/com/wind/funds/wallet/impl/DefaultFundsAccountQueryServiceImpl.java`、`wallet/wallet-face` / `wallet/wallet-impl` 预算组模型、`core` DSL 契约、H2 schema、目标测试和状态文档。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger posting、wallet Spend Control、transaction balance control、现有预算组测试和 Git/code baseline。 |
| 只读参考 | `docs/产品设计/01-PRD总览.md`、`docs/DSL设计/README.md`、`docs/系分设计/02-交易路由钱包账目与投影系分设计.md`、`DefaultLedgerTransactionPostingServiceImpl`、`FundsBalanceControlInstructionConverter`、`DefaultLedgerProfileServiceImpl`、`SpendControlMovementApplicationService`、`DefaultLedgerTransactionPostingServiceImplTests`、`FundsBalanceControlFailureFlowTests`。 |
| 上下文账本 | [GSD-2-LWT-生产可用能力Goal.md](GSD-2-LWT-生产可用能力Goal.md)、OpenSpec `tdd-baseline-reset` tasks 和本确认包。 |
| Git 策略 | 本轮为 `summary_only`，已完成代码和文档验证但未执行 Git 提交；若用户要求提交，再按当前工作树、验证结果和提交范围单独处理。 |

## 2. 背景、目标和非目标

背景：PRD、DSL 和系分目标态已经确认预算组是预算 scope、控制视图和审计维度，不是核心资金账务主体。前序代码已经阻断预算组资金价值类 LedgerEntry，本轮在 Phase 1 补齐控制视图替代入口后，Phase 2 已移除预算组 `CONTROL` 类账本分录兼容入账。

业务目标：让预算额度、Spend Rule 控制和账户资金账务彻底分层。预算额度调整应落在控制活动和预算控制投影中，资金账户和信用账户继续承载 ledger fact、LedgerEntry、余额投影和对账事实。

用户价值：产品、运营、财务、风控和研发能清楚区分“预算可用控制”和“真实资金余额”，避免把预算组控制账本误读成资金池、负债、资产或可对账账本。

成功标准：

1. 给出预算组控制账本兼容的保留、退出和迁移裁决。
2. 明确下一轮代码 Grant 的首个 Red、写入范围、禁止事项、验证命令和停止条件。
3. 不把当前 `BUDGET_GROUP` 控制账本兼容测试解释为生产目标态 Done。
4. `BUDGET_GROUP` LedgerEntry 主体退出前已有可替代的控制活动 / 预算投影服务层闭环，退出后旧余额控制入口前置拒绝且无资金事实副作用。

非目标：

1. 本确认包不删除 `FundsSubjectType.BUDGET_GROUP`。
2. 本确认包不修改 Controller、HTTP/RPC、交易 canonical 入参、route resolver、posting assembler 结构、公共 API 删除、生产迁移或历史数据。
3. 本确认包不声明完整 Spend Rule 规则引擎、预算运营后台、VCC facade、清结算补事实或生产发布 Done。

## 3. 现状和影响范围

现状：

1. `DefaultLedgerTransactionPostingServiceImpl` 的资金价值入账主体白名单是 `FUNDING_ACCOUNT` 和 `CREDIT_ACCOUNT`。
2. 同一实现已移除 `BUDGET_GROUP` 的 `CONTROL` 类 `LIMIT` / `AVAILABLE` / `AUTHORIZATION` 例外，任意 `BUDGET_GROUP` LedgerEntry 主体均在事实落库前拒绝。
3. `FundsBalanceControlInstructionConverter` 只把 `CREDIT_ACCOUNT` 判定为 `LIMIT_ADJUST`，预算组旧余额控制调额入口会返回迁移提示并前置拒绝。
4. `BalanceControlFundsInstructionRouteResolver` 仍可服务资金账户 / 信用账户余额控制；预算额度调整目标入口已迁到 wallet spend application。
5. `DefaultLedgerProfileServiceImpl` 已不再暴露 `BUDGET_BASIC` 活跃 profile；显式 `SubjectLedgerInitializer` 遇到 `BUDGET_GROUP` 会 fail-fast。
6. `DefaultFundsAccountQueryServiceImpl` 不再把预算组解析为资金余额主体；预算组余额查询返回不可用语义。
7. `DefaultRouteReplayService` 和 DSL 契约均拒绝预算组作为可入账 route leg 参与方。
8. `DefaultLedgerTransactionPostingServiceImplTests` 已覆盖预算组资金价值分录和控制分录均拒绝，`FundsBalanceControlFailureFlowTests` 已覆盖预算组旧余额控制入口失败无资金事实副作用。

影响范围：

| 能力 | 当前风险 | 迁移影响 |
| --- | --- | --- |
| ledger posting | 新增写入已硬禁任意 `BUDGET_GROUP` LedgerEntry 主体。 | Phase 3 已清理 profile、初始化、查询、replay 和测试夹具旧语义。 |
| balance control | 历史预算额度调整曾复用交易路由和 ledger posting。 | 已迁到 wallet Spend Control 控制活动或预算控制投影服务；旧入口前置拒绝。 |
| wallet Spend Control | 已有控制活动、交易消费和预算投影基础。 | 可以承接预算额度调整控制事实，避免污染 ledger。 |
| 查询解释 | 预算额度和资金余额容易被同一套 ledger 查询混淆。 | 预算控制查询应使用控制投影，不走资金账本余额投影。 |

## 4. 角色 Loop 裁决

### 4.1 产品架构专家裁决

核心决策：预算组不是资金账户、不是信用账户、不是平台资金主体，也不是可对账 ledger subject。预算组只表达预算范围、规则归集、控制展示和审计追踪。即使是 `CONTROL` 类账目，也不应长期写成 LedgerEntry 主体。

能力地图：

| 能力域 | 前台能力 | 后台能力 | 数据能力 |
| --- | --- | --- | --- |
| 预算控制 | 配置预算范围、规则和额度。 | 记录额度调整、预占、消耗、释放、退款补偿。 | Spend Control Activity、Budget Control Projection。 |
| 资金账务 | 展示真实资金账户、信用账户余额。 | 过账、余额投影、对账和审计。 | LedgerTransaction、LedgerEntry、BalanceProjection。 |
| 查询解释 | 解释预算控制和交易资金事实的关系。 | 只读聚合控制活动、交易事实和 route snapshot。 | Projection explain、控制投影、交易投影。 |

业务对象和字段口径：

- `BudgetGroup`：预算 scope、展示和审计边界。
- `SpendRule`：授权前控制规则和决策证据。
- `SpendControlMovement`：控制事实，包括额度调整、预留、消耗、释放、退款补偿。
- `BudgetControlProjection`：只读控制投影，不替代资金余额。
- `LedgerEntry`：只能表达资金账户、信用账户或明确授权资金主体的账本分录。

业务流程：

1. 主流程：预算额度调整请求进入 wallet application 或 Spend Control application，记录控制活动，派生预算控制投影。
2. 交易流程：支付、授权、退款等资金事实继续走账户主体型交易内核，并只在交易后消费或释放控制活动。
3. 异常流程：控制活动写入失败不得创建资金交易、route、posting、LedgerEntry、账本交易或余额投影。
4. 人工兜底：历史预算组控制账本数据只作为兼容审计证据，不作为新增生产写入目标。

规则矩阵：

| 规则 | 触发条件 | 判断逻辑 | 优先级 | 版本 |
| --- | --- | --- | --- | --- |
| 预算组非 ledger subject | 任一 LedgerEntry 构建或入账 | `subjectType=BUDGET_GROUP` 最终必须拒绝。 | P0 | GSD2-LD |
| 控制活动替代 | 预算额度调整 | 记录 Spend Control Activity，更新 Budget Control Projection，不生成 ledger posting。 | P0 | GSD2-B5 |
| 兼容退出 | 历史余额控制预算额度调整 | 旧入口前置拒绝并提示迁移到预算控制活动，不生成资金或账本事实。 | P0 | GSD2-LD |
| 查询分层 | 查询预算或资金余额 | 预算看控制投影，资金看 ledger balance projection。 | P0 | GSD2-LWT |

运营后台、指标、报表和审计：预算额度调整报表应按预算组、Spend Rule、目标账户、控制活动类型和业务流水统计；财务资金报表只消费资金账户、信用账户、ledger transaction 和 LedgerEntry。两类报表可以关联展示，但不能合并为同一余额口径。

### 4.2 资深架构师裁决

核心决策：推荐 `staged-control-view-backed`。先补控制活动 / 预算投影替代路径，再移除 ledger posting 中的预算组控制账本例外。

取舍：

| 方案 | 结论 | 理由 |
| --- | --- | --- |
| 立即硬禁所有 `BUDGET_GROUP` LedgerEntry | Phase 1 前不推荐，Phase 2 已执行 | 替代服务层入口就绪前会打断兼容路径；控制视图替代完成后已可硬禁新增 ledger posting。 |
| 分阶段退出并先补控制视图 | 推荐 | 符合 PRD / DSL 目标态，也能避免一次性破坏兼容路径。 |
| 长期保留 `CONTROL` 账本兼容 | 不推荐 | 容易让预算组继续被误用为账务主体，影响对账、投影和生产可解释性。 |

接口契约和入参出参建议：

- 下一轮首选新增或扩展 wallet application / spend application 服务层能力，表达预算额度调整控制事实。
- 请求入参应包含 `tenantId`、`budgetGroupId`、`targetAccountId`、`amount`、`currency`、`increase`、`movementSn`、`businessScene`、`businessSn`、`reasonCode`、`operator` 和审计引用。
- 出参应返回控制活动流水、预算组、目标账户、调整方向、控制投影摘要和幂等结果。
- 错误码或异常必须区分预算组不存在、目标账户不一致、币种不一致、重复流水异摘要、金额非法和兼容路径禁用。
- 幂等键建议使用 `tenantId + movementSn`，同摘要回放返回既有控制活动，异摘要拒绝。
- 兼容口径：旧 `BUDGET_GROUP` ledger control entry 只作为迁移期兼容，不作为新调用方入口。

数据方案和一致性：

- 控制活动写入和预算投影派生在 wallet Spend Control 边界内闭合。
- 事务边界不得跨越到资金交易、route、posting、LedgerEntry 或余额投影。
- 对账层只对真实资金事实和外部清算事实对账；预算控制只做内部控制审计，不进入资金对账主链路。
- 若发现历史预算组控制账本数据，后续生产迁移需要独立 migration Grant 和回滚方案。

可靠性、安全、权限和审计：

- 预算额度调整属于高风险运营动作，服务层应要求操作者、原因码和证据引用。
- 权限模型、运营审批、告警和灰度不在本轮实现，但必须作为 Not Done 保留。
- 告警建议后续覆盖：控制投影为负、同一预算组高频调额、异摘要幂等冲突、历史 ledger/control 双写不一致。

## 5. 建议的分阶段 Execution Grant

本确认包原先给出推荐授权文本；用户已确认 `control-view-replacement-first`、`GSD2-LD-BUDGET-GROUP-CONTROL-POSTING-FORBID-001` 和 `GSD2-B2-BUDGET-GROUP-LEDGER-COMPAT-CLEANUP-001` 三段 Grant。当前已完成 Phase 1 控制视图替代、Phase 2 ledger posting 硬禁和 Phase 3 兼容清理。生产历史数据迁移、运营报表解释、权限、审批、告警和生产发布仍需独立 Grant。

### 5.1 推荐授权文本

`Execution Grant：GSD2-LD-BUDGET-GROUP-CONTROL-COMPAT-EXIT-001 / compatDecision=staged-control-view-backed / phase=control-view-replacement-first`

授权范围：只处理服务层预算额度调整从 `BUDGET_GROUP` ledger control entry 迁移到 Spend Control Activity / Budget Control Projection 的最小替代路径，允许写 wallet-face application/spend 契约、Request/DTO、wallet-impl application/spend 实现、目标测试、必要 H2 schema 字段和状态文档回写。

禁止事项：不得修改 Controller、HTTP/RPC、交易 canonical 入参、支付工具 `REFUND` 方向、直接交易/授权交易入参、清结算补事实、生产迁移、VCC facade、完整规则引擎或 Git 历史重写；不得在第一阶段删除 public API 或删除 `FundsSubjectType.BUDGET_GROUP`。

撤销方式：若首个 Red 证明必须先改 DDL、删除公共契约、改交易 canonical 入参或处理生产迁移，则停止本 Grant，回退到 confirmation 状态并重新拆分。

### 5.2 后续切片顺序

| 阶段 | Task ID | 目标 | 验收场景 |
| --- | --- | --- | --- |
| Phase 1 | `GSD2-B5-BUDGET-CONTROL-LIMIT-ADJUST-ACTIVITY-001` | 预算额度调整写控制活动和预算投影，不写资金账本。 | 已完成：成功调增、幂等回放、异摘要拒绝、调减低于已占用控制额拒绝、失败无资金事实和账本副作用。 |
| Phase 2 | `GSD2-LD-BUDGET-GROUP-CONTROL-POSTING-FORBID-001` | 移除 `DefaultLedgerTransactionPostingServiceImpl` 中的 `BUDGET_GROUP` 控制账本例外，并让预算组旧余额控制调额入口前置拒绝。 | 已完成：任意 `BUDGET_GROUP` LedgerEntry 主体均在事实落库前拒绝；旧余额控制入口失败无资金交易、route、posting、LedgerEntry、账本交易或余额投影副作用。 |
| Phase 3 | `GSD2-B2-BUDGET-GROUP-LEDGER-COMPAT-CLEANUP-001` | 清理 `BUDGET_BASIC` profile、显式 ledger 初始化、余额查询、route replay 和测试夹具中的旧语义。 | 已完成：兼容清单闭合，旧测试不再把预算组入账当目标态。 |

### 5.3 Phase 1 执行结果

本轮新增 `BudgetControlLimitAdjustmentApplicationService`，定位为 wallet application / spend 服务层入口，只表达预算控制额度调增和调减，不调用交易内核、route resolver、posting assembler 或 ledger posting。服务将预算组、目标账户、Spend Rule、金额、币种、原因码、操作者和审计引用记录为 Spend Control Activity，并通过 Budget Control Projection 派生 `limitAmount`、`availableControlAmount` 和既有占用控制额。

产品口径：预算额度调整是控制视图事实，不是资金余额调整；预算组仍不是核心账务主体。

架构口径：Phase 1 没有删除 `FundsSubjectType.BUDGET_GROUP`，没有修改交易 canonical 入参，没有改变支付工具方向规则，没有改 Controller、HTTP/RPC、生产迁移或 ledger posting 兼容例外。

TDD 证据：

1. 目标 Red 先证明缺少预算控制额度调额服务、请求和结果契约。
2. Green 后覆盖额度调增成功、同摘要幂等回放、异摘要冲突拒绝、调减后额度低于已占用控制额时 fail-fast 且无副作用。
3. 相邻回归覆盖原 Spend Control Activity 和 Spend Control Transaction Consumption 链路，证明既有预占、消耗、释放和退款控制补偿未被本轮投影字段污染。

### 5.4 Phase 2 执行结果

本轮消费 `GSD2-LD-BUDGET-GROUP-CONTROL-POSTING-FORBID-001`，移除 `DefaultLedgerTransactionPostingServiceImpl` 中的预算组 `CONTROL` 类入账例外。`POSTABLE_SUBJECT_TYPES` 继续只允许资金账户和信用账户，任意 `subjectType=BUDGET_GROUP` 的 LedgerEntry 都会在账本交易、posting plan、LedgerEntry 和余额投影落库前被拒绝。

同时，`FundsBalanceControlInstructionConverter` 已把预算组额度调整从旧余额控制入口退出：`adjust` 遇到 `BUDGET_GROUP` 时前置拒绝并提示迁移到预算控制活动，`LIMIT_ADJUST` 只保留信用账户。预算额度调增 / 调减的可用入口是 Phase 1 新增的 `BudgetControlLimitAdjustmentApplicationService`。

产品口径：预算组和 Spend Rule 只作为预算控制范围、规则归属、控制活动、预算控制投影和审计维度；不再作为新增 ledger entry 主体。

架构口径：Phase 2 没有删除 `FundsSubjectType.BUDGET_GROUP`，没有改 Controller、HTTP/RPC、交易 canonical 入参、支付工具方向规则、route resolver、posting assembler 结构、公共 API 删除或生产迁移。

TDD 证据：

1. `DefaultLedgerTransactionPostingServiceImplTests#testPostShouldRejectBudgetGroupControlEntriesBeforeLedgerFacts` 证明预算组 `CONTROL` 类 `LIMIT` / `AVAILABLE` 分录会在 ledger fact 落库前失败，且事实快照不变。
2. `FundsBalanceControlFailureFlowTests#testBudgetLimitAdjustShouldRejectLegacyBalanceControlEntry` 证明历史预算组调增入口前置拒绝且无资金交易和账本事实副作用。
3. `FundsBalanceControlFailureFlowTests#testBudgetLimitDecreaseShouldRejectLegacyBalanceControlEntryAndLeaveNoSideEffects` 证明历史预算组调减入口前置拒绝且无资金交易和账本事实副作用。
4. `BudgetControlLimitAdjustmentApplicationServiceTests` 继续证明替代入口可用：预算额度调整只写控制活动和预算控制投影，不写资金交易、route、posting、LedgerEntry、账本交易或余额投影。

### 5.5 Phase 3 执行结果

本轮消费 `GSD2-B2-BUDGET-GROUP-LEDGER-COMPAT-CLEANUP-001`，把预算组兼容路径从“仍可被显式建账、余额查询或 route replay 识别为账本参与方”收敛为“只保留枚举和控制上下文，不参与核心资金账本”。`BUDGET_BASIC` 不再作为活跃 ledger profile 暴露；`DefaultSubjectLedgerInitializer` 对 `BUDGET_GROUP` fail-fast；预算组 DTO / Entity / 创建请求不再承载 ledger profile 或 ledgerIds；H2 `t_budget_group` 测试 schema 移除 ledger profile 字段；资金主体余额查询不再把预算组解析为账本余额主体。

交易与 DSL 侧同步收敛：`RouteSubjectSupport` 不再为预算组创建可入账 `SubjectRef` 或 ledger profile；`DefaultRouteReplayService` 在 replay leg 选择后统一校验 source / target 均为资金账户或信用账户；DSL JSON 契约拒绝 route leg 节点使用 `BUDGET_GROUP`；多主体授权夹具只把预算组放在 `budgetControlScope` 控制上下文，不再作为 route participant、route leg 或 posting subject。

产品口径：预算组是预算 scope、控制窗口、控制活动、预算控制投影和审计解释，不是资金账户、信用账户、平台资金账户、账本主体或余额投影主体。

架构口径：Phase 3 没有删除 `FundsSubjectType.BUDGET_GROUP`，没有改 Controller、HTTP/RPC、交易 canonical 入参、支付工具方向规则、生产迁移或历史数据；只清理新增写入、显式初始化、余额查询、route replay 和 DSL 夹具中的账务主体化旧语义。

TDD 证据：

1. `ControlAccountLedgerInitializationTests` 覆盖预算组即使携带 profile / period 也不允许显式初始化账本，且 `BUDGET_BASIC` 不再是活跃账本初始化 profile。
2. `FundsSubjectBalanceQueryServiceImplTests` 覆盖预算组存在时，主体余额查询仍拒绝把它当账本余额主体。
3. `DefaultRouteReplayServiceTests` 覆盖历史 route snapshot 含预算组 route leg 时 replay 失败，不生成新的资金指令。
4. `FundsDslJsonContractTests` 覆盖 DSL route leg 中出现预算组节点会被契约校验拒绝。
5. `FundsDirectTransactionFlowTests` 和 `FundsAuthorizationTransactionFlowTests` 覆盖预算组作为直接交易或授权交易主体失败后，预算组余额投影仍未初始化且无资金或账本副作用。

## 6. TDD、Review 和 AI 产物复核

已消费 Red / Green：

1. `BudgetControlLimitAdjustmentApplicationServiceTests`：预算额度上调成功后，只新增控制活动和预算控制投影，不新增资金交易、route、posting、LedgerEntry、账本交易或余额投影。
2. 同一 `movementSn` 同摘要重复请求返回既有控制活动；异摘要重复请求拒绝且无资金副作用。
3. 调减后 `limitAmount` 低于当前已占用控制额时 fail-fast，失败路径不写控制活动或资金事实。

第二批 Red 已消费，Phase 3 兼容清理已消费：

1. `DefaultLedgerTransactionPostingServiceImplTests`：任意 `BUDGET_GROUP` LedgerEntry 主体被拒绝，包括 `CONTROL` 类 `LIMIT` / `AVAILABLE` / `AUTHORIZATION`。
2. `FundsBalanceControlFailureFlowTests`：预算组额度调整不再依赖交易路由和 ledger posting，旧入口返回迁移提示并受控拒绝。
3. `ControlAccountLedgerInitializationTests`、`FundsSubjectBalanceQueryServiceImplTests`、`DefaultRouteReplayServiceTests` 和 `FundsDslJsonContractTests`：清理 `BUDGET_BASIC` profile、显式 ledger 初始化、预算组余额查询、route replay 和历史测试夹具中的旧兼容语义。

最小断言清单：

- 控制活动事实可查询。
- 预算控制投影只读可解释。
- 失败无资金交易、route、posting、LedgerEntry、账本交易或余额投影副作用。
- 幂等回放同摘要返回同一事实。
- 异摘要和跨主体请求拒绝。
- ledger subject 中不出现 `BUDGET_GROUP`。

Review 重点：

1. wallet 只做预算控制应用层编排，不写资金交易事实。
2. transaction canonical 入参保持账户主体，不引入支付工具或预算组作为交易内核主体。
3. ledger 不再承接预算组控制视图新增写入。
4. Entity 字段按项目编码约规补中文注释。
5. default 方法、内存版实现、绕过 application facade 或测试夹具承载业务规则均视为阻断。

AI 产物复核：实现后必须核对 PRD、DSL、系分和 TDD 是否仍保持“预算组不入核心资金账本”的统一口径，不能让生成代码恢复预算组 ledger bucket、route leg 或余额查询目标态。

## 7. 验证方案

文档结构验证：

1. `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-2-LD-预算组控制账本兼容退出ExecutionGrant确认包.md`
2. `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-LD-预算组控制账本兼容退出ExecutionGrant确认包.md`
3. `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-LD-预算组控制账本兼容退出ExecutionGrant确认包.md`
4. `git diff --check`

代码验证候选：

1. `just test-one BudgetControlLimitAdjustmentApplicationServiceTests tests`
2. `just test-one SpendControlMovementApplicationServiceTests tests`
3. `just test-one DefaultLedgerTransactionPostingServiceImplTests tests`
4. `just test-one FundsBalanceControlFailureFlowTests tests`
5. `just test-ledger`
6. `just test-balance-control`
7. `just test-one ControlAccountLedgerInitializationTests tests`
8. `just test-one FundsSubjectBalanceQueryServiceImplTests tests`
9. `just test-one DefaultRouteReplayServiceTests tests`
10. `just test-one FundsDslJsonContractTests tests`
11. `just test-one FundsDirectTransactionFlowTests tests`
12. `just test-one FundsAuthorizationTransactionFlowTests tests`
13. `just compile`
14. `just pmd`

本轮已执行验证：

1. `just test-one BudgetControlLimitAdjustmentApplicationServiceTests tests`：4 tests 通过。
2. `just test-one SpendControlMovementApplicationServiceTests tests`：10 tests 通过。
3. `just test-one SpendControlTransactionConsumptionApplicationServiceTests tests`：23 tests 通过。
4. `just test-one DefaultLedgerTransactionPostingServiceImplTests tests`：13 tests 通过。
5. `just test-one FundsBalanceControlFailureFlowTests tests`：19 tests 通过。
6. `just test-one ControlAccountLedgerInitializationTests tests`：11 tests 通过。
7. `just test-one FundsSubjectBalanceQueryServiceImplTests tests`：12 tests 通过。
8. `just test-one DefaultRouteReplayServiceTests tests`：11 tests 通过。
9. `just test-one FundsDslJsonContractTests tests`：36 tests 通过。
10. `just test-one FundsDirectTransactionFlowTests tests`：51 tests 通过。
11. `just test-one FundsAuthorizationTransactionFlowTests tests`：32 tests 通过。
12. `just compile`：通过。
13. `just pmd`：通过。

待收口验证：完成文档状态回写后执行 `git diff --check`。

完成条件：目标测试、相邻回归、compile、pmd 和 diff 均通过；同时完成产品验收、架构 Review、TDD 证据、OpenSpec tasks 和 Goal 状态回写。

## 8. 风险、待确认和停止条件

风险：

1. Phase 1 替代入口、Phase 2 posting 硬禁和 Phase 3 兼容清理已完成，但生产历史数据如果已有预算组控制账本，需要单独迁移和报表解释。
2. 本轮不删除 `FundsSubjectType.BUDGET_GROUP`，它仍可作为预算控制上下文和历史兼容枚举存在，不能被重新解释为可入账主体。
3. 本轮只完成服务层和测试 schema 能力，不包含运营权限、审批流、告警、生产迁移或历史 ledger/control 双写差异处理。

待确认：

1. 生产迁移、灰度、权限和告警由后续 production-change 任务确认。
2. 是否删除或隐藏 public face 中历史资源型 CRUD、是否删除 `FundsSubjectType.BUDGET_GROUP` 枚举，需要独立兼容策略和调用方影响清单。

停止条件：

1. 需要改公共契约删除、DDL 生产迁移、Controller、HTTP/RPC、交易 canonical 入参或支付工具 `REFUND` 方向。
2. 首个 Red 必须依赖真实生产数据或外部通道才能成立。
3. 发现预算组仍被产品或系分定义为资金池、资产、负债或可对账资金主体。
4. 验证失败且无法在单一 Grant 授权范围内修复。

发布和回滚：本确认包不发布；后续代码阶段若涉及生产开关、灰度或历史数据迁移，必须另起 production-change 任务。服务层代码回滚应通过补偿提交完成，不得回滚已提交的 B5 Spend Control、预算组非建账或 ledger posting guard 证据。

## 9. 交接和恢复入口

交接要求：

1. 后续进入编码前，先读取本确认包、LWT Goal、OpenSpec tasks、PRD、DSL、系分和 TDD。
2. Phase 1 和 Phase 2 已消费，后续不得复用本 Grant 继续扩写公共契约、Controller、HTTP/RPC、生产迁移或历史数据清理。
3. 若继续本链路，下一轮只能进入生产迁移、历史数据解释、权限 / 审批 / 告警或公共契约兼容清理，必须另起新的单一 Grant。
4. 若改选 ledger application facade、wallet 授权准入组合或特殊业务能力地图，必须另起新的单一 Grant。

残余风险：当前代码仍保留 `FundsSubjectType.BUDGET_GROUP` 和预算组资源服务，用于预算控制上下文、历史数据和只读审计解释；它不是可入账主体。生产历史数据、报表口径、权限审批、告警和 public API 兼容清理仍未 Done。
