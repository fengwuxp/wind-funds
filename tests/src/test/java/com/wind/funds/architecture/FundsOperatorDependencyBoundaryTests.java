package com.wind.funds.architecture;

import com.wind.funds.route.spec.ReplayRequestSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.operator.WindOperator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金 Core 使用统一操作者契约的架构边界测试。
 */
class FundsOperatorDependencyBoundaryTests {

    private static final List<String> FORBIDDEN_RUNTIME_OPERATION_TOKENS = List.of(
            ".hasAuthority(",
            ".hasAnyAuthority(",
            ".hasRole(",
            ".hasAnyRole(",
            ".isSuperAdmin(",
            ".getRequestSourceIp(",
            ".getRequestDeviceId(",
            ".getRequestDeviceUserAgent(");

    /**
     * 场景：资金指令和回放请求统一使用 WindOperator 表达当前操作者。
     * 预期：两个 Core 公共契约暴露相同的统一类型。
     * 红线：不得重新引入平行的资金操作者模型。
     */
    @Test
    void testCoreOperatorContractsShouldUseWindOperator() throws NoSuchMethodException {
        assertThat(FundsInstructionSpec.class.getMethod("getOperator").getReturnType())
                .isEqualTo(WindOperator.class);
        assertThat(ReplayRequestSpec.class.getMethod("getOperator").getReturnType())
                .isEqualTo(WindOperator.class);
    }

    /**
     * 场景：资金 Core 持有并透传 WindOperator。
     * 预期：Core 只使用稳定身份语义，不执行权限判断或读取动态请求信息。
     * 红线：运行时安全和 Trace 上下文不得成为资金 DSL 规则。
     */
    @Test
    void testCoreShouldNotUseWindOperatorRuntimeOperations() throws IOException {
        List<String> violations = new ArrayList<>();
        Path sourceRoot = workspaceRoot().resolve("core/src/main/java");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path javaFile : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList()) {
                String content = Files.readString(javaFile);
                if (!content.contains("import com.wind.integration.operator.WindOperator;")) {
                    continue;
                }
                for (String forbiddenToken : FORBIDDEN_RUNTIME_OPERATION_TOKENS) {
                    if (content.contains(forbiddenToken)) {
                        violations.add(workspaceRoot().relativize(javaFile) + " contains " + forbiddenToken);
                    }
                }
            }
        }

        assertThat(violations)
                .as("core must treat WindOperator as runtime identity context only")
                .isEmpty();
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (multiModuleDir != null && !multiModuleDir.isBlank()) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        return "tests".equals(current.getFileName().toString()) ? current.getParent() : current;
    }
}
