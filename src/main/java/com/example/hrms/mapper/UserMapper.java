package com.example.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hrms.dto.UserDTO;
import com.example.hrms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    List<UserDTO> searchUsers(@Param("query") String query);
    List<UserDTO> findUsersByOrgAndPosition(@Param("orgId") Integer orgId, @Param("positionId") Integer positionId);
    List<UserDTO> searchUsersByOrg(@Param("orgId") Integer orgId);
    List<UserDTO> searchUsersByNameAndPosition(@Param("name") String name, @Param("position") String position, @Param("orgId") Integer orgId);
    List<UserDTO> searchUsersByOrgAndQuery(@Param("orgId") Integer orgId, @Param("q") String q);
}