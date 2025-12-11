package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.SalaryRegisterDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
/*
 * 自定义查询：查找某员工所有属于“已批准(Approved)”发放单的明细
 * 只展示已发放的工资，避免员工看到未审核/未发放的记录
 */
public interface SalaryRegisterDetailMapper extends BaseMapper<SalaryRegisterDetail> {

    /**
     * 查询指定员工的所有已批准工资明细
     * 连接主表 T_Salary_Register_Master，限定 Audit_Status='Approved'
     */
    @Select("""
            SELECT d.*
            FROM T_Salary_Register_Detail d
            JOIN T_Salary_Register_Master m
              ON d.Register_ID = m.Register_ID
            WHERE d.User_ID = #{userId}
              AND m.Audit_Status = 'Approved'
            ORDER BY m.Pay_Date DESC, d.Detail_ID DESC
            """)
    List<SalaryRegisterDetail> selectApprovedDetailsByUser(Integer userId);

    /**
     * 查询指定员工在某一月份内的已批准工资明细
     * @param userId    员工ID
     * @param startDate 月份起始（含）
     * @param endDate   下个月第一天（不含）
     */
    @Select("""
            SELECT d.*
            FROM T_Salary_Register_Detail d
            JOIN T_Salary_Register_Master m
              ON d.Register_ID = m.Register_ID
            WHERE d.User_ID = #{userId}
              AND m.Audit_Status = 'Approved'
              AND d.Payroll_Month >= #{startDate}
              AND d.Payroll_Month < #{endDate}
            ORDER BY m.Pay_Date DESC, d.Detail_ID DESC
            """)
    List<SalaryRegisterDetail> selectApprovedDetailsByUserAndMonth(Integer userId,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate);
}
