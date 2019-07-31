package com.techgrid.slickbatch.port.primary.web;

import com.techgrid.slickbatch.application.AwsBatchMockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
    @Autowired
    private AwsBatchMockService service;

    @GetMapping("/console")
    public String index(Model model) {
        model.addAttribute("jobs", service.allJobs());
        return "index";
    }

}
