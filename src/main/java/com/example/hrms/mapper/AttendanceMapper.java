package com.example.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttendanceMapper extends BaseMapper<AttendanceRecord> {}