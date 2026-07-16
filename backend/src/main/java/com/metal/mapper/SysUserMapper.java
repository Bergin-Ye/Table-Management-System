package com.metal.mapper;

import com.metal.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(Long id);

    @Insert("INSERT INTO sys_user (username, password, real_name, role) VALUES (#{username}, #{password}, #{realName}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Select("SELECT id, username, real_name, role, created_at, updated_at FROM sys_user ORDER BY id")
    List<SysUser> findAll();

    @Select("SELECT * FROM sys_user WHERE role = #{role}")
    List<SysUser> findByRole(String role);

    @Update("UPDATE sys_user SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(Long id);
}
