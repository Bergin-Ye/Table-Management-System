package com.metal.mapper;

import com.metal.entity.DeliveryRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DeliveryRecordMapper {

    @Select("SELECT * FROM delivery_record ORDER BY id DESC")
    List<DeliveryRecord> findAll();

    @Select("SELECT * FROM delivery_record WHERE id = #{id}")
    DeliveryRecord findById(Long id);

    @Insert("INSERT INTO delivery_record (company_id, record_date, category, material_name, spec_model, material_code, " +
            "material_serial, quantity, unit, brand, product_attr, factory, shipment_no, remark, `year_month`, created_by, updated_by) " +
            "VALUES (#{companyId}, #{recordDate}, #{category}, #{materialName}, #{specModel}, #{materialCode}, " +
            "#{materialSerial}, #{quantity}, #{unit}, #{brand}, #{productAttr}, #{factory}, #{shipmentNo}, " +
            "#{remark}, #{yearMonth}, #{createdBy}, #{updatedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DeliveryRecord record);

    @Update("UPDATE delivery_record SET record_date=#{recordDate}, category=#{category}, material_name=#{materialName}, " +
            "spec_model=#{specModel}, material_code=#{materialCode}, material_serial=#{materialSerial}, " +
            "quantity=#{quantity}, unit=#{unit}, brand=#{brand}, product_attr=#{productAttr}, factory=#{factory}, " +
            "shipment_no=#{shipmentNo}, remark=#{remark}, `year_month`=#{yearMonth}, updated_by=#{updatedBy} " +
            "WHERE id=#{id}")
    int update(DeliveryRecord record);

    @Delete("DELETE FROM delivery_record WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("<script>DELETE FROM delivery_record WHERE id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    int batchDelete(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "SELECT * FROM delivery_record WHERE 1=1 " +
            "<if test='companyId != null'>AND company_id = #{companyId}</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (category LIKE CONCAT('%',#{keyword},'%') OR material_name LIKE CONCAT('%',#{keyword},'%') " +
            "OR spec_model LIKE CONCAT('%',#{keyword},'%') OR material_code LIKE CONCAT('%',#{keyword},'%') " +
            "OR material_serial LIKE CONCAT('%',#{keyword},'%') OR brand LIKE CONCAT('%',#{keyword},'%') " +
            "OR factory LIKE CONCAT('%',#{keyword},'%') OR shipment_no LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='category != null and category != \"\"'>AND category = #{category}</if> " +
            "<if test='productAttr != null and productAttr != \"\"'>AND product_attr = #{productAttr}</if> " +
            "<if test='factory != null and factory != \"\"'>AND factory = #{factory}</if> " +
            "<if test='startDate != null'>AND record_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND record_date &lt;= #{endDate}</if> " +
            "ORDER BY ${sortField} ${sortOrder} " +
            "</script>")
    List<DeliveryRecord> search(@Param("companyId") Long companyId,
                                @Param("keyword") String keyword,
                                @Param("category") String category,
                                @Param("productAttr") String productAttr,
                                @Param("factory") String factory,
                                @Param("startDate") String startDate,
                                @Param("endDate") String endDate,
                                @Param("sortField") String sortField,
                                @Param("sortOrder") String sortOrder);

    @Select("SELECT DISTINCT material_code, category, material_name, spec_model FROM delivery_record")
    List<DeliveryRecord> findDistinctMaterials();

    /** 批量插入（每批最多 500 条，提升大数据量导入性能） */
    @Insert("<script>" +
            "INSERT INTO delivery_record (company_id, record_date, category, material_name, spec_model, material_code, " +
            "material_serial, quantity, unit, brand, product_attr, factory, shipment_no, remark, `year_month`, created_by, updated_by) VALUES " +
            "<foreach collection='list' item='r' separator=','>" +
            "(#{r.companyId}, #{r.recordDate}, #{r.category}, #{r.materialName}, #{r.specModel}, #{r.materialCode}, " +
            "#{r.materialSerial}, #{r.quantity}, #{r.unit}, #{r.brand}, #{r.productAttr}, #{r.factory}, #{r.shipmentNo}, " +
            "#{r.remark}, #{r.yearMonth}, #{r.createdBy}, #{r.updatedBy})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<DeliveryRecord> records);
}
