package com.metal.controller;

import com.metal.common.Result;
import com.metal.entity.SysUser;
import com.metal.service.UserManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class UserManageController {

    @Autowired
    private UserManageService userManageService;

    @GetMapping
    public Result<List<SysUser>> list() {
        return Result.ok(userManageService.listAll());
    }

    @PutMapping("/{id}/role")
    public Result<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userManageService.updateRole(id, body.get("role"));
        return Result.ok("角色更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userManageService.deleteUser(id);
        return Result.ok("用户删除成功");
    }

    @PutMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userManageService.resetPassword(id, body.get("password"));
        return Result.ok("密码重置成功");
    }
}
