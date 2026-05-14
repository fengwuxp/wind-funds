package com.wind.integration.funds.ledger;

import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import org.jspecify.annotations.NonNull;

/**
 * LedgerTransactionPostingService（账本交易入账服务 / 统一记账入口）
 *
 * <p>定义：
 * 账本系统的唯一写入口，用于校验并落地已完成账务组装的 LedgerTransaction。
 *
 * <p>该接口是整个账本系统的“Write Path Gateway”（写入网关）。
 * <p>
 * ------------------------------------------------------------
 * <h2>核心职责（必须严格遵守）</h2>
 *
 * <b>1. 交易接入（Transaction Ingestion）</b>
 * - 接收业务侧 LedgerTransactionSpec
 * - 校验基础合法性（币种 / 金额 / 幂等）
 *
 * <b>2. 账务计划校验（Plan Validation）</b>
 * - 校验 LedgerTransactionSpec 已携带 LedgerPostingPlanSpec
 * - 校验每个 PostingPlan 独立借贷平衡
 * - 不在本层重新解析 RouteSnapshot 或重新生成 Plan
 *
 * <b>3. 执行编排（Execution Orchestration）</b>
 * - 控制 Phase 执行顺序
 * - 按 LedgerTransaction → Plan → Phase → Entry 落账
 *
 * <b>4. 账本落地（Ledger Persistence）</b>
 * - 生成 LedgerEntry
 * - 写入账本存储（不可变事实）
 * - 保证借贷平衡（Plan级约束）
 * <p>
 * ------------------------------------------------------------
 * <h2>非职责（禁止在此层实现）</h2>
 * <p>
 * ❌ 不负责规则定义和账务计划生成（Assembler 做）
 * ❌ 不负责路由解析（RouteResolver 做）
 * ❌ 不负责账户余额计算（Projection Service 做）
 * ❌ 不负责风控决策（Risk Engine 做）
 * ❌ 不负责对账（Reconciliation Service 做）
 * <p>
 * ------------------------------------------------------------
 * <h2>设计原则</h2>
 * <p>
 * ✔ 单一入口（Single Write Gateway）
 * ✔ 本地事务内原子落账
 * ✔ Phase 可审计
 * ✔ 规则与执行解耦（Assembler vs Posting）
 * <p>
 * ------------------------------------------------------------
 * <h2>典型执行链路</h2>
 * <p>
 * FundsInstruction
 * ↓
 * RouteResolver / RouteSnapshot
 * ↓
 * LedgerPostingAssembler
 * ↓
 * LedgerTransaction + LedgerPostingPlan
 * ↓
 * LedgerPostingPhase（语义阶段）
 * ↓
 * LedgerEntry（事实账本）
 * <p>
 * ------------------------------------------------------------
 * <h2>设计目标</h2>
 * <p>
 * - 保证账务一致性（Consistency）
 * - 支持高并发写入
 * - 支持审计
 *
 */
public interface LedgerTransactionPostingService {

    /**
     * 执行账本交易（统一记账入口）
     *
     * <p>注意：
     * - 幂等、重试和业务事件保存由上层业务交易层负责
     * - 本层不允许同一 transaction.sn 重复入账
     * - 必须保证最终一致性
     *
     * @param transaction 业务账本交易（唯一事实输入源）
     */
    void post(@NonNull LedgerTransactionSpec transaction);
}
