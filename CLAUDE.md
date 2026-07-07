# CLAUDE.md

本文件是 `wind-funds` 面向 Claude 的项目入口，独立承载 Claude 运行时需要遵守的项目约束。

## 1. 项目身份

- 项目类型：Java 21 / Spring Boot 3.x / Maven 多模块 Wind 资金底座。
- 核心业务：资金 DSL、账本、钱包、资金交易、交易路由、清结算、对账和治理。
- 默认命令入口：根目录 `Justfile`。
- 默认语言：中文沟通、评审、规划、交付说明和 Git commit message；代码标识符、协议字段、API、命令、错误码、标准 commit type 和英文专有名词按项目既有英文保留。

核心原则：资金语义独立、契约稳定、核心先行、实现内聚、可验证交付。构建依赖会访问私有 Maven 仓库；验证失败时先区分代码问题和仓库、网络、凭据或本地缓存问题。

## 2. 先读什么

修改代码或设计文档前，先读当前任务相关模块源码和测试。

涉及支付资金底座目标态、DSL、API、清结算、对账、归档、TDD 门禁或能力规格时，同步阅读 `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计`。涉及接入契约、生产使用说明或能力差距复核时，再读 `docs/用户接入指南`。

## 3. 模块边界

模块职责：`core` 放资金 DSL、核心契约、枚举、值对象和端口；各 `*-face` 放对外契约；各 `*-impl` 放实现、DAL、Mapper、MapStruct 和内部规则；`tests` 放资金域测试、契约测试、架构边界测试和 H2 表结构；`dependencies` 只做依赖聚合 / BOM。

强制依赖方向：`*-face -> core / capte-domain-core`；`transaction-impl -> transaction-face / wallet-face / core / infrastructure`；`wallet-impl -> wallet-face / ledger-face / core / infrastructure`；`ledger-impl -> ledger-face / core / infrastructure`；`reconciliation-impl -> reconciliation-face / transaction-face / ledger-face / core / infrastructure`；`governance-impl -> governance-face / transaction-face / ledger-face / reconciliation-face / core / infrastructure`；`tests -> impl / face / core`。

模块红线：

- `*-face` 不依赖 `*-impl`；生产模块不得依赖 `tests`。
- `wallet-impl` 不依赖 `transaction-face`，不创建资金交易事实。
- `transaction-impl` 可依赖 `wallet-face` 消费钱包准入、支付工具、资金责任和支出控制契约；不得依赖 `wallet-impl`、钱包 DAL、Mapper 或钱包内部实现包。
- `reconciliation-impl` 和 `governance-impl` 只能通过 `*-face`、core port 或只读证据引用消费主链事实；禁止依赖其他模块 `*-impl`、DAL、Mapper 或反写交易、账本、钱包事实。
- `route` 只解析资金路径，不直接写交易事实或账本事实；`ledger` 只维护账本事实和账本投影，不反向持有业务交易生命周期状态。
- 包名和源码路径统一使用 `com.wind.funds` / `com/wind/funds`；不得恢复历史 Capte funds 包根或旧 Wind integration funds 包根。`com.capte.domain` 是外部领域依赖边界，可按模块依赖约束保留。

## 4. Wind 项目约规

本项目遵守 Wind 项目编码约规。涉及 face/impl、模型归位、Entity 不外露、基础服务、ServiceImpl、MyBatis Flex、币种枚举、TDD/CR 或代码生成后审查时，先按 Wind 规则判断，再闭环源码设计、测试和验证。

项目级 Wind 红线：

- Public API、ApplicationService、Adapter、事件和跨模块契约只能暴露 DTO、Request、Query、Command、枚举或值对象；不得暴露 Entity、Mapper、Repository、MyBatis Page、QueryWrapper 或实现类。
- 公共契约放在 `*-face` 或 `core`；实现、DAL、Mapper、MapStruct 和内部规则放在 `*-impl`。
- 优先复用已有服务和 helper；不新增一行透传、浅服务、伪抽象、Mapper 包装、Fake/Mock 业务实现或内存版业务 Service。
- MyBatis Flex 使用项目既有模式和 `XxxRefs`；不要新增 `LambdaQueryWrapper` 或裸字符串字段名。
- 模型转换使用 MapStruct converter；converter 不做业务决策、数据库读取、远程调用、权限判断或审计。
- 空值契约遵循 JSpecify：已声明非空的值不写重复防御式空判断；只有 `@Nullable`、外部输入、反序列化边界或持久化读取等不可信来源才做显式空处理。
- 业务事件、审计展示和可回放消息优先使用稳定 `eventKey + params`，不得把中文文案或可变翻译作为业务判断依据。

## 5. 资金测试红线

涉及金额、状态流转、幂等、重放、账务平衡、余额约束、冻结 / 解冻、清结算和对账差错的变更必须补测试。资金变化测试必须同时断言相关主体账本余额桶、posting plan 平衡、ledger transaction 可追溯和幂等行为。

- Spring 服务层流程测试优先使用真实内部 Spring Bean 和 H2 表结构；可继承 `AbstractFundsServiceTest`。
- Mock / Fake / Recording 只用于外部系统、不可控环境或明确端口边界。
- 业务组合测试每一步都断言余额变化，不能只断言最终余额。
- 冻结 / 解冻只做同主体 `AVAILABLE <-> FROZEN` 控制，不表达消费、扣划或跨主体价值转移。
- 授权拒绝不得生成 route、posting、LedgerEntry，不得写入 `declinedAmount`，不得被当作 chargeback 事件。
- 清结算、对账、归档和报表测试必须证明来源事实、批次、规则版本、审计、重跑幂等和只读投影边界。

测试 backlog 权威入口：`docs/TDD设计/支付资金底座测试驱动设计.md`。

## 6. 业务日志与审计

`ledger`、`wallet`、`transaction` 及清结算 / 治理实现的关键用例边界必须输出可追溯业务日志，覆盖准入 / 拒绝、交易或账本状态变化、冻结 / 解冻、入账 / 退款 / 冲正、幂等复用、重试 / 补偿和对账差错处理。

- 优先使用 `@Slf4j` 和参数化日志；禁止 `System.out`、`printStackTrace`、吞异常或只打印异常 message。
- 异常日志保留 cause，并带上稳定业务标识。
- 日志字段只保留最小可定位上下文：`tenantId`、业务场景 / 业务单号、`transactionSn`、`ledgerTransactionSn`、账户或主体脱敏标识、状态、金额币种、规则 / 版本 / 幂等摘要和 traceId。
- 不得输出完整 Request/Response、Entity、SQL、`contextVariables`、PAN、CVV、token、密钥、证件号、手机号、外部账号等敏感信息。
- 日志不替代交易事实、账本事实、审计证据、对账证据或测试断言；涉及资金事实、幂等、补偿、对账和安全边界的日志改动必须随相关切片验证。

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

提交前优先执行 `just pmd`；阶段收口、完整基线复核或高风险提交前执行 `just verify-cad`。仅修改文档、产品设计、系统分析设计、方案讨论、需求澄清、流程图或说明性材料时，不要求运行编译；至少执行 `git diff --check`。

## 8. Claude 工作方式

- 改动要小，并且能直接回到用户请求。
- 优先删除过时过程产物，而不是新增计划文件。
- 不主动创建任务账本、进度追踪、临时基线或重复流程文档，除非用户明确要求。
- 不把 CAD、Goal、Harness、Execution Grant 等内部流程词当作面向用户的完成状态，除非任务明确讨论流程内部。
- 做代码评审时，先列问题，按严重级别排序，并给出文件和行号。
- 做实现交付时，说明改了哪些文件、覆盖了哪些测试、执行了哪些验证、是否通过和残余风险。
- Git commit message 默认用中文；需要时可保留 `feat:`、`fix:`、`test:`、`docs:`、`chore:` 等标准前缀。

## 9. Skill 路由

| 场景 | 必用 Skill | 边界 |
| --- | --- | --- |
| 端到端角色协作、Goal / Loop / GSD / CAD、owner、交接物、授权、验证、停止条件 | `ai-native-engineering-workflow` | 只做流程准入和交接闭环，不替代产品、架构、代码、测试、Git 授权或上线审批。 |
| 编码、架构设计、系统分析、技术方案、代码评审、重构评估、测试设计、工程治理 | `资深架构师` | 负责工程边界、接口契约、代码修改、测试策略、验证命令和 Review 结论。 |
| Wind 约规判断、face/impl、模型归位、Entity 不外露、MyBatis Flex、ServiceImpl、TDD/CR 约规 | `wind-project-coding-conventions`，源码执行配合 `资深架构师` | 只判断规则和最小整改建议；真实源码修改和验证由架构师闭环。 |
| 产品架构、PRD、业务建模、能力地图、业务流程、状态机、规则矩阵、产品验收、支付资金产品方案 | `产品架构专家` | 输出产品目标、角色、对象、流程、规则、异常路径、验收和风险。 |
| 结构化 Java Service 脚手架生成 | `java-service-code-generator` | 必须有 DDL / schema / Java 类 / 字段表格；不从纯自然语言生成生产代码。 |
