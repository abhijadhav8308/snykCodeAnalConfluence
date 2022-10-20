<html>
<head>
    <link href="https://fonts.googleapis.com/css2?family=Inter" rel="stylesheet"></link>
    <style>
        <#include "MasterCss.css">
        <#include "TemplateStyle.css">
    </style>
</head>
<body>
<div class="pdf-header">
    <p>  ${spaceName}-${pageTitle} </p>
</div>

<p class="header-title">
    <span style="color:#bbbbbb !important;padding-right: 0.15cm;">${index}</span> ${headerTitle}
</p>
<div class="ak-renderer-document">
    ${headerContent}
</div>
</body>
</html>