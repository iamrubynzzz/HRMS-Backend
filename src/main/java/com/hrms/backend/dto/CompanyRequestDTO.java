package com.hrms.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CompanyRequestDTO {
    private String name;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String industryType;
    private String registrationNumber;
    private LocalDate establishedDate;
}
