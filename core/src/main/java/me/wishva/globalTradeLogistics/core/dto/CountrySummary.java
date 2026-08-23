package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Wire shape for the country `<select>` dropdowns — {@code GET /v1/countries}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountrySummary implements Serializable {

    private String code;
    private String name;
}
