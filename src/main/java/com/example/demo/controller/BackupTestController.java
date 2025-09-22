package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class BackupTestController {

    @GetMapping("/ping")
    public String ping() {
        return "Backend funcionando OK 🚀";
    }
}
