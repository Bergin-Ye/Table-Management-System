package com.metal.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MachineCount {
    private Long id;
    private Long companyId;
    private String machineModel;
    private Integer count;
    private BigDecimal ratioPct;
    private String statMonth;
    private String remark;
}
