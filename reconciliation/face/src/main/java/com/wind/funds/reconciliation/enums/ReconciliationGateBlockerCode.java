package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 一个门禁要求或对账对的有限失败关闭原因。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Schema(description = "对账门禁阻断原因代码")
@Getter
@AllArgsConstructor
public enum ReconciliationGateBlockerCode implements DescriptiveEnum {

    REQUIREMENT_NOT_FOUND("Requirement not found"),
    REQUIREMENT_HEAD_CONFLICT("Requirement head conflict"),
    REQUIRED_PAIR_RUN_NOT_FOUND("Required pair run not found"),
    RUN_NOT_CURRENT("Run is not current"),
    RUN_NOT_COMPLETED("Run is not completed"),
    RUN_NOT_BALANCED("Run is not balanced"),
    RULE_MISMATCH("Rule mismatch"),
    COVERAGE_INCOMPLETE("Coverage incomplete"),
    BLOCKING_DIFFERENCE_PRESENT("Blocking difference present");

    private final String desc;
}
