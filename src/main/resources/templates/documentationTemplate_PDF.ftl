<html>
<head>
    <title>${pageTitle}</title>
    <style>
        <#include "TemplateStyle.css">
        <#include "MasterCss.css">
    </style>
</head>
<body>
<div id="page-title" style="width: 18cm;height: 27cm;text-align:center;
    display: flex;
    align-items: center;">
    <#if requestForPageTitle==true>
        <h1 id="title-text" data-test-id="title-text" data-testid="title-text"
            style="color: rgb(23, 43, 77); vertical-align: middle;padding-top: 12cm;font-size:3em;"
            class="css-1agkp1r e1vqopgf1">
            ${pageTitle}
        </h1>
        <div style="color: #6b778c; font-size: 2em;">${spaceName}</div>
    </#if>

    <#if requestForCreator==true>
        <div style="font-size: 2em;">
            <p>${creator}</p>
        </div>
    </#if>
</div>
<br/>
<div id="page-title" style="width: 18cm;height: 26cm;">
    <h1 id="title-text" data-test-id="title-text" data-testid="title-text"
        style="color: rgb(23, 43, 77);font-size:2em;"
        class="css-1agkp1r e1vqopgf1">Table of Contents
    </h1>
    <#assign index_num = 1>
    <#list   ConfluenceHeaders as ConfluenceHeader >
        <div>
            <div style="width: 0.5cm; display: inline-block">${index_num}</div>
            <div style="width: 16cm; display: inline-block">${ConfluenceHeader}</div>
            <div style="width: 0.5cm; display: inline-block">${index_num}</div>
        </div>
        <#assign index_num++>
    </#list>
</div>
<br/>
<div class="ak-renderer-document">
    ${contentBody}
</div>
</body>
</html>