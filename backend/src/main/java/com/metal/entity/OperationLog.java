package com.metal.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String action;       // INSERT, UPDATE, DELETE
    private String tableName;
    private Long recordId;
    private String detail;
    private String ip;
    private Long companyId;
    private LocalDateTime createdAt;
}
