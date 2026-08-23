package me.wishva.globalTradeLogistics.core.dto;

import jakarta.json.bind.annotation.JsonbTransient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Result of {@code IVendorPerformanceService.recomputeForSupplier} — feeds
 * {@code AuditInterceptor} via {@link Auditable} so the weekly recompute's
 * {@code @Audited} entry carries the supplier id and a human-readable score
 * summary, without the interceptor needing to know anything about vendor
 * performance specifically.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorPerformanceResult implements Serializable, Auditable {

    private Integer supplierId;
    private int posEvaluated;
    private int onTimeCount;
    private double onTimeRatePercent;
    private String summary;

    @Override
    @JsonbTransient
    public String getAuditReference() {
        return String.valueOf(supplierId);
    }

    @Override
    @JsonbTransient
    public String getAuditDetails() {
        return summary;
    }
}
