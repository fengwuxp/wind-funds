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

    /**
     * 场景：创建支付工具。
     * 输入：用户共享虚拟卡支付工具，方向为 PAYMENT，币种 USD。
     * 输出：写入支付工具记录。
     * 预期：默认状态为 ACTIVE，并保留支付方向和脱敏工具号。
     * 红线：支付工具只是支付媒介，不得被当作账本主体或资金余额来源。
     */
    @Test
    void testCreatePaymentInstrumentShouldUseActiveStatusByDefault() {
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

    /**
     * 场景：绑定支付工具与资金主体。
     * 输入：卡工具绑定到 CREDIT_ACCOUNT 主体，绑定角色为 CREDIT_SUBJECT。
     * 输出：写入支付工具绑定记录。
     * 预期：默认优先级为 0、非默认绑定、状态为 ACTIVE。
     * 红线：绑定关系只服务 route 解析，不得直接改写账户、交易事实或账本事实。
     */
    @Test
    void testCreatePaymentInstrumentBindingShouldSetRouteDefaults() {
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
