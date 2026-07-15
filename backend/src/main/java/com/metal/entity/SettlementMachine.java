package com.metal.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettlementMachine {
    private Long id;
    private Long companyId;
    private String materialCode;
    private String category;
    private String partName;
    private BigDecimal unitUsage;
    private BigDecimal ratio;
    private BigDecimal unitPriceWithTax;
    private String warrantyPeriod;
    private String priceType;
    private String remark;
    private String machineModel;
    private Integer settlementMachineCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
