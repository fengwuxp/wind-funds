package com.wind.funds.spec.ledger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 账务主体的账本 profile。
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface LedgerProfileSpec {

    /**
     * @return profile 编码
     */
    @NonNull
    String getProfileCode();

    /**
     * @return profile 名称
     */
    @NonNull
    String getProfileName();

    /**
     * @return 适用主体类型
     */
    @NonNull
    String getSubjectType();

    /**
     * @return profile 版本
     */
    @NonNull
    Integer getProfileVersion();

    /**
     * @return 状态
     */
    @NonNull
    String getStatus();

    /**
     * @return 描述
     */
    @Nullable
    String getDescription();

    /**
     * @return 科目配置
     */
    @NonNull
    List<LedgerProfileItemSpec> getItems();
}
