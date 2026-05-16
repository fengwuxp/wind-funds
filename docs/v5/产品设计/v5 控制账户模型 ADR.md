# v5 控制账户模型 ADR

## 文档信息

| 项目 | 内容 |
| --- | --- |
| 文档名称 | v5 控制账户模型 ADR |
| 版本 | v5-control-account-adr-1 |
| 日期 | 2026-05-13 |
| 状态 | 已建议，待评审确认 |
| 设计口径 | 支付产品与资金系统专家、资深架构师 |
| 上游输入 | `v4 设计修正 ADR.md`、`v5 DSL 契约复审矩阵.md`、`v5 产品层 TDD 验收矩阵.md`、`v5 红线与上线前置条件.md` |
| 决策范围 | 信用账户、预算组、额度调整、授权占用、授权结算、退款/拒付回补、已消费报表、`controlAdjustments` 与 `CONSUMED` |

## 一、背景

v4 已经修正了一个关键风险：不再把 `LIMIT` 当作普通资金迁移桶。

旧模型曾试图表达：

```text
额度调增：LIMIT -> AVAILABLE
额度调减：AVAILABLE -> LIMIT
授权结算：AUTHORIZATION -> LIMIT
授权退款/拒付：LIMIT -> AVAILABLE
```

这个模型的问题是：

1. `LIMIT` 同时像“额度总量”和“可迁移余额桶”。
2. `LIMIT` 与 `AVAILABLE/AUTHORIZATION` 正常余额方向不同，普通 source/target 迁移容易导致 PostingPlan 不平衡。
3. 授权结算后把 `AUTHORIZATION` 迁回 `LIMIT`，会把“已消费”误表达为“额度恢复”。
4. 用户、商户、运营和财务需要的是已授权、已结算、已退款、已拒付、剩余额度等视图，不一定需要账务层新增一个消费桶。

v5 第一阶段需要明确两个问题：

1. `controlAdjustments` 是否正式成为 wind-funds DSL 公共契约。
2. 是否引入 `CONSUMED` 作为账务余额桶。

## 二、术语边界

| 术语 | 定义 | 是否真实资金 |
| --- | --- | --- |
| 信用账户 | 表达授信、信用额度或先消费后结算的控制主体。 | 否 |
| 预算组 | 表达团队、项目、卡组或周期预算的控制主体。 | 否 |
| `LIMIT` | 额度或预算总量口径。 | 否 |
| `AVAILABLE` | 当前可授权、可占用的额度或预算。 | 否 |
| `AUTHORIZATION` | 已授权占用，尚未最终消费或释放。 | 否 |
| 已消费 | 已经结算确认的额度或预算使用结果。 | 不是直接资金余额，第一阶段作为产品报表口径。 |
| 真实资金账户 | 承担实际资金入账、出账、清结算的主体。 | 是 |

## 三、决策摘要

| 决策项 | 决策 | 理由 |
| --- | --- | --- |
| `LIMIT` 是否作为普通迁移桶 | 否 | `LIMIT` 是总量口径，只能在受控 `LIMIT_ADJUST` 调额路径内表达额度或预算总量调整，不参与普通交易 source/target 资金迁移。 |
| 授权成功 | 保持 `AVAILABLE -> AUTHORIZATION` | 这是可用额度被占用的稳定事实。 |
| 授权撤销 | 保持 `AUTHORIZATION -> AVAILABLE` | 释放未结算占用。 |
| 授权结算 | 控制主体减少或关闭 `AUTHORIZATION` 占用，不迁回 `LIMIT` | 结算表示消费确认，不是额度恢复。 |
| 授权退款 / 拒付回补 | 以产品规则恢复 `AVAILABLE` 或生成报表事实，不以 `LIMIT` 作为 source | 避免把总量口径当资金池。 |
| `controlAdjustments` 是否进入 wind-funds 公共 DSL | 暂不进入 | 当前需求可由应用层内部意图对象支撑，公共 DSL 过早暴露会锁死设计。 |
| 是否引入账务 `CONSUMED` | v5 第一阶段不引入 | 已消费先由产品报表、交易生命周期和交易视图投影计算，不作为账务余额桶。 |
| 是否需要控制账户报表 | 需要 | 但报表属于产品视图，不等同于 ledger bucket。 |

## 四、控制账户计算口径

### 4.1 基础公式

第一阶段采用产品计算口径：

```text
limitAmount
  = 当前额度或预算总量

availableAmount
  = 可授权额度或预算

authorizedAmount
  = 授权占用中金额

settledConsumedAmount
  = 已结算消费金额

refundedAmount
  = 已退款回补金额

chargebackAmount
  = 已拒付/争议回补金额
```

可展示公式：

```text
remainingDisplayAmount
  = limitAmount
  - authorizedAmount
  - settledConsumedAmount
  + refundedAmount
  + chargebackAmount
  + adjustmentDelta
```

注意：

1. 这不是 LedgerEntry 公式。
2. `settledConsumedAmount` 第一阶段来自交易生命周期和报表投影。
3. 不用 `LIMIT` 普通迁移来推导已消费。

### 4.2 信用账户

信用账户允许更灵活的管理调额：

| 场景 | 处理 |
| --- | --- |
| 额度调增 | 记录额度总量调增事实，同步增加 `AVAILABLE`。 |
| 额度调减且 `AVAILABLE` 足够 | 记录额度总量调减事实，同步减少 `AVAILABLE`。 |
| 额度调减导致 `AVAILABLE` 为负 | 可按授信产品策略允许受控负数，但必须有审批、原因、上限和审计。 |
| 授权 | `AVAILABLE -> AUTHORIZATION`，不足时失败或按授信策略处理。 |
| 撤销 | `AUTHORIZATION -> AVAILABLE`。 |
| 结算 | 关闭或减少 `AUTHORIZATION`，消费结果进入报表口径。 |
| 退款/拒付回补 | 按原结算消费回补 `AVAILABLE` 或报表口径，不能从 `LIMIT` 迁移。 |

### 4.3 预算组

预算组比信用账户更强调预算周期、审批和治理路径：

| 场景 | 处理 |
| --- | --- |
| 预算调增 | 记录预算总量调增事实，同步增加 `AVAILABLE`。 |
| 预算调减且 `AVAILABLE` 足够 | 记录预算总量调减事实，同步减少 `AVAILABLE`。 |
| 预算调减导致 `AVAILABLE` 为负 | 可按预算策略受控为负，但必须有预算周期、审批、原因、上限、账龄、报表标记和治理路径；新授权必须重新经过预算策略。 |
| 授权 | `AVAILABLE -> AUTHORIZATION`。 |
| 撤销 | `AUTHORIZATION -> AVAILABLE`。 |
| 结算 | 关闭或减少 `AUTHORIZATION`，消费结果进入预算报表口径。 |
| 退款/拒付回补 | 恢复可用预算或形成跨周期预算调整，必须按预算周期规则处理。 |

## 五、`controlAdjustments` 决策

### 5.1 不进入公共 DSL

本 ADR 决定：v5 第一阶段不把 `controlAdjustments` 作为 wind-funds 公共 `Spec` 暴露。

原因：

1. 当前 DSL 的核心是资金事实、路由、快照、posting 和 entry。
2. 控制账户调额不是普通资金迁移，过早放进公共 DSL 容易误用。
3. 信用和预算的负数策略、审批、周期、额度历史和报表需求还未完全稳定。
4. 应用层和 assembler 内部可以先用受控意图对象表达，不污染跨模块契约。

### 5.2 可使用内部意图对象

应用层可定义内部对象，例如：

```text
ControlAdjustmentIntent
  subjectRef
  controlType: CREDIT_LIMIT | BUDGET_LIMIT
  adjustmentDirection: INCREASE | DECREASE
  limitDeltaAmount
  availableDeltaAmount
  allowNegativeAvailable
  reasonCode
  approvalRef
  evidenceRef
```

边界：

1. 内部意图对象不作为 wind-funds 公共 DSL。
2. 它只用于 route resolver / posting assembler 内部协作。
3. 对外产品契约仍使用额度调整 request、调额单、审批单和审计字段。
4. 等预算周期、信用账单、已消费核销和跨周期结转需求明确后，再决定是否抽成公共契约。

## 六、`CONSUMED` 决策

### 6.1 第一阶段不引入账务桶

本 ADR 决定：v5 第一阶段不新增 `LedgerSubjectCode.CONSUMED`。

原因：

1. 现在主要诉求是防止不平衡分录和错误回写 `LIMIT`，不是立刻做完整额度账单。
2. 已消费金额可以从交易生命周期、授权结算事件和报表投影计算。
3. 如果现在新增 `CONSUMED`，需要同步 profile、route、posting、projection、余额查询、TDD 和迁移策略，影响面较大。
4. `CONSUMED` 的周期语义尚未确定：按自然月、预算周期、账单周期、项目周期还是 lifetime。

### 6.2 可作为产品报表口径

第一阶段允许在产品报表中出现：

```text
consumedAmount
settledConsumedAmount
refundedConsumedAmount
chargebackRecoveredAmount
```

这些字段来自：

1. 授权结算成功事实。
2. 授权链退款事实。
3. 授权结算后争议拒付回补事实。
4. 调额和预算周期规则。

它们不是账务余额桶，不直接参与 `RouteLeg`。

### 6.3 未来引入条件

只有满足以下条件，才考虑新增账务 `CONSUMED`：

1. 产品需要展示控制主体的完整账单，而不是简单余额报表。
2. 财务或风控需要基于已消费余额做周期控制、账单出账或追偿。
3. 已消费金额必须像余额一样参与冻结、调账或核销。
4. 已明确 periodType、periodId、结转和重开周期规则。
5. 已评估迁移历史数据的兼容策略。

## 七、场景矩阵

| 场景 | v5 第一阶段账务表达 | 产品报表表达 | 不允许的做法 |
| --- | --- | --- | --- |
| 信用额度调增 | `FundsBalanceControlService#adjust` 生成受控 `LIMIT_ADJUST` 调额事实，增加额度总量和 `AVAILABLE`。 | `limitAmount` 增加，`availableAmount` 增加。 | 把 `LIMIT -> AVAILABLE` 开放为普通交易迁移。 |
| 信用额度调减 | `FundsBalanceControlService#adjust` 生成受控 `LIMIT_ADJUST` 调额事实，减少额度总量和 `AVAILABLE`。 | `limitAmount` 减少，`availableAmount` 减少，可受控为负。 | `AVAILABLE -> LIMIT` 普通迁移且无审批。 |
| 预算调增 | `FundsBalanceControlService#adjust` 生成受控 `LIMIT_ADJUST` 预算调整事实，增加预算总量和 `AVAILABLE`。 | `budgetAmount` 增加，`availableAmount` 增加。 | 把预算当真实资金入账。 |
| 预算调减 | `FundsBalanceControlService#adjust` 生成受控 `LIMIT_ADJUST` 预算调整事实，减少预算总量和 `AVAILABLE`。 | `budgetAmount` 减少，`availableAmount` 减少，可按预算策略受控为负。 | 默认允许预算 `AVAILABLE` 为负或缺少预算治理上下文。 |
| 授权成功 | `AVAILABLE -> AUTHORIZATION`。 | 授权占用增加。 | 不检查预算和信用可用额度。 |
| 授权撤销 | `AUTHORIZATION -> AVAILABLE`。 | 授权占用减少，可用恢复。 | 重新解析当前绑定关系。 |
| 授权结算 | 控制主体关闭或减少 `AUTHORIZATION`。 | 已消费增加，授权占用减少。 | `AUTHORIZATION -> LIMIT`。 |
| 授权退款 | 按结算后消费回补规则恢复 `AVAILABLE` 或报表。 | 已消费减少或已回补增加。 | `LIMIT -> AVAILABLE`。 |
| 争议拒付回补 | 与退款共同占用已结算可回退金额。 | 已消费减少或争议回补增加。 | 与授权拒付混用。 |

## 八、产品 TDD 验收

| 验收 ID | 场景 | 输入 | 输出 | 预期 |
| --- | --- | --- | --- | --- |
| CTRL-001 | 信用额度调增 | 信用账户存在，审批通过，调增 1000。 | `limitAmount` 和 `availableAmount` 增加。 | 只能通过 `LIMIT_ADJUST` 受控路径表达，不开放为普通交易迁移。 |
| CTRL-002 | 信用额度调减为负 | 信用账户可用不足，策略允许受控负数。 | `availableAmount` 可为负。 | 必须有审批、原因、上限和审计。 |
| CTRL-003 | 预算调减为负 | 预算组可用不足，缺少预算策略、预算周期、审批或治理上下文。 | 操作失败。 | 预算默认不允许负数。 |
| CTRL-004 | 授权成功 | 信用、预算和资金主体可用均充足。 | 多主体 `AVAILABLE -> AUTHORIZATION`。 | 任一主体失败整体失败。 |
| CTRL-005 | 授权撤销 | 原授权存在。 | `AUTHORIZATION -> AVAILABLE`。 | 基于原快照 replay，不重新选路。 |
| CTRL-006 | 授权结算 | 原授权存在，结算金额不超过剩余授权。 | 控制主体减少授权占用，真实资金主体进入结算。 | 不生成 `AUTHORIZATION -> LIMIT`。 |
| CTRL-007 | 授权退款 | 已结算授权发生退款。 | 控制报表回补，可恢复 `AVAILABLE`。 | 不从 `LIMIT` 迁移。 |
| CTRL-008 | 争议拒付回补 | 已结算授权发生争议拒付。 | 控制报表回补，累计 `chargebackAmount`。 | 与授权拒付分离。 |
| CTRL-009 | 已消费报表 | 查询信用/预算已授权、已结算、已退款、已争议拒付。 | 返回产品报表字段。 | 不要求 ledger 存在 `CONSUMED`。 |
| CTRL-010 | 误用 `LIMIT` 普通迁移 | RouteLeg 使用 `LIMIT` 作为普通 source/target。 | 设计或测试失败。 | 防止再次产生不平衡分录。 |

## 九、对 DSL 和代码的影响

### 9.1 暂不改公共 DSL

本 ADR 不要求立刻新增：

```text
ControlAdjustmentSpec
LedgerSubjectCode.CONSUMED
LedgerPhaseCode.CONSUMED
RouteLegType.CONTROL_ADJUSTMENT
```

### 9.2 文档和测试要求

需要在后续编码前确认：

1. `LIMIT` 不出现在普通交易 source/target 迁移里；仅允许在 `BALANCE_CONTROL / LIMIT_ADJUST` 受控调额路径中表达信用额度或预算总量调整。
2. 授权结算不生成 `AUTHORIZATION -> LIMIT`。
3. 信用调减允许负数必须显式配置和审批。
4. 预算调减默认不允许负数。
5. 已消费报表不依赖账务 `CONSUMED`。
6. `controlAdjustments` 若出现在实现中，必须是内部对象，不对外暴露为 wind-funds Spec。

### 9.3 后续可能的 v5.2 决策

以下事项进入后续阶段：

1. 是否新增 `ControlAdjustmentSpec`。
2. 是否新增 `CONSUMED` 余额桶。
3. 是否支持预算周期结转。
4. 是否支持信用账单出账。
5. 是否支持已消费金额冻结、核销或追偿。
6. 是否将 `declinedAmount` 兼容迁移为 `chargebackAmount`。

## 十、红线

| 红线 | 说明 |
| --- | --- |
| 不得把 `LIMIT` 当真实资金或普通迁移桶 | `LIMIT` 是总量口径，不是资金池；只能在 `LIMIT_ADJUST` 受控调额路径中出现。 |
| 不得生成 `AUTHORIZATION -> LIMIT` 授权结算路径 | 结算是消费确认，不是额度恢复。 |
| 不得用 `CONSUMED` 未设计清楚前污染账务 DSL | 已消费先做产品报表。 |
| 不得在预算组默认允许负数 | 预算是强控制主体，默认非负。 |
| 不得无审批调减信用额度导致可用为负 | 负数必须有策略、审批、上限和审计。 |
| 不得把信用/预算控制账户当真实资金账户 | 它们控制可用性，不表达真实资金归属。 |

## 十一、决策结果

本 ADR 给出 v5 第一阶段建议决策：

```text
Decision 1:
  controlAdjustments 暂不进入 wind-funds 公共 DSL。

Decision 2:
  v5 第一阶段不新增账务 CONSUMED。

Decision 3:
  已消费先作为产品报表口径，由授权结算、退款、拒付和调额事实计算。

Decision 4:
  LIMIT 不作为普通迁移桶；仅允许在 LIMIT_ADJUST 受控调额路径中表达信用额度或预算总量调整，授权结算不迁回 LIMIT。
```

`v5 交易视图投影产品设计.md` 已建立，并确认授权交易和普通交易统一投影管线、按视图域和账户主体类型分域落库。

下一步建议以 `v5 支付底座完整产品 PRD.md` 为入口，把控制账户、交易视图、商户清结算、对账差错、争议拒付、VCC、ACH、全球收付款和错币种交易放到统一产品语境中评审。PRD 和系分设计确认后，再进入代码落地任务拆分。
