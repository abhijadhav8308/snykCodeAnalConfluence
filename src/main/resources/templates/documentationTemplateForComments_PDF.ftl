<html>
<head>
    <title>${pageTitle}</title>
    <style>
        <#include "TemplateStyle.css">
        <#include "MasterCss.css">
    </style>
</head>
<body>
<div class="pdf-header">
    <p>  ${spaceName}-${pageTitle} </p>
</div>
<#if comments?size gt 0 >
    <#if requestForComments==true >
        <div id="comments-section" data-test-id="comments-section-fabric-new" class="css-c84d6x e1xamcc80">
            <pre style="font-weight: 600; font-size: 26px;padding-left: 8px;">Comments</pre>
            <div style="border-bottom: 1px solid #efefef"></div>
            <#list comments as comment>
                <div class="CommentBox">
                    <div style="display: inline;"><img style="width: 35px; height: 35px;"
                                                       src="${comment.getAuthorThumbnailPath()}" alt=""></img></div>
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