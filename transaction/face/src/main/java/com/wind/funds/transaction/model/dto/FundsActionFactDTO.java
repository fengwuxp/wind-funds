package com.wind.funds.transaction.model.dto;

import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 一次资金动作的规范化只读事实。
 *
 * <p>本事实只回答动作结果和已经证明的资金效果，不证明账本、余额、外部终局或对账闭合。</p>
 *
 * @author Codex
 * @date 2026-08-14
 */
@Value
public class FundsActionFactDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3735069248258212257L;

    @Schema(description = "动作事实稳定引用")
    FundsActionFactRef identity;

    @Schema(description = "资金意图引用")
    String intentRef;

    @Schema(description = "资金尝试引用")
    String attemptRef;

    @Schema(description = "稳定动作类型")
    String actionKind;

    @Schema(description = "动作金额")
    Money money;

    @Schema(description = "动作领域结果")
    DomainOutcome outcome;

    @Schema(description = "已证明资金效果")
    FundsEffect fundsEffect;

    @Schema(description = "本动作承重语义摘要")
    SemanticDigest semanticDigest;

    @Schema(description = "本动作引用并分配的原资金事实")
    List<OriginalFundsFactRef> originalFundsFactRefs;

    @Schema(description = "冻结路由来源")
    List<FundsRouteProvenance> routeProvenance;

    /**
     * 后继动作引用的原资金事实及本次分配金额。
     */
    @Value
    public static class OriginalFundsFactRef implements Serializable {

        @Serial
        private static final long serialVersionUID = -2505450407701692082L;

        @Schema(description = "租户 ID")
        Long tenantId;

        @Schema(description = "原事实类型")
        String factType;

        @Schema(description = "原事实稳定身份")
        String factId;

        @Schema(description = "与本动作的因果关系")
        String relationRole;

        @Schema(description = "本动作分配给该原事实的金额")
        Money allocatedMoney;
    }

    /**
     * 事实所有方本地解释的动作结果。
     */
    @Value
    public static class DomainOutcome implements Serializable {

        @Serial
        private static final long serialVersionUID = 4387849534738863487L;

        @Schema(description = "结果事实所有方")
        String owner;

        @Schema(description = "所有方契约内的结果代码")
        String code;
    }

    /**
     * 已证明资金效果。
     */
    @Value
    public static class FundsEffect implements Serializable {

        @Serial
        private static final long serialVersionUID = 5275877011756367803L;

        @Schema(description = "效果类型")
        String effectKind;

        @Nullable
        @Schema(description = "已证明生效金额；proven-zero 时为空")
        Money provenMoney;
    }

    /**
     * 动作承重语义摘要。
     */
    @Value
    public static class SemanticDigest implements Serializable {

        @Serial
        private static final long serialVersionUID = 1993570133697478724L;

        @Schema(description = "摘要算法")
        String algorithm;

        @Schema(description = "摘要值")
        String value;

        @Schema(description = "摘要覆盖字段版本")
        String coveredFieldsVersion;
    }

    /**
     * 本动作使用的冻结路由引用。
     */
    @Value
    public static class FundsRouteProvenance implements Serializable {

        @Serial
        private static final long serialVersionUID = 5798823924459435311L;

        @Nullable
        @Schema(description = "后继动作对应的原资金事实；首次动作为空")
        OriginalFundsFactRef originalFundsFactRef;

        @Schema(description = "分配金额")
        Money allocatedMoney;

        @Schema(description = "原资金交易内冻结 RouteSnapshot 的稳定引用")
        RouteSnapshotRef routeSnapshotRef;

        @Schema(description = "来源角色")
        String provenanceRole;
    }

    /**
     * 冻结 RouteSnapshot 的稳定引用。
     */
    @Value
    public static class RouteSnapshotRef implements Serializable {

        @Serial
        private static final long serialVersionUID = -5128610425238814941L;

        @Schema(description = "租户 ID")
        Long tenantId;

        @Schema(description = "RouteSnapshot 稳定身份")
        StableIdentity identity;
    }

    /**
     * 事实所有方命名的稳定身份。
     */
    @Value
    public static class StableIdentity implements Serializable {

        @Serial
        private static final long serialVersionUID = -1263571884040246220L;

        @Schema(description = "事实所有方命名空间")
        String ownerNamespace;

        @Schema(description = "所有方命名空间内的稳定值")
        String value;
    }
}
