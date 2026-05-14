package com.capte.funds.transaction.services;

/**
 * 冻结单生命周期保存服务。
 *
 * <p>冻结和解冻的事实载体是 FrozenOrder，不应落入标准 FundsTransaction 生命周期。</p>
 */
public interface FundsFrozenOrderLifecycleSaver extends FundsInstructionLifecycleSaver {
}
