package com.wind.funds.wallet.services.impl;

import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.mapstruct.FundingAccountConverter;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.query.FundingAccountQuery;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.mybatis.flex.MybatisQueryHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 真实资金账户服务实现。
 *
 */
@Service
@AllArgsConstructor
@Slf4j
public class FundingAccountServiceImpl implements FundingAccountService {

    private static final int MAX_SN_LENGTH = 64;

    private static final int MAX_OWNER_ID_LENGTH = 30;

    private static final int MAX_ACCOUNT_TYPE_LENGTH = 50;

    private static final int MAX_DESCRIPTION_LENGTH = 512;

    private final FundingAccountMapper fundingAccountMapper;

    private final LedgerService ledgerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createFundingAccount(@NonNull CreateFundingAccountRequest request) {
        validatePersistentFieldLengths(request);
        WalletContextVariablesValidator.assertNoSensitiveContextVariables(request.getContextVariables());
        validatePlatformRole(request);
        FundingAccount entity = FundingAccountConverter.INSTANCE.convertToFundingAccount(request);
        fundingAccountMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金账户失败");
        ledgerService.initializeRequiredLedgers(new InitializeSubjectLedgerRequest()
                .setTenantId(entity.getTenantId())
                .setSubjectId(entity.getSn())
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(entity.getCurrency())
                .setLedgerProfileCode(entity.getLedgerProfileCode())
                .setLedgerProfileVersion(entity.getLedgerProfileVersion()));
        log.info("资金账户创建完成，等待事务提交，tenantId = {}, accountSn = {}, accountType = {}, state = {}, "
                        + "currency = {}, ledgerProfileCode = {}, ledgerProfileVersion = {}",
                entity.getTenantId(), entity.getSn(), entity.getAccountType(), entity.getState(), entity.getCurrency(),
                entity.getLedgerProfileCode(), entity.getLedgerProfileVersion());
        return entity.getId();
    }

    @Override
    public @NonNull FundingAccountDTO getFundingAccount(@NonNull Long tenantId, @NonNull String accountSn) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.sn.eq(accountSn));
        FundingAccount result = fundingAccountMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "资金账户不存在，tenantId = {}, accountSn = {}", tenantId, accountSn);
        return FundingAccountConverter.INSTANCE.convertToFundingAccountDTO(result);
    }

    @Override
    public @NonNull WindPagination<FundingAccountDTO> queryFundingAccounts(
            @NonNull FundingAccountQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.ownerId.eq(query.getOwnerId()))
                .and(ref.ownerType.eq(query.getOwnerType()))
                .and(ref.accountType.eq(query.getAccountType()))
                .and(ref.platform.eq(query.getPlatform()))
                .and(ref.accountRoleCode.eq(query.getAccountRoleCode()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.state.eq(query.getState()));
        return MybatisQueryHelper.<FundingAccount, FundingAccountDTO>query(wrapper)
                .counter(fundingAccountMapper::selectCountByQuery)
                .resultQueryFunc(fundingAccountMapper::selectListByQuery)
                .converter(FundingAccountConverter.INSTANCE::convertToFundingAccountDTO)
                .query(options);
    }

    private void validatePlatformRole(CreateFundingAccountRequest request) {
        if (Boolean.TRUE.equals(request.getPlatform())) {
            AssertUtils.notNull(request.getAccountRoleCode(), "平台资金账户必须指定平台账户角色");
            return;
        }
        AssertUtils.isNull(request.getAccountRoleCode(), "非平台资金账户不得指定平台账户角色");
    }

    private static void validatePersistentFieldLengths(CreateFundingAccountRequest request) {
        assertMaxLength(request.getSn(), MAX_SN_LENGTH, "sn");
        assertMaxLength(request.getOwnerId(), MAX_OWNER_ID_LENGTH, "ownerId");
        assertMaxLength(request.getAccountType(), MAX_ACCOUNT_TYPE_LENGTH, "accountType");
        assertMaxLength(request.getDescription(), MAX_DESCRIPTION_LENGTH, "description");
    }

    private static void assertMaxLength(@Nullable String value, int maxLength, String fieldName) {
        AssertUtils.isTrue(value == null || value.length() <= maxLength,
                "{} 长度不能超过 {}", fieldName, maxLength);
    }
}
