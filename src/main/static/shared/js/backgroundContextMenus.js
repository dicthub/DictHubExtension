
var MENU_ID = "DicthubQuery";

chrome.runtime.onInstalled.addListener(function (details) {

    if (details.reason == 'install') {
        chrome.tabs.create({ 'url': chrome.extension.getURL('welcome.html') }, function (tab) { });
    } else if (details.reason == 'update') {
        chrome.tabs.create({ 'url': 'https://dicthub.org/docs/getting-started/release/' }, function (tab) { });
    }

    chrome.contextMenus.create({
        "id": MENU_ID,
        "title": "DictHub Query",
        "contexts": ["selection"]
    });
    chrome.contextMenus.onClicked.addListener(function (info, tab) {
        if (info.menuItemId === MENU_ID) {
            chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
                chrome.tabs.sendMessage(tabs[0].id, { "action": "showContextMenuContainer" });
            });
        }
    });
});


function handleNotification(message) {
    if (message.cmd == "plugin-notification") {
        chrome.notifications.create("dicthub-notification", message.payload, function (id) {
            chrome.notifications.onClicked.addListener(function(id) {
                chrome.tabs.create({ 'url': chrome.runtime.getURL('options.html') }, function (tab) { })
            });
        });
    }
}

chrome.runtime.onMessage.addListener(handleNotification);
