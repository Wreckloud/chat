package com.wreckloud.wolfchat.admin.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 管理端页面入口（静态资源）。
 */
@Controller
public class AdminConsolePageController {

    @GetMapping({"/admin-console", "/admin-console/"})
    public String index() {
        return "forward:/admin-console/index.html";
    }
}

