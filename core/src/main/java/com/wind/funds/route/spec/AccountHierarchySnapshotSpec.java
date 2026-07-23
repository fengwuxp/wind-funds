package com.wind.funds.route.spec;

import com.wind.funds.route.ref.SubjectRef;
import org.jspecify.annotations.NonNull;

/**
 * 账户层级快照。
 *
 * <p>用于在资金路由完成后，固化参与账户当时使用的直接父账户关系。</p>
 */
public interface AccountHierarchySnapshotSpec {

    /**
     * 账户层级关系号。
     *
     * @return 关系号
     */
    @NonNull
    String getRelationSn();

    /**
     * 直接父账户，适用于子账户或卡账户绑定场景。
     *
     * @return 直接父账户
     */
    @NonNull
    SubjectRef getParentAccountRef();
}
