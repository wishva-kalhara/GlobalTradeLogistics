package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreatePurchaseOrderBody {

    private Integer supplierId;
    private Integer productId;
    private Integer qty;
}
