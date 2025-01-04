package com.hrms.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "employee_manager")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeManager {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "manager_id", nullable = false)
    private Integer managerId;

    @Column(name = "updated_date", nullable = false)
    @UpdateTimestamp
    private Timestamp date;
}
