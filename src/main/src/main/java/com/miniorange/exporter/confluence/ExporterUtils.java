package com.miniorange.exporter.confluence;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

public class ExporterUtils {
    public static String sanitizeText(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        text = Jsoup.parse(text).text();
        return text;
    }

}
