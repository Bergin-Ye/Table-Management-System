package com.metal.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteDTO {
    private List<Long> ids;
}
