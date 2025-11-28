package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
/*
 * 自定义查询：查找某员工所有属于“已批准(Approved)”发放单的明细
 * 解决了员工能看到未审核工资的漏洞
 */
public interface SalaryRegisterDetailMapper extends BaseMapper<SalaryRegisterDetail> {
    @Select("SELECT d.* FROM T_Salary_Register_Detail d " +
            "LEFT JOIN T_Salary_Register_Master m ON d.Register_ID = m.Register_ID " +
            "WHERE d.User_ID = #{userId} AND m.Audit_Status = 'Approved' " +
            "ORDER BY m.Pay_Date DESC")
    List<SalaryRegisterDetail> selectApprovedDetailsByUserId(Integer userId);
}
