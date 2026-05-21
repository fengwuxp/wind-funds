package com.wind.integration.funds.spec.transaction;

import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 账本可理解的资金指令。
 *
 * <p>职责：
 * <ul>
 *   <li>承载业务交易进入账务层前的稳定事实输入</li>
 *   <li>描述一笔资金动作的意图、金额、上下文、发起人和业务标识</li>
 *   <li>作为 RouteResolver 的唯一输入之一，驱动路径解析与后续账务翻译</li>
 * </ul>
 *
 * <p>非职责：
 * <ul>
 *   <li>不负责资金路径选择</li>
 *   <li>不负责账本分录生成</li>
 *   <li>不负责余额计算与持久化</li>
 * </ul>
 */
public interface FundsInstructionSpec {

    @Nullable
    Long getTenantId();

    @NonNull
    FundsInstructionType getInstructionType();

    @NonNull
    FundsTransactionEventType getEventType();

    @NonNull
    DefaultFundsTransactionType getTransactionType();

    @NonNull
    Money getAmount();

    @NonNull
    Money getOriginalAmount();

    @NonNull
    BigDecimal getExchangeRate();

    @Nullable
    PaymentInstrumentRefSpec getInstrumentRef();

    @Nullable
    ExternalAccountRefSpec getExternalAccountRef();

    @Nullable
    FundsInstructionReferenceSpec getReference();

    /**
     * 已由业务侧、订单侧或营销权益系统决策完成的权益结果快照。
     *
     * <p>为空表示无权益交易，既有资金指令语义保持不变。</p>
     *
     * @return 权益结果快照
     */
    @Nullable
    default FundsBenefitSnapshotSpec getBenefitSnapshot() {
        return null;
    }

    @NonNull
    String getBusinessScene();

    @NonNull
    String getBusinessSn();

    @NonNull
    LocalDateTime getEventTime();

    @Nullable
    String getDescription();

    @NonNull
    FundsOperationActorSpec getOperator();

    @NonNull
    Map<String, Object> getContextVariables();
}
