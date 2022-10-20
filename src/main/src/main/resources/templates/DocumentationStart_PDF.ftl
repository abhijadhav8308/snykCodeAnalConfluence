<html>
<head>
    <title>${pageTitle}</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter" rel="stylesheet"></link>
    <style>
        <#include "MasterCss.css">
        <#include "TemplateStyle.css">
    </style>
</head>
<body>

<#if requestForCreator==true>
    <div style="width:100%; padding: 26cm 0 0;font-size: 2em; float:right; position: absolute; text-align:right;">
        <p>${creator}</p>
    </div>
</#if>

<div id="page-title" style="width: 18cm;height: 27cm;text-align:center;">
    <#if requestForPageTitle==true>
        <h1 id="title-text" data-test-id="title-text" data-testid="title-text"
            style="font-family:Inter,serif; color: #000; vertical-align: middle;padding: 7cm 0 0;font-size:2em;font-weight:600;letter-spacing: -1.5px;">
            ${pageTitle}
        </h1>
        <span style="font-family: Inter,serif;color: #8b8b8b;padding: 2cm 15px 10px;font-size: 1.2em;border-bottom: 1.5px solid #D0D0D0 !important;">${spaceName}</span>
    </#if>
</div>
<br/>
<div>
    <div class="pdf-header">
        <p>  ${spaceName}-${pageTitle} </p>
    </div>
    <p id="title-text" style="font-size: 2em;font-family: Inter,serif; color: #000; font-weight:600;">Table of
        Contents</p>
    <#assign index_num = 1>
    <#list   ConfluenceHeaders as ConfluenceHeader >
        <div class="container-content">
            <span class="label-content">${index_num}.
            <span>${ConfluenceHeader}</span></span>

            <span class="page-no">${pageIndices[ConfluenceHeader?index]}</span>
        </div>
        <#assign index_num++>
    </#list>
    <#if requestForComments==true && (comments?size gt 0)>
        <div class="container-content">
            <span class="label-content">${index_num}.
                <span>Comments</span>
                </span>
            <span class="page-no">${pageIndices[index_num-1]}</span>
        </div>
    </#if>
</div>
<br/>
</body>
</html>