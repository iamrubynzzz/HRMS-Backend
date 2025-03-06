package com.hrms.backend.dto;

import com.hrms.backend.entities.Role;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequestDTO {
    private String name;
    private String email;
    private String password;
    private Role role;
    private String address;
    private String contact;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate hireDate;
    private Double salary;
    private int managerId;
    private String rfid;
    private Integer  annualLeaveBalance;
    private Integer  sickLeaveBalance;
    private Integer companyId;
}
