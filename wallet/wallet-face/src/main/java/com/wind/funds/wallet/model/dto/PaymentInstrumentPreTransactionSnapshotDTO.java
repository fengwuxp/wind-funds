package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 支付工具预交易快照 DTO。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PaymentInstrumentPreTransactionSnapshotDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -7193620392135925104L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "支付工具动作")
    private PaymentInstrumentAction action;

    @Schema(description = "交易金额，最小货币单位")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "支付工具绑定角色")
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "资金责任关系类型")
    private SpendSubjectFundingRelationType relationType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    private String businessSn;

    @Schema(description = "预交易快照是否已具备进入交易内核的准入条件")
    private Boolean ready;

    @Schema(description = "最终解析出的资金账户或信用账户标识")
    private FundsAccountId targetAccountId;

    @Schema(description = "支付工具能力准入决策")
    private PaymentInstrumentCapabilityDecisionDTO paymentInstrumentCapability;

    @Schema(description = "资金责任解析决策")
    private FundingResponsibilityDecisionDTO fundingResponsibility;

    @Schema(description = "目标账户能力准入决策")
    private FundsAccountCapabilityDecisionDTO fundsAccountCapability;
}
