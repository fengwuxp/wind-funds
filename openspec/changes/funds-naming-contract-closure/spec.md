# 资金契约命名闭合规格

## Change Metadata

| 字段 | 值 |
| --- | --- |
| Change ID | `funds-naming-contract-closure` |
| 状态 | `VERIFIED` |
| 日期 | `2026-08-26` |
| 决策 | Human Owner 明确要求不做任何兼容，直接硬切 |
| Git 策略 | 未授权 stage、commit、push 或 PR |

## 1. 重构准入

本变更需要独立重构设计：局部改一个 getter 会同时破坏 RouteSnapshot JSON、投影 payload、出款摘要和 Public API。行为变化只限命名和序列化键；非目标是不改变金额、状态值、状态机、路由、账本、余额和业务准入。

## 2. 当前问题与证据

上一轮物理列和 Java 生命周期已统一为 `state`，但稳定契约仍有同义名：

- `PaymentInstrumentRefSpec.status` 实际承载支付工具 `state`，`instrumentId` 实际承载 `instrumentSn`。
- RouteSnapshot JSON、交易投影解释和出款摘要 Map 仍写入无修饰 `status`。
- `ExternalRuleVerificationStatus` 和 `ExternalRuleVerificationEvidenceDTO.status` 表达一次核验结论，不是生命周期。
- `LedgerBalanceBucket.accountCode` 实际类型和来源都是 `LedgerSubjectCode`。
- 生产日志和异常上下文存在 31 处 `status = {}`，多数实际输出 `state`，一处输出 `admissionResult`。
- 架构测试通过源码中文文案匹配证明日志存在，不能证明真实用例路径。
- 本轮已触及的生产文件仍有不可核验的 `@author Codex` 和非标准 `@date`。

当前结构的关键调用链是：支付工具准入 -> PaymentInstrumentRef -> RouteSnapshotJsonSupport -> 持久化 route snapshot -> replay/projection explanation；出款回单 -> normalized digest -> PayoutReceipt 幂等回读。缺陷证据见上一轮 CR 的文件和行号。

## 3. 目标结构与行为边界

行为不变量和公共契约不变量只保留资金语义与枚举值；替换范围是下表 typed API、JSON/Map key 和日志标签；删除范围是旧 getter、旧 enum 和旧 key；保留范围是业务流程、数据库事实和 Mapper。

### 3.1 目标命名

| 当前名 | 目标名 |
| --- | --- |
| `PaymentInstrumentRefSpec.instrumentId` | `instrumentSn` |
| `PaymentInstrumentRefSpec.status` | `state` |
| `transactionSummary.status` | `transactionSummary.state` |
| `ExternalRuleVerificationStatus` | `ExternalRuleVerificationResult` |
| `ExternalRuleVerificationEvidenceDTO.status` | `verificationResult` |
| `PayoutPreflightResultDTO.externalRuleVerificationStatus` | `externalRuleVerificationResult` |
| 出款回单摘要 `status` | `state` |
| 外部规则证据摘要 `status` | `verificationResult` |
| `LedgerBalanceBucket.accountCode` | `ledgerSubjectCode` |
| 生命周期日志标签 `status` | `state` |
| 可清分准入日志标签 `status` | `admissionResult` |

## 4. Breaking Contract 与迁移规则

本变更是明确硬切，不提供兼容层：

1. RouteSnapshot 只写、只读 `instrumentSn` 和 `state`；旧 `instrumentId/status` 不再解析。
2. 投影解释只输出 `state`，不同时输出 `status`。
3. 出款回单和提交摘要只按新键计算；不比较旧摘要。
4. 删除旧 Java 类型、getter、builder 属性和 JSON 字段，不保留 deprecated alias。
5. `LedgerBalanceBucket` 只提供 `ledgerSubjectCode()`。
6. 不新增数据库迁移、JSON fallback、双读、双写或摘要版本兼容。

发布前置条件：若真实宿主仍有需要回放的旧 RouteSnapshot、已保存的旧出款回单摘要或旧客户端，发布必须停止，由宿主 Owner 先完成离线数据和客户端迁移。本仓库不实现该迁移。

迁移规则：主写方只写新键；禁止双写、代码回填、影子读和灰度切流。新旧契约共存期为零；下线条件是所有宿主完成离线迁移并停止旧客户端。回滚只能整体恢复旧应用与旧数据快照，不允许新旧代码交叉连接。

## 5. 行为边界

- 金额、状态值、状态机、路由、账本、余额、幂等唯一键和业务准入规则不变。
- 只改变 Java accessor、JSON/Map key、摘要输入键、日志字段标签和文档名相。
- 自定义 Mapper、MyBatis-Flex 使用方式、Entity 和物理列不变。
- 日志测试删除源码文案匹配，不新增日志框架或事件系统。
- Javadoc metadata 只清理本轮已触及文件中的不可信 `Codex/@date`，不猜测作者。

## 6. MIG 切片

| MIG 切片 | 前置条件 | 写入范围 | 验证证据 | 暂停与回退 |
| --- | --- | --- | --- | --- |
| NC-RED | 当前源码和测试清单稳定 | 仅目标契约测试 | 旧名存在、新名缺失的精准 RED | 额外 failure/error 即暂停 |
| NC-TYPED | RED 已确认 | typed API、enum、调用方、baseline | compile + focused Green | 需要 alias 即回退 |
| NC-STABLE-MAP | typed Green | RouteSnapshot、projection、payout digest | replay/projection/payout 回归 | 需要旧键读取即暂停 |
| NC-CONVENTION | 行为 Green | 日志、守卫、metadata、文档 | 静态扫描 + diff-check | 范围漂移即回退目标 hunk |
| NC-CLOSURE | 全部 Green | 无新增生产源码 | PMD、CAD、Review | 任一门禁失败不准出 |

## 7. 验收标准

| ID | 条件 |
| --- | --- |
| NC-001 | 生产代码不再出现 `PaymentInstrumentRefSpec#getStatus/getInstrumentId`。 |
| NC-002 | 新 RouteSnapshot 和投影 payload 只包含 `instrumentSn/state`，不包含 `instrumentId/status`。 |
| NC-003 | 旧 RouteSnapshot 字段不会被兼容读取。 |
| NC-004 | 外部规则核验统一使用 `Result/verificationResult`。 |
| NC-005 | 出款摘要使用 `state/verificationResult`，相同新请求保持幂等。 |
| NC-006 | 账本余额桶统一使用 `ledgerSubjectCode`。 |
| NC-007 | 生命周期日志不再使用无修饰 `status`。 |
| NC-008 | 删除日志源码文案匹配守卫；新增/改动 Swagger 和注释保持中文。 |
| NC-009 | 编译、聚焦测试、PMD、CAD 和 diff-check 通过。 |

## 8. 验证

- 特征测试：RouteSnapshot 新键、旧键拒绝、projection payload 新键。
- 契约测试：Public getter、enum、record component 和 JSON 字段。
- 回归测试：transaction、reconciliation、business-flow、boundary。
- 数据校验：本仓不执行宿主数据迁移；发布前由宿主校验旧快照和旧摘要数量为零。
- 监控与告警：宿主观察 route replay 失败、回单摘要冲突和未知 JSON 字段。

## 9. Engineering Handoff

- 第一实施切片：`NC-RED`。
- 执行 owner：当前主 Agent。
- 验证 owner：聚焦测试、项目门禁和后续独立 Review。
- 写入范围：本规格和计划列出的精确文件/hunk。
- 恢复入口：最后一个 Green MIG 与当前 dirty-tree 双读结果。

## 10. 停止条件

- 修复需要读取旧键、保留旧 accessor、双写或双摘要时停止。
- 发现宿主仍依赖旧快照或旧摘要时停止发布，但不回退本轮源码目标。
- 出现资金事实、账本、余额或状态值变化时停止。
- 目标文件并行漂移且无法按 hunk 合并时停止。

## 11. 实施证据

- RED：聚焦测试首次因 `ExternalRuleVerificationResult` 和目标 accessor 尚不存在而编译失败，符合预期。
- Green：`just compile` 通过；聚焦切片 99 个测试全部通过。
- 回归：`just test-transaction` 186 个测试全部通过；`just test-reconciliation` 247 个测试全部通过；`just test-core`、`just test-business-flow`、`just test-boundary` 均通过。
- 规约：旧生产符号、旧 JSON 写键和 `status = {}` 日志标签扫描结果为零；`just pmd`、`git diff --check` 通过。
- 全量：`just verify-cad` 通过，1194 个测试中 0 失败、0 错误、1 个环境条件跳过；Core API baseline 为 94 stable、4 experimental、4 internal public top-level types。
- Git：未执行 stage、commit、push 或 PR。
