package com.miniorange.exporter.confluence.Controllers;

import ch.qos.logback.classic.Logger;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import com.atlassian.connect.spring.AtlassianHostUser;
import com.atlassian.connect.spring.ContextJwt;
import com.fasterxml.jackson.databind.JsonNode;
import com.miniorange.exporter.confluence.MoConfluenceComment;
import com.miniorange.exporter.confluence.PageWikiDetails;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlOptions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class DownloadConfluenceDocument {

    private final Logger LOGGER = (Logger) LoggerFactory.getLogger(this.getClass());
    @Value("${addon.base-url}")
    private String addonBaseUrl;
    @Value("${server.servlet.context-path}")
    private String moContextPath;
    @Autowired
    private AtlassianHostRestClients atlassianHostRestClients;

    @Value("${addon.key}")
    private String addonKey;

    private static void setDownloadHeader(HttpServletResponse response, String filename, String mimeType) {
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "must-revalidate, no-transform");
        response.setDateHeader("Expires", 0L);
        response.setContentType(mimeType);
    }

    private static void appendBody(CTBody src, CTBody append) throws Exception {
        XmlOptions optionsOuter = new XmlOptions();
        optionsOuter.setSaveOuter();
        String appendString = append.xmlText(optionsOuter);
        String srcString = src.xmlText();
        String prefix = srcString.substring(0, srcString.indexOf(">") + 1);
        String mainPart = srcString.substring(srcString.indexOf(">") + 1, srcString.lastIndexOf("<"));
        String sufix = srcString.substring(srcString.lastIndexOf("<"));
        String addPart = appendString.substring(appendString.indexOf(">") + 1, appendString.lastIndexOf("<"));
        CTBody makeBody = CTBody.Factory.parse(prefix + mainPart + addPart + sufix);
        src.set(makeBody);
    }

    public void mergeWord(InputStream src1, InputStream src2, OutputStream dest) {

        try {
            OPCPackage src1Package = OPCPackage.open(src1);
            OPCPackage src2Package = OPCPackage.open(src2);
            XWPFDocument src1Document = new XWPFDocument(src1Package);
            CTBody src1Body = src1Document.getDocument().getBody();
            XWPFDocument src2Document = new XWPFDocument(src2Package);
            CTBody src2Body = src2Document.getDocument().getBody();
            appendBody(src1Body, src2Body);
            src1Document.write(dest);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/getWikiPageTitle", method = RequestMethod.GET, produces = "text/html")
    @ResponseBody
    @ContextJwt
    public ResponseEntity<String> getWikiPageTitle(@AuthenticationPrincipal AtlassianHostUser hostUser, @RequestParam("pageId") String pageId) {
        return new ResponseEntity<>(getPageTitle(hostUser,pageId), HttpStatus.OK);
    }

    public String getPageTitle(AtlassianHostUser hostUser,String pageId) {
        LOGGER.debug("Fetching Page title.");
        String host_url = hostUser.getHost().getBaseUrl();
        JsonNode pageJson = null;
        if (pageId != null && !pageId.equals("") && !pageId.equals("null")) {
            pageJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(host_url + "/rest/api/content/" + pageId, JsonNode.class);
        }
        String pageTitle;
        if (pageJson != null) {
            LOGGER.debug("Page Title received");
            pageTitle = pageJson.get("title").asText();
        } else {
            LOGGER.debug("NO PAGE TITLE FOUND ! returning page ID");
            pageTitle = pageId;
        }
        return pageTitle;
    }

    @RequestMapping(value = "/downloadConfluenceDocument", method = RequestMethod.GET)
    @ResponseBody
    @ContextJwt
    public String downloadConfluenceDocument(HttpServletResponse response, @AuthenticationPrincipal AtlassianHostUser hostUser, @RequestParam("pageId") String pageId, @RequestParam("spaceKey") String spaceKey, @RequestParam("jwt") String jwt, @RequestParam("format") String format, @RequestParam("fields") String fields) {
        try {
            LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": preparing to create " + format + " Document....");

            new File("tempFiles/").mkdirs();
            XRLog.setLevel(XRLog.CSS_PARSE, Level.OFF);
            PageWikiDetails pageWikiDetails = getPageWikiDetails(hostUser, pageId, spaceKey, hostUser.getHost().getBaseUrl(), jwt, fields, format);
            switch (format) {
                case "pdf":
                    return downloadPDF(response, hostUser, pageWikiDetails);
                case "word":
                    if (!isLicenseActive(hostUser.getHost().getBaseUrl())) {
                        return getLicenseErrorHtml();
                    }
                    downloadWord(response, hostUser, Objects.requireNonNull(pageWikiDetails));
                    break;
                default:
                    LOGGER.error("ILLEGAL FORMAT RECEIVED");
                    break;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        clearFiles();
        return "success";
    }

    private void clearFiles() {
        File directory = new File("tempFiles/");
        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private PageWikiDetails getPageWikiDetails(@AuthenticationPrincipal AtlassianHostUser hostUser, String pageId, String spaceKey, String baseUrl, String jwt, String fields, String format) {
        try {
            LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": getting page wiki details..");
            JsonNode jsonNode = atlassianHostRestClients.authenticatedAsAddon().getForObject(baseUrl + "/rest/api/space/" + spaceKey, JsonNode.class);
            return new PageWikiDetails(fields, jwt, getPageTitle(hostUser, pageId), format, pageId, spaceKey, baseUrl, String.valueOf(Objects.requireNonNull(jsonNode).get("name")));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private void downloadWord(HttpServletResponse response, AtlassianHostUser hostUser, PageWikiDetails pageWikiDetails) {

        FileInputStream fin = null;
        ServletOutputStream outStream = null;
        File finalFile = null;
        try {
            finalFile = getFileWord(pageWikiDetails, hostUser);
            fin = new FileInputStream(finalFile.getAbsolutePath());
            setDownloadHeader(response, pageWikiDetails.getPageTitle() + ".doc", "application/msword");

            int len;
            byte[] buf = new byte[1024];
            outStream = response.getOutputStream();
            while ((len = fin.read(buf)) != -1) {
                outStream.write(buf, 0, len);
            }
            Objects.requireNonNull(outStream).flush();
            Objects.requireNonNull(fin).close();
            Objects.requireNonNull(outStream).close();
            if (!finalFile.delete()) {
                LOGGER.error("customer: " + hostUser.getHost().getBaseUrl() + ": error while deleting final word file");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private File getFileWord(PageWikiDetails pageWikiDetails, AtlassianHostUser hostUser) throws IOException, TemplateException {
        try {
            File baseFile = new File("tempFiles/moWord" + (int) (Math.random() * 100000) + ".doc");
            Writer baseWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(baseFile.toPath()), StandardCharsets.UTF_8));
            Template bodyTemplate = getTemplate(pageWikiDetails.getBaseUrl(), "word", pageWikiDetails.getOutputTemplate());
            bodyTemplate.process(getWikiDetails(pageWikiDetails, hostUser), baseWriter);
            baseWriter.close();
            return baseFile;
        } catch (IOException | TemplateException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private File getChildrenPagesFile(PageWikiDetails OriginalPageWikiDetails, AtlassianHostUser hostUser) throws Exception {
        PageWikiDetails pageWikiDetails = new PageWikiDetails(OriginalPageWikiDetails);
        File childrenPagesFile = new File("tempFiles/childrenPageFile" + (int) (Math.random() * 100000) + ".doc");
        alterPageWikiDetailsForAppending(pageWikiDetails);
        JsonNode childrenInformation = atlassianHostRestClients.authenticatedAsAddon().getForObject(pageWikiDetails.getBaseUrl() + "/rest/api/content/" + pageWikiDetails.getPageId() + "/child?expand=page&limit=100", JsonNode.class);
        assert childrenInformation != null;
        JsonNode childrenDetails = childrenInformation.get("page").get("results");
        String childPageID;
        for (JsonNode childDetails : childrenDetails) {
            childPageID = childDetails.get("id").asText();
            pageWikiDetails.setPageId(childPageID);
            pageWikiDetails.setPageTitle(getPageTitle(hostUser, childPageID));
            File temp = getFileWord(pageWikiDetails, hostUser);
            temp.createNewFile();
            childrenPagesFile = appendWordFile(childrenPagesFile, temp);

        }
        return childrenPagesFile;
    }

    private File appendWordFile(File originalFile, File toAppendFile) throws Exception {

        File appendedFile = new File("tempFiles/appendedFile" + (int) (Math.random() * 100000) + ".doc");
        ByteArrayOutputStream dest = new ByteArrayOutputStream();

        FileInputStream fl = new FileInputStream(originalFile);
        byte[] originalFileArr = new byte[(int) originalFile.length()];
        fl.read(originalFileArr);
        fl.close();

        FileInputStream f2 = new FileInputStream(toAppendFile);
        byte[] toAppendFileArr = new byte[(int) toAppendFile.length()];
        f2.read(toAppendFileArr);
        f2.close();

        if (originalFileArr.length == 0) {
            return toAppendFile;
        }
        if (toAppendFileArr.length == 0) {
            return originalFile;
        }
        mergeWord(new ByteArrayInputStream(originalFileArr), new ByteArrayInputStream(toAppendFileArr), dest);
        FileOutputStream fos = new FileOutputStream(appendedFile);
        dest.writeTo(fos);
        return appendedFile;
    }

    private File getEbdPagesFile(PageWikiDetails pageWikiDetails, String htmlOfPage, AtlassianHostUser hostUser) throws Exception {
        Elements ebdPagesAnchors = getEbdLinksPageAnchors(htmlOfPage);
        File ebdPagesFile = new File("tempFiles/ebdPageFile" + (int) (Math.random() * 100000) + ".doc");
        alterPageWikiDetailsForAppending(pageWikiDetails);
        String ebdPageID;
        for (Element anchor : ebdPagesAnchors) {
            ebdPageID = StringUtils.substringBetween(anchor.toString(), "data-linked-resource-id=\"", "\"");
            pageWikiDetails.setPageId(ebdPageID);
            File temp = getFileWord(pageWikiDetails, hostUser);
            ebdPagesFile = appendWordFile(ebdPagesFile, temp);
        }
        return ebdPagesFile;
    }

    private String downloadPDF(HttpServletResponse response, AtlassianHostUser hostUser, PageWikiDetails pageWikiDetails) {
        try {
            LOGGER.debug("customer: " + pageWikiDetails.getBaseUrl() + ":proceeding to build PDF.");
            byte[] pdfData = getPdfData(hostUser, pageWikiDetails);
            setDownloadHeader(response, pageWikiDetails.getPageTitle() + ".pdf", "application/pdf");
            response.getOutputStream().write(pdfData);
            response.getOutputStream().close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return response.toString();
    }

    private byte[] getPdfData(AtlassianHostUser hostUser, PageWikiDetails pageWikiDetails) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ":loading Outpi");

        try {
            loadOutputStream(hostUser, outputStream, pageWikiDetails);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return outputStream.toByteArray();
    }

    private void loadOutputStream(AtlassianHostUser hostUser, ByteArrayOutputStream outputStream, PageWikiDetails pageWikiDetails) {
        try {

            if (!isLicenseActive(hostUser.getHost().getBaseUrl())) {
                String pageHtml = getLicenseErrorHtml();
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(pageHtml, "");
                builder.toStream(outputStream);
                builder.run();
                return;
            }
            if (Objects.equals(pageWikiDetails.getOutputTemplate(), "defaultPageLayout")) {
                renderDefaultPageLayoutTemplateForPDF(hostUser, pageWikiDetails, outputStream);
            } else if (Objects.equals(pageWikiDetails.getOutputTemplate(), "documentationTemplate")) {
                renderDocumentationTemplateForPDF(hostUser, pageWikiDetails, outputStream);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private String getLicenseErrorHtml() {
        return "<html>\n" + "<head>\n" + "</head>\n" + "<body>\n" + "<div>License Error</div>" + "</body>\n" + "</html>";
    }

    private void renderDocumentationTemplateForPDF(AtlassianHostUser hostUser, PageWikiDetails pageWikiDetails, ByteArrayOutputStream outputStream) {
        try {
            LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Rendering documentation Template");
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            String pageHtml = getHtmlOfPage(pageWikiDetails, hostUser);
            ByteArrayOutputStream bodyOutputStream = new ByteArrayOutputStream();
            loadOutputStreamForDocumentation(hostUser, pageWikiDetails, bodyOutputStream, builder);
            ByteArrayOutputStream commentOutputStream = new ByteArrayOutputStream();
            if (areCommentsPresent(hostUser, pageWikiDetails.getPageId()) && pageWikiDetails.isRequestForComments()) {
                LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Appending Comments..");
                AddCommentsIfRequired(pageWikiDetails, builder, bodyOutputStream, commentOutputStream, outputStream, hostUser);
            } else {
                outputStream.write(bodyOutputStream.toByteArray());
            }
            appendRequestedPages(hostUser, pageWikiDetails, outputStream, pageHtml);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }

    }

    void appendRequestedPages(AtlassianHostUser hostUser, PageWikiDetails pageWikiDetails, ByteArrayOutputStream outputStream, String pageHtml) {
        if (Objects.equals(pageWikiDetails.getScope(), "pageChildren")) {
            LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Appending child pages..");
            appendChildrenPages(pageWikiDetails, hostUser, outputStream);
        }

        if (pageWikiDetails.isEbdlinksPages()) {
            LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Appending Embedded links..");
            appendEbdLinksPages(pageHtml, pageWikiDetails, hostUser, outputStream);
        }
    }

    private void AddCommentsIfRequired(PageWikiDetails pageWikiDetails, PdfRendererBuilder builder, ByteArrayOutputStream bodyOutputStream, ByteArrayOutputStream commentOutputStream, ByteArrayOutputStream outputStream, AtlassianHostUser hostUser) throws IOException {
        pageWikiDetails.setOutputTemplate("documentationTemplateForComments");
        String pageHtmlComments = getHtmlOfPage(pageWikiDetails, hostUser);
        builder.withHtmlContent(pageHtmlComments, "");
        builder.toStream(commentOutputStream);
        builder.run();
        mergePDFs(bodyOutputStream, commentOutputStream, outputStream);
    }

    private void loadOutputStreamForDocumentation(AtlassianHostUser hostUser, PageWikiDetails pageWikiDetails, ByteArrayOutputStream bodyOutputStream, PdfRendererBuilder builder) {
        try {
            String contentHtml = getPageContent(pageWikiDetails.getPageId(), pageWikiDetails.getBaseUrl(), pageWikiDetails.getJwt(), pageWikiDetails.isRequestForAttachments());
            List<Integer> pagesLength = new ArrayList<>();
            List<String> htmlPages = segregateHtmlPagesByHeaders(contentHtml);
            mergeHtmlPages(hostUser, htmlPages, bodyOutputStream, builder, contentHtml, pageWikiDetails, pagesLength);
            ByteArrayOutputStream startOutputStream = new ByteArrayOutputStream();
            pageWikiDetails.setOutputTemplate("DocumentationStart");
            pageWikiDetails.setHeaderIndices(pageWikiDetails, pagesLength);
            String headerStartHtml = getHtmlOfPage(pageWikiDetails, hostUser);
            builder.withHtmlContent(headerStartHtml, "");
            builder.toStream(startOutputStream);
            builder.run();

            mergePDFs(startOutputStream, bodyOutputStream, bodyOutputStream);

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void appendEbdLinksPages(String pageHtml, PageWikiDetails originalPageWikiDetails, AtlassianHostUser hostUser, ByteArrayOutputStream outputStream) {
        try {
            PageWikiDetails pageWikiDetails = new PageWikiDetails(originalPageWikiDetails);
            alterPageWikiDetailsForAppending(pageWikiDetails);
            String ebdPageID;
            Elements childPagesAnchors = getEbdLinksPageAnchors(pageHtml);
            ByteArrayOutputStream[] childOutputStream = new ByteArrayOutputStream[childPagesAnchors.size()];
            ByteArrayOutputStream bufferOutputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            int index = 0;
            for (Element anchor : childPagesAnchors) {
                ebdPageID = StringUtils.substringBetween(anchor.toString(), "data-linked-resource-id=\"", "\"");
                if (ebdPageID != null && !ebdPageID.equals("") && !ebdPageID.equals("null")) {
                    fetchAndAppendChildPageContent(ebdPageID, childOutputStream, index, bufferOutputStream, outputStream, pageWikiDetails, hostUser, builder);
                }
                index++;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void appendChildrenPages(PageWikiDetails originalPageWikiDetails, AtlassianHostUser hostUser, ByteArrayOutputStream outputStream) {
        try {
            PageWikiDetails pageWikiDetails = new PageWikiDetails(originalPageWikiDetails);
            alterPageWikiDetailsForAppending(pageWikiDetails);
            JsonNode childrenInformation = atlassianHostRestClients.authenticatedAsAddon().getForObject(pageWikiDetails.getBaseUrl() + "/rest/api/content/" + pageWikiDetails.getPageId() + "/child?expand=page&limit=100", JsonNode.class);
            assert childrenInformation != null;
            JsonNode childrenDetails = childrenInformation.get("page").get("results");
            String childPageID;
            ByteArrayOutputStream[] childOutputStream = new ByteArrayOutputStream[childrenDetails.size()];
            ByteArrayOutputStream bufferOutputStream = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            int index = 0;
            for (JsonNode childDetails : childrenDetails) {
                childPageID = childDetails.get("id").asText();
                if (childPageID != null && !childPageID.equals("") && !childPageID.equals("null")) {
                    fetchAndAppendChildPageContent(childPageID, childOutputStream, index, bufferOutputStream, outputStream, pageWikiDetails, hostUser, builder);
                }
                index++;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void alterPageWikiDetailsForAppending(PageWikiDetails pageWikiDetails) {
        pageWikiDetails.setScope("singlePage");
        pageWikiDetails.setOutputTemplate("defaultPageLayout");
        pageWikiDetails.setRequestForComments(false);
        pageWikiDetails.setRequestForCreator(false);
        pageWikiDetails.setEbdlinksPages(false);
    }

    private void fetchAndAppendChildPageContent(String pageID, ByteArrayOutputStream[] childOutputStream, int index, ByteArrayOutputStream bufferOutputStream, ByteArrayOutputStream outputStream, PageWikiDetails pageWikiDetails, AtlassianHostUser hostUser, PdfRendererBuilder builder) throws IOException {

        if (pageID != null && !pageID.equals("") && !pageID.equals("null")) {
            childOutputStream[index] = new ByteArrayOutputStream();
            bufferOutputStream.flush();
            bufferOutputStream.reset();
            bufferOutputStream.write(outputStream.toByteArray());
            outputStream.flush();
            outputStream.reset();
            String childPageHtml = getPageContent(pageID, pageWikiDetails, hostUser);
            builder.withHtmlContent(childPageHtml, "");
            builder.toStream(childOutputStream[index]);
            builder.run();
            mergePDFs(bufferOutputStream, childOutputStream[index], outputStream);
        }
    }

    private String getPageContent(String pageID, PageWikiDetails pageWikiDetails, AtlassianHostUser hostUser) {

        if (pageID != null && !pageID.equals("") && !pageID.equals("null")) {
            JsonNode pageJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(pageWikiDetails.getBaseUrl() + "/rest/api/content/" + pageID + "?expand=space", JsonNode.class);
            PageWikiDetails childPageWikiDetails = new PageWikiDetails(pageWikiDetails, pageID, getPageTitle(hostUser, pageID), pageJson.get("space").get("name").asText(), pageJson.get("space").get("key").asText());
            return getHtmlOfPage(childPageWikiDetails, hostUser);
        }
        return "";
    }

    private void mergeHtmlPages(AtlassianHostUser hostUser, List<String> htmlPages, ByteArrayOutputStream bodyOutputStream, PdfRendererBuilder builder, String contentHtml, PageWikiDetails pageWikiDetails, List<Integer> pagesLength) throws IOException {
        try {
            ByteArrayOutputStream bufferOutputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream[] childOutputStream = new ByteArrayOutputStream[htmlPages.size()];
            int index = 0;
            builder.useFastMode();
            String processedHeaderContentHtml;
            List<String> allHeadersTitle = new ArrayList<>(getAllHeadersTitle(contentHtml, pageWikiDetails));
            for (String headerPageHtml : htmlPages) {
                processedHeaderContentHtml = getProcessedHeaderContent(hostUser, headerPageHtml, allHeadersTitle.get(index), pageWikiDetails, index + 1);
                childOutputStream[index] = new ByteArrayOutputStream();
                bufferOutputStream.flush();
                bufferOutputStream.reset();
                bufferOutputStream.write(bodyOutputStream.toByteArray());
                bodyOutputStream.flush();
                bodyOutputStream.reset();
                builder.withHtmlContent(processedHeaderContentHtml, "");
                builder.toStream(childOutputStream[index]);
                builder.run();
                String file_name = "moDocument" + (int) (Math.random() * 100000) + ".pdf";
                File file = new File(file_name);
                childOutputStream[index].writeTo(Files.newOutputStream(file.toPath()));
                PDDocument doc = PDDocument.load(file);
                Integer pageCount = doc.getNumberOfPages();
                pagesLength.add(pageCount);
                mergePDFs(bufferOutputStream, childOutputStream[index], bodyOutputStream);
                childOutputStream[index].flush();
                childOutputStream[index].reset();
                childOutputStream[index].close();
                doc.close();
                file.delete();
                index++;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private String getProcessedHeaderContent(AtlassianHostUser hostUser, String headerPageHtml, String headerTitle, PageWikiDetails pageWikiDetails, int index) {

        try {
            StringWriter stringWriter = new StringWriter();
            Map<String, Object> wikiDetailsMap = new HashMap<>();
            wikiDetailsMap.put("index", index);
            wikiDetailsMap.put("headerTitle", headerTitle);
            wikiDetailsMap.put("headerContent", headerPageHtml);
            wikiDetailsMap.put("spaceName", pageWikiDetails.getSpaceName());
            wikiDetailsMap.put("pageTitle", pageWikiDetails.getPageTitle());
            Template template = getTemplate(hostUser.getHost().getBaseUrl(), "pdf", "documentationTemplateHeaderHtml");
            LOGGER.debug("Processing Template...");
            template.process(wikiDetailsMap, stringWriter);
            return stringWriter.toString();
        } catch (TemplateException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    private List<String> segregateHtmlPagesByHeaders(String pageHtml) {
        List<String> htmlPages = new ArrayList<>();
        List<String> allHeaders = getAllHeadersTag(pageHtml);
        if (allHeaders.size() == 0) {
            htmlPages.add(pageHtml);
            return htmlPages;
        }
        Objects.requireNonNull(htmlPages).add(StringUtils.substringBetween(pageHtml, "", allHeaders.get(0)));
        for (int i = 0; i < allHeaders.size() - 1; i++) {
            Objects.requireNonNull(htmlPages).add(StringUtils.substringBetween(pageHtml, allHeaders.get(i), allHeaders.get(i + 1)));
        }
        Objects.requireNonNull(htmlPages).add(pageHtml.substring(pageHtml.indexOf(allHeaders.get(allHeaders.size() - 1)) + allHeaders.get(allHeaders.size() - 1).length()));
        return htmlPages;
    }

    private boolean areCommentsPresent(AtlassianHostUser hostUser, String pageId) {

        JsonNode commentsJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(hostUser.getHost().getBaseUrl() + "/rest/api/content/" + pageId + "/child/comment?expand=body.export_view,history&depth=all", JsonNode.class);
        return Objects.requireNonNull(commentsJson).get("size").asInt() != 0;
    }

    private void mergePDFs(ByteArrayOutputStream sourceOutputStream, ByteArrayOutputStream outputStreamToBeAppended, ByteArrayOutputStream destinationOutputStream) {
        try {
            if (sourceOutputStream.size() == 0 || outputStreamToBeAppended.size() == 0) {

                if (sourceOutputStream.size() != 0) {
                    destinationOutputStream.write(sourceOutputStream.toByteArray());
                }
                if (outputStreamToBeAppended.size() != 0) {
                    destinationOutputStream.write(outputStreamToBeAppended.toByteArray());
                }
                return;
            }

            byte[] first = sourceOutputStream.toByteArray();
            byte[] second = outputStreamToBeAppended.toByteArray();
            destinationOutputStream.flush();
            destinationOutputStream.reset();

            File f1 = new File("tempFiles/temp" + (int) (Math.random() * 100000) + ".pdf");
            File f2 = new File("tempFiles/temp" + (int) (Math.random() * 100000) + ".pdf");
            Files.write(f1.toPath(), first);
            Files.write(f2.toPath(), second);
            PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
            pdfMergerUtility.addSource(f1);
            pdfMergerUtility.addSource(f2);
            pdfMergerUtility.setDestinationStream(destinationOutputStream);
            pdfMergerUtility.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
            f1.delete();
            f2.delete();
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private void renderDefaultPageLayoutTemplateForPDF(AtlassianHostUser hostUser, PageWikiDetails pageWikiDetails, ByteArrayOutputStream outputStream) {
        LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Preparing Default page layout.");
        try {
            String pageHtml = getHtmlOfPage(pageWikiDetails, hostUser);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(pageHtml, "");
            builder.toStream(outputStream);
            builder.run();
            ByteArrayOutputStream commentOutputStream = new ByteArrayOutputStream();
            if (areCommentsPresent(hostUser, pageWikiDetails.getPageId()) && pageWikiDetails.isRequestForComments()) {
                LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Appending Comments..");
                AddCommentsIfRequired(pageWikiDetails, builder, outputStream, commentOutputStream, outputStream, hostUser);
            }

            appendRequestedPages(hostUser, pageWikiDetails, outputStream, pageHtml);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private Elements getEbdLinksPageAnchors(String pageHtml) {
        Document document = Jsoup.parse(pageHtml);
        Elements anchors = document.getElementsByTag("a");
        Elements childPagesAnchors = new Elements();
        for (Element anchor : anchors) {
            if (Objects.equals(StringUtils.substringBetween(anchor.toString(), "data-linked-resource-type=\"", "\""), "page")) {
                childPagesAnchors.add(anchor);
            }
        }
        return childPagesAnchors;
    }

    private String getHtmlOfPage(PageWikiDetails pageWikiDetails, AtlassianHostUser hostUser) {
        LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Getting HTML template of the page");
        try {
            StringWriter stringWriter = new StringWriter();
            Map<String, Object> wikiDetailsMap = getWikiDetails(pageWikiDetails, hostUser);
            Template template = getTemplate(pageWikiDetails.getBaseUrl(), pageWikiDetails.getFormat(), pageWikiDetails.getOutputTemplate());
            template.process(wikiDetailsMap, stringWriter);
            return stringWriter.toString();
        } catch (TemplateException | IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return "ERROR";
    }

    private Map<String, Object> getWikiDetails(PageWikiDetails pageWikiDetails, AtlassianHostUser hostUser) {
        LOGGER.debug("customer: " + hostUser.getHost().getBaseUrl() + ": Getting all wiki details!");
        Map<String, Object> input = new HashMap<>();
        try {
            String creator = getCreator(pageWikiDetails.getPageId(), pageWikiDetails.getBaseUrl());
            String contentHtmlBody = getPageContent(pageWikiDetails.getPageId(), pageWikiDetails.getBaseUrl(), pageWikiDetails.getJwt(), pageWikiDetails.isRequestForAttachments());
            List<MoConfluenceComment> comments = getPageComments(pageWikiDetails.getPageId(), pageWikiDetails.getBaseUrl(), input, pageWikiDetails.getJwt(), pageWikiDetails.isRequestForAttachments());
            input.put("baseUrl", pageWikiDetails.getBaseUrl());
            input.put("spaceName", pageWikiDetails.getSpaceName());
            input.put("pageTitle", pageWikiDetails.getPageTitle());
            input.put("creator", creator);
            input.put("contentBody", contentHtmlBody);
            input.put("ConfluenceHeaders", getAllHeadersTitle(contentHtmlBody, pageWikiDetails));
            input.put("comments", comments);
            input.put("requestForCreator", pageWikiDetails.isRequestForCreator());
            input.put("requestForComments", pageWikiDetails.isRequestForComments());
            input.put("requestForPageTitle", pageWikiDetails.isRequestForPageTitle());
            input.put("requestForAttachments", pageWikiDetails.isRequestForAttachments());
            input.put("pageIndices", pageWikiDetails.getHeaderIndices());
            input.put("childrenContent", Objects.equals(pageWikiDetails.getScope(), "pageChildren") ? getChildrenBodyHtml(pageWikiDetails, hostUser) : new ArrayList<>());
            input.put("ebdLinksContent", pageWikiDetails.isEbdlinksPages() ? getEbdLinksBodyHtml(contentHtmlBody, pageWikiDetails, hostUser) : new ArrayList<>());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return input;
    }

    private List<String> getAllHeadersTitle(String contentHtmlBody, PageWikiDetails pageWikiDetails) {
        Document document = Jsoup.parse(contentHtmlBody);
        Elements anchors = document.getElementsByTag("h1");
        List<String> allHeaders = new ArrayList<>();
        if (anchors.size() == 0) {
            allHeaders.add(pageWikiDetails.getPageTitle());
            return allHeaders;
        }

        if (!Objects.equals(StringUtils.substringBetween(contentHtmlBody, "", anchors.get(0).toString()), "")) {
            allHeaders.add(pageWikiDetails.getPageTitle());
        }
        for (Element anchor : anchors) {
            allHeaders.add(anchor.html());
        }
        return allHeaders;
    }

    private List<String> getAllHeadersTag(String contentHtmlBody) {
        Document document = Jsoup.parse(contentHtmlBody);
        Elements anchors = document.getElementsByTag("h1");
        List<String> allHeaders = new ArrayList<>();
        for (Element anchor : anchors) {
            if (Objects.equals(anchor.parentNode().nodeName(), "body")) {
                allHeaders.add(anchor.toString());
            }
        }
        return allHeaders;
    }

    private List<Pair<String, String>> getEbdLinksBodyHtml(String pageHtml, PageWikiDetails OGpageWikiDetails, AtlassianHostUser hostUser) {
        PageWikiDetails pageWikiDetails = new PageWikiDetails(OGpageWikiDetails);
        alterPageWikiDetailsForAppending(pageWikiDetails);
        List<Pair<String, String>> ebdLinksBodyHtml = new ArrayList<>();
        Elements childPagesAnchors = getEbdLinksPageAnchors(pageHtml);
        for (Element anchor : childPagesAnchors) {
            String childPageID = StringUtils.substringBetween(anchor.toString(), "data-linked-resource-id=\"", "\"");
            String childPageHtml = getPageContent(childPageID, pageWikiDetails, hostUser);
            ebdLinksBodyHtml.add(new Pair<>(getPageTitle(hostUser, childPageID), StringUtils.substringBetween(childPageHtml, "<body>", "</body>")));
        }
        return ebdLinksBodyHtml;
    }

    private List<Pair<String, String>> getChildrenBodyHtml(PageWikiDetails OGpageWikiDetails, AtlassianHostUser hostUser) {
        PageWikiDetails pageWikiDetails = new PageWikiDetails(OGpageWikiDetails);
        alterPageWikiDetailsForAppending(pageWikiDetails);
        List<Pair<String, String>> childrenBodyHtml = new ArrayList<>();
        JsonNode childrenInformation = atlassianHostRestClients.authenticatedAsAddon().getForObject(pageWikiDetails.getBaseUrl() + "/rest/api/content/" + pageWikiDetails.getPageId() + "/child?expand=page&limit=100", JsonNode.class);
        JsonNode childrenDetails = Objects.requireNonNull(childrenInformation).get("page").get("results");
        String childPageID;
        for (JsonNode childDetails : childrenDetails) {
            childPageID = childDetails.get("id").asText();
            String childPageHtml = getPageContent(childPageID, pageWikiDetails, hostUser);
            childrenBodyHtml.add(new Pair<>(getPageTitle(hostUser, childPageID), StringUtils.substringBetween(childPageHtml, "<body>", "</body>")));
        }
        return childrenBodyHtml;
    }

    private String getCreator(String pageId, String host_url) {
        try {

            LOGGER.debug("customer: " + host_url + ": Getting creator of the page");
            JsonNode pageJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(host_url + "/rest/api/content/" + pageId, JsonNode.class);
            if (pageJson != null) {
                return pageJson.get("history").get("createdBy").get("displayName").asText();
            }
        } catch (RestClientException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        LOGGER.debug("NO creator FOUND !");
        return "";
    }

    private List<MoConfluenceComment> getPageComments(String pageId, String host_url, Map<String, Object> input, String jwt, boolean requestForAttachments) {
        LOGGER.debug("customer: " + host_url + ": Getting page comments.");
        List<MoConfluenceComment> comments = new ArrayList<>();
        try {
            JsonNode comment;
            String commentAuthor;
            String authorThumbnailPath;
            String commentTime;
            String commentAuthorAccountId;
            JsonNode commentsJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(host_url + "/rest/api/content/" + pageId + "/child/comment?expand=body.export_view,history&depth=all", JsonNode.class);
            if (commentsJson != null) {
                JsonNode commentsJsonArray = commentsJson.get("results");
                int numberOfComments = commentsJson.get("size").asInt();
                input.put("numberOfComments", numberOfComments);
                for (int i = 0; i < numberOfComments; i++) {
                    comment = commentsJsonArray.get(i);
                    commentAuthor = String.valueOf(comment.get("history").get("createdBy").get("displayName").asText());
                    authorThumbnailPath = host_url.replace("/wiki", "") + comment.get("history").get("createdBy").get("profilePicture").get("path").asText();
                    commentTime = String.valueOf(comment.get("history").get("createdDate").asText());
                    commentAuthorAccountId = String.valueOf(comment.get("history").get("createdBy").get("accountId").asText());
                    comments.add(new MoConfluenceComment(commentAuthor, commentTime, StringUtils.substringBetween(parseHtml(fixImageAttachment(comment.get("body").get("export_view").get("value").asText(), host_url, pageId, jwt, requestForAttachments)), "<body>", "</body>"), authorThumbnailPath, commentAuthorAccountId));
                }
            } else {
                LOGGER.error("pageJson received is Empty!");
            }
        } catch (RestClientException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return comments;
    }

    private String getPageContent(String pageId, String host_url, String jwt, boolean requestForAttachments) {
        LOGGER.debug("Getting page content");
        try {
            String html = null;
            JsonNode pageJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(host_url + "/rest/api/content/" + pageId + "?expand=body.export_view", JsonNode.class);
            if (pageJson != null) {
                html = String.valueOf(pageJson.get("body").get("export_view").get("value"));
                html = parseHtml(html);
            } else {
                LOGGER.error("pageJson received is Empty!");
            }
            return StringUtils.substringBetween(parseHtml(fixImageAttachment(html, host_url, pageId, jwt, requestForAttachments)), "<body>", "</body>");
        } catch (RestClientException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return "ERROR";
    }

    private String parseHtml(String html) {

        if (html.charAt(0) == '"') {
            html = html.substring(1, html.length() - 1);
        }
        html = replaceLineBreak(html);
        html = html.replace("\\", "");
        html = html.replaceAll(" /", "");
        html = html.replaceAll("&nbsp;", "");
        html = html.replaceAll("nbsp;", "");
        html = html.replaceAll("<!--.*?-->", "");
        html = html.replaceAll("\\*\\*([^\\*]|\\*(?!\\/)).*?\\*\\*\\/", "");
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        html = doc.html();
        Elements allElements = doc.getAllElements();
        for (Element element : allElements) {
            html = html.replaceAll(element.id(), element.id().replaceAll("<", "_lt_").replaceAll(">", "_gt_"));
        }
        html = html.replaceAll("<\\s*([^\\s>]+)([^>]*)/\\s*>", "<$1$2></$1>");
        html = html.replaceAll("&nbsp;", "&#160;");
        html = html.replaceAll("’", "'");
        return html;
    }

    private String replaceLineBreak(String html) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < html.length(); i++) {
            if (html.charAt(i) == '\\') {
                if (html.charAt(i + 1) != 'n') {
                    result.append(html.charAt(i));
                } else {
                    i++;
                }
            } else {
                result.append(html.charAt(i));
            }
        }
        return result.toString();
    }

    private String fixImageAttachment(String html, String host_url, String pageId, String jwt, boolean requestForAttachments) {
        // logger.debug("customer: " + hostUrl + ": Fixing the attachments");
        Document document = Jsoup.parse(html);
        if (requestForAttachments) {
            Elements images = document.getElementsByTag("img");
            for (Element img : images) {
                if (img.toString().contains("emoji")) continue;
                String imgSrc = img.attr("src");
                Pattern pattern = Pattern.compile(StringUtils.trimToEmpty("(.*)/download/attachments/([0-9]*)/(.*)\\?(.*).*"));
                Matcher matcher = pattern.matcher(imgSrc);
                String attachmentTitle = StringUtils.EMPTY;
                if (matcher.find()) {
                    attachmentTitle = matcher.group(3);
                }
                attachmentTitle = attachmentTitle.replace("/wiki/download/attachments/", "");
                if (StringUtils.isNotBlank(attachmentTitle)) {
                    img.attr("src", addonBaseUrl + moContextPath + "/attachment?attachmentTitle=" + attachmentTitle + "&hostBaseUrl=" + host_url + "&pageId=" + pageId + "&jwt=" + jwt);
                }
            }
        }
        return document.toString();
    }

    public Template getTemplate(String host_url, String format, String templateType) {
        try {
            LOGGER.debug("customer: " + host_url + ": Processing the Template.");
            Configuration configuration = new Configuration((new Version("2.3.23")));
            configuration.setDefaultEncoding("UTF-8");
            configuration.setClassicCompatible(true);
            configuration.setClassForTemplateLoading(showExportPanel.class, "/templates");
            if (Objects.equals(format, "pdf")) {
                return configuration.getTemplate(templateType + "_PDF.ftl");
            } else if (Objects.equals(format, "word")) {
                return configuration.getTemplate(templateType + "_WORD.ftl");
            }

        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return null;
    }

    public boolean isLicenseActive(String hostBaseUrl) {
        JsonNode licensingJson = atlassianHostRestClients.authenticatedAsAddon().getForObject(hostBaseUrl + "/rest/atlassian-connect/1/addons/" + addonKey, JsonNode.class);
        if (licensingJson != null && licensingJson.get("license") != null)
            return BooleanUtils.toBoolean(licensingJson.get("license").get("active").asText());
        return false;
    }
}
