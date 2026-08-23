package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddProductOfferingBody {

    private Integer productId;
    private Integer warehouseId;
    private Integer leadTimeInDays;
}
