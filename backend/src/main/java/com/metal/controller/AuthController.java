package com.metal.controller;

import com.metal.common.Result;
import com.metal.dto.LoginResultDTO;
import com.metal.interceptor.AuthInterceptor;
import com.metal.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/sms-login")
    public Result<LoginResultDTO> smsLogin(@RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");
        String code = body.get("code");
        if (phone == null || !phone.matches("1\\d{10}")) {
            return Result.fail("请输入正确的手机号");
        }
        if (code == null || code.length() != 6) {
            return Result.fail("请输入6位验证码");
        }
        return Result.ok(authService.smsLogin(phone, code));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        // 仅管理员可注册
        AuthInterceptor.UserContext ctx = AuthInterceptor.getCurrentUser();
        if (ctx == null || !"admin".equals(ctx.getRole())) {
            return Result.fail("无权限：仅管理员可注册新用户");
        }
        authService.register(body.get("phoneNumber"), body.get("realName"));
        return Result.ok("注册成功");
    }

    @GetMapping("/me")
    public Result<?> me() {
        AuthInterceptor.UserContext ctx = AuthInterceptor.getCurrentUser();
        Map<String, Object> result = new HashMap<>();
        result.put("userId", ctx.getUserId());
        result.put("username", ctx.getUsername());
        result.put("realName", ctx.getRealName());
        result.put("role", ctx.getRole());
        return Result.ok(result);
    }
}
