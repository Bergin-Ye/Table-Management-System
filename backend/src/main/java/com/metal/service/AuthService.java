package com.metal.service;

import com.metal.common.BizException;
import com.metal.dto.LoginResultDTO;
import com.metal.entity.SysUser;
import com.metal.interceptor.JwtUtil;
import com.metal.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired(required = false)
    private SmsService smsService;

    /**
     * 手机验证码登录
     */
    public LoginResultDTO smsLogin(String phoneNumber, String code) {
        // 校验验证码
        if (smsService == null || !smsService.verifyCode(phoneNumber, code)) {
            throw new BizException("验证码错误或已过期");
        }
        // 查找用户
        SysUser user = sysUserMapper.findByUsername(phoneNumber);
        if (user == null) {
            throw new BizException("该手机号未注册，请联系管理员");
        }
        String role = user.getRole() != null ? user.getRole() : "user";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRealName(), role);
        return new LoginResultDTO(token, new LoginResultDTO.UserInfo(
                user.getId(), user.getUsername(), user.getRealName(), role));
    }

    /**
     * 管理员注册（仅需手机号和姓名，无需密码）
     */
    public SysUser register(String phoneNumber, String realName) {
        if (!phoneNumber.matches("1\\d{10}")) {
            throw new BizException("请输入正确的手机号");
        }
        SysUser exist = sysUserMapper.findByUsername(phoneNumber);
        if (exist != null) {
            throw new BizException("该手机号已注册");
        }
        SysUser user = new SysUser();
        user.setUsername(phoneNumber);
        user.setPassword(""); // 无密码，仅走验证码登录
        user.setRealName(realName);
        user.setRole("user");
        sysUserMapper.insert(user);
        return user;
    }
}
