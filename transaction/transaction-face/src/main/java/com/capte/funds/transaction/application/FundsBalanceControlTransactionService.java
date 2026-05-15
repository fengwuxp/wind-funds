package com.capte.funds.transaction.application;

/**
 * 资金余额控制交易命令服务。
 *
 * @deprecated since 1.0.1, use {@link FundsBalanceControlService}. Balance control commands are not standard
 * transaction main facts; this name is retained only as a compatibility alias during naming-governance migration.
 *
 * @author wuxp
 * @date 2026-04-30 10:25
 **/
@Deprecated(since = "1.0.1", forRemoval = false)
public interface FundsBalanceControlTransactionService extends FundsBalanceControlService {

}
