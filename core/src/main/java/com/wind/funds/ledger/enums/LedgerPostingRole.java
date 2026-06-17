package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账目分录在多级账户中的记账角色。
 */
@AllArgsConstructor
@Getter
public enum LedgerPostingRole implements DescriptiveEnum {

    DETAIL("明细入账分录"),

    PARENT_CONTROL("父级控制分录"),

    TRANSFER_OUT("父子账户划拨转出分录"),

    TRANSFER_IN("父子账户划拨转入分录"),

    AGGREGATE_VIEW("只读汇总视图");

    private final String desc;
}
