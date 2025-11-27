package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.PersonnelFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PersonnelFileMapper extends BaseMapper<PersonnelFile> {
    // 自定义查询：联表查询档案和机构名称
    @Select("SELECT f.*, o.Org_Name as orgName FROM T_Personnel_File f " +
            "LEFT JOIN T_Organization o ON f.L3_Org_ID = o.Org_ID " +
            "WHERE f.Is_Deleted = 0")
    List<Map<String, Object>> selectFilesWithOrgName();
}
