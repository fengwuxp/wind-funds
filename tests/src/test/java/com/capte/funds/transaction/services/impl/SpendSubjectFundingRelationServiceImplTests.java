package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.SpendSubjectFundingRel;
import com.capte.funds.transaction.dal.mapper.SpendSubjectFundingRelMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SpendSubjectFundingRelationServiceImplTests {

    @Test
    void createSpendSubjectFundingRelationShouldSetDefaults() {
        AtomicReference<SpendSubjectFundingRel> inserted = new AtomicReference<>();
        SpendSubjectFundingRelMapper mapper = FundsAccountServiceTestSupport.mapper(
                SpendSubjectFundingRelMapper.class,
                entityObject -> {
                    SpendSubjectFundingRel entity = (SpendSubjectFundingRel) entityObject;
                    entity.setId(201L);
                    inserted.set(entity);
                },
                query -> null
        );
        SpendSubjectFundingRelationServiceImpl service = new SpendSubjectFundingRelationServiceImpl(mapper);

        Long id = service.createSpendSubjectFundingRelation(new CreateSpendSubjectFundingRelationRequest()
                .setSn("rel_001")
                .setTenantId(1L)
                .setSpendSubjectId("budget_001")
                .setSpendSubjectType(FundsSubjectType.BUDGET_GROUP)
                .setFundingAccountId("funding_001")
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setDefaultRelation(true));

        assertThat(id).isEqualTo(201L);
        SpendSubjectFundingRel entity = inserted.get();
        assertThat(entity.getPriority()).isZero();
        assertThat(entity.getDefaultRelation()).isTrue();
        assertThat(entity.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
    }
}
