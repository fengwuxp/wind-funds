package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.model.request.CreateFundsFrozenOrderRequest;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsFrozenOrderServiceImplTests {

    /**
     * 场景：创建已完成冻结入账的冻结单。
     * 输入：提现冻结请求包含 freezeLedgerTransactionSn。
     * 输出：冻结单落库，状态为 FROZEN，释放金额和消费金额初始化为 0。
     * 预期：冻结单只表达同主体 AVAILABLE -> FROZEN 控制事实。
     * 红线：冻结创建不得隐式表达消费、扣划或跨主体价值转移。
     */
    @Test
    void testCreateFundsFrozenOrderShouldInitializeReleasedAndConsumedAmounts() {
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

    /**
     * 场景：创建冻结单时冻结入账流水尚未生成。
     * 输入：提现冻结请求不包含 freezeLedgerTransactionSn。
     * 输出：冻结单落库，状态保持 CREATED。
     * 预期：冻结单允许先登记业务事实，再由后续链路补齐冻结入账流水。
     * 红线：不得要求调用方伪造或提前传入未生成的账本交易流水。
     */
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

    /**
     * 场景：外部调用创建冻结单时基础冻结事实不完整。
     * 输入：冻结单号为空、金额为 0、币种为空三类请求。
     * 输出：服务层在写库前拒绝请求。
     * 预期：冻结单必须具备可追溯单号、正向金额和明确币种。
     * 红线：不得持久化无法解释金额口径或审计流水的冻结单。
     */
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

    /**
     * 场景：外部调用创建冻结单时传入历史消费状态。
     * 输入：`status=PARTIALLY_CONSUMED/CONSUMED` 的冻结单创建请求。
     * 输出：服务层在写库前拒绝请求。
     * 预期：冻结单创建只允许表达冻结事实，不接受消费状态。
     * 红线：冻结单不得承载消费、扣划或跨主体价值转移语义。
     */
    @Test
    void testCreateFundsFrozenOrderShouldRejectConsumptionStatusBeforeInsert() {
        AtomicReference<FundsFrozenOrder> inserted = new AtomicReference<>();
        FundsFrozenOrderMapper mapper = FundsAccountServiceTestSupport.mapper(
                FundsFrozenOrderMapper.class,
                entityObject -> inserted.set((FundsFrozenOrder) entityObject),
                query -> null
        );
        FundsFrozenOrderServiceImpl service = new FundsFrozenOrderServiceImpl(mapper);

        assertThatThrownBy(() -> service.createFundsFrozenOrder(validCreateRequest()
                .setStatus(FundsFrozenOrderStatus.PARTIALLY_CONSUMED)))
                .hasMessageContaining("冻结单状态不允许表达消费");
        assertThatThrownBy(() -> service.createFundsFrozenOrder(validCreateRequest()
                .setStatus(FundsFrozenOrderStatus.CONSUMED)))
                .hasMessageContaining("冻结单状态不允许表达消费");

        assertThat(inserted).hasValue(null);
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
