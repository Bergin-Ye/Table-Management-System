package com.metal.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeliveryRecord {
    /** 主键ID（导入导出时忽略） */
    @ExcelIgnore
    private Long id;
    /** 公司ID（导入导出时忽略） */
    @ExcelIgnore
    private Long companyId;
    /** 日期 */
    @ExcelProperty(value = "日期", index = 0)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate recordDate;
    /** 类别 */
    @ExcelProperty(value = "类别", index = 1)
    private String category;
    /** 物料名称 */
    @ExcelProperty(value = "物料名称", index = 2)
    private String materialName;
    /** 规格型号 */
    @ExcelProperty(value = "规格型号", index = 3)
    private String specModel;
    /** 物料编码 */
    @ExcelProperty(value = "物料编码", index = 4)
    private String materialCode;
    /** 物料序列号 */
    @ExcelProperty(value = "物料序列号", index = 5)
    private String materialSerial;
    /** 数量 */
    @ExcelProperty(value = "数量", index = 6)
    private Integer quantity;
    /** 单位 */
    @ExcelProperty(value = "单位", index = 7)
    private String unit;
    /** 品牌 */
    @ExcelProperty(value = "品牌", index = 8)
    private String brand;
    /** 产品属性（新品/维修品） */
    @ExcelProperty(value = "产品属性", index = 9)
    private String productAttr;
    /** 厂房 */
    @ExcelProperty(value = "厂房", index = 10)
    private String factory;
    /** 出厂单号 */
    @ExcelProperty(value = "出厂单号", index = 11)
    private String shipmentNo;
    /** 备注 */
    @ExcelProperty(value = "备注", index = 12)
    private String remark;
    /** 年+月（格式 FYyyMM，如 FY2607） */
    @ExcelProperty(value = "年+月", index = 13)
    private String yearMonth;
    /** 创建时间（导入导出时忽略） */
    @ExcelIgnore
    private LocalDateTime createdAt;
    /** 更新时间（导入导出时忽略） */
    @ExcelIgnore
    private LocalDateTime updatedAt;
    /** 创建人（导入导出时忽略） */
    @ExcelIgnore
    private String createdBy;
    /** 更新人（导入导出时忽略） */
    @ExcelIgnore
    private String updatedBy;
}
