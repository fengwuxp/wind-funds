package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.wallet.dal.entities.CreditAccount;
import com.capte.funds.wallet.dal.mapper.CreditAccountMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.request.CreateCreditAccountRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CreditAccountServiceImplTests {

    @Test
    void createCreditAccountShouldApplyDefaultProfileAndPeriod() {
        AtomicReference<CreditAccount> inserted = new AtomicReference<>();
        CreditAccountMapper creditAccountMapper = FundsAccountServiceTestSupport.mapper(
                CreditAccountMapper.class,
                entityObject -> {
                    CreditAccount entity = (CreditAccount) entityObject;
                    entity.setId(201L);
                    inserted.set(entity);
                },
                query -> null
        );
        FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer initializer =
                new FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer();
        CreditAccountServiceImpl service = new CreditAccountServiceImpl(
                creditAccountMapper,
                initializer,
                FundsAccountServiceTestSupport.unsupportedLedgerService()
        );

        Long id = service.createCreditAccount(new CreateCreditAccountRequest()
                .setSn("credit_001")
                .setTenantId(1L)
                .setOwnerId("user_001")
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(DefaultFundsAccountType.SHARE_VCC.name())
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(201L);
        CreditAccount entity = inserted.get();
        assertThat(entity).isNotNull();
        assertThat(entity.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(entity.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.CREDIT_BASIC);
        assertThat(entity.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);

        InitializeSubjectLedgerRequest initRequest = initializer.getRequest();
        assertThat(initRequest.getSubjectId()).isEqualTo("credit_001");
        assertThat(initRequest.getSubjectType()).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT);
        assertThat(initRequest.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.CREDIT_BASIC);
        assertThat(initRequest.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
    }
}
