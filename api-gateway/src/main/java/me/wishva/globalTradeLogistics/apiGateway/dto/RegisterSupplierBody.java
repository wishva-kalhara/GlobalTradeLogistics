package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterSupplierBody {

    private String email;
    private String fullName;
    private String mobile1;
    private String address;
    private String country;
}
