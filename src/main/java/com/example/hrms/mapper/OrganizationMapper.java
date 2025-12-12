package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.dto.OrgDTO;
import com.example.hrms.entity.Organization;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
    List<OrgDTO> selectOrgList();
}
