package com.hrms.backend.services;

import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {
    void createUser(UserRequestDTO userRequestDTO, UserInfo userInfo);
    UserResponseDTO getUserById(Integer id);

  /*  List<UserResponseDTO> getAllUsers();*/

    void updateUser(Integer id, UserRequestDTO userRequestDTO);

    Page<UserResponseDTO> getAllUsers(String name, int page, int size);
    void deleteUser(Integer id);
}
