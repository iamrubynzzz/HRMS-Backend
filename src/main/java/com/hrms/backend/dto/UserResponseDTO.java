package com.hrms.backend.dto;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserResponseDTO {
    private Integer id;
    private String name;
    private String email;
    private Role role;
    private String address;
    private String rfid;
    private String contact;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate hireDate;
    private Double salary;
    private String managerName;
    private Integer annualLeaveBalance;
    private Integer sickLeaveBalance;

    // Constructor
    public UserResponseDTO(User user, UserInfo userInfo, String managerName) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.rfid = user.getRfid();

        this.managerName = managerName != null ? managerName : "No Manager"; // Default if no manager found
        if (userInfo != null) {
            this.address = userInfo.getAddress();
            this.contact = userInfo.getContact();
            this.gender = userInfo.getGender();
            this.salary = userInfo.getSalary();
            this.dateOfBirth = userInfo.getDateOfBirth();
            this.hireDate = userInfo.getHireDate();
            this.annualLeaveBalance = userInfo.getAnnualLeaveBalance();
            this.sickLeaveBalance = userInfo.getSickLeaveBalance();
        }
    }
}

