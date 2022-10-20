package com.miniorange.exporter.confluence.Controllers;

import ch.qos.logback.classic.Logger;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ExportButton {

    private final Logger LOGGER = (Logger) LoggerFactory.getLogger(this.getClass());
    @Value("${server.servlet.context-path}")
    private String moContextPath;

    @Value("${addon.key}")
    private String addonKey;

    @Autowired
    private AtlassianHostRestClients atlassianHostRestClients;

    @RequestMapping(value = "/exporter-button", method = RequestMethod.GET)
    public String showExportButton(Model model, @AuthenticationPrincipal AtlassianHostUser hostUser) {
        model.addAttribute("moContextPath", moContextPath);
        model.addAttribute("isLicenseActive", isLicenseActive(hostUser.getHost().getBaseUrl()));
        return "ExportDialog";
    }

    public boolean isLicenseActive(String hostBaseUrl) {
        JsonNode licensingJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(hostBaseUrl + "/rest/atlassian-connect/1/addons/" + addonKey, JsonNode.class);
        if (licensingJson != null && licensingJson.get("license") != null)
            return BooleanUtils.toBoolean(licensingJson.get("license").get("active").asText());
        return false;
    }

}
