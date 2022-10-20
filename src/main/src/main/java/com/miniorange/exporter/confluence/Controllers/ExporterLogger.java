package com.miniorange.exporter.confluence.Controllers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.atlassian.connect.spring.ContextJwt;
import com.miniorange.exporter.confluence.ExporterPluginConstants;
import com.miniorange.exporter.confluence.ExporterUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@ContextJwt
public class ExporterLogger {

    private final Logger logger = (Logger) LoggerFactory.getLogger(com.miniorange.exporter.confluence.Controllers.ExporterLogger.class);

    @RequestMapping(value = "/changeExportersLoglevel", method = RequestMethod.POST)
    @ResponseBody
    public String changeLogLevel(@RequestParam("loglevel") String logLevel, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(request.getHeader("Authorization"))) return "BAD_REQUEST";
        logLevel=ExporterUtils.sanitizeText(logLevel);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(ExporterPluginConstants.LOGGING_PACKAGE).setLevel(Level.toLevel(logLevel));
        return "Logging level successfully changed to: " + logLevel;
    }

    @RequestMapping(value = "/getExportersLogLevel", method = RequestMethod.GET)
    @ResponseBody
    public String getLogLevel() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger(ExporterPluginConstants.LOGGING_PACKAGE).getLevel().levelStr;
    }
}
