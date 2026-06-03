package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingHistoryDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingHistoryQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.wind.funds.wallet.model.query.PaymentInstrumentQuery;
import com.wind.funds.wallet.model.request.ChangePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具服务。
 *
 * <p>职责：管理收款或付款工具，以及工具与资金主体、信用主体、预算主体之间的绑定关系。</p>
 *
 * <p>边界：支付工具本身不表达余额；实际可用余额和额度仍由绑定主体的 ledger 统一计算。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface PaymentInstrumentService {

    /**
     * 创建支付或收款工具。
     *
     * <p>能力范围：创建工具元数据，如卡、VA、外部收款标识等，不创建资金账户。</p>
     *
     * @param request 创建请求
     * @return 工具主键
     */
    @NonNull Long createPaymentInstrument(@NonNull CreatePaymentInstrumentRequest request);

    /**
     * 创建支付工具和资金主体绑定。
     *
     * <p>能力范围：建立工具到可支出或可收款主体的关系快照，用于后续 Route 解析。</p>
     *
     * @param request 创建请求
     * @return 绑定主键
     */
    @NonNull Long createPaymentInstrumentBinding(@NonNull CreatePaymentInstrumentBindingRequest request);

    /**
     * 变更支付工具绑定当前态。
     *
     * <p>能力范围：只更新当前候选关系，并追加绑定历史；不得覆盖历史证据。</p>
     *
     * @param request 变更请求
     * @return 当前绑定主键
     */
    @NonNull Long changePaymentInstrumentBinding(@NonNull ChangePaymentInstrumentBindingRequest request);

    /**
     * 根据主键查询支付工具。
     *
     * <p>能力范围：只读查询支付工具元数据，不查询绑定主体余额。</p>
     *
     * @param id 主键
     * @return 支付工具
     */
    @NonNull PaymentInstrumentDTO getPaymentInstrumentById(@NonNull Long id);

    /**
     * 根据工具号查询支付工具。
     *
     * <p>能力范围：按工具流水号查询工具元数据。</p>
     *
     * @param sn 工具号
     * @return 支付工具
     */
    @NonNull PaymentInstrumentDTO getPaymentInstrumentBySn(@NonNull String sn);

    /**
     * 分页查询支付工具。
     *
     * <p>能力范围：只读分页查询支付工具，不做路由解析。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 支付工具分页结果
     */
    @NonNull WindPagination<PaymentInstrumentDTO> queryPaymentInstruments(
            @NonNull PaymentInstrumentQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 分页查询支付工具绑定。
     *
     * <p>能力范围：只读分页查询绑定关系，用于 RouteResolver 选择资金或预算主体。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 支付工具绑定分页结果
     */
    @NonNull WindPagination<PaymentInstrumentBindingDTO> queryPaymentInstrumentBindings(
            @NonNull PaymentInstrumentBindingQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 分页查询支付工具绑定历史。
     *
     * <p>能力范围：只读查询绑定审计证据，不作为新交易路由候选来源。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 支付工具绑定历史分页结果
     */
    @NonNull WindPagination<PaymentInstrumentBindingHistoryDTO> queryPaymentInstrumentBindingHistories(
            @NonNull PaymentInstrumentBindingHistoryQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);
}
