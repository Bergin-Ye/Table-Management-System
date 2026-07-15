package com.metal.mapper;

import com.metal.entity.Company;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CompanyMapper {

    @Select("SELECT * FROM company ORDER BY id")
    List<Company> findAll();

    @Select("SELECT * FROM company WHERE id = #{id}")
    Company findById(Long id);

    @Insert("INSERT INTO company (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Company company);

    @Update("UPDATE company SET name = #{name} WHERE id = #{id}")
    int update(Company company);

    @Delete("DELETE FROM company WHERE id = #{id}")
    int deleteById(Long id);
}
