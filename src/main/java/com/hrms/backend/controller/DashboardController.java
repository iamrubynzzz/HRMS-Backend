package com.hrms.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {
    @GetMapping
    public ResponseEntity<String> dashboard() {
        return ResponseEntity.ok("Welcome to the Flourish HR Dashboard, user is authenticated.");
    }
}
