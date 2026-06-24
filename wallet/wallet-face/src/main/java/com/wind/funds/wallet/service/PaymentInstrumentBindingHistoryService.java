package com.wind.funds.wallet.service;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingHistoryQuery;
import com.wind.funds.wallet.model.request.RecordPaymentInstrumentBindingHistoryRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具绑定历史基础服务。
 *
 * <p>职责：封装支付工具绑定历史的追加和查询能力。</p>
 *
 * <p>边界：本服务只记录历史证据，不决定绑定是否可变更，也不参与 route 选择。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface PaymentInstrumentBindingHistoryService {

    /**
     * 追加支付工具绑定历史。
     *
     * @param request 记录请求
     * @return 历史主键
     */
    @NonNull Long recordPaymentInstrumentBindingHistory(
            @NonNull RecordPaymentInstrumentBindingHistoryRequest request);

    /**
     * 根据主键查询支付工具绑定历史。
     *
     * @param id 主键
     * @return 支付工具绑定历史
     */
    @NonNull PaymentInstrumentBindingHistoryDTO getPaymentInstrumentBindingHistoryById(@NonNull Long id);

    /**
     * 分页查询支付工具绑定历史。
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 支付工具绑定历史分页结果
     */
    @NonNull WindPagination<PaymentInstrumentBindingHistoryDTO> queryPaymentInstrumentBindingHistories(
            @NonNull PaymentInstrumentBindingHistoryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);
}
