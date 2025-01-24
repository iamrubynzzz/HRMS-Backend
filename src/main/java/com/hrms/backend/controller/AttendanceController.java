package com.hrms.backend.controller;

import com.hrms.backend.entities.Attendance;
import com.hrms.backend.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {



    @GetMapping("/log")
    public ResponseEntity<String> handleLog() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Attendance done.");
    }


}
