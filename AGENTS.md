# AGENTS.md

> 本文件为 AI 编程 Agent 提供 `wind-funds` 项目级说明、模块说明、开发约规与落地指引。
> 每次修改代码前必须阅读并遵守。兼容：Claude Code | OpenAI Codex | GitHub Copilot | Cursor | Gemini CLI。

## 1. 项目说明

### 1.1 项目定位

`wind-funds` 是支付资金底座项目，承载账务 DSL、资金账户、交易路由、钱包交易、账本分录、清结算与对账规格等能力。

项目采用 Maven 多模块组织方式，保持资金域代码与 Capte 业务域仓库解耦。当前阶段保留原有 Java 包名和对 `capte-domain-core`、`catep-infrastructure-dal` 的 Maven 契约依赖，避免在仓库迁移中混入业务语义重构；后续若需要进一步 Wind 化基础能力，应通过单独变更设计、测试和兼容迁移完成。

核心设计目标：

- **资金语义独立**：支付、账务、账户、路由、清结算、对账等能力在本仓库演进。
- **契约稳定**：跨模块调用优先依赖 `*-face` 和 `core` 契约，不暴露 Entity、Mapper、内部实现类。
- **核心先行**：原 `capte-domain/wind-ledger` 模块已迁为 `core`，作为资金 DSL、枚举、值对象和端口契约中心。
- **实现内聚**：各 `*-impl` 模块承载 DAL、服务实现、转换器和领域规则，避免规则扩散到测试、工具或外部适配层。
- **可验证交付**：所有代码变更必须能通过编译、相关测试与规约扫描验证，或说明环境/依赖限制。

### 1.2 技术栈

- Java 21
- Spring Boot 3.x / Spring Framework 6.x 生态
- Maven 多模块构建
- MyBatis Flex
- MapStruct
- Lombok
- Jakarta Validation
- JUnit 5 / Spring Boot Test
- JaCoCo
- Wind Integration / Wind Middleware 相关组件

### 1.3 构建环境要求

- 编译运行使用的 JDK 版本必须以项目 `pom.xml` 或父 POM 中配置的 Java/JDK 版本为准。若出现 `无效的目标发行版`，说明当前 `JAVA_HOME`、Maven 或 IDEA 使用的 Java 版本与 POM 要求不一致。
- 默认使用 IDEA 自带或 IDEA 项目配置的 JDK 执行编译、测试、代码生成和静态检查；执行命令前应确保该 JDK 与 POM 配置的版本一致。
- Maven 版本以项目和团队环境为准，执行前先用 `mvn -version` 确认 Java runtime。
- 构建依赖会访问私有 Maven 仓库，执行编译和测试前需确保本地 Maven 配置、仓库权限、网络与凭据可用。
## 2. Maven 模块说明

| 模块 | 类型 | 职责 |
|------|------|------|
| `core` | jar | 原 `capte-domain/wind-ledger`，承载资金 DSL、账本/路由/交易/钱包核心契约、枚举、值对象和端口。禁止依赖 DAL、Web、具体业务实现。 |
| `ledger/ledger-face` | jar | 账务服务对外契约，提供账本、账本交易、分录查询与写入请求模型。 |
| `ledger/ledger-impl` | jar | 账务实现，包含账本、账本交易、分录、余额投影、MapStruct 和 Mapper。 |
| `transaction/transaction-face` | jar | 资金交易、资金账户、预算组、支付工具、路由生命周期等契约。 |
| `transaction/transaction-impl` | jar | 资金交易编排、路由解析、快照、资金账户、交易记录和生命周期保存实现。 |
| `wallet/wallet-face` | jar | 钱包交易、授权、冻结、解冻、退款、转账、充值、提现等产品契约。 |
| `wallet/wallet-impl` | jar | 钱包产品门面实现，把钱包请求转换为资金指令并交给交易编排。 |
| `tests` | jar | 资金域测试、契约测试、架构边界测试和 H2 表结构测试资源。 |
| `dependencies` | pom | 依赖聚合/BOM 模块。仅管理依赖，不写业务代码。 |

## 3. 目录与规格说明

| 路径 | 说明 |
|------|------|
| `docs/` | 从 `capte-domain` 同步过来的支付资金底座产品设计、DSL 设计、系分设计和过程材料。 |
| `openspec/` | 支付资金底座 OpenSpec 项目上下文、能力规格和变更提案。 |
| `tests/src/test/resources/jdbc-schema.sql` | 资金账户、交易、账本相关建表语句，统一作为 H2/MySQL Mode 测试表结构来源。 |

进入编码前，涉及支付资金底座目标态、DSL、API、Harness 门禁或能力规格时，必须同步阅读相关 `docs/v5` 与 `openspec` 内容。

## 4. 架构与依赖约规

强制依赖方向：

```text
wallet-face / transaction-face / ledger-face
    -> core / capte-domain-core

wallet-impl
    -> wallet-face / transaction-face / core

transaction-impl
    -> transaction-face / ledger-face / core / infrastructure

ledger-impl
    -> ledger-face / core / infrastructure

tests
    -> impl / face / core
```

约束：

- `core` 不依赖 `ledger/transaction/wallet` 的实现模块，不依赖数据库、Web 或消息中间件细节。
- `*-face` 不依赖 `*-impl`。
- `*-impl` 可依赖本域 `face`、必要其他域 `face`、`core` 和基础设施。
- `wallet` 作为产品门面，只编排资金指令，不直接写交易事实或账本事实。
- `route` 只解析资金路径，不直接写交易事实或账本事实。
- `ledger` 只维护账本事实和账本投影，不反向持有业务交易生命周期状态。
- 生产模块不得依赖 `tests`。

## 5. 编码约规

- 类名使用 `UpperCamelCase`；方法名、参数名、成员变量、局部变量使用 `lowerCamelCase`；常量使用 `UPPER_UNDERSCORE`。
- 测试类以被测类名开头、`Test` 或 `Tests` 结尾；新增测试方法以 `test` 开头。
- Query 使用 `XxxQuery`，Request 使用 `XxxRequest`，DTO 使用 `XxxDTO`。
- 对外 API 和跨模块契约使用 DTO、Request、Query，不暴露 Entity。
- 模型转换优先使用 MapStruct，Converter 放在 `mapstruct` 包，方法命名使用 `convertToXxx`。
- 禁止使用 `LambdaQueryWrapper`，查询条件使用 MyBatis Flex 生成的 `XxxRefs` 常量类。
- 写库默认使用 selective 方法，避免无意覆盖空值。
- 金额使用 `BigDecimal` 或最小货币单位整型，禁止 `new BigDecimal(double)`；金额字段必须明确币种、精度和舍入规则。
- 时间优先使用 `LocalDateTime`、`LocalDate`、`LocalTime`。
- 对于 JDK 标准库 API，优先使用当前项目 JDK 版本提供的最新稳定 API，避免沿用过时写法；若为了兼容性、性能或语义清晰选择旧 API，应在代码或交付说明中解释原因。
- 异常使用项目统一 `BaseException` 或断言工具；包装第三方异常时保留 cause。
- 日志统一使用 SLF4J，占位符输出，不打印敏感信息。

绝对不允许：

- `System.out.println`
- `e.printStackTrace()`
- 提交密钥、token、生产地址、个人凭据
- 未告知用户调用外部 API
- 顺手重构、顺手格式化无关文件、顺手改命名
- 使用 `Vector`、`Hashtable`、`Stack`
- 在 `foreach` 中直接 `remove` / `add`
- 返回 `null` 代替空集合
- 修改或删除有明确用途的注释代码，除非先询问用户并获得确认

## 6. 开发流程

### 6.1 修改前

1. 阅读本文件和相关模块现有代码。
2. 涉及代码、测试、构建配置、数据库脚本或运行时配置变更时，运行：

```bash
mvn compile
```

3. 不涉及代码的文档、产品设计、系统分析设计、方案讨论、需求澄清、流程图或说明性材料，不运行编译命令。
4. 若编译失败，先判断是否为环境问题、依赖问题或既有代码问题，并在交付说明中如实记录。
5. 只修改当前任务明确要求的文件；遇到不确定设计选择时，列出不超过 3 个选项询问用户。

### 6.2 修改中

- 先找现有模式，再写新代码。
- 优先小范围修改，不做无关重构。
- 对有业务含义的逻辑先补齐用例、边界条件和异常路径。
- 新增公共契约时同步考虑 DTO、Query、Request、枚举、转换器、测试和兼容性。
- 不引入任务未要求的抽象层、扩展点、配置项或预留能力。

### 6.3 修改后

涉及代码、测试、构建配置、数据库脚本或运行时配置变更时，至少执行：

```bash
mvn compile
```

根据变更范围执行相关测试，例如：

```bash
mvn -pl core test -Dtest=FundsInstructionSpecContractTests
mvn -pl tests -am test -Dtest=LedgerServiceImplTests
mvn -pl tests -am test -Dtest=DefaultFundsInstructionLifecycleSaverTests
```

提交前执行团队认可的规约检查：

```bash
mvn pmd:check
```

交付时必须说明：

- 修改了哪些文件和模块。
- 执行了哪些验证命令。
- 验证是否通过。
- 未能执行或未通过的原因。

## 7. 测试指引

- 单元测试优先，不需要 Spring 容器的测试不要启动 Spring。
- 涉及金额、状态流转、幂等、重放、账务平衡、余额约束、冻结/解冻、清结算和对账差错的变更必须补测试。
- 数据库相关测试优先使用 H2 MySQL Mode：`jdbc:h2:mem:*;MODE=MySQL`。
- 外部依赖使用 fake、stub、mock、WireMock 或 Testcontainers，测试不得连接真实外部服务。
- 架构边界测试用于保护 `ledger`、`route`、`wallet` 的职责边界，不得为绕过测试而删除约束。

### 7.1 测试驱动设计

- 任何有业务含义的设计、重构或代码变更，必须先从实际使用场景、用户用例、测试用例、边界条件、异常路径和验收标准出发，再抽象模型、接口、模块和扩展点。
- 涉及支付资金底座目标态、DSL、API、清结算、对账、归档、Harness 门禁或能力规格时，必须先对照 `docs/v5/产品设计/v5 产品层 TDD 验收矩阵.md`、`docs/v5/v5 DSL 契约复审矩阵.md`、`docs/v5/系分设计/API 契约测试与编码实施计划.md` 和相关 OpenSpec requirements，确认本次变更对应哪些测试项。
- 新增能力不得只写 happy path；必须同时考虑正常路径、异常路径、边界路径、幂等重放、权限审计、余额约束、账务平衡和红线失败用例。
- 修复缺陷必须先补能复现问题的回归测试，再改实现；无法先补测试时，交付说明必须解释原因和替代验证。

### 7.2 测试分层

| 层级 | 默认落地方式 | 适用场景 |
|------|--------------|----------|
| L1 契约/纯单元测试 | `core/src/test` 或 `tests/src/test`，不启动 Spring。 | DSL、枚举、route、posting、摘要、金额、状态机、helper 和纯业务规则。 |
| L2 应用服务测试 | 直接构造 service，使用 fake/stub/mock 协作者。 | 幂等、生命周期、编排、失败路径、余额控制、授权和逆向交易。 |
| L3 H2/集成测试 | `tests` 模块，H2 MySQL Mode 或最小 Spring 上下文。 | Mapper、DDL、唯一约束、本地事务、投影持久化和 schema 兼容。 |
| L4 架构/红线测试 | ArchUnit、Maven 依赖检查、边界测试或显式失败测试。 | 模块依赖、禁止入账主体、只读投影、不可绕过账本和外部账户入账。 |

- `@SpringBootTest` 是最后选项，不是默认选项；能直接构造对象就不启动容器。
- 数据库测试必须说明为什么需要数据库行为；纯业务规则不得因为方便注入而启动 Spring。
- 高风险 SQL、锁、索引、数据库方言或性能问题，H2 无法证明时应补 Testcontainers 或专门集成测试方案。

### 7.3 测试命名与结构

- 新增测试方法统一以 `test` 开头，推荐 `test<UseCase>Should<Expected>` 风格，例如 `testFreezeAvailableFundsShouldCreateFrozenOrderOnly`。
- 测试类以被测能力或业务边界命名，避免把无关能力塞进同一个巨型测试类；巨型测试类应按交易生命周期、冻结生命周期、余额断言、route replay、边界测试等职责分批拆分。
- 关键资金测试的方法上方必须写方法级注释，至少包含“场景、输入、输出、预期、红线”；不要把场景说明塞进方法体内部。
- 测试方法内部优先使用 Given / When / Then 或 Arrange / Act / Assert 结构；一个测试聚焦一个业务行为，可以有多个必要断言，但断言必须服务同一场景。
- 测试数据必须有业务语义，避免 `foo`、`test1`、无含义金额和无含义流水；金额、币种、主体、状态、时间和流水都应能解释测试意图。

示例：

```java
/**
 * 场景：用户提现前冻结可用余额。
 * 输入：用户资金账户 AVAILABLE=1000，本次冻结 800，冻结原因为提现风控。
 * 输出：生成冻结单，账本余额从 AVAILABLE 迁移到 FROZEN。
 * 预期：不创建 FundsTransaction，posting plan 平衡，重复请求不重复冻结。
 * 红线：冻结不得表达消费或跨主体资金转移。
 */
@Test
void testFreezeAvailableFundsShouldCreateFrozenOrderOnly() {
    // Given / When / Then
}
```

### 7.4 资金测试断言要求

- 有资金变化的测试不得只断言交易状态、route、entry 数量或“不报错”；必须同时断言相关主体的账本余额桶、posting plan 平衡、ledger transaction 可追溯和幂等行为。
- 资金变化测试优先复用 `tests/src/test/java/com/capte/funds/support/FundsBalanceAssertionSupport.java` 或等价领域断言，不得在每个测试里手写不可复用的余额推导。
- 业务组合测试必须每一步都断言余额变化，不能只断言最终余额。典型组合包括：充值 -> 付款 -> 退款；充值 -> 冻结 -> 提现；A 充值 -> 转给 B -> B 付款 -> B 提现。
- 冻结/解冻测试必须证明冻结只做同主体 `AVAILABLE <-> FROZEN` 控制，不表达消费、扣划或跨主体价值转移；提现出款、追偿、退款、调账等后续动作必须作为独立资金事实测试。
- 授权测试必须区分授权批准、授权拒绝、授权撤销、授权结算、授权链退款和争议拒付；授权拒绝不得生成 route/entry，不得写入 `chargebackAmount`。
- 清结算、对账、归档和报表测试必须证明来源事实、批次、规则版本、审计、重跑幂等和只读投影边界；对账差异、交易视图或报表不得直接修改账本事实。

### 7.5 测试清单与验证命令

- `docs/v5/系分设计/API 契约测试与编码实施计划.md` 的“全量测试清单”是当前测试 backlog 的权威入口；新增产品用例、DSL 契约或系分能力时必须同步更新该清单。
- 每个实现任务必须在交付说明中列出：覆盖的测试清单项、执行的测试类、验证命令、是否通过、未覆盖风险。
- 变更范围对应的推荐验证命令：

```bash
mvn -pl core -am test -Dtest=FundsInstructionSpecContractTests,RouteDslContractTests,TransactionServiceAbilityDslJsonContractTests
mvn -pl tests -am test -Dtest=DefaultLedgerPostingAssemblerTests,DefaultLedgerTransactionPostingServiceImplTests,LedgerBalanceProjectionServiceImplTests
mvn -pl tests -am test -Dtest=FundsTransactionCommandServiceImplTests,DefaultRoutedFundsInstructionOrchestratorTests,DefaultFundsInstructionLifecycleSaverTests
mvn -pl tests -am test -Dtest=FundsFrozenOrderServiceImplTests,DefaultFundsFrozenOrderLifecycleSaverTests,BalanceControlFundsInstructionRouteResolverTests
mvn -pl tests -am test -Dtest=FundsTransactionLedgerBalanceAssertionsTests,FundsTransactionBusinessFlowIntegrationTests,FundsTransactionOrchestrationFlowTests
mvn -pl tests -am test -Dtest=LedgerLayerBoundaryTests,RouteLayerBoundaryTests,WalletLayerBoundaryTests
```

## 8. AI Agent 工作原则

- 默认情况下，凡涉及编码、编码设计、架构设计、技术方案、代码评审、重构评估、测试设计和工程治理，必须使用 `资深架构师` Skill 的原则与工作方式执行。
- 默认情况下，凡涉及支付产品设计、资金系统产品方案、交易场景、账户/账务/清结算/对账/争议/风控/合规口径设计，必须使用 `支付产品与资金系统专家` Skill 的原则与工作方式执行。
- 涉及真实资金、监管、跨境、外汇、客户资金、备付金、风控或合规口径时，只输出产品和系统设计分析，不替代法律、税务、会计或合规最终结论。
- 遇到架构边界、数据模型、安全、兼容性、生产行为或不可逆操作，先给不超过 3 个选项并等待用户确认。

## 9. Git 与提交约规

- 提交只包含任务直接相关变更。
- 不提交无关格式化、IDE 配置、临时文件、密钥或个人凭据。
- 提交前必须通过编译、相关测试和规约扫描，或说明无法执行原因。
- 提交信息格式：

```text
<type>(<scope>): <subject>

<body>

Assisted-by: [Model Name] via [Tool Name]
```

`type` 可选：`feat`、`fix`、`docs`、`style`、`refactor`、`test`、`chore`。
- 提交信息优先使用中文描述，保留 `type(scope): subject` 结构和 `Assisted-by` 标识。

## 10. 一句话总结

资金事实先于页面流程；核心契约与实现分离；只做最小修改，只交付可验证结果。
