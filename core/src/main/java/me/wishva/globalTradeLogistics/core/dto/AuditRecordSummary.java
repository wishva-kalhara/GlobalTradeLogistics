package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecordSummary implements Serializable {

    private Integer id;
    private Instant createdAt;
    private String resource;
    private String action;
    private String reference;
    private String details;
}
