
chrome.runtime.onMessage.addListener(
    function (request, sender) {
        if (request.action === "showContextMenuContainer") {
            addContextMenuContainer();
        }
    });

function removeContextMenuContainer() {
    var contextMenuContainer = document.getElementById("contextMenuContainer");
    if (contextMenuContainer != null) {
        contextMenuContainer.outerHTML = "";
    }
}

var popupWidth = 320;
var popupHeight = 300;

function addContextMenuContainer() {

    removeContextMenuContainer();

    var jsonStr = JSON.stringify({
        "selection": window.getSelection().toString(),
        "fromLang": document.documentElement.lang,
        "toLang": null
    });

    var browser = browser || chrome; // Firefox: browser, Chrome: chrome

    var frameUrl = browser.runtime.getURL("overlay.html") + "#" + encodeURIComponent(jsonStr);
    var x = mouseX + popupWidth < window.innerWidth ? mouseX : window.innerWidth - popupWidth;
    var y = mouseY + popupHeight < window.innerHeight ? mouseY : window.innerHeight - popupHeight;

    var container = document.createElement('div');
    container.id = "contextMenuContainer";
    container.style.backgroundColor = "#FFFFFF";
    container.style.width = popupWidth + "px";
    container.style.height = popupHeight + "px";
    container.style.position = "fixed";
    container.style.zIndex = 1000;
    container.style.left = x + "px";
    container.style.top = y + "px";

    var iframe = document.createElement("iframe");
    iframe.src = frameUrl;
    iframe.width = "100%";
    iframe.height = "100%";
    iframe.style = "border: 1px dashed";

    container.appendChild(iframe);
    document.body.appendChild(container);
}



var heldKey = undefined;
var mouseX = 0;
var mouseY = 0;

function onClickHandler(e) {
    e = e || window.event;
    mouseX = e.clientX;
    mouseY = e.clientY;

    removeContextMenuContainer();
}

function onDoubleClickHandler(e) {
    if (heldKey === 18 && window.getSelection().toString().length > 0) {
        addContextMenuContainer()
    }
}

function onKeyDown(e) {
    heldKey = e.which || e.keyCode;
}

function onKeyUp(e) {
    heldKey = undefined;
}

if (document.attachEvent) {
    document.attachEvent('onclick', onClickHandler);
    document.attachEvent('dblclick', onDoubleClickHandler);
    document.attachEvent('keydown', onKeyDown);
    document.attachEvent('keyup', onKeyUp);
} else {
    document.addEventListener('click', onClickHandler);
    document.addEventListener('dblclick', onDoubleClickHandler);
    document.addEventListener('keydown', onKeyDown);
    document.addEventListener('keyup', onKeyUp)
}
