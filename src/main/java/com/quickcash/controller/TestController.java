package com.quickcash.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/secure")
    public Map<String, String> secureTest() {

        return Map.of(
                "message",
                "JWT authentication is working!"
        );
    }
}
