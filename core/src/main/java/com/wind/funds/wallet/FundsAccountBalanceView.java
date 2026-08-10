package com.wind.funds.wallet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.ledger.LedgerBalanceView;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;

/**
 * 资金账户余额视图（Balance Projection View）
 *
 * <p>用于描述某个资金账户在某一时刻的余额状态，是 Ledger 账本聚合后的结果视图。</p>
 *
 * <h3>核心职责</h3>
 * <ul>
 *     <li>表达账户“当前资金状态”和当前 Profile 定义的只读派生口径</li>
 *     <li>所有金额均来源于 LedgerEntry 的聚合结果（Projection）</li>
 *     <li>不参与资金流转决策（如冻结、退款、提现逻辑）</li>
 *     <li>支持多币种账户的余额表达（按 currency 维度隔离）</li>
 * </ul>
 *
 * <h3>资金模型说明</h3>
 * <ul>
 *     <li>Available：可用于消费或支出的资金</li>
 *     <li>Frozen：因提现或风控限制暂不可用的归属资金</li>
 * </ul>
 *
 * <h3>重要原则</h3>
 * <ul>
 *     <li>余额 = Ledger 投影结果，而非业务计算结果</li>
 *     <li>该对象必须为只读模型（不可变）</li>
 *     <li>不得在 View 层发起资金变更；派生汇总只能读取已投影的余额 bucket</li>
 * </ul>
 *
 * @author wuxp
 * @date 2026-04-15 16:58
 **/
public interface FundsAccountBalanceView extends TenantIsolationObject<Long>, LedgerBalanceView {

    /**
     * 当前余额视图采用的账本 Profile。
     *
     * @return 账本 Profile
     */
    @NonNull
    LedgerProfileCode getLedgerProfileCode();

    /**
     * 可用余额（Available Balance）
     *
     * <p>
     * 表示账户当前可直接用于消费、支付或转出的资金。
     * </p>
     *
     * <h4>计算来源（概念层）</h4>
     * <pre>
     * Available = 已入账资金 - 冻结资金 - 在途占用资金
     * </pre>
     *
     * <h4>业务语义</h4>
     * <ul>
     *     <li>可用于支付（消费）</li>
     *     <li>可用于提现申请</li>
     *     <li>参与风控校验</li>
     * </ul>
     */
    Money getAvailableBalance();

    /**
     * 冻结余额（Frozen Balance）
     *
     * <p>
     * 表示已属于账户，但当前被系统限制不可使用的资金。
     * </p>
     *
     * <h4>典型来源</h4>
     * <ul>
     *     <li>提现冻结（withdraw hold）</li>
     *     <li>风控冻结（risk control freeze）</li>
     * </ul>
     *
     * <h4>业务语义</h4>
     * <ul>
     *     <li>仍属于用户资产</li>
     *     <li>不可用于消费或提现</li>
     *     <li>通常可解冻或转移状态</li>
     * </ul>
     */
    @NonNull
    Money getFrozenBalance();

    /**
     * 授权占用余额（Authorization Balance）。
     *
     * <p>表示已批准但尚未完成、撤销或过期释放的授权占用，只对应 {@link LedgerSubjectCode#AUTHORIZATION}。</p>
     *
     * @return 授权占用余额
     */
    @NonNull
    default Money getAuthorizationBalance() {
        return getBalance(LedgerSubjectCode.AUTHORIZATION);
    }

    /**
     * 当前 Profile 定义的总余额。
     *
     * <p>聚合口径由具体 View 实现，不同 Profile 的结果不能直接比较；尚未定义口径的实现必须 fail-closed。
     * 该派生值不进入通用 JSON，外部响应必须由已明确 Profile 口径的 DTO 显式映射。</p>
     *
     * @return 当前 Profile 的总余额
     */
    @JsonIgnore
    @NonNull
    Money getTotalBalance();

}
