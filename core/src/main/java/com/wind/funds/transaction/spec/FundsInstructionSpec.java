package com.wind.funds.transaction.spec;

import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.operator.WindOperator;
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

    @NonNull
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
    default String getExternalSourceCode() {
        return null;
    }

    @Nullable
    default String getExternalFundsFactSn() {
        return null;
    }

    @Nullable
    default FundsEffectType getExternalFundsEffectType() {
        return null;
    }

    @Nullable
    default String getExternalFundsFactDigest() {
        return null;
    }

    @Nullable
    default FundsAccountId getAccountId() {
        return null;
    }

    @Nullable
    default FundsAccountId getPayerAccountId() {
        return null;
    }

    @Nullable
    default FundsAccountId getPayeeAccountId() {
        return null;
    }

    @Nullable
    default FundsAccountId getPayerId() {
        return null;
    }

    @Nullable
    default FundsAccountId getPayeeId() {
        return null;
    }

    @Nullable
    default LedgerSubjectCode getPayerLedgerSubjectCode() {
        return null;
    }

    @Nullable
    default LedgerSubjectCode getPayeeLedgerSubjectCode() {
        return null;
    }

    @Nullable
    default FundsAccountId getLinkedFundingAccountId() {
        return null;
    }

    @Nullable
    default AccountBalancePeriodType getLedgerPeriodType() {
        return null;
    }

    @Nullable
    default String getLedgerPeriodId() {
        return null;
    }

    @Nullable
    FundsInstructionReferenceSpec getReference();

    @NonNull
    String getBusinessScene();

    @NonNull
    String getBusinessSn();

    @NonNull
    LocalDateTime getEventTime();

    @Nullable
    String getDescription();

    /**
     * 返回当前指令的运行时操作者上下文。
     *
     * <p>资金 Core 只使用稳定身份和主体类型，不得直接序列化、持久化该对象，
     * 也不得在此执行权限判断或读取动态请求信息。</p>
     *
     * @return 当前运行时操作者
     */
    @NonNull
    WindOperator getOperator();

    @NonNull
    Map<String, Object> getContextVariables();
}
