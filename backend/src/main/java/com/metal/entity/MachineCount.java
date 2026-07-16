package com.metal.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MachineCount {
    @ExcelIgnore
    private Long id;
    @ExcelIgnore
    private Long companyId;
    @ExcelProperty(value = "机型", index = 0)
    private String machineModel;
    @ExcelProperty(value = "数量", index = 1)
    private Integer count;
    @ExcelProperty(value = "比例(%)", index = 2)
    private BigDecimal ratioPct;
    @ExcelProperty(value = "统计月份", index = 3)
    private String statMonth;
    @ExcelProperty(value = "备注", index = 4)
    private String remark;
    @ExcelIgnore
    private String createdBy;
    @ExcelIgnore
    private String updatedBy;
}
