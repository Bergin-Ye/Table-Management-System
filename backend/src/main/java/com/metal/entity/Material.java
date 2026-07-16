package com.metal.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class Material {
    @ExcelIgnore
    private Long id;
    @ExcelProperty(value = "类别", index = 0)
    private String category;
    @ExcelProperty(value = "物料名称", index = 1)
    private String materialName;
    @ExcelProperty(value = "规格型号", index = 2)
    private String specModel;
    @ExcelIgnore
    private Long companyId;
    @ExcelProperty(value = "物料编码", index = 3)
    private String materialCode;
    @ExcelIgnore
    private String createdBy;
    @ExcelIgnore
    private String updatedBy;
}
