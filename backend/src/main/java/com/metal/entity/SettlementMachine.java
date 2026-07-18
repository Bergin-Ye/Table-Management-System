package com.metal.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettlementMachine {
    @ExcelIgnore
    private Long id;
    @ExcelIgnore
    private Long companyId;
    @ExcelProperty(value = "物料编码", index = 0)
    private String materialCode;
    @ExcelProperty(value = "类别", index = 1)
    private String category;
    @ExcelProperty(value = "零件名称", index = 2)
    private String partName;
    @ExcelProperty(value = "单台用量", index = 3)
    private BigDecimal unitUsage;
    @ExcelProperty(value = "比例", index = 4)
    private BigDecimal ratio;
    @ExcelProperty(value = "含税单价", index = 5)
    private BigDecimal unitPriceWithTax;
    @ExcelProperty(value = "质保期", index = 6)
    private String warrantyPeriod;
    @ExcelProperty(value = "价格类型", index = 7)
    private String priceType;
    @ExcelProperty(value = "备注", index = 8)
    private String remark;
    @ExcelProperty(value = "机型", index = 9)
    private String machineModel;
    @ExcelProperty(value = "结算机台数", index = 10)
    private Integer settlementMachineCount;
    @ExcelProperty(value = "统计月份", index = 11)
    private String statMonth;
    @ExcelIgnore
    private LocalDateTime createdAt;
    @ExcelIgnore
    private LocalDateTime updatedAt;
    @ExcelIgnore
    private String createdBy;
    @ExcelIgnore
    private String updatedBy;
}
