package com.capte.funds.wallet.service;

import com.capte.funds.wallet.model.dto.SpendSubjectFundingRelationDTO;
import com.capte.funds.wallet.model.query.SpendSubjectFundingRelationQuery;
import com.capte.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;

/**
 * 支出主体资金关系服务。
 *
 * <p>职责：维护信用账户、预算组、支付工具等支出主体到真实 FundingAccount 的资金来源关系。</p>
 *
 * <p>边界：只维护关系，不直接执行扣款、不计算 Spend Rules、不生成账本分录。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface SpendSubjectFundingRelationService {

    /**
     * 创建支出主体和真实资金账户关系。
     *
     * <p>能力范围：建立支出主体到 FundingAccount 的资金归属或扣款关系，供 RouteResolver 使用。</p>
     *
     * @param request 创建请求
     * @return 关系主键
     */
    @NonNull Long createSpendSubjectFundingRelation(@NonNull CreateSpendSubjectFundingRelationRequest request);

    /**
     * 根据主键查询支出主体资金关系。
     *
     * <p>能力范围：只读查询单条关系，不校验余额和额度。</p>
     *
     * @param id 主键
     * @return 支出主体资金关系
     */
    @NonNull SpendSubjectFundingRelationDTO getSpendSubjectFundingRelationById(@NonNull Long id);

    /**
     * 分页查询支出主体资金关系。
     *
     * <p>能力范围：只读分页查询关系配置，供管理后台和路由排查使用。</p>
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 支出主体资金关系分页结果
     */
    @NonNull WindPagination<SpendSubjectFundingRelationDTO> querySpendSubjectFundingRelations(
            @NonNull SpendSubjectFundingRelationQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);
}
