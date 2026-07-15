package com.metal.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DeliveryStatsDaily {
    private Long id;
    private Long statId;       // FK → delivery_stats.id
    private Integer dayNumber; // 1-31
    private BigDecimal value;
}
