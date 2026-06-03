package com.wind.funds.route.spec;

import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 交易级参与方快照。
 *
 * <p>职责：
 * <ul>
 *   <li>记录一次资金路径里参与主体的身份</li>
 *   <li>承载主体引用、角色、金额和路由期间的附加上下文</li>
 *   <li>为后续账务计划和审计提供主体维度信息</li>
 * </ul>
 *
 * <p>边界：
 * <ul>
 *   <li>不代表主体实体本身</li>
 *   <li>不负责主体开户、初始化或余额维护</li>
 * </ul>
 */
public interface RouteParticipantSpec {

    @NonNull
    RouteParticipantRole getParticipantRole();

    @NonNull
    SubjectRef getSubjectRef();

    @Nullable
    default String getLedgerProfileCode() {
        return null;
    }

    @Nullable
    default String getCurrency() {
        return null;
    }

    @Nullable
    default Money getAmount() {
        return null;
    }

    @Nullable
    default String getDescription() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}
