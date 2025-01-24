package com.hrms.backend.dto;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Integer id;
    private String email;
    private String name;
    private Role role;
    private UserStatus status;


    public UserDTO(Integer id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }


}
