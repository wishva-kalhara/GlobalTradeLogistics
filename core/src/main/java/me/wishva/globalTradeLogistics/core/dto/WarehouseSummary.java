package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Wire shape for the warehouse `<select>` on the inventory console — {@code GET /v1/inventory/warehouses}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseSummary implements Serializable {

    private Integer warehouseId;
    private String country;
}
