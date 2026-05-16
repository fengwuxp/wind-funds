# AGENTS.md

> 本文件是 `wind-funds` 的常驻默认层，只记录每个会话都应知道的项目级约束：仓库定位、模块边界、资金域红线、规格入口和验证命令。
> Skill 是专题流程层，不替代本文件的最低约束；具体使用场景见第 8 节 Skill 路由。

## 1. 项目定位

`wind-funds` 是支付资金底座项目，承载账务 DSL、资金账户、交易路由、钱包交易、账本分录、清结算与对账规格等能力。

核心目标：

- **资金语义独立**：支付、账务、账户、路由、清结算、对账等能力在本仓库演进。
- **契约稳定**：跨模块调用优先依赖 `*-face` 和 `core`，不得暴露 Entity、Mapper 或内部实现类。
- **核心先行**：`core` 承载资金 DSL、枚举、值对象和端口契约，不依赖 DAL、Web、消息或具体实现。
- **实现内聚**：`*-impl` 承载 DAL、服务实现、转换器和领域规则，避免规则扩散到测试、工具或外部适配层。
- **可验证交付**：代码变更必须说明编译、相关测试和规约扫描结果；无法执行时说明环境或依赖限制。

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
| `transaction/transaction-face` | 资金交易、资金账户、预算组、支付工具、路由生命周期等契约。 |
| `transaction/transaction-impl` | 资金交易编排、路由解析、快照、资金账户、交易记录和生命周期保存实现。 |
| `wallet/wallet-face` | 钱包交易、授权、冻结、解冻、退款、转账、充值、提现等产品契约。 |
| `wallet/wallet-impl` | 钱包产品门面实现，把钱包请求转换为资金指令并交给交易编排。 |
| `tests` | 资金域测试、契约测试、架构边界测试和 H2 表结构测试资源。 |
| `dependencies` | 依赖聚合/BOM，只管理依赖，不写业务代码。 |

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

资金域边界：

- `*-face` 不依赖 `*-impl`；生产模块不得依赖 `tests`。
- `wallet` 作为产品门面，只编排资金指令，不直接写交易事实或账本事实。
- `route` 只解析资金路径，不直接写交易事实或账本事实。
- `ledger` 只维护账本事实和账本投影，不反向持有业务交易生命周期状态。

## 4. 规格入口

| 路径 | 用途 |
|------|------|
| `docs/` | 支付资金底座产品设计、DSL 设计、系分设计和过程材料。 |
| `docs/v5/` | 当前目标态、产品 TDD、DSL 契约、API 契约测试、Harness 门禁和 P0 编码任务的主要入口。 |
| `openspec/` | 支付资金底座 OpenSpec 项目上下文、能力规格和变更提案。 |
| `tests/src/test/resources/jdbc-schema.sql` | 资金账户、交易、账本相关建表语句，统一作为 H2/MySQL Mode 测试表结构来源。 |

进入编码前，凡涉及支付资金底座目标态、DSL、API、清结算、对账、归档、Harness 门禁或能力规格，必须同步阅读相关 `docs/v5` 与 `openspec` 内容，并在交付说明中列出本次覆盖的测试清单项或说明不适用原因。

## 5. 项目编码入口

通用编码规约按第 8 节 Skill 路由执行。本节不重复展开编码规则。

## 6. 资金测试红线

通用测试设计按第 8 节 Skill 路由执行。本项目只保留资金域不可丢失的断言红线：

- 涉及金额、状态流转、幂等、重放、账务平衡、余额约束、冻结/解冻、清结算和对账差错的变更必须补测试。
- 有资金变化的测试不得只断言交易状态、route、entry 数量或“不报错”；必须同时断言相关主体的账本余额桶、posting plan 平衡、ledger transaction 可追溯和幂等行为，优先复用 `tests/src/test/java/com/capte/funds/support/FundsBalanceAssertionSupport.java` 或等价领域断言。
- 业务组合测试必须每一步都断言余额变化，不能只断言最终余额。
- 冻结/解冻测试必须证明冻结只做同主体 `AVAILABLE <-> FROZEN` 控制，不表达消费、扣划或跨主体价值转移。
- 授权拒绝不得生成 route/entry，不得写入 `chargebackAmount`。
- 清结算、对账、归档和报表测试必须证明来源事实、批次、规则版本、审计、重跑幂等和只读投影边界。

测试 backlog 权威入口：`docs/v5/系分设计/API 契约测试与编码实施计划.md` 的“全量测试清单”。

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
just test-core
just test-ledger
just test-transaction
just test-balance-control
just test-business-flow
just test-boundary
just test-one <TestClass> [module]
```

提交前优先执行：

```bash
just pmd
```

如 `pmd:check` 因私有仓库、snapshot、本地 Maven 缓存或依赖解析失败，应在交付说明中按环境依赖问题记录，不得等同于代码规约违规。

仅修改文档、产品设计、系统分析设计、方案讨论、需求澄清、流程图或说明性材料时，不要求运行编译；交付时说明未运行编译的原因。

## 8. Skill 路由

| 场景 | 必用 Skill | 产物边界 |
|------|------------|----------|
| 编码、编码设计、架构设计、系统分析、技术方案、代码评审、重构评估、测试设计、Git 提交建议、工程治理 | `资深架构师` | 工程边界、模块设计、接口契约、代码修改、测试策略、验证命令、Review 结论和交付说明。 |
| 支付产品设计、资金系统产品方案、交易场景、账户/账务模型、资金流、清结算、对账、争议拒付、风控、合规口径 | `支付产品与资金系统专家` | 角色、四流、账户/账务模型、状态机、账务矩阵、清结算/对账规则、异常路径、产品验收和风险清单。 |
| 支付资金能力进入编码或系分落地 | 两者都用 | 先由 `支付产品与资金系统专家` 定资金语义、业务不变量和合规边界，再由 `资深架构师` 落模块、接口、测试、验证和代码实现。 |

- 涉及真实资金、监管、跨境、外汇、客户资金、备付金、风控或合规口径时，只输出产品和系统设计分析，不替代法律、税务、会计或合规最终结论。
- 遇到架构边界、数据模型、安全、兼容性、生产行为或不可逆操作，先给不超过 3 个选项并等待用户确认。

## 9. 交付说明

交付说明的通用结构、Git 提交建议和验证结论表达按第 8 节 Skill 路由执行。本项目交付时必须额外说明：

- 修改了哪些文件和模块。
- 覆盖了哪些测试清单项；若不适用，说明原因。
- 执行了哪些验证命令。
- 验证是否通过。
- 未能执行或未通过的原因。
