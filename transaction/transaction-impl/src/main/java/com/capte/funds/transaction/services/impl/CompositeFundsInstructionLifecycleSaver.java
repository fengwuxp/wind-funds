package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;

import java.util.List;

/**
 * 组合资金指令生命周期保存器。
 *
 * @deprecated since 1.0.1, use {@link DelegatingFundsInstructionLifecycleRecorder}. The old name is retained only
 * as a source-compatible alias during naming-governance migration.
 */
@Deprecated(since = "1.0.1", forRemoval = false)
public class CompositeFundsInstructionLifecycleSaver extends DelegatingFundsInstructionLifecycleRecorder
        implements FundsInstructionLifecycleSaver {

    public CompositeFundsInstructionLifecycleSaver(List<FundsInstructionLifecycleRecorder> delegates) {
        super(delegates);
    }
}
