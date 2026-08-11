package com.wind.funds.wallet.services.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.dal.entities.SpendSubjectFundingRel;
import com.wind.funds.wallet.dal.entities.table.SpendSubjectFundingRelNameRefs;
import com.wind.funds.wallet.dal.mapper.SpendSubjectFundingRelMapper;
import com.wind.funds.wallet.mapstruct.SpendSubjectFundingRelationConverter;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.wind.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.mybatis.flex.MybatisQueryHelper;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支出主体资金关系服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class SpendSubjectFundingRelationServiceImpl implements SpendSubjectFundingRelationService {

    private static final WindSequenceType SPEND_SUBJECT_FUNDING_RELATION_SEQUENCE_TYPE =
            WindSequenceType.immutable("SPEND_SUBJECT_FUNDING_RELATION", "SSFR", 6);

    private final SpendSubjectFundingRelMapper spendSubjectFundingRelMapper;

    private final FundingAccountService fundingAccountService;

    private final CreditAccountService creditAccountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull Long createSpendSubjectFundingRelation(
            @NonNull CreateSpendSubjectFundingRelationRequest request) {
        resolveAndValidateTargetSubject(request);
        SpendSubjectFundingRel entity =
                SpendSubjectFundingRelationConverter.INSTANCE.convertToSpendSubjectFundingRel(request);
        entity.setSn(TemporalSequenceFactory.hourNext(SPEND_SUBJECT_FUNDING_RELATION_SEQUENCE_TYPE));
        spendSubjectFundingRelMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建支出主体资金关系失败");
        return entity.getId();
    }

    @Override
    public @NonNull SpendSubjectFundingRelationDTO getSpendSubjectFundingRelationById(@NonNull Long id) {
        SpendSubjectFundingRel result = spendSubjectFundingRelMapper.selectOneById(id);
        AssertUtils.notNull(result, "支出主体资金关系不存在，id = {}", id);
        return toDTO(result);
    }

    @Override
    public @NonNull WindPagination<SpendSubjectFundingRelationDTO> querySpendSubjectFundingRelations(
            @NonNull SpendSubjectFundingRelationQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options) {
        SpendSubjectFundingRelNameRefs ref = SpendSubjectFundingRelNameRefs.spendSubjectFundingRel;
        QueryWrapper wrapper = MybatisQueryHelper.from(options).select()
                .from(ref)
                .where(ref.sn.eq(query.getSn()))
                .and(ref.tenantId.eq(query.getTenantId()))
                .and(ref.spendSubjectId.eq(query.getSpendSubjectId()))
                .and(ref.spendSubjectType.eq(query.getSpendSubjectType()))
                .and(ref.targetSubjectType.eq(query.getTargetSubjectType()))
                .and(ref.targetSubjectId.eq(query.getTargetSubjectId()))
                .and(ref.currency.eq(query.getCurrency()))
                .and(ref.relationType.eq(query.getRelationType()));
        wrapper.orderBy(ref.id.asc());
        return MybatisQueryHelper.<SpendSubjectFundingRel, SpendSubjectFundingRelationDTO>query(wrapper)
                .counter(spendSubjectFundingRelMapper::selectCountByQuery)
                .resultQueryFunc(spendSubjectFundingRelMapper::selectListByQuery)
                .converter(this::toDTO)
                .query(options);
    }

    private SpendSubjectFundingRelationDTO toDTO(SpendSubjectFundingRel entity) {
        return SpendSubjectFundingRelationConverter.INSTANCE.convertToSpendSubjectFundingRelationDTO(entity);
    }

    private void resolveAndValidateTargetSubject(CreateSpendSubjectFundingRelationRequest request) {
        AssertUtils.notNull(request.getTargetSubjectType(), "资金责任目标主体类型不能为空");
        AssertUtils.hasText(request.getTargetSubjectId(), "资金责任目标主体 ID 不能为空");
        AssertUtils.isTrue(request.getTargetSubjectType() == FundsSubjectType.FUNDING_ACCOUNT
                        || request.getTargetSubjectType() == FundsSubjectType.CREDIT_ACCOUNT,
                "资金责任目标主体类型不支持，targetSubjectType = {}", request.getTargetSubjectType());
        if (request.getTargetSubjectType() == FundsSubjectType.FUNDING_ACCOUNT) {
            resolveAndValidateFundingAccountTarget(request);
            return;
        }
        resolveAndValidateCreditAccountTarget(request);
    }

    private void resolveAndValidateFundingAccountTarget(CreateSpendSubjectFundingRelationRequest request) {
        FundingAccountDTO fundingAccount =
                fundingAccountService.getFundingAccount(request.getTenantId(), request.getTargetSubjectId());
        assertFundingAccountCanBind(fundingAccount, request);
    }

    private void resolveAndValidateCreditAccountTarget(CreateSpendSubjectFundingRelationRequest request) {
        CreditAccountDTO creditAccount =
                creditAccountService.getCreditAccount(request.getTenantId(), request.getTargetSubjectId());
        AssertUtils.isTrue(creditAccount.getState().canDebit(),
                "资金责任目标主体不可用，targetSubjectType = {}, targetSubjectId = {}",
                request.getTargetSubjectType(), request.getTargetSubjectId());
        AssertUtils.equals(creditAccount.getCurrency(), request.getCurrency(),
                "资金责任目标主体币种与资金责任解析关系币种不一致，targetSubjectType = {}, targetSubjectId = {}",
                request.getTargetSubjectType(), request.getTargetSubjectId());
    }

    private void assertFundingAccountCanBind(FundingAccountDTO fundingAccount,
                                             CreateSpendSubjectFundingRelationRequest request) {
        AssertUtils.isTrue(fundingAccount.getState().canDebit(),
                "资金账户不可作为资金责任目标主体，targetSubjectId = {}", request.getTargetSubjectId());
        AssertUtils.equals(fundingAccount.getCurrency(), request.getCurrency(),
                "资金账户币种与资金责任解析关系币种不一致，targetSubjectId = {}", request.getTargetSubjectId());
    }
}
