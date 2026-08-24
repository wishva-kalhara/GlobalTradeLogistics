package me.wishva.globalTradeLogistics.core.dto;

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
public class InventorySummary implements Serializable {

    private Integer inventoryId;
    private Integer warehouseId;
    private Integer productId;
    private String productName;
    private Integer qty;
    private Integer reorderLevel;
    private Double unitPrice;
    private Instant lastUpdatedAt;
}
