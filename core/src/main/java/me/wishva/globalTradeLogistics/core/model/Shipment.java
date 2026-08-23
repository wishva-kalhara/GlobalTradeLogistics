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
import me.wishva.globalTradeLogistics.core.enums.ShipmentStatus;

/**
 * Maps the existing {@code shipments} table (schema.postgres.sql).
 * {@code vessal_id} keeps its legacy column name (typo and all) — only the
 * Java field name is corrected to {@code vesselId}.
 */
@Entity
@Table(name = "shipments")
@NamedQueries({
        @NamedQuery(
                name = "Shipment.findByStatus",
                query = "SELECT s FROM Shipment s WHERE s.status = :status")
})
@Getter
@Setter
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Integer shipmentId;

    @Column(name = "tracking_number", nullable = false)
    private String trackingNumber;

    @Column(name = "vessal_id", nullable = false)
    private String vesselId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "wearhouses_wearhous_id", nullable = false)
    private Integer warehousesWarehouseId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "shipment_type")
    private String shipmentType;

    @Column(name = "ref")
    private String ref;
}
