package com.capte.funds.dsl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.integration.funds.util.FundsDslJsonContractVerifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DSL JSON 样例契约测试。
 */
class FundsDslJsonContractTests {

    /**
     * 场景：DSL 文档样例作为交易、路由、账务和投影测试的入口。
     * 预期：样例可被机器校验，金额表达、枚举字段和基础结构不漂移。
     * 红线：JSON 样例不能只停留在文档层，不能出现无法映射到 core 契约的字段。
     */
    @Test
    void testTransactionLayerJsonSamplesShouldRemainMachineVerifiable() throws IOException {
        List<Path> samples;
        try (Stream<Path> sampleStream = Files.list(transactionLayerDslDir())) {
            samples = sampleStream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }

        assertThat(samples).isNotEmpty();
        for (Path sample : samples) {
            JSONObject document = JSON.parseObject(Files.readString(sample));

            assertThatCode(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                    .as(sample.getFileName().toString())
                    .doesNotThrowAnyException();
        }
    }

    /**
     * 场景：样例作者误把金额写成主单位 value。
     * 预期：JSON 契约校验显式失败。
     * 红线：测试样例不能把小数主单位绕过金额最小单位边界。
     */
    @Test
    void testJsonContractVerifierShouldRejectMajorUnitMoneyShape() {
        Map<String, Object> document = Map.of(
                "caseId", "DSL-INVALID-MONEY-001",
                "instruction", Map.of(
                        "instructionType", "DIRECT_TRANSACTION",
                        "eventType", "PAY",
                        "transactionType", "PAY",
                        "amount", Map.of("currency", "USD", "value", "1.23")));

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.amount")
                .hasMessageContaining("money.minorValue is required");
    }

    /**
     * 场景：样例作者把生命周期事件误写入交易类型。
     * 预期：JSON 契约校验显式失败。
     * 红线：transactionType 不承载 AUTHORIZE、SETTLE、REVERSAL 等生命周期事件。
     */
    @Test
    void testJsonContractVerifierShouldRejectEventAsTransactionType() {
        Map<String, Object> document = Map.of(
                "caseId", "DSL-INVALID-TRANSACTION-TYPE-001",
                "instruction", Map.of(
                        "instructionType", "AUTHORIZATION_TRANSACTION",
                        "eventType", "AUTHORIZE",
                        "transactionType", "AUTHORIZE",
                        "amount", Map.of("currency", "USD", "minorValue", 100)));

        assertThatThrownBy(() -> FundsDslJsonContractVerifier.verifyTransactionLayerCase(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instruction.transactionType")
                .hasMessageContaining("DefaultFundsTransactionType");
    }

    private Path transactionLayerDslDir() {
        return workspaceRoot().resolve("core/src/test/resources/dsl/transaction-layer");
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (multiModuleDir != null && !multiModuleDir.isBlank()) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        if ("tests".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        return current;
    }
}
