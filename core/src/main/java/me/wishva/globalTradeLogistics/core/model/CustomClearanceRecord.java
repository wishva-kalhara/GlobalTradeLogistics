package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.CustomsClearanceStatus;

/**
 * Maps the existing {@code custom_clearence_records} table
 * (schema.postgres.sql, typo and all — table name kept as-is).
 */
@Entity
@Table(name = "custom_clearence_records")
@NamedQueries({
        @NamedQuery(
                name = "CustomClearanceRecord.findByShipment",
                query = "SELECT c FROM CustomClearanceRecord c WHERE c.supplierShipmentsShipmentId = :shipmentId"),
        @NamedQuery(
                name = "CustomClearanceRecord.findLatestByShipment",
                query = "SELECT c FROM CustomClearanceRecord c WHERE c.supplierShipmentsShipmentId = :shipmentId ORDER BY c.recordId DESC"),
        @NamedQuery(
                name = "CustomClearanceRecord.findPending",
                query = "SELECT c FROM CustomClearanceRecord c WHERE c.status = me.wishva.globalTradeLogistics.core.enums.CustomsClearanceStatus.PENDING")
})
@Getter
@Setter
@NoArgsConstructor
public class CustomClearanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId;

    @Column(name = "declaration_number")
    private String declarationNumber;

    @Column(name = "supplier_shipments_shipment_id", nullable = false)
    private Integer supplierShipmentsShipmentId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CustomsClearanceStatus status = CustomsClearanceStatus.PENDING;
}
