package com.wind.integration.funds.wallet;

import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.integration.funds.wallet.enums.FundsAccountCapability;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 资金账户（FundsAccount）
 *
 * <p>表示一个具备资金收支能力的账户实体，是 Ledger 系统的业务入口对象。</p>
 *
 * <h3>模型分层</h3>
 * <ul>
 *   <li>Account Entity：账户身份与状态</li>
 *   <li>Balance View：账户资金状态（由 Ledger 投影生成）</li>
 *   <li>Capability Policy：账户行为能力控制</li>
 * </ul>
 *
 * <h3>重要原则</h3>
 * <ul>
 *   <li>账户余额来源于 Ledger，不可直接修改</li>
 *   <li>账户能力不等于余额能力（需同时满足 Policy + Balance）</li>
 *   <li>账户状态变化不应直接修改余额</li>
 * </ul>
 *
 * @author wuxp
 * @date 2023-12-01 10:37
 **/
public interface FundsAccount extends TenantIsolationObject<Long> {

    /**
     * 账户标识
     *
     * @return 账户标识
     */
    @NonNull
    FundsAccountId getAccountId();

    /**
     * 账户所属主体
     *
     * @return 账户所属主体
     */
    @NonNull
    FundsAccountOwner getOwner();

    /**
     * 账户状态
     *
     * @return 账户状态
     */
    @NonNull
    FundsAccountStatus getStatus();

    /**
     * 账户余额ID
     *
     * @return {
     *  key  账本会计编码
     *  value 账本 id
     * }
     */
    @NonNull
    Map<LedgerSubjectCode, Long> getAccountLedgerIds();

    /**
     * 获取账户余额ID
     *
     * @param accountCode 账户标识
     * @return 账户余额ID
     */
    @NonNull
    default Long getLedgerId(@NonNull LedgerSubjectCode accountCode) {
        Long result = getAccountLedgerIds().get(accountCode);
        AssertUtils.notNull(result, "{} ledger balance id not found", accountCode);
        return result;
    }

    /**
     * 获取账户余额ID
     *
     * @param accountCodes 账户标识
     * @return 账户余额ID
     */
    @NonNull
    default List<Long> getLedgerIds(@NonNull List<LedgerSubjectCode> accountCodes) {
        Map<LedgerSubjectCode, Long> accountLedgerIds = getAccountLedgerIds();
        return accountCodes.stream().map(accountLedgerIds::get).filter(Objects::nonNull).toList();
    }

    /**
     * 账户能力
     *
     * @return 账户能力
     */
    @NonNull
    default Set<FundsAccountCapability> getCapabilities() {
        return FundsAccountCapability.allValues();
    }

    default boolean canWithdraw() {
        return getStatus().canDebit() && getCapabilities().contains(FundsAccountCapability.WITHDRAW);
    }

    default boolean canPay() {
        return getStatus().canDebit() && getCapabilities().contains(FundsAccountCapability.PAY);
    }

    default boolean canReceive() {
        return getStatus().canCredit() && getCapabilities().contains(FundsAccountCapability.RECEIVE);
    }

    /**
     * 是否允许透支，由具体实现决定
     *
     * @return if true 账户可以透支
     */
    default boolean canOverdraft() {
        return false;
    }

    /**
     * 账户金额的币种
     *
     * @return 币种
     */
    CurrencyIsoCode getCurrency();

    /**
     * 账户是否可用，由具体实现决定
     *
     * @return if true 可用
     */
    boolean isAvailable();

    /**
     * @return 账户描述
     */
    default String getDesc() {
        return WindConstants.EMPTY;
    }

    /**
     * 账户数据版本
     *
     * @return 账户数据版本
     */
    @NonNull
    Integer getVersion();
}
