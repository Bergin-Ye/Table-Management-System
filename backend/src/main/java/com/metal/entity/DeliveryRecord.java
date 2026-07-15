package com.metal.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeliveryRecord {
    private Long id;
    private Long companyId;
    private LocalDate recordDate;
    private String category;
    private String materialName;
    private String specModel;
    private String materialCode;
    private String materialSerial;
    private Integer quantity;
    private String unit;
    private String brand;
    private String productAttr;
    private String factory;
    private String shipmentNo;
    private String yearMonth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
