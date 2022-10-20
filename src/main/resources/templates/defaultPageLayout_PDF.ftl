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
<br/>
<div class="ak-renderer-document">
    ${contentBody}
</div>
</body>
</html>