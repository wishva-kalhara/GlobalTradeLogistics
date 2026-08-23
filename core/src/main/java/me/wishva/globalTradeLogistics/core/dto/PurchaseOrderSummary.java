package me.wishva.globalTradeLogistics.core.dto;

import jakarta.json.bind.annotation.JsonbTransient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderSummary implements Serializable, Auditable {

    private Integer poId;
    private Integer supplierId;
    private Integer productId;
    private String productName;
    private Integer requestingQty;
    private Double totalPrice;
    private boolean completed;
    private Instant createdAt;

    @Override
    @JsonbTransient
    public String getAuditReference() {
        return String.valueOf(poId);
    }
}
