# OpenSpec Project Context

## 一、定位

本目录是支付资金底座进入后续开发前的规格基线目录。它不保存历史版本演进，不承载历史过程内容，也不替代产品设计、DSL 设计、系分设计和 TDD 设计。

OpenSpec / Superpowers / Harness 在本项目中的定位：

1. OpenSpec 定目标：把当前最终版设计转成可开发、可测试、可评审的能力规格。
2. Superpowers 保纪律：以测试驱动设计、Review、Refactor、金融红线和验证门禁约束后续编码。
3. Harness 管协作：按 MVP 任务切片拆分写入范围、只读范围、验证命令和人工确认点。

项目决策和协作结论必须回到 `docs/`、`openspec/`、Harness Plan、Git 提交点或用户确认，不依赖外部记忆层作为编码准入或验收依据。

产品架构准入锚点：业务目标是让支付资金底座的后续编码只消费已确认的资金事实、对象、流程、规则和验收口径；用户价值是让产品、运营、财务和研发能从同一份规格基线判断当前任务是否可进入单一 Execution Grant。业务对象、对象模型、字段口径、生命周期和状态以产品设计、DSL、系分和 TDD 为准；业务流程必须区分主流程、异常流程和人工兜底。规则矩阵必须写清触发条件、判断逻辑、优先级、版本、审计和验收来源。

## 二、历史内容处理

历史 OpenSpec specs、changes、Superpowers/Harness 计划和旧测试代码均已作废。后续开发不得引用旧规格、旧任务拆分或旧测试断言作为通过依据。

保留内容：

| 类型 | 路径 | 用途 |
| --- | --- | --- |
| 产品设计 | `docs/产品设计` | 产品目标、对象、流程、规则、验收和风险边界。 |
| DSL 设计 | `docs/DSL设计` | 资金事实、指令、路由、账务计划、分录、JSON 契约和场景矩阵。 |
| 系分设计 | `docs/系分设计` | 模块边界、服务契约、状态机、表设计、观测、安全和金融红线。 |
| TDD 设计 | `docs/TDD设计` | 后续测试重建的唯一场景和断言入口。 |
| 测试 resources | `core/src/test/resources`、`tests/src/test/resources` | 测试配置、H2 schema、测试数据等资源基线。 |

## 三、当前 Source of Truth

| 层级 | 权威入口 | 说明 |
| --- | --- | --- |
| 产品 | `docs/产品设计/README.md` | 判断需求是否属于资金底座、扩展能力或外部模块。 |
| DSL | `docs/DSL设计/支付资金底座DSL承载层设计.md` | 判断资金事实、事件、交易类型、route、posting 和 JSON 契约。 |
| 系分 | `docs/系分设计/README.md` | 判断模块边界、服务能力、表设计、状态机和非功能要求。 |
| TDD | `docs/TDD设计/支付资金底座测试驱动设计.md` | 判断测试顺序、测试分层、红线用例和进入编码前检查项。 |
| OpenSpec | `openspec/specs/payment-funds-foundation/spec.md` | 把上述设计压缩成后续开发基线。 |
| Harness | `openspec/changes/tdd-baseline-reset/tasks.md` | 按 TDD 拆分后续 MVP 任务切片、覆盖索引和验证门禁。 |

### 3.1 当前对齐点

| 基线 | 当前对齐点 | 说明 |
| --- | --- | --- |
| 上一设计交付基线 | `30b1a00 docs: 冻结权益快照设计基线` | 产品设计、DSL 设计、系分设计和 TDD 设计已完成权益快照合并与基线冻结，可作为本轮准入复核输入。 |
| 当前编码准备基线 | 最新已提交设计和任务对齐输入以确认时 Git HEAD 为准；`270122e docs: 刷新 CAD 准入基线`、`9456ab6 docs: 对齐 A0 准入与代码基线`、`4a7ef12 docs: 固化代码准入 CR 基线` 和 `f99800b docs: 对齐代码 CR 任务基线` 只保留为历史准入证据，不再作为当前恢复入口。当前有效输入聚焦 B1-10 契约承载、A0/A1 准入证据、钱包账户/账户层级、交易内核 Round 0、支付工具与 Spend Rule 后置支持、B4 授权后继能力闭环提交，以及 `openspec/changes/tdd-baseline-reset/tasks.md` 的最新任务账本。 | B4 已闭合的局部代码能力包括 `b0666ba` / `f99f3a3` 授权过期释放、`616dac1` / `3825466` 授权强制完成、`006bcaa` / `818da34` / `967586c` 无授权退款最小契约与路由回退、`949b24a` 授权争议退款可区分性和 `47c5269` 授权后继并发竞争；`4d5f9c2` 与本轮任务账本已回写对应 GSD-CAD handoff。当前设计基线不得自动等同为授权支付工具应用入口代码完成、钱包 application facade 代码完成、资金责任目标字段迁移完成、预算组账务主体化兼容缺口关闭、完整 dispute/chargeback case、VCC/全球账户业务专项、清结算对账、资金数据治理、指标治理或任一未授权 MVP 编码 Done 证据。 |
| 当前 A0/A1 准入证据 | `4f0a43e docs: 记录 A0 只读验证证据`、`26144cc docs: 补齐 A1 直接交易准入卡`、`4dbad21 docs: 记录 CAD 完整验证证据`、`52f116f docs: 记录 A1 现有覆盖扫描`、`dbba956 docs: 记录 A1 门禁复核证据`。 | 这些提交只作为 A1 直接交易事实红线 Execution Grant 的确认输入；其中 `52f116f` 记录既有测试覆盖扫描和目标测试通过证据，`dbba956` 记录证据链同步后的 `just verify-cad` 完整门禁复核；未获用户确认前，不授权生产代码、测试代码、DDL/H2 schema 或运行时配置写入。 |
| 当前任务账本状态 | 已消费的 B4-NO-AUTH-REFUND、B4-DISPUTE-SEMANTIC-ALIGNMENT、B4-AUTH-RACE、过期恢复入口、B3-DIRECT-REFUND-REFERENCE-REPLAY 和旧候选包只作为历史证据，不再作为当前默认任务计划；当前执行优先级按“账本账目 > 钱包账户/账户层级 > 交易层 > 清结算对账 > 支付工具支持 > VCC/全球账户支持 > 收单设计-only”排列。2026-06-11 Agent Loop Plan Grant 已为 B4 补一个低风险授权后继缺原授权事实 fail-fast 覆盖；随后用户确认并消费 B3，直接退款原交易引用回放切片已闭合；随后验证 `R0-TRX-REPLAY-002` 的纯 route replay 原快照复用边界，`DefaultRouteReplayServiceTests` 9 tests 通过；随后补 `R0-TRX-REPLAY-001` 原交易存在但 route snapshot 缺失的直接退款全链路失败无副作用覆盖，`FundsDirectTransactionFlowTests` 50 tests 通过；本轮补 `R0-TRX-REPLAY-002` 直接退款交易 flow 子场景，证明原支付 route snapshot 固化旧支付工具和旧资金责任后退款仍沿历史快照回放，`FundsDirectTransactionFlowTests` 51 tests 通过。 | 公共契约、DDL/H2、生产代码、运行时配置和 Git 操作仍必须确认一个单一 Execution Grant；B3 已消费的公共契约例外仅限 `FundsTransactionRefundRequest.referenceTransactionSn`，route replay 纯边界、缺原快照 flow 和直接退款当前绑定回放 flow 覆盖不能外推为冻结编码基线、生产 Done 证据或后续可写范围。 |
| 当前 GSD + Goal 生产可用 MVP 基线 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | 本轮把金融创业公司主业务目标收束为 VCC 发卡、VCC 交易处理和全球收付款 MVP，但执行顺序按依赖关系优先交付被依赖方能力：账本账目、钱包账户、交易内核、清结算对账，再推进支付工具、VCC 和全球账户。GSD 计划已补生产可用 MVP 交付雷达和 Goal 完成度审计，逐域标明当前状态、下一生产证据、下一 Grant 入口、不能算 Done 的情况和当前未完成结论；交易内核 Round 0 已新增 `B4-CANONICAL-REPLAY-FAILFAST` 首切片。当前会话已开启运行时 Goal；2026-06-07 运行时 Goal 证据已回写到 GSD 计划和任务账本，证明当前账本目标测试资产可运行；2026-06-11 Agent Loop Plan Grant 证据已回写到 GSD 计划第 8.5 节，证明 B4 授权后继缺原授权事实 fail-fast 覆盖已补齐；第 8.7 节证明 B3 直接退款原交易引用回放已消费；第 8.8 节证明 B4 route replay 原快照复用的纯服务边界已由 `DefaultRouteReplayServiceTests` 验证；第 8.9 节证明原交易存在但 route snapshot 缺失时直接退款交易 flow fail-fast 且无新资金或账务副作用；第 8.10 节证明原支付 route snapshot 固化旧支付工具和旧资金责任后，直接退款交易 flow 仍沿历史快照回放。Goal、交付雷达、完成度审计、运行时证据、Plan Grant 覆盖、B3 局部闭环、route replay 纯边界、缺原快照 flow 和直接退款当前绑定回放 flow 覆盖都不是完整 Execution Grant，当前仍不授权新的公共契约、DDL/H2、生产代码、运行时配置或 Git 操作。 |
| 当前 PRD/系分/DSL/TDD 权威入口门禁 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#82-authorityentrygateevidence2026-06-07` | 2026-06-07 已完成 PRD 总览、交易路由钱包账目产品设计、清结算与对账产品设计、VCC PRD、交易路由钱包账目系分、清结算与对账系分、DSL 承载层和 TDD 设计的授权前结构门禁复核。该门禁只证明上游权威入口可支撑下一单一 Grant 确认，不替代 Execution Grant、代码实现、测试通过、DDL/H2 证据或生产 Done。 |
| 当前编码准入裁决 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#83-codingadmissiondecision2026-06-07`、`docs/TDD设计/GSD-1-账本账目状态账本.md#21-verificationevidence2026-06-11-004aconsumed`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#85-agentloopexecutionevidence2026-06-11`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#87-executionevidence2026-06-11-b3-direct-refund-reference-replay`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#88-agentloopexecutionevidence2026-06-11-b4-route-replay-snapshot-boundary`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#89-agentloopexecutionevidence2026-06-11-b4-missing-route-snapshot-flow`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#810-agentloopexecutionevidence2026-06-11-b4-current-binding-replay-flow` | `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 已由 `GSD1-LD-RED-002A` 消费并登记为覆盖补齐；`Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 已由 `GSD1-LD-RED-003` 消费并登记为既有投影强化回归通过；`Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 已由 `GSD1-LD-RED-004A` 消费并登记为 BudgetGroup 兼容 guard 通过。2026-06-11 Agent Loop Plan Grant 已允许低风险目标测试覆盖补齐，并完成 B4 授权后继缺原事实覆盖、route replay 纯边界覆盖、缺原 route snapshot 直接退款 flow 覆盖和直接退款当前绑定/资金责任变化后沿原快照回放覆盖；`Execution Grant：B3-DIRECT-REFUND-REFERENCE-REPLAY` 已消费并关闭直接退款原交易引用回放切片。新的公共契约、DDL/H2、生产代码、运行时配置和 Git 操作仍需新的单一 Execution Grant。 |
| 当前来源采纳分级口径 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#21-来源采纳分级门禁` | 后续 Grant 引用 fincone、fincone-issuing、nobe、Highnote、陈天宇宙公开文章或其他公开资料时，必须选择 `source-of-truth`、`advisory-reference`、`scenario-seed`、`semantic-reference` 或 `blocked-reference`。只有 wind-funds 当前 PRD/DSL/系分/TDD/OpenSpec/源码/测试/验证命令和确认时 Git HEAD 可作为 Source of Truth；兄弟项目和外部资料只能校准业务目标、场景种子、语义或待确认项，不能直接生成代码授权、表结构、公共契约或生产 Done。 |
| 当前 Grant 级别选择口径 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#711-execution-grant-级别选择矩阵` | 任一 Grant 确认前必须选择 `contract-only`、`ddl-backed`、`service-flow-backed`、`projection-store-backed` 或 `design-only` 等交付级别。`contract-only` 只能声明契约或目标 Red 可评审，`ddl-backed` 只能声明数据承载可验证，只有 `service-flow-backed` 加真实 Spring Bean、H2/fixture、资金事实、幂等、失败无副作用、审计和验证命令，才可作为生产交付证据之一；收单默认保持 `design-only`。 |
| 当前账本到账户交接口径 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#712-账本账目到钱包账户交接门禁` | 钱包账户、账户层级、VCC 子账户和全球账户钱包可以先做 `contract-only/no-ddl` 准入 Red，但生产可用、H2-backed、账本初始化、余额可用、父子账户汇总或 VCC funding 必须依赖当前工作树 001A/001B/002A 账本证据和新的账户层级 Grant；002A 证据未被冻结为 Git 基线，不能外推为账本整体生产 Done。若后续目标 Red 直接 Green，只登记覆盖补齐；若证明真实缺口，只能在对应 Grant 写入范围内最小修复。 |
| 当前 GSD-1 账本账目候选 | `docs/TDD设计/GSD-1-账本账目Round0准入卡.md`、`docs/TDD设计/GSD-1-账本账目Wave1执行计划.md`、`docs/TDD设计/GSD-1-账本账目状态账本.md`、`docs/TDD设计/GSD-1-账本账目代码库理解结论包.md`、`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | 当前候选 `GSD1-LEDGER-FACTS-CAD-001` 已完成五个切片：`GSD1-LD-RED-001A` 已消费 `Execution Grant：GSD1-LEDGER-FACTS`，`GSD1-LD-RED-001B` 已消费 `Execution Grant：GSD1-LEDGER-IDEMPOTENCY`，`GSD1-LD-RED-002A` 已消费 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`，`GSD1-LD-RED-003` 已消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION`，`GSD1-LD-RED-004A` 已消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。004A 已通过 direct 47 tests、auth 29 tests、balance-control failure 19 tests 和 compile，预算组控制账本兼容路径保留，资金价值交易入口拒绝 `BUDGET_GROUP`。当前状态为 `WAVE_2_RED_004A_CONSUMED_SUMMARY_ONLY`；下一轮若继续编码，必须确认新的单一 Execution Grant。 |
| 2026-06-11 GSD + Goal 续跑裁决 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#84-goalresumeadmissionevidence2026-06-11`、`docs/TDD设计/GSD-1-账本账目状态账本.md#20-verificationevidence2026-06-11-004apregrantresume`、`docs/TDD设计/GSD-1-账本账目状态账本.md#21-verificationevidence2026-06-11-004aconsumed`、`docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md#16-consumedbudgetgroupcompatguardgrant2026-06-11` | 续跑先完成 004A 授权前源码锚点复核，随后用户确认并消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。本轮仅在 transaction converter 和 transaction flow 测试范围内做最小 guard：直接交易和授权交易拒绝 `BUDGET_GROUP` 作为资金价值主体，余额控制兼容回归保持通过；未写 DDL/H2 schema、公共契约、Entity、Mapper、钱包、支付工具、VCC、清结算对账或运行时配置。下一轮不得沿用本 Grant。 |
| 2026-06-11 Agent Loop Plan Grant / B4 覆盖补齐 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#85-agentloopexecutionevidence2026-06-11`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#88-agentloopexecutionevidence2026-06-11-b4-route-replay-snapshot-boundary`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#89-agentloopexecutionevidence2026-06-11-b4-missing-route-snapshot-flow`、`docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#810-agentloopexecutionevidence2026-06-11-b4-current-binding-replay-flow`、`docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` | 用户明确要求进入 Agent Loop Engineering 并按 GSD + Goal 任务计划推进；首轮选择 `B4-CANONICAL-REPLAY-FAILFAST` 的低风险 `R0-TRX-REPLAY-001` 子场景，新增 `FundsAuthorizationTransactionFlowTests#testAuthorizationSuccessorsMissingOriginalFactShouldRejectAndLeaveNoSideEffects`，证明授权后继缺原授权交易事实时 fail-fast 且无资金、账本事实副作用，30 tests 通过；随后验证 `R0-TRX-REPLAY-002` 的纯 route replay 边界，`DefaultRouteReplayServiceTests` 9 tests 通过，证明当前工具、外部账户或资金责任上下文不会覆盖原 `RouteSnapshot`；随后补原交易存在但 route snapshot 缺失时直接退款 fail-fast 且无新资金或账务副作用，`FundsDirectTransactionFlowTests` 50 tests 通过；本轮补直接退款当前绑定/资金责任变化后沿原支付快照回放 flow，`FundsDirectTransactionFlowTests` 51 tests 通过。该证据不是完整 B4 Done，不授权公共契约、DDL/H2、生产代码、运行时配置或 Git。 |
| 2026-06-11 B3 直接退款原交易引用回放 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md#87-executionevidence2026-06-11-b3-direct-refund-reference-replay`、`docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` | 用户确认并消费 `Execution Grant：B3-DIRECT-REFUND-REFERENCE-REPLAY` 后，直接退款新增 `referenceTransactionSn` 内部原交易引用，converter 转为 `ORIGINAL_TRANSACTION`，直接付款本金 leg 和费用 leg 支持部分回放，lifecycle 保存独立退款交易并更新原交易累计已退摘要；缺原交易和累计超额失败无副作用。已验证 `just test-one FundsDirectTransactionFlowTests tests` 49 tests、`just test-transaction` 102 tests、`just test-boundary` 152 tests 通过；未写 DDL/H2 schema、支付工具 facade、VCC、清结算对账、治理或运行时配置。 |
| 2026-06-11 授权争议 / chargeback 语义裁决 | `docs/产品设计/02-交易路由钱包账目与投影.md#411-交易入口分层裁决`、`docs/DSL设计/支付资金底座DSL承载层设计.md#92-授权交易用例族`、`docs/系分设计/02-交易路由钱包账目与投影系分设计.md#421-服务契约`、`docs/TDD设计/支付资金底座测试驱动设计.md#81-授权交易` | dispute / chargeback 定性为案件过程，不是授权交易层默认资金结果。资金底座只消费裁决后的退款结果：用户胜诉或需退款时通过 `settleRefund / AUTH_REFUND` 沿原完成路径承接；用户败诉或无需资金处理时不得生成 route、posting、LedgerEntry、余额变化或新的交易事实。既有 `FundsAuthorizationTransactionService#chargeback` 只能视为历史兼容或内部适配资产；移除公共 API、迁移测试、调整 `CHARGEBACK` event 入口或建立完整 dispute case 必须另起新的单一 Execution Grant，不能混入账本 004A、VCC 或清结算切片。 |
| 当前钱包账户/账户层级候选 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#802-accounthierarchyonepageconfirmation2026-06-07` | 若用户要求先推进 VCC、共享卡、预付卡或钱包账户层级，下一确认入口为 `Execution Grant：B2-ACCOUNT-HIERARCHY` / `B2-ACCOUNT-HIERARCHY-CAD-001`。默认决策是 `contract-only`、`parent-child-snapshot-required`、`detail-only-first` 和 `no-ddl`，先证明 VCC 卡绑定资金/信用子账户、父账户/根账户快照、账目 profile、币种、状态、绑定摘要和资金来源准入；该快速路径只是账户层级准入 Red 先行准备，不豁免账本账目、资金责任、交易内核、对账差错和支付工具准入依赖。不新增 `VCC_ACCOUNT`，不把卡号、支付工具、预算组、Spend Rule 或父账户聚合视图写成账本主体，不替换交易 canonical 请求。该候选只在用户确认后允许进入账户层级 Red，未确认前仍不授权写生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。 |
| 规格任务基线 | `openspec/project.md`、`openspec/specs/payment-funds-foundation/spec.md`、`openspec/changes/tdd-baseline-reset/tasks.md` | 本次把目标态设计、当前代码和能力域准入重新对齐；后续编码仍需按能力域授予 Execution Grant。 |
| 代码能力基线 | 截至 `967586c`，B1-10 权益快照 DSL 契约、契约测试、JSON 夹具、请求摘要、稳定摘要、账务计划装配器长 ID、预算组默认周期 `LIFETIME`、账务事实断言、钱包边界、交易稳定摘要、治理投影重放边界、出款前准入候选实现、CAD 完整验证门禁、提现/解冻红线、余额日志证据、路由事实边界、交易投影解释、权益回放摘要、治理重放差异校验、资金事实红线、敏感上下文阻断、上下文不可变性、失败无副作用、费用幂等、外部账户主体阻断、支付工具绑定对象约束、钱包入口文档口径、授权过期释放、授权强制完成、无授权退款最小契约、无授权退款路由外部引用回退、争议退款可区分性和授权并发竞争首轮能力已纳入局部基线。 | 共享承载已有 DSL 契约测试和 B1-10 权益快照契约承载基线，基础事实已有支付工具、绑定历史、资金责任解析关系（历史表名、服务名或字段仍可能使用 funding relation 命名）、账务计划装配器、预算组默认周期 `LIFETIME`、余额投影、上下文敏感字段阻断、支付工具绑定对象约束和主链路事实红线局部基线；交易与读模型已有直接交易、授权过期释放、授权强制完成、无授权退款、争议退款可区分性、授权并发竞争首轮闭环、局部边界测试、解释摘要和重放差异校验；出款前准入候选实现只作为清结算与对账差距复核输入，清结算与对账、资金数据治理、完整 dispute/chargeback case 和支付工具授权应用入口仍是独立待落地域。 |
| B4 授权后继当前恢复入口 | B4-DISPUTE-SEMANTIC-ALIGNMENT 已由 `949b24a` 闭合并由 `4d5f9c2` 回写任务账本；B4-AUTH-RACE 已由 `47c5269` 闭合；B4-NO-AUTH-REFUND、B4-DISPUTE 旧恢复入口和 Grant 候选包都不再作为当前恢复入口。当前没有可直接恢复的 B4 授权后继默认编码入口。 | 下一轮若选择授权支付工具应用入口、授权权益生命周期、完整 dispute/chargeback case、余额控制调账或其他候选，必须另起 Round 0 或新的单一 Execution Grant。 |
| 当前 B4 交易内核 Round 0 候选 | `docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md#151-canonicalreplayonepageconfirmation2026-06-07` | 已把交易层生产可用补强收敛为 `B4-CANONICAL-REPLAY-FAILFAST-CAD-001` / `Execution Grant：B4-CANONICAL-REPLAY-FAILFAST`，只证明账户主体型 canonical 内核的原路径回放、缺原快照或原事实 fail-fast、当前绑定变化不重选路和失败无副作用。2026-06-11 Plan Grant 已补“授权后继缺原授权事实”覆盖；`B3-DIRECT-REFUND-REFERENCE-REPLAY` 已单独关闭直接退款原交易引用回放；已验证纯 route replay resolver 边界下当前支付工具、外部账户和资金责任变化不会覆盖原快照；已补原交易存在但 route snapshot 缺失时直接退款全链路失败无副作用覆盖；本轮已补直接退款交易 flow 下当前绑定和资金责任变化后仍沿原快照回放的服务级证据。剩余交易投影解释、调账审计、授权/争议/VCC lifecycle 更大组合 replay flow 仍未完成。默认不改新的公共契约、不改 DDL/H2、不替换 canonical 入参，不授权支付工具 facade、VCC、清结算对账、治理或交易 canonical 入参替换。 |
| 当前 B7 清结算与对账候选 | `docs/TDD设计/B7-清结算与对账Round0准入卡.md#151-reconciliationdifferenceonepageconfirmation2026-06-07` | 已把清结算与对账首切片收敛为 `B7-RECON-DIFFERENCE-MVP-CAD-001` / `Execution Grant：B7-RECON-DIFFERENCE-MVP`，先证明已过账交易进入对账、差异生成差错、阻断清算或出款、重跑和白名单补事实准入；默认生产可用路径要求 `service-flow-backed` 和最小 DDL/H2 范围，`contract-only` 只能作为契约/DTO/目标 Red，不得声明 B7 生产可用；不授权完整清分、清算、结算、出款、追偿、外部协议或运营后台实现。 |
| 导出附件 | `docs/*.zip` 等导出包不作为 Source of Truth。 | 评审、编码和测试只以 Markdown、OpenSpec、源码、测试和 Git 提交点为准。 |

### 3.2 生产交付判定

当前 Source of Truth 的统一结论是：PRD、DSL、系分和 TDD 的目标一致，可作为后续编码和生产落地评审基线；但生产交付必须按 MVP 任务切片逐一证明，不因文档齐备而自动成立。

| 域 | 当前判定 | 后续进入 Done 前必须证明 |
| --- | --- | --- |
| P0 账本、账目、余额投影、对账、清分、清算、结算、账本余额快照和资金数据治理证据 | 账本账目与余额投影是当前最高优先级；清结算与对账、资金数据治理只达到 TDD 分析输入态，整体编码准入未打开。原 04 已拆入产品 02、03、05，04 仅保留兼容索引。 | DSL 契约、账本账目服务、余额断言、清结算对账对象、DDL/H2、服务层测试、Manifest、账本余额快照、差异报告、异常人工处理、外部规则确认和边界测试通过。 |
| P0 钱包基础能力 | 钱包账户、内部钱包入口、资金责任解析和钱包 application facade 排在交易层之前；支付工具只作为最后一组支持能力，不再代表钱包优先级。 | 钱包服务、账户能力、资金责任解析、幂等摘要、失败无副作用、边界测试和审计断言闭合。 |
| P1 交易层 | 直接交易、授权交易、余额控制、route replay、交易投影和重投影排在账本账目与钱包之后。 | 代码实现、契约测试、服务层测试、route、posting、entry、projection、幂等、失败无副作用、权益准入和边界测试通过。 |
| 支付工具、VCC 和全球账户支持 | 均放到默认编码队列最后，只能作为专项支持能力或后置 application facade 候选。 | 支付工具/VCC/全球账户专项 PRD、系分、TDD、OpenSpec 和 Execution Grant 证明归一资金事实、外部规则核验、轨道边界和资金底座复用接口。 |
| 收单能力 | 仅保留设计、边界复核和收单 PRD/DSL/系分/TDD 差距分析；不进入实现候选。 | 收单 capture、dispute、商户清结算和 PCI/外部规则边界只作为设计输入，除非后续用户明确重新打开收单实现优先级。 |

### 3.3 当前编码准入裁决

| 项 | 结论 | 执行口径 |
| --- | --- | --- |
| 设计基线 | 最新已提交设计和任务对齐输入以确认时 Git HEAD 为准；`270122e` 是上一完整 CAD 验证证据提交；`81a7ecb`、`4a7ef12`、`f99800b` 和 `9456ab6` 保留为历史准入证据。 | 后续 MVP 编码任务若要采用资金责任解析、支付工具投影、授权支付工具应用入口、钱包 DDD 应用层、资金责任目标字段、BudgetGroup/Spend Rule 控制上下文、核心设计骨架、代码 CR 差异、B2/B4 准入口径和钱包入口收敛，必须引用确认时 Git HEAD，或在 Execution Grant 的 `authorityBaseline` 中显式列为基线附件。 |
| A0 至 A4 MVP 基础任务 | 可在 Execution Grant 下进入。 | 每个任务必须写明 `mvpScenario`、产品验收、DSL caseId、系分章节、TDD 用例、红线编号、写入范围、验证命令和 Not Done 条件；B1 至 B6 只作为覆盖索引，不作为一次性授权范围。 |
| 2026-05-31 代码准入 CR | 历史裁决中 `A1 直接交易事实红线` 曾是当时最接近可确认的单一 Execution Grant；交易 canonical 入口保持账户主体型，不需要改为支付工具引用。B2/B4/B5/B6 只能先做独立 Round 0 或单一 Execution Grant：钱包 application facade、资金责任目标字段、BudgetGroup 兼容策略和预算控制投影不能混成一次写入。2026-06-02 恢复入口 `B4-NO-AUTH-REFUND` / `B4-NAR-CAD-001` 已由 `006bcaa feat: 补齐无授权退款 canonical 能力` 消费并闭合。 | 若用户只说“继续编码”但未确认具体 Execution Grant，默认不得写生产代码、测试代码、DDL/H2 schema 或运行时配置；可继续做只读差距复核，或请用户在 A1/B2/B4/B5/B6 中另行确认一个最小切片。 |
| 2026-05-31 B2/B4 准入口径修复 | 支付工具、钱包 application facade 和授权支付工具入口的文档阻断已关闭：外部工具和工具快照才使用 `PaymentInstrumentRef`，内部余额钱包、信用额度、返利钱包和商户钱包等业务入口先解析为 `SubjectRef`、`BenefitSnapshot`、`FundingAllocationDecision` 或等价不可变快照；`PaymentInstrumentCapabilityApplicationService` 只做能力准入和快照，不承接注册或绑定变更；TDD 显式纳入 `AC-AUTH-000`。 | 该修复允许 B2/B4 进入独立 Round 0 或单一 Execution Grant 准备，但仍不等于编码授权；后续若新增 application facade、Request/DTO、幂等摘要、H2/DDL 或测试资产，Execution Grant 必须单独列明写入范围和验证命令。 |
| 2026-05-31 钱包入口二次收敛 | 内部余额钱包、平台钱包、商户钱包、返利钱包和信用额度入口不再按“钱包标识=支付工具”读取；内部入口解析为 `SubjectRef`、`BenefitSnapshot`、`FundingAllocationDecision` 或等价不可变快照，外部钱包端点、通道 token、卡、VA 和外部账户才使用 `PaymentInstrumentRef` / `ExternalAccountRef`。 | 该收敛只消除 PRD/DSL/系分/TDD/OpenSpec 读法分叉，不新增编码授权；若后续要实现钱包入口 facade、工具准入快照或请求模型，仍必须走 B2/B4 的单一 Execution Grant。 |
| 2026-05-31 本轮编码准入复核 | 工作树在复核起点为 clean，上一完整 CAD 验证证据提交为 `270122e`。本轮只刷新准入引用：钱包/交易 application facade 仍未落地，资金责任关系仍以 `fundingAccountId` 兼容字段为主，BudgetGroup 兼容路径仍存在，交易 canonical 请求仍是账户主体型；支付工具绑定对象约束和钱包入口文档口径已进入局部基线。 | 可把 A1 推进到“用户确认态”，但不能把本轮复核解释为 A1 自动授权；B2/B4/B5/B6 仍需各自 Round 0 或单一 Execution Grant；B7/B8/P2 仍只进入 TDD 分析和独立授权准备。 |
| 2026-06-02/04 B4 授权后继能力代码基线 | B4-TRX-EXPIRE 已由 `b0666ba` 闭合；B4-FORCE-SETTLE 首轮账户主体型 canonical 能力已由 `616dac1` 闭合，策略红线已由 `3825466` 加固；B4-NO-AUTH-REFUND 已由 `006bcaa` / `818da34` / `967586c` 闭合并经 CR 收缩为资金层最小契约；B4-DISPUTE-SEMANTIC-ALIGNMENT 已由 `949b24a` 闭合；B4-AUTH-RACE 已由 `47c5269` 闭合。普通完成与 FORCE 分支已切开；无授权退款以空原授权流水进入 no-auth 语义，请求侧不恢复 `refundMode`，route resolver 在内部 `REFUND_MODE` 缺失时可由 `EXTERNAL_TRANSACTION` reference 推断 no-auth refund 路由，显式 `DISPUTE` 或其他退款归类不被覆盖；争议退款通过 `settleRefund / AUTH_REFUND` 的 `disputeMode`、`disputeReason`、`disputeVoucherRef`、`externalDisputeRef` 和内部 `DISPUTE` 上下文保留可区分性；授权后继并发通过同一授权的 settle / expire / reversal 竞争 Red 证明只有一个赢家，失败方无 route、posting、ledger entry、projection 或余额副作用。 | B4-FORCE-SETTLE、B4-NO-AUTH-REFUND、B4-DISPUTE-SEMANTIC-ALIGNMENT 和 B4-AUTH-RACE 只作为回归基线；授权支付工具 application facade、授权占券和权益生命周期、完整 dispute/chargeback case、VCC、Spend Rule、清结算对账和治理 apply 仍不开放默认编码准入。 |
| 2026-06-03 B4-AUTH-PI GSD-CAD Round 0 | 本轮自动推进只读复核把下一候选收敛为 `B4-AUTH-INSTRUMENT-APPLICATION` / `B4-AUTH-PI` 授权支付工具应用入口。代码扫描确认 `FundsAuthorizationTransactionService#authorize` 和 `FundsAuthorizationTransactionAuthorizeRequest.accountId` 仍是账户主体型 canonical 内核，`wallet-face` 当前只有 `PaymentInstrumentService`、`SpendSubjectFundingRelationService` 等资源服务，未发现 `AuthorizationAdmissionApplicationService` 或 `authorizeByInstrument` 生产入口。 | 当前状态为 `ROUND0_READY_NOT_CODE_AUTHORIZED`。后续若用户确认新的单一 Execution Grant，首批 Red 应从 `R0-AUTH-001` 开始，证明 application facade 先做工具、绑定、Spend Rule、资金责任和账户能力准入，批准后委派账户主体型授权内核，拒绝无 route、posting、LedgerEntry、projection 或敏感上下文副作用；未确认前不写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |
| 2026-06-04 B4-AUTH-PI Grant 候选基线刷新 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#82-authinstrumentgrantcandidate2026-06-03` 已补齐 `B4-AUTH-PI-CAD-001` 可确认包，并已把确认基线刷新到包含 `88d80c7` 索引基线、`7b49684` 候选门禁、`be3df9f` / `c58431e` 索引同步和 `226dfc2` Harness 流水回写的 Git HEAD；候选包列明 Owner、authority baseline、写入范围、只读范围、首批 Red、验证命令、Git 策略、禁止事项和停止条件，且已通过资深架构师 Harness checker 的 `cad-candidate` 结构检查。 | 该包仍只是 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`，不等于用户已确认 Execution Grant。后续只有用户明确确认 `Execution Grant：B4-AUTH-PI` 后，才允许写授权 application facade Red 和最小实现；否则继续保持 Round 0 / summary_only。 |
| 2026-06-04 B2-PI-CAP Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#83-b2-pi-cap-round-0-扫描2026-06-04` 已把支付工具能力准入单切片收敛为 `B2-PI-CAP-CAD-001`；代码扫描确认 `PaymentInstrumentService` 仍是资源服务，已覆盖工具状态、方向、币种、生效窗口、绑定历史、敏感字段阻断和无账务副作用局部基线，但未形成 `PaymentInstrumentCapabilityApplicationService` 或 RECEIVE/PAY/AUTHORIZE/REFUND/WITHDRAW 五类动作能力准入快照。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：B2-PI-CAP` 后，才允许写支付工具能力 application facade Red 和最小实现；否则继续保持 Round 0 / summary_only。 |
| 2026-06-04 B2-FR-FAO Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#85-b2-fr-round-0-扫描2026-06-04` 已把资金责任解析低风险单切片收敛为 `B2-FR-FAO-CAD-001`；代码扫描确认 `SpendSubjectFundingRelationService` 仍是资源服务，`fundingAccountId` / `funding_account_id` 仍是主要目标字段，现有局部基线只证明真实资金账户责任关系，不证明信用账户或平台角色责任主体生产可用。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：B2-FR-FAO` 后，才允许写 funding-account-only 资金责任解析 application facade Red 和最小实现；若要支持 `targetSubjectType + targetSubjectId`，必须另起 `B2-FR-TARGET` 迁移 Grant。 |
| 2026-06-07 B2-FR-TARGET Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#861-fundingresponsibilitytargetgrantcandidate2026-06-07` 已把 VCC、信用账户和平台责任来源所需的资金责任目标主体迁移收敛为 `B2-FR-TARGET-CAD-001`；字段策略为 `targetSubjectType + targetSubjectId`，`fundingAccountId` 只能作为兼容读取、派生字段或历史查询字段，不能继续作为唯一写入事实并声明 VCC 子账户、信用账户或平台角色责任生产可用。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：B2-FR-TARGET`、账户层级依赖、schemaGate、写入范围、首批 Red 和验证命令后，才允许写目标主体迁移 Red、wallet application facade 最小实现或相关 DTO/DDL/H2/Entity/Mapper/route snapshot/fixture；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |
| 2026-06-04 B5-SR-CONTROL Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#87-b5-sr-control-round-0-扫描2026-06-04` 已把 Spend Rule 控制切片收敛为 `B5-SR-CONTROL-CAD-001`；代码扫描确认当前只有 BudgetGroup、`BUDGET_GROUP` ledger profile、余额控制调账和预算组余额查询等兼容路径，未形成 `SpendRuleDecisionLog`、`SpendControlActivity`、Spend Rule application facade 或预算控制投影生产模型。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：B5-SR-CONTROL` 并选择 `contract-only` 或 `ddl-backed` 后，才允许写 Spend Rule 控制 application facade Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得把 BudgetGroup ledger 兼容路径写成目标态 Done。 |
| 2026-06-04 B6/B8-PI-VIEW Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#89-b6b8-pi-view-round-0-扫描2026-06-04` 已把支付工具解释视图和治理重放切片收敛为 `B6-B8-PI-VIEW-CAD-001`；代码扫描确认正常交易投影发布、原 route snapshot 回放和交易投影治理重放已有局部边界基线，但尚未形成支付工具维度流水 query DTO、预算控制视图、规则命中时间线或完整 B8 Manifest/余额快照/指标水位闭环。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：B6-B8-PI-VIEW` 并选择 `query-contract-only` 或 `projection-store-backed` 后，才允许写支付工具解释视图 Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得把交易投影或治理重放结果反写成资金事实、账本事实或余额事实。 |
| 2026-06-06 B2-ACCOUNT-HIERARCHY Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#80-b2-account-hierarchy-round-0-扫描2026-06-06` 已把资金账户 / 信用账户多级结构、VCC 关联资金/信用子账户、父账户默认只读聚合和 `VCC_ACCOUNT` 禁止恢复收敛为 `B2-ACCOUNT-HIERARCHY-CAD-001`；代码扫描确认当前只有 `PREPAID_CARD`、`SHARED_CARD`、`CREDIT_CARD` 等账户类型枚举和 `funding_account_id` 资金责任关系局部基线，未形成 `AccountHierarchySnapshot`、`PostingRole`、父账户/根账户快照、层级版本或 posting role 公共承载。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：B2-ACCOUNT-HIERARCHY`、选择 `contract-only` 或 `ledger-snapshot-backed`、确认父账户/根账户快照策略、postingRoleDecision 和 DDL/H2 范围后，才允许写账户层级 application facade Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得把 VCC 卡、支付工具、父账户汇总、预算组或 Spend Rule 写成资金账户、ledger subject、route leg、posting subject、LedgerEntry subject 或余额投影主体。 |
| 2026-06-04 P2-VCC-PREPAID Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#811-p2-vcc-prepaid-round-0-扫描2026-06-04` 已把 VCC 预付资金确认切片收敛为 `P2-VCC-PREPAID-CAD-001`；代码扫描确认当前只有支付工具资源服务、资金责任关系资源服务、账户主体型直接交易/授权交易、route replay 和交易投影局部基线，未形成 `VccPrepaidFundingApplicationService`、VCC prepaid funding 请求模型、资金子账户/父账户快照契约或专项资金流实现。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：P2-VCC-PREPAID`、选择 `contract-only` 或 `funding-flow-backed`、确认资金子账户、父账户快照、背后资金来源策略和外部规则核验状态后，才允许写 VCC prepaid funding application facade Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得把 prepaid virtual card、shared card、父账户汇总、预算组或 Spend Rule 写成资金账户、ledger subject、route leg、posting subject、LedgerEntry subject 或余额投影主体。 |
| 2026-06-04 P2-VCC-LIFECYCLE Grant 候选补齐 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#813-p2-vcc-lifecycle-round-0-扫描2026-06-04` 已把共享卡和预付卡授权后生命周期回放切片收敛为 `P2-VCC-LIFECYCLE-CAD-001`；代码扫描确认交易层已有账户主体型授权生命周期、route replay 和交易投影局部能力，但尚未形成 VCC lifecycle application facade、原快照引用、绑定版本、外部事件幂等和差错/人工入口闭环。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：P2-VCC-LIFECYCLE`、选择 `contract-only` 或 `canonical-lifecycle-backed`、确认原快照策略、disputeDecision 和外部规则核验状态后，才允许写 VCC lifecycle application facade Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得按当前绑定、当前预算组、当前 Spend Rule 或当前默认资金责任重新选路。 |
| 2026-06-04 P2-GA-INBOUND Grant 候选补齐 | `docs/TDD设计/P2-业务能力包Round0准入卡.md#4-p2-ga-inbound-round-0-扫描2026-06-04` 已把全球账户入金匹配与外部受理在途切片收敛为 `P2-GA-INBOUND-CAD-001`；代码扫描确认当前只有外部账户引用、敏感值阻断、FX 端口和出款前准入局部基线，未形成 `GlobalAccountInboundApplicationService`、全球账户入金 Request/DTO、VA/银行流水匹配 facade、外部非终态不入账 Red 或入金幂等摘要。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：P2-GA-INBOUND`、选择 `contract-only` 或 `canonical-funds-backed`、确认账户解析决策、FX 决策和外部规则核验状态后，才允许写全球账户入金 application facade Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得把 VA、银行账户、Nostro、Vostro、外部银行流水或完整敏感账号写成资金账户、ledger subject、route leg、posting subject、LedgerEntry subject 或余额投影主体。 |
| 2026-06-04 P2-GA-OUTBOUND Grant 候选补齐 | `docs/TDD设计/P2-业务能力包Round0准入卡.md#10-p2-ga-outbound-round-0-扫描2026-06-04` 已把全球账户出款在途、成功回单和退汇边界切片收敛为 `P2-GA-OUTBOUND-CAD-001`；代码扫描确认当前已有出款前准入候选、外部账户引用、敏感值阻断和 FX 端口局部基线，但未形成 `GlobalAccountOutboundApplicationService`、全球账户出款 Request/DTO、外部非终态不误展示、成功回单终态、退汇关联原出金、费用和责任处理目标 Red。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：P2-GA-OUTBOUND`、选择 `preflight-contract-only` 或 `canonical-transit-backed`、确认 preflightDecision、transitDecision、returnDecision、FX 决策和外部规则核验状态后，才允许写全球账户出款 application facade Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得把外部提交、message sent、processing 或银行退汇误写成成功付款或普通退款。 |
| 2026-06-04 P2-GA-FX-FEE Grant 候选补齐 | `docs/TDD设计/P2-业务能力包Round0准入卡.md#12-p2-ga-fx-fee-round-0-扫描2026-06-04` 已把全球账户 FX quote 引用、费用分离和错币种阻断切片收敛为 `P2-GA-FX-FEE-CAD-001`；代码扫描确认当前已有 FX 端口、原币金额/汇率字段和直接交易手续费/退费局部基线，但尚未形成 `GlobalAccountFxFeeApplicationService`、全球账户 FX/费用 Request/DTO、quote approval snapshot、费用归因、FX P&L 专业确认、错币种无 quote 阻断、费用净额混淆阻断和不执行 FX 的目标 Red。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：P2-GA-FX-FEE`、选择 `contract-only` 或 `attribution-backed`、确认 `no-fx-execution` 或 `quote-validation-only`、`fee-attribution-only` 或 `fee-ledger-backed`、外部规则和专业确认状态后，才允许写全球账户 FX/费用 application facade Red 和最小实现；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也不得静默换汇、净额混淆费用或把资金服务写成 FX 执行系统。 |
| 2026-06-04 P2-ACQ-CAPTURE 设计-only 降级 | `docs/TDD设计/P2-业务能力包Round0准入卡.md#14-p2-acq-capture-round-0-扫描2026-06-04` 只作为收单 capture 归一、商户 `CLEARING` 和敏感数据边界的设计复核输入；此前收单 capture 实现候选不再作为当前可确认编码任务。 | 当前状态为 `DESIGN_ONLY_NOT_CODE_CANDIDATE`。收单能力仅做产品、DSL、系分、TDD、OpenSpec 和外部规则/PCI 边界设计，不写 Java、测试、DDL/H2 schema、公共契约或运行时配置；除非用户后续明确重新打开收单实现优先级，否则不得确认或消费 P2-ACQ-CAPTURE 实现授权。 |
| 2026-06-04 任务优先级重排 | 用户确认当前任务优先级按模块或能力排列：账本账目优先于钱包，钱包优先于交易层；2026-06-07 B7 Round 0 后，全局顺位补正为交易层之后先做清结算对账差错闭环，再做支付工具、VCC、全球账户支持，收单能力仅做设计不做实现。 | 后续 GSD-CAD 恢复入口必须先在账本账目、钱包基础能力、交易层或清结算对账中选择单一 Execution Grant；支付工具、VCC、全球账户只作为后置支持候选；收单只允许 design-only 文档和差距复核。 |
| P0 清结算与对账 | Round 0 已收敛到 `docs/TDD设计/B7-清结算与对账Round0准入卡.md`，默认首切片为 `B7-RECON-DIFFERENCE-MVP`；编码不开放。 | 当前状态为 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。后续只有用户明确确认 `Execution Grant：B7-RECON-DIFFERENCE-MVP`，并选择 `contract-only`、`ddl-backed` 或 `service-flow-backed`，同时列明补事实白名单、DDL/H2 范围、服务级 H2 流程、并发重跑、权限审计、使用者解释、外部规则和 Not Done 矩阵后，才能切到编码态；未确认前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |
| P0/P1 资金数据治理 | 可进入 TDD 分析；编码不开放。 | TDD 分析必须产出 `GOV-GATE-*`、`TDD-B8-RED-*`、治理物理落点候选、Manifest/H2 范围、dry-run/apply、指标水位隔离、治理导出和大数据消费边界测试矩阵；只有独立 Execution Grant 明确 Manifest、checkpoint、watermark、差异报告、人工处理、回滚/续跑和边界测试后，才能进入编码态。 |
| 本轮任务清理 | 只清理过时任务基线、任务计划和无效恢复入口。 | 不授权新切片；生产代码、测试代码、DDL/H2 schema、运行时配置均不纳入本轮写入范围。 |

当前编码推荐路径：先按 `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` 做 GSD + Goal 恢复入口确认，再按“账本账目 > 钱包账户/账户层级 > 交易内核 > 清结算对账 > 支付工具支持 > VCC/全球账户支持 > 收单 design-only”选择一个单一 MVP Execution Grant。`GSD1-LD-RED-002A` / `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 已消费，`GSD1-LD-RED-003` / `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 已消费，`GSD1-LD-RED-004A` / `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 已消费；当前无可沿用编码 Grant。下一轮必须重新确认新的单一 Execution Grant，可在预算组 control ledger 退出条件、钱包账户/账户层级、交易内核、清结算对账等范围中选择一个切片。

若业务要求优先 VCC，应先确认 `B2-ACCOUNT-HIERARCHY`，因为 VCC 关联资金/信用子账户、父账户快照、账目 profile 和卡绑定摘要是 prepaid funding、shared card 和卡账单投影的前置依赖。该优先只允许在 `contract-only/no-ddl` 范围内先做账户层级准入 Red，不允许跳过账本账目、资金责任、交易内核、清结算对账或支付工具准入后直接声明 P2 VCC 生产可用。账本账目优先看 ledger account、账目、posting、余额投影、清结算对账和账本余额快照的 Round 0；钱包优先看钱包账户、内部钱包入口、账户层级、资金责任解析和钱包 application facade。

交易层当前 Round 0 已收敛到 `B4-CANONICAL-REPLAY-FAILFAST-CAD-001`，本轮已验证 route replay 原快照复用的纯服务边界、原交易存在但 route snapshot 缺失时直接退款全链路失败无副作用，以及直接退款交易 flow 在当前绑定和资金责任变化后仍沿原支付快照回放；后续再拆余额控制调账审计、交易投影解释和授权/争议/VCC lifecycle 更大组合 replay flow。清结算对账需先补对账差错闭环 Round 0。`B4-TRX-EXPIRE`、`B4-FORCE-SETTLE`、`B4-NO-AUTH-REFUND`、`B4-DISPUTE-SEMANTIC-ALIGNMENT` 和 `B4-AUTH-RACE` 已进入代码基线，只作为回归资产。

`B2-ACCOUNT-HIERARCHY-CAD-001` 归入钱包账户/账户层级队列，是 VCC prepaid funding 和 lifecycle 的前置候选。

`B2-PI-CAP-CAD-001`、`B4-AUTH-PI-CAD-001`、`B5-SR-CONTROL-CAD-001` 和 `B6-B8-PI-VIEW-CAD-001` 归入支付工具及 Spend Rule 后置支持队列，整体排在账本账目、钱包基础能力、交易层和清结算对账之后。`P2-VCC-PREPAID-CAD-001`、`P2-VCC-LIFECYCLE-CAD-001`、`P2-GA-INBOUND-CAD-001`、`P2-GA-OUTBOUND-CAD-001` 和 `P2-GA-FX-FEE-CAD-001` 归入最后的 P2 支持队列。

收单 `P2-ACQ-CAPTURE` 和 `P2-ACQ-DISPUTE` 当前仅保留设计、外部规则、PCI 和边界差距复核，不作为可确认编码 Grant。后续若继续编码，必须按上述顺序优先从账本账目、钱包基础能力、交易层或清结算对账确认一个单一 Execution Grant；若越过该顺序选择支付工具、VCC 或全球账户，必须在 Grant 中显式说明业务原因和 P0/P1 依赖已满足；收单实现必须由用户另行明确重新打开优先级。

### 3.3.1 Agent Loop Engineering 恢复口径

当前可进入 `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-LOOP-2026-06-11`，但只能作为 `Plan Grant Loop / summary_only` 使用。Loop 的状态载体是 `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md`、`docs/TDD设计/GSD-1-账本账目状态账本.md`、`docs/TDD设计/GSD-1-账本账目Wave1执行计划.md`、本文件和 `openspec/changes/tdd-baseline-reset/tasks.md`；反馈源是 Harness checker、文档结构检查、`rg` 一致性扫描、`git status --short`、`git diff --check` 和用户确认。未确认新的单一 Execution Grant 前，Loop 只允许修正事实错误、索引漂移、恢复入口、验证矩阵和 handoff，不得写 Java、测试代码、DDL/H2 schema、公共契约、运行时配置、Git add/commit/push、联网、依赖安装或不可逆操作。连续 2 轮没有新增验证证据、状态变化或缺口收敛时，Loop 暂停并回到用户确认新的单一 Grant。

### 3.4 Execution Grant 最小字段

Execution Grant 必须是可执行授权，不是口头“可以开始”。字段缺失时，本任务只能做设计、Round 0、契约草案或 dry-run。

| 字段 | 必填口径 |
| --- | --- |
| 业务驱动准入页 | `productGoal`、`businessQuestion`、`mvpScenario`、资金事实、使用者可见结果、成功/失败终态、产品验收 ID、DSL caseId、系分章节和明确不做范围。 |
| MVP 场景和任务切片 | `mvpScenario`、A0 至 A4、B7、B8 或 P2 业务专项；不得混合授权。B1 至 B8 的细项编号只作为覆盖索引引用。 |
| 基线附件 | docs、OpenSpec、Harness Plan、Git 提交点、未提交变更清单和允许读取文件。 |
| 写入范围 | 生产代码、测试代码、公共契约、枚举、Request/Query/DTO、状态机、表结构、H2 schema、运行时配置的允许项和禁止项。 |
| 物理落点 | 模块、包、face/impl、端口、依赖方向、DTO/Entity/Mapper 归属和边界测试范围。 |
| 首批 Red | Red ID、目标测试资产、失败断言、验证命令和必须先失败的证据。 |
| 资金不变量 | 主体、账目、币种、周期、route、posting、entry、projection、幂等、失败无副作用和审计断言。 |
| 运营治理门禁 | 清结算、对账、出款、归档、重放、指标水位、白名单、Manifest、checkpoint、差异报告和人工处理入口。 |
| 外部规则核验 | 规则来源、版本或发布日期、生效日期、适用主体或范围、适用法域、核验日期、确认方和确认状态。 |
| 验证和停止条件 | 目标验证命令、`just test-boundary` 或等效边界检查、PMD/完整验证口径、失败停止条件和 Not Done 判定。 |

## 四、后续开发 Definition of Ready

任一 MVP 编码任务开始前必须满足：

1. 需求能映射到业务驱动准入页、产品验收 ID、DSL 契约用例、系分模块和 TDD 用例。
2. 明确本任务写入范围、只读范围、非目标和禁止事项。
3. 明确先写或先恢复哪些测试，且测试名称和断言来自 TDD 设计。
4. 明确是否涉及公共契约、枚举、表结构、状态机、金额、权限、审计或生产行为。
5. 明确验证命令；无法执行时必须说明环境、依赖或私有仓库限制。
6. 涉及资金红线、表结构、外部协议、清结算对象或归档重放时设置人工确认点。
7. 当前设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果已作为独立检查点冻结；若后续存在未提交的设计、OpenSpec 或 Harness Plan 变更，必须先提交或在 Execution Grant 中显式纳入本任务基线附件，未冻结前不得把后续编码实现混入同一轮交付。
8. 若本任务需要修改公共契约、枚举、服务入口、Request/Query/DTO、状态机或表结构，Execution Grant 必须显式写明“允许修改公共契约/新增枚举或事件/新增服务入口/扩展 Request/Query/DTO/修改状态机/表结构/新增模块”的取值和范围。
9. 若本任务触碰生产行为、外部结果回调、并发写入、余额锁定、清结算批次、归档重放或报表口径，Execution Grant 必须补齐容量假设、并发和锁策略、观测告警、回滚或补偿方案。
10. 若本任务触碰清结算、对账、出款、差错、补事实、冲正、调账或追偿，并允许通过交易层追加资金事实，Execution Grant 必须列出运营补事实命令白名单、来源单据、审批号、证据引用、幂等键、原事实引用、操作者、原因、可撤销边界和失败无副作用测试；未列入白名单的运营动作不得生成资金事实。
11. 若本任务触碰模块边界、能力域归属、事实端口层、公共依赖方向、投影反写边界或资金数据治理能力的物理落点，Execution Grant 必须显式声明受影响 ADR、治理物理落点选择、依赖方向、公共契约范围、DDL/H2 schema 范围、Mapper/Entity 归属、指标水位隔离测试、边界测试范围、`just test-boundary` 或等效静态检查命令。
12. 编码准入或重大设计 CR 前，以当前 Source of Truth、Harness Plan、Git 提交点和用户确认作为执行依据；若发现规格缺口，先补齐权威文档或人工确认点。
13. 若工作树存在未提交变更，Execution Grant 必须列出允许读取和允许写入的未提交文件；未列入的变更只能作为草稿参考，不得作为冻结基线或 Done 证据。
14. 若本任务涉及平台补贴、储值券、预付券、礼品卡、客户资金、商户待结算资金、负债、备付或财务收入成本口径，Execution Grant 必须记录财务、税务、会计、合规或法务确认状态；未确认时只能进入契约或设计验证，不得进入生产资金流 Done 结论。
15. 若本任务涉及含权益交易的 route/posting/replay、清结算、对账、投影、归档、冷热读取或治理重放消费，Execution Grant 必须声明 Phase 能力边界、JSON 夹具级别、权益快照事实源、零实付表达、平台补贴表达、独立伴随指令原子性、储值预付口径、退款分摊粒度、退款分摊确定性规则版本、分摊依据、稳定组件顺序、舍入模式、尾差归属、组件剩余额度版本、幂等摘要字段、并发保护、历史无权益快照处理策略、补充权益事实模型、专业确认状态、审计证据包、使用者解释视图、证据最小化和外部规则核验状态；外部规则核验状态必须至少包含规则来源、版本或发布日期、生效日期、适用主体或适用范围、适用法域、核验日期、确认方和确认状态；缺任一项时不得进入生产资金流 Done 结论。
16. 若本任务涉及营销账户、营销成本、权益负债、合作方补贴、商户让利归因、营销留置或补贴冲回，Execution Grant 必须声明营销账户承接方式（资金账户、平台责任资金账户、账户 profile 或另行确认的 subject type）、账户 profile、专业确认状态、权益快照事实源、核销或占用引用、退款回放策略、清结算/对账消费边界、是否允许落地 `FundsBenefitFundingApplicationService` Java 公共契约以及首批 `TDD-BEN-MKT-*` / `TDD-BEN-ENTRY-006` / `TDD-BEN-RED-031` 至 `TDD-BEN-RED-034`；`FundsBenefitFundingApplicationService` 是已确认保留的权益资金责任应用门面命名，不得把营销账户当支付工具、活动、券实例、营销规则系统或券包库存，不得新增 `FundsMarketingTransactionService` 或权益授权/结算/退款平行生命周期。若需要独立伴随权益指令，必须同时声明 `companionGroupSn`、原子性模式、补偿策略、投影合并键和失败无副作用测试。
17. 若本任务临时使用 `contextVariables` 承接权益追溯信息，Execution Grant 必须列出允许字段白名单和迁移退出条件；组件金额、资金责任、退款处置完整内容和当前营销规则不得作为 `contextVariables` 生产事实。
18. 若本任务涉及 VCC 或全球账户业务能力包，Execution Grant 必须声明业务分册、业务验收 ID、场景 pack 范围、归一资金动作、允许新增或修改的公共契约、外部引用脱敏字段、外部适配证据包、外部规则核验状态、P0/P1 回归测试、业务专项 TDD 用例和 Not Done 红线；VCC 还必须声明卡产品/资金模式（credit、debit、charge、prepaid、shared 等）、`PaymentInstrument` 边界、卡绑定资金/信用子账户、父账户快照、预付资金责任来源、共享卡绑定快照、内部主体能力选择、VCC 对账来源对象采纳范围、差错关闭条件和禁止新增的账户/账本/余额投影主体；不得因 prepaid/shared 卡产品类型自动创建卡号账户、预算组或独立 `VCC_ACCOUNT`，不得让多张共享卡绑定同一子账户，不得用供应商账单、清算文件、funding statement、对账结果或财务凭证直接改历史分录或余额；未声明时只能做产品、DSL、系分或 contract-only 验证，不得进入生产资金流 Done 结论。收单业务当前仅允许 design-only 设计、边界复核和差距登记，不适用常规 P2 编码 Execution Grant；除非用户后续明确重新打开收单实现优先级，否则不得写生产代码、测试代码、公共契约、DDL/H2 schema 或运行时配置。
19. 若本任务触碰交易 Request 或任何 `contextVariables` 写入入口，Execution Grant 必须声明是否复用 `ReadonlyContextVariables`、是否影响请求摘要、是否允许新增一等字段或不可变事实快照，以及必须回归的敏感上下文和权益核心字段阻断测试；不得恢复 `WritableContextVariables` 字段、`FundsRequestContextVariables` request 专属快照工具或把核心资金事实塞回普通上下文。
20. 若进入 CAD 自动模式或声明完整本地基线复核，验证命令优先使用 `just verify-cad`；若本轮只执行专项验证或仅修改文档，交付说明必须写清未执行完整门禁的原因。

DoR 检查步骤：

| 步骤 | 检查问题 | 未通过时处理 |
| --- | --- | --- |
| 1. 范围是否可追溯 | 是否能列出产品验收、DSL caseId、系分分册、TDD 用例和红线编号。 | 回补设计映射，不进入编码。 |
| 2. 写入是否已授权 | 是否明确允许修改公共契约、服务入口、枚举、Request/Query/DTO、状态机、表结构、模块和测试资源。 | 补 Execution Grant 或缩小写入范围。 |
| 3. 测试是否先行 | 是否明确先写或先恢复哪些失败测试、契约测试或服务级测试。 | 先补测试计划，不直接实现。 |
| 4. 生产风险是否有边界 | 是否涉及并发、锁、外部回调、清结算批次、归档重放、敏感数据或报表口径。 | 补人工确认点、回滚/补偿和观测告警要求。 |
| 5. 现有差距是否已复核 | 是否查过当前代码、DDL/H2、测试资产和现有行为差距。 | 先做差距复核，再拆实现。 |
| 6. 权益准入是否完整 | 含权益任务是否已声明 Phase 能力边界、JSON 夹具级别、事实源、零实付表达、平台补贴表达、独立伴随指令原子性、储值预付口径、退款分摊粒度、退款分摊确定性规则、历史无快照处理、补充权益事实模型、专业确认状态、审计证据包、使用者解释视图、证据最小化和外部规则核验状态；外部规则核验状态是否包含规则来源、版本或发布日期、生效日期、适用主体或适用范围、适用法域、核验日期、确认方和确认状态；涉及投影、归档、冷热读取或治理重放时是否同步声明对应消费边界和最终重校验点。 | 降级为 contract-only 或补齐决策卡。 |
| 6A. 营销账户准入是否完整 | 涉及平台补贴、储值负债释放、合作方补贴、商户让利归因、营销留置或补贴冲回时，是否声明账户 profile、账户引用、专业确认状态、核销或占用引用、退款回放策略、清结算/对账消费边界和 `TDD-BEN-MKT-*` / `TDD-BEN-RED-031` 至 `TDD-BEN-RED-033`；是否明确营销账户不是支付工具、活动、券实例、营销规则系统或券包库存。 | 降级为权益契约、设计验证、阻断或人工处理。 |
| 7. P2 业务能力包准入是否完整 | VCC 或全球账户是否已声明业务分册验收 ID、场景 pack、归一资金动作、外部引用脱敏、外部适配证据包、外部规则核验、P0/P1 回归和 `TDD-P2-*` 专项用例；VCC 是否额外声明对账来源对象、匹配键、差错处理动作和审计证据；收单是否仍保持 design-only。 | VCC/全球账户缺任一项时只能做业务专项设计、DSL 契约草案或 contract-only；不得实现生产资金流。收单缺 design-only 边界时先回补设计，不进入实现。 |
| 8. 能力域首批 Red 是否先行 | 清结算与对账是否先补 `TDD-B7-RED-001` 至 `TDD-B7-RED-007`；资金数据治理是否先补 `TDD-B8-RED-001` 至 `TDD-B8-RED-005`；Red 是否证明当前实现会错误放行。 | 未补 Red 前只能做设计、契约草案或 dry-run，不进入 Green 实现。 |
| 9. 运营和治理高危门禁是否显式 | 清结算对账触发资金事实时是否列出运营补事实命令白名单；治理任务是否确认 `GOV-GATE-*`、物理落点、依赖方向、公共契约、DDL/H2、Mapper/Entity、边界测试、指标水位隔离测试和大数据消费边界。 | 补 Execution Grant 或缩小到只读/contract-only 范围。 |

## 五、后续开发 Definition of Done

任一 MVP 编码任务完成前必须满足：

1. 代码、测试、DSL 契约和系分设计一致，若发现设计错漏，先回补设计再继续。
2. 覆盖本任务对应 TDD 用例和必须失败红线。
3. 资金变化测试同时断言状态、route snapshot、posting plan、ledger entry、余额投影和幂等。
4. 不恢复旧测试源码或旧过渡断言，不以旧测试通过替代当前最终版 TDD 断言。
5. 不引入未确认概念、无主依赖、真实外部调用、生产配置或敏感数据。
6. 交付说明列出覆盖用例、验证命令、验证结果、未覆盖项和残余风险。
7. 涉及生产行为的任务必须说明并发边界、幂等和锁保护、告警指标、降级/回滚或补偿路径。
8. 涉及架构边界的任务必须说明是否保持已接受 ADR，是否新增或调整架构边界测试，以及是否通过边界验证。

完成结论只能使用下列口径：

| 结论 | 使用条件 |
| --- | --- |
| Done | Execution Grant 覆盖范围内的代码、契约、DDL/H2、测试、验证命令和待确认边界均已闭合。 |
| Conditional Done | 本任务 P0/P1/P2 能力证据闭合，但外部规则、非本任务扩展、性能容量或人工确认项仍有明确不覆盖范围。 |
| Not Done | 任一资金红线、公共契约授权、DDL/H2、服务级测试、失败无副作用、幂等或审计证据缺失。 |

中文评审结论只用于文档评审表达：通过不等于 Done，带条件通过不等于自动 Conditional Done。MVP 编码任务只有在 Execution Grant 覆盖范围内的实现、测试和验证证据满足上表时，才能使用 Done 或 Conditional Done；缺少本任务生产目标必需证据时必须写 Not Done。

DoD 检查步骤：

| 步骤 | 检查问题 | 未通过时结论 |
| --- | --- | --- |
| 1. 范围是否一致 | 实际修改是否仍在 Execution Grant 写入范围内，未越过只读范围和非目标。 | Not Done。 |
| 2. 契约是否闭合 | face、DTO、Request、Query、枚举、错误码、DSL fixture 和目标态语义说明是否齐全。 | Not Done 或 Conditional Done。 |
| 3. 资金断言是否齐全 | 有资金变化的测试是否覆盖状态、route、posting、entry、projection、幂等和审计。 | Not Done。 |
| 4. 数据落地是否齐全 | 涉及持久化时，DDL/H2 schema、Entity/Mapper、唯一键、索引和迁移影响是否说明。 | Not Done。 |
| 5. 验证是否执行 | 编译、专项测试、边界测试或无法执行原因是否列明。 | Not Done。 |
| 6. 风险是否收口 | 未覆盖项、外部规则、生产容量、回滚/补偿和人工处理是否列为残余风险。 | Conditional Done 或 Not Done。 |

## 六、语言与协作规则

OpenSpec、Superpowers 和 Harness 协作摘要默认使用简体中文。代码标识符、协议字段、枚举、命令、包名、类名和第三方产品名保持英文原文。

本目录只记录规格与协作基线。进入 CAD Mode 仍需要单独确认 Execution Grant、Git 策略、人工确认点和停止条件；具备完整本地验证条件时，以 `just verify-cad` 作为 CAD 轮次的完整门禁命令。
