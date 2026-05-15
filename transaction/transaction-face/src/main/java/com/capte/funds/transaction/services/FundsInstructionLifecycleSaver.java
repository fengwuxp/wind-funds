package com.capte.funds.transaction.services;

/**
 * 资金指令业务生命周期保存服务。
 *
 * @deprecated since 1.0.1, use {@link FundsInstructionLifecycleRecorder}. Lifecycle implementations record
 * transaction or control facts; "saver" is retained only as a compatibility alias during the naming-governance
 * migration.
 */
@Deprecated(since = "1.0.1", forRemoval = false)
public interface FundsInstructionLifecycleSaver extends FundsInstructionLifecycleRecorder {

}
