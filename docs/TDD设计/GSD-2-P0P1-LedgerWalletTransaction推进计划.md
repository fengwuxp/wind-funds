# GSD-2 P0/P1 Ledger-Wallet-Transaction 优先推进计划

## 1. 文档定位

本文是 `GSD2-W5-P0P1-LWT-PRIORITY-PLAN` 的工程编排计划卡，用于在 GSD-2 新基线下重新明确 ledger、wallet、transaction 三条被依赖能力的推进顺序、单一 Grant 候选、验证门禁和停止条件。

本文不是编码授权、测试写入授权、DDL/H2 schema 授权、公共契约变更授权或 Git 授权。它只在当前 Plan Grant 的低风险文档范围内落地优先级和执行计划。进入 Red/Green/CAD Loop 仍必须由用户确认单一 Execution Grant。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD2-W5-P0P1-LWT-PRIORITY-PLAN` |
| 原子任务 | 重新对齐 ledger、wallet、transaction 的依赖顺序、Grant 队列、写入范围、验证命令和停止条件。 |
| 所属阶段 | GSD-2 Wave 5 / P0-P1 dependency planning / full gate verified。 |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| 当前状态 | `B7_RECON_DIFFERENCE_ACTION_GUARD_GREEN_VERIFIED` |
| 上游输入 | GSD-2 W1 基线差距审计、W2 单一 Grant 选择卡、W3 B2 账户层级 CAD 准入草案、W4 B2 Execution Grant 确认包、当前 PRD/DSL/系分/TDD/OpenSpec 和 Git/code baseline `10853e2d`。 |
| Owner | AI Native 流程编排负责顺序和门禁；产品架构专家确认业务价值、验收和 Not Done；资深架构师确认源码锚点、写入范围、Red、验证命令和 CAD 停止条件；用户确认单一 Grant。 |
| 写入范围 | 本文、GSD-2 入口、TDD README、docs README 和 OpenSpec tasks 的状态同步。 |
| 写入文件 | `docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md`、`docs/TDD设计/GSD-2-新基线工作流规划.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger、wallet、transaction、core、tests、Justfile、AGENTS.md、最近 Git 提交和旧 GSD/Grant 历史材料。 |
| Git 策略 | `summary_only`。本文不授权 `git add`、`git commit`、push、PR、merge、rebase、reset 或分支切换。 |

Wave 边界：本文最初只做任务规划和设计落地；用户后续已确认 `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 并进入首个 Red。Red 已证明当前授权服务流缺少合法账户层级来源；随后 `GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 补齐来源契约，`GSD2-B2-FR-TARGET-001` 采用 `ddl-backed targetSubjectType + targetSubjectId` 补齐资金责任目标主体，`GSD2-B2-WALLET-APPLICATION-FACADE-001` 补齐资金责任解析 facade，`GSD2-B2-WALLET-APPLICATION-FACADE-002 / B2-PI-CAP-CAD-001` 补齐支付工具能力准入 facade，`GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` 补齐交易投影解释首轮只读查询能力，`GSD2-B5-BALANCE-ADJUST-AUDIT-001` 补齐外部余额异常纠偏和受控负可用调账的首轮审计准入，`GSD2-B7-RECON-DIFFERENCE-MVP-001` 补齐对账差错登记、处理回链、重新对账幂等和无资金副作用首轮闭环，`GSD2-B7-RECON-DIFFERENCE-MVP-002` 补齐差错处理动作白名单上下文守卫。上述已完成切片均保留 Not Done 边界；下一步应重新确认清算/结算/出款准入如何消费差错状态和阻断范围，或确认 `GSD2-B5-BALANCE-ADJUST-AUDIT-002`。

## 2. 编排结论

当前仍不应跳过 W4 已收敛的 B2 账户层级服务流确认包。ledger、wallet、transaction 的优先推进顺序应按“被依赖方证据优先、真实服务流优先、清结算和 P2 后置”的原则收敛：

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
| 3 | `GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` | transaction 投影解释 | 依赖 route snapshot、账户层级和支付工具快照稳定。 | 首轮已补只读投影解释 application service，能基于已持久化交易事实、RouteSnapshot 和交易明细解释 posted pay、declined authorization 和缺失 RouteSnapshot fail-fast。 | 已写入 transaction-face projection 查询契约、解释来源、transaction-impl 持久化事实解释实现和服务流测试；schema 变更需单独授权。 | 目标投影解释测试、原有投影发布回归、compile、pmd、diff。 | 不反写资金事实，不替代 ledger balance projection，不新增 projection store/DDL/治理重放，不覆盖退款、no-auth refund、释放、拒付全量解释场景。 |
| 4 | `GSD2-B5-BALANCE-ADJUST-AUDIT-001` | transaction / balance control | 依赖 ledger guard 和交易事实审计。 | 首轮已证明外部余额异常、负可用余额纠偏必须携带来源类型、来源流水、原因、外部终局事件、外部余额快照、差错单和责任引用。 | 已写入 balance adjust 请求一等审计字段、外部异常来源类型、instruction context keys、转换器校验/透传和服务流测试。 | 目标余额调账审计测试、`test-balance-control`、`test-reconciliation`、compile、pmd、diff 已通过。 | 不绕过对账差错和白名单，不开放泛化运营补账；未新增独立审计表、运营审批流、B7 差错单闭环或 route snapshot 审计持久化。 |
| 5 | `GSD2-B7-RECON-DIFFERENCE-MVP-001` | reconciliation 差错闭环 | 依赖 B5 外部余额异常审计和 reconciliation 出款前准入候选。 | 首轮已证明对账差错必须登记来源质量、匹配强度、差异金额、责任方、阻断范围、规则版本和证据引用；处理后通过回链处理动作和重新对账关闭，且幂等冲突必须拒绝。 | 已写入 reconciliation-face application 契约、Request/DTO、差错枚举、reconciliation-impl Entity/Mapper/服务实现、H2 schema 和服务流测试；生产迁移和完整清结算消费需后续 Grant。 | 目标差错生命周期测试、`test-reconciliation`、`verify-fast`、compile、pmd、diff 已通过。 | 不直接生成资金事实，不修改历史交易、LedgerEntry、余额投影或交易投影；不开放完整清分、清算、结算、出款、追偿、账龄升级、运营后台或补事实白名单。 |
| 6 | `GSD2-B7-RECON-DIFFERENCE-MVP-002` | reconciliation 差错处理动作守卫 | 依赖 B7-001 差错对象、处理回链和重新对账闭环。 | 首轮已证明差错处理动作必须声明白名单动作类型、处理幂等键和原始事实引用；重复回链时动作、幂等键或原始事实引用漂移必须拒绝。 | 已写入 `ReconciliationDifferenceActionType`、link 请求、DTO、Entity、H2 schema、服务校验和服务流测试。 | 目标差错处理动作守卫测试、`test-reconciliation`、compile、pmd、diff。 | 不直接生成资金事实，不新增补事实执行服务，不修改交易、账本、余额投影、交易投影、清算、结算或出款对象。 |
| 7 | `GSD2-LD-LEDGER-GUARD-REGRESSION-001` | ledger 回归门禁 | 可作为资金变化切片的伴随 guard；若发现 ledger 缺口，则升级为独立 Grant。 | entry 与 bound ledger 的 subject、账目、币种、normal side、allow negative、余额投影和幂等必须一致。 | ledger 目标测试或回归测试；默认不改生产代码。 | `test-ledger`、目标 ledger 测试、compile、diff。 | 不重启 GSD1 大包，不做清结算或治理重放。 |

执行裁决：

1. `Execution Grant：GSD2-B2-ACCOUNT-HIERARCHY-SOURCE-CONTRACT-002` 已完成本地 Green，不要重复消费旧 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001` 或来源契约 Grant。
2. `Execution Grant：GSD2-B2-FR-TARGET-001` 已进入首轮 Green，当前完成的是资源关系字段、H2 schema 和服务校验，不等于 wallet application facade、平台角色责任解析或完整 route snapshot 回放已完成。
3. `Execution Grant：GSD2-B4-TRANSACTION-PROJECTION-EXPLAIN-001` 已完成首轮 Green，当前完成的是只读解释查询和持久化事实回放，不等于投影存储、治理重放、差异报告或全交易类型解释矩阵完成。
4. `Execution Grant：GSD2-B5-BALANCE-ADJUST-AUDIT-001` 已完成首轮 Green，当前完成的是外部余额异常纠偏的交易入参审计、instruction context 透传、受控负可用策略字段和失败无副作用服务流测试，不等于独立运营审批、审计表或完整补偿闭环完成。
5. `Execution Grant：GSD2-B7-RECON-DIFFERENCE-MVP-001` 已完成首轮 Green，当前完成的是差错单登记、处理动作回链、重新对账幂等和无资金副作用，不等于完整清分、清算、结算、出款、追偿、账龄升级、运营后台、生产迁移或补事实白名单完成。
6. `Execution Grant：GSD2-B7-RECON-DIFFERENCE-MVP-002` 已完成首轮 Green，当前完成的是差错处理动作上下文守卫，不等于补事实命令执行服务、运营审批流、清算/结算/出款消费或真实资金修正闭环完成。
7. 任一后续切片触碰 ledger posting、LedgerEntry、余额投影或 H2 schema 时，`GSD2-LD-LEDGER-GUARD-REGRESSION-001` 立即升级为前置或伴随 Red。

## 5. Coding Loop Contract 候选

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD2-P0P1-LWT-LOOP-2026-06-15` |
| 关联 Goal | `GSD2-GOAL-PRODUCTION-FUNDS-BASELINE-2026-06-12` |
| 当前状态 | `B7 reconciliation difference action guard green / next clearing-payout consumption or audit persistence expansion` |
| 状态载体 | 本文、GSD-2 新基线工作流规划、W2/W3/W4 B2 账户层级文档、TDD README、docs README、OpenSpec tasks。 |
| 反馈源 | checker、`rg`、`git status --short`、`git diff --check`、目标测试、compile、pmd、用户确认。 |
| 验证者 | 产品语义由产品架构专家确认；工程边界由资深架构师确认；优先级和 Grant 由用户确认。 |
| 默认推进 | 当前来源契约、资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade、交易投影解释、余额调账审计、对账差错闭环和差错处理动作上下文守卫首轮 Green 已完成目标测试、分组回归、规约扫描和状态回写。下一轮默认进入 B7 差错状态消费，让清算、结算或出款准入读取差错阻断范围、处理动作和重跑结果；或继续扩展 B5 审计持久化/route snapshot 回链。若继续 wallet，需要另行确认钱包账户聚合、账户能力来源、授权 admission 或完整预交易快照子切片。 |
| 显式确认 | Java、测试、公共契约、DDL/H2、运行时配置、Git、联网、依赖安装、生产配置、真实资金、外部规则和专业合规确认。 |
| 无进展检测 | 连续两轮只是重复 W4 确认包而无新增源码证据、验证证据、任务收敛或用户确认时暂停。 |
| 交接 | 用户确认 B2-AH 后交给资深架构师进入 CAD；未确认时继续 docs-only 计划维护。 |

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
| 当前 Wave / Task | `GSD2-B7-RECON-DIFFERENCE-MVP-001` |
| 当前状态 | `FIRST_GREEN_VERIFIED_RECON_DIFFERENCE_MVP` |
| 下一建议 Grant | `GSD2-B7-RECON-DIFFERENCE-MVP-002`，让清算/出款准入消费差错状态、阻断范围和重跑结果；若继续 B5，则另行确认 `GSD2-B5-BALANCE-ADJUST-AUDIT-002` 扩展审计持久化、route snapshot 回链或运营审批闭环。 |
| 下一 Red | B7 消费方 Red：证明存在 `BLOCKED` 或未对平差错时不得生成清算候选、确认清算批次或提交出款；或 B5 扩展 Red：证明审计字段进入 route snapshot/独立审计查询且不污染普通调账。 |
| 写入范围 | 来源契约已写入 core port、wallet-face/impl、H2 schema、授权路由接入和目标测试；资金责任目标主体已写入 wallet Request/DTO/Query、Entity、H2 schema、资源服务校验和目标测试；wallet application facade 首轮已写入资金责任解析契约、支付工具能力准入契约、Request/DTO、实现和目标测试；B4 投影解释已写入 transaction-face projection 查询契约、解释来源、transaction-impl 查询实现和服务流测试；B5 余额调账审计已写入 balance adjust 请求审计字段、外部异常来源类型、instruction context keys、转换器校验/透传和服务流测试；B7 差错闭环已写入 reconciliation application 契约、Request/DTO、差错枚举、Entity/Mapper、H2 schema 和服务流测试。 |
| 只读范围 | PRD、DSL、系分、TDD、OpenSpec、ledger、wallet、transaction、core、tests、Justfile、AGENTS.md、最近 Git 提交和历史准入卡。 |
| 验证命令 | B4 首轮已执行 `just test-one FundsTransactionProjectionExplainApplicationServiceTests tests`、`just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests`、`just compile`、`just pmd` 和 `git diff --check`；B5 首轮已执行 `just test-one FundsBalanceAdjustAuditFlowTests tests`、`just test-one FundsBalanceControlFailureFlowTests tests`、`just test-balance-control`、`just test-reconciliation`、`just compile`、`just pmd` 和 `git diff --check`；B7 首轮已执行 `just test-one ReconciliationDifferenceApplicationServiceTests tests`、`just test-reconciliation`、`just compile`、`just verify-fast`、`just pmd` 和 `git diff --check`；此前 `e81a8a25` 已完成 `verify-cad` 全门禁。下一 Grant 后运行对应目标测试、route snapshot 回归、`just compile`、`git diff --check`，必要时补分组测试和 `pmd`。 |
| Git 策略 | `summary_only`。 |
| 交接要求 | 若用户确认下一 Grant，先写最小 Red，再最小 Green；完成后回写本文第 4 节 Grant 队列、OpenSpec tasks、验证证据、Not Done 和残余风险。 |

## 8. 验证矩阵

| 验证层 | 命令或方式 | 通过口径 |
| --- | --- | --- |
| Harness 结构 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | Task、Owner、范围、Wave、上下文账本、禁止事项、验证和 handoff 字段齐全。 |
| 产品结构 | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | 业务目标、能力地图、对象、流程、规则、运营数据、风险和验收齐全。 |
| 架构结构 | `check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/GSD-2-P0P1-LedgerWalletTransaction推进计划.md` | 背景目标、现状约束、核心决策、契约、数据一致性、可靠性安全、验证、发布风险齐全。 |
| 状态一致性 | `rg "GSD2-W5|P0P1-LWT|GSD2-B2-FR-TARGET|FR_TARGET|targetSubject" docs openspec` | GSD2 入口、README 和 OpenSpec tasks 能追踪到本文、FR target Green 结果和下一 Grant。 |
| 空白和 Markdown | `git diff --check` | 无行尾空白或 patch 格式问题。 |
| 编译、测试、PMD | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsTransactionProjectionExplainApplicationServiceTests tests`、`just test-one DefaultRoutedFundsInstructionOrchestratorProjectionTests tests`、`just compile`、`just pmd`、`git diff --check`。 | B4 首轮只读投影解释服务流和原投影发布回归通过；账户层级来源契约、资金责任目标主体、资金责任解析 facade、支付工具能力准入 facade 的完整门禁已在 `e81a8a25` 收口通过。 |

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
| Not Done | 清算/结算/出款对差错状态的消费、完整清分清算结算出款、追偿、账龄升级、运营后台、生产迁移脚本、差异报告、补事实命令执行服务、运营审批流、余额调账独立审计表/审批流/route snapshot 审计回链、退款/no-auth refund/释放/拒付全量投影解释矩阵、projection store、治理重放、钱包账户聚合、账户能力来源组合、授权 admission、完整预交易快照、Spend Rule 控制闭环、VCC / 全球账户 / 清结算 P2 场景。 |
| 禁止误读 | 本轮 full gate 只证明已落地切片的工程门禁通过，不等于 wallet 全量生产 Done，不等于支付工具授权入口、VCC facade、清结算对账或 P2 业务已可交付。 |
