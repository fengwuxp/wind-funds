package com.wind.funds.route;

import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 账户层级快照解析端口。
 *
 * <p>职责：根据账务主体和事件时间读取账户父子关系来源，并生成 Route 可固化的账户层级快照。
 * 该端口只输出快照，不维护账户关系，不生成交易、账本分录或余额投影。</p>
 */
public interface AccountHierarchySnapshotResolver {

    /**
     * 解析账户层级快照。
     *
     * <p>能力范围：只读查询当前事件时间下生效的账户层级关系；不存在关系时返回空，用于保持非层级账户路径兼容。</p>
     *
     * @param accountRef 账务主体引用
     * @param effectiveAt 事件时间
     * @return 账户层级快照
     */
    @NonNull
    Optional<AccountHierarchySnapshotSpec> resolve(@NonNull SubjectRef accountRef, @NonNull LocalDateTime effectiveAt);
}
