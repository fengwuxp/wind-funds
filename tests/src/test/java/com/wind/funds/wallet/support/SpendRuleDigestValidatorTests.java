package com.wind.funds.wallet.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spend Rule 摘要格式校验测试。
 */
class SpendRuleDigestValidatorTests {

    @Test
    void testAssertSha256DigestShouldAcceptSha256Digest() {
        assertThatCode(() -> SpendRuleDigestValidator.assertSha256Digest("sha256:spend-rule-digest", "规则摘要"))
                .doesNotThrowAnyException();
    }

    @Test
    void testAssertSha256DigestShouldRejectBlankOrNonSha256Digest() {
        assertThatThrownBy(() -> SpendRuleDigestValidator.assertSha256Digest(" ", "规则摘要"))
                .hasMessageContaining("规则摘要不能为空");
        assertThatThrownBy(() -> SpendRuleDigestValidator.assertSha256Digest("md5:spend-rule-digest", "规则摘要"))
                .hasMessageContaining("规则摘要必须使用 sha256:<digest> 格式");
    }
}
