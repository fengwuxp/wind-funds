package com.wind.funds.ledger;

import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import org.jspecify.annotations.NonNull;

/**
 * 账本高阶入账命令。
 *
 * <p>作为 Funds 内部跨模块唯一账本写入口，接收已归一资金指令、资金动作身份和已解析路径，
 * 由 Ledger 负责组装、校验并持久化 LedgerTransaction、PostingPlan、Entry 与余额投影。</p>
 *
 * <p>不负责业务订单、外部协议、finality、路由选择、风控、清结算或对账决策。</p>
 */
public interface LedgerTransactionPostingService {

    /**
     * 将一笔已归一资金动作按已解析路径提交账本。
     *
     * @param instruction 已归一资金指令
     * @param fundsTransactionSn 资金交易动作根流水
     * @param resolvedRoute 已解析资金路径
     * @return 本次账本交易流水
     */
    @NonNull String post(@NonNull FundsInstructionSpec instruction,
                         @NonNull String fundsTransactionSn,
                         @NonNull ResolvedRouteSpec resolvedRoute);
}
