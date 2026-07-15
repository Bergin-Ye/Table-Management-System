package com.metal.mapper;

import com.metal.entity.DeliveryStatsDaily;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DeliveryStatsDailyMapper {

    @Select("SELECT * FROM delivery_stats_daily WHERE stat_id = #{statId} ORDER BY day_number")
    List<DeliveryStatsDaily> findByStatId(Long statId);

    @Insert("INSERT INTO delivery_stats_daily (stat_id, day_number, value) VALUES (#{statId}, #{dayNumber}, #{value})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DeliveryStatsDaily daily);

    @Delete("DELETE FROM delivery_stats_daily WHERE stat_id = #{statId}")
    int deleteByStatId(Long statId);

    @Insert("<script>" +
            "INSERT INTO delivery_stats_daily (stat_id, day_number, value) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.statId}, #{item.dayNumber}, #{item.value})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<DeliveryStatsDaily> dailies);
}
