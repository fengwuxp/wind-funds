# 支付资金公共能力层 DSL 设计

## 文档状态与边界

| 项目 | 当前值 |
| --- | --- |
| DSL 基线 | 跨场景稳定词汇、资金指令、动作事实、路径、账务、逆向与证据边界 |
| 执行记录 | 当前任务、验证状态、授权和恢复入口统一见 [重设计执行规格](../../openspec/changes/funds-public-capability-redesign/spec.md)；本文不承载运行态 |
| 产品输入 | [支付资金公共能力层产品设计](../产品设计/支付资金公共能力层-产品设计.md) 的 `G1 PASS` 与 Product Context Card |
| DSL Owner | Funds DSL Owner；Funds Product、架构、Consumer 与测试角色共同挑战 |
| 权威范围 | 跨场景稳定词汇、事实层级、引用语义、序列化候选和负例 |
| 非授权范围 | Java/API/DTO、表、枚举、route/posting 细节、实现、测试、Consumer、Git、发布和生产 |

本文只回答“资金公共能力使用什么稳定语言描述输入、动作和证据”。它不把当前 `core` 类型当成目标答案，也不把宿主业务对象、raw rail 协议或专业规则搬入公共 DSL。

旧文档 [支付资金底座DSL承载层设计.md](支付资金底座DSL承载层设计.md) 继续作为当前实现、账务矩阵和历史方案的证据；其中 API 草图、场景枚举、Benefit 旧模型、账目和治理细节必须在 `W2-02` 后逐项迁移或删除，不能反向覆盖本文。

## 1. 准入原则

一个概念只有同时满足以下条件，才进入稳定 DSL：

1. 至少被两个真实场景需要，且语义相同。
2. 有明确 Owner，能说明它证明什么、不能证明什么。
3. 能用稳定身份、Money、引用和时间表达，不依赖宽 `Map<String, Object>`。
4. 重启、重放和状态未知时仍能沿原身份查询或停止。
5. 不需要宿主业务枚举、厂商状态、rail code、Java 类型或数据库字段才能解释。

只在单一场景出现的对象、流程状态、协议字段、风险策略和展示文案留在宿主或 adapter。没有真实 Consumer 证明复用价值的扩展点不进入 DSL。

## 2. 最小稳定词汇

下表定义概念语义，不批准同名 Java 类型。

| 概念 | 是什么 | 不是什么 | Owner 与不变量 | 序列化最小形状 | 场景证据 |
| --- | --- | --- | --- | --- | --- |
| `TenantScope` | 一组事实所属且不可跨越的租户边界。 | 用户、商户、账户或权限角色。 | Tenant Owner；任何引用、重放和对账必须同 scope。 | `tenantId` | 钱包支付；ACH 入金。 |
| `StableIdentity` | 由事实 Owner 命名、可重查且不会因 delivery/retry 改变的身份。 | 数据库主键、到达时间、随机重试号或一个万能全局 key。 | 对应事实 Owner；同身份同语义复用，同身份异语义冲突。 | `ownerNamespace + value` | Coupon confirm；payout receipt。 |
| `Money` | 正金额与明确币种组成的价值量。 | 方向、责任、余额或 FX 决策。 | Funds Owner；负数不表达方向，跨币必须另有显式换算事实。 | `amount` 使用最小货币单位，`currency` 使用稳定币种代码。 | 钱包 900 CNY；VCC authorization 100。 |
| `ResponsibilityPartyRef` | 指向承担资金、信用、应收、应付或成本责任的稳定主体。 | 交易角色、内部账户、支付工具、外部账户、merchant 标签或账务目标。 | Business/Product Owner；主体身份不能从当前账户或类型反推。 | `tenantId + owner + partyType + partyId` | 钱包付款/收款责任方；Benefit 资金 contribution 成本方/承接方。 |
| `InternalAccountRef` | 指向已由 Funds Account Owner 准入的内部资金或额度账户。 | 业务主体、Card/token、外部银行账户、PaymentInstrument 或余额值。 | Funds Account Owner；必须同 tenant、currency 和冻结责任，后继动作不得按当前 binding 重选。 | `tenantId + accountId`；账户类别与能力由被引用事实解释。 | 钱包 FundingAccount；VCC SHARED Credit + 父 Funding。 |
| `LedgerTargetRef` | 指向本次资金效果所需的内部账务分类目标。 | posting plan、LedgerEntry、余额桶、当前余额或外部结算状态。 | Ledger Owner；只声明账务目标，不证明已经过账。 | `tenantId + owner + targetId + currency` | 钱包付款/收款账务目标；payout 待清算/出款账务目标。 |
| `BalanceTargetRef` | 指向需要验证的指定账户、币种和余额投影范围。 | Ledger target、资金动作、当前余额值或外部到账。 | Balance Owner；只声明投影范围，不证明账务或投影已经闭合。 | `tenantId + accountRef + currency + projectionScope` | 钱包 payer/payee 余额效果；VCC Credit/Funding 双侧余额效果。 |
| `FactTime` | 事实 Owner 声明的发生、适用或记录时间及其语义。 | 到达顺序、幂等身份、external finality 或无时区日期字符串。 | 对应事实 Owner；时间种类、时区/日期语义必须明确，不能用 `receivedAt` 猜 `occurredAt` 或 sequence。 | `timeType + value + zoneOrOffset` | VCC issuer occurrence；对账半开窗口。 |
| `BusinessFactRef` | 指向宿主已成立业务事实的稳定引用。 | 内部资金交易、账本分录或外部终局证据。 | Business Owner；只证明引用对象在业务域的声明。 | `tenantId + owner + factType + factId` | Order payee/complete；Coupon redeem。 |
| `ExternalEvidenceRef` | 指向 adapter 已留存外部 observation、query、report 或 statement 的引用。 | raw payload、内部资金完成或 rail finality 的默认证明。 | External/Adapter Owner；必须能关联适用规则和 scope。 | `tenantId + owner + evidenceType + evidenceId` | VCC issuer event；beneficiary bank evidence。 |
| `LedgerFactRef` | 指向已形成的内部账本交易或分录事实。 | 业务完成、资金动作结果、余额已刷新或外部到账。 | Ledger Owner；引用必须同 tenant 且可沿原账本事实查询。 | `tenantId + owner + factType + factId` | 钱包完成账本事实；payout 成功账本事实。 |
| `RuleRef` | 指向一次判断实际使用的版本化规则。 | 资金域内置 rail matrix、当前配置或无版本默认值。 | 规则 Owner；至少能证明来源、版本、scope、生效期和确认方。 | `owner + ruleId + version + scope + effectiveAt` | VCC authority profile；payout arrival profile。 |
| `NormalizedExternalFundsFact` | adapter 依据已签收规则，从外部证据归一出的稳定资金输入事实。 | raw payload、provider 状态字符串、内部资金动作或 external finality 的默认结论。 | External/Adapter Owner；身份、Money、方向、时间、规则、证据和摘要必须闭合，Funds 仍独立准入。 | `identity + money + direction + factTime + ruleRef + externalEvidenceRefs[] + semanticDigest` | VCC normalized delta；ACH confirmed primary/recovery fact；acquiring capture。 |
| `DomainOutcome` | 指定事实 Owner 对本层动作给出的领域结果。 | 资金效果、账本、余额、外部终局或跨 Owner 全局状态。 | 对应事实 Owner；`owner + code` 只在该 Owner 的契约内解释，标签本身不证明零资金效果。 | `owner + code` | 钱包动作 accepted/rejected；payout 动作 succeeded/failed。 |
| `FundsResponsibilitySnapshot` | Funds 对上游责任决策执行账户/route 准入后形成的冻结资金责任事实。 | 宿主非资金责任生命周期、上游责任决策本身、当前账户绑定、活动配置、merchant 标签或 posting plan。 | Funds Account/Route Owner；Business/Product Owner 只拥有被引用的上游责任决策，Funds 独立校验后写 snapshot，后继动作不得按当前配置重算。 | `identity + businessFactRefs[] + ruleRef + items[]`；每项含 partyRef、transactionRole、accountRef、Money、ledgerTargetRef 和 balanceTargetRef。 | 钱包付款/收款资金责任；Benefit 平台/商户资金 contribution。 |
| `FundsIntent` | 一个稳定经济目的及其允许的资金价值范围，是受控执行的根。 | 一次网络请求、一次 Attempt、资金交易终态、组合业务计划或非资金业务效果。 | Business/Funds Product Owner；同一 Intent 不因超时换身份，经济语义改变必须新 Intent；reverse/recovery Intent 必须声明原资金事实。 | `identity + businessFactRefs[] + money + fundsResponsibilitySnapshotRef + originalFundsFactRefs[]`；正向可为空，逆向不可为空。 | 钱包支付/退款意图；payout/recovery 意图。 |
| `FundsAttempt` | 在原 Intent 下、经授权的一次执行尝试。 | 资金效果、重试次数配置或完成证据。 | Funds Execution Owner；同一时刻最多一个可能生效的 Attempt，`UNKNOWN` 时零新 Attempt。 | `identity + intentRef + semanticDigest` | 钱包 timeout recovery；normalized capture 提交。 |
| `FundsActionInstruction` | 在一个已准入 Attempt 下请求执行一次资金动作的稳定指令；指令身份也是后续查询该动作事实的身份。 | 业务意图、动作结果、raw rail command、route 选择、posting plan 或万能上下文容器。 | Funds Transaction Owner；责任只沿 Attempt 所属 Intent 获取，原事实按 action kind 限定，外部事实最多一个，同身份异语义冲突。 | `identity + attemptRef + actionKind + money + originalFundsFactRefs[] + normalizedExternalFact? + semanticDigest` | 钱包 complete/refund；VCC complete/release；ACH primary/recovery。 |
| `FundsEffect` | 资金动作事实中对已证明资金影响范围的声明。 | 领域结果、账本、余额、外部终局或含糊 evidence 容器。 | Funds Transaction Owner；`proven-full` 的 Money 等于动作 Money，`proven-partial` 为正且小于动作 Money，`proven-zero/unknown` 不携带 Money；已证明部分不得丢失。 | `effectKind`；仅 full/partial 带 `provenMoney`。 | 钱包 partial complete；payout primary/recovery effect。 |
| `FundsActionFact` | 一次 authorize/complete/release/primary/recovery 等动作的耐久、可唯一定位结果事实。 | 生命周期根、同步返回字符串、主聚合 current status 或账本事实。 | Funds Transaction Owner；领域结果与 `FundsEffect` 分开，已证明 partial 必须保留；账本关联另作追加证据，不改写本事实。 | `identity + intentRef + attemptRef + actionKind + money + outcome + fundsEffect + originalFundsFactRefs[] + routeProvenance[]` | 钱包 complete/refund；VCC complete/release。 |
| `OriginalFundsFactRef` | 后继动作对真实原资金动作事实的因果引用与本次分配金额。 | authorization root 的含糊 SN、当前余额、businessSn 或外部 accepted。 | Funds Transaction Owner；每项分配 Money 必须同原事实币种且不超其剩余上限，多项合计等于本动作 Money。 | `tenantId + factType + factId + relationRole + allocatedMoney` | 商品退款逐 complete 分配；payout recovery 逐原 payout effect 分配。 |
| `FundsRouteProvenance` | 嵌入 public `FundsActionFact` 的不可变 provenance value，把本动作分配到的原资金事实与其冻结 route snapshot 引用逐项绑定；首次动作记录自身执行 route。 | 独立 route fact、Consumer 选路、route legs 或 posting plan。 | Route/Funds Transaction Owner；后继项由 Funds 从原 ActionFact 加载，数量、原事实和 allocated Money 必须与指令一致。 | `originalFundsFactRef? + allocatedMoney + routeSnapshotRef + provenanceRole` | 多笔 complete 退款；多 contribution recovery。 |
| `SemanticDigest` | 对同一身份下承重经济语义的稳定摘要。 | authority、签名、幂等身份本身或日志文本 hash。 | 事实 Owner；同身份同摘要可重放，异摘要必须冲突停止。 | `algorithm + value + coveredFieldsVersion` | external funds fact；reconciliation source snapshot。 |

### 2.1 主体、角色、账户、账务目标与余额目标不可互换

`ResponsibilityPartyRef` 回答谁承担责任，`transactionRole` 只回答其在本次资金动作中的付款/收款等上下文角色，`InternalAccountRef` 回答哪个内部账户被准入，`LedgerTargetRef` 回答账务分类目标，`BalanceTargetRef` 回答需验证的投影范围；任何一项都不能由另一项或当前余额推导。

### 2.2 四类事实引用不可互换

以下引用即使底层都使用字符串，也必须保持不同语义：

| 引用 | 证明范围 | 不能证明 |
| --- | --- | --- |
| `BusinessFactRef` | 业务原因、对象与宿主声明。 | 资金动作、过账、余额或外部终局。 |
| `OriginalFundsFactRef` | 后继资金动作的真实原资金事实与因果范围。 | 业务履约或外部 finality。 |
| `LedgerFactRef` | 内部账本交易/分录事实。 | 业务完成、资金动作结果、余额已刷新或外部到账。 |
| `ExternalEvidenceRef` | 指定外部 Owner 留存的 observation/evidence。 | 未经 RuleRef 和 authority 判断的外部事实成立。 |

`LedgerFactRef` 不在 W2-01 展开 posting 字段；它的 public/internal 边界由 `W2-02` 决定。

## 3. 事实与证据层级

“层级”表示事实 Owner 和证明范围，不表示一个全局成功状态机。

| 层级 | 权威事实 | 可以证明 | 不能证明 |
| --- | --- | --- | --- |
| `L0 Business` | 业务事实与 `FundsIntent`。 | 为什么需要资金效果、Money 上限、责任范围。 | Attempt 已执行或资金已形成。 |
| `L1 External` | 外部 observation、authority decision 和 normalized fact。 | 指定 source/rule/scope 下的外部声明。 | 内部资金、账本、余额或跨 rail finality。 |
| `L2 Execution` | `FundsAttempt` 与 `FundsActionFact`。 | 哪次动作被接受/拒绝/未知，已证明何种资金效果。 | Ledger 已过账、余额已投影或外部终局。 |
| `L3 Route/Responsibility` | 冻结责任与 route snapshot。 | 本次动作应沿哪组责任和路径解释。 | 动作或过账已完成。 |
| `L4 Ledger` | Ledger transaction 与 entries。 | 内部账务事实和平衡结果。 | 余额投影、业务完成或外部到账。 |
| `L5 Balance` | 指定 owner/account/currency/bucket/time 的投影证据。 | 该投影范围在该时点的结果。 | 动作来源、外部 finality 或对账闭合。 |
| `L6 Reconciliation` | 冻结 scope/current-lineage 的 match、difference、action 和 Gate evidence。 | 指定范围与规则下的核对或阻断结果。 | 创造/修改业务、资金、账本、余额或外部事实。 |

外部 beneficiary arrival、rail finality、issuer settlement 等仍属于各自 External Owner 的事实；不能因为编号较高就覆盖内部层级。

## 4. 正交完成维度

同一个动作至少需要分别回答下列问题，不能压成一个 `SUCCESS/FAILED`：

| 维度 | 问题 | 缺证时行为 |
| --- | --- | --- |
| authority | 输入是否由有权 Owner 在适用规则下确认。 | 不产生新的资金动作。 |
| domain outcome | 本层动作结果是什么。 | 保持未知或冲突，不猜失败。 |
| funds effect | 是否形成全部、部分或零资金效果。 | 已知部分保留；未知部分停止。 |
| ledger | 所需账务是否平衡并可追溯。 | 不声明账务闭合，不重做已证明资金动作。 |
| balance | 指定责任范围的余额投影是否闭合。 | 修复投影维度，不重做资金动作。 |
| external finality | 外部 Owner 是否证明到账或终局。 | 保持待证据，不从内部结果外推。 |
| reconciliation | 指定 current lineage 是否完成核对。 | Gate 阻断或 Difference/manual，不自动调账。 |

`REJECTED/FAILED` 标签本身既不证明资金效果应计入，也不证明零效果。`UNKNOWN/PROCESSING`、冲突或查询不可用时，只沿原 identity 查询和补证；零新同类动作、零相反动作、零换号补单。

## 5. 跨概念不变量

1. `FundsIntent` 稳定，`FundsAttempt` 可受控增加；经济目的、Money、责任或原事实改变时必须新 Intent。
2. 每个 `FundsActionFact` 只记录一次动作语义；生命周期根和聚合只汇总，不冒充动作完成。
3. 同一 `StableIdentity` 的 `SemanticDigest` 一致时重放原结果；不一致时冲突停止。
4. 后继动作只引用真实原事实追加新事实，不覆盖历史。
5. complete/release 累计不得超过原授权可处置范围；refund/recovery 逐原成功事实、同币种和原责任累计不得超上限。
6. Responsibility/route 必须来自原动作冻结快照；当前绑定、余额、卡状态、活动配置或商户标签不能重算历史。
7. raw webhook、rail status/reason、NOC、业务聚合 current status 和外部账户原文不进入 Funds Core DSL。
8. 关键 Money、责任、原事实、authority、幂等和累计语义不得放入 `contextVariables`。
9. 账本只证明内部账务，Balance 只证明指定投影，对账只证明冻结范围；三者均不制造外部终局。
10. 任何真实运行切片仍需独立证明 HOST/E4/E5；本文不构成 runtime 或 production 准出。

## 6. 序列化候选

序列化使用明确对象和数组，不以字段名约定替代引用类型。下列 JSON 是 W2-01 词汇契约样例，不是 Java DTO 或 Public API。

### 6.1 钱包完成动作

```json
{
  "tenantId": 1001,
  "fundsResponsibilitySnapshot": {
    "identity": {"ownerNamespace": "funds-account", "value": "responsibility-001"},
    "ruleRef": {"owner": "wallet-product", "ruleId": "wallet-responsibility", "version": "v1", "scope": "tenant/scene", "effectiveAt": "2026-08-13T00:00:00Z"},
    "items": [
      {
        "partyRef": {"tenantId": 1001, "owner": "order", "partyType": "contract-party", "partyId": "payee-001"},
        "transactionRole": "payee",
        "accountRef": {"tenantId": 1001, "accountId": "funding-account-001"},
        "money": {"amount": 90000, "currency": "CNY"},
        "ledgerTargetRef": {"tenantId": 1001, "owner": "funds-ledger", "targetId": "wallet-payee-ledger-target", "currency": "CNY"},
        "balanceTargetRef": {"tenantId": 1001, "accountRef": {"tenantId": 1001, "accountId": "funding-account-001"}, "currency": "CNY", "projectionScope": "available-balance"}
      }
    ]
  },
  "intent": {
    "identity": {"ownerNamespace": "order", "value": "pay-intent-001"},
    "businessFactRefs": [
      {"tenantId": 1001, "owner": "order", "factType": "payment-plan", "factId": "plan-001"}
    ],
    "money": {"amount": 90000, "currency": "CNY"},
    "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "responsibility-001"}
  },
  "attempt": {
    "identity": {"ownerNamespace": "funds", "value": "attempt-001"},
    "intentRef": {"ownerNamespace": "order", "value": "pay-intent-001"},
    "semanticDigest": {"algorithm": "SHA-256", "value": "example-digest", "coveredFieldsVersion": "v1"}
  },
  "actionFact": {
    "identity": {"ownerNamespace": "funds", "value": "complete-001"},
    "intentRef": {"ownerNamespace": "order", "value": "pay-intent-001"},
    "attemptRef": {"ownerNamespace": "funds", "value": "attempt-001"},
    "actionKind": "complete",
    "money": {"amount": 90000, "currency": "CNY"},
    "factTime": {"timeType": "occurred-at", "value": "2026-08-13T08:00:00Z", "zoneOrOffset": "Z"},
    "outcome": {"owner": "funds-transaction", "code": "succeeded"},
    "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 90000, "currency": "CNY"}},
    "originalFundsFactRefs": [
      {"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-001", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 90000, "currency": "CNY"}}
    ],
    "routeProvenance": [
      {
        "originalFundsFactRef": {"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-001", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 90000, "currency": "CNY"}},
        "allocatedMoney": {"amount": 90000, "currency": "CNY"},
        "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "authorization-route-001"}},
        "provenanceRole": "replayed-original-route"
      }
    ]
  },
  "ledgerEvidence": {
    "actionIdentity": {"ownerNamespace": "funds", "value": "complete-001"},
    "ledgerFactRefs": [
      {"tenantId": 1001, "owner": "funds-ledger", "factType": "ledger-transaction", "factId": "ledger-tx-001"}
    ]
  }
}
```

该样例只证明结构能区分 Intent、Attempt、动作、逐原事实 route provenance 和独立账本证据；不证明当前 Provider 已实现逐动作引用或宿主 E4。

### 6.2 归一外部资金事实

```json
{
  "tenantId": 1001,
  "normalizedExternalFact": {
    "identity": {"ownerNamespace": "global-account-adapter", "value": "external-fact-001"},
    "money": {"amount": 50000, "currency": "USD"},
    "direction": "inbound",
    "factTime": {"timeType": "occurred-at", "value": "2026-08-13T09:00:00Z", "zoneOrOffset": "Z"},
    "ruleRef": {
      "owner": "rail-owner",
      "ruleId": "confirmed-credit-authority",
      "version": "2026-08",
      "scope": "provider/account/environment/direction",
      "effectiveAt": "2026-08-01T00:00:00Z"
    },
    "externalEvidenceRefs": [
      {"tenantId": 1001, "owner": "global-account-adapter", "evidenceType": "authoritative-query", "evidenceId": "query-001"}
    ],
    "semanticDigest": {"algorithm": "SHA-256", "value": "example-digest", "coveredFieldsVersion": "v1"}
  }
}
```

该事实只有在 adapter Owner 对 source/version/scope/identity/direction 已签收时才可成为 Funds 输入；`confirmed` 文本或 HTTP 200 不能替代它。

### 6.3 语义负例

```json
{
  "mustFail": [
    "使用 currentBalance 作为退款原事实",
    "只用 transactionSn 或 SUCCESS 证明某次动作完成",
    "把 responsibility、originalFundsFactRefs 或 authority 放进 contextVariables",
    "把 webhook status、rail reason code 或 NOC 直接传入 Funds Core",
    "UNKNOWN 后更换 identity 重发或先做相反动作",
    "用 Ledger、Balance 或 Reconciliation 反推 external finality"
  ]
}
```

## 7. 七场景映射

| 场景 | 稳定词汇落点 | 明确保留在上游 |
| --- | --- | --- |
| `SIM-01` 钱包 | Intent、Attempt、责任快照、authorization/complete/release/refund action facts、逐原事实累计。 | Order 生命周期和 merchant 模式选择。 |
| `SIM-02` 券 + 钱包 | 父计划与券腿只作 BusinessFactRef；钱包腿使用 FundsIntent、Attempt、ActionFact 与逆向因果引用。 | 父计划生命周期、Coupon confirm/release/return 事实与策略执行。 |
| `SIM-03` Benefit | 非资金折让只作 BusinessFactRef；真实 contribution 才进入 FundsResponsibilitySnapshot、ActionFact 和逐原事实逆向。 | 成本商业责任、活动规则、商户折让与非资金责任生命周期。 |
| `SIM-04` VCC | normalized external fact、授权根、增量 action facts、Credit/Funding 原责任引用。 | issuer authority/sequence/finality 和卡产品状态。 |
| `SIM-05` ACH | authoritative normalized primary/recovery fact、原 primary effect 与 direction-neutral 累计。 | rail status/reason、NOC 和 finality。 |
| `SIM-06` 收单/payout | normalized capture/payout/recovery facts、独立 arrival evidence、分项 liability facts。 | PSP/rail authority、merchant policy、beneficiary display 和 loss 决策。 |
| `SIM-07` 对账 | scope、source digest、evidence refs、current lineage、difference/action refs。 | 原始来源解析、复杂聚合、容差和人工专业裁决。 |

## 8. 当前 Core 对照与差距

当前源码只作为迁移证据：

| 当前对象 | 可复用证据 | W2-01 差距 |
| --- | --- | --- |
| `Money` | 已有金额/币种值对象和稳定摘要投影。 | 需确认所有 public 序列化统一使用最小货币单位且禁止负数表方向。 |
| `FundsAccountId` / `SubjectRef` | 已区分内部账户身份与 route 主体。 | 多套账户/主体引用的稳定词汇仍需 W2-02 收敛。 |
| `FundsInstructionSpec` | 已承载 Money、业务身份、引用和外部事实摘要。 | 当前把执行输入、场景枚举、账户/账目、操作者和宽 context 混在一个对象；不能直接等同 `FundsIntent` 或 `FundsAttempt`。 |
| `FundsInstructionReferenceSpec` | 已有原交易、授权、外部交易等引用意图。 | 单对象 + 宽 context 不能表达多 complete 分配和四类不可互换引用。 |
| route/ledger specs | 已有 route snapshot、posting、ledger 的分层形状。 | public/internal 边界、逐动作回链和事实查询证据留 W2-02。 |
| `contextVariables` | 可保存非承重、可丢弃的扩展审计摘要。 | 当前仍可能承载责任、权益、外部事实等关键语义；目标态必须迁出。 |

本文不要求新增接口、兼容层或抽象类。只有 W2-02 确认边界、真实 Consumer 和垂直切片后，才判断现有类型保留、修改或删除。

## 9. W2-01 验收与交接

W2-01 通过条件：

1. 每个稳定概念都有“是/不是”、Owner、不变量、序列化和至少两个场景证据。
2. 主体、交易角色、内部账户、账务目标、余额目标、四类事实引用、七个证据层和七个完成维度不可互换。
3. JSON 代码块可由标准 parser 解析；负例能覆盖宽 Map、场景枚举、UNKNOWN、原事实和跨层误推。
4. 当前 Core 只作差距证据，不形成 Java/API 批准。
5. 独立 Checker 确认后，只进入 `W2-02` 指令、事实、路由、账务与逆向边界；仍不进入 RED 或实现。

当前结论：`CORE_CANDIDATE / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。W2-01 仅准出稳定词汇与事实层级到 `W2-02`；HOST/E4/E5、VC eligibility、RED、Execution Grant、Java/API、生产/测试源码、Consumer、Git、发布和生产继续保持阻断。

## 10. W2-02 指令、事实、route、账务与逆向边界

### 10.1 公共语义与内部语义

“public”表示可成为跨模块稳定契约的产品语义，不等于本轮批准同名 Java API。

| 对象 | 边界 | 写入 Owner | 可对外证明 | 禁止用法 |
| --- | --- | --- | --- | --- |
| `BusinessFactRef` / `NormalizedExternalFundsFact` | public input | Business / Adapter Owner | 业务原因或已归一外部事实。 | raw payload、宿主状态或 rail code 直接进入 Funds。 |
| `FundsIntent` / `FundsAttempt` / `FundsActionInstruction` | public input | Business/Funds Product、Execution、Transaction Owner | 经济目的、受控执行身份和一次动作请求。 | 用 instruction 冒充动作成功，或用一个万能 instruction 承载所有场景。 |
| `FundsResponsibilitySnapshot` | public reference + Funds-owned frozen fact | Funds Account/Route Owner；Business/Product 只提供责任决策事实 | 已由 Funds 独立准入并冻结的责任、账户、Money、账务/余额目标与规则。 | caller 自报“已准入”，或后继动作按当前 binding 重算。 |
| `FundsActionFact` | public result/query fact | Funds Transaction Owner | 本动作领域结果与已证明资金效果。 | 用主流水、同步字符串或聚合状态替代独立动作事实。 |
| resolved route | internal transient | Route Owner | 不对外作完成证明。 | Consumer 选择 route、传 legs 或把 route 当 posting。 |
| route snapshot | internal durable fact；public 仅持稳定引用 | Route Owner | 原动作冻结责任与路径的稳定引用。 | 暴露 legs 让 Consumer 回放，或按当前配置重路由历史。 |
| `FundsRouteProvenance` | 嵌入 public `FundsActionFact` 的不可变 value | Route/Funds Transaction Owner | 本动作逐原事实分配所沿用的 route snapshot 引用。 | 作为独立 route fact、以单一 route 冒充多原事实或让 Consumer 选路。 |
| posting plan | internal transient | Ledger Posting Owner | 不对外作账本事实。 | Consumer 提交 debit/credit、账目或 posting matrix。 |
| ledger transaction/entry | internal durable fact；经 ledger face 提供引用/只读查询 | Ledger Owner | 内部账务是否平衡、可追溯。 | 由调用方创建、改写，或反推资金/业务/外部完成。 |
| balance projection | public read evidence | Balance Owner | 指定账户、币种、投影范围与时点的结果。 | 作为动作输入、原事实或外部到账证据。 |
| reconciliation evidence | reconciliation face 的独立 public read/gate evidence | Reconciliation Owner | 冻结 scope/current-lineage 的核对与阻断结果。 | 自动创建/修改资金、账本、余额或外部事实。 |

公共动作链只有一条：上游事实 -> `FundsIntent` -> `FundsAttempt` -> `FundsActionInstruction` -> `FundsActionFact`。route、posting、ledger 与 balance 是该动作内部产生或查询的正交证据，不扩展成第二套公共指令模型。

`routeSnapshotRef` 的稳定形状为 `tenantId + StableIdentity`。Consumer 不在指令中选择 route；Funds 按每个 `OriginalFundsFactRef` 加载原 `FundsActionFact.routeProvenance`，将对应原 fact、allocated Money 与原 route 写入新事实的 `routeProvenance[]`。首次动作由已准入责任生成一个执行 route provenance；public contract 不暴露 legs、账目或路由规则。

### 10.2 指令与事实

1. `FundsActionInstruction` 请求执行，不证明已接受、已生效或已过账；其 `identity` 必须可用于 timeout 后查询同一 `FundsActionFact`。
2. `Attempt` 只属于一个 Intent；指令不重复携带责任快照，而是沿 `attemptRef -> intentRef -> fundsResponsibilitySnapshotRef` 获取并校验冻结责任，禁止跨 Intent 拼装。
3. `normalizedExternalFact` 基数为 `0..1`；若动作声明由外部事实驱动则必须恰好一个。多 delivery/source 先由 Adapter Owner 归一为一个动作事实，其内部 `externalEvidenceRefs[]` 保留多来源证据。
4. reverse/recovery Intent 的 `originalFundsFactRefs[]` 与其指令必须逐项完全一致，包含 allocated Money；complete 的原授权引用属于原经济 Intent，release 则使用独立 reverse Intent 并唯一引用原 authorization。任何不一致均拒绝。
5. 多原事实 reverse/recovery Intent 的 responsibility snapshot 必须逐项覆盖原事实分配对应的冻结责任，不得合并后转嫁责任；Funds 同时校验 snapshot items、原事实分配和 route provenance 守恒。
6. 动作事实沿用指令身份，分别记录 `DomainOutcome`、`FundsEffect` 和逐原事实 `routeProvenance[]`；ledger 关联以稳定 action identity 为查询键追加独立 `LedgerFactRef` 证据，不能依赖 JSON 邻接或原地补写不可变 ActionFact。具体关联存储/API 留 `W3-01`。缺任一维度只表示该维度未闭合。
7. 同身份同摘要复用原事实；同身份异动作、Money、责任、原事实、外部事实或摘要冲突并停止。`UNKNOWN` 只查询同一身份，不创建新指令或相反动作。

#### Action kind 基数矩阵

| action kind | 原资金事实 | normalized external fact | 特有约束 |
| --- | --- | --- | --- |
| `authorize` | `0` | `0..1`；外部驱动时 `1` | 只建立授权范围；拒绝零 legs/posting/entry/balance。 |
| `complete` | `1` 个同 Intent 的 successful authorization；allocated Money = action Money | `0..1`；外部驱动时 `1` | 只消费未 complete/release 的授权范围。 |
| `release` | `1` 个 successful authorization；allocated Money = action Money | `0..1`；外部驱动时 `1` | 独立 reverse Intent，Intent/指令的原 authorization 引用逐项一致；只释放未完成授权，不撤销 complete。 |
| `primary` | `0` | `0..1`；外部 primary 时 `1` | 无外部事实时，Intent 的 BusinessFactRef 必须证明准入业务原因。 |
| `refund` | `1..n` 个 successful complete；逐项 allocated Money 合计 = action Money | `0..1`；外部退款时 `1` | reverse Intent、指令与 route provenance 逐笔分配完全一致。 |
| `recovery/adjustment` | `1..n` 个实际形成效果的原事实；逐项 allocated Money 合计 = action Money | `0..1`；外部 return/reversal 时 `1` | reverse/recovery Intent、指令与 route provenance 完全一致，逐原事实累计。 |

未列 action kind 不进入稳定 DSL；新增类型必须重新证明跨场景语义，不能通过字符串或 context 扩展。

### 10.3 Route、posting、ledger 与 balance

- resolved route 是本次责任快照在规则下的运行态解析结果；route snapshot 是其不可变、耐久的 provenance。退款、return、recovery 必须引用原 snapshot，不重新选择当前 route。
- 产生 posting 的 route leg 只连接已准入内部账户/资金主体并表达 Money 方向；外部账户只能作为 route evidence，Card、PaymentInstrument、Coupon、Order、merchant 标签和外部账户都不能成为 LedgerEntry 主体。
- 授权拒绝可以保留无 legs、不可回放的解释性 route snapshot；它不得进入 complete/release/refund 累计，也不得生成 posting、LedgerEntry 或余额变化。
- posting plan 只由内部 assembler 从动作和 route 生成；每个可记账计划必须有非空 entries、正金额且同币借贷平衡。本轮只准同币 posting；跨币在独立 FX 事实/引用获得场景和 Owner 证据前 fail-closed，不能靠 posting plan 猜换算。
- ledger transaction/entry 是追加事实，不能被 route、余额投影或 reconciliation 改写。Balance 是 ledger 的指定投影；投影失败只修复投影，不重做已证明的动作或 posting。
- reconciliation 只消费已有来源、资金、账本、余额与外部证据。`BALANCED/Gate PASS` 不构成资金指令、route 选择、posting、退款或外部终局。

### 10.4 后继与逆向动作

| 动作 | Intent/Attempt | 必须引用 | 累计与停止线 |
| --- | --- | --- | --- |
| complete | 原经济 Intent 下的新动作 Attempt | 原 successful authorization action 与原 route | 每次增量独立；confirmed complete + release 不超授权可处置上限。 |
| release | 新的 reverse Intent、Attempt 和动作事实 | 唯一 successful authorization action 与原 route | reverse Intent/指令原引用一致；只释放尚未 complete/release 的范围，不撤销已完成效果。 |
| refund | 新的 reverse Intent、Attempt 和动作事实 | 一个或多个真实 successful complete，按分配逐笔引用原 route | 每个原 complete 的 confirmed refund 累计不超其可退上限；不恢复 authorization 可完成范围。 |
| return/recovery/adjustment | 新的 reverse/recovery Intent、Attempt 和动作事实 | 实际形成效果的原 primary/payout/fee 等资金事实 | 同 tenant/Money/责任/原 route 逐原事实累计；原事实、责任或上限未知即 manual。 |

逆向不使用负 `Money`、覆盖原事实或修改历史 LedgerEntry。业务取消、Coupon return、非资金折让恢复和 loss/write-off 仍由各自 Owner 形成业务/专业事实；只有其中已归一且明确需要资金影响的部分，才创建资金 reverse/recovery Intent。

### 10.5 四组契约样例

以下 JSON 只验证语义形状，不是 API 字段表。

#### 支付与退款

```json
{
  "originalPaymentIntents": [
    {"identity": {"ownerNamespace": "order", "value": "payment-intent-001"}, "businessFactRefs": [{"tenantId": 1001, "owner": "order", "factType": "payment-plan-component", "factId": "component-001"}], "money": {"amount": 5000, "currency": "CNY"}, "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "responsibility-001"}, "originalFundsFactRefs": []},
    {"identity": {"ownerNamespace": "order", "value": "payment-intent-002"}, "businessFactRefs": [{"tenantId": 1001, "owner": "order", "factType": "payment-plan-component", "factId": "component-002"}], "money": {"amount": 4000, "currency": "CNY"}, "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "responsibility-002"}, "originalFundsFactRefs": []}
  ],
  "originalCompleteAttempts": [
    {"identity": {"ownerNamespace": "funds", "value": "complete-attempt-001"}, "intentRef": {"ownerNamespace": "order", "value": "payment-intent-001"}, "semanticDigest": {"algorithm": "SHA-256", "value": "complete-attempt-digest-001", "coveredFieldsVersion": "v1"}},
    {"identity": {"ownerNamespace": "funds", "value": "complete-attempt-002"}, "intentRef": {"ownerNamespace": "order", "value": "payment-intent-002"}, "semanticDigest": {"algorithm": "SHA-256", "value": "complete-attempt-digest-002", "coveredFieldsVersion": "v1"}}
  ],
  "originalCompleteFacts": [
    {
      "identity": {"ownerNamespace": "funds", "value": "complete-001"},
      "intentRef": {"ownerNamespace": "order", "value": "payment-intent-001"},
      "attemptRef": {"ownerNamespace": "funds", "value": "complete-attempt-001"},
      "actionKind": "complete",
      "money": {"amount": 5000, "currency": "CNY"},
      "outcome": {"owner": "funds-transaction", "code": "succeeded"},
      "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 5000, "currency": "CNY"}},
      "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-001", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 5000, "currency": "CNY"}}],
      "routeProvenance": [{"originalFundsFactRef": {"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-001", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 5000, "currency": "CNY"}}, "allocatedMoney": {"amount": 5000, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "route-001"}}, "provenanceRole": "replayed-original-route"}]
    },
    {
      "identity": {"ownerNamespace": "funds", "value": "complete-002"},
      "intentRef": {"ownerNamespace": "order", "value": "payment-intent-002"},
      "attemptRef": {"ownerNamespace": "funds", "value": "complete-attempt-002"},
      "actionKind": "complete",
      "money": {"amount": 4000, "currency": "CNY"},
      "outcome": {"owner": "funds-transaction", "code": "succeeded"},
      "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 4000, "currency": "CNY"}},
      "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-002", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 4000, "currency": "CNY"}}],
      "routeProvenance": [{"originalFundsFactRef": {"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-002", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 4000, "currency": "CNY"}}, "allocatedMoney": {"amount": 4000, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "route-002"}}, "provenanceRole": "replayed-original-route"}]
    }
  ],
  "refundIntent": {
    "identity": {"ownerNamespace": "order", "value": "refund-intent-001"},
    "businessFactRefs": [{"tenantId": 1001, "owner": "order", "factType": "refund-decision", "factId": "refund-001"}],
    "money": {"amount": 2000, "currency": "CNY"},
    "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "refund-responsibility-001"},
    "originalFundsFactRefs": [
      {"tenantId": 1001, "factType": "complete-action", "factId": "complete-001", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 1200, "currency": "CNY"}},
      {"tenantId": 1001, "factType": "complete-action", "factId": "complete-002", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 800, "currency": "CNY"}}
    ]
  },
  "refundAttempt": {
    "identity": {"ownerNamespace": "funds", "value": "refund-attempt-001"},
    "intentRef": {"ownerNamespace": "order", "value": "refund-intent-001"},
    "semanticDigest": {"algorithm": "SHA-256", "value": "refund-attempt-digest", "coveredFieldsVersion": "v1"}
  },
  "instruction": {
    "identity": {"ownerNamespace": "funds", "value": "refund-action-001"},
    "attemptRef": {"ownerNamespace": "funds", "value": "refund-attempt-001"},
    "actionKind": "refund",
    "money": {"amount": 2000, "currency": "CNY"},
    "originalFundsFactRefs": [
      {"tenantId": 1001, "factType": "complete-action", "factId": "complete-001", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 1200, "currency": "CNY"}},
      {"tenantId": 1001, "factType": "complete-action", "factId": "complete-002", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 800, "currency": "CNY"}}
    ],
    "semanticDigest": {"algorithm": "SHA-256", "value": "refund-digest", "coveredFieldsVersion": "v1"}
  },
  "refundActionFact": {
    "identity": {"ownerNamespace": "funds", "value": "refund-action-001"},
    "intentRef": {"ownerNamespace": "order", "value": "refund-intent-001"},
    "attemptRef": {"ownerNamespace": "funds", "value": "refund-attempt-001"},
    "actionKind": "refund",
    "money": {"amount": 2000, "currency": "CNY"},
    "outcome": {"owner": "funds-transaction", "code": "succeeded"},
    "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 2000, "currency": "CNY"}},
    "originalFundsFactRefs": [
      {"tenantId": 1001, "factType": "complete-action", "factId": "complete-001", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 1200, "currency": "CNY"}},
      {"tenantId": 1001, "factType": "complete-action", "factId": "complete-002", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 800, "currency": "CNY"}}
    ],
    "routeProvenance": [
      {"originalFundsFactRef": {"tenantId": 1001, "factType": "complete-action", "factId": "complete-001", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 1200, "currency": "CNY"}}, "allocatedMoney": {"amount": 1200, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "route-001"}}, "provenanceRole": "replayed-original-route"},
      {"originalFundsFactRef": {"tenantId": 1001, "factType": "complete-action", "factId": "complete-002", "relationRole": "refunds-allocated-amount", "allocatedMoney": {"amount": 800, "currency": "CNY"}}, "allocatedMoney": {"amount": 800, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "route-002"}}, "provenanceRole": "replayed-original-route"}
    ]
  },
  "unknownRecovery": {
    "queryActionIdentity": {"ownerNamespace": "funds", "value": "refund-action-001"},
    "allowed": ["query-same-action-fact"],
    "forbidden": ["new-attempt", "new-refund", "opposite-action"]
  }
}
```

#### 授权、部分完成与释放

```json
{
  "intent": {
    "identity": {"ownerNamespace": "vcc", "value": "authorization-intent-100"},
    "businessFactRefs": [{"tenantId": 1001, "owner": "vcc", "factType": "card-authorization", "factId": "auth-business-100"}],
    "money": {"amount": 10000, "currency": "CNY"},
    "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "vcc-responsibility-001"},
    "originalFundsFactRefs": []
  },
  "releaseIntent": {
    "identity": {"ownerNamespace": "vcc", "value": "authorization-release-intent-20"},
    "businessFactRefs": [{"tenantId": 1001, "owner": "vcc", "factType": "authorization-release-decision", "factId": "release-business-20"}],
    "money": {"amount": 2000, "currency": "CNY"},
    "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "vcc-responsibility-001"},
    "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "releases-authorized-range", "allocatedMoney": {"amount": 2000, "currency": "CNY"}}]
  },
  "authorizationAttempt": {"identity": {"ownerNamespace": "funds", "value": "attempt-authorize-100"}, "intentRef": {"ownerNamespace": "vcc", "value": "authorization-intent-100"}, "semanticDigest": {"algorithm": "SHA-256", "value": "authorize-attempt-digest", "coveredFieldsVersion": "v1"}},
  "authorizationActionFact": {
    "identity": {"ownerNamespace": "funds", "value": "authorize-100"},
    "intentRef": {"ownerNamespace": "vcc", "value": "authorization-intent-100"},
    "attemptRef": {"ownerNamespace": "funds", "value": "attempt-authorize-100"},
    "actionKind": "authorize",
    "money": {"amount": 10000, "currency": "CNY"},
    "outcome": {"owner": "funds-transaction", "code": "succeeded"},
    "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 10000, "currency": "CNY"}},
    "originalFundsFactRefs": [],
    "routeProvenance": [{"allocatedMoney": {"amount": 10000, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "vcc-route-001"}}, "provenanceRole": "initial-execution"}]
  },
  "attempts": [
    {"identity": {"ownerNamespace": "funds", "value": "attempt-complete-30"}, "intentRef": {"ownerNamespace": "vcc", "value": "authorization-intent-100"}, "semanticDigest": {"algorithm": "SHA-256", "value": "attempt-30-digest", "coveredFieldsVersion": "v1"}},
    {"identity": {"ownerNamespace": "funds", "value": "attempt-complete-50"}, "intentRef": {"ownerNamespace": "vcc", "value": "authorization-intent-100"}, "semanticDigest": {"algorithm": "SHA-256", "value": "attempt-50-digest", "coveredFieldsVersion": "v1"}},
    {"identity": {"ownerNamespace": "funds", "value": "attempt-release-20"}, "intentRef": {"ownerNamespace": "vcc", "value": "authorization-release-intent-20"}, "semanticDigest": {"algorithm": "SHA-256", "value": "attempt-release-digest", "coveredFieldsVersion": "v1"}}
  ],
  "instructions": [
    {"identity": {"ownerNamespace": "funds", "value": "complete-30"}, "attemptRef": {"ownerNamespace": "funds", "value": "attempt-complete-30"}, "actionKind": "complete", "money": {"amount": 3000, "currency": "CNY"}, "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 3000, "currency": "CNY"}}], "semanticDigest": {"algorithm": "SHA-256", "value": "complete-30-digest", "coveredFieldsVersion": "v1"}},
    {"identity": {"ownerNamespace": "funds", "value": "complete-50"}, "attemptRef": {"ownerNamespace": "funds", "value": "attempt-complete-50"}, "actionKind": "complete", "money": {"amount": 5000, "currency": "CNY"}, "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 5000, "currency": "CNY"}}], "semanticDigest": {"algorithm": "SHA-256", "value": "complete-50-digest", "coveredFieldsVersion": "v1"}},
    {"identity": {"ownerNamespace": "funds", "value": "release-20"}, "attemptRef": {"ownerNamespace": "funds", "value": "attempt-release-20"}, "actionKind": "release", "money": {"amount": 2000, "currency": "CNY"}, "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "releases-authorized-range", "allocatedMoney": {"amount": 2000, "currency": "CNY"}}], "semanticDigest": {"algorithm": "SHA-256", "value": "release-20-digest", "coveredFieldsVersion": "v1"}}
  ],
  "actionFacts": [
    {"identity": {"ownerNamespace": "funds", "value": "complete-30"}, "intentRef": {"ownerNamespace": "vcc", "value": "authorization-intent-100"}, "attemptRef": {"ownerNamespace": "funds", "value": "attempt-complete-30"}, "actionKind": "complete", "money": {"amount": 3000, "currency": "CNY"}, "outcome": {"owner": "funds-transaction", "code": "succeeded"}, "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 3000, "currency": "CNY"}}, "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 3000, "currency": "CNY"}}], "routeProvenance": [{"originalFundsFactRef": {"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 3000, "currency": "CNY"}}, "allocatedMoney": {"amount": 3000, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "vcc-route-001"}}, "provenanceRole": "replayed-original-route"}]},
    {"identity": {"ownerNamespace": "funds", "value": "complete-50"}, "intentRef": {"ownerNamespace": "vcc", "value": "authorization-intent-100"}, "attemptRef": {"ownerNamespace": "funds", "value": "attempt-complete-50"}, "actionKind": "complete", "money": {"amount": 5000, "currency": "CNY"}, "outcome": {"owner": "funds-transaction", "code": "succeeded"}, "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 5000, "currency": "CNY"}}, "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 5000, "currency": "CNY"}}], "routeProvenance": [{"originalFundsFactRef": {"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "consumes-authorized-range", "allocatedMoney": {"amount": 5000, "currency": "CNY"}}, "allocatedMoney": {"amount": 5000, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "vcc-route-001"}}, "provenanceRole": "replayed-original-route"}]},
    {"identity": {"ownerNamespace": "funds", "value": "release-20"}, "intentRef": {"ownerNamespace": "vcc", "value": "authorization-release-intent-20"}, "attemptRef": {"ownerNamespace": "funds", "value": "attempt-release-20"}, "actionKind": "release", "money": {"amount": 2000, "currency": "CNY"}, "outcome": {"owner": "funds-transaction", "code": "succeeded"}, "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 2000, "currency": "CNY"}}, "originalFundsFactRefs": [{"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "releases-authorized-range", "allocatedMoney": {"amount": 2000, "currency": "CNY"}}], "routeProvenance": [{"originalFundsFactRef": {"tenantId": 1001, "factType": "authorization-action", "factId": "authorize-100", "relationRole": "releases-authorized-range", "allocatedMoney": {"amount": 2000, "currency": "CNY"}}, "allocatedMoney": {"amount": 2000, "currency": "CNY"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "vcc-route-001"}}, "provenanceRole": "replayed-original-route"}]}
  ],
  "invariant": "confirmedCompleteAmount + confirmedReleaseAmount <= authorizedAmount"
}
```

每个 action 都引用原 authorization；Funds 从该 authorization ActionFact 加载并校验同一 `vcc-route-001`。累计为 complete 80、release 20、授权剩余 0。重复 `complete-50` 复用原事实，异金额重放冲突，`release` 不能作用于已 complete 的 80。

#### 外部入金与 return

```json
{
  "primaryIntent": {
    "identity": {"ownerNamespace": "global-account", "value": "primary-intent-001"},
    "businessFactRefs": [{"tenantId": 1001, "owner": "global-account", "factType": "credit-admission", "factId": "credit-admission-001"}],
    "money": {"amount": 50000, "currency": "USD"},
    "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "external-credit-responsibility-001"},
    "originalFundsFactRefs": []
  },
  "primaryAttempt": {"identity": {"ownerNamespace": "funds", "value": "primary-attempt-001"}, "intentRef": {"ownerNamespace": "global-account", "value": "primary-intent-001"}, "semanticDigest": {"algorithm": "SHA-256", "value": "primary-attempt-digest", "coveredFieldsVersion": "v1"}},
  "primaryInstruction": {
    "identity": {"ownerNamespace": "funds", "value": "primary-credit-001"},
    "attemptRef": {"ownerNamespace": "funds", "value": "primary-attempt-001"},
    "actionKind": "primary",
    "money": {"amount": 50000, "currency": "USD"},
    "originalFundsFactRefs": [],
    "normalizedExternalFact": {
      "identity": {"ownerNamespace": "global-account-adapter", "value": "confirmed-credit-001"},
      "money": {"amount": 50000, "currency": "USD"},
      "direction": "inbound",
      "factTime": {"timeType": "occurred-at", "value": "2026-08-13T10:00:00Z", "zoneOrOffset": "Z"},
      "ruleRef": {"owner": "rail-owner", "ruleId": "confirmed-credit-authority", "version": "2026-08", "scope": "provider/account/environment/direction", "effectiveAt": "2026-08-01T00:00:00Z"},
      "externalEvidenceRefs": [{"tenantId": 1001, "owner": "global-account-adapter", "evidenceType": "authoritative-query", "evidenceId": "query-002"}],
      "semanticDigest": {"algorithm": "SHA-256", "value": "confirmed-credit-digest", "coveredFieldsVersion": "v1"}
    },
    "semanticDigest": {"algorithm": "SHA-256", "value": "primary-instruction-digest", "coveredFieldsVersion": "v1"}
  },
  "primaryActionFact": {
    "identity": {"ownerNamespace": "funds", "value": "primary-credit-001"},
    "intentRef": {"ownerNamespace": "global-account", "value": "primary-intent-001"},
    "attemptRef": {"ownerNamespace": "funds", "value": "primary-attempt-001"},
    "actionKind": "primary",
    "money": {"amount": 50000, "currency": "USD"},
    "outcome": {"owner": "funds-transaction", "code": "succeeded"},
    "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 50000, "currency": "USD"}},
    "originalFundsFactRefs": [],
    "routeProvenance": [{"allocatedMoney": {"amount": 50000, "currency": "USD"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "external-credit-route-001"}}, "provenanceRole": "initial-execution"}]
  },
  "returnIntent": {
    "identity": {"ownerNamespace": "global-account", "value": "return-intent-001"},
    "businessFactRefs": [{"tenantId": 1001, "owner": "global-account", "factType": "return-decision", "factId": "return-decision-001"}],
    "money": {"amount": 10000, "currency": "USD"},
    "fundsResponsibilitySnapshotRef": {"ownerNamespace": "funds-account", "value": "external-credit-responsibility-001"},
    "originalFundsFactRefs": [{"tenantId": 1001, "factType": "primary-credit-action", "factId": "primary-credit-001", "relationRole": "reverses-confirmed-effect", "allocatedMoney": {"amount": 10000, "currency": "USD"}}]
  },
  "returnAttempt": {"identity": {"ownerNamespace": "funds", "value": "return-attempt-001"}, "intentRef": {"ownerNamespace": "global-account", "value": "return-intent-001"}, "semanticDigest": {"algorithm": "SHA-256", "value": "return-attempt-digest", "coveredFieldsVersion": "v1"}},
  "returnInstruction": {
    "identity": {"ownerNamespace": "funds", "value": "return-001"},
    "attemptRef": {"ownerNamespace": "funds", "value": "return-attempt-001"},
    "actionKind": "recovery",
    "originalFundsFactRefs": [{"tenantId": 1001, "factType": "primary-credit-action", "factId": "primary-credit-001", "relationRole": "reverses-confirmed-effect", "allocatedMoney": {"amount": 10000, "currency": "USD"}}],
    "money": {"amount": 10000, "currency": "USD"},
    "normalizedExternalFact": {
      "identity": {"ownerNamespace": "global-account-adapter", "value": "confirmed-return-001"},
      "money": {"amount": 10000, "currency": "USD"},
      "direction": "outbound",
      "factTime": {"timeType": "occurred-at", "value": "2026-08-13T11:00:00Z", "zoneOrOffset": "Z"},
      "ruleRef": {"owner": "rail-owner", "ruleId": "return-authority", "version": "2026-08", "scope": "provider/account/environment/direction", "effectiveAt": "2026-08-01T00:00:00Z"},
      "externalEvidenceRefs": [{"tenantId": 1001, "owner": "global-account-adapter", "evidenceType": "authoritative-report", "evidenceId": "report-001"}],
      "semanticDigest": {"algorithm": "SHA-256", "value": "confirmed-return-digest", "coveredFieldsVersion": "v1"}
    },
    "semanticDigest": {"algorithm": "SHA-256", "value": "return-instruction-digest", "coveredFieldsVersion": "v1"}
  },
  "returnActionFact": {
    "identity": {"ownerNamespace": "funds", "value": "return-001"},
    "intentRef": {"ownerNamespace": "global-account", "value": "return-intent-001"},
    "attemptRef": {"ownerNamespace": "funds", "value": "return-attempt-001"},
    "actionKind": "recovery",
    "money": {"amount": 10000, "currency": "USD"},
    "outcome": {"owner": "funds-transaction", "code": "succeeded"},
    "fundsEffect": {"effectKind": "proven-full", "provenMoney": {"amount": 10000, "currency": "USD"}},
    "originalFundsFactRefs": [{"tenantId": 1001, "factType": "primary-credit-action", "factId": "primary-credit-001", "relationRole": "reverses-confirmed-effect", "allocatedMoney": {"amount": 10000, "currency": "USD"}}],
    "routeProvenance": [{"originalFundsFactRef": {"tenantId": 1001, "factType": "primary-credit-action", "factId": "primary-credit-001", "relationRole": "reverses-confirmed-effect", "allocatedMoney": {"amount": 10000, "currency": "USD"}}, "allocatedMoney": {"amount": 10000, "currency": "USD"}, "routeSnapshotRef": {"tenantId": 1001, "identity": {"ownerNamespace": "funds-route", "value": "external-credit-route-001"}}, "provenanceRole": "replayed-original-route"}]
  }
}
```

adapter 负责 return authority、方向和规则版本；Funds 只校验 normalized fact、原 primary effect、Money、责任、route、幂等和累计，不解析 ACH/rail code。

#### 清结算与对账

```json
{
  "reconciliationEvidence": {
    "scopeIdentity": {"ownerNamespace": "reconciliation", "value": "scope-001"},
    "currentLineage": {"ownerNamespace": "reconciliation", "value": "run-003"},
    "sourceDigest": {"algorithm": "SHA-256", "value": "source-digest", "coveredFieldsVersion": "v1"},
    "result": "balanced",
    "resultRef": {"ownerNamespace": "reconciliation", "value": "result-003"}
  },
  "mayCreateFundsInstruction": false,
  "mayRewriteLedgerOrBalance": false
}
```

复杂 `1:N/N:1` 必须由来源 Owner 先固化聚合事实；Difference 只能追加 action evidence，并由后继 current-lineage `BALANCED` 关闭。

### 10.6 当前对象处置候选

| 当前对象 | W2-02 处置 |
| --- | --- |
| `FundsInstructionSpec` | 目标态拆为 Intent/Attempt/ActionInstruction/typed facts；当前万能对象不进入稳定公共 DSL。 |
| `FundsInstructionReferenceSpec` | 目标态由 Business、OriginalFunds、Ledger、External 四类显式引用替代，支持多个原事实；宽 context 不承重。 |
| `ResolvedRouteSpec` | 保留为 internal transient 候选，不成为 Consumer 输入或完成事实。 |
| `RouteSnapshotSpec` | 保留为 internal durable fact 候选；public 只持稳定引用，不读取 legs 重放。 |
| `LedgerPostingPlanSpec` / `LedgerTransactionSpec` / `LedgerEntrySpec` | posting write primitive 保持 internal；ledger face 只提供必要写用例和只读事实查询，具体接口留 W3-01。 |
| balance/reconciliation face | 保持各自只读投影、证据和 Gate 边界；不并入 FundsActionInstruction。 |

目标态切换不保留 bridge、转发重载、别名、双写双读或兼容窗口。具体删除与 Consumer 同步迁移只在 W3 系分、RS-001 清册、垂直切片和独立 Execution Grant 后执行；本轮不改 Java/API。

### 10.7 W2-02 验收与交接

W2-02 通过条件：

1. 业务意图、执行指令、动作事实、route snapshot、posting plan、ledger fact、balance projection 与 reconciliation evidence 的 Owner 和 public/internal 边界不可互换。
2. 四组契约样例可解析，并覆盖原事实、累计、UNKNOWN、只读对账与零跨层证明。
3. 逆向均以新 Intent/Attempt/动作事实追加；release 与 refund、资金与非资金恢复不混用。
4. 当前 Core 只作迁移证据，不批准目标 Java/API、状态枚举、表、事务或 posting matrix。
5. 独立 Checker PASS 后只进入 `W3-01` Core/Ledger 系分；HOST/E4/E5、VC、RED、Grant、TDD、代码、测试、Consumer、Git、发布和生产继续阻断。

当前结论：`BOUNDARY_CANDIDATE / INDEPENDENT_CHECKER_PASS / 0 P0-P2`。W2-02 只准出上述 DSL 边界到 `W3-01` Core/Ledger 系分；不批准 RED、Java/API、实现、测试、Consumer、Git、发布或生产。

### 10.8 `MIG-02B` authorization release 语义 Profile

本 Profile 不增加稳定词汇，只把既有 `FundsIntent`、`FundsAttempt`、`FundsActionInstruction`、`FundsActionFact`、`OriginalFundsFactRef`、`FundsRouteProvenance` 和 `FundsEffect` 约束到普通 authorization release：

1. release 建立引用原 authorization 的独立 reverse Intent；release Attempt 属于该 reverse Intent，同一时刻仍受“最多一个可能生效 Attempt”和 UNKNOWN 停止规则约束。
2. instruction 的 `actionKind=release`，Money 为正；reverse Intent 与 instruction 的 `originalFundsFactRefs` 必须逐项相同且恰好一项，引用成功 authorization，`allocatedMoney=instruction.money`。
3. 普通内部 release 不要求 `normalizedExternalFact`；若未来由外部事实驱动，外部 authority/finality 必须先由 adapter 独立归一，本 Profile 不批准该路径。
4. 成功 ActionFact 保持 `actionKind=release`、`proven-full`、同 Money、原 authorization 引用和逐原 route provenance；不得用 root 聚合或余额变化冒充本次动作事实。
5. `complete + release` 的逐原授权累计不得超过授权可处置金额。`proven-zero` 必须有独立耐久零效果证据；FAILED 标签本身不证明零，UNKNOWN 不携带 Money。
6. identity、release `intentRef`、Money、outcome/effect、原 authorization ref、route provenance 与 SemanticDigest 必须跨重启和 projector 版本保持不变；digest 的承重语义必须同时覆盖 release intentRef 与原 authorization ref。具体 Java 类型、字符串编码和持久化形态不在本 Profile 决定。

负例：release 只引用 authorization root String、调用方自报 route、用当前 binding 重算责任、释放已完成金额、expired/timeout 自动释放、把 unfreeze/settlement release/payout failure 因名称相近纳入本 Profile，均不构成合法公共 DSL。

#### 10.8.1 Release ActionFact 物理命名 Contract

当前 durable group 没有独立 release root，但在 authorization root 下持久化了唯一 `businessScene + businessSn + REVERSAL` action group。`businessScene/businessSn` 允许冒号，不能用原文分隔拼接。定义 `b64(x)=Base64URL(UTF-8(x), without padding)`，目标只从耐久字段机械派生三个版本化、无歧义身份，不把 authorization root 自身冒充 reverse Intent：

- `identity=release:v1:<b64(authorizationSn)>:<b64(businessScene)>:<b64(businessSn)>`；
- `intentRef=release-intent:v1:<b64(authorizationSn)>:<b64(businessScene)>:<b64(businessSn)>`；
- `attemptRef=release-attempt:v1:<b64(authorizationSn)>:<b64(businessScene)>:<b64(businessSn)>:REVERSAL`。

成功事实固定 `actionKind=release`、`DomainOutcome=funds-transaction/succeeded`、`FundsEffect=proven-full`。唯一 `OriginalFundsFactRef` 使用 `factType=funds-action`、原 authorization ActionFact identity、`relationRole=releases-authorized-effect` 与本次 Money；每条原 HOLD 责任使用 `provenanceRole=replayed-original-route`。ActionFact Money 只计一次，SHARED 多 sibling/provenance 不得相加。

SemanticDigest 继续使用 `SHA-256`，domain/version 固定为 `transaction.action.release.projection` / `transaction.action.release.projection.v1`。摘要覆盖 identity、intentRef、attemptRef、action kind、Money、outcome/effect、原 authorization identity 与 semantic digest、关系与 allocated Money、排序后的 release siblings、原/本次 Ledger refs、原 route 摘要和逐 HOLD/RELEASE provenance；不得吸收会随后续 complete/release/refund 改变的 root 当前累计或余额。累计只作查询时完整性合取，不改写历史 release DTO/digest。

列表查询必须同时检查主交易和全部 authorization successor detail 候选：同一业务键命中主交易与 release detail、命中多个 action family 或多个 authorization root 时属于歧义，列表与对应 identity 查询共同返回空/UNKNOWN，不按查询顺序遮蔽或合并。delimiter collision 种子 `(scene=A,sn=B:C)` 与 `(scene=A:B,sn=C)` 必须生成不同三层身份并可分别查询。

release 完整性合取同时验证全部 successful COMPLETE groups 与全部 successful REVERSAL groups：前者合计精确等于 `completedAmount`，后者合计精确等于 `reversedAmount`，再验证二者相加不超过 authorized。任一 complete/release sibling、route、reference 或 root 累计不一致时双查询 fail-closed；这些可变累计只作当前完整性判断，不进入单个历史 release digest。

RED rework 已把该 DSL 约束落成可达行为：每个 release 先精确核验 Funding/SHARED Balance delta、原 HOLD leg、replay consumed ID/amount 与 `RELEASE_<originalLegId>` posting；首笔 release DTO 形成后分别以后继 complete 和第二笔 release 改变 root 当前累计，再双查询原 DTO。fresh focused=`60/7F/0E`、transaction=`185/7F/0E`，7F 仍只指向缺失 release projector；最终独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。

Green 已在既有查询实现中机械实现本节 Contract：三层 Base64URL v1 身份使用 UTF-8/no-padding 并做 canonical 重编码校验；同业务键多义候选双查询 fail-closed；release group 只接受完整 successful PAY/REVERSAL/RELEASE 责任组，逐原 HOLD replay 与原/本次 Ledger 引用一致；verified COMPLETE/REVERSAL 分别闭合 root 累计。release digest 纳入原 fact digest algorithm/value/version、allocated Money、排序 siblings、Ledger、route 与 replay，不纳入 root mutable cumulative、余额、描述或时间。fresh focused=`60/0F/0E`、transaction=`185/0F/0E`，最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。本轮未新增稳定 DSL 词汇、Java 类型、兼容形式或第二事实源；MIG-02B 当前范围关闭，下一切片必须重新成卡。

### 10.9 `MIG-02C` authorization refund 语义 Profile

本 Profile 不新增稳定词汇，只把既有 reverse Intent、`refund`、`OriginalFundsFactRef`、`FundsRouteProvenance`、`FundsEffect` 和 `SemanticDigest` 约束到普通 authorization refund：

1. refund 建立独立 reverse Intent；refund Attempt 属于该 Intent，UNKNOWN 时只恢复原 Attempt/action identity。
2. Intent 与 instruction 的 `originalFundsFactRefs` 必须逐项一致，基数为 `1..n`，每项引用真实 successful complete，携带正的同币种 `allocatedMoney`；合计等于 refund instruction Money。
3. authorization root 可作生命周期关联，但不得代替 complete 原事实或分配。多来源证据必须先由上游归一为明确分配，Funds 不根据时间、顺序、金额或余额推测。
4. 成功 `FundsActionFact` 保持 `actionKind=refund`、`proven-full`、同 Money、全部逐原分配和对应 route provenance；一个原 complete 一条 provenance 关系，不得用全局单 route 覆盖多原事实。
5. 对每条 complete，已证明 refund 累计必须与耐久成功 refund 分配合计一致且不超过可退上限；已证明 partial 占用上限。FAILED 标签不证明零，UNKNOWN 不携带 Money。
6. identity、refund `intentRef`、Money、outcome/effect、规范化的逐原分配、route provenance 和 digest 必须跨重启与 projector 版本不变。具体排序/编码算法不在本 Profile 预批，但 digest 不得遗漏 intentRef、complete refs 或 allocated Money。

当前 authorization-root `AUTH_REFUND` 没有逐 complete 分配耐久证据，因而不符合本 Profile。它不能通过根级 `refundedAmount`、Ledger 或 Balance 反推出分配；在 `D-MIG-001` 重开并闭合耐久证据决策前，canonical refund ActionFact 必须保持 unsupported/manual。

### 10.10 `D-MIG-001-R` 对 DSL 的不变边界

`D-MIG-001-R` 三个候选都不新增 action kind、original relation role 或通用扩展点，也不改变 10.9 的 refund 语义。A 只表示当前不能产出该公共事实；B 必须使现有 durable group 完整承载已定义的 refs、allocated Money 和 provenance；C 若被选择，也必须一次性迁移全部已接受 action kind，不能为 refund 发明新 DSL。

Human Owner 已接受 A：公共查询对缺少上游权威逐 complete 来源和耐久分配的 authorization refund 必须返回空/UNKNOWN；不得用 nullable refs、宽 context、默认算法或多态 registry 绕过决策。root-level refund 执行可继续，但不因此形成 10.9 定义的 canonical refund ActionFact。

`D-MIG-001-R` 决策包与 A Acceptance Checker 均判定 `PASS / 0 P0-P2`。A 的选择不新增 DSL 或实现授权；未来只有项目级重开并证明完整关联合同后，才能产出 10.9 的 canonical refund 事实。该结论已进入 MIG-04 纯文档卡，不得借模块归属迁移重开 refund DSL 或代码。

### 10.11 `MIG-04` 模块归属不新增 DSL

MIG-04 不新增 action kind、Intent/Attempt、责任 snapshot、route provenance、LedgerFactRef 或 FundsEffect。它只把既有稳定概念归还给正确 Owner：

1. Wallet 持有账户、责任关系、支付工具/binding、能力、状态、Spend Rule/decision/control movement；这些事实只回答资金准入与控制，不代表交易动作完成。
2. Transaction 持有 Intent/Attempt、ActionInstruction/ActionFact、交易生命周期、原事实、累计与 route，并协调 Wallet 的冻结准入/控制事实；Wallet facade 名称不能改变该事实 Owner。
3. Ledger 持有 Ledger profile、posting、LedgerTransaction/Entry、Balance projection 和窄只读事实；Wallet 的 Ledger wrapper/DTO 不是新的稳定 DSL，也不能成为长期公共契约。
4. Host/Consumer 持有 Benefit 等场景事实。场景 facade 退出 Provider 时只能归一到已有资金 DSL，不得为迁移增加万能 action、registry、factory 或 context 扩展槽。

模块搬迁不得改变任何公共事实的 identity、Money、outcome/effect、original refs、route provenance 或 SemanticDigest。不存在兼容 alias、双 DTO、双读/双写或按 Consumer 选择旧/新 Owner；若真实调用清册、MIG-05 原 LedgerTransaction 引用解析合同或 MIG-08 可部署 Consumer E4 未闭合，对应对象保持未迁移并阻断整个原子切换。MIG-05 文档方向 PASS 不代替该物理契约准入。

MIG-04 独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。本 Profile 只证明迁移不需要扩展 DSL；该 PASS 当时只准进入 MIG-05 文档卡，当前活动状态见 10.12，不授权实现。

### 10.12 `MIG-05` 不新增 Projection DSL

MIG-05 不新增 `Projection`、`View`、`ReadStore`、`LedgerProfileDTO` 或通用 evidence envelope。已有稳定词汇已经足够表达边界：

1. `FundsActionFact` 仍是 Transaction Owner 对唯一 durable action group 的版本化只读解释；它只证明动作结果和 `FundsEffect`，不证明 Ledger、Balance、外部 finality 或 reconciliation。
2. `LedgerFactRef` 只引用 Ledger Owner 的耐久账本事实。它与 `FundsActionFact` 正交，不能因两者可以关联就把 Ledger 字段嵌入 ActionFact，或由任一侧推导另一侧已闭合。
3. `BalanceTargetRef` 只定位指定余额投影目标；LedgerFactRef 不自动证明该目标已经反映，仍需 Ledger/Balance Owner 的可重查证据。
4. Ledger profile、posting spec/plan、LedgerTransaction/Entry 和 Balance projection 是 Ledger 内部或 Ledger-owned 语义；Wallet wrapper、event string、分页参数和 Mapper/Entity 都不是公共 DSL。
5. 所谓交易投影只允许执行确定性映射：相同源事实版本必须得到逐字段相同的 ActionFact；缺失、冲突或版本不可识别时返回空/UNKNOWN，不允许猜责任、原事实、Money、route、finality 或退款分配。

面向 Transaction 的原账本引用只需满足稳定可解析性，不自动推导出新的 Ledger evidence DSL。Human Owner 已接受 `CI-MIG05-TRANSACTION-LEDGER-REFERENCE-001-A`：Transaction 从自有 durable `root + routeSnapshot + detail` 事实组解析唯一 `ledgerTransactionSn`，Ledger posting 再验证引用；B 的 core internal reader 与 C 的 Wallet bridge/`ledger-face` 依赖均未选择且不是 fallback。A 的选择顺序固定为 `source root/event -> frozen replay leg -> route participant coverage -> SUCCEEDED details -> distinct nonblank ledgerTransactionSn=1`：普通 referenced refund 选择 `PAY` 的非 `FEE` leg，standalone fee refund 选择 `FEE_CHARGE/FEE` leg，embedded fee refund 选择带费 `PAY` 的唯一 `FEE` leg且必须命中 `FEE_RECEIVER`。participant 的主体和 Money 与各自 route participant/leg 对齐，不要求不同 sibling 彼此相等；合法 principal/fee 差异不能被当成冲突。A 不新增稳定概念，不把 LedgerFactRef 嵌入或倒推为 ActionFact 的完成证据，也不引入自由分页、event string 搜索、Entity/Mapper 暴露或通用 evidence envelope。Ledger profile 另属 `MIG-05B` 决策，不与本引用问题合并。

本 Profile 选择同库按需投影，不新增异步物化视图、第二读库、缓存或事件订阅。只有真实性能/隔离指标证明现有确定性读取不够，且能同时冻结重建、版本、延迟和一致性合同，才重开新的 DSL/架构决策。

`W5-MIG05-TRANSACTION-LEDGER-REFERENCE-ENTRY-CARD` 不新增 DSL 概念。实现候选只能把既有 `referenceLedgerTransactionSn` 从 Transaction 自有事实组确定性解析后交给 converter；禁止引入 `LedgerEvidenceResolver`、通用查询 envelope、registry/factory、兼容 facade/V2 或新的 Public ref。缺失或冲突仍表达为 fail-closed/UNKNOWN，不能以 nullable 字段、contextVariables 或查询第一条形成运行时 fallback。该卡已 `ENTRY_CARD_INDEPENDENT_CHECKER_PASS / RED_EXECUTION_COMPLETE / RED_INDEPENDENT_CHECKER_PASS`；其 Green 在 `plan-r2.159` 当时执行，现已因测试合同 Checker NOT PASS 暂停，当前状态见 10.12.1。

#### 10.12.1 Green 测试合同返工边界

本次返工不改变任何稳定 DSL，只纠正测试对事实 Owner 的误读：Transaction 只从自身 durable `root + route + detail` 选择唯一 `LedgerFactRef`；Ledger 才能用 transaction/plan/entry/route-leg 证明该引用真实。额外未被 detail 引用的 Ledger 行不能反向改变 ActionFact 或原引用；后继 Ledger 引用污染必须由 Ledger ownership 校验拒绝；带费 `PAY` 的普通退款必须保留原 `PAY + FEE` route，只选择非 `FEE` replay leg。

不考虑兼容：不新增旧 Wallet 文案映射、Ledger 宽查询、bridge/facade/V2、双读或 fallback。测试合同返工保持 `Money`、original ref、route provenance、FundsEffect、LedgerFact 与 Balance 完成维度不变，并已通过独立 Checker；`plan-r2.162` 只作历史事实，当前入口见 10.12.2。

MIG-05 独立 Checker 最终判定 `PASS / 0 P0 / 0 P1 / 0 P2`。该阶段当时机械进入 `MIG-07_RECONCILIATION_STAGE_DOCUMENT_CARD / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.121`；当前活动状态见 10.13，不新增 Projection DSL，也不授权 Public API/DTO、DDL、Mapper、实现、测试或 Git。

#### 10.12.2 `MIG-05B` profile 引用与受控建账边界

`MIG-05B` 不新增资金 DSL。`LedgerProfileCode` 继续是账户事实中冻结的唯一 Public 模板引用；现有 `core.LedgerProfileSpec` 与 `LedgerProfileItemSpec` 错把内部 catalog 暴露成 DSL，必须连同 stable API baseline 一次删除。profile item、required ledger 集、normal side、period、allowNegative、settlement policy 和内部版本选择都不是跨 Owner DSL，也不得作为 Transaction 的动作准入语言。

无论 Human Owner 选择 A 或 B，稳定读法都相同：Wallet 只提交已准入的 `tenant + subject identity/type + currency + profile code + period`；Ledger 负责解释 profile、确保 required ledgers，并用同一内部 catalog integrity guard 在初始化与 posting/admission 两个边界复验 profile/version 与 bucket 快照；Transaction 不读取 profile、不自动建账。C 把 profile read/DTO 暴露给调用方，违反该边界。

本题不新增 `ProfileFact`、`LedgerAdmissionFact`、registry、factory、policy engine 或兼容 alias。Human Owner 已接受 A：复用 `LedgerService` 的受控初始化 surface，catalog/integrity guard 留在 `ledger-impl`，B/C 未选择且不是 fallback。Acceptance Checker、原 Entry Card Checker、三检查点返工卡 Checker、surface execution Checker 与 Green 返工卡 Checker 均已 `PASS / 0 P0 / 0 P1 / 0 P2`；首轮 RED Checker 的 `NOT PASS / 0 P0 / 2 P1 / 1 P2` 仅作历史，Behavioral RED 已在 `plan-r2.173` 完成并通过独立 Checker。首轮 Green 与 Green rework 未准出；方向测试证据返工、signed adjustment Entry/RED/Green 及外部资金腿最终门禁已在 `plan-r2.186` 关闭，当前活动状态见 10.12.8。

#### 10.12.3 `MIG-05B` Entry Card 的稳定读法

本卡仍不新增资金 DSL。唯一 Public 稳定语义是账户事实持有的 `LedgerProfileCode + profileVersion`，以及 `LedgerService` 接收已准入 subject/account snapshot 的受控初始化命令；请求字段仅承载 `tenantId + subject identity/type + currency + profileCode + profileVersion + optional effective period`，不暴露 profile items 或 catalog 决策。

命令无返回值：ledger identities 仍由既有 Ledger 查询契约按需读取，不能因旧测试消费 `Map<LedgerSubjectCode, Long>` 而新增 Public result。profile item、required bucket set、normal side、allowNegative、period、settlement policy、cutoff 与版本选择全部是 `ledger-impl` 内部 catalog 语义；初始化和 posting/admission 必须复用同一个 concrete guard，不新增 SPI、resolver 或第二服务。

无兼容迁移一次完成：旧 core profile spec、Wallet profile/initializer API/DTO/request 与默认实现全部删除；`LedgerProfileCode` 保留，capte-domain 两个测试宿主单独迁移装配。Entry Card Checker PASS 前不得形成 RED；任何新增 DSL 类型、结果 DTO、schema 或第二 catalog 文件都视为越界并停止。

三检查点不改变上述稳定读法。surface ownership move 已把既有能力迁到正确 Owner且 Checker PASS，不新增 DSL；behavioral RED 已验证 `LedgerProfileCode + profileVersion` 所指向的 required bucket 集、并发 identity、原子回滚与 catalog integrity 缺口；behavioral Green 也不能把内部 profile item、ledger id map 或 catalog 配置重新暴露为公共事实。该段描述的是 `plan-r2.173` 当时进入 Green 的历史，当前状态见 10.12.4。

#### 10.12.4 Behavioral Green 返工的稳定读法

首轮 Green Checker 的三个缺口不产生新 DSL。`LedgerProfileCode + profileVersion` 仍是唯一 Public profile 引用；实际 ledger row 与内部 catalog 的完整比对、测试 fixture 的 profile 一致性和测试事务宿主都只是 `ledger-impl/tests` 的实现证据。返工不得增加 `ProfileFact`、catalog DTO、结果 map、第二 service、SPI、schema、兼容 bridge 或 Transaction 对 Ledger profile 的读取。

同一个具体 `LedgerProfileCatalog` 必须成为初始化与 posting 的唯一内部语义来源；测试数据必须先满足该 catalog，再单独篡改目标字段证明 fail-closed。Funding/Credit 账户组回滚必须由既有真实事务管理器证明，不能把命令行临时属性或测试假象升级为公共契约。返工卡 Checker 已 PASS；当时的 Green rework Grant 已消耗且未准出，当前状态见 10.12.5。

#### 10.12.5 外部资金腿方向的稳定读法

本卡不新增 DSL。既有 `RouteLegType` 已足够表达方向语义；route source/target 是经济路径端点，不是 debit/credit，也不能被统一解释成 source 减少、target 增加。

1. 普通 `EXTERNAL_IN` 的 source 与 target 均为 `INCREASE`；普通 `EXTERNAL_OUT` 的 source 与 target 均为 `DECREASE`。
2. 普通内部 `INTERNAL_TRANSFER/HOLD/RELEASE/CONSUME` 以及无 `replayRefLegId` 的 ordinary `RESTORE/RELEASE` 保持 source `DECREASE`、target `INCREASE`；standalone refund 与 `PAYOUT_FAILED` 不因 leg 名称被强制当成反向回放。
3. 只有具有非空且唯一 `replayRefLegId` 的 reverse-class refund/reversal/release replay 才解析唯一原 posting，并对原 entries 逐项取反；不能只根据当前 `RESTORE/RELEASE` 或业务 event 推断。authorization completion 等非反向 successor replay 继续按当前 `CONSUME` 等稳定 leg 语义入账，不强制取反。
4. `ADJUST/LIMIT_ADJUST` 继续使用指令冻结的显式 effect，不纳入上述默认矩阵。
5. entry 的 debit/credit side 由 Ledger normal side 与 balance effect 推导；任一原 posting 缺失、多义、币种不一致或最终 plan 不平衡都必须 fail-closed，且零 LedgerEntry/Balance 效果。

因此不需要 direction strategy、registry、factory、新枚举、Public DTO 或 schema。唯一目标是修正既有 Ledger assembler 对稳定词汇的解释，并保持 original fact、route provenance、semantic digest、幂等 identity 与 LedgerFactRef 不变。

RED/Green 的执行阶段不属于 DSL：当前 RED 只用 assembler 四个方法证明两个外部方向缺口与两个既有不变量，15 个真实 caller 的 fresh 动态验证在共享 assembler 根因修复后的 Green 硬门执行。该分阶段不改变上述词汇、身份、引用或失败关闭合同。

#### 10.12.6 Green 证据返工不改变 DSL

Assembler 外部腿候选已使四个承重方法全绿；15 个 caller 的 `235/79F/2E/0S` 只暴露了 profile fixture、normal-side 测试算法与 explicit adjust 三个证据层问题，不产生新资金词汇。`FUNDING_MERCHANT` 已表达需要 `SETTLEMENT/CLEARING` 的商户账户；Ledger normal side 已决定 debit/credit 如何投影为余额增减；`ADJUST/LIMIT_ADJUST` 仍只使用指令显式 effect。

证据返工不得增加 profile alias、新 direction enum、event-name 特判、compat/V2/bridge/fallback，也不得把 `FUNDING_BALANCE_ADJUST` 归入 `EXTERNAL_IN/OUT`。它只允许使用已有 profile 和 normal-side 规则修正测试证据；explicit adjust 的物理 posting 闭合留给独立 Entry Card。

#### 10.12.7 `FUNDING_BALANCE_ADJUST` 的稳定读法

本卡不增加词汇。既有 `BALANCE_CONTROL + BALANCE_ADJUST + ADJUSTMENT`、指令上下文中的显式 `increase`、`Money`、route participant、platform `ADJUSTMENT` snapshot、Ledger profile 与 normal side 已足够表达该能力。

1. `increase=true` 冻结为 `INCREASE` effect；`increase=false` 冻结为 `DECREASE` effect。该 effect 同时作用于目标资金账户 `AVAILABLE` 与平台 `ADJUSTMENT`，不是 source/target 价值转移矩阵。
2. 平台 `ADJUSTMENT` 保持 debit-normal、有符号、允许负余额。调增后它增加，调减后它减少；正数调账量属于只读审计/统计解释，不是另一种 Ledger balance。
3. Ledger 按每个 bucket 的 normal side 把 effect 机械转换为 entry side：调增=`ADJUSTMENT DEBIT + AVAILABLE CREDIT`，调减=`AVAILABLE DEBIT + ADJUSTMENT CREDIT`；Money、currency 与 period 必须相同，plan 必须平衡。
4. 调减的目标 `AVAILABLE` 默认 `MUST_NOT_BE_NEGATIVE`；只有既有受控负余额策略事实明确授权时才可放宽。确定性拒绝可保留既有 FAILED funds fact 与解释 route，但不得形成成功 posting、LedgerEntry 或 Balance 效果。
5. 同一业务 identity 与同一摘要重放复用原事实；异 Money、方向、账户、币种、周期、来源或审计摘要冲突且零新增事实。

该稳定读法不改变 `LIMIT_ADJUST`、外部资金腿、ordinary internal leg 或 reverse replay，也不新增 direction enum、adjustment SPI、registry、factory、V2、兼容 alias、双写或 fallback。Human Owner 已接受方案 A；Green fresh=`4/0F/0E/0S`，扩大门禁与独立 Checker均 PASS。该切片在 `plan-r2.186` 关闭，不向当前 Entry Card 继承实现授权。

#### 10.12.8 余额调账非负 Public surface 的稳定读法

稳定资金 DSL 只需要 `account identity + positive Money + increase/decrease effect + business identity + source/evidence refs`。Consumer 自报的 `allowNegativeBalance`、负余额策略、风险状态、单笔/累计额度和账龄开始时间不是资金事实，也不能证明审批、额度占用或风险权威，因此不进入稳定 DSL 或 Public Request。

`FundsContextVariables.ALLOW_NEGATIVE_BALANCE` 与 transaction-face 的六个负余额 context keys 不是稳定扩展词汇，目标一次删除。`ReadonlyContextVariables` 中同名 raw key 也不得成为旁路：`BALANCE_ADJUST` 与 `LIMIT_ADJUST` decrease 的资金账户 entry 固定为 `MUST_NOT_BE_NEGATIVE`；平台 `ADJUSTMENT` entry 继续使用 Ledger profile 默认约束。Ledger 内部 profile 的 `allowNegative` 和 `LedgerBalanceConstraintType.ALLOW_NEGATIVE` 仍属于 Ledger 自身不变量与内部用例，不因本卡删除。

本卡不改变 10.12.7 的 signed `ADJUSTMENT`、Money、route、posting、LedgerEntry、Balance、幂等和审计引用。未来真实 Consumer 若需要负余额，必须先提供可查询的 authority、额度窗口、并发累计、撤销/到期和恢复证据，再重开独立 Contract Inquiry；当前不预设授权事实、策略引擎、SPI、DTO、schema 或兼容入口。

Green 实现已证明上述 DSL 收缩和 raw-context no-bypass；完整 capability 类的商户 profile 测试夹具错误与 core API 基线 `107/99 -> 105/97`、`getStatus -> getState` 漂移不产生新 DSL，也不重开本节资金语义。后续修复只能校准既有测试前置和 API 治理证据，禁止恢复本轮删除词汇、旧 profile spec、`getStatus()` 兼容方法或任何双入口。当前只登记 `GREEN_IMPLEMENTATION_VERIFIED / REPOSITORY_BASELINE_BLOCKED`，不冒充 `GREEN_INDEPENDENT_CHECKER_PASS`。

core 治理卡已用 superseding decision 将长期稳定规格统一为 `105/97/4/4 / 1043 baseline lines / getState()`，并把 `107/99/1062`、两个旧 profile spec 与 `ALLOW_NEGATIVE_BALANCE` 标为历史；卡 A、卡 B 与最终 Green 复验均已通过独立 Checker。当前只重冻迁移进度，不改变本节稳定 DSL 语义。

### 10.13 `MIG-07` Reconciliation Profile

MIG-07 不把 raw source、文件、provider status、业务状态或 stage policy 放入全局资金 DSL。它复用 `Money`、`StableIdentity`、不可变事实和版本化摘要，只在 `reconciliation-face` 语义域冻结以下最小概念；Java 名称和物理字段仍待未来 Contract Inquiry。

| 概念 | 是什么 | 不是什么 | 稳定不变量 |
| --- | --- | --- | --- |
| `ReconciliationSourceSnapshot` | Source Owner/Adapter 对一次来源范围、成员与覆盖度的不可变快照。 | 文件、API response、raw report 或 Reconciliation 对外部 authority 的猜测。 | carrier 无关；冻结 scope、source/rule version、snapshot version、member count/digest、coverage/watermark 与 evidence refs。 |
| `NormalizedComparisonFact` | `reconciliation-face` 接收的单个已归一比较事实。 | 全局 FundsActionFact、raw DomainOutcome、Ledger fact 或外部 finality。 | 含 source fact identity、comparison identity、Money、ComparisonStatus、结构性 `comparisonProven`、`claim kind + economic component + direction`、ComparisonRuleRef 与 semantic digest；同 identity 异语义冲突。只有 `comparisonProven=true` 才可进入 strict-exact。 |
| `ReconciliationScope` | 一次 run 的不可变比较边界。 | 可变 query filter 或 Gate 的隐式默认范围。 | tenant/object/account、time range/semantics/timezone、currency、sources、rule version、snapshot refs、coverage 必须完整。 |
| `ComparisonStatus` | 某一已签收 pair comparison rule 内、可跨两侧比较的归一状态。 | 跨 Owner 全局状态、领域 outcome 或完成证明。 | Source Owner/Adapter 只能按共同 ComparisonRuleRef 从本域 outcome 显式映射；只有结论明确且规则唯一命中时才同时给出 `comparisonProven=true`。UNKNOWN、没有映射、两侧 rule ref 不同、规则过期或多命中均不可比较；双侧 UNKNOWN 不因 code 相等而 Matched。 |
| `ComparisonRuleRef` | Pair Comparison Rule Owner 签收的共享比较语义引用。 | Adapter 自报的状态别名、全局规则引擎或运行时 fallback。 | 绑定 source roles/namespaces、DomainOutcome 映射、claim kind、economic component、direction、scope、effective period、owner 与 version；两侧必须引用同一有效规则。 |
| `GateRequirement` | Stage Owner 对一次 stage action 所需对账维度的不可变、版本化要求。 | Reconciliation policy engine、永久 PASS token、可选 pair 清单或资金指令。 | 明确 exact stage identity、scope/rule version、全部 required pair/source roles 与 evidence refs；同一 stage identity 同一时刻只能有一个 current/effective head。首包固定全部 required pair 必须通过，不开放 blocking policy。 |

现有 `ReconciliationRun`、`MatchResult`、`DifferenceCase` 和 `GateDecision` 继续使用以下合同：

1. run 只消费一个完整 `ReconciliationScope` 和所引用的 source snapshots；同 scope/snapshot/rule/digest 重放复用，任一语义变化新建 run/current lineage。
2. 首包匹配关系固定为 `1:1 strict exact`。两侧 comparison identity 各恰好一条、`comparisonProven=true`，Money 同币且相等，`claim kind + economic component + direction` 逐项相等，引用同一有效 ComparisonRuleRef 且 ComparisonStatus 相等，coverage 完整时才 Matched/Balanced；`UNKNOWN == UNKNOWN`、缺映射或多映射均不可比较。
3. aggregate fact 仍是一个 `NormalizedComparisonFact`，但必须由来源 Owner 预先冻结 member count/digest；Reconciliation 不持有 `1:N/N:1`、tolerance、netting 或 FX 算法。
4. MatchResult 永久保存 matched/missing/mismatch/conflict；DifferenceCase 仅增加运营跟踪，不覆盖 MatchResult。Difference `RESOLVED` 需要受控 action evidence 与后继 current Balanced run，`INVALIDATED` 需要正式 superseding evidence。
5. GateDecision 是对 exact stage identity 自动解析出的 current/effective `GateRequirement head + current run heads + blockers` 的当次计算证据，不是 Stage fact。caller 不能传旧 requirement ref；并行 head 直接阻断。Stage 消费时必须重新锁定 requirement head 和全部 run heads，并保存 requirement version、run identities/lineage 与 decision digest，之后 source/rule/lineage 变化不会改写历史消费证据。
6. `Matched`、`Balanced`、`Gate PASS`、Stage success、Ledger/Balance 和 external finality 是正交声明，互不推导。

身份与摘要分层：

- delivery identity 只定位一次载体交付；source fact identity 定位不可变归一事实；comparison identity 只用于本 rule/scope 下的配对。
- `semanticDigest` 覆盖 source namespace、identity、Money、ComparisonStatus、`comparisonProven`、`claim kind + economic component + direction`、ComparisonRuleRef/version 和归一语义版本。
- `evidenceBundleDigest` 覆盖 carrier/delivery/report/query/file/manual evidence associations。新增等价证据只追加关联，不改变 semantic fact 或其 digest。
- manual correction/aggregation/writeoff/decision evidence 不是旧 fact 的 update；它促使来源侧形成新 fact/snapshot，并由新 run 评估。

该 Profile 不新增全局 `core` 类型，不冻结 Java enum/DTO/API/table，也不建立兼容 alias、V2 平行契约或通用规则引擎。`ComparisonRuleRef` 是 `reconciliation-face` 语义引用，不批准 registry 或策略执行器。首轮 Checker 的两项 P1 已按上述最小合同返工，独立复核为 `PASS / 0 P0-P2`；本卡关闭状态为 `MIG-07_DOCUMENT_CARD_REWORK_CHECKER_PASS / DOCUMENTATION_ONLY / CODE_FREEZE / plan-r2.124`，全局当前入口见 OpenSpec。

### 10.14 Consumer 角色不进入资金 DSL

Consumer、模拟器和部署宿主是证据角色，不是新的资金对象，也不新增全局 `ConsumerType`、`HostType`、source engine 或场景枚举：

1. `capte-domain` 作为真实库 Consumer，只能通过已接受的 `*-face`/`core` 契约消费能力；它为未来 Reconciliation 提供目标调用场景和 E4 宿主，不把 Order/Coupon/Benefit 对象搬入公共 DSL。
2. `fincone` 作为设计与模拟 Consumer，只提供 BusinessFactRef、source pair、规则作用域和预期结果；模拟载体不是运行事实，也不成为 Reconciliation source 类型。
3. 可部署应用只负责运行装配和 L4，不改变 `ReconciliationSourceSnapshot`、`NormalizedComparisonFact`、`ComparisonRuleRef`、`GateRequirement` 的稳定语义。
4. 首个 Benefit 交接样例只验证既有 Profile 能表达“业务交接事实 vs 资金结果事实”的严格核对；场景名、文件格式、Java 类型和数据库均不进入 DSL。

因此 Contract Inquiry 可以在 L4 宿主出现前进行，但只能冻结最小公共物理契约；E4、L4 和生产准出继续分别提供证据，互不替代。角色校准 Checker 已 `PASS / 0 P0-P2`；该阶段当时进入 `plan-r2.128 / CONTRACT_INQUIRY`，当前候选与状态见 10.15。

### 10.15 Contract Inquiry 对稳定 DSL 的物理投影约束

Human Owner 已接受 `CI-MIG07-RECONCILIATION-001-A / PROVIDER_COMPUTED_STRICT_EXACT`，但不新增全局 DSL 概念。物理契约只能把 10.13 已接受的概念投影到 `reconciliation-face`，不得反向改变含义：

| DSL 概念 | 最小物理表达 | 基数/不变量 |
| --- | --- | --- |
| `ComparisonRuleRef` | `rule namespace + rule identity + rule version` 的结构化值。 | batch 与两侧 facts 恰好引用同一 ref；rule evidence 只由 fact/Requirement 外层承载；Provider 只比较引用和作用域，不执行厂商规则、状态映射或 registry 路由。 |
| `ReconciliationScope` | tenant、scope identity、pair identity、currency、window start/end、time semantics/timezone、ComparisonRuleRef、previous lineage。 | 一次 batch 一个 scope/pair/currency；Gate object 不属于 scope。 |
| `NormalizedComparisonFact` | source fact identity、comparison identity、正 Money、逐事实 `ComparisonRuleRef`、rule-scoped ComparisonStatus、`comparisonProven`、claim kind、economic component、direction、normalization version、evidence refs。 | source fact identity 在单侧唯一；batch 与两侧 fact 的 `ComparisonRuleRef` 必须完全一致；只有双方 `comparisonProven=true` 才可比较；同 identity 异语义冲突；多 carrier 证据先归一为一个 fact，不重复入列。 |
| `ReconciliationSourceSnapshot` | source role、logical source namespace、snapshot identity/version、coverage/watermark、facts、evidence refs 和 Provider 生成的 snapshot digest。 | 每个 batch 每侧恰好一个不可变 snapshot；一侧可空但必须显式完整覆盖，双方不能同时空。 |
| `ReconciliationRun` | batch/scope、两侧 snapshot refs/digests、Provider 计算的 MatchResult 集合、counts/outcome/result digest。 | 调用方只能请求执行，不能提交 match label、difference type 或 outcome。 |
| `GateRequirement` | exact stage identity、requirement identity/version/digest、required scope+pair identities 和 evidence refs。 | 由 Stage Owner 预先冻结；同一 stage identity 只有一个 current/effective head；check 按 stage binding 自动解析，不接受 caller requirement/run list。全部 required scope+pair 必须通过，不含 blocking policy。 |

`ComparisonStatus`、claim kind、economic component 和 direction 在物理层可以是由 ComparisonRuleRef 约束的稳定 code/value，但不得升级为跨 Owner 全局 enum。`comparisonProven` 是结构性准入标记，不是业务状态：只有共同规则能唯一、明确映射本域 outcome 时才为 true。Provider 先要求两侧均为 true，再做非空、作用域和逐项严格相等校验；其业务映射仍由 Pair Comparison Rule Owner 与两侧 Source Owner 负责。

已接受 A 只允许以下破坏式替换：已有 Batch/Run/Gate 服务边界可保留；`sourceItemRef + contentDigest` 输入替换为规范化事实；caller-submitted MatchResult 删除，由无业务载荷的 execute command 触发 Provider 计算；单 run Gate 输入替换为不可裁剪 GateRequirement。B/C 未选择且不是事实缺失或执行失败时的 fallback。禁止同时保留旧/new API、V2、alias、bridge、双读/双写、通用 matcher、tolerance/FX/netting engine 或 carrier-specific DTO。

共同 contract RED 已由 `plan-r2.145` 精准证明旧 caller assertion、单 run Gate 与缺 Requirement/strict-exact surface；`plan-r2.146` 文件卡 Checker 又证明它不足以约束已接受行为。Human Owner 明确不考虑任何兼容问题，`plan-r2.147` 无兼容三检查点文件卡已通过独立 Checker，`plan-r2.149` 已完成 contract surface hard break，`plan-r2.150` 已完成 behavioral RED/Green 并通过独立 Checker。当前只允许重新形成下一 W5 Entry Card，不批准继续修改 Java、表、测试或行为实现。

### 10.16 Source/Run Entry Card 的物理投影护栏

本卡不新增 DSL 概念，只冻结后续物理 Contract Inquiry 必须满足的最小映射：

1. `ComparisonRuleRef`、`ReconciliationScope`、`SnapshotCoverage` 和 `NormalizedComparisonFact` 必须是结构化 public value/request；不得退化为 `contextVariables`、JSON 字符串、`trusted=true` 或 raw provider payload。
2. Money 在物理层复用既有 `amount + CurrencyIsoCode` 约定；`ComparisonStatus`、claim kind、economic component、direction 是受 rule ref 约束的稳定 code，不升级为跨 Owner 全局 enum。
3. Provider 计算的逐笔结果使用一个有限 result kind，覆盖 `MATCHED / NOT_COMPARABLE / *_MISSING / CURRENCY_MISMATCH / MONEY_MISMATCH / 其他 *_MISMATCH / IDENTITY_CONFLICT`；caller 不能写 result kind、Difference type、severity 或 run outcome。异币种固定为 `CURRENCY_MISMATCH`，不做 FX 且不携带差额；只有同币种金额不等的 `MONEY_MISMATCH` 携带正的绝对差额 Money，并用 `REFERENCE / COMPARISON` 标明金额较大侧；其他 result kind 不携带差额字段。
4. Source fact、snapshot、match 和 run 的 identity/digest 都由 Provider 对冻结字段计算；逐事实 `ComparisonRuleRef` 纳入 source semantic digest，差额 Money 与较大侧纳入 result digest；carrier evidence 只进入 evidence bundle，不改变 semantic identity/digest。
5. 现有 `sourceItemRef + contentDigest` 和 caller `matchResults[]` 不能与目标 contract 并存。目标持久化只保留一套 source/snapshot/run/match 事实，不建立 V2、bridge、双读/双写或第二表族。
6. GateRequirement、Stage binding、Difference 运营分级和责任处置继续留给独立卡；它们不能反向改变本卡的 source fact 或 strict-exact result。

若后续 Java surface 无法在不引入通用 matcher、rule registry、carrier 分支或兼容层的前提下表达上述合同，Contract Inquiry 必须拒绝并返回本卡，不得以实现方便反改稳定 DSL。

Human Owner 接受的是上述最小物理映射及破坏式替换方向，不是新的 DSL 概念、兼容层或实现授权；独立 Acceptance Checker 已 PASS。该阶段当时要求先形成独立 GateRequirement Entry Card；该卡现由 10.17 承接，Gate 契约未关闭前不得进入 Source/Run RED。

### 10.17 GateRequirement Entry Card 的最小 DSL 投影

本卡不新增通用策略语言，只把 10.13 已接受的 `GateRequirement` 投影成四个最小稳定值：

| 稳定概念 | 最小 shape | 不变量 |
| --- | --- | --- |
| `GateStageRef` | `stageKind + StableIdentity stageIdentity`。 | 标识一次 exact Stage action；不同动作类型或不同业务实例不能共用。它不是订单、PayoutOrder 或资金交易的生命周期状态。 |
| `RequiredPairRef` | `scopeIdentity + pairIdentity + ComparisonRuleRef`。 | Requirement 中按 scope+pair 唯一，全部 mandatory；scope 与 rule ref 必须与 current run 完全一致。同一 pair 的不同对象/窗口/币种 scope 不可互换；没有 optional/blocking flag。 |
| `GateRequirementRef` | `stageRef + requirementIdentity + requirementVersion + semanticDigest + evidenceBundleDigest`。 | identity 由 Provider 生成；immutable；同一 stageRef 只有一个 current/effective head；新版本显式 supersede，旧版本只读。 |
| `ConsumedGateEvidence` | `stageRef + requirementRef + requiredPairEvidence[] + decisionDigest`。 | 只在 Stage action 成功提交的本地事务中固化；Pair evidence 数必须等于 Requirement required pair 数。它是历史证据，不是可再次使用的 PASS token。 |

`GateDecision` 是一次计算结果，不是新的 durable business fact。它至少分别表达整体 `PASSED/BLOCKED`、当前 requirement ref/digest、每个 required scope+pair 的 current run identity/lineage/result digest/outcome 和有限 blocker code。解释文案、Difference 详情和 evidence refs 可以随只读 DTO 返回，但不能成为 pass/fail 的隐藏输入。

Requirement semantic digest 覆盖 stageRef、requirement version，以及按稳定 scope+pair key 排序的全部 `RequiredPairRef`；Provider 生成的物理 requirement identity 不进入 semantic digest，避免首次并发重放因不同临时 identity 产生假冲突。evidence-bundle digest 单独覆盖冻结 evidence refs。两者都相同才是同版本幂等重放，任一不同即冲突。Decision digest 覆盖 requirement identity、两个 requirement digest和排序后的全部 current pair run/lineage/result digest 与判定结果；Stage consumed evidence 必须保存同一组 digest。

发布 Requirement 的稳定行为只有三种：首次发布；同版本两个 digest 均相同的重放；带 exact expected-current 的新版本追加并原子推进 head。业务唯一键固定为 tenant+stageRef+requirementVersion；首次并发由该唯一键冲突后一致性回读，后继版本再做 expected-current CAS。同版本任一 digest 不同、expected-current 不匹配、并行 head、空 Pair、重复 scope+pair 均冲突。首包没有 future-effective schedule、规则 registry、condition expression 或 fallback requirement。

`inspectGate(stageRef)` 可以在无锁只读快照下解释当前结果；`checkGate(stageRef)` 必须加入 Stage 写事务并重新解析/锁定唯一 requirement head 和全部 current scope+pair heads。任何 scope+pair 缺失、非 current、非 Completed/Balanced、coverage 不完整、rule 不符或仍有 blocker，整体 BLOCKED。Gate、Ledger、Balance、Stage 和外部 finality 继续正交；任何一层都不能反证另一层。

MIG-07 在 `plan-r2.150` 已完成 surface、behavioral RED 与 behavioral Green 并通过独立 Checker；该授权已经耗尽。本节稳定语义不变；MIG-04 已在 `plan-r2.232` 完成无兼容内部化并通过独立 Checker，当前活动状态只见本文顶部。

### 10.18 breaking Green 的物理映射约束

最终 breaking release 只能把 10.16/10.17 的既有稳定概念投影到 `reconciliation-face` 与 `reconciliation-impl`：`StableIdentity=ownerNamespace+value`、结构化 `ComparisonRuleRef`、`SnapshotCoverage`、`GateStageRef`、`GateRequirementRef` 和 `RequiredPairRef`。不得新增通用 matcher、registry、policy engine、V2、bridge、alias、双读双写或兼容 facade；`recordGateRequirement` 的最小返回为已接受的 `GateRequirementRef`，不再创造 Requirement DTO。最终 Java/schema/caller 白名单只在 TDD 20.22.5 列举，hard-break surface 与行为 RED 顺序只在 TDD 20.22.6 冻结；本节不建立第二白名单。

### 10.19 `MIG-03` Action、Ledger 与 Balance 闭合的 DSL 护栏

`MIG-03` 不新增“全局完成状态”。`FundsActionFactRef`、`LedgerFactRef` 与 Balance evidence 继续是三类正交引用；闭合能力只能沿稳定引用组合读取，不能把 Ledger/Balance 字段塞入 `FundsActionFactDTO`，也不能让当前余额或 root 状态反证某次动作完成。

未来文档卡只允许在以下语义内裁决物理合同：

- `proven-full/proven-partial` 动作按 action identity 找到与其 tenant、Money、事件和来源一致的完整 Ledger facts，再从 entries 得到必需 BalanceTarget，并读取同一已提交写链的投影证据。
- `proven-zero` 明确表达 Ledger/Balance 不适用；“未找到 Ledger”本身不能把未知动作推断为零效果。
- 缺失、多命中、跨 tenant、Money/币种/主体/route 不一致或投影不可读时保持 UNKNOWN/CONFLICT，零自动修复、零重做 action/posting。
- 查询结果是时点证据，不是可复用授权 token，不证明外部 finality、业务完成或 reconciliation Balanced。

当前只确认上述既有 DSL 边界与源码差距，不接受 Java service、DTO、枚举、表、缓存或物化视图；下一 Entry Card 必须先证明真实 Consumer 或 Provider 内部恢复用例，并由 Owner 选择最小承载模块。

### 10.20 `MIG-03` 首个 closure 切片的稳定 DSL 候选

Human Owner 已接受候选 A。A 不新增全局“闭合事实”，只约束三段引用关系：

```text
owner-qualified source action identity
  -> Transaction-recorded execution references
  -> Ledger-verified transaction / posting / entries
  -> atomic balance-projection commit invariant
```

- source action identity 在 Reconciliation 边界继续使用既有 `StableIdentity(ownerNamespace, value)` 形态，owner namespace 必须精确指向 Funds ActionFact；tenant 仍是独立 scope，不能编码进字符串。
- Transaction-recorded references 是 Transaction 自有耐久事实的只读投影，必须包含 ActionFact semantic digest 和完整 matched sibling set：principal、唯一 PAYEE、可选唯一 FEE_RECEIVER 的角色、Money、detail refs，以及全组唯一 distinct recorded LedgerTransaction ref。只返回 principal、漏掉 PAYEE/fee sibling、角色重复/交换或 sibling 指向不同 LedgerTransaction 都是 conflict。它们不是 LedgerFact，也不能因字符串存在就声明 Ledger 成功。
- Ledger verification 由 Ledger Owner 的现有事实完成，必须校验 transaction、posting plan、entries、Money、币种、主体和 digest；Reconciliation 只能从完整 sibling set 中唯一选择 PAYEE/CLEARING credit entry，FEE_RECEIVER 只参与完整性校验且不得误选。Reconciliation 不能接收 caller 提交的 LedgerEntry ref 后取 first/latest。
- Balance closure 在首切只表达“同一 Ledger 本地事务已提交 entries 与投影”。它不是余额数值、余额快照、授权 token或 reconciliation outcome；后续资金动作改变当前余额时，历史 action closure 不被覆盖。

`proven-zero` 对 Ledger/Balance 是明确不适用，不是 closure failure；但当前 clearing source admission 只接受 `primary + proven-full`，其他 effect/action kind 均 fail-closed。任何需要解析 `attemptRef` 字符串、读取 Entity/Mapper、把 `FundsActionFactDTO` 扩成 Ledger/Balance DTO、增加 balance evidence 表或引入兼容双入口的实现，都说明候选 A 尚不可执行，必须停止并返回合同裁决。

### 10.21 `MIG-03` Contract RED 的最小物理 shape

下列 shape 只是已接受 A 的 RED 目标，不是当前 Java 授权：

```text
FundsActionRecordedEvidenceQueryService
  findRecordedEvidence(FundsActionFactRef) -> Optional<FundsActionRecordedEvidenceDTO>

FundsActionRecordedEvidenceDTO
  actionFact: FundsActionFactDTO
  matchedSiblings: List<RecordedSiblingRef>
  recordedLedgerTransactionSn: String
  recordedReferenceDigest: FundsActionFactDTO.SemanticDigest

RecordedSiblingRef
  detailSn: String
  participantRole: RouteParticipantRole
  subjectId: String
  subjectType: String
  money: Money
  recordedLedgerTransactionSn: String

IdentifyClearingSplittableDetailRequest
  tenantId + sourceActionFactRef: StableIdentity
  + businessLine + splitPeriod + splitRuleCode + splitRuleVersion
```

`recordedReferenceDigest` 必须覆盖 action identity/action semantic digest，以及按稳定 role + detailSn 排序的完整 sibling 字段；所有 sibling 的 `recordedLedgerTransactionSn` 必须相同并等于 DTO 顶层值。DTO 不包含 LedgerTransactionDTO、LedgerEntryDTO、LedgerDTO、current balance、Entity、Mapper 或修复命令。

Clearing request 删除 `fundsTransactionSn`、`fundsTransactionDetailSn`、`ledgerEntrySn`，不保留 deprecated/V2/alias/context fallback。`sourceActionFactRef.ownerNamespace` 必须精确指向 Funds ActionFact；tenant 继续独立传递，不编码进 identity 字符串。

Contract RED 已按该 shape 执行：唯一反射测试类 fresh=`2/2F/0E/0S`，两项 assertion failure 分别证明 recorded-evidence service/DTO 尚不存在，以及 clearing request 仍保留旧 tuple。独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`；这只证明 DSL shape 当前缺失，不授权 Green 或扩展本节语义。

### 10.22 `MIG-03` Green 的精确语义

- `sourceActionFactRef.ownerNamespace` 固定为 `funds`，`value` 原样承载 `FundsActionFactRef.identity`；Reconciliation 只做显式类型转换，不解析 `attemptRef`、不从 root/detail/entry 猜 identity。
- `findRecordedEvidence` 首切只支持 exact direct PAY principal `primary / proven-full`。Provider 用自身投影确认请求 identity 等于 principal ActionFact；fee ActionFact、proven-zero、UNKNOWN、authorization、recovery 和 unsupported kind 返回 empty。
- `matchedSiblings` 按 `participantRole + detailSn` 稳定排序，完整包含 principal、唯一 PAYEE、可选唯一 FEE_RECEIVER；每项都携带角色、Money、subject 和同一非空 recorded LedgerTransaction ref。DTO 顶层 ref 必须等于所有 sibling ref。
- `recordedReferenceDigest` 使用 `SHA-256 / transaction.action.recorded-reference.v1`，覆盖 ActionFact identity/semantic digest、顶层 Ledger ref及排序后的全部 sibling 字段；Transaction 不读取 Ledger，也不把 Ledger/Balance DTO 放入该摘要模型。
- Reconciliation 通过现有 ledger-face 精确读取 LedgerTransaction 和全量 entries；若分页 count 与读取数量不一致、任一 sibling 无唯一对应 entry、posting plan 不存在、Money/subject/digest 不一致或 PAYEE/CLEARING credit 非唯一，则 fail-closed 且不持久化候选。Gate stage identity 直接复用 `sourceActionFactRef`。

上述语义不保留旧字段、alias、V2、bridge 或 context fallback。独立 Entry Card Checker=`PASS / 0 P0 / 0 P1 / 0 P2`；当时只进入 Human Owner Green Grant 决策。后续 Green Execution 未通过 Checker，当前由 10.23 接替。

### 10.23 `MIG-03` Ledger Persisted Digest 稳定 DSL

本节只定义 Ledger 内部可重建事实摘要的稳定语言，不新增 Public DTO、service 或调用参数。摘要值不是独立资金事实、授权 token 或外部 finality；它只证明一组持久化 Ledger facts 与 Ledger Owner 的当前唯一 canonical contract 一致。

```text
PersistedLedgerFactDigest :=
  SHA-256(
    domain,
    contractVersion,
    canonicalPersistedFields
  )

CanonicalLedgerTime :=
  transactionTime truncated to SECONDS

CanonicalDecimal :=
  BigDecimal.stripTrailingZeros().toPlainString()

VerifiedLedgerAggregate :=
  LedgerTransaction
  + ordered PostingPlans
  + ordered LedgerEntries
  + every persisted digest recomputed successfully
```

稳定不变量：

1. `CanonicalLedgerTime` 在进入摘要和持久化前只归一一次；writer、同 key replay 和 reader 使用同一值。它不从数据库回读结果猜测原 nanos，也不因数据库类型改变语义。
2. LedgerTransaction、PostingPlan 和 LedgerEntry 使用显式 domain/version；Map key 与成员排序固定，Money 固定为 amount/currency，时间使用归一后的 ISO 文本，`exchangeRate` 等 BigDecimal 使用 `CanonicalDecimal`。`1`、`1.0` 与 `1.00000000` 必须生成相同数值 token；不得使用数据库 scale 或 `BigDecimal.toString()` 直接承重。
3. transaction digest 覆盖 transaction header 与有序 posting/entry 语义；plan/entry digest 各自覆盖稳定身份和父引用。child 缺失、重复、父引用漂移或 digest 不一致均使 aggregate invalid。
4. `VerifiedLedgerAggregate` 是 Ledger read boundary 的前置条件。调用方仍收到既有 Ledger DTO；DTO 中的 `sha256` 不是让调用方自行选择算法或声明验证通过的输入。
5. `FundsActionRecordedEvidenceDTO.recordedReferenceDigest` 与 Ledger persisted digest 正交：前者证明 Transaction recorded sibling refs 未漂移，后者证明这些 refs 指向的持久化 Ledger facts 可重建。两个摘要都通过，仍不证明当前余额数值、外部到账或对账完成。

目标 canonical field set 至少包含：

- Transaction：ledger/funds transaction identity、tenant、instruction/event/type、business identity、Money/original Money、exchange rate、debit/credit totals、归一 transaction time、reference ledger transaction。
- PostingPlan：plan identity、父 transaction refs、route leg、intent/scope/effect/phase、Money、debit/credit totals。
- LedgerEntry：entry identity、transaction/plan/funds refs、ledger/period/subject、ledger subject/category、entry side/posting role/constraint、intent/scope/effect/phase、business identity、Money/original Money、exchange rate、归一 transaction time。

描述文案、日志字段、`contextVariables`、当前余额和展示状态不进入 persisted digest。若未来证明某个 context 字段是承重账务事实，应先提升为显式字段并重开 DSL 决策，不能把整个 Map 纳入摘要。

本卡选择 `ledger.persisted-transaction.v1 / ledger.persisted-plan.v1 / ledger.persisted-entry.v1` 作为唯一目标 domain/version，不兼容旧 `ledger.transaction.request` 或 `WindObjectDigestUtils` 结果。旧摘要不匹配即 fail-closed，不双验、不回填、不降级为 hasText 校验。首轮 Checker 指出的 exchange-rate scale drift 已由 `CanonicalDecimal` 关闭；最终 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。本节仍是文档合同，后续必须先取得 RED Entry Card 文档授权。

### 10.24 `MIG-03` Ledger Persisted Digest RED Shape

RED 不新增 DSL 类型，只用现有 test API/JDBC 对目标稳定语义施压：

```text
legacy stored digest
  -> same-key replay MUST reject

request canonical facts
  -> persist
  -> rebuild from stored transaction + plans + entries
  -> expected digest MUST equal stored digest

tamper(transaction | plan | entry)
  -> stable-SN Ledger read MUST reject
  -> clearing admission MUST reject before effects
```

失败分层固定为：

- `LD-RED-01`：legacy fallback 当前被接受。
- `LD-RED-02`：CanonicalLedgerTime + CanonicalDecimal round-trip 当前不成立。
- `LD-RED-03~05`：stable-SN read 对 transaction/plan/entry tamper 当前不拒绝。
- `LD-RED-06~08`：clearing 对 transaction/plan/entry tamper 当前不拒绝。

测试中的 expected canonical builder 只能机械实现 10.23 已冻结的字段、排序、time/decimal 规则，用于生成 expected/golden 或 fixture；不得成为第二生产算法、宽 Map 扩展点或兼容 fallback。Gate fixture 可以使用同一目标 canonical values，但 Gate tests 自身保持 Green。

RED 观察后，四个测试文件全部 immutable；Green 只能修改 `LedgerTransactionServiceImpl` 让上述失败自然转绿。若任何 failure 需要新 API、DTO、schema、helper production file 或修改 Reconciliation 生产代码，说明 10.23 合同不可由当前文件卡承载，必须停止重冻。

八个 RED 已按本节语义实际观察为 `56/8F/0E/0S`；transaction/plan/entry 参数、stable labels 和 Gate fixture 均未漂移。扩大门禁返工不新增 DSL 类型，也不改变 `CanonicalLedgerTime`、`CanonicalDecimal` 或 `VerifiedLedgerAggregate`：它只区分 `owned behavior proof` 与 `test-infrastructure observation`。non-assembler Ledger 的 `50/5F/0E/0S` 可证明当前公共 Ledger 缺口；assembler 的 Mockito/ByteBuddy `15E` 只证明当前环境未执行其行为，不能支持或否定 persisted digest 合同。

门禁分层经独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`，并以 `plan-r2.220` 收口；当前由 10.25 Green shape 接替。

### 10.25 `MIG-03` Ledger Persisted Digest Green Shape

Green 不新增 DSL 类型，只把 10.23 的三个 persisted v1 domain 落到同一个 Ledger internal builder family：

```text
NormalizedRequest
  -> materialize transaction + ordered plans + generated-SN entries
  -> digest(entry.v1)
  -> digest(plan.v1)
  -> digest(transaction aggregate.v1)
  -> persist atomically
  -> reload same facts
  -> verify same builders

ExistingSameKey
  -> reload + verify persisted aggregate
  -> bind request plans by plan SN
  -> bind request entries to existing generated identities by stable order
  -> compare with the same persisted v1 builders
```

transaction aggregate 的形状固定为 `{transaction, postingPlans:[{plan, entries}]}`；plan 按 `sn` 排序，entry 在各 plan 内按 `sn` 排序。transaction/plan/entry 字段集合与 10.23 及四个 immutable RED 的 expected builder 完全一致；`description`、`contextVariables`、日志、余额和展示状态仍不进入摘要。

禁止继续使用 `ledger.transaction.request`、`WindObjectDigestUtils` 或 `matchesCanonicalOrLegacyJson` 作为 persisted contract；不存在 dual digest、legacy fallback、自动回填或调用方选版本。现有 3 个 layer RED 已聚合覆盖全部 exact read surface，并经独立 Checker 判定 `PASS / 0 P0 / 0 P1 / 0 P2`；该补强不改变 DSL，原 Green Entry Card 已重新 PASS。该阶段以 `LEDGER_DIGEST_EXACT_READ_RED_COVERAGE_REWORK_INDEPENDENT_CHECKER_PASS / LEDGER_DIGEST_GREEN_ENTRY_CARD_INDEPENDENT_CHECKER_PASS / GREEN_EXECUTION_GRANT_NO / CODE_FREEZE / plan-r2.223` 收口，当前由 10.26 接替。

### 10.26 `MIG-03` Ledger 完整性与 Clearing 语义分层

Green 已证明 `VerifiedLedgerAggregate` 的 DSL 含义必须先于任何下游业务分类：持久化 transaction/plan/entry 的摘要、父引用、数量或唯一性不成立时，不存在可供 Clearing 继续解释的合法 Ledger fact。该顺序不增加新 DSL 类型，也不允许 `legacy digest`、fallback 或调用方声明“可信”。

4 个旧 Clearing 用例混合了两种不同输入：删除 plan 属于无效 aggregate，必须由 Ledger fail-closed；修改 business identity、subject/direction/role 后仍要测试 Clearing 分类，则测试数据必须使用同一 persisted v1 重新形成有效 aggregate。重新计算测试 fixture 摘要不是兼容路径，而是明确区分“事实完整性”和“业务适用性”。当前只进入该单测试文件 Entry Card 的文档授权门，Green 保持暂停。

### 10.27 `MIG-03` Legacy Clearing 测试迁移 DSL 约束

测试输入只允许落入两个互斥集合：`InvalidLedgerAggregate` 代表摘要、父引用、数量或唯一性不成立，必须在 Ledger exact read fail-closed；`VerifiedLedgerAggregate` 代表同一 persisted v1 可重建，才允许进入 Clearing 的 source identity、subject、ledger subject、entry side、balance effect 与 phase 适用性判断。该分层是测试语言，不新增 Public DSL 类型。

四个既有场景的归位固定为：missing plan 属于 `InvalidLedgerAggregate`；business identity mismatch、AVAILABLE/non-CLEARING、debit/decrease 属于 `VerifiedLedgerAggregate + ClearingIneligible`。后三者必须使用既有 `ledger.persisted-transaction.v1 / plan.v1 / entry.v1` builder 重算，不得添加第二摘要、legacy fallback、空摘要、调用方选版本或生产侧兼容分支。

独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`；该分层不新增 DSL 或兼容语义。该阶段以 `TEST_REWORK_EXECUTION_GRANT_NO / plan-r2.226` 收口，当前由 10.28 接替。

### 10.28 `MIG-03` Ledger/Clearing 两层语义实证

Test Rework 后，`InvalidLedgerAggregate -> Ledger fail-closed` 与 `VerifiedLedgerAggregate + ClearingIneligible -> Clearing exclusion` 两条路径均有 fresh 行为证据。该结果不新增 Public DSL 类型；`Verified` 只表示 persisted v1 可重建，不代替 business identity、ledger subject 或正逆方向适用性。MIG-03 Ledger Digest Green 已通过独立 Checker，当前只等待下一 W5 Entry Card。

### 10.29 `MIG-04` Transaction orchestration 不新增 DSL

本卡只收回错误 Public surface，不新增 action kind、PaymentInstrument DSL、Spend Rule DSL、route、LedgerFact 或完成状态。六个 command 的字段和行为保持不变，只从 wallet-face Public model 迁为 transaction-impl 内部 application command；Wallet 的 instrument/binding/capability/snapshot 与 SpendControlMovement 仍是稳定事实。不得建立 transaction-face V2、alias、bridge、双 Bean、双 command 或兼容反序列化入口。

独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`；该阶段当时只进入 Human Owner RED Execution Grant 决策，当前入口见本文顶部。

### 10.30 `MIG-05C` Ledger extension surface collapse 不新增 DSL

本卡不新增或删除 Money、Funds action、route、posting plan、LedgerTransaction、LedgerEntry、BalanceTarget、normal side、period、balance constraint 或 completion evidence 语义。`LedgerBalanceProjectionService` 是当前实现层扩展接口，不是稳定资金 DSL；删除它只把 `LedgerEntry -> balance projection` 的唯一解释责任收回 Ledger Owner。`CompositeLedgerPostingAssembler` 也是实现选择器，不是 posting grammar 或资金事实。

稳定 DSL 继续要求：每个 posting plan 独立平衡；entry 金额为正且币种明确；余额由同一 Ledger 写链按 entry side、normal side、period 与 constraint 派生；同 action identity 同摘要重放零新增，异摘要冲突；逆向使用原 route/provenance；投影失败整组回滚。删除扩展 router 不允许调用方提交 projector、选择账目、提供 posting entries 或用当前余额反推动作事实。

仍在 core 的 `LedgerPostingAssembler`、`LedgerTransactionPostingService` 和 posting specs 当前属于跨模块物理承接，不因本卡自动获得长期稳定 DSL 身份，也不在本卡删除。其高阶 command 替代方案必须另经 Contract Inquiry。返工只把 face-only Public Contract 目标校正为 `307/181/42`，并把 `LedgerPostingAssembler` 的 `supports` Javadoc comment-only 校正纳入第 19 个 MODIFY；不改变 DSL、签名、行为或增加兼容入口。执行与独立 Checker 均已通过，MIG-05C 当前范围关闭；下一 W5 切片必须重新形成 Entry Card，`EXECUTION_GRANT_NO / CODE_FREEZE`。

### 10.31 `CI-W5-MIG05D-LEDGER-POSTING-COMMAND-001` DSL 边界

本决策不改变 `FundsInstructionSpec`、`ResolvedRouteSpec`、`LedgerTransactionSpec`、`LedgerPostingPlanSpec`、`LedgerPostingPhaseSpec` 或 `LedgerEntrySpec` 的资金/账务语义。Posting specs 继续位于 core，负责表达单币种 Money、plan 独立平衡、phase、entry side、posting role、normal-side 约束和 route provenance；“类型可表达”不等于“任意 Consumer 可提交写入”。

候选 A 把写能力收口为一个高阶命令，候选签名仅用于 Owner 决策，不是已接受 Java 合同：

```text
LedgerTransactionPostingService.post(
  FundsInstructionSpec instruction,
  String fundsTransactionSn,
  ResolvedRouteSpec resolvedRoute
) -> String ledgerTransactionSn
```

该命令语义固定为：调用方提供已经归一的资金指令、由 Transaction 建立的非空 `fundsTransactionSn` 和已解析 route；Ledger 边界必须独立复验 instruction 与 route 的 `tenantId/businessScene/businessSn/instructionType/eventType/transactionType` 完全一致，不能只信任 `CompositeRouteResolver` 的上游调用顺序。Ledger Owner 在内部生成唯一 `LedgerTransactionSpec`、逐 leg PostingPlan/Phase/Entry，执行平衡、账目、period、原 route replay、幂等和余额约束，并在同一本地事务返回唯一 LedgerTransaction SN。调用方不能提交 ledgerId、entry side、normal side、allowNegative、posting plan、entry、digest 或任意上下文扩展来选择账务结果。

候选阶段曾把 `tenantId + fundsTransactionSn` 当作稳定账本命令身份；后续源码闭包证明 authorization COMPLETE/REVERSAL 会复用同一 root，因此该 root-only 候选已被 10.32 的动作级 identity 取代。canonical aggregate digest 仍只覆盖可重建的归一资金语义与冻结 route 稳定字段，不吸收 operator 对象、描述、解析时间或任意可变 Map。

候选 B 继续允许 `assemble(...) -> LedgerTransactionSpec -> post(spec)` 两阶段公共调用；候选 C 只把同一低阶 DSL 写入口移到 ledger-face。B 没有第二 production grammar/Consumer 证据，C 同时保留低阶写面并违反 Transaction 依赖方向，因此 A 为推荐，C 不是 fallback。

候选阶段原拟让 `FundsTransactionProjectionPublishContext` 只保留 Ledger SN；源码闭包进一步确认既有 `FundsInstructionLifecycleResult` 已持有该引用，因此 10.32 的已接受 A 直接删除 raw spec component，不新增重复字段。完整 posting/entry 事实由 Ledger read contract 查询，不随 Transaction projection event 复制。该变化不新增完成状态，不让 Ledger 判断业务成功，不把 ActionFact、LedgerFact 与 Balance 合并，也不改变失败时 Funds 生命周期归纳。候选阶段的 `accepted_answer=none` 已由 10.32 取代。

### 10.32 `CI-W5-MIG05D-LEDGER-POSTING-COMMAND-001-A` 已接受 DSL 合同

Human Owner 已接受 A，替代 10.31 的 `accepted_answer=none`。目标高阶命令的精确 core 合同冻结为：

```text
LedgerTransactionPostingService.post(
  FundsInstructionSpec instruction,
  String fundsTransactionSn,
  ResolvedRouteSpec resolvedRoute
) -> String ledgerTransactionSn
```

该命令是 Funds 内部跨模块写端口，不是宿主订单、rail 或人工补账 API。调用前提与结果如下：

1. `instruction` 与 `resolvedRoute` 非空；`instruction.tenantId` 非空；`fundsTransactionSn` 非空白。
2. Ledger 独立要求两侧 `tenantId/businessScene/businessSn/instructionType/eventType/transactionType` 六字段完全相等，不能依赖 RouteResolver 已检查或当前 caller 顺序。
3. 命令 identity=`tenantId + fundsTransactionSn + eventType + businessScene + businessSn`；其中 fundsTransactionSn 是 lifecycle root，后三项标识 root 下的 posting action。`ledgerTransactionSn` 使用 domain=`ledger.posting.command.identity` 的 canonical v1 SHA-256，格式固定为 `LE + first 48 hex`。
4. `LedgerTransactionSpec`、`LedgerPostingPlanSpec`、`LedgerPostingPhaseSpec`、`LedgerEntrySpec` 继续留在 core 表达稳定会计 DSL，但只由 ledger-impl 组装并作为 Ledger 内部写入模型；Consumer 不再提交这些 spec。
5. 同一 root 下不同 action identity 生成不同 Ledger SN；同 action identity 生成同一 SN。persisted aggregate digest 相同则返回该 SN 且零重复投影，digest 不同则冲突且 LedgerTransaction/plan/entry/Balance 零新增。唯一键竞争后必须回读同一 persisted aggregate，不能换 SN 重试。
6. 无 route legs 的零账务动作继续由 Transaction 在调用高阶命令前短路；Ledger 高阶命令不把零账务动作伪造成 LedgerTransaction。

Ledger identity digest 只决定持久化身份，不替代完整 aggregate digest。前者只吸收 tenant、funds transaction root、event 与 action business identity；instructionType/transactionType、Money、route/plan/entry 等语义由 aggregate digest 继续校验，用于区分同义重放和异义冲突。`WindOperator`、description、任意 context Map、resolvedAt 和投影结果不进入 identity digest；不得新增调用方自报 digest 参数。

`FundsTransactionProjectionPublishContext` 不再保存 `LedgerTransactionSpec`，也不新增重复 `ledgerTransactionSn` 组件；投影解释直接读取既有 `FundsInstructionLifecycleResult.ledgerTransactionSn`。ActionFact、Funds lifecycle、Ledger facts 与 Balance 仍是正交证据，Ledger SN 不代表订单成功、外部到账、清算完成或对账通过。

旧 `LedgerPostingAssembler` core type 与 `post(LedgerTransactionSpec)` 直接删除，不保留 alias、deprecated、overload、V2、bridge、双端口或 fallback。Contract Surface RED/Green 已按该无兼容合同执行并通过独立 Checker；当时只等待 `BEHAVIORAL_RED_EXECUTION_GRANT / CODE_FREEZE`，当前入口见本文顶部。

Surface Green 的 DSL 准出当时只证明写入能力已收口为高阶命令：focused=`61/0F/0E/0S`、Core API=`103/95/4/4 / 1036 lines`、Public Contract=`307/181/42`，投影不再承载 raw `LedgerTransactionSpec`。`LedgerTransactionSpec` 及 posting plan/phase/entry 仍是 Ledger 内部会计 DSL，没有被删除或迁出 core；当时动作级 identity 与确定性 SN 尚未实现，不能把 Surface PASS 当成重放/并发合同已兑现。

Behavioral RED 已把动作级 DSL 不变量落为 12 个真实调用：instruction/route 六字段必须相等，tenant 与 root identity 必须存在，同 action identity 必须稳定定位同一 Ledger SN，完整 aggregate digest 继续区分同义与异义，同 root 下不同 action 必须分离。fresh class=`30/12F/0E/0S`、focused=`73/12F/0E/0S`，每个失败都回链到已接受合同，没有新增 DSL 类型、调用方 digest、兼容形式或第二写链。

首轮 Checker 的技术裁决为 PASS，但普通 `just compile` 触发 Maven Snapshot 私有仓库访问，形成未单独授权联网的流程 P1。Human Owner 已接受并授权在权威链记录；后续未单独获联网授权时只允许 Maven `-o`。独立 recheck 最终=`PASS / 0 P0 / 0 P1 / 0 P2`；当时只进入 `BEHAVIORAL_GREEN_EXECUTION_GRANT_NO / CODE_FREEZE`，随后已由本文顶部 `plan-r2.254` 接替。

Behavioral Green 已兑现动作级 Ledger DSL：Ledger 在组装前复验 instruction/route 六字段并要求 tenant/root，`ledger.posting.command.identity` 只吸收 `tenantId + fundsTransactionSn + eventType + businessScene + businessSn`，输出 `LE + first48(sha256CanonicalJson(...))`。identity 只定位动作，既有 persisted aggregate digest 继续判定同义/异义；同一 root 的不同 action 由 event/business action 分离。factory 的无 root 重载与时序 SN 已直接删除，不保留 alias/V2/bridge；fresh focused=`73/0`、扩大去重=`638/0`，独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。该 DSL 能力只证明 Provider E2，不把 Ledger SN 外推为订单成功、外部到账、清算完成、对账通过或生产 finality。

### 10.20 重构进度重基线的 DSL 结论（历史）

当前稳定公共资金语言已经覆盖五类 canonical ActionFact：`primary`、`recovery/adjustment`、`authorize`、ordinary `complete` 与 authorization `release`；authorization `refund` 在缺少权威逐 complete allocation 时继续只保留 root-level 执行，canonical query 必须空/UNKNOWN。Ledger 高阶写命令、Action/Ledger/Balance 正交闭合与 Reconciliation strict-exact/Gate Provider 合同均已完成当前范围。

剩余变化不产生新 DSL：`MIG-06/08` 等真实 Adapter/Consumer 把外部证据归一为既有 `NormalizedExternalFundsFact` 并提供 E4，`MIG-09` 等 Consumer cutover 后删除旧入口。未出现新 authority、allocation、Consumer 或 zero-call 证据前，不新增 raw rail matrix、refund 推断规则、`MIG-05E`、兼容类型或第二事实源。

本轮独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`；该阶段只保留 `NEXT_SLICE_EVIDENCE_REQUIRED / EXECUTION_GRANT_NO / CODE_FREEZE`，不从已完成 DSL 推导新实现。该门现已由 10.33 承接，不再构成第二当前入口。

### 10.33 `MIG-08` Capte Benefit ActionFact Consumer 读法（历史 Entry Card）

本卡不新增 DSL。它只把已经存在的三个稳定概念在真实 Consumer 中归位：

- `businessScene + businessSn` 是 Consumer 与 Provider 共同持有的业务关联查询键，不是完成证据。
- `FundsActionFact` 是 Funds Owner 对单次资金动作结果与已证明资金效果的规范化只读事实；空集合表示未知，不能解释为零效果或成功。
- `benefitTransactionSn` 是 Coupon 保存的 Funds intent/执行引用；它与 `FundsActionFact.intentRef` 对齐，供后继动作引用，但自身不证明动作完成。

Benefit 首次出资和已有 reference 复用都只接受唯一 `primary / funds-transaction:succeeded / proven-full`，并要求 ActionFact `money == provenMoney == expected contribution Money`、`intentRef == benefitTransactionSn`。Benefit 退款只接受唯一 `recovery/adjustment / funds-transaction:succeeded / proven-full`，并要求其 `intentRef == refund returned transactionSn`、`OriginalFundsFactRef` 指向原 settlement ActionFact、allocated Money 与本次退款 Money 一致。

不存在、歧义、Money/币种不一致、`intentRef` 不一致、原事实引用不一致、`proven-zero`、failed 或 unsupported 都不能变成 Coupon 完成事实。空结果只表示 UNKNOWN，不授权重试、逆向或补单；只有 Coupon Owner 已成立且仍有效的核销/退款意图、冻结请求与原业务 identity 才能独立授权同 identity Provider 恢复。语义冲突必须停止并进入人工调查，不能建立主交易 + ActionFact 双判定、fallback 或兼容 facade。

ActionFact 仍不证明 Ledger、Balance、外部 finality、Reconciliation 或 Coupon 生命周期。Consumer 角色、Coupon 对象和 `COUPON_BENEFIT_*` 常量不进入 core DSL；独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`，本卡当时状态为 `ENTRY_CARD_INDEPENDENT_CHECKER_PASS / DOCUMENTATION_ONLY / CAPTE_CONSUMER_EXECUTION_GRANT_NO / CODE_FREEZE / plan-r2.257`。

### 10.34 `MIG-08` Capte Benefit ActionFact Consumer RED 收口（历史）

Consumer RED 未引入新 DSL，只以十个精准失败验证 10.33 已有词义：`benefitTransactionSn` 或主交易存在不能替代 ActionFact，ActionFact 缺失、歧义、`proven-zero`、Money/币种、`intentRef` 或原事实引用冲突都不能形成完成事实；refund root 存在也不能替代 recovery ActionFact。application=`57/8F/0E/0S`、boundary=`31/2F/0E/0S`，独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。

未来 Green 仍只复用既有 `FundsActionFactQuery/FundsActionFactDTO`，不增加 Coupon DSL、完成状态、Provider surface、identity 解析、双判定或兼容语义。独立重冻 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`；该卡当时进入 Green Gate，现由 10.35 承接。

### 10.35 `MIG-08` Capte Benefit ActionFact Consumer Green 收口

Green 没有新增或改写 DSL：`businessScene + businessSn` 仍只是关联键，`benefitTransactionSn` 仍是执行引用，唯一 `succeeded + proven-full` ActionFact 仍只证明本层 Funds 动作结果。Capte 已用这一既有词义关闭 settle、已有 reference、原 primary、existing recovery、root-only 和 refund returned intentRef 场景，目标源码中的旧主交易完成判定为零。

因此该结论只说明真实 Consumer 可以使用现有 DSL，不把 ActionFact 升级成 Coupon、Ledger、Balance、finality、Reconciliation 或全局 completion DSL，也不构成 E4 制品谱系证明；后续执行状态和验证明细统一见 [重设计执行规格](../../openspec/changes/funds-public-capability-redesign/spec.md)。
