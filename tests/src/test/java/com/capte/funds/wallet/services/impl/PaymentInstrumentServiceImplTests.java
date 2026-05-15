package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.wallet.dal.entities.PaymentInstrument;
import com.capte.funds.wallet.dal.entities.PaymentInstrumentBinding;
import com.capte.funds.wallet.dal.mapper.PaymentInstrumentBindingMapper;
import com.capte.funds.wallet.dal.mapper.PaymentInstrumentMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.integration.funds.wallet.enums.PaymentInstrumentDirection;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.capte.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentInstrumentServiceImplTests {

    @Test
    void createPaymentInstrumentShouldUseActiveStatusByDefault() {
        AtomicReference<PaymentInstrument> inserted = new AtomicReference<>();
        PaymentInstrumentMapper instrumentMapper = FundsAccountServiceTestSupport.mapper(
                PaymentInstrumentMapper.class,
                entityObject -> {
                    PaymentInstrument entity = (PaymentInstrument) entityObject;
                    entity.setId(101L);
                    inserted.set(entity);
                },
                query -> null
        );
        PaymentInstrumentServiceImpl service = new PaymentInstrumentServiceImpl(
                instrumentMapper,
                unsupportedBindingMapper()
        );

        Long id = service.createPaymentInstrument(new CreatePaymentInstrumentRequest()
                .setSn("card_001")
                .setTenantId(1L)
                .setOwnerId("user_001")
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("SHARE_VCC")
                .setInstrumentDirection(PaymentInstrumentDirection.PAYMENT)
                .setInstrumentNo("CARD_****9876")
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(101L);
        PaymentInstrument entity = inserted.get();
        assertThat(entity.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(entity.getInstrumentDirection()).isEqualTo(PaymentInstrumentDirection.PAYMENT);
        assertThat(entity.getInstrumentNo()).isEqualTo("CARD_****9876");
    }

    @Test
    void createPaymentInstrumentBindingShouldSetRouteDefaults() {
        AtomicReference<PaymentInstrumentBinding> inserted = new AtomicReference<>();
        PaymentInstrumentBindingMapper bindingMapper = FundsAccountServiceTestSupport.mapper(
                PaymentInstrumentBindingMapper.class,
                entityObject -> {
                    PaymentInstrumentBinding entity = (PaymentInstrumentBinding) entityObject;
                    entity.setId(102L);
                    inserted.set(entity);
                },
                query -> null
        );
        PaymentInstrumentServiceImpl service = new PaymentInstrumentServiceImpl(
                unsupportedInstrumentMapper(),
                bindingMapper
        );

        Long id = service.createPaymentInstrumentBinding(new CreatePaymentInstrumentBindingRequest()
                .setSn("bind_001")
                .setTenantId(1L)
                .setInstrumentSn("card_001")
                .setBindingRole(PaymentInstrumentBindingRole.CREDIT_SUBJECT)
                .setSubjectId("credit_001")
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(102L);
        PaymentInstrumentBinding entity = inserted.get();
        assertThat(entity.getPriority()).isZero();
        assertThat(entity.getDefaultBinding()).isFalse();
        assertThat(entity.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
    }

    private static PaymentInstrumentMapper unsupportedInstrumentMapper() {
        return FundsAccountServiceTestSupport.mapper(
                PaymentInstrumentMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> null
        );
    }

    private static PaymentInstrumentBindingMapper unsupportedBindingMapper() {
        return FundsAccountServiceTestSupport.mapper(
                PaymentInstrumentBindingMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> null
        );
    }
}
