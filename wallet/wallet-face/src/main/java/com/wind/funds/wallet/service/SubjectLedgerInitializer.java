package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * 账务主体账本初始化器。
 *
 * <p>职责：在主体创建阶段，按 LedgerProfile 显式创建 required ledger。</p>
 *
 * <p>边界：只服务于主体初始化流程；交易执行路径不得调用该接口自动建账。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface SubjectLedgerInitializer {

    /**
     * 按 LedgerProfile 初始化 required ledger，交易路径不得自动建账。
     *
     * <p>能力范围：根据主体、币种和 profile 创建必需账本，返回科目到 ledger id 的映射。
     * 已存在账本时应遵循唯一约束和幂等创建策略。</p>
     *
     * @param request 初始化请求
     * @return 科目到 ledger id 的映射
     */
    @NonNull Map<LedgerSubjectCode, Long> initializeRequiredLedgers(@NonNull InitializeSubjectLedgerRequest request);
}
