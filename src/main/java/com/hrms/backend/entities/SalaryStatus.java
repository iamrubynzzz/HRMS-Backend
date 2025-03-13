package com.hrms.backend.entities;

public enum SalaryStatus {
    PENDING_REVIEW, // Default status when calculated
    APPROVED,       // Reviewed by admin
    RELEASED        // Finalized and paid
}
