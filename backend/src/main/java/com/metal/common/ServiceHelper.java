package com.metal.common;

import com.metal.interceptor.AuthInterceptor;

/**
 * Service 层通用工具方法，消除各 Service 中的重复代码
 */
public final class ServiceHelper {

    private ServiceHelper() {
        // 工具类，禁止实例化
    }

    /**
     * SQL ORDER BY 字段安全过滤，防止 SQL 注入
     * 只允许字母、数字、下划线组成的字段名
     *
     * @param field        前端传入的排序字段
     * @param defaultField 兜底默认字段
     * @return 安全的字段名
     */
    public static String sanitizeSortField(String field, String defaultField) {
        if (field == null || field.isBlank()) {
            return defaultField;
        }
        if (!field.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return defaultField;
        }
        return field;
    }

    /**
     * 安全的排序方向校验
     *
     * @param sortOrder 前端传入的排序方向 (asc/desc)
     * @return 安全的排序方向
     */
    public static String sanitizeSortOrder(String sortOrder) {
        return "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
    }

    /**
     * 获取当前登录用户的真实姓名
     * 未登录时返回 "系统"
     *
     * @return 当前用户姓名
     */
    public static String getCurrentUserName() {
        AuthInterceptor.UserContext ctx = AuthInterceptor.getCurrentUser();
        return ctx != null ? ctx.getRealName() : "系统";
    }
}
