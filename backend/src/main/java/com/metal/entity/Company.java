package com.metal.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Company {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
