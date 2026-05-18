# TDD 基线重置 Proposal

## 背景

产品设计、DSL 设计、系分设计和 TDD 设计已经收敛为最终版设计基线。历史 OpenSpec、Superpowers/Harness 计划和旧测试代码包含过程版本、旧命名和过渡断言，继续沿用会干扰后续编码。

## 目标

1. 作废历史 OpenSpec specs、changes、Superpowers/Harness 计划和旧测试代码。
2. 以 `docs/产品设计`、`docs/DSL设计`、`docs/系分设计`、`docs/TDD设计` 重建后续开发基线。
3. 以 TDD 为核心，分批恢复测试和实现，确保每批都能对照设计检查错漏。
4. 明确当前代码和设计的首批差距，尤其是授权过期、`settle` 强制完成模式和旧测试重建。

## 非目标

1. 本 change 不修改生产代码。
2. 本 change 不恢复任何旧测试源码。
3. 本 change 不进入 CAD Mode 自动编码。
4. 本 change 不实现清结算、对账、归档、报表指标模块。

## 成功标准

1. `openspec/project.md` 不再引用历史过程入口。
2. `openspec/specs/payment-funds-foundation/spec.md` 成为当前开发基线规格。
3. `openspec/changes/tdd-baseline-reset/tasks.md` 给出可执行的后续批次计划。
4. 全仓 `src/test/java` 已移除，`src/test/resources` 保留。
5. TDD 文档不再把旧测试源码描述为可复用资产。
