package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Self-service "Create Account" — deliberately minimal (email + country only). */
@Getter
@Setter
@NoArgsConstructor
public class SignUpCustomerBody {

    private String email;
    private String country;
}
