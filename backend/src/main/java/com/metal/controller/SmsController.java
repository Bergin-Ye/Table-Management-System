package com.metal.controller;

import com.metal.common.Result;
import com.metal.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/send-code")
    public Result<?> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");
        if (phone == null || !phone.matches("1\\d{10}")) {
            return Result.fail("请输入正确的手机号");
        }
        boolean ok = smsService.sendCode(phone);
        if (ok) return Result.ok("验证码已发送");
        return Result.fail("验证码发送失败，请稍后重试");
    }
}
