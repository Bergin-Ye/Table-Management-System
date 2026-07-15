package com.metal.controller;

import com.metal.common.Result;
import com.metal.dto.LoginDTO;
import com.metal.dto.LoginResultDTO;
import com.metal.interceptor.AuthInterceptor;
import com.metal.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginResultDTO> login(@RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        authService.register(body.get("username"), body.get("password"), body.get("realName"));
        return Result.ok("注册成功");
    }

    @GetMapping("/me")
    public Result<?> me() {
        AuthInterceptor.UserContext ctx = AuthInterceptor.getCurrentUser();
        return Result.ok(ctx);
    }
}
