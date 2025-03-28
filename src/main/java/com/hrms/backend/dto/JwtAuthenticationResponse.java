package com.hrms.backend.dto;

import com.hrms.backend.entities.Role;
import lombok.Data;

@Data
public class JwtAuthenticationResponse {
    private String token;
    private String refreshToken;
    private Role role;
}
