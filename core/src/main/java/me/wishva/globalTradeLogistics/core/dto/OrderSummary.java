package me.wishva.globalTradeLogistics.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.wishva.globalTradeLogistics.core.enums.OrderStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummary implements Serializable {

    private Integer orderId;
    private Instant orderedAt;
    private Double totalPrice;
    private OrderStatus status;
    private List<OrderLineSummary> items;
}
