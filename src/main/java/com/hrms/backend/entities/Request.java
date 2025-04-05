package com.hrms.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Reference to User entity

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    private LocalDate startDate; // Used for leave and overtime

    private LocalDate endDate; // Used for leave and overtime
    @Column(updatable = false)
    private LocalDate createdDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDate.now(); // Set createdDate when saving for the first time
    }
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING; // Default status

    private Integer leaveDays; // Only applicable for leave requests

    private float allowanceAmount;
    private int overtimeHours;
    private int approvedBy;
    private int rejectedBy;

}
