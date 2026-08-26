# AGENTS.md

> 本文件是 `wind-funds` 的常驻项目契约，只保留每次会话都应知道的项目定位、模块边界、资金红线、规格入口、验证命令和 Skill 路由。
> 本项目启用 Wind 项目编码约规；通用 Java 编码、测试、Review 和跨阶段协作细节不在本文件重复展开，按 Skill 路由读取。

## 1. 项目身份

- 项目类型：Java 21 / Spring Boot 3.x / Maven 多模块 Wind 资金底座。
- 项目定位：面向多个上层业务场景提供资金相关的基础设施和公共能力；不是任一单一业务场景的应用、流程编排或外部协议适配实现。
- 核心业务：资金 DSL、账本、钱包、资金交易、交易路由、清结算、对账和治理。
- 默认命令入口：根目录 `Justfile`。
- 默认语言：中文沟通、评审、规划、交付说明和 Git commit message；代码标识符、协议字段、API、命令、错误码、标准 commit type 和英文专有名词按项目既有英文保留。

核心原则：

- 公共能力前提：所有产品、DSL、系分、TDD、API、模型和模块设计，都必须先判断其服务目标是否属于跨场景稳定复用的资金语义、资金事实或资金不变量；本项目只建设公共资金能力及其必要实现、验证和治理，只有跨场景稳定的资金语义、资金事实和资金不变量才可进入 `core`、`*-face` 或 Public API。
- 上游归一：单一场景的业务对象、流程状态、宿主策略、外部协议、权威/finality 判断和适配路由由对应上层或适配器负责；上层必须先归一为明确、稳定、可验证的资金指令或事实引用，再调用本项目公共能力。
- 克制抽象：公共能力不等于最大化抽象；没有真实场景和 Consumer 证明的复用需求，不为可能出现的未来场景新增通用引擎、配置路由、扩展点或平行内核。
- 账务目标：不管场景多复杂，底层只回答三件事：谁的钱、多少钱、怎么变的。
- 资金语义独立：支付、账务、账户、路由、清结算、对账等能力在本仓库演进。
- 契约稳定：跨模块调用优先依赖 `*-face` 和 `core`，不得暴露 Entity、Mapper 或内部实现类。
- 核心先行：`core` 承载资金 DSL、枚举、值对象和端口契约，不依赖 DAL、Web、消息或具体实现。
- 实现内聚：`*-impl` 承载 DAL、服务实现、转换器和领域规则，避免规则扩散到测试、工具或外部适配层。
- 可验证交付：代码变更必须说明编译、相关测试和规约扫描结果；无法执行时说明环境或依赖限制。

构建依赖会访问私有 Maven 仓库。验证失败时先区分代码问题和仓库、网络、凭据或本地缓存问题。

## 2. 模块边界

模块职责：`core` 放资金 DSL、核心契约、枚举、值对象和端口；`fx-impl` 放汇率快照选择和金额换算默认实现；各 `*-face` 放对外契约；各 `*-impl` 放实现、DAL、Mapper、MapStruct 和内部规则；`tests` 放资金域测试、契约测试、架构边界测试和 H2 表结构；`dependencies` 只做依赖聚合 / BOM。

目录组织：根 POM 只声明 `core`、业务能力聚合模块、`tests` 和 `dependencies`；`fx`、`ledger`、`wallet`、`transaction`、`reconciliation`、`governance` 各自由本目录聚合 POM 管理 `face` / `impl` 叶子模块。聚合 POM 不承载 Java 源码或业务依赖，叶子模块 artifactId 继续使用完整的 `wind-funds-<capability>-<layer>` 名称。

强制依赖方向：`core -> wind-operator / wind-integration-core / wind-money`；`*-face -> core / wind-operator`；`fx-impl -> core`；`transaction-impl -> transaction-face / wallet-face / core / infrastructure`；`wallet-impl -> wallet-face / ledger-face / core / infrastructure`；`ledger-impl -> ledger-face / core / infrastructure`；`reconciliation-impl -> reconciliation-face / transaction-face / ledger-face / core / infrastructure`；`governance-impl -> governance-face / transaction-face / ledger-face / reconciliation-face / core / infrastructure`；`tests -> impl / face / core`。

模块红线：

- `*-face` 不依赖 `*-impl`；生产模块不得依赖 `tests`。
- `fx-impl` 只提供显式汇率价格选择和金额换算，不创建报价、换汇执行、资金交易或账本事实。
- `wallet-impl` 不依赖 `transaction-face`，不创建资金交易事实。
- `transaction-impl` 可以依赖 `wallet-face` 消费钱包准入、支付工具、资金责任和支出控制契约；不得依赖 `wallet-impl`、钱包 DAL、Mapper 或钱包内部实现包。
- `reconciliation-impl` 和 `governance-impl` 只能通过 `*-face`、core port 或只读证据引用消费主链事实；禁止依赖其他模块 `*-impl`、DAL、Mapper 或反写交易、账本、钱包事实。
- `route` 只解析资金路径，不直接写交易事实或账本事实。
- `core` 只把 `WindOperator` 作为运行时操作者契约，不调用权限判断或动态请求信息方法，不直接序列化、持久化或纳入稳定摘要；审计只在事实写入边界投影稳定身份字段。
- `ledger` 只维护账本事实和账本投影，不反向持有业务交易生命周期状态。
- 资金域 Java 包名和源码路径统一使用 `com.wind.funds` / `com/wind/funds`；不得引入其他资金域包根。

## 3. Wind 项目约规

本项目遵守 Wind 项目编码约规。涉及 face/impl、模型归位、Entity 不外露、基础服务、ServiceImpl、MyBatis Flex、币种枚举、TDD/CR 或代码生成后审查时，先按 `wind-coding-conventions` 判断规则，再由 `senior-software-architect` 闭环源码设计、测试和验证。

项目级 Wind 红线：

- Public API、ApplicationService、Adapter、事件和跨模块契约只能暴露 DTO、Request、Query、Command、枚举或值对象；不得暴露 Entity、Mapper、Repository、MyBatis Page、QueryWrapper 或实现类。
- 公共契约放在 `*-face` 或 `core`；实现、DAL、Mapper、MapStruct 和内部规则放在 `*-impl`。
- 优先复用已有服务和 helper；不新增一行透传、浅服务、伪抽象、Mapper 包装、Fake/Mock 业务实现或内存版业务 Service。
- MyBatis Flex 使用项目既有模式和 `XxxRefs`；不要新增 `LambdaQueryWrapper` 或裸字符串字段名。
- 模型转换使用 MapStruct converter；converter 不做业务决策、数据库读取、远程调用、权限判断或审计。
- 领域生命周期当前值在数据库列、Entity、DTO、Request、Query 和 JSON 中统一使用 `state`，枚举类型使用 `XxxState`；一次运行、准入或决策结果使用 `outcome`、`result` 或更具体的业务名。外部、展示或发布状态必须使用 `externalStatus`、`displayStatus`、`publishStatus` 等限定名称，不得与领域生命周期混用；数据库 `is_*` 布尔列与 Java 无 `is` 前缀属性按各自语言约规命名。
- 空值契约遵循 JSpecify：已声明非空的值不写重复防御式空判断；只有 `@Nullable`、外部输入、反序列化边界或持久化读取等不可信来源才做显式空处理。
- 业务事件、审计展示和可回放消息优先使用稳定 `eventKey + params`，不得把中文文案或可变翻译作为业务判断依据。

## 4. 资金测试红线

涉及金额、状态流转、幂等、重放、账务平衡、余额约束、冻结 / 解冻、清结算和对账差错的变更必须补测试。资金变化测试必须同时断言相关主体账本余额桶、posting plan 平衡、ledger transaction 可追溯和幂等行为。

- Spring 服务层流程测试优先使用真实内部 Spring Bean 和 H2 表结构；可继承 `AbstractFundsServiceTest`。
- Mock / Fake / Recording 只用于外部系统、不可控环境或明确端口边界。
- 业务组合测试每一步都断言余额变化，不能只断言最终余额。
- 冻结 / 解冻只做同主体 `AVAILABLE <-> FROZEN` 控制，不表达消费、扣划或跨主体价值转移。
- 授权拒绝不得生成账务 RouteLeg、posting、LedgerEntry，不得写入 `declinedAmount`，不得被当作 chargeback 事件；允许保存不含 legs、不可回放的 RouteSnapshot 作为拒绝解释证据。
- 清结算、对账、归档和报表测试必须证明来源事实、批次、规则版本、审计、重跑幂等和只读投影边界。

测试 backlog 权威入口：`docs/TDD设计/支付资金底座测试驱动设计.md`。

## 5. 业务日志与审计

`ledger`、`wallet`、`transaction` 及清结算 / 治理实现的关键用例边界必须输出可追溯业务日志，覆盖准入 / 拒绝、交易或账本状态变化、冻结 / 解冻、入账 / 退款 / 冲正、幂等复用、重试 / 补偿和对账差错处理。

日志要求：

- 优先使用 `@Slf4j` 和参数化日志；禁止 `System.out`、`printStackTrace`、吞异常或只打印异常 message。
- 异常日志保留 cause，并带上稳定业务标识。
- 日志字段只保留最小可定位上下文：`tenantId`、业务场景 / 业务单号、`transactionSn`、`ledgerTransactionSn`、账户或主体脱敏标识、状态、金额币种、规则 / 版本 / 幂等摘要和 traceId。
- 不得输出完整 Request/Response、Entity、SQL、`contextVariables`、PAN、CVV、token、密钥、证件号、手机号、外部账号等敏感信息。
- 日志不替代交易事实、账本事实、审计证据、对账证据或测试断言；涉及资金事实、幂等、补偿、对账和安全边界的日志改动必须随相关切片验证。

## 6. 规格入口

权威设计入口：`docs/产品设计/`、`docs/DSL设计/`、`docs/系分设计/`、`docs/TDD设计/`、`docs/用户接入指南/`。测试表结构入口：`tests/src/test/resources/jdbc-schema.sql`。

进入编码前，凡涉及支付资金底座目标态、DSL、API、清结算、对账、归档、TDD 门禁或能力规格，必须同步阅读 `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`。涉及接入契约、生产使用说明或能力差距复核时，再同步阅读 `docs/用户接入指南`。

## 7. 验证命令

修改代码、测试、构建配置、数据库脚本或运行时配置前运行 `just mvn-version` 和 `just compile`；修改后至少执行 `just compile`。

聚焦改动优先运行最小相关验证：

```bash
just verify-slice <TestClass>[,<TestClass>] [module]
just test-one <TestClass> [module]
just test-core
just test-ledger
just test-transaction
just test-balance-control
just test-business-flow
just test-boundary
just test-governance
```

提交前优先执行 `just pmd`；阶段收口、完整基线复核或高风险提交前执行 `just verify-cad`。

仅修改文档、产品设计、系统分析设计、方案讨论、需求澄清、流程图或说明性材料时，不要求运行编译；至少执行 `git diff --check`。

## 8. Skill 路由

| 场景 | 必用 Skill | 产物边界 |
| --- | --- | --- |
| 跨专业、跨阶段或跨轮持续推进，或需统一项目执行规范 / Loop / Worker / Checker、授权、验证和知识回流 | `wise-agent` | 统一目标、边界、能力选择、执行和交付闭环；Goal 请求落入项目已有 OpenSpec / Spec / Issue / 任务计划，不创建运行时 Goal；简单单领域任务直接使用对应 Skill，不做多余编排，不替代人类 Owner、Git 授权或上线审批。 |
| 编码、编码设计、架构设计、系统分析、技术方案、代码评审、重构评估、测试设计、工程治理 | `senior-software-architect` | 工程边界、模块设计、接口契约、代码修改、测试策略、验证命令、Review 结论和交付说明。 |
| Wind/Nobe 编码约规判断、face/impl、模型归位、基础服务、Entity 不外露、MyBatis Flex、ServiceImpl 和 TDD/CR 约规 | `wind-coding-conventions`，源码执行配合 `senior-software-architect` | 判断是否偏离 Wind 约规、给最小整改建议；真实源码修改和验证由架构师闭环。 |
| 产品架构、PRD、业务建模、能力地图、业务流程、状态机、规则矩阵、产品验收，及支付资金产品方案 | `product-architecture-expert` | 产品目标、角色、对象、流程、状态、规则、权限、指标、异常路径、产品验收和风险清单。 |
| 结构化 Java Service 脚手架生成 | `java-service-code-generator` | 必须有 DDL / schema / Java 类 / 字段表格；不从纯自然语言生成生产代码。 |

涉及真实资金、监管、跨境、外汇、客户资金、备付金、风控或合规口径时，只输出产品和系统设计分析，不替代法律、税务、会计或合规最终结论。

## 9. 交付说明

完成任务时说明：

- 改了什么。
- 覆盖了哪些测试清单项；若不适用，说明原因。
- 执行了哪些验证命令。
- 验证是否通过。
- 未能执行或未通过的原因。
- 残余风险和下一 owner。

Git commit message 默认使用中文；可保留 `feat:`、`fix:`、`test:`、`docs:`、`chore:` 等标准前缀，但 subject 尽可能用中文表达实际变更。
