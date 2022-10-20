<html>
<head>

    <title>${pageTitle}</title>
    <style>
        <#include "TemplateStyle.css">
        <#include "MasterCss.css">
    </style>
</head>
<body>
<div id="page-title" style="margin-top:10cm;margin-bottom:14cm;text-align:center;
    display: flex;
    align-items: center;">
    <#if requestForPageTitle==true>
        <h1 id="title-text" data-test-id="title-text" data-testid="title-text"
            style="font-family:Inter,serif; color: #000; vertical-align: middle;padding: 7cm 0 0;font-size:2em;font-weight:600;letter-spacing: -1.5px;">
            ${pageTitle}
        </h1>
        <div style="font-family: Inter,serif;color: #8b8b8b;padding: 2cm 15px 10px;font-size: 1.2em;">${spaceName}</div>
    </#if>

    <#if requestForCreator==true>
        <div style="width:100%; margin: 12cm 0 0;font-size: 2em; float:right; position: absolute; text-align:right;">
            <p>${creator}</p>
        </div>
    </#if>
</div>
<div style="margin-bottom:24cm;">
    <div class="pdf-header">
        <p>  ${spaceName}-${pageTitle} </p>
    </div>
    <p id="title-text" style="font-size: 2em;font-family: Inter,serif; color: #000; font-weight:600; margin-bottom: 1cm;">Table of
        Contents</p>
    <#assign index_num = 1>
    <#list   ConfluenceHeaders as ConfluenceHeader >
        <div class="word-content-title">
            <span class="label-content" style="font-size:16pt;">${index_num}.
            <span style="font-size:16pt;">${ConfluenceHeader}</span></span>
            <#--            <span class="page-no">${pageIndices[ConfluenceHeader?index]}</span>-->
        </div>
        <#assign index_num++>
    </#list>
    <#if requestForComments==true && (comments?size gt 0)>
        <div class="word-content-title">
            <span class="label-content" style="font-size:16pt;">${index_num}.
                <span style="font-size:16pt;">Comments</span>
                </span>
            <#--            <span class="page-no">${pageIndices[index_num-1]}</span>-->
        </div>
    </#if>
</div>
<br/>
<div class="ak-renderer-document">
    ${contentBody}
</div>

<#list childrenContent as childContent>
    <#if requestForPageTitle==true>
        <h1 id="title-text" data-test-id="title-text" data-testid="title-text" style="color: rgb(23, 43, 77);"
            class="css-1agkp1r e1vqopgf1">
        </h1>
    </#if>
    <div class="ak-renderer-document">
        ${childContent.getValue()}
    </div>
    <br/>
</#list>

<#list ebdLinksContent as ebdLinkContent>
    <#if requestForPageTitle==true>
        <h1 id="title-text" data-test-id="title-text" data-testid="title-text" style="color: rgb(23, 43, 77);"
            class="css-1agkp1r e1vqopgf1">
        </h1>
    </#if>
    <div class="ak-renderer-document">
        ${ebdLinkContent.getValue()}
    </div>
    <br/>
</#list>

<#if comments?size gt 0 >
    <#if requestForComments==true >
        <div id="comments-section" data-test-id="comments-section-fabric-new" class="css-c84d6x e1xamcc80">
            <pre style="font-weight: 600; font-size: 26px;padding-left: 8px;">Comments</pre>
            <div style="border-bottom: 1px solid #efefef"></div>
            <#list comments as comment>
                <div class="CommentBox" style="margin-bottom: 1cm;">
                    <div style="margin-left: 50px; margin-top: -40px;">
                        <div class="css-wyxvz7w">
                            <p style="color: #0052CC;">
                                <a href="${baseUrl}/people/${comment.getCommentAuthorAccountId()}?ref=confluence"
                                   target="_blank">
                                    ${comment.getAuthor()}
                                </a>
                            </p>
                        </div>
                        <div class="">
                            <p style="color: rgb(107, 119, 140);font-size: 12px;"> ${comment.getCommentDate()}  </p>
                        </div>

                        <div class="desc-con" style="margin-top: 10px">
                            ${comment.getCommentBody()}
                        </div>
                    </div>
                </div>
            </#list>
        </div>
    </#if>
</#if>
</body>
</html>