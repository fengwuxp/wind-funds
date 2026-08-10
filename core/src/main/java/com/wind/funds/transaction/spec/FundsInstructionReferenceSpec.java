package com.wind.funds.transaction.spec;

import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 资金指令关联引用。
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface FundsInstructionReferenceSpec {

    /**
     * @return 引用类型
     */
    @NonNull
    FundsInstructionReferenceType getReferenceType();

    /**
     * @return 关联资金指令或交易流水
     */
    @Nullable
    String getReferenceSn();

    /**
     * @return 关联业务单号
     */
    @Nullable
    String getReferenceBusinessSn();

    /**
     * @return 关联账本交易流水
     */
    @Nullable
    String getReferenceLedgerTransactionSn();

    /**
     * @return 外部交易流水
     */
    @Nullable
    String getExternalTransactionId();

    /**
     * @return 授权码
     */
    @Nullable
    String getAuthCode();

    /**
     * @return 扩展上下文
     */
    @NonNull
    Map<String, Object> getContextVariables();
}
