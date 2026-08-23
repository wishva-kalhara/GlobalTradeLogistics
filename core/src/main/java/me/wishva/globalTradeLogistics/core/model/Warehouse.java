package me.wishva.globalTradeLogistics.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the existing {@code wearhouses} table (schema.postgres.sql). PK is a
 * plain {@code INT}, not {@code SERIAL} — ids are assigned by whoever
 * inserts the row (here: the demo catalog seed), not the database.
 */
@Entity
@Table(name = "wearhouses")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse {

    @Id
    @Column(name = "wearhous_id")
    private Integer warehouseId;

    @Column(name = "country", nullable = false)
    private String country;
}
