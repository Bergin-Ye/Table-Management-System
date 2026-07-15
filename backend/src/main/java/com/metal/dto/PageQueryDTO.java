package com.metal.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PageQueryDTO {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String keyword;           // 全局模糊搜索
    private Map<String, String> filters; // 精确筛选字段
    private String startDate;
    private String endDate;
    private String sortField = "id";
    private String sortOrder = "desc"; // asc/desc
}
