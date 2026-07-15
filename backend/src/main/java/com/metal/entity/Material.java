package com.metal.entity;

import lombok.Data;

@Data
public class Material {
    private Long id;
    private String category;
    private String materialName;
    private String specModel;
    private Long companyId;
    private String materialCode;
}
