package com.metal.controller;

import com.metal.common.PageResult;
import com.metal.common.Result;
import com.metal.entity.OperationLog;
import com.metal.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operation-log")
public class OperationLogController {

    @Autowired
    private OperationLogService service;

    @GetMapping
    public Result<PageResult<OperationLog>> query(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(service.query(page, pageSize, userId, tableName, action, startDate, endDate));
    }
}
