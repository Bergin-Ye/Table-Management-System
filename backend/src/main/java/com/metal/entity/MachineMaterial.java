package com.metal.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MachineMaterial {
    @ExcelIgnore
    private Long id;
    @ExcelIgnore
    private Long companyId;
    @ExcelProperty(value = "年+月", index = 0)
    private String yearMonth;
    @ExcelProperty(value = "日期", index = 1)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate recordDate;
    @ExcelProperty(value = "班次", index = 2)
    private String shift;
    @ExcelProperty(value = "厂房", index = 3)
    private String factory;
    @ExcelProperty(value = "序号", index = 4)
    private String serialNumber;
    @ExcelProperty(value = "机台号", index = 5)
    private String machineNo;
    @ExcelProperty(value = "维修人", index = 6)
    private String repairPerson;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @ExcelProperty(value = "报修时间", index = 7)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairRequestTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @ExcelProperty(value = "开始时间", index = 8)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    @ExcelProperty(value = "结束时间", index = 9)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    @ExcelProperty(value = "维修工时", index = 10)
    private BigDecimal repairHours;
    @ExcelProperty(value = "停机工时", index = 11)
    private BigDecimal downtimeHours;
    @ExcelProperty(value = "机型", index = 12)
    private String machineModel;
    @ExcelProperty(value = "故障现象", index = 13)
    private String faultPhenomenon;
    @ExcelProperty(value = "维修描述", index = 14)
    private String faultDescription;
    @ExcelProperty(value = "物料编码", index = 15)
    private String materialCode;
    @ExcelProperty(value = "零件名称", index = 16)
    private String partName;
    @ExcelProperty(value = "数量", index = 17)
    private Integer quantity;
    @ExcelProperty(value = "上机物料", index = 18)
    private String machineOnMaterial;
    @ExcelProperty(value = "下机物料", index = 19)
    private String machineOffMaterial;
    @ExcelProperty(value = "备注", index = 20)
    private String remark;
    @ExcelProperty(value = "确认人", index = 21)
    private String confirmer;
    @ExcelProperty(value = "送货记录引用", index = 22)
    private String deliveryRecordRef;
    @ExcelProperty(value = "上次上机时间", index = 23)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate lastMachineOnTime;
    @ExcelProperty(value = "是否过保", index = 24)
    private String isOutOfWarranty;
    @ExcelIgnore
    private LocalDateTime createdAt;
    @ExcelIgnore
    private LocalDateTime updatedAt;
    @ExcelIgnore
    private String createdBy;
    @ExcelIgnore
    private String updatedBy;
}
