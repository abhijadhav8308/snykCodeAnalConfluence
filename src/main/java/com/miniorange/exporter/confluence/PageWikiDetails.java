package com.miniorange.exporter.confluence;

import ch.qos.logback.classic.Logger;
import com.atlassian.connect.spring.AtlassianHostRestClients;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class PageWikiDetails {
    private final Logger LOGGER = (Logger) LoggerFactory.getLogger(this.getClass());
    private final String jwt;
    private final String format;
    private final String spaceKey;
    private final String baseUrl;
    private final String fields;
    private final boolean requestForPageTitle;
    private final boolean requestForAttachments;
    private final String spaceName;
    private String scope;
    private boolean ebdlinksPages;
    private String pageTitle;
    private String pageId;
    private boolean requestForComments;
    private boolean requestForCreator;
    private String outputTemplate;
    @Autowired
    private AtlassianHostRestClients atlassianHostRestClients;
    private List<Integer> headerIndices;

    public PageWikiDetails(String fields, String jwt, String pageTitle, String format, String pageId, String spaceKey, String baseUrl, String spaceName) {

        this.requestForComments = fields.contains("comments");
        this.requestForCreator = fields.contains("creator");
        this.requestForPageTitle = fields.contains("pageTitle");
        this.requestForAttachments = fields.contains("attachments");
        this.ebdlinksPages = fields.contains("pageEbdLinks");
        this.outputTemplate = fields.contains("defaultPageLayout") ? "defaultPageLayout" : fields.contains("documentationTemplate") ? "documentationTemplate" : "defaultPageLayout";
        this.scope = fields.contains("singlePage") ? "singlePage" : fields.contains("pageChildren") ? "pageChildren" : "singlePage";
        this.jwt = jwt;
        this.pageTitle = pageTitle;
        this.format = format;
        this.pageId = pageId;
        this.spaceKey = spaceKey;
        this.baseUrl = baseUrl;
        this.fields = fields;
        this.spaceName = spaceName.substring(1, spaceName.length() - 1);
    }

    public PageWikiDetails(PageWikiDetails pageWikiDetails, String childPageID, String pageTitle, String spaceName, String spaceKey) {
        String fields = pageWikiDetails.getFields();
        this.requestForComments = pageWikiDetails.requestForComments;
        this.requestForCreator = pageWikiDetails.requestForCreator;
        this.requestForPageTitle = pageWikiDetails.requestForPageTitle;
        this.requestForAttachments = pageWikiDetails.requestForAttachments;
        this.ebdlinksPages = pageWikiDetails.ebdlinksPages;
        this.outputTemplate = "defaultPageLayout";
        this.scope = "singlePage";
        this.jwt = pageWikiDetails.getJwt();
        this.fields = fields;
        this.format = pageWikiDetails.getFormat();
        this.baseUrl = pageWikiDetails.getBaseUrl();
        this.pageTitle = pageTitle;
        this.pageId = childPageID;
        this.spaceKey = spaceName;
        this.spaceName = spaceKey;
    }

    public PageWikiDetails(PageWikiDetails pageWikiDetails) {

        this.requestForComments = pageWikiDetails.requestForComments;
        this.requestForCreator = pageWikiDetails.requestForCreator;
        this.requestForPageTitle = pageWikiDetails.requestForPageTitle;
        this.requestForAttachments = pageWikiDetails.requestForAttachments;
        this.ebdlinksPages = pageWikiDetails.ebdlinksPages;
        this.outputTemplate = pageWikiDetails.outputTemplate;
        this.scope = pageWikiDetails.scope;
        this.jwt = pageWikiDetails.jwt;
        this.pageTitle = pageWikiDetails.pageTitle;
        this.format = pageWikiDetails.format;
        this.pageId = pageWikiDetails.pageId;
        this.spaceKey = pageWikiDetails.spaceKey;
        this.baseUrl = pageWikiDetails.baseUrl;
        this.fields = pageWikiDetails.fields;
        this.spaceName = pageWikiDetails.spaceName;

    }

    public List<Integer> getHeaderIndices() {
        return headerIndices;
    }

    public void setHeaderIndices(PageWikiDetails pageWikiDetails, List<Integer> headerPageLength) {
        List<Integer> headerIndices = new ArrayList<>();
        int firstPageIndex = 3, currentIndex = 3;
        headerIndices.add(firstPageIndex);
        for (Integer pageSize : headerPageLength) {
            headerIndices.add(currentIndex + pageSize);
            currentIndex += pageSize;
        }
        if (pageWikiDetails.isRequestForComments()) {
            headerIndices.add(currentIndex);
        }
        this.headerIndices = headerIndices;
    }

    public boolean isRequestForComments() {
        return requestForComments;
    }

    public void setRequestForComments(boolean requestForComments) {
        this.requestForComments = requestForComments;
    }

    public boolean isRequestForCreator() {
        return requestForCreator;
    }

    public void setRequestForCreator(boolean requestForComments) {
        this.requestForCreator = requestForComments;
    }

    public String getJwt() {
        return jwt;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getFormat() {
        return format;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getFields() {
        return fields;
    }

    public boolean isRequestForPageTitle() {
        return requestForPageTitle;
    }

    public boolean isRequestForAttachments() {
        return requestForAttachments;
    }

    public String getOutputTemplate() {
        return outputTemplate;
    }

    public void setOutputTemplate(String outputTemplate) {
        this.outputTemplate = outputTemplate;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isEbdlinksPages() {
        return ebdlinksPages;
    }

    public void setEbdlinksPages(boolean ebdlinksPages) {
        this.ebdlinksPages = ebdlinksPages;
    }
}
