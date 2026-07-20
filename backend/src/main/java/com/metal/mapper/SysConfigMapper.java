package com.metal.mapper;

import com.metal.entity.SysConfig;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysConfigMapper {

    @Select("SELECT * FROM sys_config WHERE config_key = #{key}")
    SysConfig findByKey(@Param("key") String key);

    @Select("SELECT * FROM sys_config")
    List<SysConfig> findAll();

    @Update("UPDATE sys_config SET config_value = #{configValue} WHERE config_key = #{configKey}")
    int updateValue(@Param("configKey") String configKey, @Param("configValue") String configValue);

    @Insert("INSERT INTO sys_config (config_key, config_value, description) VALUES (#{configKey}, #{configValue}, #{description})")
    int insert(SysConfig config);
}
