package com.wind.funds.ledger;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.service.LedgerService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账本受控初始化公开契约测试。
 *
 * @author wuxp
 * @since 2026-08-20
 */
class LedgerControlledInitializationPublicContractTests {

    private static final String REQUEST_TYPE =
            "com.wind.funds.ledger.request.InitializeSubjectLedgerRequest";

    private static final List<String> RETIRED_TYPES = List.of(
            "com.wind.funds.ledger.spec.LedgerProfileSpec",
            "com.wind.funds.ledger.spec.LedgerProfileItemSpec",
            "com.wind.funds.wallet.service.LedgerProfileService",
            "com.wind.funds.wallet.service.SubjectLedgerInitializer",
            "com.wind.funds.wallet.model.dto.LedgerProfileDTO",
            "com.wind.funds.wallet.model.dto.LedgerProfileItemDTO",
            "com.wind.funds.wallet.model.request.InitializeSubjectLedgerRequest",
            "com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl",
            "com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer"
    );

    @Test
    void testControlledInitializationShouldExposeOnlyLedgerOwnedStableContract() {
        List<String> violations = new ArrayList<>();
        RETIRED_TYPES.stream()
                .filter(LedgerControlledInitializationPublicContractTests::isPresent)
                .map(type -> "retired public type still exists: " + type)
                .forEach(violations::add);

        Class<?> requestType = loadType(REQUEST_TYPE);
        if (requestType == null) {
            violations.add("missing Ledger-owned request: " + REQUEST_TYPE);
        } else if (!instanceFieldNames(requestType).stream().sorted().toList().equals(List.of(
                "currency", "ledgerProfileCode", "ledgerProfileVersion", "periodId",
                "periodType", "subjectId", "subjectType", "tenantId"))) {
            violations.add("Ledger-owned request fields do not match the accepted contract");
        }

        Method method = Arrays.stream(LedgerService.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("initializeRequiredLedgers"))
                .findFirst()
                .orElse(null);
        if (method == null) {
            violations.add("LedgerService.initializeRequiredLedgers is missing");
        } else if (requestType == null
                || method.getReturnType() != void.class
                || !Arrays.equals(method.getParameterTypes(), new Class<?>[]{requestType})) {
            violations.add("LedgerService.initializeRequiredLedgers must be void and accept only the Ledger request");
        }
        if (!LedgerProfileCode.class.isEnum()) {
            violations.add("LedgerProfileCode must remain the stable public profile reference");
        }

        assertThat(violations).isEmpty();
    }

    private static List<String> instanceFieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName)
                .toList();
    }

    private static boolean isPresent(String className) {
        return loadType(className) != null;
    }

    private static Class<?> loadType(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
}
