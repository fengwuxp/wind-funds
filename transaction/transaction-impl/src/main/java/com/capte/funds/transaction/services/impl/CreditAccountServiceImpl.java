package com.capte.funds.transaction.services.impl;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.transaction.dal.entities.CreditAccount;
import com.capte.funds.transaction.dal.entities.table.CreditAccountNameRefs;
import com.capte.funds.transaction.dal.mapper.CreditAccountMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.mapstruct.CreditAccountConverter;
import com.capte.funds.wallet.model.dto.CreditAccountDTO;
import com.capte.funds.wallet.model.query.CreditAccountQuery;
import com.capte.funds.wallet.model.request.CreateCreditAccountRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.wallet.service.CreditAccountService;
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
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

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

    private final SubjectLedgerInitializer subjectLedgerInitializer;

    private final LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createCreditAccount(@NonNull CreateCreditAccountRequest request) {
        CreditAccount entity = CreditAccountConverter.INSTANCE.convertToCreditAccount(request);
        creditAccountMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建信用账户失败");
        subjectLedgerInitializer.initializeRequiredLedgers(new InitializeSubjectLedgerRequest()
                .setTenantId(entity.getTenantId())
                .setSubjectId(entity.getSn())
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(entity.getCurrency())
                .setLedgerProfileCode(entity.getLedgerProfileCode())
                .setPeriodType(entity.getPeriodType()));
        return entity.getId();
    }

    @Override
    public @NonNull CreditAccountDTO getCreditAccountById(@NonNull Long id) {
        CreditAccount result = creditAccountMapper.selectOneById(id);
        AssertUtils.notNull(result, "信用账户不存在，id = {}", id);
        return toDTO(result);
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
        return toDTO(result);
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
                .and(ref.status.eq(query.getStatus()));
        return MybatisQueryHelper.<CreditAccount, CreditAccountDTO>query(wrapper)
                .counter(creditAccountMapper::selectCountByQuery)
                .resultQueryFunc(creditAccountMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private CreditAccountDTO toDTO(CreditAccount entity) {
        CreditAccountDTO result = CreditAccountConverter.INSTANCE.convertToCreditAccountDTO(entity);
        return result.setLedgerIds(loadLedgerIds(entity));
    }

    private Map<LedgerSubjectCode, Long> loadLedgerIds(CreditAccount entity) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(entity.getTenantId())
                        .setSubjectId(entity.getSn())
                        .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT.name())
                        .setCurrency(entity.getCurrency()),
                DefaultPageQueryOptions.defaults(50)).getRecords().stream()
                .collect(Collectors.toMap(LedgerDTO::getLedgerSubjectCode, LedgerDTO::getId));
    }
}
