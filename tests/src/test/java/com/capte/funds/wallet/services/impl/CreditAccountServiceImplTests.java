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

    /**
     * 场景：创建信用账户资金主体。
     * 输入：用户 SHARED_CARD 信用账户，币种 USD，未显式指定账本 profile 和周期。
     * 输出：写入信用账户，并初始化 CREDIT_BASIC profile 下的生命周期账本。
     * 预期：信用账户为 ACTIVE，periodType=LIFETIME，账本初始化请求使用 CREDIT_ACCOUNT 主体。
     * 红线：信用额度主体不得误用资金账户或预算组 profile，授权/消费控制必须落在信用账本边界内。
     */
    @Test
    void testCreateCreditAccountShouldApplyDefaultProfileAndPeriod() {
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
                .setAccountType(DefaultFundsAccountType.SHARED_CARD.name())
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
