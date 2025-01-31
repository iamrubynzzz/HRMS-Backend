package com.hrms.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "user_informations")
public class UserInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @ToString.Exclude // Prevent recursion in toString()
    @JsonIgnore
    private User user;
    private String address;
    private String contact;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate hireDate;
    private Double salary;
    private Integer annualLeaveBalance;
    private Integer sickLeaveBalance;
}
