package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

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
}
