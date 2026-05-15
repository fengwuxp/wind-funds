package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.wallet.dal.entities.BudgetGroup;
import com.capte.funds.wallet.dal.mapper.BudgetGroupMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.capte.funds.wallet.model.request.CreateBudgetGroupRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetGroupServiceImplTests {

    @Test
    void createBudgetGroupShouldApplyDefaultProfileAndMonthlyPeriod() {
        AtomicReference<BudgetGroup> inserted = new AtomicReference<>();
        BudgetGroupMapper budgetGroupMapper = FundsAccountServiceTestSupport.mapper(
                BudgetGroupMapper.class,
                entityObject -> {
                    BudgetGroup entity = (BudgetGroup) entityObject;
                    entity.setId(301L);
                    inserted.set(entity);
                },
                query -> null
        );
        FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer initializer =
                new FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer();
        BudgetGroupServiceImpl service = new BudgetGroupServiceImpl(
                budgetGroupMapper,
                initializer,
                FundsAccountServiceTestSupport.unsupportedLedgerService()
        );

        Long id = service.createBudgetGroup(new CreateBudgetGroupRequest()
                .setSn("budget_001")
                .setTenantId(1L)
                .setOwnerId("team_001")
                .setOwnerType(FundsAccountOwnerType.MERCHANT)
                .setBudgetType("TEAM_BUDGET")
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(301L);
        BudgetGroup entity = inserted.get();
        assertThat(entity).isNotNull();
        assertThat(entity.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(entity.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.BUDGET_BASIC);
        assertThat(entity.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);

        InitializeSubjectLedgerRequest initRequest = initializer.getRequest();
        assertThat(initRequest.getSubjectId()).isEqualTo("budget_001");
        assertThat(initRequest.getSubjectType()).isEqualTo(FundsSubjectType.BUDGET_GROUP);
        assertThat(initRequest.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.BUDGET_BASIC);
        assertThat(initRequest.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
    }
}
