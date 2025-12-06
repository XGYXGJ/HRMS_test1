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

    // 原有方法1
    @Select("SELECT * FROM T_Position")
    List<Position> selectAll();

    // 原有方法2
    @Select("SELECT DISTINCT p.* " +
            "FROM T_Position p " +
            "JOIN T_User u ON p.Position_ID = u.Position_ID " +
            "WHERE u.L3_Org_ID = #{l3OrgId}")
    List<Position> selectPositionsInL3Org(Integer l3OrgId);

    // 原有方法3
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

    // 原有方法4
    @Select("SELECT * FROM T_Position WHERE L3_Org_ID = #{l3OrgId}")
    List<Position> selectPositionsByOrgId(Integer l3OrgId);

    /**
     * 【新增的方法】根据职位ID列表，查询每个职位的在职员工人数
     * @param positionIds 职位ID列表
     * @return 返回一个Map列表，每个Map包含 "positionId" 和 "employeeCount"
     */
    @Select({
            "<script>",
            "SELECT Position_ID as positionId, COUNT(User_ID) as employeeCount FROM T_User",
            "WHERE Is_Deleted = 0 AND Position_ID IN",
            "<foreach item='id' index='index' collection='positionIds' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "GROUP BY Position_ID",
            "</script>"
    })
    List<Map<String, Object>> countEmployeesByPositionIds(@Param("positionIds") List<Integer> positionIds);
}
