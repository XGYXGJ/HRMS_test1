package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.PersonnelFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PersonnelFileMapper extends BaseMapper<PersonnelFile> {
    // 自定义查询：联表查询档案和机构名称
    List<Map<String, Object>> selectFilesWithOrgName(@Param("l3OrgId") Integer l3OrgId, @Param("q") String q);

    @Select("SELECT * FROM T_Personnel_File WHERE Audit_Status = #{auditStatus}")
    List<PersonnelFile> findByAuditStatus(String auditStatus);
}
