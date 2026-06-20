# GSD-2 P0/P1 Ledger-Wallet-Transaction 优先推进计划

## 1. 文档定位

本文是 `GSD2-W5-P0P1-LWT-PRIORITY-PLAN` 的工程编排计划卡，用于在 GSD-2 新基线下重新明确 ledger、wallet、transaction 三条被依赖能力的推进顺序、单一 Grant 候选、验证门禁和停止条件。

本文不是编码授权、测试写入授权、DDL/H2 schema 授权、公共契约变更授权或 Git 授权。它只在当前 Plan Grant 的低风险文档范围内落地优先级和执行计划。进入 Red/Green/CAD Loop 仍必须由用户确认单一 Execution Grant。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W5-P0P1-LWT-PRIORITY-PLAN` |
| 原子任务 | 重新对齐 ledger、wallet、transaction 的依赖顺序、Grant 队列、写入范围、验证命令和停止条件。 |
| 所属阶段 | GSD-2 Wave 5 / P0-P1 dependency planning / B5 transaction spend control activity concurrency green verified。 |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| 当前状态 | `SR_TRANSACTION_CONSUME_CONCURRENCY_GREEN_VERIFIED_COMMITTED` |
| 上游输入 | GSD-2 W1 基线差距审计、W2 单一 Grant 选择卡、W3 B2 账户层级 CAD 准入草案、W4 B2 Execution Grant 确认包、当前 PRD/DSL/系分/TDD/OpenSpec、支出控制准入快照已提交基线、B5 Spend Rule 控制活动已提交基线、预算组非建账已提交基线、支付工具授权准入和 route snapshot 回链提交、`GSD2-B7-RECON-GATE-CONSUME-002` 本地 Green 证据、`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`、`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001`、`GSD2-LD-LEDGER-GUARD-REGRESSION-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001 / scopeDecision=object-scope-schema-backed`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`、`GSD2-B7-RECON-DIFFERENCE-REPORT-001`、`GSD2-B5-SR-CONTROL-ACTIVITY-001` 和 `GSD2-B2-BUDGET-GROUP-NON-LEDGER-SUBJECT-001` 已消费证据、`a5b12a3f feat: 收敛资金服务层交付基线` 当前已提交代码基线、[GSD-2-LWT-生产可用能力Goal.md](GSD-2-LWT-生产可用能力Goal.md)、[GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md](GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md)、[GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md](GSD-2-B5-SpendRule控制活动与预算投影ExecutionGrant确认包.md)、[GSD-2-B7-对账差异报告ExecutionGrant确认包.md](GSD-2-B7-对账差异报告ExecutionGrant确认包.md)、[GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md](GSD-2-B7-清算结算Gate消费ExecutionGrant确认包.md)、[GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md](GSD-2-B7-清算结算真实消费方ExecutionGrant确认包.md)、[GSD-2-AUTH-Chargeback目标语义对齐任务卡.md](GSD-2-AUTH-Chargeback目标语义对齐任务卡.md)、[GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md](GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md) 和 [GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md](GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md)。 |
| Owner | AI Native 流程编排负责顺序和门禁；产品架构专家确认业务价值、验收和 Not Done；资深架构师确认源码锚点、写入范围、Red、验证命令和 CAD 停止条件；用户确认单一 Grant。 |
| 写入范围 | 本文、LWT 生产可用能力 Goal、B5 交易消费支出控制活动确认包、GSD-2 入口、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/GSD-2-LWT-生产可用能力Goal.md`、`docs/TDD设计/GSD-2-B5-交易消费支出控制活动ExecutionGrant确认包.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger、wallet、transaction、core、tests、Justfile、AGENTS.md、最近 Git 提交和旧 GSD/Grant 历史材料。 |
| Git 策略 | B7 差异报告已提交到 `a1397ddf`；支出控制准入快照已提交到 `021ee2ce`；Spend Rule 控制活动与预算控制投影已提交到 `78f7f008`；预算组非建账和资金服务层收敛已提交到 `a5b12a3f`；交易消费支出控制服务已提交到 `b29a5df`；本轮并发幂等硬化随本提交固化。后续新 Grant 默认 `summary_only`，除非用户再次明确授权提交。 |
| 服务层边界 | 当前 Goal 只推进服务层能力；B5 Spend Rule 控制活动与预算控制投影已交付服务层最小能力，不新增 Controller、HTTP/RPC、页面、导出端点或外部适配入口。 |

状态层级：`GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` 是 GSD-2 父 Goal，`GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` 是当前 LWT 子 Goal，当前代码基线以 `SR_TRANSACTION_CONSUME_CONCURRENCY_GREEN_VERIFIED_COMMITTED` 为准。`GSD2-B5-SR-TRANSACTION-CONSUME-001` 已完成服务层 Green / Verify / Commit，`GSD2-B5-SR-TRANSACTION-CONSUME-CONCURRENCY-001` 已补齐并发唯一键冲突同摘要回读既有活动的硬化证据；完整映射见 [GSD-2-LWT-生产可用能力Goal.md](GSD-2-LWT-生产可用能力Goal.md) 第 1.1 节。历史 B7、B2、AUTH 兼容 adapter、B4 remaining、wallet route snapshot、账户能力来源、预交易快照、支出控制准入快照、ledger guard、B5-002、B5-003、B7 对象级 Gate、B7 consumer、B7 差异报告、B5 控制活动、B5 交易消费、B5 交易消费并发幂等或预算组非建账状态只作为已消费证据，不作为下一步授权。

Wave 边界：本文最初只做任务规划和设计落地；用户后续已确认 `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 并进入首个 Red。Red 已证明当前授权服务流缺少合法账户层级来源；随后 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 补齐来源契约，`GSD2-B2-FR-TARGET-001` 采用 `ddl-backed targetSubjectType + targetSubjectId` 补齐资金责任目标主体，`GSD2-B2-WALLET-APPLICATION-FACADE-001` 补齐资金责任解析 facade，`GSD2-B2-WALLET-APPLICATION-FACADE-002 / B2-PI-CAP-CAD-001` 补齐支付工具能力准入 facade，`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` 补齐交易投影解释首轮只读查询能力，`GSD2-B5-BALANCE-ADJUST-AUDIT-001` 补齐外部余额异常纠偏和受控负可用调账的首轮审计准入，`GSD2-B7-RECON-DIFFERENCE-MVP-001` 补齐对账差错登记、处理回链、重新对账幂等和无资金副作用首轮闭环，`GSD2-B7-RECON-DIFFERENCE-MVP-002` 补齐差错处理动作白名单上下文守卫，`GSD2-B7-RECON-GATE-CONSUME-001` 补齐清算、结算和出款准入的只读差错消费决策，`GSD2-B7-RECON-GATE-CONSUME-002` 已把 gate 决策接入出款前准入消费方。上述已完成切片均保留 Not Done 边界；`GSD2-AUTH-CHARGEBACK-TARGET-ALIGN-001` 已补 contract/design-only 任务卡，确认 dispute / chargeback 是案件过程，资金结果默认由 `settleRefund` 争议字段承接，现有 `chargeback` 仅作为兼容资产；`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001` 已完成兼容说明、最小审计 guard、兼容测试和状态回写；`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002` 已完成授权争议退款和 remaining 矩阵；`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` 已完成支付工具授权准入最小服务流；`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` 已完成授权准入后的支付工具引用、绑定版本和准入决策 route snapshot 回链；`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001` 和 `GSD2-B2-SPEND-CONTROL-ADMISSION-001` 已完成账户能力来源、支付工具预交易快照和支出控制准入快照；`GSD2-LD-LEDGER-GUARD-REGRESSION-001` 已补 ledger 固定账目类别正常余额方向 guard 首轮；`GSD2-B5-BALANCE-ADJUST-AUDIT-002` 已补余额调账 route snapshot 审计回链；`GSD2-B5-BALANCE-ADJUST-AUDIT-003` 已补余额调账独立审计查询最小服务流；`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001` 已按对象级 schema-backed 完成差错阻断基座；`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001` 已完成清算 / 结算只读 consumer 服务流并提交到 `0d3f68dc`；`GSD2-B7-RECON-DIFFERENCE-REPORT-001` 已完成本地 Red / Green / Verify。下一步不得复用 B5-003、B7 对象级 Gate、B7 consumer、B7 差异报告或已完成 wallet 准入 Grant 扩审批、补事实、批次报告、导出、完整清结算、完整 Spend Rule 控制活动或预算投影，若继续推进需先确认新的单一 Grant。

运行时恢复口径：若当前会话 Goal objective、旧摘要或历史任务卡仍写着优先补齐 `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、B7 `scopeDecision` 待确认、清算 / 结算真实消费方待确认，或误写 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 仍待授权 / 待提交，以当前 Git/code baseline、LWT Goal 和 OpenSpec tasks 为准；这些切片已经完成 Green 并成为已消费证据，不得作为下一轮默认任务或授权依据。2026-06-19 已按恢复请求重跑授权准入目标测试 3 tests 和授权准入组合回归 40 tests，均在沙箱外通过，B2 授权准入只作为被依赖基线继续维护；后续 wallet 方向必须重新确认新的单一 Grant。

## 2. 编排结论

当前不再以 W4/B2 账户层级确认包作为下一候选；W4/B2 只保留为历史已消费证据。ledger、wallet、transaction 的优先推进顺序应按“被依赖方证据优先、真实服务流优先、当前单一 Grant 优先、清结算和 P2 后置”的原则收敛：

1. ledger 不重新开启大包改造，而作为每个资金变化切片的强制验证门禁：posting 绑定账本、账目、币种、余额投影、幂等和失败无副作用必须在目标测试或回归中证明。
2. wallet 的下一关键缺口不是支付工具 facade，而是账户层级和资金责任能否被真实服务流稳定消费；`GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 已补齐账户层级来源契约，`GSD2-B2-FR-TARGET-001` 首轮已补齐资金责任目标主体字段和资源服务校验。
3. transaction 的下一关键缺口不是新增支付工具交易入口，而是 route snapshot、原路径回放、交易投影解释和余额调账审计能否在账户主体型 canonical 内核下闭合。
4. B7 清结算与对账、P2 VCC / 全球账户 / 收单继续后置；它们只能消费已确认的 ledger、wallet、transaction 证据，不得平行实现资金内核。

## 3. 产品和架构对齐

业务目标：面向金融创业公司 MVP，优先把 VCC 发卡、VCC 交易处理和全球账户收付款依赖的资金底座能力做成可解释、可核对、可回归的生产级内核，而不是先扩业务 facade 或外部协议。

用户价值：产品、运营、财务、风控、研发和测试可以在同一条资金事实链路中解释“谁是入账主体、使用哪个钱包或信用账户、如何形成交易事实、如何落账、如何投影、如何失败无副作用”。

成功指标：任一进入编码的切片都能回链 AC/DSL/TDD/RED、真实 Spring Bean、H2/fixture、route snapshot、posting plan、LedgerEntry、余额投影、幂等、审计和验证命令。

非目标：不新增 `VCC_ACCOUNT`，不新增统一支付工具交易内核，不把支付工具、预算组、Spend Rule 或交易投影写成 ledger subject，不直接实现 VCC processor、卡组织、ACH、SWIFT、FX、银行协议或完整清结算对账。

| 模块 | 当前定位 | 下一步产品/工程价值 | 当前不做 |
| --- | --- | --- | --- |
| ledger | 账本事实、分录、账目绑定、余额投影和账务平衡的事实源。 | 作为所有 wallet / transaction 切片的强制验证门禁，必要时补独立 ledger guard Red。 | 不重新开启 GSD1 大包，不把父账户、支付工具、预算组或投影写成账务主体。 |
| wallet | 账户、支付工具、资金责任解析和钱包 application facade 的产品门面。 | 先证明账户层级和资金责任能被真实服务流消费，再推进资金责任目标主体和 application facade。 | 不让调用方绕过 application facade 拼资源服务，不先做 VCC facade。 |
| transaction | 资金交易事实、授权事实、余额控制、route snapshot、生命周期和交易投影。 | 先稳定账户主体型 canonical 内核、原快照回放、交易投影解释和余额调账审计。 | 不把 canonical 请求替换成支付工具引用，不新增统一 InstrumentTransactionService。 |

### 3.1 业务对象、流程和状态

- 业务对象：FundingAccount、CreditAccount、AccountHierarchy、WalletApplicationFacade、FundingResponsibilityResolution、FundsTransaction、RouteSnapshot、LedgerTransaction、LedgerEntry、BalanceProjection、TransactionProjection。
- 字段口径：本文最初作为计划卡不直接新增生产字段；后续已由 `GSD2-B2-FR-TARGET-001` 单一 Grant 消费并写入资金责任目标主体字段。后续再触及错误码、幂等摘要、枚举、投影字段或生产迁移脚本，仍必须单独升级 Execution Grant。
- 生命周期 / 状态：当前只固化任务准入状态，不新增交易状态机。交易事实仍遵循直接交易、授权交易、退款/撤销/重放、余额控制现有生命周期；账本事实仍以 ledger transaction 和 entry 为不可变事实。
- 业务流程：`B2-AH-RED-001` 已从“钱包入口 -> 账户层级/责任解析 -> 交易内核 -> 账本分录 -> 余额/交易投影”服务流验证到授权 route snapshot 可携带账户层级来源快照；`GSD2-B2-FR-TARGET-001` 首轮进一步证明资金责任关系可表达 `FUNDING_ACCOUNT` 和 `CREDIT_ACCOUNT` 目标主体，不再只依赖 `fundingAccountId`。
- 主流程：账户层级服务流快照和资金责任目标主体首轮通过后，依次推进钱包 application facade、交易投影解释、余额调整审计，ledger 作为每轮验证护栏。
- 异常流程：若发现预算组、支付工具、营销账户或外部工具被误当成核心账务主体，立即停止编码并回到设计/任务基线修正。
- 人工兜底：涉及兼容表结构、公开接口、清结算批次、真实资金调整、数据迁移或跨项目接入时，由用户重新确认单一 Execution Grant。
- 运营后台 / 数据口径：本计划不新增运营后台、指标、报表或导出能力；后续运营、审计、报表只消费交易事实、route snapshot、ledger entry、余额投影和交易投影解释形成的数据口径。

### 3.2 规则矩阵

| 规则项 | 触发条件 | 判断逻辑 | 优先级 | 版本 | 验证方式 | 确认方 |
| --- | --- | --- | --- | --- | --- | --- |
| 可入账主体 | 任一交易、余额控制或账本分录生成 | 只允许资金账户、信用账户或明确授权的平台/控制账户成为账务主体；预算组、支付工具、路由、Spend Rule 只能作为控制、路由或快照维度。 | P0 | W5 | B2-AH-RED-001 + ledger guard regression | 用户 + 架构 |
| Ledger 护栏 | 账本交易、分录、余额投影或交易投影变更 | 每笔资金变化必须可追溯到 ledger transaction、entry、posting plan 平衡和幂等事实。 | P0 | W5 | `test-ledger` / 相关 slice | 架构 |
| 多级账户快照 | 钱包、VCC、VA、平台账户等绑定子账户或父账户 | 子账户是实际资金责任和记账主体；父账户用于归集、限额、报表和授权范围，不替代子账户入账。 | P0 | W5 | B2-AH service-flow snapshot | 产品 + 架构 |
| 交易 canonical 入口 | 直接交易、授权交易、余额控制、退款/撤销/重放 | 内核保持账户主体/资金主体入参；支付工具入口只在 application facade 完成解析并固化快照。 | P1 | W5 | contract review + transaction slice | 架构 |
| 钱包 application facade | 外部调用需要开户、绑工具、资金能力解析或预交易快照 | facade 只编排能力和快照，不直接写交易事实或账本事实。 | P1 | W5 | wallet facade RED plan | 产品 + 架构 |
| P2 场景延后 | VCC 发卡、全球账户、清结算、代理分佣要求插队 | 只有 P0/P1 被依赖方能力通过准入后，才进入 P2 场景实现。 | P1 | W5 | Harness plan review | 用户 |

### 3.3 接口契约和数据边界

- 接口契约：本文最初不直接授权修改 `ledger-face`、`transaction-face`、`wallet-face` 的公开接口契约；`GSD2-B2-FR-TARGET-001` 已在授权范围内扩展 wallet 资金责任 Request/Query/DTO。后续不再基于本文直接新增或调整出参、错误码、幂等摘要、枚举和状态机。
- 兼容策略：若 B2-AH-RED-001 证明现有契约无法表达多级账户服务流，只允许在下一张 Grant 中提出兼容扩展方案，不在本计划卡中直接授权修改。
- 幂等要求：所有后续交易、余额控制、钱包 facade 和 ledger guard 的实现必须保留业务请求号、外部请求号、route snapshot、ledger transaction 与 entry 的可重放幂等链路。
- 数据方案：本文原始计划默认 no-ddl；`GSD2-B2-FR-TARGET-001` 已明确采用 `ddl-backed` 并修改 H2 schema。后续如需新增投影表、迁移脚本或生产 DDL，仍必须停止并单独申请数据 Grant。
- 事务边界：wallet 只做用例编排，transaction 生成交易事实和路由快照，ledger 生成账本事实和投影；不得跨层直接写事实表。
- 补偿 / 对账：异常资金调整、外部钱包/VCC 余额不一致、清结算差错和对账修复不在本计划直接实现，必须由后续 B5/B7 Grant 给出审计、补偿和对账闭环。

## 4. 依赖顺序和 Grant 队列

| 顺位 | Grant 候选 | 模块焦点 | 依赖关系 | 首批 Red / AC | 候选写入范围 | 验证门禁 | 不做范围 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `GSD2-B2-FR-TARGET-001` | wallet 资金责任解析 | 依赖账户层级快照服务流证据；该依赖已由来源契约 Green 提供局部证明。 | 支出主体资金责任从 `fundingAccountId` 升级到 `targetSubjectType + targetSubjectId`。 | 已首轮写入 Request/DTO/Query、Entity、H2 schema、资源服务校验和服务测试；route snapshot 决策、application facade 和平台角色解析仍后续承接。 | 资金责任解析测试、route snapshot 回归、敏感上下文回归、`compile`、`diff --check`。 | 不混入支付工具能力准入、Spend Rule 策略引擎或 P2 业务 facade。 |
| 2 | `GSD2-B2-WALLET-APPLICATION-FACADE-001` | wallet application facade | 依赖账户层级和资金责任目标主体口径。 | 首轮已补 `FundingResponsibilityResolutionApplicationService`，能把支出主体、主体类型、币种和关系类型解析为当前默认资金责任目标主体。 | 已写入 wallet-face application 契约、Request/DTO、wallet-impl application 实现和服务测试；支付工具能力、钱包账户聚合和预交易快照仍后续承接。 | 目标 wallet application 服务测试、`test-boundary`、`compile`、`pmd`、`diff`。 | 不让 transaction-impl 依赖 wallet 资源服务，不直接写交易、route、LedgerEntry 或余额投影，不把 facade 做成内存版业务 Service。 |
| 3 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001 / 002 / 002-REMAINING` | transaction 投影解释 | 依赖 route snapshot、账户层级和支付工具快照稳定。 | 已补只读投影解释 application service，能基于已持久化交易事实、RouteSnapshot 和交易明细解释 posted pay、declined authorization、缺失 RouteSnapshot fail-fast、授权争议退款、普通退款、无授权退款、授权释放/过期和兼容 chargeback。 | 已写入 transaction-face projection 查询契约、解释来源、transaction-impl 持久化事实解释实现和服务流测试；schema 变更需单独授权。 | 目标投影解释测试、原有投影发布回归、compile、pmd、diff。 | 不反写资金事实，不替代 ledger balance projection，不新增 projection store/DDL/治理重放，不声明失败态、差异报告或历史节点选择查询全量完成。 |
| 4 | `GSD2-B5-BALANCE-ADJUST-AUDIT-001` | transaction / balance control | 依赖 ledger guard 和交易事实审计。 | 首轮已证明外部余额异常、负可用余额纠偏必须携带来源类型、来源流水、原因、外部终局事件、外部余额快照、差错单和责任引用。 | 已写入 balance adjust 请求一等审计字段、外部异常来源类型、instruction context keys、转换器校验/透传和服务流测试。 | 目标余额调账审计测试、`test-balance-control`、`test-reconciliation`、compile、pmd、diff 已通过。 | 不绕过对账差错和白名单，不开放泛化运营补账；未新增独立审计表、运营审批流、B7 差错单闭环或 route snapshot 审计持久化。 |
| 5 | `GSD2-B7-RECON-DIFFERENCE-MVP-001` | reconciliation 差错闭环 | 依赖 B5 外部余额异常审计和 reconciliation 出款前准入候选。 | 首轮已证明对账差错必须登记来源质量、匹配强度、差异金额、责任方、阻断范围、规则版本和证据引用；处理后通过回链处理动作和重新对账关闭，且幂等冲突必须拒绝。 | 已写入 reconciliation-face application 契约、Request/DTO、差错枚举、reconciliation-impl Entity/Mapper/服务实现、H2 schema 和服务流测试；生产迁移和完整清结算消费需后续 Grant。 | 目标差错生命周期测试、`test-reconciliation`、`verify-fast`、compile、pmd、diff 已通过。 | 不直接生成资金事实，不修改历史交易、LedgerEntry、余额投影或交易投影；不开放完整清分、清算、结算、出款、追偿、账龄升级、运营后台或补事实白名单。 |
| 6 | `GSD2-B7-RECON-DIFFERENCE-MVP-002` | reconciliation 差错处理动作守卫 | 依赖 B7-001 差错对象、处理回链和重新对账闭环。 | 首轮已证明差错处理动作必须声明白名单动作类型、处理幂等键和原始事实引用；重复回链时动作、幂等键或原始事实引用漂移必须拒绝。 | 已写入 `ReconciliationDifferenceActionType`、link 请求、DTO、Entity、H2 schema、服务校验和服务流测试。 | 目标差错处理动作守卫测试、`test-reconciliation`、compile、pmd、diff。 | 不直接生成资金事实，不新增补事实执行服务，不修改交易、账本、余额投影、交易投影、清算、结算或出款对象。 |
| 7 | `GSD2-B7-RECON-GATE-CONSUME-001` | reconciliation 准入消费 | 依赖 B7-001 差错对象和 B7-002 动作守卫。 | 首轮已证明清算、结算或出款准入可以只读消费差错状态、阻断范围、处理动作和重跑结果；未闭环或重跑未对平必须阻断，已处理且重跑对平只能条件放行。 | 已写入 gate application 契约、Request/DTO、决策枚举、Mapper 只读查询、实现和服务测试；`test-reconciliation` 分组已纳入 Gate 测试。 | 目标 gate 测试、`test-reconciliation`、compile、pmd、diff。 | 不创建清算候选，不确认清算批次，不锁定结算，不提交出款，不生成资金事实或账本事实。 |
| 8 | `GSD2-B7-RECON-GATE-CONSUME-002` | reconciliation 出款准入消费 | 依赖 B7 gate application 和既有出款 preflight 候选服务。 | 首轮已证明出款前准入必须消费 `PAYOUT` 阻断范围差错：未闭环差错阻断，已处理且重跑对平的差错条件放行并保留证据。 | 已写入 `PayoutOrderServiceImpl` gate 调用、阻断原因映射、证据引用合并和 `PayoutPreflightServiceTests` 服务流回归。 | `PayoutPreflightServiceTests`、`test-reconciliation`、compile、pmd、diff。 | 不创建出款单，不调用通道，不处理外部回单，不新增出款表结构，不生成资金事实或账本事实。 |
| 9 | `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001` | wallet 授权准入 | 依赖资金责任解析 facade、支付工具能力准入 facade、账户能力查询和账户主体型授权内核。 | 新增 `AuthorizationAdmissionApplicationService` 或等价入口：工具、绑定、资金责任和账户能力通过后委派 `FundsAuthorizationTransactionService#authorize`；准入失败无资金事实；`approved=false` 授权拒绝只生成标准拒绝交易事实，route legs 为空且无 posting、LedgerEntry、projection 或余额副作用。 | wallet-face application 契约、Request/DTO、wallet-impl application 实现、目标服务测试和必要授权 flow 回归；默认不改交易 canonical 请求。 | `AuthorizationAdmissionApplicationServiceTests`、`PaymentInstrumentCapabilityApplicationServiceTests`、`FundingResponsibilityResolutionApplicationServiceTests`、`FundsAuthorizationTransactionFlowTests`、`test-boundary`、compile、pmd、diff。 | 不做 VCC facade、不做 Spend Rule 策略引擎、不替换 `FundsAuthorizationTransactionAuthorizeRequest.accountId`、不新增统一支付工具交易内核。 |
| 10 | `GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001` | wallet 授权准入快照回链 | 依赖授权准入 facade 已完成最小服务流。 | 支付工具授权准入成功后，route snapshot 顶层 `paymentInstrumentRef` 必须可审计支付工具引用、绑定版本、绑定角色、准入动作和准入决策；交易内核仍以账户主体为 canonical 入参。 | 先写授权准入目标 Red，证明 persisted route snapshot 缺少支付工具回链；Green 仅允许把可选支付工具快照写入授权资金指令、授权 route snapshot 和必要交易审计上下文。 | `AuthorizationAdmissionApplicationServiceTests`、`RouteSnapshotJsonSupportTests`、`FundsAuthorizationTransactionFlowTests`、compile、pmd、diff。 | 不替换 `FundsAuthorizationTransactionAuthorizeRequest.accountId`，不做 VCC facade、不做 Spend Rule 策略引擎、不新增统一支付工具交易内核，不改 DDL/H2 schema。 |
| 11 | `GSD2-LD-LEDGER-GUARD-REGRESSION-001` | ledger 回归门禁 | 已完成首轮 Green；后续资金变化切片继续伴随复跑。 | 固定账目类别的 `normalBalanceSide` 必须与类别默认方向一致；创建、入账和余额投影入口均需 fail-fast。 | 已写入 ledger guard、ledger service、posting service、projection service 和目标测试。 | 目标 ledger 测试、`test-ledger`、compile、pmd 已通过；收口执行 diff。 | 不重启 GSD1 大包，不做清结算或治理重放。 |
| 12 | `GSD2-B5-SR-TRANSACTION-CONSUME-001` / `GSD2-B5-SR-TRANSACTION-CONSUME-CONCURRENCY-001` | wallet / transaction 服务层控制活动消费 | 已完成本地 Green / Verify / Commit；依赖支出控制准入、支付工具预交易快照、Spend Control Activity、预算控制投影和账户主体型交易内核。 | 成功交易记录 `CONSUMED` 控制消耗；失败 / 拒绝 / 过期记录 `RELEASED`；已有退款资金事实记录 `REFUND_COMPENSATED` 控制补偿；缺原控制活动、金额超占用、币种 / 主体不一致或幂等摘要冲突 fail-fast；并发同流水同摘要唯一键冲突回读既有活动。 | 已写 wallet-face application/spend 契约、Request、必要活动类型、wallet-impl 服务实现、H2 schema / Entity 字段和目标服务流测试；并发硬化只改控制活动记录服务，不改支付工具 `REFUND` 能力方向。 | `SpendControlTransactionConsumptionApplicationServiceTests` 6 tests、`SpendControlActivityApplicationServiceTests` 6 tests、`SpendControlAdmissionApplicationServiceTests` 3 tests、compile、pmd、diff 已通过。 | 不复用本 Grant 扩交易 canonical 入参、统一支付工具交易内核、Controller、HTTP/RPC、事件消费者、完整规则引擎、VCC facade、生产迁移、ledger posting 或支付工具 `REFUND` 方向规则。 |

执行裁决：

1. `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 已完成本地 Green，不要重复消费旧 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 或来源契约 Grant。
2. `Execution Grant：GSD2-B2-FR-TARGET-001` 已进入首轮 Green，当前完成的是资源关系字段、H2 schema 和服务校验，不等于 wallet application facade、平台角色责任解析或完整 route snapshot 回放已完成。
3. `Execution Grant：GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` 已完成首轮 Green，当前完成的是只读解释查询和持久化事实回放，不等于投影存储、治理重放、差异报告或全交易类型解释矩阵完成。
4. `Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-001` 已完成首轮 Green，当前完成的是外部余额异常纠偏的交易入参审计、instruction context 透传、受控负可用策略字段和失败无副作用服务流测试，不等于独立运营审批、审计表或完整补偿闭环完成。
5. `Execution Grant：GSD2-B7-RECON-DIFFERENCE-MVP-001` 已完成首轮 Green，当前完成的是差错单登记、处理动作回链、重新对账幂等和无资金副作用，不等于完整清分、清算、结算、出款、追偿、账龄升级、运营后台、生产迁移或补事实白名单完成。
6. `Execution Grant：GSD2-B7-RECON-DIFFERENCE-MVP-002` 已完成首轮 Green，当前完成的是差错处理动作上下文守卫，不等于补事实命令执行服务、运营审批流、清算/结算/出款消费或真实资金修正闭环完成。
7. `Execution Grant：GSD2-B7-RECON-GATE-CONSUME-001` 已完成首轮 Green，当前完成的是只读准入决策，不等于清算、结算、出款消费方已经接入，也不等于补事实命令、运营审批或真实资金修正闭环完成。
8. `Execution Grant：GSD2-B7-RECON-GATE-CONSUME-002` 已完成首轮 Green，当前完成的是出款前准入消费 gate 决策，不等于完整出款单生命周期、通道回单、金额不一致处理、清算/结算消费方、补事实命令或运营审批完成。
9. 任一后续切片触碰 ledger posting、LedgerEntry、余额投影或 H2 schema 时，`GSD2-LD-LEDGER-GUARD-REGRESSION-001` 立即升级为前置或伴随 Red。

## 5. Coding Loop Contract 候选

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD2-P0P1-LWT-LOOP-2026-06-15` |
| 关联 Goal | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| 当前状态 | `SR_TRANSACTION_CONSUME_CONCURRENCY_GREEN_VERIFIED_COMMITTED` |
| 状态载体 | 本文、GSD-2 新基线工作流规划、LWT 生产可用能力 Goal、B5 交易消费支出控制活动确认包、AUTH Chargeback 目标语义任务卡、AUTH Chargeback 兼容入口确认包、W2/W3/W4 B2 账户层级文档、TDD README、docs README、OpenSpec tasks。 |
| 反馈源 | checker、`rg`、`git status --short`、`git diff --check`、目标测试、compile、pmd、用户确认。 |
| 验证者 | 产品语义由产品架构专家确认；工程边界由资深架构师确认；优先级和 Grant 由用户确认。 |
| 默认推进 | 当前来源契约、资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade、账户能力来源准入 facade、支付工具预交易快照 facade、支出控制准入 facade、授权准入 facade、授权 route snapshot 回链、交易投影解释、余额调账审计、对账差错闭环、差错处理动作上下文守卫、B7 准入消费、出款 preflight 首个消费方接入、ledger 正常余额方向 guard、B5 route snapshot 审计回链、B5 独立审计查询、B7 对象级 Gate 基座、B7 清算 / 结算 consumer、B7 差异报告、Spend Rule 控制活动、预算组非建账、交易消费支出控制活动和交易消费并发幂等硬化均已完成目标测试、相关回归、规约扫描和状态回写。当前没有可复用默认编码 Grant；下一轮需重新选择单一 Grant。 |
| 显式确认 | Java、测试、公共契约、DDL/H2、运行时配置、Git、联网、依赖安装、生产配置、真实资金、外部规则和专业合规确认。 |
| 无进展检测 | 连续两轮只是重复当前 B5-003 已消费证据、B7 候选或已消费 B2 / AUTH / B4 / ledger guard 口径而无新增状态证据、缺口收敛或用户确认时暂停；不得用历史 W4/B2、AUTH 兼容、B4 remaining、wallet 授权准入、ledger guard 或 B5-003 口径继续推进。 |
| 交接 | `GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`、`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001`、`GSD2-LD-LEDGER-GUARD-REGRESSION-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`、`GSD2-B7-RECON-DIFFERENCE-REPORT-001`、`GSD2-B5-SR-CONTROL-ACTIVITY-001`、`GSD2-B5-SR-TRANSACTION-CONSUME-001`、`GSD2-B5-SR-TRANSACTION-CONSUME-CONCURRENCY-001` 和 `GSD2-B2-BUDGET-GROUP-NON-LEDGER-SUBJECT-001` 已消费；交易消费切片仍保留 Not Done：完整规则引擎、事件消费、VCC facade、生产迁移、自动告警、补偿重试和支付工具 `REFUND` 方向裁决。若继续 wallet，需重新选择新的单一 Grant。 |

## 6. 停止条件

出现以下任一情况，本计划不得推进到代码：

1. 用户未确认单一 Execution Grant。
2. 需要 Java、测试、公共契约、DDL/H2 schema、Entity、Mapper、状态机或运行时配置，但 Grant 未列名授权。
3. 任务从 ledger / wallet / transaction 基础能力漂移到 B7 清结算、P2 VCC、全球账户、收单、外部通道或完整运营后台。
4. 发现支付工具、预算组、Spend Rule、交易投影或父账户被写成 ledger subject 的设计或代码倾向。
5. 需要联网、依赖安装、生产配置、真实资金、卡组织、银行、ACH、SWIFT、FX、税务、会计、法务或合规确认。
6. 工作树出现影响目标文件的用户未归属变更，或验证失败且无法在授权范围内修复。

## 7. Execution Handoff Card

| 字段 | 内容 |
| --- | --- |
| 当前 Wave / Task | 已消费的被依赖能力包括 wallet 授权准入、授权 route snapshot 回链、账户能力来源、支付工具预交易快照、支出控制准入、ledger guard、B5 余额调账审计、B7 gate / consumer / report、Spend Rule 控制活动、预算组非建账和交易消费支出控制活动。 |
| 当前状态 | `SR_TRANSACTION_CONSUME_CONCURRENCY_GREEN_VERIFIED_COMMITTED` |
| 下一建议 Grant | 暂无默认建议；下一轮需在完整 Spend Rule 规则引擎、事件消费 / outbox、VCC facade、B7 报告扩展、完整清结算生命周期、生产迁移或其他服务层单一切片中重新确认。 |
| 下一 Red | 需由下一 Grant 决定；不得复用 `SpendControlTransactionConsumptionApplicationServiceTests` 继续扩本 Grant。 |
| 写入范围 | 来源契约已写入 core port、wallet-face/impl、H2 schema、授权路由接入和目标测试；资金责任目标主体已写入 wallet Request/DTO/Query、Entity、H2 schema、资源服务校验和目标测试；wallet application facade 首轮已写入资金责任解析契约、支付工具能力准入契约、账户能力来源契约、支付工具预交易快照契约、支出控制准入契约、支付工具授权准入契约、Request/DTO、实现、route snapshot 回链和目标测试；ledger guard 首轮已写入 `LedgerNormalBalanceGuard`、账本创建、入账、余额投影入口和目标测试；B4 投影解释已写入 transaction-face projection 查询契约、解释来源、transaction-impl 查询实现和服务流测试；B5 余额调账审计已写入 balance adjust 请求审计字段、外部异常来源类型、instruction context keys、转换器校验/透传、route snapshot 审计摘要和服务流测试；B7 差错闭环已写入 reconciliation application 契约、Request/DTO、差错枚举、Entity/Mapper、H2 schema 和服务流测试；B7 准入消费已写入 reconciliation gate application 契约、Request/DTO、决策枚举、Mapper 只读查询、实现和服务测试；B7 出款准入消费已写入 `PayoutOrderServiceImpl` gate 调用、阻断原因映射、证据合并和服务流测试；B7 对象级 Gate 已写入差错阻断对象字段、H2 schema、Mapper 对象级查询、gate 返回字段和目标测试；B7 清算 / 结算 consumer 已写入 reconciliation-face 请求/结果/服务契约、reconciliation-impl 只读实现和目标服务流测试。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger、wallet、transaction、core、tests、Justfile、AGENTS.md、最近 Git 提交和历史准入卡。 |
| 验证命令 | wallet 授权准入、route snapshot 回链、账户能力来源、预交易快照和支出控制准入已执行目标服务流、组合回归、`compile`、`pmd`、`git diff --check` 和状态结构检查；ledger guard 已执行目标 ledger 测试、`test-ledger`、`compile`、`pmd` 和结构检查；B5-002 已执行目标 Red/Green、`FundsBalanceAdjustAuditFlowTests`、`test-balance-control`、`compile`、`pmd` 和 `git diff --check`；B5-003 已执行 `LedgerDtoContextVariablesContractTests`、`FundsBalanceAdjustAuditFlowTests`、`test-balance-control`、`test-boundary`、`test-transaction`、`compile` 和 `pmd`；B5 控制活动已执行 `SpendControlActivityApplicationServiceTests`、`SpendControlAdmissionApplicationServiceTests`、`compile`、`pmd` 和 `git diff --check`；B5 交易消费与并发幂等已执行 `SpendControlTransactionConsumptionApplicationServiceTests,SpendControlActivityApplicationServiceTests,SpendControlAdmissionApplicationServiceTests` 服务层组合回归、`compile`、`pmd`、`git diff --check` 和边界扫描；B7 对象级 Gate 和清算 / 结算 consumer 已执行目标测试、`test-reconciliation`、`compile` 和 `pmd`。B4 remaining、B5-001、B7 和 `e81a8a25 verify-cad` 保留为历史已消费验证证据；下一 Grant 后运行对应目标测试、`just compile`、`git diff --check`，必要时补分组测试和 `pmd`。 |
| Git 策略 | 当前 B5-002 已验证 Green 并提交固化到 `da3b4f19`；B5-003 已验证 Green 并提交固化到 `4ef64275`；B7 对象级 Gate 和清算 / 结算 consumer 已提交固化到 `0d3f68dc`；B7 差异报告已提交固化到 `a1397ddf`；支出控制准入快照已提交固化到 `021ee2ce`；B5 控制活动已提交固化到 `78f7f008`；本轮 B5 交易消费支出控制活动随本提交固化。后续新 Grant 默认 `summary_only`，除非用户再次明确授权提交。 |
| 交接要求 | 若用户确认下一 Grant，先读取对应确认包第 7.1 节 wallet application CR、Red / AC、验证矩阵、停止条件和可复制确认文本，再写最小 Red、最小 Green；完成后回写本文第 4 节 Grant 队列、OpenSpec tasks、验证证据、Not Done 和残余风险。 |

## 8. 验证矩阵

| 验证层 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| Harness 结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | Task、Owner、范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| 产品结构 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | 背景目标、现状约束、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-W5|P0P1-LWT|GSD2-AUTH-CHARGEBACK-TARGET-ALIGN|GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER|FundsAccountCapabilityApplicationServiceTests|PaymentInstrumentPreTransactionSnapshotApplicationServiceTests|SpendControlAdmissionApplicationServiceTests|GSD2-B2-SPEND-CONTROL-ADMISSION-001|GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001|GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001|LedgerNormalBalanceGuard|B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED|GSD2-B7-RECON-DIFFERENCE-REPORT-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001|blockingObjectType|blockingObjectSn" docs openspec` | GSD2 入口、AUTH Chargeback 目标语义任务卡、兼容入口确认包、B7 清算结算 Gate 确认包、B7 清算结算真实消费方记录、B7 对账差异报告 Green 结果、账户能力来源准入、预交易快照、支出控制准入快照、README 和 OpenSpec tasks 能追踪到本文、ledger guard Green 结果、B5-002 route snapshot 审计回链、B5-003 独立审计查询、B7 对象级 Gate 基座和清算 / 结算 consumer。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译、测试、PMD | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one SpendControlAdmissionApplicationServiceTests,PaymentInstrumentPreTransactionSnapshotApplicationServiceTests,PaymentInstrumentCapabilityApplicationServiceTests,FundingResponsibilityResolutionApplicationServiceTests,FundsAccountCapabilityApplicationServiceTests,AuthorizationAdmissionApplicationServiceTests tests`、`just compile`、`just pmd`、`git diff --check`。 | 支出控制准入目标测试、wallet application 组合回归、编译和 PMD 已通过；沙箱内 embedded Redis 端口限制已在非沙箱复跑确认。 |

## 9. Ledger-Wallet Loop 恢复裁决（2026-06-16）

本节只记录本轮进入 ledger / wallet 模块能力完善 loop 后的最终准入裁决，不替代新的单一 Execution Grant。

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD2-LD-WALLET-CAPABILITY-LOOP-2026-06-16` |
| 当前状态 | `FR_TARGET_SERVICE_FLOW_GREEN_VERIFIED_IN_E81A8A25` |
| 本轮输入 | 当前 GSD2 计划、OpenSpec tasks、wallet 资金责任关系服务、route funding allocation 快照契约、账户层级来源契约和当前工作树变更。 |
| 代码锚点 | `CreateSpendSubjectFundingRelationRequest`、`SpendSubjectFundingRelationDTO`、`SpendSubjectFundingRelationQuery`、`SpendSubjectFundingRel` 和 `t_spend_subject_funding_rel` 已新增 `targetSubjectType / targetSubjectId` 与 `target_subject_type / target_subject_id`；`fundingAccountId / funding_account_id` 保留为资金账户目标兼容字段。 |
| 裁决 | ledger profile 契约切片已具备本地 Green 证据；wallet 资金责任目标主体已选择 `targetSubjectType + targetSubjectId` 并完成首轮资源服务 Green。 |
| 已消费 Grant | `Execution Grant：GSD2-B2-FR-TARGET-001`。 |
| 已采用 schema gate | `ddl-backed`。新增 `target_subject_type`、`target_subject_id`、目标主体唯一键和查询索引，保留 `funding_account_id` 作为兼容字段。 |
| 首批 Green 证据 | 旧 `fundingAccountId` 兼容路径归一为 `FUNDING_ACCOUNT` 目标；`CREDIT_ACCOUNT` 目标可创建并查询；不可用信用账户和 `BUDGET_GROUP` 目标被拒绝；失败路径不写 relation，也不改 ledger facts。 |
| 候选写入上限 | 已消费 `wallet-face` Request/DTO/Query、`wallet-impl` Entity/service、`tests/src/test/resources/jdbc-schema.sql` 和资金责任服务测试；不触碰支付工具 facade、Spend Rule 策略引擎、VCC processor、清结算、对账、运行时配置或 Git。 |
| 验证门禁 | `just test-one SpendSubjectFundingRelationServiceImplTests tests`、目标 route snapshot 回归、敏感上下文阻断回归、`just compile`、`git diff --check`；若改公共契约或 schema，收口追加 `just test-boundary` 和 `just pmd`。 |
| 停止条件 | 用户未确认 `GSD2-B2-FR-TARGET-001`；schema gate 未确认；需要跨模块 public API 或生产数据迁移但 Grant 未列名；发现 BudgetGroup、Spend Rule、PaymentInstrument 或父账户被写成 ledger subject。 |

## 10. Full Gate 收口裁决（2026-06-17）

本节只记录 `ledger / wallet / transaction` 被依赖能力本轮收口结果，不替代后续新的单一 Execution Grant。

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD2-P0P1-LWT-FULL-GATE-2026-06-17` |
| Git / code baseline | `10853e2d feat: 收紧对账差错处理动作守卫`。 |
| 收口范围 | 账户层级来源契约、资金责任目标主体、资金责任解析 application facade、支付工具能力准入 application facade、交易投影解释、余额调账审计、对账差错闭环、差错处理动作守卫、相关 H2 schema、目标服务测试、边界测试、治理测试和 PMD。 |
| 验证证据 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just verify-cad` 已在 `e81a8a25` 通过；B7 动作守卫已在 `10853e2d` 完成 `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd` 和 `git diff --check` 验证。 |
| 当前状态 | `B7_RECON_DIFFERENCE_ACTION_GUARD_GREEN_VERIFIED`。 |
| 下一候选 | 清算/结算/出款准入消费差错状态、阻断范围、处理动作和重跑结果；若继续 B5，则为 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`。 |
| Not Done | 完整清分清算结算出款、追偿、账龄升级、运营后台、生产迁移脚本、B7 批次报告 / 导出、补事实命令执行服务、运营审批流、余额调账独立审计表/审批流、退款/no-auth refund/释放/拒付全量投影解释矩阵、projection store、治理重放、钱包账户聚合、完整 Spend Rule 控制活动、预算控制投影、VCC / 全球账户 / 清结算 P2 场景。 |
| 禁止误读 | 本轮 full gate 只证明已落地切片的工程门禁通过，不等于 wallet 全量生产 Done，不等于支付工具授权入口、VCC facade、清结算对账或 P2 业务已可交付。 |

## 11. LWT 生产可用能力 Goal 聚合裁决（2026-06-18）

本节记录用户要求“把建议聚合成 Goal，使用 loop + goal 推进”后的状态收口。该裁决不替代后续单一 Execution Grant，也不授权继续写 Java、测试、DDL/H2 schema、公共契约或 Git。

| 字段 | 内容 |
| --- | --- |
| Goal ID | `GSD2-GOAL-LWT-PRODUCTION-CAPABILITY-2026-06-18` |
| Loop ID | `GSD2-LWT-PRODUCTION-CAPABILITY-LOOP-2026-06-18` |
| Git / code baseline | 当前已提交代码基线为 `78f7f008 feat: 补齐支出控制活动与预算投影`；`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001 / scopeDecision=object-scope-schema-backed`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`、`GSD2-B7-RECON-DIFFERENCE-REPORT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001` 和 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 已完成 Red / Green / Verify 并提交固化。`a1397ddf feat: 补齐对账差异报告只读查询`、`4ef64275 feat: 补齐余额调账独立审计查询`、`da3b4f19 feat: 补齐余额调账路由审计回链` 和 `0b251593 feat: 补齐账本正常余额方向护栏` 保留为已消费能力证据，`dd442888` 和 `ca603eab` 保留为上一已消费证据。 |
| 新增状态载体 | [GSD-2-LWT-生产可用能力Goal.md](GSD-2-LWT-生产可用能力Goal.md)。 |
| 裁决 | 当前不再把 `GSD2-B7-RECON-GATE-CONSUME-002`、`GSD2-AUTH-CHARGEBACK-COMPAT-ADAPTER-001`、`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-002-REMAINING`、`GSD2-B2-WALLET-AUTHORIZATION-ADMISSION-001`、`GSD2-B2-WALLET-AUTHORIZATION-ROUTE-SNAPSHOT-001`、`GSD2-LD-LEDGER-GUARD-REGRESSION-001`、`GSD2-B5-BALANCE-ADJUST-AUDIT-002`、`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`、`GSD2-B7-RECON-DIFFERENCE-REPORT-001` 或 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 作为下一候选；这些切片均已完成并成为已消费证据。下一轮必须重新确认新的单一 Grant。 |
| 基线判定 | `GSD-2-LWT-生产可用能力Goal.md` 第 5.5 节已把当前能力定为 `CONDITIONAL_DELIVERABLE_BASELINE`：可作为上层 MVP 后续单一 Grant 的被依赖能力继续消费，但不等于 ledger、wallet、transaction 或 B7 清结算全量生产 Done。 |
| 可消费证据 | 账户主体、账户层级来源、资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade、账户主体型交易内核、route snapshot、ledger guard、余额投影、B4 首轮交易投影解释、B5 首轮余额调账审计、B7 差错 gate 和出款 preflight 消费。 |
| 不可外推范围 | 完整 Spend Rule 规则引擎、交易消费控制活动、VCC facade、失败态全量解释、projection store、独立 ledger guard 回归包、B7 报告扩展、完整清分 / 清算 / 结算生命周期、补事实命令执行、运营审批、生产迁移、灰度、告警、VCC / 全球账户 / 收单 P2 业务。 |
| Completion Audit | `GSD-2-LWT-生产可用能力Goal.md` 第 11 节已补完成度审计和三卡交接：状态载体、完备性矩阵和条件基线为 `DONE_DOCS_ONLY`；AUTH 兼容 adapter 和 B4 争议退款投影解释为已消费 Green；三模块全量生产 Done、完整投影解释矩阵、完整清结算和 P2 业务仍为 `NOT_DONE`。 |
| Evidence Anchors | `GSD-2-LWT-生产可用能力Goal.md` 第 11.3 节已把条件基线映射到 wallet / transaction / ledger / reconciliation 的 face、impl、测试类和已记录验证命令；后续任何编码切片必须复跑目标测试，不得只凭锚点矩阵声明 Done。 |
| Structure Gates | `GSD-2-LWT-生产可用能力Goal.md` 第 11.4 节已记录 GSD-2 总入口、LWT Goal、P0/P1 LWT 推进计划、AUTH Chargeback 目标语义任务卡和 AUTH Chargeback 兼容入口确认包的 Harness、产品和架构结构 checker 通过结果；该结果只证明文档入口可消费，不替代 Java 编译、目标测试、PMD、Git 授权或生产准出。 |
| Grant Decision Ledger | `GSD-2-LWT-生产可用能力Goal.md` 第 8.1 节已补单一 Grant 决策账本：`GSD2-B5-BALANCE-ADJUST-AUDIT-003`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001`、`GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001`、`GSD2-B7-RECON-DIFFERENCE-REPORT-001`、`GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001`、`GSD2-B2-WALLET-PRE-TRANSACTION-SNAPSHOT-001`、`GSD2-B2-SPEND-CONTROL-ADMISSION-001` 和 `GSD2-B5-SR-CONTROL-ACTIVITY-001` 已消费；当前没有默认确认包，下一轮必须重新确认新的单一 Grant。 |
| B5-003 确认包 | [GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md](GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md) 已把首个子切片收敛为“余额调账独立审计查询最小服务流”，不选择运营审批闭环，并在第 13 节记录 Red/Green、验证命令、敏感字段安全、无资金副作用、Not Done 和下一候选；本 Grant 已消费，不得复用为审批、补事实或清结算授权。 |
| B5 Grant Consume Precheck / Run Card | `GSD-2-B5-余额调账审计扩展ExecutionGrant确认包.md` 第 9 至 11 节保留为 B5-003 历史消费预检和运行卡；第 13 节记录实际消费结果。 |
| AUTH Grant Consume Precheck / Run Card | `GSD-2-AUTH-Chargeback兼容入口ExecutionGrant确认包.md` 第 11 至 12 节保留为 AUTH 兼容 Grant 的历史消费预检和运行卡；该 Grant 已消费，不得复用为 B5-003 授权。 |
| 下一候选顺序 | 若继续 wallet，可在交易消费控制活动、完整 Spend Rule 规则引擎或 VCC facade 中择一建立新的单一 Grant；若改选 B7 报告扩展，也必须重新建立对应单一 Grant。 |
| 当前可默认推进 | 低风险文档同步、状态回写、只读 Gap Audit、Spec/AC/Harness/CAD 任务卡和验证矩阵。 |
| 显式确认后才可推进 | Java、测试、DDL/H2 schema、公共契约、Entity、Mapper、状态机、运行时配置、Git、联网、真实资金、外部规则和专业合规确认。 |
| 验证命令 | `rg "GSD2-LWT|LWT-PRODUCTION|单一 Grant 决策账本|结构门禁验证证据账本|FundsAccountCapabilityApplicationServiceTests|GSD2-B2-ACCOUNT-CAPABILITY-SOURCE-001|B7_RECON_DIFFERENCE_REPORT_GREEN_VERIFIED_COMMITTED|GSD2-B7-RECON-DIFFERENCE-REPORT-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-GATE-CONSUME-001|GSD2-B7-RECON-CLEARING-SETTLEMENT-CONSUMER-SERVICE-001|blockingObjectType|blockingObjectSn" docs openspec`、`git diff --check`。 |

## 12. B5 交易消费支出控制活动 Grant 消费记录（2026-06-20）

本节记录 `GSD2-B5-SR-TRANSACTION-CONSUME-001` 的实际执行结果，不替代后续新的单一 Execution Grant。

| 字段 | 内容 |
| --- | --- |
| Execution Grant | `GSD2-B5-SR-TRANSACTION-CONSUME-001`。 |
| schemaDecision / refundActionDecision | `activity-schema-backed` / `control-compensation-only`。 |
| 写入范围 | `wallet-face` application 契约和请求、`wallet-impl` application 实现与控制活动模型扩展、`core` 控制活动类型、H2 测试表字段、目标服务流测试和任务证据回写。 |
| 已完成能力 | 交易成功后记录 `CONSUMED` 控制活动；失败 / 拒绝 / 过期交易可记录 `RELEASED`；已有退款资金事实后可记录 `REFUND_COMPENSATED` 控制补偿；活动回链原预留活动和资金交易流水；预算控制投影可解释 `consumedAmount`。 |
| 禁止范围确认 | 未写 Controller、HTTP/RPC、统一支付工具交易内核、支付工具 `REFUND` 方向重裁决、交易 canonical 入参改造、route resolver、route replay、posting assembler、ledger posting、生产迁移或 Git push。 |
| 验证证据 | `SpendControlTransactionConsumptionApplicationServiceTests` 5 tests、`SpendControlActivityApplicationServiceTests` 6 tests、`SpendControlAdmissionApplicationServiceTests` 3 tests、`just compile`、`just pmd` 和 `git diff --check` 均通过。 |
| 本 Grant 状态 | `SR_TRANSACTION_CONSUME_GREEN_VERIFIED_COMMITTED`。 |
| Not Done | 完整 Spend Rule 规则引擎、外部事件消费 / outbox、运营后台、生产 DDL、历史补数、自动告警、补偿重试、VCC facade 和清结算补事实。 |

## 13. B5 交易消费并发幂等硬化记录（2026-06-20）

本节记录 `GSD2-B5-SR-TRANSACTION-CONSUME-CONCURRENCY-001` 的实际执行结果。该切片只补交易消费控制活动记录的并发幂等硬化，不重新打开交易 canonical 入参、支付工具 `REFUND` 方向或 ledger posting。

| 字段 | 内容 |
| --- | --- |
| Execution Grant | `GSD2-B5-SR-TRANSACTION-CONSUME-CONCURRENCY-001`。 |
| 写入范围 | `wallet-impl` 控制活动记录服务最小实现、交易消费目标服务流并发测试、本文、LWT Goal、TDD 清单和 OpenSpec tasks 状态同步。 |
| 已完成能力 | 两个线程并发消费同一 `tenantId + activitySn` 且摘要相同时，唯一键冲突方读取既有活动并比对摘要，同摘要返回同一条 `CONSUMED` 活动，不生成重复活动或资金事实。 |
| 禁止范围确认 | 未写 Controller、HTTP/RPC、统一支付工具交易内核、支付工具 `REFUND` 方向重裁决、交易 canonical 入参改造、route resolver、route replay、posting assembler、ledger posting、生产迁移或 Git push。 |
| 验证证据 | `SpendControlTransactionConsumptionApplicationServiceTests`、`SpendControlActivityApplicationServiceTests`、`SpendControlAdmissionApplicationServiceTests` 服务层组合回归 15 tests 通过；`just compile`、`just pmd`、`git diff --check` 和边界扫描通过。其中并发用例先在非沙箱环境 Red 为 `DuplicateKeyException`，Green 后同摘要回读同一活动事实。 |
| 当前状态 | `SR_TRANSACTION_CONSUME_CONCURRENCY_GREEN_VERIFIED_COMMITTED`。 |
| Not Done | 完整 Spend Rule 规则引擎、事件消费 / outbox、自动告警、补偿重试、运营后台、生产 DDL、历史补数、VCC facade 和清结算补事实。 |
