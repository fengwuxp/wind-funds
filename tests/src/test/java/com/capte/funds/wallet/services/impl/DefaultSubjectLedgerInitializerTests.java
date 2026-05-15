package com.capte.funds.wallet.services.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.dto.LedgerProfileDTO;
import com.capte.funds.wallet.model.dto.LedgerProfileItemDTO;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.wallet.service.LedgerProfileService;
import com.wind.common.query.WindPagination;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.SettlementPolicySpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSubjectLedgerInitializerTests {

    /**
     * 场景：按账本 profile 初始化主体必需账本。
     * 输入：信用账户主体、CREDIT_BASIC profile，LIMIT/AVAILABLE 为必需账本，FROZEN 为非必需账本。
     * 输出：只创建必需账本，并返回必需科目到账本 ID 的映射。
     * 预期：创建请求继承主体、币种、profile、周期和结算策略信息。
     * 红线：初始化不得为非必需科目建账，也不得改变主体类型或账本 profile 语义。
     */
    @Test
    void testInitializeRequiredLedgersShouldCreateOnlyRequiredLedgers() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.CREDIT_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setItems(List.of(
                        item(LedgerSubjectCode.LIMIT, EntrySide.DEBIT, false, true),
                        item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true),
                        item(LedgerSubjectCode.FROZEN, EntrySide.CREDIT, false, false)
                ));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                ledgerService(requests, List.of())
        );

        Map<LedgerSubjectCode, Long> result = initializer.initializeRequiredLedgers(
                new InitializeSubjectLedgerRequest()
                        .setTenantId(1L)
                        .setSubjectId("credit_001")
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)
        );

        assertThat(result).containsEntry(LedgerSubjectCode.LIMIT, 101L)
                .containsEntry(LedgerSubjectCode.AVAILABLE, 102L)
                .doesNotContainKey(LedgerSubjectCode.FROZEN);
        assertThat(requests)
                .extracting(CreateLedgerRequest::getLedgerSubjectCode)
                .containsExactly(LedgerSubjectCode.LIMIT, LedgerSubjectCode.AVAILABLE);

        CreateLedgerRequest first = requests.getFirst();
        assertThat(first.getTenantId()).isEqualTo(1L);
        assertThat(first.getSubjectId()).isEqualTo("credit_001");
        assertThat(first.getSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT.name());
        assertThat(first.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(first.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.CREDIT_BASIC.name());
        assertThat(first.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(first.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(first.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        assertThat(first.getSettlementPolicy()).isEqualTo(SettlementPolicySpec.RT.getRaw());
        assertThat(first.getCutOffTime()).isEqualTo(LocalTime.MIDNIGHT);
    }

    /**
     * 场景：账本 profile 与请求主体类型不一致。
     * 输入：FUNDING_BASIC profile 绑定 FUNDING_ACCOUNT，请求却声明 CREDIT_ACCOUNT。
     * 输出：初始化失败且不创建任何账本。
     * 预期：错误信息带出 profileCode 和 subjectType，便于定位配置问题。
     * 红线：不得用不匹配的 profile 给主体建账，避免账本主体边界被污染。
     */
    @Test
    void testInitializeRequiredLedgersShouldRejectProfileSubjectTypeMismatch() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.FUNDING_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setItems(List.of(item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true)));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                ledgerService(requests, List.of())
        );

        assertThatThrownBy(() -> initializer.initializeRequiredLedgers(
                new InitializeSubjectLedgerRequest()
                        .setTenantId(1L)
                        .setSubjectId("credit_001")
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LedgerProfile 主体类型不匹配")
                .hasMessageContaining("profileCode = FUNDING_BASIC")
                .hasMessageContaining("subjectType = CREDIT_ACCOUNT");
        assertThat(requests).isEmpty();
    }

    /**
     * 场景：主体账本初始化请求被重复提交。
     * 输入：信用账户主体已经存在 LIMIT/AVAILABLE required ledgers，再次按 CREDIT_BASIC 初始化。
     * 输出：返回既有账本 ID，不发起新的 createLedger。
     * 预期：初始化遵循 ledger 唯一桶幂等语义，重复调用不制造重复账本。
     * 红线：开户重试不得把同一主体、币种、周期和账目重复建账，也不得依赖交易路径补账。
     */
    @Test
    void testInitializeRequiredLedgersShouldReuseExistingRequiredLedgers() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.CREDIT_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setItems(List.of(
                        item(LedgerSubjectCode.LIMIT, EntrySide.DEBIT, false, true),
                        item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true)
                ));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                ledgerService(requests, List.of(
                        ledger(901L, LedgerSubjectCode.LIMIT, EntrySide.DEBIT, false),
                        ledger(902L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true)
                ))
        );

        Map<LedgerSubjectCode, Long> result = initializer.initializeRequiredLedgers(
                new InitializeSubjectLedgerRequest()
                        .setTenantId(1L)
                        .setSubjectId("credit_001")
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)
        );

        assertThat(result).containsEntry(LedgerSubjectCode.LIMIT, 901L)
                .containsEntry(LedgerSubjectCode.AVAILABLE, 902L)
                .hasSize(2);
        assertThat(requests).isEmpty();
    }

    /**
     * 场景：唯一账本桶已存在，但既有账本配置与当前 profile 不一致。
     * 输入：CREDIT_BASIC 期望 AVAILABLE 允许受控负余额，既有 AVAILABLE 账本却记录 allowNegative=false。
     * 输出：初始化失败且不创建新账本。
     * 预期：错误信息带出 subjectId 和 ledgerSubjectCode，便于排查 profile 漂移。
     * 红线：幂等复用不得掩盖账本 profile、余额方向或负余额策略漂移。
     */
    @Test
    void testInitializeRequiredLedgersShouldRejectExistingLedgerProfileMismatch() {
        LedgerProfileDTO profile = new LedgerProfileDTO()
                .setCode(LedgerProfileCode.CREDIT_BASIC)
                .setVersion(1)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setItems(List.of(item(LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, true, true)));
        List<CreateLedgerRequest> requests = new ArrayList<>();
        DefaultSubjectLedgerInitializer initializer = new DefaultSubjectLedgerInitializer(
                ledgerProfileService(profile),
                ledgerService(requests, List.of(ledger(902L, LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT, false)))
        );

        assertThatThrownBy(() -> initializer.initializeRequiredLedgers(
                new InitializeSubjectLedgerRequest()
                        .setTenantId(1L)
                        .setSubjectId("credit_001")
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("已存在账本与初始化 profile 不一致")
                .hasMessageContaining("subjectId = credit_001")
                .hasMessageContaining("ledgerSubjectCode = AVAILABLE");
        assertThat(requests).isEmpty();
    }

    private static LedgerProfileItemDTO item(LedgerSubjectCode code,
                                             EntrySide normalBalanceSide,
                                             boolean allowNegative,
                                             boolean required) {
        return new LedgerProfileItemDTO()
                .setLedgerSubjectCode(code)
                .setLedgerSubjectCategory(LedgerSubjectCategory.CONTROL)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setRequired(required)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setSettlementPolicy(SettlementPolicySpec.RT.getRaw())
                .setCutOffTime(LocalTime.MIDNIGHT);
    }

    private static LedgerDTO ledger(Long id,
                                    LedgerSubjectCode code,
                                    EntrySide normalBalanceSide,
                                    boolean allowNegative) {
        return new LedgerDTO()
                .setId(id)
                .setTenantId(1L)
                .setSubjectId("credit_001")
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT.name())
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(code)
                .setLedgerSubjectCategory(LedgerSubjectCategory.CONTROL)
                .setNormalBalanceSide(normalBalanceSide)
                .setAllowNegative(allowNegative)
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.formatPeriodId())
                .setSettlementPolicy(SettlementPolicySpec.RT.getRaw())
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setDebitAmount(0L)
                .setCreditAmount(0L);
    }

    @SuppressWarnings("unchecked")
    private static LedgerService ledgerService(List<CreateLedgerRequest> requests, List<LedgerDTO> ledgers) {
        AtomicLong sequence = new AtomicLong(100L);
        return (LedgerService) Proxy.newProxyInstance(
                LedgerService.class.getClassLoader(),
                new Class<?>[]{LedgerService.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "LedgerServiceProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    if ("createLedger".equals(method.getName())) {
                        requests.add((CreateLedgerRequest) args[0]);
                        return sequence.incrementAndGet();
                    }
                    if ("queryLedgers".equals(method.getName())) {
                        return pagination(filterLedgers((LedgerQuery) args[0], ledgers));
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static List<LedgerDTO> filterLedgers(LedgerQuery query, List<LedgerDTO> ledgers) {
        return ledgers.stream()
                .filter(ledger -> matches(query.getTenantId(), ledger.getTenantId()))
                .filter(ledger -> matches(query.getSubjectId(), ledger.getSubjectId()))
                .filter(ledger -> matches(query.getSubjectType(), ledger.getSubjectType()))
                .filter(ledger -> matches(query.getLedgerSubjectCode(), ledger.getLedgerSubjectCode()))
                .filter(ledger -> matches(query.getCurrency(), ledger.getCurrency()))
                .filter(ledger -> matches(query.getPeriodType(), ledger.getPeriodType()))
                .filter(ledger -> matches(query.getPeriodId(), ledger.getPeriodId()))
                .toList();
    }

    private static boolean matches(Object expected, Object actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    @SuppressWarnings("unchecked")
    private static WindPagination<LedgerDTO> pagination(List<LedgerDTO> ledgers) {
        return (WindPagination<LedgerDTO>) Proxy.newProxyInstance(
                WindPagination.class.getClassLoader(),
                new Class<?>[]{WindPagination.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "WindPaginationProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    if ("getRecords".equals(method.getName())) {
                        return ledgers;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static LedgerProfileService ledgerProfileService(LedgerProfileDTO profile) {
        return new LedgerProfileService() {
            @Override
            public LedgerProfileDTO getProfile(LedgerProfileCode profileCode) {
                assertThat(profileCode).isEqualTo(profile.getCode());
                return profile;
            }

            @Override
            public LedgerProfileItemDTO getRequiredItem(LedgerProfileCode profileCode,
                                                        LedgerSubjectCode subjectCode) {
                throw new UnsupportedOperationException("getRequiredItem");
            }
        };
    }
}
