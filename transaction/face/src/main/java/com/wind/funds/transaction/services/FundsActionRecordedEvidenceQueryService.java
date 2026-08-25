package com.wind.funds.transaction.services;

import com.wind.funds.transaction.model.dto.FundsActionFactRef;
import com.wind.funds.transaction.model.dto.FundsActionRecordedEvidenceDTO;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * 资金动作已记录引用证据查询服务。
 *
 * @author wuxp
 * @since 2026-08-23
 */
public interface FundsActionRecordedEvidenceQueryService {

    /**
     * 查询 direct PAY principal 动作的完整已记录 sibling 引用。
     *
     * @param actionFactRef 资金动作事实引用
     * @return 完整且一致时返回已记录引用证据，否则返回 empty
     */
    @NonNull
    Optional<FundsActionRecordedEvidenceDTO> findRecordedEvidence(@NonNull FundsActionFactRef actionFactRef);
}
