package com.wind.funds.wallet.service;

import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.model.dto.PaymentInstrumentBindingDTO;
import com.wind.funds.wallet.model.query.PaymentInstrumentBindingQuery;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.UpdatePaymentInstrumentBindingRequest;
import org.jspecify.annotations.NonNull;

/**
 * 支付工具绑定基础服务。
 *
 * <p>职责：封装支付工具绑定当前态的基础持久化、分页查询和唯一性辅助判断。</p>
 *
 * <p>边界：本服务不解析资金责任、不创建交易或账务事实，也不追加绑定历史。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface PaymentInstrumentBindingService {

    /**
     * 创建支付工具绑定当前态。
     *
     * @param request 创建请求
     * @return 绑定主键
     */
    @NonNull Long createPaymentInstrumentBinding(@NonNull CreatePaymentInstrumentBindingRequest request);

    /**
     * 根据主键查询支付工具绑定。
     *
     * @param id 主键
     * @return 支付工具绑定
     */
    @NonNull PaymentInstrumentBindingDTO getPaymentInstrumentBindingById(@NonNull Long id);

    /**
     * 按租户和绑定号查询支付工具绑定。
     *
     * @param tenantId 租户 ID
     * @param bindingSn 绑定号
     * @return 支付工具绑定
     */
    @NonNull PaymentInstrumentBindingDTO getPaymentInstrumentBinding(@NonNull Long tenantId,
                                                                     @NonNull String bindingSn);

    /**
     * 分页查询支付工具绑定。
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 支付工具绑定分页结果
     */
    @NonNull WindPagination<PaymentInstrumentBindingDTO> queryPaymentInstrumentBindings(
            @NonNull PaymentInstrumentBindingQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 按期望版本更新支付工具绑定当前态。
     *
     * @param request 更新请求
     * @return 绑定主键
     */
    @NonNull Long updatePaymentInstrumentBinding(@NonNull UpdatePaymentInstrumentBindingRequest request);

    /**
     * 按期望版本删除支付工具绑定当前态。
     *
     * @param tenantId 租户 ID
     * @param bindingSn 绑定号
     * @param expectedVersion 期望版本
     */
    void deletePaymentInstrumentBinding(@NonNull Long tenantId,
                                        @NonNull String bindingSn,
                                        @NonNull Integer expectedVersion);

    /**
     * 判断是否存在同工具、同角色、同币种的重叠 ACTIVE 默认绑定。
     *
     * @param binding 待校验绑定
     * @return 存在重叠绑定时返回 true
     */
    boolean existsOverlappingActiveDefaultBinding(@NonNull PaymentInstrumentBindingDTO binding);

    /**
     * 判断是否存在同工具、同角色、同币种、同优先级的重叠 ACTIVE 绑定。
     *
     * @param binding 待校验绑定
     * @return 存在重叠绑定时返回 true
     */
    boolean existsOverlappingActivePriorityBinding(@NonNull PaymentInstrumentBindingDTO binding);
}
