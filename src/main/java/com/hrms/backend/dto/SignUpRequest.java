package com.hrms.backend.dto;

import com.hrms.backend.entities.Role;
import lombok.Data;

@Data
public class SignUpRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
}
