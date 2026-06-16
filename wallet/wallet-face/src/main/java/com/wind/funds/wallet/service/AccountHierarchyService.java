package com.wind.funds.wallet.service;

import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.wallet.model.request.CreateAccountHierarchyBindingRequest;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 账户层级服务。
 *
 * <p>职责：维护资金账户、信用账户之间的父子关系来源，并为交易路由提供可固化的账户层级快照。</p>
 *
 * <p>边界：账户层级关系只用于归属、责任、账期或账单解释；不得把父账户自动作为交易落账主体，
 * 不创建账户、不初始化账本、不改变余额。</p>
 */
public interface AccountHierarchyService {

    /**
     * 创建账户层级绑定。
     *
     * <p>能力范围：登记一个子账户到父账户、根账户的当前关系；同一账户只能有一个 ACTIVE
     * 账户层级来源。</p>
     *
     * @param request 创建请求
     * @return 绑定主键
     */
    @NonNull Long createAccountHierarchyBinding(@NonNull CreateAccountHierarchyBindingRequest request);

    /**
     * 查询账户层级快照。
     *
     * <p>能力范围：只读返回指定事件对应的账户层级快照；当前按 ACTIVE 关系来源解析，
     * 不按有效期窗口切片；不存在关系时返回空。</p>
     *
     * @param accountRef 账户主体引用
     * @param effectiveAt 事件时间
     * @return 账户层级快照
     */
    @NonNull
    Optional<AccountHierarchySnapshotSpec> findAccountHierarchySnapshot(@NonNull SubjectRef accountRef,
                                                                        @NonNull LocalDateTime effectiveAt);
}
