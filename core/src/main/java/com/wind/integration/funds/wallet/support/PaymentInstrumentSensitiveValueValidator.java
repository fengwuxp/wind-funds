package com.wind.integration.funds.wallet.support;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

/**
 * 支付工具敏感值校验器。
 *
 * <p>用于阻断银行卡号等原始敏感号进入资金 DSL 快照、支付工具存储或日志链路。
 * 允许使用脱敏展示号、token 或外部稳定引用。</p>
 */
public final class PaymentInstrumentSensitiveValueValidator {

    private static final int MIN_RAW_PAN_LENGTH = 12;

    private static final int MAX_RAW_PAN_LENGTH = 19;

    private PaymentInstrumentSensitiveValueValidator() {
        throw new AssertionError();
    }

    /**
     * 判断输入是否形似原始 PAN。
     *
     * @param instrumentNo 支付工具展示号、token 或原始号候选
     * @return true 表示输入为 12 到 19 位数字，可带空格或短横线分隔
     */
    public static boolean isRawSensitiveInstrumentNo(@Nullable String instrumentNo) {
        if (!StringUtils.hasText(instrumentNo)) {
            return false;
        }
        String compactInstrumentNo = instrumentNo.replace(" ", "").replace("-", "");
        if (compactInstrumentNo.length() < MIN_RAW_PAN_LENGTH
                || compactInstrumentNo.length() > MAX_RAW_PAN_LENGTH) {
            return false;
        }
        for (int i = 0; i < compactInstrumentNo.length(); i++) {
            if (!Character.isDigit(compactInstrumentNo.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
