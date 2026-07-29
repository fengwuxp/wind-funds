package com.wind.funds.reconciliation.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 创建清分批次请求。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateClearingSplitBatchRequest implements Serializable {

    public static final int MAX_SPLITTABLE_DETAIL_COUNT = 1000;

    @Serial
    private static final long serialVersionUID = 1318165169705725464L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "本批次承接的可清分明细流水号；主体、币种和规则等事实由明细派生")
    @NotEmpty
    @Size(max = MAX_SPLITTABLE_DETAIL_COUNT, message = "单个清分批次明细数量不能超过 1000")
    private List<String> splittableDetailSns;
}
