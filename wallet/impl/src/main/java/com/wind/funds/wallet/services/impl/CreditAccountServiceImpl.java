package com.wind.funds.wallet.services.impl;

import com.wind.funds.wallet.dal.entities.CreditAccount;
import com.wind.funds.wallet.dal.entities.table.CreditAccountNameRefs;
import com.wind.funds.wallet.dal.mapper.CreditAccountMapper;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.mapstruct.CreditAccountConverter;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.query.CreditAccountQuery;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 信用账户服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class CreditAccountServiceImpl implements CreditAccountService {

    private final CreditAccountMapper creditAccountMapper;

    private final LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createCreditAccount(@NonNull CreateCreditAccountRequest request) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(request.getContextVariables());
        validatePeriodId(request);
        CreditAccount entity = CreditAccountConverter.INSTANCE.convertToCreditAccount(request);
        creditAccountMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建信用账户失败");
        ledgerService.initializeRequiredLedgers(new InitializeSubjectLedgerRequest()
                .setTenantId(entity.getTenantId())
                .setSubjectId(entity.getSn())
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(entity.getCurrency())
                .setLedgerProfileCode(entity.getLedgerProfileCode())
                .setLedgerProfileVersion(entity.getLedgerProfileVersion())
                .setPeriodType(entity.getPeriodType())
                .setPeriodId(request.getPeriodId()));
        return entity.getId();
    }

    private void validatePeriodId(CreateCreditAccountRequest request) {
        AccountBalancePeriodType periodType = request.getPeriodType() == null
                ? AccountBalancePeriodType.LIFETIME : request.getPeriodType();
        if (periodType != AccountBalancePeriodType.LIFETIME) {
            AssertUtils.hasText(request.getPeriodId(), "非生命周期账本周期 periodId 不能为空");
        }
    }

    @Override
    public @NonNull CreditAccountDTO getCreditAccountById(@NonNull Long id) {
        CreditAccount result = creditAccountMapper.selectOneById(id);
        AssertUtils.notNull(result, "信用账户不存在，id = {}", id);
        return CreditAccountConverter.INSTANCE.convertToCreditAccountDTO(result);
    }

    @Override
    public @NonNull CreditAccountDTO getCreditAccount(@NonNull FundsAccountId accountId) {
        CreditAccountNameRefs ref = CreditAccountNameRefs.creditAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(accountId.id()))
                .and(ref.accountType.eq(accountId.type()));
        CreditAccount result = creditAccountMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "信用账户不存在，accountId = {}", accountId);
        return CreditAccountConverter.INSTANCE.convertToCreditAccountDTO(result);
    }

    @Override
    public @NonNull CreditAccountDTO getCreditAccount(@NonNull Long tenantId, @NonNull String accountSn) {
        CreditAccountNameRefs ref = CreditAccountNameRefs.creditAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(accountSn));
        CreditAccount result = creditAccountMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "信用账户不存在，tenantId = {}, accountSn = {}", tenantId, accountSn);
        return CreditAccountConverter.INSTANCE.convertToCreditAccountDTO(result);
    }

    @Override
    public @NonNull WindPagination<CreditAccountDTO> queryCreditAccounts(
            @NonNull CreditAccountQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        CreditAccountNameRefs ref = CreditAccountNameRefs.creditAccount;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.ownerId.eq(query.getOwnerId()))
                .and(ref.ownerType.eq(query.getOwnerType()))
                .and(ref.accountType.eq(query.getAccountType()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.periodType.eq(query.getPeriodType()))
                .and(ref.periodId.eq(query.getPeriodId()))
                .and(ref.state.eq(query.getState()));
        return MybatisQueryHelper.<CreditAccount, CreditAccountDTO>query(wrapper)
                .counter(creditAccountMapper::selectCountByQuery)
                .resultQueryFunc(creditAccountMapper::selectListByQuery)
                .converter(CreditAccountConverter.INSTANCE::convertToCreditAccountDTO)
                .query(options);
    }
}
