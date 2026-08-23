package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCustomerProfileBody {

    private String mobile1;
    private String mobile2;
    private String address;
    private String country;
    private String regionKey;
}
