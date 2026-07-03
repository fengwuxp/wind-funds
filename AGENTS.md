# AGENTS.md

> 本文件是 `wind-funds` 的常驻默认层，只记录每个会话都应知道的项目级约束：仓库定位、模块边界、资金域红线、规格入口和验证命令。
> Skill 是专题流程层，不替代本文件的最低约束；具体使用场景见第 8 节 Skill 路由，本地学习边界见第 9 节。
> 在条件满足的情况下尽可能进入 CAD 自动提交模式推进。

## 1. 项目定位

`wind-funds` 是支付资金底座项目，承载账务 DSL、资金账户、交易路由、钱包交易、账本分录、清结算与对账规格等能力。

核心目标：

- **资金语义独立**：支付、账务、账户、路由、清结算、对账等能力在本仓库演进。
- **契约稳定**：跨模块调用优先依赖 `*-face` 和 `core`，不得暴露 Entity、Mapper 或内部实现类。
- **核心先行**：`core` 承载资金 DSL、枚举、值对象和端口契约，不依赖 DAL、Web、消息或具体实现。
- **实现内聚**：`*-impl` 承载 DAL、服务实现、转换器和领域规则，避免规则扩散到测试、工具或外部适配层。
- **可验证交付**：代码变更必须说明编译、相关测试和规约扫描结果；无法执行时说明环境或依赖限制，测试用例覆盖所有变更，并说明测试范围。

## 2. 技术栈与环境

- Java 21
- Spring Boot 3.x / Spring Framework 6.x
- Maven 多模块构建
- MyBatis Flex
- MapStruct
- Lombok
- Jakarta Validation
- JUnit 5 / Spring Boot Test
- JaCoCo
- Wind Integration / Wind Middleware 相关组件

构建与测试默认复用 IDEA 提供的 JDK；如需命令行显式指定，优先设置 `WIND_FUNDS_JAVA_HOME`，其次使用 `JAVA_HOME`。执行 Maven 前用 `just mvn-version` 或 `mvn -version` 确认 Java runtime 与 POM 要求一致。构建依赖会访问私有 Maven 仓库，失败时需区分代码问题与仓库、网络、凭据或本地缓存问题。

## 3. 模块边界

| 模块 | 职责 |
|------|------|
| `core` | 资金 DSL、账本/路由/交易/钱包核心契约、枚举、值对象和端口。禁止依赖 DAL、Web、具体业务实现。 |
| `ledger/ledger-face` | 账务服务对外契约，提供账本、账本交易、分录查询与写入请求模型。 |
| `ledger/ledger-impl` | 账务实现，包含账本、账本交易、分录、余额投影、MapStruct 和 Mapper。 |
| `transaction/transaction-face` | 资金交易、授权交易、余额控制、让利出资、交易查询和生命周期等契约。 |
| `transaction/transaction-impl` | 资金交易编排、路由解析、快照、交易记录和生命周期保存实现；承接需要交易内核的 wallet-face 应用入口实现。 |
| `wallet/wallet-face` | 钱包账户、支付工具、资金责任、支出控制、账户能力和支付工具交易入口等产品契约。 |
| `wallet/wallet-impl` | 钱包资源、账户、支付工具、资金责任、Spend Rule/预算控制和账本事实只读查询等实现；不创建资金交易事实。 |
| `reconciliation/reconciliation-face` | 清结算与对账能力契约，提供对账、清算准入、差错和审计引用等接口。 |
| `reconciliation/reconciliation-impl` | 清结算与对账实现，只通过 face/core 消费交易和账本事实，不反向依赖其他模块实现。 |
| `governance/governance-face` | 资金数据治理、归档、重放、差异报告和生产修复控制面契约。 |
| `governance/governance-impl` | 治理实现，只通过 face/port 编排、引用和校验交易、账本、对账事实，不反写资金事实。 |
| `tests` | 资金域测试、契约测试、架构边界测试和 H2 表结构测试资源。 |
| `dependencies` | 依赖聚合/BOM，只管理依赖，不写业务代码。 |

强制依赖方向：

```text
wallet-face / transaction-face / ledger-face / reconciliation-face / governance-face
    -> core / capte-domain-core

transaction-impl
    -> transaction-face / wallet-face / core / infrastructure

wallet-impl
    -> wallet-face / ledger-face / core / infrastructure

ledger-impl
    -> ledger-face / core / infrastructure

reconciliation-impl
    -> reconciliation-face / transaction-face / ledger-face / core / infrastructure

governance-impl
    -> governance-face / transaction-face / ledger-face / reconciliation-face / core / infrastructure

tests
    -> impl / face / core
```

资金域边界：

- `*-face` 不依赖 `*-impl`；生产模块不得依赖 `tests`。
- `wallet-impl` 只维护钱包资源、准入、控制事实和账本事实只读聚合，不依赖 `transaction-face`，不创建资金交易事实；需要交易内核的 wallet-face 应用入口由 `transaction-impl` 实现。
- `transaction-impl` 可以依赖 `wallet-face` 消费钱包准入、支付工具、资金责任和支出控制契约，但不得依赖 `wallet-impl`、钱包 DAL、Mapper 或钱包内部实现包。
- `reconciliation-impl` 和 `governance-impl` 是横向对账/治理能力，只能通过 `*-face`、core port 或只读证据引用消费主链事实；允许依赖 `transaction-face`、`ledger-face`、`reconciliation-face`，禁止依赖其他模块 `*-impl`、DAL、Mapper 或反写交易、账本、钱包事实。
- `route` 只解析资金路径，不直接写交易事实或账本事实。
- `ledger` 只维护账本事实和账本投影，不反向持有业务交易生命周期状态。
- 资金域 Java 包名和源码路径统一使用 `com.wind.funds` / `com/wind/funds`；不得恢复历史 Capte funds 包根或旧 Wind integration funds 包根。`com.capte.domain` 仍是外部领域依赖边界，可按模块依赖约束保留。

编码约规：

- 空值契约遵循 JSpecify 标注：`@NonNull` 方法返回值、参数和集合元素按非空契约使用，不写重复的 `null` 防御；只有 `@Nullable`、外部输入、反序列化边界或持久化读取等不可信来源才做显式空处理。

业务日志与可观测性：

- `ledger`、`wallet`、`transaction` 及清结算/治理实现的关键用例边界必须输出可追溯业务日志，覆盖准入/拒绝、交易或账本状态变化、冻结/解冻、入账/退款/冲正、幂等复用、重试/补偿和对账差错处理；DTO、Entity、Mapper、MapStruct 转换、简单查询和循环明细不打流水账。
- 业务日志使用项目统一日志框架，优先 `@Slf4j` 和参数化日志；禁止 `System.out`、`printStackTrace`、吞异常或只打印异常 message。异常日志保留 cause，并带上稳定业务标识。
- 日志字段只保留最小可定位上下文：`tenantId`、业务场景/业务单号、`transactionSn`、`ledgerTransactionSn`、账户或主体脱敏标识、状态、金额币种、规则/版本/幂等摘要和 traceId；不得输出完整 Request/Response、Entity、SQL、`contextVariables`、PAN、CVV、token、密钥、证件号、手机号、外部账号等敏感信息。
- 日志不替代交易事实、账本事实、审计证据、对账证据或测试断言；涉及资金事实、幂等、补偿、对账和安全边界的日志改动必须随相关切片验证，并在交付说明里列出验证命令。

## 4. 规格入口

| 路径 | 用途 |
|------|------|
| `docs/产品设计/` | 支付资金底座最终版产品设计入口，承载 PRD 总览、交易/路由/钱包/账目/投影、清结算与对账、归档重放与指标治理、产品验收矩阵。 |
| `docs/DSL设计/` | 支付资金底座最终版 DSL 承载层入口，承载资金指令、路由快照、账务计划、账本分录、投影和 JSON 契约场景。 |
| `docs/系分设计/` | 支付资金底座最终版系统分析设计入口，承载模块边界、架构分层、服务能力、表设计、状态机、观测、安全和金融红线。 |
| `docs/TDD设计/` | 支付资金底座最终版 TDD 设计入口，承载测试对象、测试层级、场景矩阵、红线用例、目标测试资产和验证命令。 |
| `docs/用户接入指南/` | 支付资金底座生产接入说明，承载当前已验证公共契约、能力场景、接入顺序和禁用路径。 |
| `tests/src/test/resources/jdbc-schema.sql` | 资金账户、交易、账本相关建表语句，统一作为 H2/MySQL Mode 测试表结构来源。 |

进入编码前，凡涉及支付资金底座目标态、DSL、API、清结算、对账、归档、TDD 门禁或能力规格，必须同步阅读 `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`；涉及接入契约、生产使用说明或能力差距复核时再同步阅读 `docs/用户接入指南`，并在交付说明中列出本次覆盖的测试清单项或说明不适用原因。

## 5. 项目编码入口

权威设计包回答做什么、做到什么程度；Superpowers 是工程纪律层，约束 TDD、Review、Refactor、金融红线和验证门禁；Harness 是执行编排层，管理批次、写入范围、只读范围、人工确认点和验证命令。

进入编码、重大设计 CR、基线复核或 CAD Mode 前，以当前 `docs/`、Harness Plan、Git 提交点和用户确认作为执行依据。需要长期沉淀的项目决策，优先写入权威文档、Harness Plan 或提交记录，避免只存在于临时对话。

### 5.1 项目编码入口

通用编码规约按第 8 节 Skill 路由执行。本节不重复展开编码规则。

### 5.2 工程约规执行门禁

代码编写、测试代码编写、代码 CR、重构评估、Bug 修复、TDD 推进和 CAD Mode 都必须先按第 8 节触发 `资深架构师` Skill，并严格执行该 Skill 的场景路由、编码规约、测试规约、Review 规约、AI 协作规约和对应 reference。

落地要求：

- 写生产代码时，必须遵循 `资深架构师` 的编码红线和 Java/Spring/Wind 编码规约；本项目额外遵循第 3 节模块边界、第 6 节资金测试红线和当前 Harness Execution Grant。
- 写测试代码时，必须遵循 `资深架构师` 的测试驱动设计与测试资产治理要求，参考测试最佳实践；测试必须表达业务场景、真实链路、替身边界、断言事实和验证命令，资金变化场景同时满足第 6 节余额、账务、幂等和审计断言。
- 做代码 CR 时，必须按 `资深架构师` 的 Review 判断顺序输出问题优先的结论，先看业务语义、边界方向、契约完整性、失败路径和工程一致性，再看复用质量与格式问题；不得只做风格化总结。
- 遇到 `资深架构师` Skill 与本文件、权威设计包、Harness Plan 或资金域红线冲突时，以更具体、更保守、资金安全优先的约束为准；冲突无法自行消解时，先列不超过 3 个选项等待确认。

### 5.3 CAD 自动推进约规

当用户明确要求进入 CAD 自动模式，且 `资深架构师` Skill、权威设计包、Harness Plan、Execution Grant、工作树状态和工具权限均满足进入门禁时，执行方式默认从逐步询问切换为受控自动推进。代理应在授权范围内持续完成本轮任务拆解、实现、测试、验证、Review、提交或摘要收口；没有触发停止条件时，应自动进入下一轮，不得把普通不确定、局部测试失败或可自行修复的问题作为中断理由交还用户。

自动推进要求：

- 优先自行补齐可从本仓库发现的信息、运行必要命令、修复本轮范围内的测试或规约问题，并给出阶段性简短更新；除高风险确认点外，不反复询问用户。
- 每轮必须遵循 `资深架构师` CAD Mode 的执行循环：确认范围、实现或修复、运行对应验证、CR 本轮改动、记录残余风险；具备 `auto_commit` 授权且验证通过时自动提交，权限不足时进入 `summary_only` 但继续推进可执行下一轮。
- CAD 自动推进默认采用分层验证，平衡编码速度和资金安全：原子代码或测试改动优先运行 `just verify-slice <TestClass>[,<TestClass>] [module]` 或最相关的 `just test-*` 分组；跨模块契约、资金红线、验证矩阵、构建配置、数据库脚本或批次收口时，再运行 `just verify-fast` 或 `just verify-cad`。不得把每个小改都机械升级为全量验证，也不得在触及金额、账务、幂等、清结算、对账或发布前收口时省略必要的完整验证。
- 只有出现用户明确中断、Execution Grant 越界、工作树冲突无法安全合并、生产/数据/安全/兼容等高风险决策需确认、工具权限被拒且无法降级、验证失败且无法在本轮授权范围内修复，或严重错误会扩大风险时，才暂停自动推进并说明阻断原因和下一步选项。
- CAD 自动推进不得绕过第 3 节模块边界、第 6 节资金测试红线、平台权限、沙箱限制、Git 授权或不可逆操作确认；不得为了保持自动推进而提交未验证、越界或不可解释的变更。

## 6. 资金测试红线

通用测试设计按第 8 节 Skill 路由执行。本项目只保留资金域不可丢失的断言红线：

- 涉及金额、状态流转、幂等、重放、账务平衡、余额约束、冻结/解冻、清结算和对账差错的变更必须补测试。
- 有资金变化的测试不得只断言交易状态、route、entry 数量或“不报错”；必须同时断言相关主体的账本余额桶、posting plan 平衡、ledger transaction 可追溯和幂等行为。测试支撑能力按 TDD 设计重建，不恢复旧版过渡用例。
- Spring 服务层流程测试必须优先使用真实内部 Spring Bean 和 H2 表结构，可继承：AbstractFundsServiceTest；账户、平台账户、路由基础数据等测试依赖由 `setup` 或用例显式准备，Mock/Fake/Recording 只用于外部系统、不可控环境或明确的端口边界。PodamUtils 可用于数据模拟
- 业务组合测试必须每一步都断言余额变化，不能只断言最终余额。
- 冻结/解冻测试必须证明冻结只做同主体 `AVAILABLE <-> FROZEN` 控制，不表达消费、扣划或跨主体价值转移。
- 授权拒绝不得生成 route/entry，不得写入 `declinedAmount` 或被当作 chargeback 事件。
- 清结算、对账、归档和报表测试必须证明来源事实、批次、规则版本、审计、重跑幂等和只读投影边界。

测试 backlog 权威入口：`docs/TDD设计/支付资金底座测试驱动设计.md`。

## 7. 常用命令

仓库命令固化在根目录 `Justfile`。优先使用 `just`；若本机未安装 `just`，回退到对应 Maven 命令。

修改代码、测试、构建配置、数据库脚本或运行时配置前：

```bash
just mvn-version
just compile
```

修改后至少执行：

```bash
just compile
```

按范围执行相关测试：

```bash
just verify-slice <TestClass>[,<TestClass>] [module]
just test-core
just test-ledger
just test-transaction
just test-balance-control
just test-business-flow
just test-boundary
just test-governance
just test-one <TestClass> [module]
```

提交前优先执行：

```bash
just pmd
```

CAD 原子变更默认优先执行：

```bash
just verify-slice <TestClass>[,<TestClass>] [module]
```

`verify-slice` 聚合 `mvn-version`、`compile` 和指定测试类，用于单个实现切片、单类回归或小范围测试资产调整。若只改 `Justfile`、文档或治理入口，可选择 `just verify-fast`、`git diff --check` 或对应分组命令作为更合适的验证。

CAD 阶段收口、完整基线复核或提交前风险较高时执行：

```bash
just verify-cad
```

`verify-fast` 聚合 `mvn-version`、`compile`、`test-boundary`、`test-governance` 和 `test-reconciliation`，用于非业务逻辑、治理入口和测试基线的中等成本复核。

`verify-cad` 聚合 `mvn-version`、`compile`、`test-core`、`test-ledger`、`test-transaction`、`test-balance-control`、`test-business-flow`、`test-boundary`、`test-governance`、`test-reconciliation` 和 `pmd`，用于声明本地完整验证证据。

如 `pmd:check` 因私有仓库、snapshot、本地 Maven 缓存或依赖解析失败，应在交付说明中按环境依赖问题记录，不得等同于代码规约违规。

仅修改文档、产品设计、系统分析设计、方案讨论、需求澄清、流程图或说明性材料时，不要求运行编译；交付时说明未运行编译的原因。

## 8. Skill 路由

| 场景 | 必用 Skill | 产物边界 |
|------|------------|----------|
| 编码、编码设计、架构设计、系统分析、技术方案、代码评审、重构评估、测试设计、Git 提交建议、工程治理 | `资深架构师` | 工程边界、模块设计、接口契约、代码修改、测试策略、验证命令、Review 结论和交付说明。 |
| 产品架构、PRD、业务建模、能力地图、角色权限、业务流程、状态机、规则矩阵、运营后台、产品验收，及支付资金产品方案、账户/账务模型、资金流、清结算、对账、争议拒付、风控、合规口径 | `产品架构专家` | 产品目标、角色、对象、流程、状态、规则、权限、指标、运营后台、账户/账务模型、账务矩阵、异常路径、产品验收和风险清单。 |
| 支付资金能力进入编码或系分落地 | 两者都用 | 先由 `产品架构专家` 定产品架构、资金语义、业务不变量、验收边界和合规待确认项，再由 `资深架构师` 落模块、接口、测试、验证和代码实现。 |

- 涉及真实资金、监管、跨境、外汇、客户资金、备付金、风控或合规口径时，只输出产品和系统设计分析，不替代法律、税务、会计或合规最终结论。
- 遇到架构边界、数据模型、安全、兼容性、生产行为或不可逆操作，先给不超过 3 个选项并等待用户确认。

## 9. 本地协作学习

- 本项目默认不启用长期学习；只有用户明确同意后，才可读取或写入 `~/.skill-learning/` 或 `SKILL_LEARNING_HOME`。
- 当出现可长期复用的团队约规、稳定偏好、业务背景或决策方式，且不会打断当前任务时，可以一句话询问是否启用。
- 用户同意但未说明范围时，只对当前项目或当前技能生效；全局生效必须由用户明确说明。
- 不得记录密钥、账号、客户/商户/用户敏感信息、生产配置、合同、权限细节或未经确认的合规/资金判断。
- 低风险观察可进入 `Pending Observations`；影响长期行为、跨项目复用、业务/合规/隐私边界的记录必须先显示确认。
- 用户拒绝时不创建目录、不写入文件，本任务链中不再主动提示。

## 10. 交付说明

交付说明的通用结构、Git 提交建议和验证结论表达按第 8 节 Skill 路由执行。本项目交付时必须额外说明：

- Git commit message 默认使用中文，除非引用标准 commit type、外部任务号、版本号、代码标识符或英文专有名词更清晰；可保留 `feat:`、`fix:`、`test:`、`docs:`、`chore:` 等类型前缀，但 subject 尽可能用中文表达实际变更。
- 修改了哪些文件和模块。
- 覆盖了哪些测试清单项；若不适用，说明原因。
- 执行了哪些验证命令。
- 验证是否通过。
- 未能执行或未通过的原因。
