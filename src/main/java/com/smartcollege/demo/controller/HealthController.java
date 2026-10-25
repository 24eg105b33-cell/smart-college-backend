package com.smartcollege.demo.controller;

import com.smartcollege.demo.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        Map<String, Object> healthData = new LinkedHashMap<>();
        healthData.put("status", "UP");
        healthData.put("service", "Smart College Resource Management System API");
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("version", "1.0.0");

        return ResponseEntity.ok(ApiResponse.success("System is operating normally", healthData));
    }
}
