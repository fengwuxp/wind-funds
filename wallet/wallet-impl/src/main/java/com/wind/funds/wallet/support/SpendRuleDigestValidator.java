package com.wind.funds.wallet.support;

import com.wind.common.exception.AssertUtils;

/**
 * Spend Rule 摘要格式校验器。
 */
public final class SpendRuleDigestValidator {

    private static final String SHA256_PREFIX = "sha256:";

    private SpendRuleDigestValidator() {
    }

    /**
     * 断言摘要使用 sha256 格式。
     *
     * @param digest    摘要值
     * @param fieldName 字段名
     */
    public static void assertSha256Digest(String digest, String fieldName) {
        AssertUtils.hasText(digest, "{}不能为空", fieldName);
        AssertUtils.isTrue(digest.startsWith(SHA256_PREFIX) && digest.length() > SHA256_PREFIX.length(),
                "{}必须使用 sha256:<digest> 格式",
                fieldName);
    }
}
