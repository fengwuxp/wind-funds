# OpenSpec Project Context

## 一、定位

本目录是支付资金底座进入后续开发前的规格基线目录。它不保存历史版本演进，不承载历史过程内容，也不替代产品设计、DSL 设计、系分设计和 TDD 设计。

Gbrain 与 OpenSpec / Superpowers / Harness 在本项目中的定位：

1. Gbrain 做记忆和检索：查询和沉淀本项目的设计决策、规格基线、Execution Grant、CR 结论、验证摘要和交付复盘。
2. OpenSpec 定目标：把当前最终版设计转成可开发、可测试、可评审的能力规格。
3. Superpowers 保纪律：以测试驱动设计、Review、Refactor、金融红线和验证门禁约束后续编码。
4. Harness 管协作：按模块拆分开发批次、写入范围、只读范围、验证命令和人工确认点。

Gbrain 是上下文层，不是权威规格层。Gbrain 命中的历史决策必须回到 `docs/`、`openspec/`、Harness Plan 和 Git 提交点校验；Gbrain 未命中时，不阻塞编码准入，但必须在交付说明中记录未命中，并以当前权威规格和用户确认作为执行依据。

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
| Harness | `openspec/changes/tdd-baseline-reset/tasks.md` | 按 TDD 拆分后续落地批次和验证门禁。 |
| Gbrain | `.gbrain-source` = `wind-funds` | 跨轮检索项目决策、CR 结论、Execution Grant 和验证摘要；不作为规格或验收的唯一依据。 |

## 四、后续开发 Definition of Ready

任一编码批次开始前必须满足：

1. 需求能映射到产品验收 ID、DSL 契约用例、系分模块和 TDD 用例。
2. 明确本批次写入范围、只读范围、非目标和禁止事项。
3. 明确先写或先恢复哪些测试，且测试名称和断言来自 TDD 设计。
4. 明确是否涉及公共契约、枚举、表结构、状态机、金额、权限、审计或生产行为。
5. 明确验证命令；无法执行时必须说明环境、依赖或私有仓库限制。
6. 涉及资金红线、表结构、外部协议、清结算对象或归档重放时设置人工确认点。
7. 当前设计基线、OpenSpec 基线、Harness Plan 和旧测试清理结果已作为独立检查点冻结；未冻结前不得把后续编码实现混入同一轮交付。
8. 若本批次需要修改公共契约、枚举、请求模型、状态机或表结构，Execution Grant 必须显式写明“允许修改公共契约/表结构/新增模块”的取值和范围。
9. 若本批次触碰生产行为、外部结果回调、并发写入、余额锁定、清结算批次、归档重放或报表口径，Execution Grant 必须补齐容量假设、并发和锁策略、观测告警、回滚或补偿方案。
10. 编码准入或重大设计 CR 前，应先查询 Gbrain 中的本项目历史决策；若未命中，以当前 Source of Truth 和用户确认作为执行依据，并在交付说明中记录。

## 五、后续开发 Definition of Done

任一编码批次完成前必须满足：

1. 代码、测试、DSL 契约和系分设计一致，若发现设计错漏，先回补设计再继续。
2. 覆盖本批次对应 TDD 用例和必须失败红线。
3. 资金变化测试同时断言状态、route snapshot、posting plan、ledger entry、余额投影和幂等。
4. 不恢复历史测试源码或已废弃断言，不以历史测试通过替代当前最终版 TDD 断言。
5. 不引入未确认概念、无主依赖、真实外部调用、生产配置或敏感数据。
6. 交付说明列出覆盖用例、验证命令、验证结果、未覆盖项和残余风险。
7. 涉及生产行为的批次必须说明并发边界、幂等和锁保护、告警指标、降级/回滚或补偿路径。

## 六、语言与协作规则

OpenSpec、Superpowers、Harness 和 Gbrain 协作摘要默认使用简体中文。代码标识符、协议字段、枚举、命令、包名、类名和第三方产品名保持英文原文。

本目录只记录规格与协作基线。进入 CAD Mode 仍需要单独确认 Execution Grant、Git 策略、人工确认点和停止条件。
