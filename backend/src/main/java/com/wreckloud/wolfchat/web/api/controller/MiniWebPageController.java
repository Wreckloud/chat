package com.wreckloud.wolfchat.web.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web 版小程序页面入口（静态资源）。
 */
@Controller
public class MiniWebPageController {

    @GetMapping({"/mini-web", "/mini-web/"})
    public String index() {
        return "forward:/mini-web/index.html";
    }
}

