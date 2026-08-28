package me.wishva.globalTradeLogistics.core.dto;

import jakarta.json.bind.annotation.JsonbTransient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.CustomsClearanceStatus;
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentSummary implements Serializable, Auditable {

    private Integer shipmentId;
    private String trackingNumber;
    private String vesselId;
    private String type;
    private Integer warehouseId;
    private ShipmentStatus status;
    private String shipmentType;
    private String ref;
    private Integer poId;
    /** Status of the most recent customs clearance record for this shipment, or {@code null} if none exists yet. */
    private CustomsClearanceStatus customsStatus;
    /** Declaration number of the most recent customs clearance record, or {@code null} if none exists yet. */
    private String declarationNumber;

    @Override
    @JsonbTransient
    public String getAuditReference() {
        return String.valueOf(shipmentId);
    }
}
