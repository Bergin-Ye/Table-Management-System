package com.metal.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
