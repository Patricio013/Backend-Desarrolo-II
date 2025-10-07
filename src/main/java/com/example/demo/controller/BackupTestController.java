package com.example.demo.controller;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.response.ModuleResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class BackupTestController {

    private final ModuleResponseFactory responseFactory;

    @GetMapping("/ping")
    public ModuleResponse<String> ping() {
        return responseFactory.build("health", "ping", "Backend funcionando OK 🚀");
    }
}
