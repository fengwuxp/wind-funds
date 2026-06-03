package com.wind.funds.wallet.services.impl;

import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.mapstruct.FundingAccountConverter;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.query.FundingAccountQuery;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.SubjectLedgerInitializer;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 真实资金账户服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class FundingAccountServiceImpl implements FundingAccountService {

    private final FundingAccountMapper fundingAccountMapper;

    private final SubjectLedgerInitializer subjectLedgerInitializer;

    private final LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createFundingAccount(@NonNull CreateFundingAccountRequest request) {
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(request.getContextVariables());
        validatePlatformRole(request);
        FundingAccount entity = FundingAccountConverter.INSTANCE.convertToFundingAccount(request);
        fundingAccountMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金账户失败");
        subjectLedgerInitializer.initializeRequiredLedgers(new InitializeSubjectLedgerRequest()
                .setTenantId(entity.getTenantId())
                .setSubjectId(entity.getSn())
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(entity.getCurrency())
                .setLedgerProfileCode(entity.getLedgerProfileCode()));
        return entity.getId();
    }

    @Override
    public @NonNull FundingAccountDTO getFundingAccountById(@NonNull Long id) {
        FundingAccount result = fundingAccountMapper.selectOneById(id);
        AssertUtils.notNull(result, "资金账户不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull FundingAccountDTO getFundingAccount(@NonNull FundsAccountId accountId) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.sn.eq(accountId.id()))
                .and(ref.accountType.eq(accountId.type()));
        FundingAccount result = fundingAccountMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "资金账户不存在，accountId = {}", accountId);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<FundingAccountDTO> queryFundingAccounts(
            @NonNull FundingAccountQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.ownerId.eq(query.getOwnerId()))
                .and(ref.ownerType.eq(query.getOwnerType()))
                .and(ref.accountType.eq(query.getAccountType()))
                .and(ref.accountRoleCode.eq(query.getAccountRoleCode()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.status.eq(query.getStatus()));
        return MybatisQueryHelper.<FundingAccount, FundingAccountDTO>query(wrapper)
                .counter(fundingAccountMapper::selectCountByQuery)
                .resultQueryFunc(fundingAccountMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private FundingAccountDTO toDTO(FundingAccount entity) {
        FundingAccountDTO result = FundingAccountConverter.INSTANCE.convertToFundingAccountDTO(entity);
        return result.setLedgerIds(loadLedgerIds(entity.getTenantId(), entity.getSn(), entity.getCurrency().name()));
    }

    private Map<LedgerSubjectCode, Long> loadLedgerIds(Long tenantId, String subjectId, String currency) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(tenantId)
                        .setSubjectId(subjectId)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                        .setCurrency(CurrencyIsoCode.valueOf(currency))
                        .setPeriodType(AccountBalancePeriodType.LIFETIME)
                        .setPeriodId(AccountBalancePeriodType.LIFETIME.name()),
                DefaultPageQueryOptions.defaults(50)).getRecords().stream()
                .collect(Collectors.toMap(LedgerDTO::getLedgerSubjectCode, LedgerDTO::getId));
    }

    private void validatePlatformRole(CreateFundingAccountRequest request) {
        if (Boolean.TRUE.equals(request.getPlatform())) {
            AssertUtils.notNull(request.getAccountRoleCode(), "平台资金账户必须指定平台账户角色");
            return;
        }
        AssertUtils.isNull(request.getAccountRoleCode(), "非平台资金账户不得指定平台账户角色");
    }
}
