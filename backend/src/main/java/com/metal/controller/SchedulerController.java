package com.metal.controller;

import com.metal.common.Result;
import com.metal.interceptor.AuthInterceptor;
import com.metal.scheduler.DeliveryStatsScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/scheduler")
public class SchedulerController {

    @Autowired
    private DeliveryStatsScheduler scheduler;

    @GetMapping
    public Result<?> getCron() {
        AuthInterceptor.UserContext ctx = AuthInterceptor.getCurrentUser();
        if (ctx == null || !"admin".equals(ctx.getRole())) {
            return Result.fail("无权限：仅管理员可操作");
        }
        return Result.ok(Map.of("cron", scheduler.getCurrentCron()));
    }

    @PutMapping
    public Result<?> updateCron(@RequestBody Map<String, String> body) {
        AuthInterceptor.UserContext ctx = AuthInterceptor.getCurrentUser();
        if (ctx == null || !"admin".equals(ctx.getRole())) {
            return Result.fail("无权限：仅管理员可操作");
        }
        String cron = body.get("cron");
        if (cron == null || cron.isBlank()) {
            return Result.fail("cron表达式不能为空");
        }
        // 简单校验 cron 格式 (5段或6段)
        String[] parts = cron.trim().split("\\s+");
        if (parts.length < 5 || parts.length > 7) {
            return Result.fail("cron表达式格式不正确，应为5-7段空格分隔的值");
        }
        try {
            scheduler.updateCron(cron.trim());
            return Result.ok(Map.of("msg", "定时任务已更新", "cron", cron.trim()));
        } catch (Exception e) {
            return Result.fail("cron表达式无效: " + e.getMessage());
        }
    }
}
