package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.wallet.dal.entities.SpendSubjectFundingRel;
import com.capte.funds.wallet.dal.mapper.SpendSubjectFundingRelMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SpendSubjectFundingRelationServiceImplTests {

    /**
     * 场景：创建支出主体与真实资金来源关系。
     * 输入：预算组 spend subject 绑定 funding account，关系类型为 FUNDING_SOURCE。
     * 输出：写入支出主体供资关系。
     * 预期：默认优先级为 0，defaultRelation=true，状态为 ACTIVE。
     * 红线：供资关系只描述路由候选关系，不得直接表达资金转移或账本入账。
     */
    @Test
    void testCreateSpendSubjectFundingRelationShouldSetDefaults() {
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
