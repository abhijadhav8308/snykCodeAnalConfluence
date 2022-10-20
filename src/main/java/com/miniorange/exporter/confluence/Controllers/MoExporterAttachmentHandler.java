package com.miniorange.exporter.confluence.Controllers;

import ch.qos.logback.classic.Logger;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.ContextJwt;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class MoExporterAttachmentHandler {

    private final Logger LOGGER = (Logger) LoggerFactory.getLogger(this.getClass());
    @Autowired
    private AtlassianHostRestClients atlassianHostRestClients;

    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[8192];
        while (true) {
            int r = inputStream.read(buf);
            if (r == -1) return;
            outputStream.write(buf, 0, r);
        }
    }

    @RequestMapping(value = "/attachment", method = GET)
    @ResponseBody
    @ContextJwt
    public String showAttachmentContent(@RequestParam("attachmentTitle") String attachmentTitle, @RequestParam("hostBaseUrl") String hostBaseUrl, @RequestParam("pageId") String pageId, HttpServletResponse response) {
        try {
            JsonNode attachmentMetadataJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(hostBaseUrl + "/rest/api/content/" + pageId + "/child/attachment/", JsonNode.class);
            JsonNode results = Objects.requireNonNull(attachmentMetadataJson).get("results");
            String attachmentId = "";
            String mimeType = "";
            for (int i = 0; i < results.size(); i++) {
                JsonNode attachment = results.get(i);
                if (Objects.equals(attachment.get("title").asText(), attachmentTitle)) {
                    attachmentId = attachment.get("id").asText();
                    mimeType = attachment.get("metadata").get("mediaType").asText();
                    break;
                }
            }
            String contentUrl = hostBaseUrl + "/rest/api/content/" + pageId + "/child/attachment/" + attachmentId + "/download";
            response.setContentType(mimeType);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename= " + attachmentTitle);
            response.getOutputStream().write(provideContent(contentUrl).toByteArray());
            response.getOutputStream().close();
            return response.toString();
        } catch (RestClientException | IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return null;
    }

    private ByteArrayOutputStream provideContent(String contentUrl) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        RequestCallback requestCallback = request -> request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        ResponseExtractor<Void> responseExtractor = response -> {
            copy(response.getBody(), byteArrayOutputStream);
            return null;
        };
        atlassianHostRestClients.authenticatedAsAddon().execute(contentUrl, HttpMethod.GET, requestCallback, responseExtractor);
        return byteArrayOutputStream;
    }

}