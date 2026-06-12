# GSD + Goal 生产可用 MVP 推进计划

## 1. 文档定位

本文是 wind-funds 在 GSD + Goal 模式下的生产可用 MVP 推进基线，用于把业务目标、依赖顺序、Execution Grant 队列、验证矩阵和停止条件收束到一份可恢复、可审查、可交付的任务计划。

本文不是新的 PRD，不替代产品设计、DSL 设计、系分设计或 TDD 设计；也不是生产发布授权。公共契约、生产代码、DDL/H2 schema、运行时配置、Git 操作和高风险资金语义变更仍必须依赖单一 `Execution Grant`；用户明确要求进入 `Agent Loop Engineering` / `GSD + Goal 按任务计划推进` 时，可形成受控 `Plan Grant`，只允许选择低风险、本地可验证、无公共契约和无 DDL 的测试覆盖或文档同步切片。

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-2026-06-07` |
| Goal 名称 | 金融创业公司 MVP 资金底座生产可用推进 |
| Goal 边界 | 以 VCC 发卡、VCC 交易处理、全球收付款为业务目标输入，但按依赖关系优先交付账本账目、钱包账户、交易内核、清结算对账等被依赖方能力。 |
| Owner | AI Native 流程编排负责 GSD/Goal/门禁；产品架构专家负责资金语义、业务验收和外部规则待确认项；资深架构师负责系统边界、TDD、Execution Grant、代码实现和验证。 |
| 写入范围 | 默认只允许写 `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md`、B2/B4/B7 Round 0 准入卡、TDD README、docs README、OpenSpec project 和 Harness tasks 索引；进入 `Plan Grant` 后，可额外写入所选低风险切片的目标测试文件，并必须回写验证证据和 Not Done 边界。 |
| 只读范围 | `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`、`openspec`、wind-funds 源码与测试、fincone、fincone-issuing、nobe 参考代码、公开可访问支付语义资料。 |
| Git 策略 | 默认 `summary_only`；未获显式 Git 授权前不自动 git add、git commit。 |
| 当前状态 | `SUPERSEDED_BY_GSD2_BASELINE_RESET_HISTORY_ONLY`；新的活跃工作流入口见 [GSD-2-新基线工作流规划.md](GSD-2-新基线工作流规划.md)。 |

Harness 恢复字段：

| 字段 | 内容 |
| --- | --- |
| 原子任务 | 历史 Goal Ledger、依赖顺序、Wave 计划和 Execution Grant 队列归档。 |
| 所属阶段 | History / Baseline Evidence / Superseded by GSD-2。 |
| 写入文件 | `docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md`、`docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md`、`docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md`、`docs/TDD设计/B7-清结算与对账Round0准入卡.md`、`docs/TDD设计/README.md`、`docs/README.md`、`openspec/project.md`、`openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 只读参考 | wind-funds 设计与代码、fincone 产品架构、fincone-issuing v3 设计、nobe 真实业务代码和公开可访问支付语义资料。 |

GSD-2 迁移裁决：本文保留上一轮 GSD + Goal 的证据、消费记录、Not Done 边界和 handoff，不再承载当前活跃计划。旧的 `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`、`QUEUED_AFTER_P0_P1` 和 `PARTIAL_*_NOT_DONE` 候选均已移出活跃执行队列；后续必须通过 [GSD-2-新基线工作流规划.md](GSD-2-新基线工作流规划.md) 重新选择 Task ID、Goal 映射、写入范围、验证命令和单一 Execution Grant。

## 2. 证据来源和边界

本计划采用“本项目权威文档优先，sibling repo 和外部资料只做校准”的证据顺序。

| 来源 | 当前用途 | 边界 |
| --- | --- | --- |
| wind-funds 产品、DSL、系分、TDD、OpenSpec | Source of Truth，决定资金主体、能力优先级、模块边界、TDD Red 和 Execution Grant。 | 不因本计划扩展设计目标或自动授权编码。 |
| fincone 产品架构 | 校准业务主线：全球支付、VCC 发卡、开发平台、未来收单和运营治理。 | 不把 fincone 业务域对象直接沉入 wind-funds。 |
| fincone-issuing docs/v3 | 校准发卡系统和 wind-funds 的防腐接入边界：VCC 产品域、发卡资源域和资金底座域分层。 | 草稿文档如与 wind-funds 最新设计冲突，以 wind-funds 为准。 |
| nobe 现有 VCC/全球账户项目 | 作为真实业务能力参考：充值、提现、冻结、解冻、共享卡调额、VCC 交易明细、全球账户收付款和对账入口。 | 不复制 nobe 的 VCC 作为交易账户或平行账本设计。 |
| 陈天宇宙公开文章和公开支付资料 | 校准“清算、结算、清结算”在不同语境下容易混用，必须先定义本系统口径。 | 不作为监管、法务、会计或通道规则结论。微信公众号原始链接当前只能访问到微信验证页，不能直接核验正文。 |

外部参考核验状态：

| 规则来源 | 版本或发布日期 | 生效日期 | 适用主体或适用范围 | 适用法域 | 核验日期 | 确认方 | 确认状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 陈天宇宙公开文章《万字：“清算、结算、清结算”的区别》，人人都是产品经理页面 | 2024-05-08 | 不适用 | 支付产品语义参考，不作为外部规则 | 不适用 | 2026-06-07 | 产品架构专家只读核验 | 可作为语义参考；非规则核验通过 |
| 微信公众号链接 `https://mp.weixin.qq.com/s/mTLMJVO4_NNlENZP8utZGA` | 未能核验 | 不适用 | 用户指定参考来源 | 不适用 | 2026-06-07 | Codex curl 只读核验 | 当前环境返回微信验证页，不能作为已核验来源 |

### 2.1 来源采纳分级门禁

本节定义外部资料、兄弟项目和公开文章进入 GSD/Grant 的采纳方式。任何来源都不能越过 wind-funds 的 PRD、DSL、系分、TDD、OpenSpec 和单一 `Execution Grant`。

| 采纳级别 | 适用来源 | 可用于 | 禁止用于 |
| --- | --- | --- | --- |
| `source-of-truth` | wind-funds 当前 PRD、DSL、系分、TDD、OpenSpec、源码、测试、验证命令和确认时 Git HEAD。 | 确定资金主体、模块边界、公共契约、TDD Red、验证命令和 Grant 写入范围。 | 用外部资料覆盖已确认的本仓库资金红线。 |
| `advisory-reference` | fincone 产品架构、fincone-issuing docs/v3、Highnote 等公开/兄弟系统设计资料。 | 校准业务目标、产品域边界、防腐层、命名和场景拆解。 | 直接复制对象、表、状态机、接口或通道规则到 wind-funds。 |
| `scenario-seed` | nobe 现有 VCC/全球账户项目、历史业务代码、运营样例。 | 提供真实场景种子、异常路径、验收样例和目标 Red 候选。 | 把历史实现当目标架构，或把 VCC/VA/外部账户写成平行账本主体。 |
| `semantic-reference` | 陈天宇宙公开文章、人人都是产品经理等公开支付语义资料。 | 统一术语、解释语境差异、识别清算/结算/清结算等概念歧义。 | 作为监管、法务、会计、卡组织、银行、ACH、SWIFT、FX 或通道规则结论。 |
| `blocked-reference` | 当前环境无法核验正文、来源不完整、版本不明或确认方缺失的资料。 | 记录待确认项和不采纳原因。 | 写入产品结论、系统设计、Execution Grant、测试断言或生产 Done 证据。 |

任一后续 Grant 若引用 `advisory-reference`、`scenario-seed` 或 `semantic-reference`，必须在 Grant 中列明来源路径或 URL、版本或发布日期、核验日期、确认方、采纳级别、采纳字段或场景、Not Done 边界和与 wind-funds Source of Truth 的冲突处理。若引用外部规则，仍必须通过外部规则核验字段检查，并交由法务、合规、财务、通道或持牌机构确认。

## 3. Goal 卡

| 字段 | 内容 |
| --- | --- |
| Objective | 交付一个最小但生产可用的资金底座 MVP，支撑金融创业公司主业务：VCC 发卡、VCC 交易处理、全球收付款，并能解释清分、清算、结算、对账、资金归属和余额变化。 |
| Success Criteria | 账本账目、钱包账户、交易内核、清结算对账和 VCC/全球账户接入均按单一 MVP 切片形成真实代码、服务级测试、H2/fixture、幂等、失败无副作用、审计和验证命令证据。 |
| Production Evidence | 使用真实 Spring Bean、真实 H2 schema、真实服务入口和可追溯账务断言；不能用 mock 流程、内存版业务 Service、空 facade、只断言状态或数量的测试冒充生产完成。 |
| Non-goals | 不做完整发卡处理商、卡组织协议、PAN/CVV/HSM、完整 Spend Rule 引擎、完整 FX 执行、收单生产实现、监管或会计最终结论。 |
| 状态 | 历史当时曾开启运行时 Goal；2026-06-12 已迁移到 GSD-2，本文不再承载当前运行时 Goal 或 Execution Grant。 |
| 停止条件 | 缺少单一 Execution Grant、跨能力域写入、需要改公共契约或 DDL 但未授权、外部规则未确认却要自动放行、测试无法证明资金不变量、工作树冲突无法安全合并。 |

### 3.1 Goal Ledger 当前更新

| 字段 | 内容 |
| --- | --- |
| Goal ID | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-2026-06-07` |
| 当前状态 | `Active / B4 current binding replay flow verified / Summary only`。 |
| 最近证据 | 用户明确要求重新加载流程技能并进入 Agent Loop Engineering，使用 GSD + Goal 按任务计划推进；本轮据此形成受控 `Plan Grant`，未等待新的人工 Execution Grant 即选择低风险 B4 目标测试覆盖。2026-06-07 已消费 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 并完成 002A 覆盖补齐；2026-06-07 已消费 `Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 并完成 003 投影回归；2026-06-11 已消费 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 并完成 004A BudgetGroup 兼容 guard；2026-06-11 Plan Grant 已为 B4 增加“授权后继缺原授权事实 fail-fast 且无资金副作用”服务级测试覆盖，`FundsAuthorizationTransactionFlowTests` 30 tests 通过；随后用户确认并消费 `Execution Grant：B3-DIRECT-REFUND-REFERENCE-REPLAY`，补齐直接退款原交易引用、原 route snapshot 回放、独立退款事实和缺原交易/累计超额失败无副作用，`FundsDirectTransactionFlowTests` 49 tests、`test-transaction` 102 tests、`test-boundary` 152 tests 通过；随后在 Plan Grant 下验证 `R0-TRX-REPLAY-002` 的纯 route replay 边界，`DefaultRouteReplayServiceTests` 9 tests 通过，证明 replay resolver 使用原 `RouteSnapshot` 中的支付工具快照、外部账户快照和资金责任决策，不被当前请求的工具或账户上下文覆盖；随后补齐 `R0-TRX-REPLAY-001` 缺原 route snapshot 的直接退款交易全链路失败无副作用目标测试；本轮继续补齐 `R0-TRX-REPLAY-002` 的直接退款交易 flow 子场景，证明原支付交易 route snapshot 已固化旧支付工具和旧资金责任时，后续退款仍沿原快照回放并保持余额、账务事实和新退款 route snapshot 归因一致，`FundsDirectTransactionFlowTests` 51 tests 通过。 |
| 变化假设 | Goal 从文档化推进基线升级为当前会话运行时目标；当用户明确要求 `Agent Loop Engineering` / `GSD + Goal 按任务计划推进` 时，Loop 可自动选择低风险测试覆盖或文档同步切片，但不自动扩大到公共契约、DDL、生产代码或 Git 操作。 |
| 开放风险 | 工作树存在多项未提交文档和测试变更，后续必须区分用户已有变更和本轮变更；B4 已补授权后继缺原事实、纯 route replay 边界、缺原 route snapshot 直接退款全链路覆盖和直接退款当前绑定/资金责任变化后沿原快照回放覆盖，B3 只关闭直接退款原交易引用回放，不代表交易内核全域生产 Done。 |
| 下一 owner | AI Native 流程编排继续按 Plan Grant 选择低风险本地切片；公共契约、DDL、生产代码和 Git 操作仍需用户确认单一 Execution Grant 或显式授权。 |
| 下一动作 | 若继续 B4，应回到 `B4-CANONICAL-REPLAY-FAILFAST` 剩余缺口，优先选择交易投影解释、余额调账审计，或授权/争议/VCC lifecycle 更大组合 replay flow 中的单一切片；否则切换到 B2 账户层级 contract-only/no-ddl 准入 Red 或 B7 对账差错 Round 0 中的低风险切片；不得沿用已消费的 004A 或 B3。 |
| 复盘 / 知识回流位置 | 本文、`docs/TDD设计/GSD-1-账本账目状态账本.md`、`openspec/project.md` 和 `openspec/changes/tdd-baseline-reset/tasks.md`。 |

### 3.2 runtimeEvidence2026-06-07

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just mvn-version` | PASS | Maven 3.6.3 + Amazon Corretto 21.0.11。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just compile` | PASS | reactor 14/14 modules `BUILD SUCCESS`。 |
| `WIND_FUNDS_JAVA_HOME=<Java21 home> just test-one DefaultLedgerTransactionPostingServiceImplTests tests` | PASS_AFTER_ESCALATION | 沙箱内因 embedded Redis 端口绑定失败；提权复跑后当前工作树目标测试 10 tests / 0 failures / 0 errors。目标测试文件当前未被 Git 跟踪，本证据不等于已冻结 Git 基线。 |

本节是运行时 Goal 证据，不是编码授权。后续进入任何 Java、测试、DDL 或公共契约写入前，仍必须确认单一 `Execution Grant`。

### 3.3 产品和架构准入摘要

本节只把 GSD + Goal 的产品准入和架构准入锚点显式化，方便产品专家、架构师和 Harness checker 复核；不扩大写入范围，不替代单一 `Execution Grant`。

| 产品准入项 | 当前口径 |
| --- | --- |
| 业务目标、用户价值、成功指标和非目标 | 业务目标是交付金融创业公司 MVP 资金底座，支撑 VCC 发卡、VCC 交易处理和全球收付款；用户价值是让产品、运营、财务和研发能用同一套资金事实解释账户、交易、清结算和对账；成功指标是每个 Grant 都有真实 Spring Bean、H2/fixture、账务事实、余额投影、幂等、失败无副作用、审计和验证命令；非目标是不做完整发卡处理商、卡组织协议、完整 FX 执行、完整 Spend Rule 引擎或收单生产实现。 |
| 能力地图和能力域 | 能力地图按账本账目、钱包账户、交易内核、清结算对账、支付工具/Spend Rule、VCC、全球账户和收单 design-only 排列；前台能力只进入业务 application facade，后台能力承接运营解释和差错，数据能力承接账本事实、余额投影、交易投影和对账差错。 |
| 业务对象、对象模型、字段口径、生命周期和状态 | 业务对象包括资金账户、信用账户、账户层级、支付工具、资金责任、资金交易、授权交易、余额控制、route snapshot、ledger transaction、LedgerEntry、余额投影、交易投影、对账批次和差错单；对象模型必须区分账务主体、工具快照、控制规则和只读投影；字段口径和生命周期状态以 PRD、DSL、系分和 TDD 为准。 |
| 业务流程、主流程、异常流程和人工兜底 | 主流程按请求、准入、路由、账务、投影、对账和解释推进；异常流程包括缺快照、金额不闭合、重复同键不同摘要、外部非终态、差错阻断和敏感字段越界；人工兜底只能进入差错、审批或补事实白名单，不直接改历史资金事实。 |
| 规则矩阵、触发条件、判断逻辑、优先级和版本 | 规则矩阵按账户主体、资金责任、route snapshot、posting 平衡、幂等摘要、Spend Rule 控制、对账差错、外部规则和敏感字段阻断拆分；触发条件和判断逻辑进入对应 Red；优先级按被依赖方能力先行；规则版本、审计和验收来源必须在 Grant 中列明。 |
| 运营后台、指标、报表、审计和数据口径 | 运营后台只展示交易事实、失败原因、原路径、差错状态、审批和审计引用；指标和报表消费只读投影和账本事实；数据口径必须区分资金事实、账本事实、交易投影、余额投影、对账差错和外部非终态。 |
| 风险、待确认、验收、确认方和发布 | 风险是把文档 Done 当生产 Done、把支付工具或预算组当账务主体、用内存版实现冒充生产能力或跳过 P0/P1 依赖；待确认项包括单一 Grant、公共契约、DDL/H2、外部规则、专业确认和发布策略；验收和发布必须由产品、架构、研发、测试、运营、财务、风控、安全和合规按范围确认。 |

| 架构准入项 | 当前口径 |
| --- | --- |
| 背景、目标、非目标和成功标准 | 背景是 VCC、全球账户和清结算能力依赖资金底座先证明账本、钱包和交易内核；目标是按依赖顺序交付可验证的 MVP；非目标是不一次性铺开 P2 业务包或替换交易 canonical 入参；成功标准是每个切片都有真实链路、数据方案、事务边界、一致性、补偿或阻断、对账或差错证据、测试和回归。 |
| 现状、约束、问题和影响范围 | 现状是账本 001A/001B 已有当前工作树目标测试证据但目标测试文件尚未被 Git 跟踪；交易内核已补一个 Plan Grant 目标测试覆盖；钱包账户和清结算对账仍待 Grant。约束是 Plan Grant 只允许低风险测试覆盖或文档同步，公共契约、DDL/H2、生产代码和运行时配置仍需单一 Execution Grant；问题影响范围覆盖 core、wallet、transaction、ledger、reconciliation、governance 和 tests。 |
| 核心决策、职责边界和取舍 | 核心决策是账户主体型交易内核不被支付工具替换，wallet 负责 application facade，transaction 负责资金事实和生命周期，ledger 负责账本事实，reconciliation 负责差错闭环，governance 负责归档重放；取舍是先做被依赖方能力，再做 VCC 和全球账户业务入口。 |
| 接口契约、入参、错误码、幂等和兼容 | 接口契约、入参、出参、错误码、幂等摘要和兼容策略必须由单一 Grant 列名；账户层级、交易 replay、清结算差错、支付工具准入和 P2 业务 facade 不得混在同一轮修改。 |
| 数据方案、事务边界、一致性、补偿和对账 | 数据方案必须说明表、H2 schema、Entity/Mapper、fixture 和投影落点；事务边界必须证明失败无半截 route、posting、entry、projection 或差错副作用；一致性和补偿通过标准逆向交易、差错白名单或对账重跑闭合。 |
| 可靠性、安全、权限、审计和告警 | 可靠性覆盖重复请求、并发、重跑、外部非终态和范围锁；安全覆盖敏感字段阻断、外部引用脱敏和权限边界；审计和告警覆盖操作者、规则版本、traceId、差错状态和生产 Runbook。 |
| 验证方案、测试、静态检查和回归 | 验证方案按 Grant 选择 `just test-one`、分组测试、`just compile`、`just pmd`、`just verify-cad` 和 `git diff --check`；测试必须覆盖真实 Spring Bean、H2、资金断言、边界测试和回归。 |
| 发布、灰度、回滚、风险和待确认 | 本计划不发布生产能力；发布、灰度、回滚、风险接受、待确认外部规则和专业确认只在具体 Grant 完成代码、测试、DDL/H2、审计和验证证据后进入生产变更评审。 |

### 3.4 GSD 系分结构自检

本节只用于让 GSD + Goal 计划也满足系统设计准入结构，不替代各 Execution Grant 的详细设计。

| 系分字段 | 当前口径 |
| --- | --- |
| 需求背景、问题和不做的风险 | 需求背景是 wind-funds 需要支撑 VCC 发卡、VCC 交易处理和全球收付款，但必须先把账本账目、钱包账户、交易内核、清结算对账等被依赖方能力做成生产可用；问题是直接打开 P2 业务入口会绕过资金主体、账务事实、对账差错和回放证据；不做的风险是继续产出 mock facade、空服务或只断言状态数量的样子货。 |
| 目标、非目标、系统边界、数据边界和安全边界 | 目标是按依赖顺序形成可确认的单一 Execution Grant 队列；非目标是不在本计划中写 Java、测试、DDL/H2、公共契约或运行时配置；系统边界是 core、wallet、transaction、ledger、reconciliation、governance 和 tests 各守职责；数据边界是资金事实、账本事实、只读投影、差错事实和外部非终态分开；安全边界是敏感字段、外部规则和专业确认未满足时不放行。 |
| 概要设计、核心方案、关键依赖、同步和异步 | 概要设计是账本账目 -> 钱包账户/账户层级 -> 资金责任 -> 交易内核 -> 清结算对账 -> 支付工具/Spend Rule -> VCC/全球账户；核心方案是每个 Wave 只推进一个可验证能力；关键依赖包括 `GSD1-LEDGER-BOUND-LEDGER`、`B2-ACCOUNT-HIERARCHY`、`B2-FR-TARGET`、`B4-CANONICAL-REPLAY-FAILFAST` 和 `B7-RECON-DIFFERENCE-MVP`；同步路径为服务级用例和交易事实，异步路径为投影、重放、对账和治理。 |
| 详细设计、模块、类设计、接口设计和数据设计 | 详细设计下沉到对应准入卡和 Grant；模块包括 `ledger`、`wallet`、`transaction`、`reconciliation`、`governance` 和 `tests`；类设计、接口设计和数据设计必须由单一 Grant 列名，包括 Request/DTO、Service、Entity/Mapper、H2 schema、route snapshot、ledger entry、projection、difference 和 fixture。 |
| 状态机、主流程、异常流程、补偿流程和人工介入 | 状态机按 Goal `Active / Verified / Closed` 和 Grant `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED / CODE_AUTHORIZED / DONE` 区分；主流程是确认单一 Grant、Red、Green、Review、验证和提交；异常流程包括 Grant 越界、验证失败、字段策略冲突、工作树冲突和外部规则未确认；补偿流程只通过原路径回放、对账差错、白名单补事实或人工介入处理。 |
| 非功能、性能、容量、可用性、兼容性和生产就绪 | 非功能要求幂等、并发、失败无副作用、审计、可重放和可解释；性能和容量在具体生产变更前确认；可用性要求失败可解释和可恢复；兼容性要求不破坏 face/core/ledger 公共契约；生产就绪必须等具体 Grant 的代码、测试、DDL/H2、审计、外部确认和发布回滚证据闭合。 |
| 测试设计、单元测试、集成测试、契约测试和回归测试 | 测试设计由每个 Grant 的首批 Red 驱动；单元测试保护纯策略，集成测试保护真实 Spring Bean/H2 服务流，契约测试保护 DSL/Request/DTO/route snapshot，回归测试覆盖账本、钱包、交易、清结算、边界、治理和 PMD；仅文档变更使用结构脚本与 `git diff --check`。 |
| 研发计划、负责人、里程碑和验收方式 | 研发计划按 Wave 队列推进；负责人包括产品架构专家、资深架构师、研发、测试、运营、财务、风控、安全、合规和用户确认方；里程碑是 Round 0、Grant 确认、TDD/CAD、CR、验证、提交和复盘；验收方式是 AC/DSL/TDD/RED 映射、验证命令、Not Done 清单和残余风险签出。 |

### 3.5 Agent Loop Engineering 契约

本节把当前 GSD + Goal 持续推进方式收敛为可停止、可恢复的 Agent Loop 契约。Loop 只负责读取状态、选择低风险动作、验证和回写，不替代 Goal、Harness、Execution Grant、测试通过、CR 结论或生产发布审批。

| 字段 | 内容 |
| --- | --- |
| Loop ID | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-LOOP-2026-06-11` |
| 关联 Goal | `GSD-GOAL-MVP-VCC-GLOBAL-FUNDS-2026-06-07` |
| Loop 类型 | `Plan Grant Loop / summary_only`；允许低风险本地文档、状态、索引、验证矩阵、交接记录和目标测试覆盖补齐。 |
| 当前状态 | `CLOSED_AS_HISTORY_BY_GSD2_BASELINE_RESET`。 |
| 状态载体 | 本文、`GSD-1-账本账目状态账本.md`、`GSD-1-账本账目Wave1执行计划.md`、`openspec/project.md` 和 `openspec/changes/tdd-baseline-reset/tasks.md`。 |
| 决策输入 | 当前 Git 工作树、已消费 Grant、GSD Wave 队列、Execution Grant 队列、PRD/DSL/系分/TDD/OpenSpec 权威入口、验证结果和用户新增约束。 |
| 允许动作 | 仅允许读取历史证据、消费记录、Not Done 边界和审计线索；新的低风险文档同步、只读 Gap Audit、目标测试覆盖或 Grant 草案必须进入 GSD-2。 |
| 禁止动作 | 未确认新的单一 Execution Grant 前，不改公共契约、DDL/H2 schema、运行时配置、生产配置、外部规则结论、Git add/commit/push、联网、依赖安装或任何不可逆操作；Plan Grant 下若目标测试暴露必须改生产代码的真实缺口，应先收口为 Red 证据并等待新的单一 Execution Grant。 |
| 反馈源 | Harness checker、产品/架构 deliverable checker、外部规则字段 checker、`rg` 一致性扫描、`git status --short`、`git diff --check`、用户确认和后续目标测试结果。 |
| 验证者 | 文档和 Harness 结构由脚本验证；代码、测试、TDD/CAD 和 CR 由 `资深架构师` 接手；产品语义和验收种子由 `产品架构专家` 接手；最终优先级和 Grant 由用户确认。 |
| 预算 / 最大轮次 | 每轮最多 1 个低风险本地任务；连续 2 轮没有新增证据、状态变化或缺口收敛时暂停。 |
| 无进展检测 | 若复扫仍只是重复“无可沿用 Execution Grant”且没有新增事实、候选、目标测试或验证证据，则停止扩写，回到用户确认新的单一 Grant。 |
| 停止条件 | 命中公共契约、DDL/H2、生产代码修改、跨能力域、外部规则专业确认、工作树冲突、验证失败无法解释、工具权限升级、Git 操作或用户中断时停止。 |
| 恢复入口 | [GSD-2-新基线工作流规划.md](GSD-2-新基线工作流规划.md)。后续若选择交易、账户或对账候选，必须在 GSD-2 下重新编号并确认新的单一 Grant。 |
| 失败回写位置 | GSD-2、新 OpenSpec project 指针、Harness tasks 和对应候选准入卡；本文只保留历史证据。 |
| Git 策略 | `summary_only`；本 Loop 不执行 `git add` 或 `git commit`。 |

## 4. 生产可用定义

生产可用不是“接口看起来有了”，而是目标能力在本轮授权范围内能被真实链路证明。

| 维度 | 必须证明 | 不可接受替代 |
| --- | --- | --- |
| 真实执行路径 | face/application/service/impl 通过真实 Spring Bean 和 H2 数据执行。 | 只写 Request/DTO、空实现、内存 Map/List 业务实现或只 mock 内部核心组件。 |
| 资金事实 | route snapshot、posting plan、LedgerEntry、余额投影、交易投影或差错单能互相追溯。 | 只断言交易状态、entry 数量、接口不报错或日志存在。 |
| 幂等和失败 | 重复请求不重复入账；失败不产生半截 route、posting、entry、projection、出款或敏感导出。 | 只测成功路径，失败靠人工解释。 |
| 数据落地 | 涉及持久化时明确 DDL/H2、唯一键、索引、Entity/Mapper 和迁移边界。 | 只在测试 fixture 或临时对象里保留状态。 |
| 审计和解释 | 用户、商户、运营、财务能看到原因、状态、引用、操作者、规则版本和下一步。 | 只给研发内部对象，不给使用者解释视图。 |
| 外部规则 | 涉及卡组织、银行、ACH、SWIFT、FX、客户资金或敏感数据时列规则来源、日期、范围、确认方和状态。 | 用公开文章、口头经验或历史系统行为替代规则确认。 |

禁止事项：

1. 禁止用 `InMemoryXxxService`、`FakeXxxService`、`MockXxxService` 或 Map/List 存储型业务实现承载生产能力。
2. 禁止把支付工具、卡号、PAN、token、VA、外部账户、预算组、Spend Rule、父账户只读汇总写成 LedgerEntry 主体。
3. 禁止恢复独立 `VCC_ACCOUNT`，禁止让 VCC 业务绕过统一钱包、交易、账本、清结算和对账。
4. 禁止把外部 accepted、submitted、processing、message sent、IN_TRANSIT 展示为到账或出款成功。
5. 禁止把 Goal、GSD Wave、Round 0、文档 CR 或 Harness checker 通过写成编码授权、测试通过或生产 Done。

## 5. 依赖优先级和 MVP 能力地图

依赖关系按“先完整被依赖方能力，再推进业务入口能力”执行。

| 顺序 | 能力域 | 生产可用目标 | 为什么先做 |
| --- | --- | --- | --- |
| 1 | P0 账本账目 | 账本、账目、账本交易、分录、余额投影和绑定账本约束可独立证明。 | 所有钱包、交易、VCC、全球账户和清结算都依赖账务事实。 |
| 2 | P0 钱包账户 | 资金账户、信用账户、父子账户、账户层级快照、钱包 application facade 和资金责任解析可用。 | VCC 子账户、预付卡、共享卡、全球账户钱包都依赖账户能力。 |
| 3 | P1 交易内核 | 直接交易、授权交易、余额控制、退款/撤销/拒付、原路径回放和交易投影保持账户主体型 canonical 能力。 | 业务入口只能委派给稳定交易内核，不能绕过账户和账本。 |
| 4 | P0 清结算与对账 | 对账差错闭环、清分、内部清算、结算锁定、出款结果和追偿能形成批次、差错和补事实白名单。 | VCC clearing、全球账户出入金、商户结算都依赖外部证据和差错闭环。 |
| 5 | 支付工具与 Spend Rule 支持 | 工具能力准入、绑定快照、Spend Rule 决策、拒绝原因和只读投影可用。 | 它是 VCC/VA/外部钱包入口的 application facade，不是账务内核。 |
| 6 | P2 VCC 支持 | VCC 预付卡 funding、共享卡授权、clearing、refund、chargeback 和卡账单通过资金/信用子账户接入。 | 只能在账户层级、交易内核、清结算对账满足后推进。 |
| 7 | P2 全球账户支持 | VA 收款、全球账户付款、退汇、FX quote 引用、费用分离和外部非终态边界。 | 依赖账户、交易、对账和外部规则待确认字段。 |
| 8 | 收单 | 保持 design-only，除非用户重新打开实现优先级。 | 当前 MVP 以 VCC 和全球收付款为主，不扩大到 acquiring。 |

### 5.1 生产可用 MVP 交付雷达

本节用于每轮 GSD / CR / 准入复核时判断“下一步做什么才会让生产可用目标更真”。状态只代表当前设计和任务基线，不代表代码 Done；只有对应 Execution Grant 的代码、测试、DDL/H2、审计和验证证据闭合后，才能把某一能力从待交付推进到 Done。

| 能力域 | 当前状态 | 下一生产证据 | 下一 Grant 入口 | 不能算 Done 的情况 |
| --- | --- | --- | --- | --- |
| 账本账目 | `PARTIAL_SERVICE_FLOW_EVIDENCE_NOT_GIT_FROZEN`。001A/001B/002A 已有当前工作树目标测试证据，目标测试文件未被 Git 跟踪；003 已完成既有余额投影强化回归登记；004A 已完成 BudgetGroup 兼容 guard。 | 下一步不再是 004A；若继续账本账目，需选择预算组 control ledger 退出条件、账本生产基线冻结或其他新的单一 Grant。 | 新的账本/钱包/交易/对账单一 Grant；不得沿用 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。 | 只凭当前工作树测试声明账本整体生产 Done，忽略 Git 未冻结、清结算对账依赖、未提交基线或预算控制目标态退出条件。 |
| 钱包账户 / 账户层级 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。已有 `PREPAID_CARD`、`SHARED_CARD`、`CREDIT_CARD` 账户类型和支付工具到资金/信用账户绑定局部基线，但账户 Request、H2 schema 和 wallet-face 仍缺父账户、根账户、层级版本、账目 profile、绑定摘要和账户层级 application facade。 | 资金账户 / 信用账户父子结构、VCC 资金/信用子账户、父账户只读聚合、账目 profile、绑定摘要和层级版本能通过契约或 H2-backed 流程证明。 | `B2-ACCOUNT-HIERARCHY`。 | 只把枚举或支付工具绑定服务当作账户层级 Done，恢复独立 `VCC_ACCOUNT`，把父账户聚合写成入账主体，或缺快照、缺 Red、缺失败无副作用断言。 |
| 钱包资金责任 / application facade | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。`B2-FR-FAO` 只能证明普通资金账户责任解析；`B2-FR-TARGET` 已补一页式确认入口，承接 VCC、信用账户和平台责任目标主体迁移。 | 资金责任唯一决策、`targetSubjectType + targetSubjectId`、账户层级快照引用、拒绝事实、幂等摘要和审计快照能委派到账户主体型内核；工具动作能力准入留到后置支付工具支持队列。 | `B2-FR-TARGET`。 | 让调用方拼多个资源服务、长期只用 `fundingAccountId` 表达所有责任、混用字段策略，或用 mock facade 冒充生产能力。 |
| 交易内核 | `PARTIAL_SERVICE_FLOW_COVERAGE_ADDED_B3_AND_ROUTE_REPLAY_VERIFIED_NOT_DONE`。Round 0 已收敛到账户主体型 canonical replay fail-fast 首切片；2026-06-11 Plan Grant 已补授权后继缺原授权事实 fail-fast 目标测试，直接 Green，说明现有 command service 在路由回放前已阻断；同日用户确认并消费 B3，直接退款原交易引用回放、独立退款事实、缺原交易失败无副作用和累计超额阻断已闭合；随后验证 `DefaultRouteReplayServiceTests` 9 tests，通过纯 route replay 边界证明当前支付工具、外部账户或资金责任变化不会覆盖原快照；随后新增直接退款原交易存在但 route snapshot 缺失的交易全链路目标测试，证明失败发生在回放解析阶段且无新资金事实、账务事实或余额副作用；本轮新增直接退款原交易 route snapshot 固化旧支付工具和旧资金责任后，后续退款仍沿原快照回放的交易 flow 目标测试，`FundsDirectTransactionFlowTests` 51 tests 通过。 | 首切片已补直接退款缺原快照和当前绑定/资金责任变化后沿原快照回放的服务级证据；交易投影解释、余额调账审计、授权/争议/VCC lifecycle 更大组合 replay flow 后续单独拆分。 | `B4-CANONICAL-REPLAY-FAILFAST`，后续再拆 `B6-TRANSACTION-PROJECTION-EXPLAIN`、`B5-BALANCE-ADJUST-AUDIT` 或新的授权/VCC lifecycle replay flow Grant。 | 新增统一 `InstrumentTransactionService`、把核心请求改成支付工具引用，或只测状态不测账务事实；B3 直接退款闭环、单个缺原事实覆盖、route replay 纯服务边界、缺原快照直接退款 flow 和本轮直接退款快照归因 flow 覆盖不能声明 B4 全部 Done；不得借 B3 已消费继续扩展其他公共契约。 |
| 清结算与对账 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`。Round 0 已收敛到对账差错闭环首切片，完整清分、清算、结算、出款和追偿仍未打开。 | 至少一条已过账交易进入对账、发现差异、生成差错、阻断清算或出款、处理后重跑或追加白名单调账事实；批次、规则版本、审批、审计和幂等齐备。 | `B7-RECON-DIFFERENCE-MVP`，后续再拆 `B7-CLEARING-GATE`、`B7-PAYOUT-EXPLAIN`、`B7-OPS-AUDIT`。 | 把结算审批当外部到账成功，把清算写成持牌清算结论，缺差错闭环、重跑幂等、补事实白名单，或一口气打开完整 B7 全量对象。 |
| 支付工具 / Spend Rule | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED`，但排在交易内核和清结算对账之后。 | 工具 RECEIVE/PAY/AUTHORIZE/REFUND/WITHDRAW 能力准入、绑定快照、Spend Rule 决策、拒绝原因、控制活动和只读投影可审计。 | `B2-PI-CAP`、`B4-AUTH-PI`、`B5-SR-CONTROL`、`B6-B8-PI-VIEW`。 | 把支付工具、预算组或 Spend Rule 写成 ledger subject，或让预算控制活动改写账本余额。 |
| VCC MVP | `QUEUED_AFTER_P0_P1`。业务设计已收敛为卡是支付工具、背后绑定资金/信用子账户。 | 预付卡 funding、共享卡授权、clearing、refund、chargeback 和卡账单按子账户、原 route snapshot、外部事件幂等和敏感字段阻断证明。 | 依赖 `B2-ACCOUNT-HIERARCHY` 后，再选 `P2-VCC-PREPAID` 或 `P2-VCC-LIFECYCLE`。 | 直接打开 P2 VCC facade、把卡号/卡 token/父账户当账务主体，或绕过钱包、交易、账本、清结算对账。 |
| 全球账户 MVP | `QUEUED_AFTER_P0_P1`。外部账户、FX 端口和出款前准入有局部基线。 | VA 收款、全球账户付款、退汇、FX quote 引用、费用分离、外部非终态不入账和敏感字段最小化能形成服务级证据。 | `P2-GA-INBOUND`、`P2-GA-OUTBOUND`、`P2-GA-FX-FEE`。 | 把 submitted/accepted/processing 展示成到账或付款成功，保存完整敏感银行账户，或把资金服务做成 FX 执行系统。 |
| 收单 | `DESIGN_ONLY_NOT_CODE_CANDIDATE`。 | 仅做产品、DSL、系分、TDD、外部规则和 PCI 边界复核。 | 无默认实现 Grant。 | 未重新打开优先级就写 capture/dispute 生产代码、测试或 DDL。 |

### 5.2 Goal 完成度审计（2026-06-07）

本节按 active Goal 的显式目标做完成度审计。结论先行：当前 Goal 未完成，状态是 `PARTIAL_LEDGER_EVIDENCE_NOT_PRODUCTION_DONE`。已有证据能证明文档基线、依赖顺序、Grant 队列和当前工作树中的 001A/001B/002A 账本目标测试可运行；但目标测试文件当前未被 Git 跟踪，因此这些证据不能证明 Git 基线已冻结，也不能证明账本账目、钱包、交易内核、清结算对账、VCC 或全球账户已经生产可用。

| Goal 要求 | 当前最强证据 | 证据强度 | 审计结论 | 下一证明动作 |
| --- | --- | --- | --- | --- |
| 做几轮设计评审、优化、完善、可交付准入。 | 本计划、B2/B4/B7/P2 Round 0 准入卡、OpenSpec tasks、Harness checker 和 README 索引已形成同一套 Goal/Wave/Grant 队列。 | `docs/harness-structure`。 | 已具备授权前基线，但不是生产 Done。 | 用户确认一个单一 Execution Grant 后进入 TDD/CAD。 |
| 账目模块生产可用。 | 001A/001B/002A 已有当前工作树 `DefaultLedgerTransactionPostingServiceImplTests` 10 tests 运行证据；002A 已证明绑定账本账目、币种和负余额约束失败无半截事实；003 已回归 `LedgerBalanceProjectionServiceImplTests` 5 tests；004A 已回归 direct/auth/balance-control 并阻断 `BUDGET_GROUP` 作为资金价值交易主体。 | `partial service-flow evidence`，覆盖当前工作树 001A/001B/002A、既有 003 投影回归和 004A 兼容 guard；目标测试文件未被 Git 跟踪。 | 未完成。仍未形成冻结 Git 基线，也未完成预算控制目标态退出、钱包账户、交易内核和清结算对账依赖。 | 选择新的单一 Grant：预算组 control ledger 退出条件、账本基线冻结、钱包账户/账户层级、交易内核或清结算对账。 |
| 钱包模块生产可用。 | 钱包账户、账户层级、资金责任解析和 application facade 已有 Round 0 候选；现有代码仅有枚举、绑定和普通资金账户责任解析局部基线。 | `contract/round0`。 | 未完成。不能把资源型服务、枚举或普通 funding relation 当成钱包生产能力。 | 账本 002A 后确认 `B2-ACCOUNT-HIERARCHY`，再确认 `B2-FR-TARGET`。 |
| 明确清算、结算设计、流程、产品能力。 | B7 Round 0 已把首切片收敛为对账差错闭环，并明确不一次性打开完整清分、清算、结算、出款或追偿。 | `design/round0`。 | 设计入口已收敛，生产能力未完成。 | 确认 `B7-RECON-DIFFERENCE-MVP`，用真实服务/H2 证明差错、阻断、重跑和补事实白名单。 |
| 完善交易层能力，优先完成 VCC 场景支持。 | B4 Round 0 已收敛到账户主体型 canonical replay fail-fast；2026-06-11 Plan Grant 已补 `FundsAuthorizationTransactionFlowTests` 授权后继缺原授权事实失败无副作用覆盖，30 tests 通过；同日 `B3-DIRECT-REFUND-REFERENCE-REPLAY` 已补直接退款原交易引用回放，直接交易 flow 49 tests、交易分组 102 tests、边界分组 152 tests 通过；随后 `DefaultRouteReplayServiceTests` 9 tests 通过，验证 replay resolver 在当前工具、外部账户或资金责任变化时继续使用原快照；随后 `FundsDirectTransactionFlowTests` 50 tests 通过，验证原交易存在但 route snapshot 缺失时直接退款 fail-fast 且无新资金或账务副作用；本轮 `FundsDirectTransactionFlowTests` 51 tests 通过，验证直接退款交易 flow 在原支付快照固化旧支付工具和旧资金责任后仍沿原快照回放；VCC 被排到依赖账本、账户层级、资金责任、交易内核和对账之后。 | `partial service-flow coverage + B3 direct refund replay consumed + route replay snapshot boundary verified + missing snapshot flow verified + current binding replay flow verified`。 | 未完成。当前不能直接打开 VCC facade 或新增统一支付工具交易服务，B4 仍缺交易投影解释、余额调账审计以及授权/争议/VCC lifecycle 更大组合 replay flow 后续切片。 | 继续交易内核时回到 `B4-CANONICAL-REPLAY-FAILFAST` 剩余缺口；否则进入支付工具准入和 VCC 切片前证据不足。 |
| VCC 发卡、VCC 交易处理、全球收付款 MVP。 | PRD、DSL、系分和 TDD 已明确卡是支付工具、背后绑定资金/信用子账户；全球账户有 P2 候选和外部非终态红线。 | `design/contract-candidate`。 | 未完成。当前只是业务目标和候选切片，不是可上线能力。 | 按 `B2-ACCOUNT-HIERARCHY -> B2-FR-TARGET -> B4 -> B7 -> B2-PI-CAP/P2-VCC/P2-GA` 顺序推进。 |
| 交付生产可用代码，不是一堆模拟实现。 | 当前未确认新的 Execution Grant；本计划和 AGENTS 均禁止 mock、内存实现、空 facade 和只断言状态数量。 | `pregrant-authority`。 | 未开始本轮代码交付。未授权前不能写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 | 用户明确确认单一 Grant 后，按真实 Spring Bean、H2/fixture、审计、幂等和失败无副作用补 Red/Green。 |

完成度审计规则：只有对应 Grant 内的 `service-flow-backed` 证据，且同时具备真实 Spring Bean、H2/fixture、账务事实、余额投影、幂等、失败无副作用、审计和验证命令，才允许把能力域从 Not Done 推进到 Done。

评审循环：

| 轮次 | 目标 | 准出条件 |
| --- | --- | --- |
| Round 0 | 对齐 PRD、DSL、系分、TDD、OpenSpec 和代码事实，确认一个最小生产切片。 | 有单一 Task ID、业务问题、写入范围、只读范围、首批 Red、验证命令、停止条件和 Not Done。 |
| Grant 确认 | 用户确认一个 Execution Grant。 | Grant 明确是否允许公共契约、DTO、DDL/H2、状态机、运行时配置和 Git 策略。 |
| TDD/CAD | 先 Red 后 Green，按真实 Spring Bean/H2/服务入口推进。 | 目标测试、相关回归、边界/规约检查和 CR 通过；失败无副作用和资金不变量可解释。 |
| Handoff | 回写状态账本、OpenSpec tasks、README 或下一候选。 | Done / Coverage Added / Blocked / Next Candidate 状态清晰，未授权范围留在 Not Done。 |

## 6. GSD Wave 计划

### Wave 0：基线对齐和证据封存

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD-GOAL-W0-BASELINE` |
| Owner | AI Native 流程编排 + 产品架构专家 + 资深架构师 |
| 写入范围 | 本计划、B4/B7 Round 0 准入卡、TDD README、docs README、OpenSpec project、Harness tasks 索引。 |
| 只读范围 | wind-funds docs/code，fincone、fincone-issuing、nobe，公开文章。 |
| 完成条件 | Goal 卡、依赖顺序、生产可用定义、Grant 队列、验证矩阵和 handoff 写入。 |
| 验证命令 | Harness checker、外部规则完整性 checker、`git diff --check`。 |
| 状态 | `PREGRANT_BASELINE_READY` |

### Wave 1：账本账目生产可用

| 字段 | 内容 |
| --- | --- |
| Task ID | `GSD1-LD-RED-002A` |
| Execution Grant | `GSD1-LEDGER-BOUND-LEDGER`，已确认并消费。 |
| 目标 | 先补 `DefaultLedgerTransactionPostingServiceImpl` 入口处 entry 与绑定 ledger 的账目、币种和负余额约束目标 Red；只读源码锚点显示生产 guard 可能已存在。 |
| 依赖关系 | Wave 0 完成；不依赖钱包、支付工具、VCC 或全球账户。 |
| 写入范围 | 已接续当前未被 Git 跟踪的 `DefaultLedgerTransactionPostingServiceImplTests`，保护既有 001A/001B 覆盖并补 002A；Red 直接 Green，已登记覆盖补齐并停止生产改动。 |
| 禁止事项 | 不处理预算组兼容策略，不写钱包、交易新能力、支付工具、VCC、全球账户、清结算对账。 |
| 完成条件 | 目标测试通过，资金不变量和失败无副作用断言完整；未形成冻结 Git 基线。 |
| 状态 | `DONE_COVERAGE_ADDED_SUMMARY_ONLY` |

### Wave 2：钱包账户和账户层级

| 字段 | 内容 |
| --- | --- |
| Task ID | `B2-ACCOUNT-HIERARCHY-CAD-001` |
| Execution Grant | `B2-ACCOUNT-HIERARCHY`，待用户确认。 |
| 目标 | 交付账户层级 application facade、VCC 关联资金/信用子账户准入、父账户/根账户快照、账目 profile、工具绑定摘要和只读聚合边界。 |
| 依赖关系 | 不豁免 Wave 1。若只做 `contract-only/no-ddl` 准入 Red，可作为 VCC 快速路径的前置确认入口先行准备；若声明生产 Done、`ledger-snapshot-backed`、账本初始化、posting role 或余额投影回归，必须等 Wave 1 账本账目基础闭合并在 Grant 中显式授权。 |
| 默认决策 | `contract-only`、`parent-child-snapshot-required`、`detail-only-first`。 |
| 禁止事项 | 不新增 `VCC_ACCOUNT`，不让父账户聚合、支付工具、预算组或 Spend Rule 入账。 |
| 状态 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` |

### Wave 3：钱包资金责任解析

| 字段 | 内容 |
| --- | --- |
| Task ID | `B2-FR-TARGET-CAD-001` 或等价单切片 |
| Execution Grant | `B2-FR-TARGET`，待用户确认。 |
| 目标 | 资金责任唯一决策、`targetSubjectType + targetSubjectId`、账户层级快照引用、拒绝事实和审计快照可用；支付工具动作能力准入后移到 Wave 6。 |
| 依赖关系 | Wave 2 完成账户层级和子账户快照基础。 |
| 关键取舍 | 涉及 VCC、信用账户或平台责任时，资金责任目标必须支持 `targetSubjectType + targetSubjectId` 或等价主体引用。 |
| 一页式确认入口 | `docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#861-fundingresponsibilitytargetgrantcandidate2026-06-07` |
| 状态 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` |

### Wave 4：交易内核生产可用补强

| 字段 | 内容 |
| --- | --- |
| Task ID | `B4-CANONICAL-REPLAY-FAILFAST-CAD-001` |
| Execution Grant | `B4-CANONICAL-REPLAY-FAILFAST`，未完整消费；2026-06-11 Plan Grant 已补授权后继缺原事实覆盖，并验证 route replay 原快照复用边界。 |
| 目标 | 先证明账户主体型 canonical 内核的原路径回放、缺快照 fail-fast、当前绑定变化不重选路和失败无副作用；后续再拆交易投影解释和余额控制调账审计。 |
| 依赖关系 | Wave 1 账本稳定，Wave 2/3 提供账户和资金责任快照。 |
| 禁止事项 | 不新增统一 `InstrumentTransactionService`，不替换直接交易、授权交易或余额控制的 canonical 入参。 |
| Round 0 入口 | [B4-交易内核生产可用性Round0准入卡.md](B4-交易内核生产可用性Round0准入卡.md)。 |
| 状态 | `PARTIAL_COVERAGE_ADDED_BY_PLAN_GRANT_NOT_DONE` |

### Wave 5：清结算、结算和对账 MVP

| 字段 | 内容 |
| --- | --- |
| Task ID | `B7-RECON-DIFFERENCE-MVP-CAD-001` |
| Execution Grant | `B7-RECON-DIFFERENCE-MVP`，待用户确认。 |
| 目标 | 先交付对账任务、匹配结果、差错单、阻断、重跑和补事实白名单准入；再交付清分、内部清算、结算锁定、出款结果和追偿。 |
| 依赖关系 | Wave 1 账本已具备可运行覆盖；若落到完整清分/出款，还依赖 Wave 4 交易补事实和 route replay 边界。首切片只做对账差错闭环准入，不一次性打开完整 B7。 |
| 最小场景 | 一笔已过账交易进入对账，发现差异，生成差错单，阻断清算或出款，处理后可重新对账或追加调账事实。 |
| 默认决策 | `implementationDecision=service-flow-backed`、`schemaDecision=minimal-reconciliation-ddl-h2-required`、`adjustmentWhitelist=closed-first`。若只确认 `contract-only`，只能交付契约、DTO 或目标 Red，不得声明 B7 生产可用。 |
| 禁止事项 | 不把结算单审批当外部到账成功，不把清算写成持牌清算结论。 |
| Round 0 入口 | [B7-清结算与对账Round0准入卡.md](B7-清结算与对账Round0准入卡.md)。 |
| 状态 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` |

### Wave 6：支付工具与 Spend Rule 支持

| 字段 | 内容 |
| --- | --- |
| Task ID | `B2-PI-CAP-CAD-001`、`B4-AUTH-PI-CAD-001`、`B5-SR-CONTROL-CAD-001` 或 `B6-B8-PI-VIEW-CAD-001` |
| Execution Grant | 每次只确认一个：`B2-PI-CAP`、`B4-AUTH-PI`、`B5-SR-CONTROL` 或 `B6-B8-PI-VIEW`。 |
| 目标 | 工具动作能力准入、授权支付工具 application facade、Spend Rule 控制活动和支付工具维度只读解释。 |
| 依赖关系 | Wave 2 账户层级、Wave 3 资金责任、Wave 4 交易内核和 Wave 5 清结算对账差错准入满足后再推进。 |
| 禁止事项 | 不把支付工具、预算组或 Spend Rule 写成账本主体，不替换交易 canonical 请求。 |
| 状态 | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` |

### Wave 7：VCC MVP 支持

| 字段 | 内容 |
| --- | --- |
| Task ID | `P2-VCC-MVP` |
| Execution Grant | 待在 `P2-VCC-PREPAID` 和 `P2-VCC-LIFECYCLE` 中选一个。 |
| 目标 | 先支持 VCC 预付资金或共享卡授权最小闭环，再支持 clearing、refund、chargeback 和卡账单投影。 |
| 依赖关系 | Wave 1 至 Wave 6 对应能力满足后才能声明 VCC 生产可用；若业务要求 VCC 优先，只允许先切到 Wave 2 `B2-ACCOUNT-HIERARCHY` 的 `contract-only/no-ddl` 准入 Red，用于证明卡绑定子账户、父账户快照和账目 profile，不得直接进入 P2 VCC 资金流。 |
| 关键证据 | 卡是 PaymentInstrument，账务主体是资金/信用子账户；逆向按原 route snapshot；敏感字段阻断。 |
| 状态 | `QUEUED_AFTER_P0_P1` |

### Wave 8：全球账户 MVP 支持

| 字段 | 内容 |
| --- | --- |
| Task ID | `P2-GLOBAL-ACCOUNT-MVP` |
| Execution Grant | 待在 inbound、outbound、FX/fee 中选一个。 |
| 目标 | VA 收款、全球账户付款、退汇、FX quote 引用和费用拆分按外部非终态边界接入。 |
| 依赖关系 | Wave 1 账本、Wave 2 钱包账户、Wave 4 交易、Wave 5 对账；若使用外部账户或 VA 工具能力，还依赖 Wave 6 支付工具准入。 |
| 状态 | `QUEUED_AFTER_P0_P1` |

## 7. Execution Grant 队列

GSD-2 迁移说明：下表从 2026-06-12 起不再是当前活跃队列，只是 backlog reference 和历史准入材料。新的活跃候选队列以 [GSD-2-新基线工作流规划.md#7-下一候选优先级](GSD-2-新基线工作流规划.md#7-下一候选优先级) 为准。

| 优先级 | Grant 候选 | 当前状态 | 依赖 | 备注 |
| --- | --- | --- | --- | --- |
| 已消费 | `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`（`GSD1-LD-RED-004A`） | `CONSUMED_BY_GSD1_LD_RED_004A` | 001A/001B/002A 已有当前工作树证据，003 已有既有投影回归证据 | 004A 已完成 BudgetGroup 兼容 guard；后续不得沿用该 Grant。 |
| 已消费 | `GSD1-LEDGER-PROJECTION-REGRESSION` | `CONSUMED_BY_GSD1_LD_RED_003` | 既有投影回归 5 tests 通过，未修改生产代码或测试代码 | 不再作为可确认入口；后续不得沿用该 Grant 继续写 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git 提交。 |
| 已消费 | `GSD1-LEDGER-BOUND-LEDGER` | `CONSUMED_BY_GSD1_LD_RED_002A` | 当前工作树目标测试 10 tests 通过，未修改生产代码 | 不再作为可确认入口；后续不得沿用该 Grant 继续写 Java、测试、DDL/H2 schema、公共契约、运行时配置或 Git 提交。 |
| 2 | `B2-ACCOUNT-HIERARCHY` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` | Wave 1；VCC 快速路径只允许 `contract-only/no-ddl` 准入 Red 先行准备 | VCC 关联子账户、共享卡、预付卡的前置；不代表跳过账本账目依赖。 |
| 3 | `B2-FR-TARGET` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` | Wave 2 | 涉及 VCC、信用账户或平台责任时必须用目标主体引用；`B2-FR-FAO` 只保留给普通资金账户低风险闭环。 |
| 4 | `B4-CANONICAL-REPLAY-FAILFAST` | `PARTIAL_COVERAGE_ADDED_BY_PLAN_GRANT_NOT_DONE` | Wave 1/2/3 | 已补授权后继缺原授权事实 fail-fast 覆盖；已验证 route replay 纯服务边界下当前支付工具、外部账户和资金责任变化不覆盖原快照；已补直接退款原交易存在但 route snapshot 缺失的全链路失败无副作用覆盖；已补直接退款交易 flow 下当前绑定和资金责任变化后沿原快照回放覆盖；投影解释、调账审计和授权/争议/VCC lifecycle 更大组合 replay flow 仍未完成。 |
| 5 | `B7-RECON-DIFFERENCE-MVP` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` | Wave 1/4 | 对账差错闭环优先，清分、清算、结算、出款和追偿随后。 |
| 6 | `B2-PI-CAP` / `B4-AUTH-PI` / `B5-SR-CONTROL` / `B6-B8-PI-VIEW` | `READY_TO_CONFIRM_NOT_CODE_AUTHORIZED` | Wave 2/3/4/5 | 支付工具和 Spend Rule 后置支持，不替换交易内核。 |
| 7 | `P2-VCC-PREPAID` / `P2-VCC-LIFECYCLE` | `QUEUED_AFTER_P0_P1` | Wave 2/3/4/5/6 | 不先于账户层级、交易内核、对账差错和工具准入。 |
| 8 | `P2-GA-INBOUND` / `P2-GA-OUTBOUND` / `P2-GA-FX-FEE` | `QUEUED_AFTER_P0_P1` | Wave 2/4/5/6 | 外部非终态和规则核验是强门禁。 |
| 9 | `P2-ACQ-*` | `DESIGN_ONLY_NOT_CODE_CANDIDATE` | 重新打开优先级 | 当前不进入实现。 |

若用户要求“先做 VCC”，本计划仍建议先确认 `B2-ACCOUNT-HIERARCHY`，因为每张卡绑定资金/信用子账户、父账户约束和账目 profile 是 VCC 预付卡、共享卡和卡账单的前置依赖。这个切换只表示业务队列优先对齐账户层级准入，不表示跳过账本账目、资金责任、交易内核、对账差错或支付工具准入；除 `contract-only/no-ddl` 目标 Red 外，任何生产 Done、H2-backed 流程、账本初始化或 VCC 资金流仍必须满足对应前置 Wave。若用户要求“先做全球账户”，本计划建议先确认钱包账户、交易内核和对账差错闭环，不直接写外部付款 facade。

### 7.1 默认下一确认入口

当前默认下一入口不再是 `GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`：`GSD1-LD-RED-003` 投影强化回归已消费并登记为既有覆盖通过，`GSD1-LD-RED-004A` BudgetGroup 兼容 guard 已消费并登记为通过。历史 002A 确认与消费记录保留在 [GSD-1-账本账目ExecutionGrant确认卡.md#12-consumedgrantonepageconfirmation2026-06-07](GSD-1-账本账目ExecutionGrant确认卡.md#12-consumedgrantonepageconfirmation2026-06-07)，003 消费记录保留在 [GSD-1-账本账目ExecutionGrant确认卡.md#13-consumedprojectionregressiongrant2026-06-07](GSD-1-账本账目ExecutionGrant确认卡.md#13-consumedprojectionregressiongrant2026-06-07)，004A 消费记录保留在 [GSD-1-账本账目ExecutionGrant确认卡.md#16-consumedbudgetgroupcompatguardgrant2026-06-11](GSD-1-账本账目ExecutionGrant确认卡.md#16-consumedbudgetgroupcompatguardgrant2026-06-11)。后续若继续 Agent Loop，优先从 B4 剩余原路径回放、B2 账户层级 contract-only/no-ddl、B7 对账差错或新的账本控制账本退出条件中选一个低风险切片。

如果用户明确要求先推进 VCC、共享卡、预付卡或钱包账户层级，则切换到 [B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#802-accounthierarchyonepageconfirmation2026-06-07](B2B4-支付工具与SpendRule生产可用性Round0准入卡.md#802-accounthierarchyonepageconfirmation2026-06-07)。该入口把 `Execution Grant：B2-ACCOUNT-HIERARCHY` 收束为 `contract-only`、`parent-child-snapshot-required`、`detail-only-first` 和 `no-ddl` 的默认准入包；只有确认后才允许进入账户层级 Red。

如果用户明确要求先推进交易内核或清结算对账，则分别切换到 [B4-交易内核生产可用性Round0准入卡.md#151-canonicalreplayonepageconfirmation2026-06-07](B4-交易内核生产可用性Round0准入卡.md#151-canonicalreplayonepageconfirmation2026-06-07) 或 [B7-清结算与对账Round0准入卡.md#151-reconciliationdifferenceonepageconfirmation2026-06-07](B7-清结算与对账Round0准入卡.md#151-reconciliationdifferenceonepageconfirmation2026-06-07)。B4 默认只证明账户主体型原路径回放 fail-fast；B7 默认需要服务级 H2 闭环和最小 DDL/H2 范围，`contract-only` 不能声明生产可用。

### 7.1.1 Execution Grant 级别选择矩阵

确认任一 Grant 时必须先选定交付级别。级别决定可写范围、验证命令和可声明结论；低级别通过不得上升声明为生产可用。

| 级别 | 适用场景 | 可声明 | 不可声明 |
| --- | --- | --- | --- |
| `contract-only` | 字段、DTO、枚举、DSL case、目标 Red 或 application facade 入口命名还未稳定。 | 契约语义可评审，目标 Red 可进入下一轮。 | 真实资金流、账本事实、服务级流程、H2-backed 行为或生产可用。 |
| `ddl-backed` | 需要先证明最小表结构、H2 schema、Entity、Mapper、唯一键、索引或迁移边界。 | 数据承载结构可验证，后续可以接服务流。 | 业务流程已闭环、差错阻断已生效、资金事实已安全落账。 |
| `service-flow-backed` | 要声明生产可用候选，或涉及账本、钱包、交易、对账、补事实、VCC funding、全球账户入出金等真实业务流。 | Grant 覆盖范围内可作为生产交付证据之一。 | 未覆盖的外部规则、容量、并发、权限、审计、发布回滚或 P2 全量场景自动完成。 |
| `projection-store-backed` | 只读交易投影、支付工具流水、预算控制视图或治理重放需要持久化读模型。 | 查询视图和重投影范围内可验证。 | 投影成为资金事实、账本事实或余额事实。 |
| `design-only` | 收单、外部规则、监管/合规、PCI、通道协议或尚未确认优先级的 P2 能力。 | 设计差距、待确认项和后续 Grant 输入。 | 写生产代码、测试代码、DDL/H2 schema 或声明实现 Done。 |

默认口径：账本账目、交易内核和 B7 对账差错若要声明生产可用，必须到 `service-flow-backed`；VCC 或全球账户若只先证明账户层级、绑定快照、字段和业务语义，使用 `contract-only/no-ddl`，不得声明资金流 Done；收单默认保持 `design-only`。

### 7.1.2 账本账目到钱包账户交接门禁

本节定义被依赖方能力的交接条件。钱包账户、账户层级、VCC 子账户和全球账户钱包可以先做契约准入，但不得把账本覆盖补齐、枚举补齐或绑定字段补齐误写成生产可用账户能力。

| 交接目标 | 允许先行 | 必须具备的最小证据 | 不可声明 |
| --- | --- | --- | --- |
| `GSD1-LEDGER-BOUND-LEDGER` 作为钱包账户生产前置 | 已完成覆盖补齐；不再作为默认下一账本入口。 | 当前工作树 `DefaultLedgerTransactionPostingServiceImplTests` 已证明 entry 与绑定 ledger 的 subject、账目 code/category、currency 和 `ALLOW_NEGATIVE` 不匹配时，在 ledger transaction、posting plan、ledger entry 和余额投影前失败；重复 post 返回既有事实且不重复投影；目标测试 10 tests 通过。 | 只凭当前工作树测试声明钱包/账户可生产，或忽略目标测试文件未被 Git 跟踪、`BUDGET_GROUP` 兼容策略和后续余额投影强化。 |
| `B2-ACCOUNT-HIERARCHY contract-only/no-ddl` | 可以作为 VCC 快速路径准备，但不表示账本生产 Done。 | 只允许证明账户层级字段、父账户/根账户快照、账目 profile、卡绑定摘要、application facade 命名和目标 Red 可评审。 | 账户开户真实落账、账本初始化、余额可用、父子账户汇总可生产、VCC funding 或共享卡授权 Done。 |
| `B2-ACCOUNT-HIERARCHY ledger-snapshot-backed/service-flow-backed` | 不默认先行。 | 依赖账本当前工作树 001A/001B/002A 证据和新的账户层级 Grant；同时证明账户 profile 到 ledger 初始化、父/根账户快照、子账户 posting role、父账户只读聚合不双算、失败无半截账户/账务事实、幂等摘要和审计快照。 | 跳过账本前置、把父账户聚合当入账主体、把卡号/支付工具/预算组/Spend Rule 当 ledger subject。 |
| VCC prepaid/shared card 后续切片 | 只能消费已确认的账户层级和资金责任快照。 | 预付卡 funding、共享卡授权、clearing、refund、chargeback 和卡账单必须回到子账户、route snapshot、交易内核和对账差错闭环；外部规则核验另行确认。 | 直接进入 P2 VCC facade、在 P2 包里平行实现账本、钱包、清结算或对账对象。 |

若授权后的目标 Red 直接 Green，只登记为覆盖补齐和生产证据增强，不强行修改生产代码；若 Red 暴露真实缺口，只允许在该 Grant 写入范围内最小修复。本文档交接门禁不构成新的 Java、测试、DDL/H2 schema、公共契约或运行时配置授权。

### 7.2 授权前收口判断

当前 GSD + Goal 基线已经迁移为历史证据。2026-06-12 起，旧未完成计划已从活跃队列移除；后续推进的价值应来自 GSD-2 下的 Gap Audit、重新选择单一 `Execution Grant`、执行目标 Red/Green、运行真实验证和回写交付证据。

| 判断项 | 当前结论 | 下一动作 |
| --- | --- | --- |
| 默认依赖顺序 | `GSD1-LD-RED-002A`、`GSD1-LD-RED-003` 和 `GSD1-LD-RED-004A` 均已闭合；当前无可沿用编码 Grant。 | 在 GSD-2 中重新选择新的单一 Grant；若业务强制 VCC 优先，优先评估 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`。 |
| 授权前文档饱和度 | Goal 卡、交付雷达、Wave 队列、Grant 队列、默认入口、切换规则和验证矩阵已齐备，现已作为历史证据冻结。 | 只在发现事实错误、索引漂移或用户新增约束时修正；新的计划在 GSD-2 维护。 |
| VCC 快速路径 | 不直接打开 P2 VCC facade；先进入账户层级。该快速路径只允许 `contract-only/no-ddl` 准入 Red 先行准备，不豁免账本账目、资金责任、交易内核、对账差错和工具准入依赖。 | 明确要求 VCC 优先时，切换到 `B2-ACCOUNT-HIERARCHY`；若要生产 Done 或 H2-backed 资金流，先补对应前置 Wave。 |
| 编码门禁 | Plan Grant 可补低风险目标测试覆盖；公共契约、DDL/H2、生产代码、运行时配置和 Git 操作仍不是自动授权。 | 目标测试直接 Green 时登记覆盖补齐；若 Red 暴露必须改生产代码的真实缺口，先停在 Red 证据并重新确认单一 Execution Grant。 |
| 停止扩写条件 | 001A/001B/002A 已具备消费记录、验证证据和交接记录。 | 不再为 001B 或 002A 重复新增候选卡、扫描表或测试设计表；后续只维护新的单一 Grant。 |

确认规则：

| 用户输入 | 默认动作 |
| --- | --- |
| 再次提及或确认 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER` | 提醒该 Grant 已消费，不能复用；必须重新选择 `GSD1-LD-RED-003`、`GSD1-LD-RED-004` 或切到其他单一 Grant。 |
| 只说继续推进、继续完善、自动模式推进 | 进入 GSD-2，只做新基线 Gap Audit、状态、索引、验证矩阵和下一候选准备。 |
| 明确要求 `Agent Loop Engineering` / `GSD + Goal 按任务计划推进` | 进入 GSD-2 的受控 Plan Grant Loop；先选择低风险文档同步或只读 Gap Audit。目标测试、生产代码、公共契约、DDL/H2、运行时配置或 Git 仍需新的 Grant 或显式授权。 |
| 要求先做 VCC | 不直接进入 P2 VCC facade；优先切到 `B2-ACCOUNT-HIERARCHY` 的账户层级 Grant，且默认仅限 `contract-only/no-ddl` 准入 Red。 |
| 要求先做全球账户 | 不直接进入外部付款 facade；先补钱包账户、交易内核和对账差错 Round 0 或对应 Grant。 |
| 要求先做交易内核 | 切到 `B4-CANONICAL-REPLAY-FAILFAST`，只处理原路径回放 fail-fast，不改 canonical 入参。 |
| 要求先做清结算对账 | 切到 `B7-RECON-DIFFERENCE-MVP`，先处理对账差错闭环，不打开完整清分、清算、结算、出款和追偿。 |

## 8. 验证矩阵

| 阶段 | 验证命令 | 通过口径 |
| --- | --- | --- |
| 本轮文档基线 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | GSD Wave 字段完整，包含 Task ID、Owner、写入范围、只读范围、依赖顺序、验证命令、停止条件和 handoff。 |
| 本轮 Grant 队列 | `python3 /Users/wuxp/.codex/skills/senior-software-architect/scripts/check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | 候选结构能表达 Execution Grant、Git 策略、撤销方式、验证和停止条件。 |
| 外部参考完整性 | `python3 /Users/wuxp/.codex/skills/product-architecture-expert/scripts/check_external_rules.py --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | 外部规则或参考项的来源、版本/日期、范围、法域、核验日期、确认方和确认状态字段完整。 |
| 空白与补丁 | `git diff --check` | 无空白错误。 |
| 后续代码切片 | `just mvn-version`、`just compile`、目标 `just test-one`、相关分组测试、必要时 `just test-boundary` 和 `just pmd` | 代码、测试、模块边界和规约通过；失败要区分环境、私服、依赖或代码问题。 |

### 8.1 preGrantAdmissionEvidence2026-06-07

本节记录 Goal 继续推进后的跨候选准入复核。本轮只运行文档结构、产品结构、架构结构和外部规则字段检查；未写 Java、测试、DDL/H2 schema、公共契约或运行时配置，也未把任何候选提升为已授权编码状态。

| 检查对象 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| Goal 总计划 | `check_harness_plan.py --kind gsd-wave --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | PASS | GSD Wave 字段完整。 |
| Goal 总计划 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | PASS | Grant 队列具备 CAD 候选结构；该证据随后已被 004A 消费事实更新。 |
| Goal 总计划 | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | PASS | 产品目标、能力、对象、流程、风险和验收结构完整。 |
| Goal 总计划 | `check_external_rules.py --file docs/TDD设计/GSD-Goal-生产可用MVP推进计划.md` | PASS | 外部参考字段完整；真实性和适用性仍待专业确认。 |
| 账本 002A | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/GSD-1-账本账目ExecutionGrant确认卡.md` | PASS | 当时 002A 具备可确认候选结构；该 Grant 现已消费，后续只作为历史执行证据。 |
| 钱包账户 / 支付工具 / Spend Rule | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md` | PASS | `B2-ACCOUNT-HIERARCHY`、`B2-FR-TARGET` 和支付工具支持候选可恢复。 |
| 钱包账户 / 支付工具 / Spend Rule | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md` | PASS | 产品能力、资金语义、风险和验收结构完整。 |
| 钱包账户 / 支付工具 / Spend Rule | `check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md` | PASS | 架构边界、接口数据、一致性、安全和验证结构完整。 |
| 钱包账户 / 支付工具 / Spend Rule | `check_external_rules.py --file docs/TDD设计/B2B4-支付工具与SpendRule生产可用性Round0准入卡.md` | PASS | 外部规则字段完整；不代表外部规则已可上线。 |
| 交易内核 B4 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` | PASS | `B4-CANONICAL-REPLAY-FAILFAST` 可作为后续候选。 |
| 交易内核 B4 | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` | PASS | 交易内核产品边界和验收结构完整。 |
| 交易内核 B4 | `check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/B4-交易内核生产可用性Round0准入卡.md` | PASS | 交易内核架构准入结构完整。 |
| 清结算与对账 B7 | `check_harness_plan.py --kind cad-candidate --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` | PASS | `B7-RECON-DIFFERENCE-MVP` 可作为后续候选。 |
| 清结算与对账 B7 | `check_product_deliverable.py --kind product-architecture --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` | PASS | 清分、清算、结算、对账和差错闭环产品结构完整。 |
| 清结算与对账 B7 | `check_architecture_deliverable.py --kind architecture-plan --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` | PASS | B7 架构准入结构完整。 |
| 清结算与对账 B7 | `check_external_rules.py --file docs/TDD设计/B7-清结算与对账Round0准入卡.md` | PASS | 外部规则字段完整；不替代法务、合规、财务或通道确认。 |

结论：当前跨候选准入材料已经具备“确认下一单一 Grant 或切换新 Grant”的结构条件。`Execution Grant：GSD1-LEDGER-BOUND-LEDGER` 已被 002A 消费，`Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 已被 003 消费，`Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD` 已被 004A 消费；当前无可沿用编码 Grant。若业务强制先做 VCC，则切到 `Execution Grant：B2-ACCOUNT-HIERARCHY` 的 `contract-only/no-ddl` 准入 Red。未确认新的单一 Grant 前，仍不得写生产代码、测试代码、DDL/H2 schema、公共契约或运行时配置。

### 8.2 authorityEntryGateEvidence2026-06-07

本节记录 PRD、系分、DSL、TDD 权威入口的授权前结构门禁复核。它回答“上游设计入口是否足以支撑下一 Grant 确认”，不回答“代码是否已生产可用”；本轮未写 Java、测试、DDL/H2 schema、公共契约或运行时配置。

| 权威入口 | 命令 | 结果 | 结论 |
| --- | --- | --- | --- |
| PRD 总览 | `check_product_deliverable.py --kind prd --file docs/产品设计/01-PRD总览.md` | PASS | PRD 入口具备目标范围、角色主体、对象状态、流程规则、数据审计、风险确认和验收结构。 |
| 交易路由钱包账目产品设计 | `check_product_deliverable.py --kind product-architecture --file docs/产品设计/02-交易路由钱包账目与投影.md` | PASS | 账户、钱包、交易、路由、账目、投影和资金语义可支撑 P0/P1 Grant 裁剪。 |
| 清结算与对账产品设计 | `check_product_deliverable.py --kind product-architecture --file docs/产品设计/03-清结算与对账.md` | PASS | 清分、清算、结算、对账和差错闭环具备产品结构入口。 |
| VCC 发卡业务资金底座 PRD | `check_product_deliverable.py --kind product-architecture --file docs/产品设计/06-VCC发卡业务资金底座PRD.md` | PASS | VCC 场景可作为后置业务目标输入，但不绕过账本、钱包、交易和清结算依赖。 |
| 交易路由钱包账目系分 | `check_architecture_deliverable.py --kind system-design --file docs/系分设计/02-交易路由钱包账目与投影系分设计.md` | PASS | 模块边界、接口数据、一致性、安全、验证和发布回滚结构完整。 |
| 清结算与对账系分 | `check_architecture_deliverable.py --kind system-design --file docs/系分设计/03-清结算与对账系分设计.md` | PASS | B7 对账差错闭环和后续清结算对象具备系分承载入口。 |
| DSL 承载层 | `check_architecture_deliverable.py --kind architecture-plan --file docs/DSL设计/支付资金底座DSL承载层设计.md` | PASS | 资金指令、route snapshot、账务计划、LedgerEntry、投影和 JSON 契约结构可支撑 Grant 断言。 |
| TDD 设计 | `check_harness_plan.py --kind lightweight --file docs/TDD设计/支付资金底座测试驱动设计.md` | PASS | TDD 分层、红线、验证命令和测试清单入口可支撑下一 Red 选择。 |

结论：PRD、系分、DSL、TDD 权威入口已经具备支撑下一单一 Grant 确认的结构条件。默认执行顺序不变：账本账目已完成 `GSD1-LD-RED-003` 投影强化回归登记和 `GSD1-LD-RED-004A` BudgetGroup 兼容 guard；下一步不再沿用 004A，应重新选择预算组 control ledger 退出条件、钱包账户/账户层级、交易内核或清结算对账中的一个新切片。若业务强制先做 VCC，只能先确认 `Execution Grant：B2-ACCOUNT-HIERARCHY` 的 `contract-only/no-ddl` 账户层级准入 Red。权威入口门禁通过不等于 Execution Grant、测试通过或生产 Done。

### 8.3 codingAdmissionDecision2026-06-07

本节把 GSD + Goal 的授权前材料收敛为编码准入裁决，防止在缺少单一 `Execution Grant` 时继续重复扩写同一批 Round 0 文档。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| Goal 状态 | 历史当时曾开启运行时 Goal；2026-06-12 已迁移到 GSD-2，旧 Goal 只作为历史证据。 | 只能作为历史准入证据，不能继续推进或当授权。 |
| 上游权威入口 | 第 8.2 节确认 PRD、系分、DSL、TDD 结构门禁通过。 | 上游入口足以支撑下一 Grant 确认。 |
| 跨候选准入 | 第 8.1 节确认 Goal 总计划、账本 002A、钱包账户/支付工具/Spend Rule、B4 和 B7 候选结构门禁通过。 | 候选材料足以选择一个单一 Grant。 |
| 默认下一编码入口 | 无可沿用 Grant；需重新选择一个新的单一 Grant，或按业务优先级切 `B2-ACCOUNT-HIERARCHY`。 | `NEEDS_NEW_SINGLE_GRANT`。 |
| VCC 快速路径 | 仅当业务强制先做 VCC，才切到 `B2-ACCOUNT-HIERARCHY` 的 `contract-only/no-ddl` 账户层级准入 Red。 | 不允许直接进入 P2 VCC facade 或资金流 Done。 |
| 当前工作树风险 | `DefaultLedgerTransactionPostingServiceImplTests.java` 当前存在但未被 Git 跟踪，001A/001B/002A 只能作为当前工作树证据。004A 的 transaction converter 和 transaction flow 改动也仍在当前工作树中。 | 授权后首步必须先保护既有覆盖，并在 Grant 中列明允许纳入和必须排除的 dirty 变更。 |
| 编码授权状态 | 用户尚未明确确认新的单一 `Execution Grant`。 | 当前不得写 Java、测试、DDL/H2 schema、公共契约或运行时配置。 |
| 继续文档价值 | 同一批候选已具备结构门禁和权威入口门禁。 | 后续只修正事实错误、索引漂移或新增用户约束；不再重复扩写同一准入包。 |

编码准入结论：002A 已闭合为覆盖补齐，003 已闭合为既有投影回归登记，004A 已闭合为 BudgetGroup 兼容 guard，当前达到“必须确认新的单一 Grant 或切换新 Grant”的门槛，但尚未达到“可以继续编码”的门槛。默认不再推荐 `Execution Grant：GSD1-LEDGER-BOUND-LEDGER`、`Execution Grant：GSD1-LEDGER-PROJECTION-REGRESSION` 或 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`；后续若走账本，应重新选择预算组 control ledger 退出条件或账本基线冻结等新切片；若业务强制先做 VCC，只能切到 `B2-ACCOUNT-HIERARCHY` 的 `contract-only/no-ddl` 准入 Red。

### 8.4 goalResumeAdmissionEvidence2026-06-11

本节记录 004A 消费前的运行时 Goal 续跑准入刷新，作为历史状态证据保留。该阶段目标仍是生产可用 MVP，但当时续跑消息没有提供新的单一 Execution Grant，因此只更新 004A 授权前证据和交接口径，不进入 Red/Green 编码闭环；后续 Agent Loop Plan Grant 执行证据见第 8.5 节。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| Goal 状态 | 运行时 Goal 继续保持“按依赖优先交付被依赖方能力，再选择单一最小编码切片完成 TDD、实现、验证和交付收口”。 | 可继续推进准入，但 Goal 仍不等于 Execution Grant。 |
| 当前工作树 | `git status --short` 显示多份既有文档修改、GSD 未跟踪文档和未跟踪 `DefaultLedgerTransactionPostingServiceImplTests.java`。 | 授权后必须显式列入允许读取/写入范围；未列入内容不能作为冻结基线或 Done 证据。 |
| 下一编码入口 | 004A 已消费；当前无可沿用编码 Grant。 | `NEEDS_NEW_SINGLE_GRANT`。 |
| 004A 源码锚点 | `DefaultLedgerTransactionPostingServiceImpl` 仍保留 `BUDGET_GROUP` 入账兼容白名单；`BalanceControlFundsInstructionRouteResolver` 仍保留预算额度控制路径。 | 004A 必须同时保护兼容控制路径和阻断资金价值主体扩大，不能简单全删。 |
| 既有测试资产 | `PaymentInstrumentServiceImplTests` 保护预算组不是真实资金主体绑定；`ControlAccountLedgerInitializationTests` 保护预算组控制账本初始化不生成账本交易事实。 | 首批 Red 应围绕交易/路由价值主体阻断补缺，不重复扩写资源服务用例。 |
| 当时验证边界 | 004A 授权前阶段未运行 Maven 测试，因为当时未获新 Grant，不写 Java/测试/schema。 | 该记录只解释 004A 前置续跑，不覆盖第 8.5 节的 Plan Grant 目标测试验证。 |

续跑结论：004A 授权前证据随后已被用户确认并消费；当前不得沿用 `Execution Grant：GSD1-LEDGER-BUDGET-GROUP-COMPAT-GUARD`。下一步只允许修正设计事实、索引漂移或准入证据，或重新确认新的单一 Grant 后进入目标 Red/Green。

### 8.5 agentLoopExecutionEvidence2026-06-11

本节记录用户明确要求进入 Agent Loop Engineering 后的首轮 Plan Grant 执行证据。本轮不是完整 `B4-CANONICAL-REPLAY-FAILFAST` 消费，只是低风险目标测试覆盖补齐；若后续 Red 暴露需要改生产代码的缺口，仍必须重新确认单一 Execution Grant。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| 触发输入 | 用户要求“重新加载所有 skills”，并明确质疑不能一直确认任务计划，要求进入 Agent Loop Engineering、使用 GSD + Goal 按任务计划推进。 | 满足 Plan Grant 触发条件。 |
| 任务选择 | 对比候选后选择 `B4-CANONICAL-REPLAY-FAILFAST` 的 `R0-TRX-REPLAY-001` 子场景；`B2-ACCOUNT-HIERARCHY` 涉及账户公共契约和潜在 schema，风险更高。 | 优先低风险、无公共契约、无 DDL 的交易内核覆盖补齐。 |
| 写入内容 | 在 `FundsAuthorizationTransactionFlowTests` 增加 `testAuthorizationSuccessorsMissingOriginalFactShouldRejectAndLeaveNoSideEffects`。 | 只写目标测试，不写生产代码、公共契约、DDL/H2 schema、运行时配置或 Git。 |
| 行为结论 | 授权撤销、授权完成和已完成授权退款在缺原授权交易事实时均抛出“授权交易不存在”，且用户 `AVAILABLE`、`AUTHORIZATION`、平台 `CASH`、平台 `SETTLEMENT` 和 ledger facts 均不变化。 | 现有 command service 在路由回放和账务计划前已 fail-fast；目标 Red 直接 Green，登记为覆盖补齐。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsAuthorizationTransactionFlowTests tests` | PASS，30 tests / 0 failures / 0 errors。 |
| Not Done | 当时未覆盖缺原 route snapshot、当前绑定或资金责任变化后不得重选路、交易投影解释、余额调账审计、VCC facade、支付工具交易入口或清结算对账；其中纯 route replay 原快照复用边界已由第 8.8 节补充验证，原交易存在但 route snapshot 缺失的直接退款交易 flow 已由第 8.9 节补充验证。 | B4 仍为 `PARTIAL_COVERAGE_ADDED_BY_PLAN_GRANT_NOT_DONE`。 |

### 8.6 agentLoopStopEvidence2026-06-11-direct-refund-contract-gap

本节记录同一 Agent Loop 的历史停止证据。该轮没有写 Java 或测试；原因是低风险切片复核触发了公共契约边界，按 Plan Grant 停止条件收口。该缺口随后已由第 8.7 节的 `Execution Grant：B3-DIRECT-REFUND-REFERENCE-REPLAY` 消费，不再作为当前阻塞项。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| 任务选择 | 继续从 `B4-CANONICAL-REPLAY-FAILFAST` 的剩余缺口中筛选低风险切片，优先检查直接退款是否可补“缺原交易事实 / 原路径 fail-fast”目标测试。 | 与 VCC、全球账户退汇和清结算补事实强相关，属于交易内核生产可用前置。 |
| 代码事实 | `FundsTransactionRefundRequest` 当前只有退款到账账户、退款出资账户、退款出资账目、金额、业务流水和可选渠道流水；没有原支付交易流水、原 route snapshot、原 ledger transaction 或 replay policy 字段。`FundsDirectTransactionInstructionConverter#convertToRefundInstruction` 在无 `channelTransactionSn` 时不构造 reference，有 `channelTransactionSn` 时只构造 `EXTERNAL_TRANSACTION` reference，不等同内部原支付交易。 | 直接退款当前更像按退款出资方余额做一笔逆向资金转移，不足以证明严格原交易 / 原路径 replay。 |
| 停止原因 | 若新增 `FundsDirectTransactionFlowTests` 目标 Red 来要求“缺原支付事实必须 fail-fast”，现有请求契约无法表达原支付事实；继续推进将需要修改 `transaction-face` 公共契约或新增等价 reference DTO。 | 命中 Plan Grant 禁止动作，停止在 `DIRECT_REFUND_REFERENCE_CONTRACT_GAP`。 |
| 写入内容 | 仅回写 GSD 计划、B4 准入卡和 OpenSpec 任务账本，标记下一 Grant 入口。 | 不写生产代码、测试代码、公共契约、DDL/H2 schema、运行时配置或 Git。 |
| 下一 Grant 建议 | 新增或并入 `B3-DIRECT-REFUND-REFERENCE-REPLAY`：确认直接退款是否必须携带原支付交易 / 原 route snapshot 引用；若确认，需要列明兼容字段、错误码、request summary、route replay、lifecycle 和测试验证范围。 | 未确认前不得给 `FundsTransactionRefundRequest` 增字段，不得把当前直接退款声明为完整原路径 replay Done。 |
| 验证命令 | 文档结构检查、OpenSpec 一致性检查和 `git diff --check`。 | 本轮未改 Java 或测试，不运行 Maven 编译和测试。 |

### 8.7 executionEvidence2026-06-11-b3-direct-refund-reference-replay

本节记录用户确认 `Execution Grant：B3-DIRECT-REFUND-REFERENCE-REPLAY` 后的 Red / Green 交付证据。该 Grant 只覆盖直接退款原交易引用回放，不替代 B4 交易内核全量生产可用。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| 触发输入 | 用户确认推进 `B3-DIRECT-REFUND-REFERENCE-REPLAY`。 | 允许修改直接退款公共契约、transaction converter、route replay、lifecycle saver 和目标测试。 |
| 公共契约 | `FundsTransactionRefundRequest` 新增可选 `referenceTransactionSn`，表示内部原资金交易流水。 | 与 `channelTransactionSn` 分层：前者驱动原 route snapshot 回放，后者只保留外部通道或退款流水追溯。 |
| 实现范围 | 直接退款 converter 将 `referenceTransactionSn` 转为 `ORIGINAL_TRANSACTION` reference；直接付款本金 leg 和费用 leg 支持部分回放；生命周期保存独立退款交易事实，并在成功后更新原交易累计已退摘要。 | 不新增支付工具交易入口，不改 DDL/H2 schema，不改 canonical 主体入参，不引入 VCC 或清结算能力。 |
| Red / Green 证据 | 先证明缺 `referenceTransactionSn` setter 时目标测试编译失败，再补契约和 converter；随后证明原路径回放因 `PAY` leg 全量限制失败，再补 route replay policy；再证明退款事实写到原交易而非独立退款交易，最后补 lifecycle 分层。 | TDD 证据覆盖公共契约、route replay 和 lifecycle 三个真实缺口。 |
| 业务行为 | 直接退款携带原交易引用时按原 route snapshot 回放并形成独立退款资金事实；缺原交易或累计退款超过原 leg 可回放金额时失败，且余额、资金交易、交易明细、账本交易和分录均无副作用。 | `TDD-DIR-005`、`TDD-DIR-ERR-007`、`TDD-DIR-ERR-008` 已纳入目标回归。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsDirectTransactionFlowTests tests`；`... just test-transaction`；`... just test-boundary`。 | PASS：直接交易 flow 49 tests；交易分组 102 tests；边界分组 152 tests。 |
| Not Done | B3 当时未覆盖缺原 route snapshot、当前绑定变化不重选路的交易 flow 全链路副作用、交易投影解释、余额调账审计、VCC lifecycle facade、清结算对账和完整生产发布；其中直接退款缺原 route snapshot flow 已由第 8.9 节补覆盖。 | B4 仍需新的单一 Grant；B3 本切片状态为 `CONSUMED_DIRECT_REFUND_REFERENCE_REPLAY_ONLY`。 |

### 8.8 agentLoopExecutionEvidence2026-06-11-b4-route-replay-snapshot-boundary

本节记录 B4 继续推进时对既有 route replay 覆盖的核验证据。本轮没有新增 Java、测试、公共契约、DDL/H2 schema 或运行时配置，只把已存在的目标测试结果回写到 GSD 与 OpenSpec 状态中。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| 任务选择 | 从 `B4-CANONICAL-REPLAY-FAILFAST` 剩余缺口中选择 `R0-TRX-REPLAY-002` 的纯 route replay 边界复核。 | 属于低风险 Plan Grant，可先确认现有覆盖，不扩大公共契约。 |
| 代码事实 | `DefaultRouteReplayServiceTests#testResolveReplayInstructionShouldReuseSnapshotPaymentInstrumentRef` 覆盖当前请求支付工具引用不覆盖原 route snapshot；`testResolveReplayInstructionShouldReuseSnapshotExternalAccountAndFundingAllocation` 覆盖当前账户/外部账户/资金责任上下文不覆盖原 route snapshot。 | 纯 resolver 边界已经证明原支付工具快照、外部账户快照和资金责任决策优先。 |
| 写入内容 | 仅回写 GSD 计划、B4 准入卡、OpenSpec project、OpenSpec spec 和 Harness tasks 的状态与证据。 | 不写生产代码、测试代码、公共契约、DDL/H2 schema、运行时配置或 Git。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one DefaultRouteReplayServiceTests tests`。 | PASS：9 tests / 0 failures / 0 errors。 |
| Not Done | 该证据不是完整 B4 Done；第 8.8 本身只证明纯 resolver 边界，不证明交易 flow 副作用；直接退款缺原 route snapshot flow 已由第 8.9 节补覆盖，直接退款当前绑定/资金责任变化后的 flow 副作用已由第 8.10 节补覆盖。交易投影解释、余额调账审计、授权/争议/VCC lifecycle 更大组合 replay flow、清结算对账和生产发布仍未完成。 | 下一轮若继续交易层，优先选择投影解释、调账审计或授权/争议/VCC lifecycle 更大组合 replay flow 中的单一低风险切片。 |

### 8.9 agentLoopExecutionEvidence2026-06-11-b4-missing-route-snapshot-flow

本节记录 B4 在 Plan Grant 下补齐的低风险目标测试覆盖。本轮新增测试代码和测试支撑钩子，不修改生产代码、公共契约、DDL/H2 schema、运行时配置或 Git。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| 任务选择 | 从 `B4-CANONICAL-REPLAY-FAILFAST` 剩余缺口中选择 `R0-TRX-REPLAY-001` 的缺原 route snapshot 交易全链路失败无副作用。 | 属于低风险 Plan Grant：只补目标测试，若暴露生产缺口则停在 Red 证据。 |
| 测试事实 | 新增 `FundsDirectTransactionFlowTests#testRefundWithMissingReferenceRouteSnapshotShouldRejectAndLeaveNoSideEffects`，模拟原支付交易存在但 `route_snapshot` 缺失后执行引用原交易的直接退款。 | 现有交易编排在 route replay 解析阶段 fail-fast，未生成新的退款资金交易、交易明细、ledger transaction、posting plan、LedgerEntry 或余额变化。 |
| 测试支撑 | 新增 `FundsTransactionFlowTestSupport#clearFundsTransactionRouteSnapshot`，只在 H2 flow 测试中清空指定资金交易的 route snapshot，并断言命中唯一交易。 | 测试钩子仅用于模拟历史数据或上游异常造成的快照缺失，不进入生产代码路径。 |
| 验证命令 | 首次执行因测试 fixture `owner_id` 过长触发 H2 `VARCHAR(30)` 约束失败；缩短 payee fixture 后复跑 `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsDirectTransactionFlowTests tests`。 | PASS：50 tests / 0 failures / 0 errors。 |
| Not Done | 该证据不是完整 B4 Done；直接退款当前绑定/资金责任变化后的 flow 副作用已由第 8.10 节补覆盖；交易投影解释、余额调账审计、授权/争议/VCC lifecycle 更大组合 replay flow、清结算对账和生产发布仍未完成。 | 下一轮若继续交易层，优先选择投影解释、调账审计或授权/争议/VCC lifecycle 更大组合 replay flow 中的单一低风险切片。 |

### 8.10 agentLoopExecutionEvidence2026-06-11-b4-current-binding-replay-flow

本节记录 B4 在 Plan Grant 下补齐的第二个低风险交易 flow 覆盖。本轮新增测试代码和测试支撑钩子，不修改生产代码、公共契约、DDL/H2 schema、运行时配置或 Git。

| 检查项 | 当前证据 | 裁决 |
| --- | --- | --- |
| 任务选择 | 从 `B4-CANONICAL-REPLAY-FAILFAST` 剩余缺口中选择 `R0-TRX-REPLAY-002` 的直接退款交易 flow 子场景。 | 属于低风险 Plan Grant：只补目标测试，若暴露生产缺口则停在 Red 证据。 |
| 测试事实 | 新增 `FundsDirectTransactionFlowTests#testRefundWithReferenceTransactionShouldReuseOriginalInstrumentAndFundingSnapshot`，在原支付交易 route snapshot 中固化旧支付工具 `CARD-OLD`、旧绑定版本 `BINDING-OLD/v1` 和旧资金责任 `ALLOC-OLD`，随后用携带当前规则上下文的退款请求引用原支付交易。 | 现有交易编排按原 route snapshot 回放；新退款交易 route snapshot 保留旧支付工具和旧资金责任归因，余额、ledger transaction、posting plan 和 LedgerEntry 均与原路径回放一致。 |
| 测试支撑 | 新增 `FundsTransactionFlowTestSupport#enrichFundsTransactionRouteSnapshot`，只在 H2 flow 测试中读取已持久化 route snapshot JSON 并补充顶层归因字段。 | 测试钩子用于模拟 application facade 已经固化历史工具快照和资金责任快照，不进入生产代码路径，不改变 canonical 请求仍以账户主体为入参的结论。 |
| 验证命令 | `WIND_FUNDS_JAVA_HOME=/Users/wuxp/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home just test-one FundsDirectTransactionFlowTests tests`。 | PASS：51 tests / 0 failures / 0 errors。 |
| Not Done | 该证据不是完整 B4 Done；当前仅覆盖直接退款 flow 子场景，不覆盖授权后继、争议/拒付、VCC lifecycle、交易投影解释、余额调账审计、清结算对账或生产发布。 | 下一轮若继续交易层，优先选择交易投影解释、余额调账审计，或授权/争议/VCC lifecycle 更大组合 replay flow 中的单一切片。 |

## 9. handoff

恢复入口：

1. 当前恢复入口已迁移到 [GSD-2-新基线工作流规划.md](GSD-2-新基线工作流规划.md)。本文不再承载活跃未完成计划。
2. 若继续按依赖优先级推进，先进入 `GSD2-W1-BASELINE-GAP-AUDIT`；涉及公共契约、DDL/H2、生产代码、运行时配置或 Git 时，必须进入 `GSD2-W2-SINGLE-GRANT-SELECTION` 并重新确认单一 Execution Grant。
3. 若业务强制先做 VCC，GSD-2 默认候选为 `GSD2-B2-ACCOUNT-HIERARCHY-CONTRACT-001`，不要直接确认 P2 VCC facade。
4. 若业务强制先做全球账户，先在 GSD-2 复核钱包账户、交易内核和对账差错，不直接写 outbound/inbound facade。
5. 若任何 Wave 发现 PRD、DSL、系分、TDD 或代码冲突，先回补权威文档和 OpenSpec，再继续编码。

交接要求：

| 项 | 要求 |
| --- | --- |
| 交付说明 | 必须列出修改文件、覆盖的 TDD 清单项、验证命令、通过/未通过原因和残余风险。 |
| Review | 每轮代码后必须做资深架构师 CR，先列问题和风险，再列变更摘要。 |
| 生产 Done | 只有 Grant 覆盖范围内代码、测试、DDL/H2、审计和验证证据闭合，才可写 Done。 |
| 撤销方式 | 用户说暂停、停止、撤销授权、调整优先级或发现越界，即停止当前 Wave 并回到 handoff。 |
