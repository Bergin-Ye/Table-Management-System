package com.metal.mapper;

import com.metal.entity.MachineMaterial;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MachineMaterialMapper {

    @Select("SELECT * FROM machine_material WHERE id = #{id}")
    MachineMaterial findById(Long id);

    @Insert("INSERT INTO machine_material (company_id, `year_month`, record_date, shift, factory, serial_number, machine_no, " +
            "repair_person, repair_request_time, start_time, end_time, repair_hours, downtime_hours, " +
            "machine_model, fault_phenomenon, fault_description, material_code, part_name, quantity, " +
            "machine_on_material, machine_off_material, remark, confirmer, delivery_record_ref, " +
            "last_machine_on_time, is_out_of_warranty, created_by, updated_by) " +
            "VALUES (#{companyId}, #{yearMonth}, #{recordDate}, #{shift}, #{factory}, #{serialNumber}, #{machineNo}, " +
            "#{repairPerson}, #{repairRequestTime}, #{startTime}, #{endTime}, #{repairHours}, #{downtimeHours}, " +
            "#{machineModel}, #{faultPhenomenon}, #{faultDescription}, #{materialCode}, #{partName}, #{quantity}, " +
            "#{machineOnMaterial}, #{machineOffMaterial}, #{remark}, #{confirmer}, #{deliveryRecordRef}, " +
            "#{lastMachineOnTime}, #{isOutOfWarranty}, #{createdBy}, #{updatedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MachineMaterial record);

    @Update("UPDATE machine_material SET `year_month`=#{yearMonth}, record_date=#{recordDate}, shift=#{shift}, " +
            "factory=#{factory}, serial_number=#{serialNumber}, machine_no=#{machineNo}, " +
            "repair_person=#{repairPerson}, repair_request_time=#{repairRequestTime}, " +
            "start_time=#{startTime}, end_time=#{endTime}, repair_hours=#{repairHours}, downtime_hours=#{downtimeHours}, " +
            "machine_model=#{machineModel}, fault_phenomenon=#{faultPhenomenon}, fault_description=#{faultDescription}, " +
            "material_code=#{materialCode}, part_name=#{partName}, quantity=#{quantity}, " +
            "machine_on_material=#{machineOnMaterial}, machine_off_material=#{machineOffMaterial}, " +
            "remark=#{remark}, confirmer=#{confirmer}, delivery_record_ref=#{deliveryRecordRef}, " +
            "last_machine_on_time=#{lastMachineOnTime}, is_out_of_warranty=#{isOutOfWarranty}, updated_by=#{updatedBy} " +
            "WHERE id=#{id}")
    int update(MachineMaterial record);

    @Delete("DELETE FROM machine_material WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("<script>DELETE FROM machine_material WHERE id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    int batchDelete(@Param("ids") List<Long> ids);

    @Select("SELECT MAX(record_date) FROM machine_material WHERE machine_on_material = #{machineOnMaterial}")
    LocalDate findLastMachineOnTime(@Param("machineOnMaterial") String machineOnMaterial);

    @Select("<script>" +
            "SELECT * FROM machine_material WHERE 1=1 " +
            "<if test='companyId != null'>AND company_id = #{companyId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (serial_number LIKE CONCAT('%',#{keyword},'%') OR machine_no LIKE CONCAT('%',#{keyword},'%') " +
            "OR material_code LIKE CONCAT('%',#{keyword},'%') OR machine_model LIKE CONCAT('%',#{keyword},'%') " +
            "OR repair_person LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='factory != null and factory != \"\"'>AND factory = #{factory}</if> " +
            "<if test='isOutOfWarranty != null and isOutOfWarranty != \"\"'>AND is_out_of_warranty = #{isOutOfWarranty}</if> " +
            "<if test='startDate != null'>AND record_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND record_date &lt;= #{endDate}</if> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "</script>")
    List<MachineMaterial> search(@Param("companyId") Long companyId, @Param("keyword") String keyword,
                                 @Param("factory") String factory,
                                 @Param("isOutOfWarranty") String isOutOfWarranty,
                                 @Param("startDate") String startDate,
                                 @Param("endDate") String endDate,
                                 @Param("sortField") String sortField,
                                 @Param("sortOrder") String sortOrder);
}
