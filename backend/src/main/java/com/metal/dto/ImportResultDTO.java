package com.metal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDTO {
    private int total;
    private int success;
    private int fail;
    private List<FailDetail> failDetails;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FailDetail {
        private int row;
        private String reason;
    }
}
