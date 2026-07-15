package com.metal.mapper;

import com.metal.entity.MachineDetail;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MachineDetailMapper {

    @Select("SELECT * FROM machine_detail WHERE id = #{id}")
    MachineDetail findById(Long id);

    @Select("SELECT * FROM machine_detail ORDER BY id")
    List<MachineDetail> findAll();

    @Insert("INSERT INTO machine_detail (company_id, factory, machine_no, machine_brand) VALUES (#{companyId}, #{factory}, #{machineNo}, #{machineBrand})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MachineDetail record);

    @Update("UPDATE machine_detail SET factory=#{factory}, machine_no=#{machineNo}, machine_brand=#{machineBrand} WHERE id=#{id}")
    int update(MachineDetail record);

    @Delete("DELETE FROM machine_detail WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("<script>DELETE FROM machine_detail WHERE id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    int batchDelete(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "SELECT * FROM machine_detail WHERE 1=1 " +
            "<if test='companyId != null'>AND company_id = #{companyId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (factory LIKE CONCAT('%',#{keyword},'%') OR machine_no LIKE CONCAT('%',#{keyword},'%') " +
            "OR machine_brand LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='factory != null and factory != \"\"'>AND factory = #{factory}</if> " +
            "<if test='brand != null and brand != \"\"'>AND machine_brand = #{brand}</if> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "</script>")
    List<MachineDetail> search(@Param("companyId") Long companyId, @Param("keyword") String keyword,
                               @Param("factory") String factory,
                               @Param("brand") String brand,
                               @Param("sortField") String sortField,
                               @Param("sortOrder") String sortOrder);
}
