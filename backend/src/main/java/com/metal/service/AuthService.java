package com.metal.service;

import com.metal.common.BizException;
import com.metal.dto.LoginDTO;
import com.metal.dto.LoginResultDTO;
import com.metal.entity.SysUser;
import com.metal.interceptor.JwtUtil;
import com.metal.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResultDTO login(LoginDTO dto) {
        SysUser user = sysUserMapper.findByUsername(dto.getUsername());
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRealName());
        return new LoginResultDTO(token, new LoginResultDTO.UserInfo(user.getId(), user.getUsername(), user.getRealName()));
    }

    public SysUser register(String username, String password, String realName) {
        SysUser exist = sysUserMapper.findByUsername(username);
        if (exist != null) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        sysUserMapper.insert(user);
        return user;
    }
}
