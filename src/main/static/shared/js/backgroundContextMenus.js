
var MENU_ID = "WordhubQuery";

chrome.runtime.onInstalled.addListener(function () {
    chrome.contextMenus.create({
        "id": MENU_ID,
        "title": "DictHub Query",
        "contexts": ["selection"]
    });
    chrome.contextMenus.onClicked.addListener(function(info, tab) {
        if (info.menuItemId === MENU_ID) {
            chrome.tabs.query({active: true, currentWindow: true}, function (tabs) {
                chrome.tabs.sendMessage(tabs[0].id, {"action": "showContextMenuContainer"});
            });
        }
    });
});