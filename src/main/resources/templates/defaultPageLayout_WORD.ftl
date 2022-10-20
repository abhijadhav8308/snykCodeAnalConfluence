<html>
<head>
    <title>${pageTitle}</title>
    <style>
        <#include "TemplateStyle.css">
        <#include "MasterCss.css">
    </style>
</head>
<body>
<div id="page-title">
    <#if requestForPageTitle==true>
        <h1 id="title-text" data-test-id="title-text" data-testid="title-text" style="color: rgb(23, 43, 77);"
            class="css-1agkp1r e1vqopgf1">
            ${pageTitle}
        </h1>
    </#if>
    <#if requestForCreator==true>
        <div>
            <p>Created by:- ${creator}</p>
        </div>
    </#if>
</div>
<#if requestForPageTitle==true || requestForCreator==true >
    <br/>
</#if>

<div class="ak-renderer-document">
    ${contentBody}
</div>
<br/>
<br/>
<br/>
<br/>

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
    <br/>
</#list>


<#if requestForComments==true>
    <div id="comments-section" data-test-id="comments-section-fabric-new" class="css-c84d6x e1xamcc80">


        <pre style="font-weight: 600; font-size: 14px;padding-left: 8px;">${numberOfComments} Page Comments</pre>
        <div style="border-bottom: 1px solid #efefef"></div>
        <#list comments as comment>
            <div class="CommentBox">
                <div style="margin-left: 50px; margin-top: -40px;">
                    <div class="css-wyxvz7w">
                        <p style="color: #0052CC;">${comment.getAuthor()}</p>
                    </div>
                    <div class="">
                        <p style="color: rgb(107, 119, 140);font-size: 12px;"> ${comment.getCommentDate()}  </p>
                    </div>
                    <div class="desc-con" style="margin-top: 10px">
                        ${comment.getCommentBody()}
                    </div>
                </div>
            </div>
            <br/>
            <br/>
        </#list>
    </div>
</#if>

</body>
</html>