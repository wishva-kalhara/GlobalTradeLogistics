package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Wire shape for pre-filling {@code me/update-profile.jsp} — {@code GET /v1/me/customer}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileSummary implements Serializable {

    private String email;
    private String fullName;
    private String mobile1;
    private String mobile2;
    private String address;
    private String country;
}
