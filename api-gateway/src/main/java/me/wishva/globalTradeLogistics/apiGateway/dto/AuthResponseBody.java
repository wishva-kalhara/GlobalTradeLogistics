package me.wishva.globalTradeLogistics.apiGateway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseBody {

    private String token;
    private String email;
    private String role;
}
