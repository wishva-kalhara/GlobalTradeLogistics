package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Wire shape for the staff dashboard's sales chart — {@code GET /admin/sales-summary}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummary implements Serializable {

    private Double totalSales;
    private Integer totalOrders;
    private Map<String, Integer> ordersByStatus;
    private List<ProductSalesSummary> topProducts;
}
