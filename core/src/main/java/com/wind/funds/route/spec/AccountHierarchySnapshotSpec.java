package com.wind.funds.route.spec;

import com.wind.funds.route.ref.SubjectRef;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 账户层级快照。
 *
 * <p>用于在支付工具、VCC 卡或多级账户路由完成后，固化当次资金责任实际落账账户及其父级关系。</p>
 */
public interface AccountHierarchySnapshotSpec {

    /**
     * 当次实际落账账户，只允许资金账户或信用账户。
     *
     * @return 实际落账账户
     */
    @NonNull
    SubjectRef getAccountRef();

    /**
     * 直接父账户，适用于子账户或卡账户绑定场景。
     *
     * @return 直接父账户
     */
    @Nullable
    default SubjectRef getParentAccountRef() {
        return null;
    }

    /**
     * 层级快照扩展上下文，不承载卡号、外部账户原文或通道密钥等敏感信息。
     *
     * @return 扩展上下文
     */
    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}
