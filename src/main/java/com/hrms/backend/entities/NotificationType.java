package com.hrms.backend.entities;

public enum NotificationType {
    REQUEST,       // When an employee creates a request (leave, overtime, etc.)
    LEAVE_APPROVED, // When a leave request is approved
    LEAVE_REJECTED, // When a leave request is rejected
    SALARY_PROCESSED, // When salary is processed and reviewed

}
