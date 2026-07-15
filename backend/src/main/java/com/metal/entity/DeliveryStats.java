package com.metal.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeliveryStats {
    private Long id;
    private Long companyId;
    private String category;
    private String materialCode;
    private String systemName;
    private String partName;
    private BigDecimal unitUsage;
    private BigDecimal ratio;
    private BigDecimal unitPriceWithTax;
    private Integer machineCount;
    private Integer deliveryQuantity;
    private Integer machineOnQuantity;
    private Integer monthRepair;
    private BigDecimal agreedRatioQuantity;
    private BigDecimal excessQuantity;
    private BigDecimal excessAmountWithTax;
    private LocalDate statDate;
    private String yearMonth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
