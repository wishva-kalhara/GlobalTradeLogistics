package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Self-service "Create Account" — deliberately minimal (email + country only). */
@Getter
@Setter
@NoArgsConstructor
public class SignUpSupplierBody {

    private String email;
    private String country;
}
