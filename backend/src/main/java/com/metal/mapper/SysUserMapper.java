package com.metal.mapper;

import com.metal.entity.SysUser;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(Long id);

    @Insert("INSERT INTO sys_user (username, password, real_name) VALUES (#{username}, #{password}, #{realName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);
}
