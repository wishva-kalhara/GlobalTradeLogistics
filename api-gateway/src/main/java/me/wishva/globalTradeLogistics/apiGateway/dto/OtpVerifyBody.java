package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OtpVerifyBody {

    private String email;
    private String code;
}
