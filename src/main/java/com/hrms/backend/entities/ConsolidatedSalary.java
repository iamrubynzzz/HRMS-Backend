package com.hrms.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "consolidated_salary")
@Getter
@Setter
public class ConsolidatedSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_number_of_employees", nullable = false)
    private int totalNumberOfEmployees;

    @Column(name = "total_expense", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "calculation_date", nullable = false, updatable = false)
    private LocalDate calculationDate;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ConsolidatedSalaryStatus status;

    @PrePersist
    protected void onCreate() {
        this.calculationDate = LocalDate.now();
    }
}
