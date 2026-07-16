package com.metal.mapper;

import com.metal.entity.MachineCount;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MachineCountMapper {

    @Select("SELECT * FROM machine_count WHERE id = #{id}")
    MachineCount findById(Long id);

    @Select("SELECT * FROM machine_count ORDER BY id")
    List<MachineCount> findAll();

    @Insert("INSERT INTO machine_count (company_id, machine_model, count, ratio_pct, stat_month, remark) " +
            "VALUES (#{companyId}, #{machineModel}, #{count}, #{ratioPct}, #{statMonth}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MachineCount record);

    @Update("UPDATE machine_count SET machine_model=#{machineModel}, count=#{count}, " +
            "ratio_pct=#{ratioPct}, stat_month=#{statMonth}, remark=#{remark} WHERE id=#{id}")
    int update(MachineCount record);

    @Delete("DELETE FROM machine_count WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("<script>DELETE FROM machine_count WHERE id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    int batchDelete(@Param("ids") List<Long> ids);

    @Insert("<script>" +
            "INSERT INTO machine_count (company_id, machine_model, count, ratio_pct, stat_month, remark, created_by, updated_by) VALUES " +
            "<foreach collection='list' item='r' separator=','>" +
            "(#{r.companyId}, #{r.machineModel}, #{r.count}, #{r.ratioPct}, #{r.statMonth}, #{r.remark}, #{r.createdBy}, #{r.updatedBy})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<MachineCount> records);

    @Select("<script>" +
            "SELECT * FROM machine_count WHERE 1=1 " +
            "<if test='companyId != null'>AND company_id = #{companyId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND machine_model LIKE CONCAT('%',#{keyword},'%') " +
            "</if>" +
            "<if test='statMonth != null and statMonth != \"\"'>AND stat_month = #{statMonth}</if> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "</script>")
    List<MachineCount> search(@Param("companyId") Long companyId, @Param("keyword") String keyword,
                              @Param("statMonth") String statMonth,
                              @Param("sortField") String sortField,
                              @Param("sortOrder") String sortOrder);
}
