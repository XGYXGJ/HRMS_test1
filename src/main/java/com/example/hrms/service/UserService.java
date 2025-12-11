package com.example.hrms.service;

import com.example.hrms.dto.UserDTO;
import java.util.List;

public interface UserService {
    List<UserDTO> getUsersByOrgAndPosition(Integer orgId, Integer positionId);
    List<UserDTO> searchUsers(String query);
    boolean transferEmployee(Integer employeeId, Integer targetOrgId, Integer targetPositionId);
}
