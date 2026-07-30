package com.wind.funds.reconciliation.service;

import com.wind.funds.reconciliation.model.dto.PayoutOrderDTO;
import com.wind.funds.reconciliation.model.dto.PayoutSubmissionAdmissionDecisionDTO;
import com.wind.funds.reconciliation.model.request.SubmitPayoutOrderRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 宿主提供的权威出款准入边界。
 *
 * <p>宿主负责账户、端点、通道、风控和外部规则事实；未配置实现时出款提交失败关闭。</p>
 */
@NullMarked
public interface PayoutSubmissionAuthority {

    PayoutSubmissionAdmissionDecisionDTO authorize(PayoutOrderDTO order,
                                                    SubmitPayoutOrderRequest request,
                                                    WindOperator operator);
}
