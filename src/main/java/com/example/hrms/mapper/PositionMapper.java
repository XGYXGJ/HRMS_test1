package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {
    // 简单的查询所有职位
    @Select("SELECT * FROM T_Position")
    List<Position> selectAll();

    // 根据L3机构ID查询职位
    @Select("SELECT * FROM T_Position WHERE L3_Org_ID = #{l3OrgId}")
    List<Position> selectPositionsByL3OrgId(Integer l3OrgId);
}