let pageId, spaceKey;
var moContextPath;
$(window).on('load', async function () {
    loadMoExporterSetting();
    while (AP == null) {
        console.log(" AP NOT FOUND YET...");
    }
    moContextPath = AJS.$("#moContextPath").val();
    setTimeout(removeLoader, 500);
    AJS.$(document).on("click", "#PDFFormat", async function (e) {
        document.getElementById("PDFFormat").checked = true;
        document.getElementById("WordFormat").checked = false;
    });
    AJS.$(document).on("click", "#WordFormat", function (e) {
        document.getElementById("WordFormat").checked = true;
        document.getElementById("PDFFormat").checked = false;
    });
    AJS.$(document).on("click", "#export-button", async function (e) {
        try {
            if (document.getElementById("PDFFormat").checked) {
                downloadDocument("pdf");
            } else if (document.getElementById("WordFormat").checked) {
                downloadDocument("word");
            } else {
                console.log("invalid format");
            }
        } catch (e) {
            console.log(e);
        }

    });
    AJS.$(document).on("click", "#defaultPageLayout", async function (e) {
        document.getElementById("defaultPageLayout").checked = true;
        document.getElementById("documentationTemplate").checked = false;
    });
    AJS.$(document).on("click", "#documentationTemplate", async function (e) {
        document.getElementById("defaultPageLayout").checked = false;
        document.getElementById("documentationTemplate").checked = true;
    });
    AJS.$(document).on("click", "#singlePage", async function (e) {
        document.getElementById("singlePage").checked = true;
        document.getElementById("pageChildren").checked = false;
    });
    AJS.$(document).on("click", "#pageChildren", async function (e) {
        document.getElementById("singlePage").checked = false;
        document.getElementById("pageChildren").checked = true;
    });
    AJS.$(document).on("click", "#mo-close-button", async function (e) {
        setTimeout(function () {
            AP.dialog.close();
        }, 350);
    });
    AJS.$(document).on("click", "#mo-close-button-2", async function (e) {
        setTimeout(function () {
            AP.dialog.close();
        }, 350);
    });
})

function saveSettingInStorage() {
    let moExporterSettings = {
        "pageTitle": document.getElementById("pageTitle").checked,
        "creator": document.getElementById("creator").checked,
        "attachments": document.getElementById("attachments").checked,
        "comments": document.getElementById("comments").checked,
        "pageEbdLinks": document.getElementById("pageEbdLinks").checked,
        "defaultPageLayout": document.getElementById("defaultPageLayout").checked,
        "singlePage": document.getElementById("singlePage").checked,
        "PDFFormat": document.getElementById("PDFFormat").checked
    };
    localStorage.setItem("moExporterSetting", JSON.stringify(moExporterSettings));
}

function loadMoExporterSetting() {
    if (localStorage["moExporterSetting"] != null) {
        let jsonObj = JSON.parse(localStorage["moExporterSetting"]);
        document.getElementById("pageTitle").checked = jsonObj.pageTitle;
        document.getElementById("creator").checked = jsonObj.creator;
        document.getElementById("attachments").checked = jsonObj.attachments;
        document.getElementById("comments").checked = jsonObj.comments;
        document.getElementById("pageEbdLinks").checked = jsonObj.pageEbdLinks;
        document.getElementById("defaultPageLayout").checked = jsonObj.defaultPageLayout
        document.getElementById("documentationTemplate").checked = !jsonObj.defaultPageLayout;
        document.getElementById("singlePage").checked = jsonObj.singlePage;
        document.getElementById("pageChildren").checked = !jsonObj.singlePage;
        document.getElementById("PDFFormat").checked = jsonObj.PDFFormat;
        document.getElementById("WordFormat").checked = !jsonObj.PDFFormat;
    }
}

function hideExportForm() {
    setTimeout(function () {
        document.getElementById("exporter-dialog").style.display = "none";
    }, 100);
    setTimeout(function () {
        document.getElementById("exporter-loading").style.display = "flex";
    }, 100);
}

function requestDocument(pageId, spaceKey, format) {
    var fields = [];
    $.each($("input[name='formatcheckbox']:checked"), function () {
        fields.push($(this).val());
    });
    token = $('meta[name="token"]').attr("content");
    let pageTitle;
    $.ajax({
        url: moContextPath+'/getWikiPageTitle?pageId=' + pageId + "&jwt=" + token, type: "GET", success: function (data) {
            pageTitle = data;
        }, error: function (error) {
            pageTitle = pageId;
            console.log(`Error ${error}`);
        }
    });
    fetch(moContextPath+"/downloadConfluenceDocument" + "?jwt=" + token + "&pageId=" + pageId + "&spaceKey=" + spaceKey + "&format=" + format + "&fields=" + fields)
        .then(resp => resp.blob())
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            if (format === "pdf") {
                a.download = pageTitle + '.pdf';
            } else if (format === "word") {
                a.download = pageTitle + '.doc';
            }
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);

            fl.finish();

            setTimeout(function () {
                document.getElementById("exporter-dialog").style.display = "block";
                document.getElementById("exporter-loading").style.display = "none";
                AP.dialog.close();
            }, 500);
        })
        .catch(() => console.log('exception occurred while downloading!'));
}

function removeLoader() {
    jQuery("#preloader-container").fadeOut(10, function () {
        jQuery("#preloader-container").remove(), jQuery("#exporter-dialog").css({
            visibility: "visible", height: "auto", overflow: "visible"
        })
    })
}

function downloadDocument(format) {
    fl.start();
    setTimeout(function () {
        hideExportForm();
        saveSettingInStorage();
        AP.navigator.getLocation(function (ConfluenceContext) {
            pageId = ConfluenceContext.context.contentId;
            spaceKey = ConfluenceContext.context.spaceKey;
            requestDocument(pageId, spaceKey, format);
        });
    }, 350);

}

function PostLoader(options) {
    this.options = this.extend({
        stepMilliseconds: 8, splitMilliseconds: 3000, fillUpToPercentage: 100,
    }, options);

    this._interval = null;

    this.progressBarElement = document.createElement("div");
    this.progressBarElement.className = "fake-loader-progress-bar";

    this.progressIndicatorElement = document.createElement("div");
    this.progressIndicatorElement.className = "fake-loader-progress-indicator";

    this.progressBarContainerElement = document.createElement("div");
    this.progressBarContainerElement.className = "fake-loader-container";
    this.progressBarContainerElement.appendChild(this.progressBarElement);
    this.progressBarContainerElement.appendChild(this.progressIndicatorElement);

    this.containerElement = document.getElementById(options.containerId);
    this.containerElement.appendChild(this.progressBarContainerElement);
}

PostLoader.prototype.extend = function (obj1, obj2) {
    var obj3 = {};
    for (var k in obj1) obj3[k] = obj1[k];
    for (var l in obj2) obj3[l] = obj2[l];
    return obj3;
};

PostLoader.prototype.start = function () {
    var self = this;
    this.stop();
    this._progress = 0;
    this._progressLeft = this.options.fillUpToPercentage;
    this.progressBarElement.className = this.progressBarElement.className.replace(/fake-loader-active/, "") + " fake-loader-active";
    this._interval = window.setInterval(function () {
        self.step();
    }, this.options.stepMilliseconds);
    return this;
};

PostLoader.prototype.stop = function () {
    if (this._interval) {
        this.progressBarElement.className = this.progressBarElement.className.replace(/fake-loader-active/, "");
        window.clearInterval(this._interval);
        this._interval = null;
    }
    return this;
};

PostLoader.prototype.finish = function () {
    this._progress = 100;
    this.progressBarElement.style.width = this._progress + "%";
    this.progressIndicatorElement.innerHTML = Math.round(this._progress) + "%";
    this.stop();
    return this;
};

PostLoader.prototype.step = function () {
    this._progress += (this._progressLeft / 2) / (this.options.splitMilliseconds / this.options.stepMilliseconds);
    this.progressBarElement.style.width = this._progress + "%";
    this.progressIndicatorElement.innerHTML = Math.round(this._progress) + "%";
    if (this._progress >= (this.options.fillUpToPercentage - this._progressLeft / 2)) {
        this._progressLeft = this._progressLeft / 2;
    }
    return this;
};


var fl = new PostLoader({
    containerId: "loader",
});
