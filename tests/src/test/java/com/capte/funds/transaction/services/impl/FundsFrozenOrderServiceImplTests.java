package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsFrozenOrderServiceImplTests {

    @Test
    void createFundsFrozenOrderShouldInitializeReleasedAndConsumedAmounts() {
        AtomicReference<FundsFrozenOrder> inserted = new AtomicReference<>();
        FundsFrozenOrderMapper mapper = FundsAccountServiceTestSupport.mapper(
                FundsFrozenOrderMapper.class,
                entityObject -> {
                    FundsFrozenOrder entity = (FundsFrozenOrder) entityObject;
                    entity.setId(401L);
                    inserted.set(entity);
                },
                query -> null
        );
        FundsFrozenOrderServiceImpl service = new FundsFrozenOrderServiceImpl(mapper);

        Long id = service.createFundsFrozenOrder(new CreateFundsFrozenOrderRequest()
                .setSn("frozen_001")
                .setTenantId(1L)
                .setSubjectId("funding_001")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setFreezeType("WITHDRAW")
                .setBusinessScene("WITHDRAW_APPLY")
                .setBusinessSn("wd_001")
                .setFreezeLedgerTransactionSn("ledger_txn_001")
                .setAmount(1000L)
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(401L);
        FundsFrozenOrder entity = inserted.get();
        assertThat(entity.getStatus()).isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(entity.getReleasedAmount()).isZero();
        assertThat(entity.getConsumedAmount()).isZero();
    }

    @Test
    void testCreateFundsFrozenOrderShouldStayCreatedWhenLedgerTransactionGeneratedLater() {
        AtomicReference<FundsFrozenOrder> inserted = new AtomicReference<>();
        FundsFrozenOrderMapper mapper = FundsAccountServiceTestSupport.mapper(
                FundsFrozenOrderMapper.class,
                entityObject -> {
                    FundsFrozenOrder entity = (FundsFrozenOrder) entityObject;
                    entity.setId(402L);
                    inserted.set(entity);
                },
                query -> null
        );
        FundsFrozenOrderServiceImpl service = new FundsFrozenOrderServiceImpl(mapper);

        Long id = service.createFundsFrozenOrder(new CreateFundsFrozenOrderRequest()
                .setSn("frozen_pending_001")
                .setTenantId(1L)
                .setSubjectId("funding_001")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setFreezeType("WITHDRAW")
                .setBusinessScene("WITHDRAW_APPLY")
                .setBusinessSn("wd_pending_001")
                .setAmount(1000L)
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(402L);
        FundsFrozenOrder entity = inserted.get();
        assertThat(entity.getStatus()).isEqualTo(FundsFrozenOrderStatus.CREATED);
        assertThat(entity.getFreezeLedgerTransactionSn()).isNull();
        assertThat(entity.getReleasedAmount()).isZero();
        assertThat(entity.getConsumedAmount()).isZero();
    }

    @Test
    void testFreezeLedgerTransactionSnShouldNotBeCallerRequired() throws NoSuchFieldException {
        Field requestField = CreateFundsFrozenOrderRequest.class.getDeclaredField("freezeLedgerTransactionSn");
        Field entityField = FundsFrozenOrder.class.getDeclaredField("freezeLedgerTransactionSn");

        assertThat(requestField.isAnnotationPresent(NotBlank.class)).isFalse();
        assertThat(entityField.isAnnotationPresent(NotNull.class)).isFalse();
    }

    @Test
    void testCreateFundsFrozenOrderShouldRejectInvalidFrozenFactBeforeInsert() {
        AtomicReference<FundsFrozenOrder> inserted = new AtomicReference<>();
        FundsFrozenOrderMapper mapper = FundsAccountServiceTestSupport.mapper(
                FundsFrozenOrderMapper.class,
                entityObject -> inserted.set((FundsFrozenOrder) entityObject),
                query -> null
        );
        FundsFrozenOrderServiceImpl service = new FundsFrozenOrderServiceImpl(mapper);

        assertThatThrownBy(() -> service.createFundsFrozenOrder(validCreateRequest().setSn(" ")))
                .hasMessageContaining("冻结单号不能为空");
        assertThatThrownBy(() -> service.createFundsFrozenOrder(validCreateRequest().setAmount(0L)))
                .hasMessageContaining("冻结金额必须大于 0");
        assertThatThrownBy(() -> service.createFundsFrozenOrder(validCreateRequest().setCurrency(null)))
                .hasMessageContaining("冻结币种不能为空");

        assertThat(inserted).hasValue(null);
    }

    @Test
    void testJdbcSchemaShouldAllowPendingFreezeLedgerTransactionSn() throws IOException {
        String schema = new String(Objects.requireNonNull(getClass().getClassLoader()
                .getResourceAsStream("jdbc-schema.sql")).readAllBytes(), StandardCharsets.UTF_8);
        String freezeLedgerTransactionSnLine = schema.lines()
                .filter(line -> line.contains("`freeze_ledger_transaction_sn`"))
                .findFirst()
                .orElseThrow();

        assertThat(freezeLedgerTransactionSnLine).contains("DEFAULT NULL");
        assertThat(freezeLedgerTransactionSnLine).doesNotContain("NOT NULL");
        assertThat(schema.lines()
                .filter(line -> line.contains("`status`") && line.contains("冻结单状态"))
                .findFirst()
                .orElseThrow()).contains("DEFAULT 'CREATED'");
    }

    private static CreateFundsFrozenOrderRequest validCreateRequest() {
        return new CreateFundsFrozenOrderRequest()
                .setSn("frozen_valid_001")
                .setTenantId(1L)
                .setSubjectId("funding_001")
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setFreezeType("WITHDRAW")
                .setBusinessScene("WITHDRAW_APPLY")
                .setBusinessSn("wd_valid_001")
                .setAmount(1000L)
                .setCurrency(CurrencyIsoCode.USD);
    }
}
