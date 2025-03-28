package com.hrms.backend.entities;

public enum Status {
    PENDING,   // Waiting for admin approval
    APPROVED,  // Approved by admin
    DISABLED,
    REJECTED   // Rejected by admin
}
