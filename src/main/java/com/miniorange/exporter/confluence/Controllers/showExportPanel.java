package com.miniorange.exporter.confluence.Controllers;

import com.atlassian.connect.spring.AtlassianHostUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class showExportPanel {

    @RequestMapping(value = "/exporter_configurations", method = RequestMethod.GET)
    public String showConfigInfo(Model model, @AuthenticationPrincipal AtlassianHostUser hostUser) {
        model.addAttribute("host_url", hostUser.getHost().getBaseUrl());
        return "ConfigInfo";
    }
}
