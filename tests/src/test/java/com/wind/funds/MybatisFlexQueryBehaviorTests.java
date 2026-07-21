package com.wind.funds;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MybatisFlexQueryBehaviorTests {

    @Test
    void testShouldPreserveEmptyAndInSizeRules() {
        assertThat(AbstractFundsServiceTest.shouldIgnoreQueryValue(null)).isTrue();
        assertThat(AbstractFundsServiceTest.shouldIgnoreQueryValue(List.of())).isTrue();
        assertThat(AbstractFundsServiceTest.shouldIgnoreQueryValue(List.of(1))).isFalse();
        assertThat(AbstractFundsServiceTest.shouldIgnoreQueryValue(new Object[]{null})).isTrue();
        assertThatThrownBy(() -> AbstractFundsServiceTest.shouldIgnoreQueryValue(
                Collections.nCopies(5120, 1)))
                .hasMessageContaining("database query in op size range");
    }
}
