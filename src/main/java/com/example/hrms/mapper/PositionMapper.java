package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {
    // 简单的查询所有职位
    @Select("SELECT * FROM T_Position")
    List<Position> selectAll();

    // 修正后的方法：通过 T_User 表关联，查询指定机构下所有员工所担任的职位
    @Select("SELECT DISTINCT p.* " +
            "FROM T_Position p " +
            "JOIN T_User u ON p.Position_ID = u.Position_ID " +
            "WHERE u.L3_Org_ID = #{l3OrgId}")
    List<Position> selectPositionsInL3Org(Integer l3OrgId);

    @Select({
            "<script>",
            "SELECT Position_ID, Position_Name FROM T_Position",
            "WHERE Position_ID IN",
            "<foreach item='id' index='index' collection='positionIds' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> selectPositionNamesByIds(@Param("positionIds") List<Integer> positionIds);
    @Select("SELECT * FROM T_Position WHERE L3_Org_ID = #{l3OrgId}")
    List<Position> selectPositionsByOrgId(Integer l3OrgId);
}