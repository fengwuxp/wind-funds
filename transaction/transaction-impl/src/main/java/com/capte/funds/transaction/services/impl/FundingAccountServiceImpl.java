package com.capte.funds.transaction.services.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.transaction.dal.entities.FundingAccount;
import com.capte.funds.transaction.dal.entities.table.FundingAccountNameRefs;
import com.capte.funds.transaction.dal.mapper.FundingAccountMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.mapstruct.FundingAccountConverter;
import com.capte.funds.transaction.model.dto.FundingAccountDTO;
import com.capte.funds.transaction.model.query.FundingAccountQuery;
import com.capte.funds.transaction.model.request.CreateFundingAccountRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.transaction.services.FundingAccountService;
import com.capte.funds.wallet.service.SubjectLedgerInitializer;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
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
                        .setCurrency(CurrencyIsoCode.valueOf(currency)),
                DefaultPageQueryOptions.defaults(50)).getRecords().stream()
                .collect(Collectors.toMap(LedgerDTO::getLedgerSubjectCode, LedgerDTO::getId));
    }
}
