package com.metal.entity;

import lombok.Data;

@Data
public class MachineDetail {
    private Long id;
    private Long companyId;
    private String factory;
    private String machineNo;
    private String machineBrand;
}
