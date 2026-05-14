package com.capte.funds.transaction.services;

import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.capte.funds.transaction.model.dto.LedgerProfileDTO;
import com.capte.funds.transaction.model.dto.LedgerProfileItemDTO;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import org.jspecify.annotations.NonNull;

/**
 * LedgerProfile 查询服务。
 *
 * <p>职责：提供主体账本初始化所需的 profile 和科目定义。</p>
 *
 * <p>边界：当前阶段 profile 由代码枚举定义，不负责运行时修改、不负责账本余额计算。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface LedgerProfileService {

    /**
     * 根据 profile 编码获取 profile。
     *
     * <p>能力范围：返回指定 profile 的完整科目配置，用于主体开户或校验。</p>
     *
     * @param profileCode profile 编码
     * @return profile 定义
     */
    @NonNull LedgerProfileDTO getProfile(@NonNull LedgerProfileCode profileCode);

    /**
     * 获取 profile 下指定科目明细。
     *
     * <p>能力范围：查询必需科目的 profile item；不存在时应失败，避免交易路径隐式降级。</p>
     *
     * @param profileCode profile 编码
     * @param subjectCode 科目编码
     * @return profile item
     */
    @NonNull
    LedgerProfileItemDTO getRequiredItem(@NonNull LedgerProfileCode profileCode,
                                         @NonNull LedgerSubjectCode subjectCode);
}
