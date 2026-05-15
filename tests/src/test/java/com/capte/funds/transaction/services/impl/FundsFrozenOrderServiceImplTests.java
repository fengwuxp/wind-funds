package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.transaction.model.dto.FundsFrozenOrderDTO;
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
     * 场景：冻结入账流水由后续链路生成。
     * 输入：检查请求模型和实体模型上的 freezeLedgerTransactionSn 字段约束。
     * 输出：字段不带 NotBlank/NotNull 强约束。
     * 预期：调用方创建冻结单时无需传入冻结账本交易流水。
     * 红线：不得把异步生成的账本流水变成创建冻结单的强制入参。
     */
    @Test
    void testFreezeLedgerTransactionSnShouldNotBeCallerRequired() throws NoSuchFieldException {
        Field requestField = CreateFundsFrozenOrderRequest.class.getDeclaredField("freezeLedgerTransactionSn");
        Field entityField = FundsFrozenOrder.class.getDeclaredField("freezeLedgerTransactionSn");

        assertThat(requestField.isAnnotationPresent(NotBlank.class)).isFalse();
        assertThat(entityField.isAnnotationPresent(NotNull.class)).isFalse();
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

    /**
     * 场景：冻结单历史消费字段仍存在于兼容模型中。
     * 输入：冻结单状态枚举、Entity 和 DTO 的历史消费字段。
     * 输出：消费状态、consumedAmount、consumeTime 均标记为 Deprecated。
     * 预期：新代码不得继续把冻结单当作消费、扣划或跨主体价值转移载体。
     * 红线：兼容字段不得被误读为 P0 目标态冻结消费能力。
     */
    @Test
    void testFrozenOrderConsumptionCompatibilityFieldsShouldBeDeprecated() throws NoSuchFieldException {
        assertThat(FundsFrozenOrderStatus.class.getField("PARTIALLY_CONSUMED")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrderStatus.class.getField("CONSUMED")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrder.class.getDeclaredField("consumedAmount")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrder.class.getDeclaredField("consumeTime")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrderDTO.class.getDeclaredField("consumedAmount")
                .isAnnotationPresent(Deprecated.class)).isTrue();
        assertThat(FundsFrozenOrderDTO.class.getDeclaredField("consumeTime")
                .isAnnotationPresent(Deprecated.class)).isTrue();
    }

    /**
     * 场景：测试库表结构允许先登记冻结单再生成冻结入账流水。
     * 输入：H2/MySQL Mode 测试 schema 中的冻结单表定义。
     * 输出：freeze_ledger_transaction_sn 默认为 NULL，status 默认为 CREATED。
     * 预期：测试 schema 与冻结生命周期设计保持一致。
     * 红线：测试 DDL 不得重新把冻结账本流水变成创建冻结单的硬前置条件。
     */
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
