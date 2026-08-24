package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesSummary implements Serializable {

    private Integer productId;
    private String productName;
    private Integer qtySold;
    private Double revenue;
}
