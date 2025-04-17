package com.hrms.backend.dto;

import com.hrms.backend.entities.Role;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
