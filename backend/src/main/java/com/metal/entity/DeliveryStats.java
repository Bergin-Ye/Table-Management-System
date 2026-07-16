package com.metal.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeliveryStats {
    /** 主键ID（导入导出时忽略） */
    @ExcelIgnore
    private Long id;
    /** 公司ID（导入导出时忽略） */
    @ExcelIgnore
    private Long companyId;
    /** 类别 */
    @ExcelProperty(value = "类别", index = 0)
    private String category;
    /** 物料编码 */
    @ExcelProperty(value = "物料编码", index = 1)
    private String materialCode;
    /** 系统名称 */
    @ExcelProperty(value = "系统名称", index = 2)
    private String systemName;
    /** 零件名称 */
    @ExcelProperty(value = "零件名称", index = 3)
    private String partName;
    /** 单台用量 */
    @ExcelProperty(value = "单台用量", index = 4)
    private BigDecimal unitUsage;
    /** 比例 */
    @ExcelProperty(value = "比例", index = 5)
    private BigDecimal ratio;
    /** 含税单价 */
    @ExcelProperty(value = "含税单价", index = 6)
    private BigDecimal unitPriceWithTax;
    /** 机台数 */
    @ExcelProperty(value = "机台数", index = 7)
    private Integer machineCount;
    /** 送货数量 */
    @ExcelProperty(value = "送货数量", index = 8)
    private Integer deliveryQuantity;
    /** 上机数量 */
    @ExcelProperty(value = "上机数量", index = 9)
    private Integer machineOnQuantity;
    /** 当月返修 */
    @ExcelProperty(value = "当月返修", index = 10)
    private Integer monthRepair;
    /** 约定比例数量 */
    @ExcelProperty(value = "约定比例数量", index = 11)
    private BigDecimal agreedRatioQuantity;
    /** 超比数量合计 */
    @ExcelProperty(value = "超比数量合计", index = 12)
    private BigDecimal excessQuantity;
    /** 超比含税金额合计 */
    @ExcelProperty(value = "超比含税金额合计", index = 13)
    private BigDecimal excessAmountWithTax;
    /** 统计日期 */
    @ExcelProperty(value = "统计日期", index = 14)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate statDate;
    /** 年+月 */
    @ExcelProperty(value = "年+月", index = 15)
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
