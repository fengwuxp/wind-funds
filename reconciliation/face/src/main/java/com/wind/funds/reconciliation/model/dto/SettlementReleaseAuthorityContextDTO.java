package com.wind.funds.reconciliation.model.dto;

import com.wind.funds.reconciliation.model.request.ReleaseSettlementOrderRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * 结算锁定资金释放的授权上下文。
 *
 * <p>宿主授权实现只能基于此处的稳定事实和可回读证据作出决定，不得把请求中的 gate 引用当作最终授权。</p>
 *
 * @author wuxp
 * @since 2026-08-06
 */
@Schema(description = "结算锁定资金释放授权上下文")
@Data
@Accessors(chain = true)
public class SettlementReleaseAuthorityContextDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1772897838715652027L;

    @Schema(description = "待释放的结算单事实")
    private SettlementOrderDTO settlementOrder;

    @Nullable
    @Schema(description = "关联出款单事实；未创建出款单时为空")
    private PayoutOrderDTO payoutOrder;

    @Schema(description = "结算释放请求及其来源闭合声明")
    private ReleaseSettlementOrderRequest request;

    @Schema(description = "在当前释放事务内重查得到的对账 Gate 决策")
    private ReconciliationGateDecisionDTO gateDecision;

    @Schema(description = "原结算锁定交易 RouteSnapshot 摘要")
    private String originalLockRouteSnapshotDigest;

    @Schema(description = "本次释放请求稳定摘要")
    private String releaseRequestDigest;
}
