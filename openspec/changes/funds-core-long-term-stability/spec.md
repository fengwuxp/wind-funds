# wind-funds core 长期稳定性 Goal 执行规格

## Change Metadata

| Field | Value |
| --- | --- |
| Change ID | `funds-core-long-term-stability` |
| Goal key | `CORE-STABILITY-20260804` |
| Runtime Goal thread | `019fc59d-b771-75b0-b5bc-6f9e366e30d8` |
| Goal state | `ACTIVE` |
| Current stage | `MIG08_R8B_CORE_API_GOVERNANCE_REBASE / EXECUTION_COMPLETE / VALIDATION_PASS / INDEPENDENT_CHECKER_PASS / CURRENT_SCOPE_COMPLETE / CODE_FREEZE` |
| Date | `2026-08-26` |
| Baseline HEAD | `ac9d1565` |
| Maker | `wind-funds` maintainer or assigned implementation Agent |
| Checker | 独立、可重复的测试/静态门禁；公共语义由对应 Owner 复核 |
| Git strategy | `summary_only`，未授权 stage、commit、push 或 PR |

## 1. Goal Card

### Objective

把 `core` 从“现有业务主链条件可用”推进为可独立验证、语义明确、摘要兼容且公共契约可演进的长期稳定资金内核；不通过大拆分或新增平行内核达成目标。

### Current Decision

- 当前业务主链可以继续使用，但 `core` 尚不满足稳定 `1.0` 公共契约冻结条件。
- `CORE-0A/B/C` 已通过独立 core 门禁、余额精确语义契约和 DIGEST-A 摘要迁移；`CORE-1B-A` 已补齐 ledger/detail persisted-legacy 持久边界证据；`CORE-1A` 已固化首轮公共 API 分级和 stable signature 门禁；`CORE-1E` 已完成 capability-first 包归位、默认实现下沉和 Benefit face 账户契约收敛；`CORE-1D / BOUNDARY-C/D` 已删除 route 会计成员和 wire 字段，由 posting/ledger 独立拥有会计效果，并通过扩大测试、PMD、完整 CAD 与独立 Checker 封板。
- 余额 View 已按 Profile 多态契约冻结，route/path 与 posting/accounting 目标边界已实现；其余摘要批次和 `D-CS-004` 时间语义仍存在长期风险，必须按独立 Owner Gate 继续验证。
- `core` 继续只承载跨能力稳定事实、值对象、不变量和引擎 SPI；场景编排、默认不可变实现、产品目录、应用服务和测试验证器按既有模块归属演进，不新建 Maven 模块。
- Owner 已批准当前 `1.0.1-SNAPSHOT` 直接按目标契约收敛，不保留旧 FQN、旧 getter、deprecated 适配、双写、双读或 V2 平行类型；该授权不降低资金平衡、幂等、回放依据和拒绝无副作用门禁。
- `D-CS-006-S` supersede `D-CS-006-R`：MIG-05C 已接受删除只有一个生产实现、没有真实变化轴的 `LedgerBalanceProjectionService`，当前 Core 直接冻结 `104 public / 96 stable / 4 experimental / 4 internal / 1039 baseline lines / FundsAccount#getState()`，不恢复旧类型、兼容接口或平行投影入口。
- Human Owner 已接受 MIG-05D A：Contract Surface Green 已无兼容删除 `LedgerPostingAssembler` 并把 `LedgerTransactionPostingService` 收窄为高阶命令；Behavioral Green 已补齐六字段复验、动作级确定性 SN、重放/冲突与并发收敛。当前物理基线为 `103 public / 95 stable / 4 experimental / 4 internal / 1036 lines`，Surface 与 Behavioral 均通过独立 Checker。
- `D-CS-006-U` supersede `D-CS-006-T`：MIG-08 R8B 已按 Consumer 归一、Provider 公共化原则无兼容删除场景枚举 `FundsBenefitFundingNature`；当前 Core 唯一物理基线为 `102 public / 94 stable / 4 experimental / 4 internal / 1025 baseline lines`。本次只同步 API 治理计数与稳定签名，不恢复 enum、alias、V2 或 Benefit Provider facade。

### Success Criteria

| ID | 完成条件 | 必须证据 |
| --- | --- | --- |
| `SC-CS-001` | `core` 契约测试不依赖 Spring ApplicationContext，`just test-core` 全绿。 | 聚焦 Red/Green、完整 `test-core` 报告。 |
| `SC-CS-002` | 原始余额 bucket 是唯一事实；含糊的通用聚合已移除或进入明确废弃窗口，产品聚合按 Profile 命名和定义。 | Product/Funds Owner 决策、余额桶契约测试、真实 Spring/H2 流程。 |
| `SC-CS-003` | 进入幂等、回放和审计的摘要有版本、规范输入和历史兼容策略。 | 固定样本 golden digest、字段顺序/空值/扩展字段测试、迁移说明。 |
| `SC-CS-004` | `core` 公共类型完成稳定/实验/内部分类，契约按 capability 归属，默认实现不进入 Core 构件。 | 公共 API 基线、包边界测试、Owner 决策记录。 |
| `SC-CS-005` | 跨系统事实时间不依赖宿主默认时区，历史事实可按已声明版本回读和重放。 | 时间契约、序列化回读和重放测试。 |
| `SC-CS-006` | route 只表达价值移动路径，posting/ledger 单独表达会计效果；现有场景保持可解释。 | 边界契约、posting 平衡、LedgerTransaction/LedgerEntry 追溯证据。 |
| `SC-CS-007` | 直接资金、支付工具授权/结算/退款、全球账户/ACH、清结算/对账四组场景无平行内核。 | 现有 H2 业务流、幂等/拒绝无副作用、余额逐步断言。 |
| `SC-CS-008` | 阶段门禁和全量 CAD 通过，独立 Checker 未发现未关闭 P0/P1。 | `compile`、`pmd`、`test-core`、`test-boundary`、相关业务流、`verify-cad`。 |

### Non-Goals

- 不创建独立 Spring Boot 应用、starter、Controller、RPC 或新的 Maven 模块。
- 不补齐 VCC、ACH、收单、跨境或外部清算轨道协议，只验证其归一化资金事实。
- 不连接独立 MySQL；公共能力库继续以 Spring/H2、DDL 静态契约和宿主责任边界取证。
- 不把仓库证据外推为宿主集成或生产验证，不替代法律、会计、税务或合规结论。
- 未单独授权前不执行 Git、联网、安装、发布、部署、生产或不可逆操作。

## 2. Decision Register

| ID | 决策 | 状态 | Owner Gate |
| --- | --- | --- | --- |
| `D-CS-001` | 保留一个跨场景资金内核，不为各产品复制账本、钱包或交易模型。 | `CONFIRMED` | 新场景只有证明现有稳定事实无法表达时才重开。 |
| `D-CS-002` | 余额汇总归属 View：`getAuthorizationBalance()` 精确表达 `AUTHORIZATION`；`getTotalBalance()` 由具体 `FundsAccountBalanceView` 按 Profile 实现；删除无稳定口径的 `pending` 和查询服务聚合。 | `CONFIRMED_VIEW_POLYMORPHISM_VERIFIED` | 原始 bucket 始终是唯一事实；新增其他 Profile 总余额实现必须重新裁决。 |
| `D-CS-003` | legacy golden + domain/version 有界 canonical v1；按持久边界 write-v1/read-legacy，不全局替换。 | `CONFIRMED_DIGEST_A_VERIFIED` | 后续批次仍需逐边界验证，不把 DIGEST-A 外推到 reconciliation。 |
| `D-CS-004` | `LocalDateTime` 到绝对时间的迁移必须定义时区来源和历史读兼容。 | `PENDING_OWNER` | Architecture Owner 批准迁移边界后实施。 |
| `D-CS-005` | route 只保留路径与来源证据；posting 独立拥有科目、期间、阶段、余额效果和约束，直接切换目标契约，不保留 hybrid route 或可恢复旧会计成员的领域/public snapshot reader。 | `CONFIRMED_BREAKING` | 实施必须分切片保持现有资金行为并通过 route/posting、回放、幂等和 H2 业务流；内部历史摘要核验可读取 generic map，但不得恢复旧 route 会计契约。 |
| `D-CS-006` | 2026-08-11 历史裁决：默认不可变 route/instruction 实现退出 Core 后，分类为 107 public 顶层类型、99 stable / 4 experimental / 4 internal。 | `HISTORICAL_SUPERSEDED_BY_D_CS_006_R` | 只保留为当时基线证据，不再代表当前 cardinality 或 stable signature。 |
| `D-CS-006-R` | 2026-08-22 superseding decision：在已接受删除两个错误公开的 profile spec、`ALLOW_NEGATIVE_BALANCE` 与旧 `FundsAccount#getStatus()` 后，当前基线为 105 public / 97 stable / 4 experimental / 4 internal、1043 行，并冻结 `FundsAccount#getState()`。 | `CONFIRMED_SUPERSEDING_REBASE` | `api-policy.tsv`、core Java 与其他 signature immutable；任何额外 diff 停止，不恢复旧类型、getter、alias 或兼容桥。 |
| `D-CS-006-S` | 2026-08-24 superseding decision：MIG-05C 删除无第二生产实现的 `LedgerBalanceProjectionService` 后，当前基线为 104 public / 96 stable / 4 experimental / 4 internal、1039 行。 | `CONFIRMED_SUPERSEDING_REBASE` | 只删除该接口四条 stable signature；`api-policy.tsv`、其他 core Java 与其他 signature immutable，不新增 alias、V2、bridge 或 concrete 公共替代。 |
| `D-CS-006-T` | 2026-08-24 superseding decision：MIG-05D A 无兼容删除 `LedgerPostingAssembler`，`LedgerTransactionPostingService` 保持 stable type 但收窄为 instruction + funds transaction identity + resolved route 的唯一高阶写命令；当前基线为 103/95/4/4、1036 行。 | `CONFIRMED_BEHAVIOR_IMPLEMENTED / INDEPENDENT_CHECKER_PASS` | Surface 删除 assembler 三条 signature 并替换 posting service 单签名；Behavioral 以 root + action identity 生成稳定 SN，复用 persisted aggregate digest 和现有唯一键。`api-policy.tsv`、其他 signature 与 schema immutable。 |
| `D-CS-006-U` | 2026-08-26 superseding decision：MIG-08 R8B 删除只表达 Consumer Benefit funding nature 的 `FundsBenefitFundingNature` 后，当前基线为 102 public / 94 stable / 4 experimental / 4 internal、1025 行。 | `CONFIRMED_SUPERSEDING_REBASE / VALIDATION_PASS / INDEPENDENT_CHECKER_PASS` | 只删除该 enum 的 11 条 stable signature 并机械校准治理脚本；`api-policy.tsv`、其他 core Java/signature 与 schema immutable，不恢复兼容 enum、alias、V2 或场景 facade。 |
| `D-CS-007` | Core DSL 是资金主链内部规范事实和引擎 SPI，不是业务直接拼装 SDK；契约按 transaction/route/ledger/wallet/fx capability 归属，业务调用方使用 `*-face` 和稳定叶子值对象。 | `CONFIRMED_VERIFIED` | 禁止顶层 `com.wind.funds.spec` / `model`、Core public `Immutable*`、`com.wind.funds.dsl` facade、兼容桥和 V2 平行类型图。 |

### 2.1 Balance Semantics Evidence

- `pending` 是业内常见但依赖上下文的状态名：Stripe Balance 用它表示尚未结算为 available 的入账，Stripe Issuing 用它表示已批准待 capture 的授权，Adyen 用它表示未来结算交易，Modern Treasury 则把 pending ledger entry 纳入 pending balance；因此不能把裸 `pending` 固化为跨产品统一金额口径。参见 [Stripe balances](https://docs.stripe.com/payments/balances)、[Stripe Issuing authorizations](https://docs.stripe.com/issuing/purchases/authorizations)、[Adyen balance types](https://docs.adyen.com/platforms/balance-types/) 和 [Modern Treasury ledger balances](https://docs.moderntreasury.com/ledgers/docs/transaction-status-and-balances)。
- `FundsAccountBalanceView` 旧 Javadoc 曾把 `pending` 描述为清算、退款和跨行转账在途，但实际实现只返回 `AUTHORIZATION`；正式名称收敛为 `getAuthorizationBalance()`，无稳定口径的 `getPendingBalance()` 直接删除。
- `AUTHORIZATION` 是授权占用；`CLEARING` 是商户待清算责任；`SETTLEMENT` 是已锁定内部结算责任；`IN_TRANSIT` 当前尚未启用。它们不能无条件合并成一个跨 Profile 的 `pending`。
- `FUNDING_BASIC`、`FUNDING_MERCHANT`、`CREDIT_BASIC`、`FUNDING_PLATFORM` 的账目集合、类别和正常余额方向不同；单一 `total` 不能同时代表客户资产、信用额度、商户结算责任和平台资产负债。
- 当前仓库及 `/Users/wuxp/Workspace/idea` 下其他本地源码没有 `getPendingBalance()` 消费者；权威查询已经能通过 `getBalanceBuckets()` / `getBalance(LedgerSubjectCode)` 返回原始投影。
- 历史 Snapshot 曾公开旧余额方法；Owner 已明确本项目不存在兼容责任，本轮直接修订目标契约，不增加迁移适配层。

`D-CS-002` 最终裁决：`FundsAccountBalanceView` 暴露 `getLedgerProfileCode()`、精确的 `getAuthorizationBalance()` 和抽象的 `getTotalBalance()`；总余额口径由具体 View 实现。当前 `ImmutableFundsBalanceView` 只为 `FUNDING_BASIC` 定义 `AVAILABLE + FROZEN + AUTHORIZATION`，其他 Profile fail-closed；查询服务不再重复解析 Profile 或承担余额公式。`getTotalBalance()` 通过 `@JsonIgnore` 排除出通用 wire JSON，对外总余额只能由已明确 Profile 口径的 DTO 显式映射。无稳定口径的 `getPendingBalance()` 直接删除。`CLEARING`、`SETTLEMENT`、`IN_TRANSIT` 和信用额度均不进入 `FUNDING_BASIC` 总余额，其他产品继续按原始 bucket 查询，直到对应 Owner 批准具体 View 口径。

首轮 Checker 发现非 BASIC View 会因 Jackson 隐式调用 fail-closed `getTotalBalance()` 而使整份余额序列化失败；现已用公共 `@JsonIgnore` 和 basic/non-basic JSON 契约测试关闭。Profile 同步增加构造期非空约束，契约测试锁定 profile accessor、查询服务无聚合和 wire 边界。复核 Checker 确认 `P0=0`、`P1=0`。仍保留一个非阻断 `P2`：`ImmutableFundsBalanceView` 的 `balanceBuckets` 是可变本地快照，但不反写账本；该项属于 `wallet-impl` 行为契约，不阻断本轮 `core` public signature 门禁，应在 Core-2 兼容与场景封板前关闭。原查询服务聚合导致的两次账户查询已随职责回归 View 一并消除。

### 2.2 Stable Digest Evidence

- `FundsStableHashSupport.sha256Json` 有 33 个生产调用，分布在 17 个调用文件：ledger 1 个、transaction 7 个、reconciliation 25 个。
- 钱包 `SpendRuleEvaluationApplicationServiceImpl` 另有 1 条绕过公共 helper 的 `sha256(WindJson.toJsonString(TreeMap))` 路径；该摘要进入对外 DTO，随后持久化并参与决策一致性比较。计入后共有 34 条 `WindJson` 相关摘要生成路径、18 个调用文件。
- 多个 64 位 digest 直接进入唯一键、活动占用键、请求一致性或幂等判断；当前 DDL 没有统一的 `hash_version/digest_version`。
- `fastjson2 -> wind-jackson` 迁移只替换了 `sha256Json` 的序列化器；现有专属测试只证明 Map 插入顺序相等，没有固定历史 digest。
- 使用本地旧 `fastjson2 2.0.62` 和当前 `wind-jackson 4.0.0-SNAPSHOT` 对照：`TreeMap + enum + BigDecimal`、`List<Map>`、嵌套 JSON 字符串三类现有典型输入文本一致；普通 record 的属性顺序不一致。该证据只证明代表性输入，不能外推全部任意对象。
- `WindJson` 的全局 mapper 可被宿主替换或重新配置，因此 `sha256Json(Object)` 不能把全局序列化配置当作长期摘要协议。
- governance 的投影差异脱敏摘要使用 `sha256(value.toString())`，不依赖 JSON，也不参与上述业务幂等协议；本轮只登记为独立证据格式，不并入 canonical JSON 迁移。

`D-CS-003` 推荐裁决：保留现有 `sha256(String)`；把 `sha256Json(Object)` 及钱包直连实现定义为 legacy，先补旧/新一致样本的 golden digest。新增 canonical v1 时只接受受控的 Map/List/标量词汇，递归排序 Map key、固定 enum/number/null 表达，并把 `domain + v1` 放入摘要前像，结果仍保持 64 位；按持久边界逐批迁移，历史比较期同时计算 legacy/v1，不一次性替换全部路径，也不扩建通用 JSON 框架。

| 迁移批次 | 范围 | 当前路径数 | 最小兼容约束 |
| --- | --- | ---: | --- |
| `DIGEST-A` | ledger request hash、transaction 请求/冻结/支付工具/支出消费 | 8 | 先固定 legacy golden；涉及请求一致性和幂等的字段必须双算、兼容旧值。 |
| `DIGEST-B` | clearing detail/candidate/batch/split batch | 9 | 活动批次和快照唯一键不能原地换值；新建事实写 v1，历史事实按 legacy 比较。 |
| `DIGEST-C` | settlement、payout、recovery | 10 | 订单/证据摘要保持可重放；按聚合边界逐类迁移。 |
| `DIGEST-D` | reconciliation batch/run result/evidence support | 6 | 来源、身份和结果摘要分别固定样本，不把一种 domain 的摘要复用于另一种 domain。 |
| `DIGEST-W` | wallet spend-rule decision digest | 1 | 先复用同一 canonical 入口替代私有实现；DTO/持久记录比较期继续接受 legacy。 |

`DIGEST-A` 实施范围：`ledger.transaction.request`、`transaction.detail.request`、`transaction.frozen-order.request`、`transaction.external-funds-fact`、`wallet.spend-control.reservation`、`wallet.spend-control.consumption`、`wallet.spend-control.release` 和 `wallet.spend-control.refund-compensation`。canonical v1 只接受 String-key Map、List、Money、LocalDateTime 和受控标量；`FeeSpec`、`MerchantInfoRequest` 在交易明细边界显式投影为字段 Map，未放开任意 POJO 序列化。新事实写 v1，已持久化 legacy 值只在同一业务事实的精确双算结果命中时允许重放。

### 2.3 Public API Baseline Evidence

- 初始源码曾有 121 个 public 顶层类型；2026-08-11 将其中 14 个 public `Immutable*` 默认实现下沉到 `transaction-impl` 后形成 107 个 public 顶层类型。2026-08-22 按 MIG-05B 删除两个错误公开的 profile spec，2026-08-24 按 MIG-05C/MIG-05D 删除单实现投影接口与 assembler，2026-08-26 按 MIG-08 R8B 删除场景枚举；当前 Core 为 102 个 public 顶层类型和 1 个 package-private 顶层 class entry。
- 使用 JDK `javap -public` 并在类型内规范化排序后，当前 94 个 stable public 类型形成 Owner 批准基线。测试专用 `FundsDslJsonContractVerifier` 和默认不可变实现均不进入 Core stable baseline；Jar checksum 不同只证明构件字节不同，不能替代 signature 比较。
- 两个早期 `jdk21` Snapshot 各有 131 个顶层 class entry；相对当前存在 8 个类型移除、2 个类型新增，其中 public 类型为 6 个移除、2 个新增，并伴随 `FundsInstructionSpec#getOperator` 类型、record canonical constructor、账户层级快照和枚举值变化。它们只能证明历史上发生过破坏性演进，不能作为当前冻结基线。
- 仓库没有 japicmp、Revapi 或等价自动兼容门禁；`core` 也没有统一的稳定/实验/内部注解，现有 `@Deprecated(forRemoval = true)` 只覆盖一个旧交易类型判断方法。

当前批准分类以“例外清单 + 默认 stable”穷举全部 102 个 public 顶层类型：

| 分类 | 数量 | 类型范围 | 冻结条件 |
| --- | ---: | --- | --- |
| `STABLE` | 94 | 除下方 8 个例外外的全部 public 顶层类型。 | 仓库内 baseline 冻结批准的 public signature；差异必须 fail closed 或有 Owner 批准的直接切换记录。 |
| `EXPERIMENTAL` | 4 | `CreditFundsAccountType`、`FundingAccountType`、`ExternalFundsAccountType`、`DefaultFeeType`。 | 4 个枚举只有测试证据、无本仓生产消费者，本轮不冻结。 |
| `INTERNAL` | 4 | `FundsBenefitLedgerEffect`、`LedgerNormalBalanceUtils`、`MerchantInfoSpec`、`UserWalletFundsAccountType`。 | 无跨模块生产消费者或只在 core 内部自用。本轮不冻结，也不据此授权删除。 |

成员级裁决如下：

- `FundsAccountBalanceView#getLedgerProfileCode/getAuthorizationBalance/getTotalBalance`：按 `D-CS-002` 的 bucket/profile 边界进入 stable baseline；查询服务聚合与含糊 `pending` 不属于公共契约。
- `FundsStableHashSupport#sha256Json`：受 `D-CS-003` 约束；`sha256(String)` 已独立进入 stable baseline。
- `FundsInstructionSpec`、ledger fact、route snapshot/replay、`LedgerBalanceBucket`、`SettlementPolicySpec` 和 `AccountBalancePeriodType` 中暴露 `LocalDateTime` 或读取宿主时钟的成员：受 `D-CS-004` 约束。
- route/spec 与 `LedgerPostingAssembler`、`LedgerTransactionPostingService` 的路径/会计效果边界成员：受 `D-CS-005` 约束。

`D-CS-006-S` 是 MIG-05D Surface 执行前的历史基线：104/96/4/4、1039 行；`D-CS-006-T` 是 MIG-08 R8B 执行前的历史基线：103/95/4/4、1036 行。二者继续作为迁移前证据，不再代表当前物理 API。

`D-CS-006-U` 当前裁决：MIG-08 R8B 无兼容删除 `FundsBenefitFundingNature` 的 11 条 stable signature，Consumer funding nature 继续由 Capte 自持，不进入 Core。当前物理基线为 `102 public / 94 stable / 4 experimental / 4 internal / 1025 baseline lines`；`api-policy.tsv` 的 4/4 分类与 20 个成员例外不变，Core API 门禁只机械同步 cardinality 与消息。

正式执行证据：

- `core/api-baseline/api-policy.tsv` 继续固化 4 个 experimental、4 个 internal 和 20 个精确成员例外；其余 94 个 public 顶层稳定类型及继承 stable 分类的 public nested 类型生成 1025 行 stable signature baseline。`LedgerBalanceProjectionService`、`LedgerPostingAssembler` 与 `FundsBenefitFundingNature` 已分别按 MIG-05C/MIG-05D/MIG-08 R8B 删除；posting service 只保留高阶单签名，Consumer funding nature 不再进入 Core。
- `scripts/verify-core-api-baseline.sh` 仅使用 JDK `javap -public` 和系统命令，校验 102/94/4/4 数量、public nested 继承分类、重复或失效分类、重复或失效成员例外，并对规范化结果做精确 diff；`just verify-core-api` 强制先执行 clean compile，只有显式 `--update` 才能重建批准基线，未增加构建或运行时依赖。
- `Justfile` 增加独立 `verify-core-api` 门禁，并在 `verify-cad` 的 clean compile 后、全量测试前执行。缺少 policy 的 RED、正式 baseline 的 GREEN 和删除一行 baseline 的 drift probe 均按预期 fail closed。
- `getLedgerProfileCode()`、`getAuthorizationBalance()`、`getTotalBalance()`、`FundsAccount#getState()`、`LedgerPostingIntentType.RELEASE` 与 `LedgerPostingScope.CONTROL_RELEASE` 已进入 stable baseline；route/path 与 posting/accounting 目标归属已冻结，`sha256Json` 和时间/宿主时钟成员继续按各自 Owner Gate 排除；具体 `Immutable*` 和两个旧 profile spec 已退出 Core 构件。
- Java 21 的 21 模块 clean compile、本轮聚焦 58/58、`test-transaction` 149/149、`test-boundary` 199/199、`test-business-flow`、PMD 和当前共享工作区完整 `verify-cad` 均通过；完整 CAD 为 112 suites / 1104 tests / 0 failures / 0 errors / 1 expected MySQL skip，API、public contract、classfile 和 codegen 门禁通过。
- `CORE-1E` 独立 Checker 确认 nested signature、standalone fresh-build 和 Benefit 幂等证据缺口均已关闭，结论为 `PASS`，`P0=0 / P1=0 / P2=0`。门禁覆盖 stable public 顶层及其 public nested classfile signature，不覆盖 Javadoc、注解值、参数名、常量值、JSON/wire 行为和运行时语义；这些继续由相应契约测试负责。

`D-CS-006-R` 执行证据（2026-08-22）：执行前 `just verify-core-api` 完成 Java 21 clean compile 21/21 后精确 RED=`Expected 107 public top-level core types; found 105`。三文件重基线只把脚本 cardinality/message 改为 `105/97`、stable baseline 的 `FundsAccount#getStatus()` 改为源码既有 `getState()`，并用本 superseding decision 消除当前/历史双权威；`api-policy.tsv`、core Java 与其他 signature 未修改。Green `just verify-core-api` 再次 clean compile 21/21，并输出 `Core API baseline verified: 97 stable, 4 experimental, 4 internal public top-level types; public nested signatures included`；baseline 保持 1043 行。当前只进入独立 Checker，不预写 Checker PASS，也不授权最终 Green 复跑、Git 或发布。

### 2.4 Time and Replay Semantics Evidence

- `core` 有 14 个 public 类型直接暴露或使用 `LocalDateTime`。`FundsInstructionSpec.eventTime` 依次成为 route `resolvedAt`、ledger `transactionTime` 和 projection event 时间，属于跨模块事实链，不是单纯内部实现类型。
- RouteSnapshot v5 把 `resolvedAt/expiresAt` 写成无 offset 的 `LocalDateTime.toString()`，读取时使用 `LocalDateTime.parse()`；旧快照缺失 `resolvedAt` 时回退订单 `gmtCreate`。当前回放只接受 `FundsRouteCodes.CURRENT_ROUTE_SNAPSHOT_SCHEMA_VERSION`，尚无按版本读取和迁移能力。
- ledger transaction/entry 把事实时间落入无时区 `DATETIME`；测试构建未固定 `user.timezone`，现有 RouteSnapshot JSON 测试也没有断言时间往返、历史 zone 或跨宿主时区结果。
- `FxRateSnapshot.observedAt` 已使用 `Instant`，证明 core 现有依赖可以表达绝对时间；reconciliation batch/settlement policy snapshot 已保存并校验 IANA `timezoneId/timezone`，业务日历时区已有明确 owner，不需要在 core 新建平行时区框架。
- `LedgerBalanceBucket#isActive()` 和 `AccountBalancePeriodType#formatPeriodId()` 直接读取宿主 `LocalDateTime.now()`；`SettlementPolicySpec` 虽接收显式 `now`，但默认 holiday calendar 是进程级可变静态状态，测试必须使用 `ResourceLock` 并在 finally 中恢复。
- 除 core 的 2 个直接 `now()` 外，wallet、transaction、reconciliation 当前分别有 9、15、35 个 `LocalDateTime.now()` 调用。它们不要求一次性重写，但说明只改 core 类型不能证明端到端可重放。

`D-CS-004` 推荐裁决：按语义而不是按统一类型迁移。

| 迁移批次 | 时间语义 | 最小目标 | 兼容与验证边界 |
| --- | --- | --- | --- |
| `TIME-A` | 纯函数和周期 helper | 新增显式时间参数的 `isActiveAt`、`formatPeriodId(at)`；结算计算只保留显式 `now + holidayCalendar` 为稳定入口。 | 旧无参方法和全局 calendar 入口先废弃一个 Snapshot；测试不再依赖宿主时钟或全局状态。 |
| `TIME-B` | RouteSnapshot/replay 事实 | 新 route snapshot schema 写 ISO-8601 `Instant`；reader 按 schema 读取 current + legacy。 | v5 无时区值必须由宿主提供 IANA `legacyFactsZone` 后转换；缺少映射时 fail closed，禁止使用 `systemDefault()` 或静默当 UTC。 |
| `TIME-C` | instruction/ledger/projection 事实链 | 在首个稳定 `1.0` 前协调切换为 `Instant`，同一事实沿 instruction -> route -> ledger -> projection 保持同一时刻。 | 这是批准后的破坏性 Snapshot 边界；H2 证明 JSON、持久化、查询范围和重放在至少两个宿主默认时区下结果一致。 |
| `TIME-D` | owner 模块发生时间 | 请求已有权威发生时间时原样传入；系统生成时间使用注入的 JDK `Clock`。 | 按 wallet/transaction/reconciliation 分批替换 59 个直接 `now()`，不引入自定义 Clock facade，不把批次扩大成一次性重构。 |
| `TIME-E` | settlement/reconciliation 业务日历 | `LocalDateTime` 只表示指定 IANA zone 下的墙上时间，zone/cutoff/holiday calendar 继续由 owner policy snapshot 管理。 | 补 DST gap/overlap、cutoff、节假日和跨日测试；转换为资金事实前必须得到唯一 `Instant`。 |

历史数据只有两种可接受入口：宿主提供数据集对应的 `legacyFactsZone` 并完成可审计转换，或 Owner 明确声明旧 Snapshot/测试数据不承担回读责任并在批准的破坏性边界清除。仓库证据无法替宿主推断历史时区，因此 `legacyFactsZone` 未裁决前不实施 `TIME-B/C`。

### 2.5 Route / Posting Boundary Evidence

- `RouteLegSpec` 已只保留路径、金额、参与方和回放来源，删除 `balanceEffectType`、`phaseCode`、`periodType/periodId` 和 `constraintOverrides`；`RouteNodeSpec` 已删除 `ledgerSubjectCode`。默认不可变实现、route resolver 和新写 RouteSnapshot JSON 同步移除这些字段，不保留旧 getter、双写或兼容 API。
- `DefaultLedgerPostingAssembler` 已成为会计语义 owner：新交易按 instruction 事件/类型、route path、参与方角色、账户 Profile 和账本查询推导科目、期间、阶段、余额效果、约束、借贷方向及 posting scope；费用腿以 `legId=FEE` 作为强身份并校验目标为平台费用账户，`FEE_RECEIVER` 角色只用于回放时消歧同主体参与方。
- `t_ledger_posting_plan` 和 `t_ledger_entry` 已持久化 route leg 关联、科目、期间、阶段、余额效果、约束、借贷方向和摘要；`LedgerPostingPlanSpec` 已有 `routeLegId`，`FundsInstructionReferenceSpec` 已有 `referenceLedgerTransactionSn`。这些现有事实足以支持追溯和回放，不需要新增平行 posting 模型或证据引用类型。
- `DefaultRouteReplayService` 现在只选择回放腿、反转主体路径和金额并保留 replay provenance；ledger assembler 通过 `referenceLedgerTransactionSn + routeLegId` 精确读取原 posting plan/entry，生成 reversal、completion、refund、fee refund 或 unfreeze 的会计事实。来源不存在、不唯一或期间不一致时 fail closed。
- 旧 route JSON 只在已完成 transaction detail 的历史摘要校验中通过内部 generic-map 读取：先剥离旧会计字段计算当前 path digest，再要求每条旧 leg 的会计字段完整，且将当前空账期规范化为 `LIFETIME/LIFETIME` 后逐 leg 严格匹配；instruction 显式提供的 payer/payee 科目只与主 leg 对应节点匹配，最后以包含完整旧 route 的原始 hash 校验其自洽性。旧字段不会还原为 `RouteSpec`、参与新写或驱动 replay，未完成 detail 不进入降级路径；当前 detail digest 已直接纳入 payer/payee ledger subject 与 period，改变这些显式输入仍产生摘要冲突。
- `BOUNDARY-B` 已把 `UNFREEZE` 固化为余额控制专属 `RELEASE + CONTROL_RELEASE`，并继续由 `LedgerPostingAccessType` 派生 `CLOSING` 准入；真实 H2 测试证明同主体 `FROZEN -> AVAILABLE` 保持平衡，`SUSPENDED` 的 FROZEN 账本仍可释放且状态不被改写，重复解冻继续复用原事实。
- 基于原交易的 direct `REFUND` 与 `FEE_REFUND` 已把原 `LedgerTransaction.sn` 写入 instruction reference、退款 `LedgerTransaction.referenceLedgerTransactionSn` 和全部退款 detail。来源规则为按 tenant 与原 `FundsTransaction.sn` 查询至多两条并要求恰好一条；不存在或不唯一时 fail closed，不任取第一条，也不新增平行 provenance 类型。既有 H2 流程继续证明 `referenceTransactionSn + replayRefLegId + routeLegId`、累计退款上限、并发、幂等和拒绝无副作用。
- 旧产品、DSL 和系分文档曾要求 route 决定 ledger subject、period 和 accounting effect；`D-CS-005` 是已批准的目标语义变更，不是实现缺陷修复。实施时必须同步这些权威规格。

目标契约采用现有资产，不新增 Maven 模块、平行内核、自定义 Clock/证据框架或仅为迁移服务的新枚举：

| 边界 | 保留/归属 | 目标字段或行为 |
| --- | --- | --- |
| route path | 保留在 route | `legId`、顺序、`RouteLegType`、source/target subject 与角色、金额/原金额/汇率、replay policy/ref、参与方和安全 context。 |
| accounting effect | 迁到 posting/ledger | `ledgerSubjectCode`、`balanceEffectType`、`phaseCode`、`periodType/periodId`、`constraintOverrides`、借贷方向和 posting scope。 |
| new posting | ledger assembler 拥有 | 按 instruction event/transaction type、route path、参与方角色、账户 Profile 和 ledger 查询推导会计效果。 |
| replay posting | ledger assembler 拥有 | 通过 `referenceLedgerTransactionSn + routeLegId` 读取原 ledger transaction、posting plan 和 entry，推导冲正或恢复。 |
| fee path | 复用现有模型 | posting 使用 `legId=FEE` 强身份并校验目标为平台费用账户；replay 使用同一 leg ID 区分本金与费用腿，`FEE_RECEIVER` 只参与同主体角色消歧，不新增 fee-purpose 枚举。 |
| contract cutover | 直接替换 | 当前 Snapshot 基线直接改为 path/provenance-only；不保留 v5 reader、legacy 字段、deprecated 访问器或双写。 |

`D-CS-005` 按四个最小批次执行：

| 迁移批次 | 最小动作 | 准出证据 |
| --- | --- | --- |
| `BOUNDARY-A` | 固定各事件/场景的 route -> posting 特征矩阵，不改生产行为。 | 每个 posting plan 平衡、LedgerEntry 可追溯、replay 和拒绝无副作用。 |
| `BOUNDARY-B` | 固化 `UNFREEZE` 的余额控制 release intent/scope，以及 refund/fee-refund 的直接 ledger provenance。 | 持久事实语义可解释，挂起账本仍可收口，逆向事实可直接追溯原 posting。 |
| `BOUNDARY-C` | 直接删除 route 会计成员和旧 snapshot 字段，由 assembler 基于 instruction、route path、账户 Profile 和原 ledger facts 生成 posting。 | direct、authorization/settlement/refund、global/ACH、clearing/reconciliation 四组 H2 场景行为保持。 |
| `BOUNDARY-D` | 同步产品、DSL、系分和接入文档，执行聚焦门禁、完整 CAD 和独立 Checker。 | `compile`、`pmd`、core/boundary/business flow、`verify-cad` 全绿且 P0/P1=0。 |

`BOUNDARY-A` 当前进度：已在 `DefaultLedgerPostingAssemblerTests` 增加 hybrid route 字段投影与全量事件分类两条特征测试，固定 source/target ledger subject、period、balance effect、phase、constraint override、`routeLegId`，并覆盖全部 19 个 `FundsTransactionEventType` 到 posting intent/scope 的当前映射；`FundsWithdrawalRejectionFlowTests` 固定首个真实 H2 拒绝/解冻切片，证明冻结/解冻 intent/scope、快照 leg 关联、posting 关联、重复解冻幂等和无 `WITHDRAW` 账务副作用；`FundsAuthorizationTransactionFlowTests` 固定原 `AUTHORIZATION_1` 快照 leg 到 reversal、completion、auth refund 的累计消费量、原账务流水引用、posting/entry intent/scope 与 replay route leg；`FundsTransactionFeeFlowTests` 固定 fee refund snapshot 的 `replayRefLegId`、posting/entry 的 `FEE_REFUND + FEE`、route node 映射、H2 累计消费量及幂等/超额/并发边界；`FundsDirectTransactionFlowTests` 固定原 pay leg 到 direct refund replay leg、posting/entry 的 `REFUND + BETWEEN_SUBJECTS`、H2 累计消费量、route node 映射及并发/历史绑定边界。聚焦测试分别为 1/1、35/35、16/16、66/66，assembler 测试 9/9、ledger 切片 54/54、business-flow 159/159、reconciliation 221/221、21 模块 compile 和 PMD 通过；新鲜 `verify-cad` 汇总 107 个 suite、1027 项测试、0 failure、0 error、1 项 target-MySQL 预期 skip。`DefaultRouteReplayService` 当前支持的 `REVERSAL`、`COMPLETE`、`AUTH_REFUND`、`REFUND`、`FEE_REFUND`、`UNFREEZE` 六类 replay 已全部完成 H2 现状刻画。

| 场景组 | 当前状态 | `BOUNDARY-A` 证据 |
| --- | --- | --- |
| direct | `VERIFIED_CURRENT` | topup/transfer/pay/withdraw、fee、refund 的 route leg、posting plan、entry、余额、幂等和拒绝副作用均由现有 H2 流程固定。 |
| authorization/settlement/refund | `VERIFIED_CURRENT` | authorization successor、direct/fee refund、clearing confirm 与 settlement lock 已固定 replay/ref、intent/scope、route node 与 entry 映射。 |
| global/ACH | `VERIFIED_CURRENT` | confirmed credit 与 terminal withdraw 已显式关联 route snapshot、posting plan 和 entry；accepted/return 保持无资金副作用；freeze 复用 frozen-order 专用证据链。 |
| clearing/reconciliation | `VERIFIED_CURRENT` | capture、clearing confirm、settlement lock、payout 由共享资金服务写事实；split、gate 与 reconciliation 继续证明只读证据链不写账务事实。 |

因此 `BOUNDARY-A` 状态为 `VERIFIED_WITH_P1_CANDIDATES`：现状矩阵已闭合；Owner 已授权按推荐实践关闭 `UNFREEZE` 语义和退款 provenance 两项 P1 candidate，并明确不承担旧契约兼容责任。实现仍须按 `BOUNDARY-B/C/D` 分切片验证。

`UNFREEZE` Owner 子决策：不复用 `AUTHORIZATION_REVERSAL`，也不退化为 `HOLD`；目标契约新增余额控制专属 release intent/scope，并保留 `CLOSING` 准入，使 `SUSPENDED` 账本仍可完成既有冻结释放。

`BOUNDARY-B` 验证证据：三类资金流程 86/86、assembler 9/9、ledger 55/55、balance-control 44/44、transaction 143/143、business-flow 169/169、core 104/104、boundary 195/195 和 21 模块 compile 均通过；退款账本来源 0 条/2 条参数化 H2 负例直接命中唯一性边界并证明零资金副作用；1049 行 stable API baseline 精确验证通过；PMD 与完整 `verify-cad` 已通过，CAD 汇总为 111 suites / 1078 tests / 0 failures / 0 errors / 1 expected MySQL skip，API、public contract、classfile 和 codegen 门禁通过。独立 Checker 结论为 `PASS`，`P0=0 / P1=0 / P2=0`，当前状态为 `BOUNDARY_B_VERIFIED`，可进入 `BOUNDARY-C`。

`BOUNDARY-C/D` 最终执行证据：route 六类会计成员及新写 wire 字段已删除；DSL JSON 契约 32/32、RouteSnapshot 历史边界 5/5、core 106/106、ledger 55/55、transaction 148/148、boundary 197/197、四组业务 H2 43/43 和 21 模块 compile/PMD 均通过。四组业务证据由 `GlobalAccountAchBusinessFlowTests`、`AcquiringSettlementBusinessFlowTests`、`AgentCommissionSettlementBusinessFlowTests`、`ClearingBatchApplicationServiceTests` 与 `ReconciliationGateApplicationServiceTests` 组成，继续复用同一 FundsTransaction/Route/Ledger/Balance 主链。完整 `verify-cad` 为 112 suites / 1100 tests / 0 failures / 0 errors / 1 expected MySQL skip；1060 行 stable API baseline、public contract、classfile 和 codegen 门禁均通过。Checker 发现的两项 P1 已分别以完成态、逐 leg 严格 legacy fallback 和拒绝旧 route 会计字段的 DSL verifier 关闭；P2 措辞漂移同步修正，最终结论为 `P0=0 / P1=0 / P2=0`，状态为 `BOUNDARY_D_VERIFIED`。

后续工程 CR 历史证据（2026-08-11）：冻结单旧 full-route 摘要兼容、resolver 输出身份、原账本交易归属、费用腿强身份与同主体角色消歧均已补回归；当时两个 instruction context 共享 key 归属 Core。聚焦测试 58/58、transaction 149/149、boundary 199/199、business-flow、PMD 与完整 CAD 均通过；完整 CAD 为 112 suites / 1104 tests / 0 failures / 0 errors / 1 expected MySQL skip，当时 stable API baseline 为 1062 行。当前 baseline 由 `D-CS-006-S` supersede 为 1039 行。

`D-CS-005` 停止条件：任一切片无法同时证明 posting 平衡、LedgerTransaction/LedgerEntry 可追溯、逆向累计上限、幂等和拒绝无副作用时停止；不回退到兼容桥或 hybrid route。

## 3. Execution Plan

### Core-0: 可用性门

| Task | 状态 | 最小写入范围 | 验证与停止条件 |
| --- | --- | --- | --- |
| `CORE-0A` 恢复独立 core 门禁 | `VERIFIED` | 两个 DSL 测试类，复用 `WindOperatorTestFixture` | 聚焦 23 项由 7 failures/6 errors 转为全绿；修复余额契约测试重命名后的门禁引用，`just test-core` 104/104；Java 21 compile/PMD 21 模块成功。 |
| `CORE-0B` 余额语义刻画与裁决 | `VIEW_POLYMORPHISM_VERIFIED` | 总余额回归 `FundsAccountBalanceView` 多态契约；当前只实现已确认的 `FUNDING_BASIC` 口径，删除含糊 `pending` 和查询服务聚合 | 公共契约 4/4、授权 H2 流程 38/38；Java 21 编译、PMD、完整 CAD 109 suites/1043 tests/0 failures/0 errors/1 expected MySQL skip；Checker P0/P1=0。 |
| `CORE-0C` 摘要兼容基线 | `DIGEST_A_VERIFIED` | legacy 固定 golden、canonical v1 有界编码、精确双读 helper，以及八个 DIGEST-A domain 的逐边界迁移 | 最终 `verify-cad` 1039/0/0/1，Checker P0/P1=0；ledger/detail persisted-legacy 边界专测作为 P2 转入 Core-1B。 |

Core-0 准出：`SC-CS-001` 完成，`D-CS-002` 和 `D-CS-003` 均有 Owner 决策，且没有把当前偶然行为误当长期契约。

### Core-1: 稳定性门

| Task | 状态 | 目标 | 准出证据 |
| --- | --- | --- | --- |
| `CORE-1A` 公共 API 分级与冻结 | `CURRENT_D_CS_006_U_REBASE_VERIFIED` | 当前基线为 102/94/4/4、1025 行，仍冻结 `FundsAccount#getState()` | `D-CS-006-S/T` 作为历史证据保留；`D-CS-006-U` 只删除场景 enum 并机械重基线，Core API 与独立 Checker 已准出。 |
| `CORE-1B` 版本化摘要协议 | `CORE1B_A_VERIFIED` | 显式 schema/hash version，规范化输入，旧版本可读/可比；ledger/detail persisted-legacy 边界专测已完成 | CORE-1B-A golden、双版本读取、幂等和重放测试已通过；其余摘要批次仍待后续 Owner Gate。 |
| `CORE-1C` 时间与重放语义 | `ANALYZED_OWNER_GATE` | 已区分绝对事实、业务日历和宿主时钟，形成 `TIME-A` 至 `TIME-E` 分批迁移 | `D-CS-004` 批准历史 zone/破坏边界后，补 JSON、H2 持久化和跨时区重放测试。 |
| `CORE-1D` route/posting 边界 | `BOUNDARY_D_VERIFIED` | `BOUNDARY-A/B` 已固定现状、release 与 refund provenance；`BOUNDARY-C` 已删除 route 会计成员，由 posting/ledger 从 instruction、path、Profile 和原账本事实推导。 | compile、PMD、core/ledger/transaction/boundary、四组 H2 与完整 CAD 已通过；独立 Checker `P0/P1/P2=0/0/0`。 |
| `CORE-1E` DSL 包与实现归属 | `PACKAGE_CONVERGENCE_VERIFIED` | capability-first 包名；默认 route/instruction 实现下沉 `transaction-impl`；face 请求只暴露稳定账户值对象。 | 当前 102/94/4/4 API 门禁、Core 包边界测试、generic direct/Consumer 资金流、compile 和 Checker。 |

`CORE-1B-A` persisted-legacy compatibility 状态为 `VERIFIED_TEST_ONLY`。仅在
`LedgerTransactionServiceImplTests` 和 `FundsDirectTransactionFlowTests` 增加真实 Spring/H2 持久边界测试：固定历史
legacy golden，证明 ledger transaction `sha256` 与 transaction detail `request_hash` 在同请求重放时复用原事实、
冲突请求 fail-closed、新事实继续写 canonical v1，且交易、明细、posting、entry 和逐桶余额不变。测试未调用当前双读
方法动态生成 legacy 值，也未修改生产源码、DDL、公共 API、摘要算法或其他 DIGEST 批次。本包只关闭 ledger/detail
persisted-legacy 证据缺口，不代表 `CORE-1B` 整体完成。

#### CORE-1B-A 执行准备证据

固定 golden 由基线提交 `ac9d1565a124cda7a6346eb679624af19f82bf17` 的旧 writer 在隔离 H2 副本中直接落库取得；
基线 tar commit id、旧 writer 生产源码 blob 与该提交均已核对一致。当前 canonical 值由当前工作树 writer 在另一隔离 H2
副本中直接落库取得，当前临时副本的 ledger/detail writer 生产源码 blob 与正式工作树一致。正式测试必须使用以下常量，
不得通过当前双读 helper 反算 legacy 值。

表中 detail canonical 值已按 `BOUNDARY-C` 当前 writer 更新：route 会计字段移除后，payer/payee ledger subject 与 period 由 instruction 直接进入 detail digest。legacy golden 保持不变，并由当前生产兼容路径继续验证；这不改变 `CORE-1B-A` 当时仅补测试的历史执行范围。

| 边界与身份 | legacy golden | canonical v1 golden |
| --- | --- | --- |
| ledger `LE_LEDGER_CONTEXT_001` | `50c63ea9e47976aae601f54b38e0ca92128ed454411ee270621ddf880068a71c` | `f27b0b1798d3c1b06576b4c87301bb037bd87dc30f5cdb69ac827990004c80a6` |
| detail `funding_user / PAYEE` | `868c7b3858bb95b4ce98345f737b547e2b9165272d58553f43d7d9386085fc91` | `5ba7816caaf9364a8f41656cf3676c406cb359b195018c39f217a9bd8214f53a` |
| detail `platform_cash_mapping / PLATFORM_FUNDING_ACCOUNT` | `d24d56e8f0eb2a3dfdfee0d76e481c3e574651bb4ad1851573b566c68998291f` | `aa08a32f91c032b3b906ad8cfcba93796f72b0ef3bdf948ff0c89ee8b6199e5e` |
| detail `platform_prepayment / PLATFORM_FUNDING_ACCOUNT` | `ce86ddc28cd3e34e818f375cda7fcbfcede955187b7f1620c85d092f8af24f84` | `b241e319e526e583fa68a85768337b9115dbb1a706633f31903d56a15e4da957` |

隔离证据结果：基线旧写 2/2、当前新写 2/2、把固定 legacy 值写回当前 H2 后的同请求双读重放 PoC 2/2，均为
0 failures / 0 errors / 0 skips。PoC 已证明现有 reader 可以读取上述 legacy 值，因此正式执行无需先修改生产源码；但临时
测试没有覆盖正式包要求的完整事实快照、冲突 fail-closed 和冲突后无副作用断言，不能替代正式验收。

独立 Checker 结论为 `READY_FOR_OWNER_GRANT / NO-ACTION`，`P0=0 / P1=0 / P2=1`。正式范围仍限定为上述两个测试类，
`FundsDirectTransactionFlowTests` 可在类内独立注入 `JdbcTemplate`，不新增共享 test support。唯一 P2 是该文件已有其他
未提交改动；Maker 必须先保存现有 diff，按 hunk 增量修改并复核，禁止整文件覆盖。Owner Grant 前不得写入正式测试。

#### CORE-1B-A 正式验收证据

- Ledger 固定 canonical/legacy golden，验证新写 `newlyPosted=true`、`1 transaction / 1 plan / 2 entries`；将持久化
  `sha256` 精确替换为 legacy 且更新数为 1 后，同请求重放复用原 ID、`newlyPosted=false` 且不重写 legacy；变更 posting
  intent 的冲突请求 fail-closed，包含 `t_ledger` 在内的全部账务事实保持不变。
- Direct topup 固定三个参与方的 canonical/legacy golden，逐条替换 `request_hash` 且更新数均为 1；验证
  `1 funds transaction / 3 details / 1 ledger transaction / 2 plans / 4 entries`。同金额重放复用原 transactionSn，
  金额 `40 -> 41` 冲突 fail-closed；资金交易、明细、route JSON/对象、全部账务事实和逐桶余额均保持不变。
- 验证结果：两个目标类 76/76、`test-ledger` 55/55、`test-transaction` 141/141；Java 21 的 21 模块 compile、PMD、
  classfile/codegen 门禁通过；完整 CAD 为 1045 tests / 0 failures / 0 errors / 1 expected MySQL skip。
- 首次 CAD 的 14 个 error 均为当前宿主 Mockito/Byte Buddy 动态 attach 无法创建 `.java_pid` socket；诊断 dump 固定为
  `AttachNotSupportedException`。预加载本地既有 `byte-buddy-agent` 后原失败用例 14/14、完整 CAD 1045/1045 通过；
  该 workaround 未写入项目。后续在同一宿主复验时应保留该启动参数。
- 正式独立 Checker 结论：`PASS`，`P0=0 / P1=0 / P2=0`；确认固定 golden 未由当前 helper 生成、未扩展生产或共享测试
  范围，并确认 `FundsDirectTransactionFlowTests` 原有 refund provenance dirty hunk 完整保留。

Core-1 准出：公共契约、摘要、时间和 route/posting 均有兼容证据；任何破坏性变化均已通过 Owner Gate。

### Core-2: 兼容与场景门

| Task | 状态 | 目标 | 准出证据 |
| --- | --- | --- | --- |
| `CORE-2A` 迁移适配与弃用窗口 | `PLANNED` | write-one/read-supported，删除无使用证据的临时兼容层 | 兼容矩阵、废弃期限、调用方迁移清单。 |
| `CORE-2B` 代表性业务包复验 | `PLANNED` | 四组场景共同消费同一资金事实与账本入口 | H2 业务流、逐步余额、幂等、拒绝无副作用。 |
| `CORE-2C` Maker/Checker 封板 | `PLANNED` | 完整门禁、差异复核和残余风险归属 | `verify-cad` 全绿，Checker P0/P1=0。 |

Core-2 准出：`SC-CS-001` 至 `SC-CS-008` 全部有新鲜证据，Goal 才能转 `VERIFIED`；不据此声明宿主或生产就绪。

## 4. Plan Grant

当前 Plan Grant 为 `CORE_1_BOUNDARY_D_VERIFIED`：Owner 已批准当前 `1.0.1-SNAPSHOT` 按推荐实践直接进行破坏式收敛；`BOUNDARY-C/D` 实现、扩大测试、API baseline、PMD、完整 CAD 和独立 Checker 已完成。

Human Owner 现另行接受 `D-CS-006-T / MIG-05D A`，Contract Surface 与 Behavioral RED/Green 均已按无兼容设计执行并通过独立 Checker。该接受不复活 Core-2 兼容窗口；旧 assembler/raw post、时序 Ledger SN 与无 root factory overload 已直接删除。MIG-05D 当前范围完成，下一切片必须回到主 OpenSpec 重新形成 Entry Card。

本轮停止在 `CORE-1D / BOUNDARY-D` 封板；下一循环不得默认实施时间迁移、其余摘要批次或工作树中的 settlement release，须按各自 Owner Gate 重新授权。

以下动作仍需单独授权或 Owner 决策：

- 继续改变已修订的余额口径、其余摘要格式或时间字段；`D-CS-004` 仍需 Architecture Owner 裁决。
- 把进入 `CORE-1E` 前已有的 settlement release 工作树改动及 `SETTLEMENT_RELEASE` 公共枚举成员纳入本执行包的 API 批准或验收结论。
- Git stage/commit/push/PR、联网、安装、数据库连接、部署和生产操作。

已批准并执行的历史执行包：

1. `CORE-0B` 保留精确 `authorization` 余额和 View 层 `total` 多态契约；当前具体实现只支持 `FUNDING_BASIC`，其他 Profile fail-closed；删除含糊 `pending` 和查询服务聚合。
2. `CORE-0C` 先增加 legacy golden 和 `test-core` 门禁，再新增 domain/version 有界的 canonical v1；逐个持久边界迁移，不全局替换。
3. 当时的 `Core-0` 执行包不授权 `CORE-1C/1D`；后续 `D-CS-005` 已单独批准 route/posting 破坏式目标，时间、Git 和生产动作仍未授权。
4. `CORE-1B-A` 仅在两个既定测试类补 ledger/detail persisted-legacy H2 兼容证据；不改生产、DDL、公共 API、摘要算法或共享测试 support。
5. `CORE-1A` 只增加受控 API policy、stable signature baseline 和验证门禁；不改生产源码、运行时依赖、JSON/wire 行为或未裁决公共语义。
6. `CORE-1E` 直接把 transaction/ledger spec、交易支持类型归入 capability 包，把 14 个默认 route/instruction 实现下沉到 `transaction-impl`，并把 Benefit settle 请求改为稳定 `FundsAccountId`；不增加 facade、factory、兼容桥或 V2 类型图，也不提前实施 `BOUNDARY-B/C`。
7. `CORE-1D / BOUNDARY-B` 新增余额控制专属 `RELEASE / CONTROL_RELEASE` 并保持 `CLOSING` 准入；direct refund 与 fee refund 复用现有账本查询端口，只有原资金交易恰好对应一条账本流水时才固化直接 provenance；0 条/2 条 H2 负例均 fail closed 且零资金副作用，不新增兼容层或平行证据模型。
8. `CORE-1D / BOUNDARY-C` 删除 route 会计成员和新写 snapshot 会计字段；posting/ledger 复用现有 instruction、route path、账户 Profile、posting/entry 与原账本引用推导会计效果，不新增公共 API、DDL、依赖、平行模型或兼容桥。
9. `CORE-1D / BOUNDARY-D` 同步 DSL verifier、五份 transaction-layer fixture、权威文档和 API 门禁；legacy detail fallback 仅接受完成态、完整逐 leg 会计事实与规范化账期严格匹配，独立 Checker `P0/P1/P2=0/0/0`。
10. `D-CS-006-R` 按 2026-08-22 已接受的无兼容迁移，把当前 API 基线从历史 `107/99/4/4 / 1062 lines / getStatus()` supersede 为 `105/97/4/4 / 1043 lines / getState()`；不恢复两个旧 profile spec、`ALLOW_NEGATIVE_BALANCE` 或兼容入口。
11. `D-CS-006-S` 按 2026-08-24 已接受的 MIG-05C 无兼容迁移，删除 `LedgerBalanceProjectionService` 四条 stable signature，把当前 API 基线 supersede 为 `104/96/4/4 / 1039 lines / getState()`；不新增 concrete 公共替代或兼容入口。
12. `D-CS-006-U` 按 2026-08-26 已接受的 MIG-08 R8B 无兼容迁移，删除只属于 Consumer Benefit 分类的 `FundsBenefitFundingNature` 十一条 stable signature，把当前 API 基线 supersede 为 `102/94/4/4 / 1025 lines / getState()`；不恢复 enum、alias、V2 或场景 facade。

## 5. Goal Ledger

| Field | Current value |
| --- | --- |
| Latest completed action | MIG-08 R8B Core API 治理重基线：fresh clean compile=`21/21`、Core API=`102/94/4/4 / 1025`，独立 Checker=`PASS / 0 P0 / 0 P1 / 0 P2`。 |
| Current task | `D-CS-006-U / MIG08_R8B_CORE_API_GOVERNANCE_REBASE / INDEPENDENT_CHECKER_PASS / CURRENT_SCOPE_COMPLETE / CODE_FREEZE`。 |
| Current write set | 本轮四文件治理重基线授权已消耗；Core 生产代码、baseline 其他 signature、兼容入口与其他任务差异保持不动。 |
| Next action | 回到主 OpenSpec；本 Goal 不直接产生 MIG-09、Git、发布或生产授权。 |
| Stop condition | 未另获联网授权却执行非 offline Maven；把 Provider Green 外推为 Consumer/HOST/生产完成；`api-policy.tsv`、额外 signature/schema/Consumer/兼容路径进入；Git、安装和生产动作仍未授权。 |
| Deferred owners | Architecture/Payment Owner：其余摘要；Architecture Owner：时间；settlement release 变更 Owner：独立 API/资金切片收口 |
