# 资金契约命名闭合 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Current instructions prohibit new subagent delegation.

**Goal:** 以无兼容硬切方式闭合稳定 JSON、摘要、公共模型、日志和 Javadoc 中遗留的命名偏差。

**Architecture:** 先用现有真实契约测试把目标新字段和旧字段缺失写成 RED，再一次性切换 typed API、RouteSnapshot、projection payload、payout digest 和 LedgerBalanceBucket。日志与 metadata 作为无行为变化的机械收口放在 Green 之后。

**Tech Stack:** Java 21、Spring Boot 3.x、MapStruct、MyBatis-Flex、Jackson、JUnit 5、AssertJ、Maven、Justfile。

**Task ID:** `ENG-NAMING-CONTRACT-CLOSURE-001`。

**任务 / 目标 / 阶段切片:** `NC-RED -> NC-TYPED -> NC-STABLE-MAP -> NC-CONVENTION -> NC-CLOSURE`，严格串行。

**Owner:** 当前主 Agent 是执行者；Human Owner 已批准无兼容硬切。

**写入范围 / 写入文件:** 仅限 File Map 和 31 个已确认消息标签、21 个已确认 metadata hunk。

**只读范围 / 只读参考:** 根 `AGENTS.md`、既有 state naming OpenSpec、调用方、测试、生成 baseline 和权威文档。

**验收场景 / 完成条件 / 验证命令:** 新键唯一、旧键拒绝、资金行为不变；按各 Task 命令完成编译、测试和 Review。

**TDD / Review / 编码红线 / AI 产物复核:** 先红后绿；不新增 alias、fallback、双写、兼容层或幻觉 API；每个 Green 后回读 diff。

**Execution Grant:** 用户明确授权范围为上一轮 CR 的命名修复；Git 策略为 `summary_only`，禁止事项包括 stage、commit、push、PR、联网、生产数据和部署；撤销方式为用户暂停。

**人工确认 / 停止条件:** 若需要兼容旧键、目标 hunk 并行冲突、资金行为变化或真实宿主阻断信息进入源码实现，则中断。

**交接 / 恢复入口 / 残余风险:** 每个 Task 回写 checkbox 和验证证据；恢复入口是最后完成 Task 与当前 status/diff；残余风险是宿主旧数据必须离线迁移。

---

## File Map

### Contract And Runtime

- `core/src/main/java/com/wind/funds/route/ref/PaymentInstrumentRefSpec.java`
- `transaction/impl/src/main/java/com/wind/funds/route/model/ImmutablePaymentInstrumentRefSpec.java`
- `transaction/impl/src/main/java/com/wind/funds/transaction/services/impl/RouteSnapshotJsonSupport.java`
- `transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/impl/PaymentInstrumentAuthorizationProcessor.java`
- `transaction/impl/src/main/java/com/wind/funds/transaction/application/instrument/impl/PaymentInstrumentTransactionApplicationServiceImpl.java`
- `transaction/face/src/main/java/com/wind/funds/transaction/projection/FundsTransactionProjectionExplanationSource.java`
- `reconciliation/face/src/main/java/com/wind/funds/reconciliation/enums/ExternalRuleVerificationStatus.java`（删除）
- `reconciliation/face/src/main/java/com/wind/funds/reconciliation/enums/ExternalRuleVerificationResult.java`（新增）
- `reconciliation/face/src/main/java/com/wind/funds/reconciliation/model/dto/ExternalRuleVerificationEvidenceDTO.java`
- `reconciliation/face/src/main/java/com/wind/funds/reconciliation/model/dto/PayoutPreflightResultDTO.java`
- `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/services/impl/PayoutPreflightServiceImpl.java`
- `reconciliation/impl/src/main/java/com/wind/funds/reconciliation/application/payout/impl/PayoutOrderApplicationServiceImpl.java`
- `core/src/main/java/com/wind/funds/ledger/LedgerBalanceBucket.java`
- `core/src/main/java/com/wind/funds/ledger/LedgerBalanceView.java`
- `wallet/impl/src/main/java/com/wind/funds/wallet/services/impl/DefaultFundsAccountQueryServiceImpl.java`
- `core/api-baseline/stable-api.txt`

### Tests

- `tests/src/test/java/com/wind/funds/transaction/services/impl/RouteSnapshotJsonSupportTests.java`
- `tests/src/test/java/com/wind/funds/route/DefaultRouteReplayServiceTests.java`
- `tests/src/test/java/com/wind/funds/dsl/PaymentInstrumentRouteDslContractTests.java`
- `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsDirectTransactionFlowTests.java`
- `tests/src/test/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentTransactionApplicationServiceTests.java`
- `tests/src/test/java/com/wind/funds/wallet/application/instrument/PaymentInstrumentTransactionAuthorizationTests.java`
- `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsTransactionProjectionBusinessScenarioTests.java`
- `tests/src/test/java/com/wind/funds/reconciliation/services/impl/PayoutPreflightServiceTests.java`
- `tests/src/test/java/com/wind/funds/transaction/application/flow/PayoutOrderApplicationServiceTests.java`
- `tests/src/test/java/com/wind/funds/support/FundsBalanceAssertionSupportTests.java`
- `tests/src/test/java/com/wind/funds/wallet/FundsAccountBalanceContractTests.java`
- `tests/src/test/java/com/wind/funds/architecture/FundsModuleDependencyBoundaryTests.java`
- `tests/src/test/java/com/wind/funds/transaction/application/flow/FundsTransactionFlowTestSupport.java`

### Logs, Metadata And Authority

- 31 个当前 `status = {}` 生产消息所在文件，只改字段标签。
- 上轮 CR 列出的 21 个已触及生产文件，只删除不可信 `@author Codex/@date`。
- `docs/系分设计/02-交易路由钱包账目与投影系分设计.md`
- `docs/系分设计/03-清结算与对账系分设计.md`
- `docs/TDD设计/支付资金底座测试驱动设计.md`

## Task 1: Contract RED

- [x] 修改 RouteSnapshot、projection、payout、preflight 和 balance tests，使其只接受目标新名称，并增加旧键不被解析的断言。
- [x] 运行：

```bash
just verify-slice RouteSnapshotJsonSupportTests,PaymentInstrumentRouteDslContractTests,PaymentInstrumentTransactionApplicationServiceTests,PaymentInstrumentTransactionAuthorizationTests,FundsTransactionProjectionBusinessScenarioTests,PayoutPreflightServiceTests,PayoutOrderApplicationServiceTests,FundsAccountBalanceContractTests tests
```

预期：只因目标 getter、builder、enum 和 JSON key 尚不存在而编译失败或断言失败。

## Task 2: Typed Contract Green

- [x] 将 PaymentInstrumentRef、ExternalRuleVerification 和 LedgerBalanceBucket 切换为目标名称，不保留旧类型或 accessor。
- [x] 更新全部生产调用方、测试调用方和 core API baseline。
- [x] 运行 `just compile` 和 Task 1 聚焦测试，预期 Green。

## Task 3: Stable Map And Digest Green

- [x] RouteSnapshot 只读写 `instrumentSn/state`；projection payload 只输出 `state`。
- [x] payout receipt digest 使用 `state`，external rule evidence digest 使用 `verificationResult`。
- [x] 明确断言旧 `instrumentId/status` 不再被解析或输出。
- [x] 运行 `just test-transaction`、`just test-reconciliation`，预期 Green。

## Task 4: Convention Closure

- [x] 将 31 个无修饰生命周期消息标签改为 `state`，准入结果改为 `admissionResult`。
- [x] 删除 `testWalletCriticalWriteServicesShouldKeepBusinessLogMarkers`。
- [x] 清理 21 个目标生产文件中的 `@author Codex/@date`，不新增猜测 metadata。
- [x] 同步三份权威文档的目标字段和 breaking 边界。

## Task 5: Verification

- [x] 运行目标名称静态扫描，确认旧生产符号和旧新写键为零。
- [x] 运行 `just compile`、`just test-core`、`just test-transaction`、`just test-reconciliation`、`just test-business-flow`、`just test-boundary`。
- [x] 运行 `just pmd`、`just verify-cad`、`git diff --check`。
- [x] 回填 spec/plan 实际证据；不执行 Git 提交。
