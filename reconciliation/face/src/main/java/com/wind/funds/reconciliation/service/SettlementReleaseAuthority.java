package com.wind.funds.reconciliation.service;

import com.wind.funds.reconciliation.model.dto.SettlementReleaseAuthorityContextDTO;
import com.wind.funds.reconciliation.model.dto.SettlementReleaseDecisionDTO;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 宿主提供的结算锁定资金释放授权边界。
 *
 * <p>实现必须基于可回读权威事实作出有时效的授权决定，不得只信任调用方提交的 Gate 或证据引用。</p>
 *
 * @author wuxp
 * @since 2026-08-06
 */
@NullMarked
public interface SettlementReleaseAuthority {

    /**
     * 对本次结算锁定资金释放作出授权决定。
     *
     * @param context  释放上下文和当前权威证据
     * @param operator 操作者
     * @return 带摘要、有效期和证据引用的授权决定
     */
    SettlementReleaseDecisionDTO authorize(SettlementReleaseAuthorityContextDTO context, WindOperator operator);
}
