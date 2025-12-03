package com.wreckloud.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @Author Wreckloud
 * @Date 2025-12-03
 */
@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "Hello World!";
    }
}
