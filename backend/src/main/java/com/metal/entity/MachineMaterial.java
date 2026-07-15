package com.metal.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MachineMaterial {
    private Long id;
    private Long companyId;
    private String yearMonth;
    private LocalDate recordDate;
    private String shift;
    private String factory;
    private String serialNumber;
    private String machineNo;
    private String repairPerson;
    private LocalDateTime repairRequestTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal repairHours;
    private BigDecimal downtimeHours;
    private String machineModel;
    private String faultPhenomenon;
    private String faultDescription;
    private String materialCode;
    private String partName;
    private Integer quantity;
    private String machineOnMaterial;
    private String machineOffMaterial;
    private String remark;
    private String confirmer;
    private String deliveryRecordRef;
    private LocalDate lastMachineOnTime;
    private String isOutOfWarranty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
