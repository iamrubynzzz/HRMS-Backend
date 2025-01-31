package com.hrms.backend.services;

import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.UserInfo;

import java.util.List;

public interface EmployeeService {
    void createEmployee(UserRequestDTO userRequestDTO, UserInfo userInfo);

    UserResponseDTO getEmployeeById(Integer id);

    List<UserResponseDTO> getAllEmployees();

    void updateEmployee(Integer id, UserRequestDTO userRequestDTO);


    void deleteEmployee(Integer id);
}
