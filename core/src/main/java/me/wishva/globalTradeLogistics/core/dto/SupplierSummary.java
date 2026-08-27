package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Wire shape for the supplier `<select>` on the create-purchase-order page — {@code GET /v1/admin/suppliers}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupplierSummary implements Serializable {

    private Integer supplierId;
    private String fullName;
    private String email;
}
