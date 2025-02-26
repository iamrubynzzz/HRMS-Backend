package com.hrms.backend.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Salary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private double grossSalary;
    private double taxDeduction;
    private double netSalary;
    private LocalDate calculationDate;
    private int overtimeHoursTotal;
    private float overtimePayTotal;
    private int unpaidLeaveTotal;
    private float unpaidLeaveAmount;
    private float allowanceAmountTotal;
}
