package com.hrms.backend.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    ADMIN,
    MANAGER,
    EMPLOYEE;

    @JsonCreator
    public static Role fromString(String role) {
        if (role == null) {
            return null;
        }
        // Normalize the input to upper case to match the enum names
        try {
            return Role.valueOf(role.trim().toUpperCase()); // Convert to uppercase before matching
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase(); // This ensures the enum is serialized in lowercase
    }
}
