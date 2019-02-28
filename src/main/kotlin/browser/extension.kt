import kotlin.js.Json

/**
 * Chrome is any visible aspect of a browser aside from the webpages themselves
 * See also:
 *  - https://developer.chrome.com/extensions/tabs#method-executeScript
 *  - https://developer.mozilla.org/en-US/Add-ons/WebExtensions/API/tabs/executeScript
 */

fun isChrome() =
        browserObj.runtime.getURL("options.html").startsWith("chrome://")

fun isFirefox() =
        browserObj.runtime.getURL("options.html").startsWith("moz-extension://")

fun isEdge() =
        browserObj.runtime.getURL("options.html").startsWith("ms-browser-extension://")

val browserObj : Chrome = js("typeof browser === 'undefined' ? chrome : browser;")

private external val chrome: Chrome?

private external val browser: Chrome?


fun i18nMessage(messageName: String) = browserObj.i18n.getMessage(messageName)

external class Chrome {
    val tabs: ChromeTabs
    val storage: ChromeStorage
    val i18n: ChromeI18N
    val runtime: Runtime
}

external class ChromeTabs {
    fun detectLanguage(tabId: Int?, callback: (String) -> Unit)
    fun executeScript(tabId: Int?, details: dynamic, callback: (Any?) -> Unit)
    fun create(createProperties: dynamic, callback: (Any?) -> Unit)
}

external interface StorageArea {
    fun get(callback: (Json) -> Unit)
    fun get(key: String, callback: (Json) -> Unit)
    fun get(keys: Array<String>, callback: (Json) -> Unit)
    fun get(keys: Json, callback: (Json) -> Unit)

    fun set(data: Json, callback: () -> Unit)

    fun remove(key: String, callback: (Json) -> Unit)
    fun remove(keys: Array<String>, callback: (Json) -> Unit)

    fun clear()
}

external class ChromeStorage {
    val local: ChromeStorageLocal
    val sync: ChromeStorageSync
}

external class ChromeStorageLocal : StorageArea {
    override fun get(callback: (Json) -> Unit)
    override fun get(key: String, callback: (Json) -> Unit)
    override fun get(keys: Array<String>, callback: (Json) -> Unit)
    override fun get(keys: Json, callback: (Json) -> Unit)

    override fun set(data: Json, callback: () -> Unit)

    override fun remove(key: String, callback: (Json) -> Unit)
    override fun remove(keys: Array<String>, callback: (Json) -> Unit)

    override fun clear()
}

external class ChromeStorageSync : StorageArea {
    override fun get(callback: (Json) -> Unit)
    override fun get(key: String, callback: (Json) -> Unit)
    override fun get(keys: Array<String>, callback: (Json) -> Unit)
    override fun get(keys: Json, callback: (Json) -> Unit)

    override fun set(data: Json, callback: () -> Unit)

    override fun remove(key: String, callback: (Json) -> Unit)
    override fun remove(keys: Array<String>, callback: (Json) -> Unit)

    override fun clear()
}

external class ChromeI18N {
    fun getMessage(messageName: String): String
    fun getAcceptLanguages(): Array<String>
    fun getUILanguage(): String
    fun detectLanguage(text: String, callback: (ChromeI18NDetectLanguageResult) -> Unit)
}

external class ChromeI18NDetectLanguageResult {
    val isReliable: Boolean
    val languages: Array<ChromeI18NDetectedLanguage>
}

external class ChromeI18NDetectedLanguage {
    val language: String
    val percentage: Int
}

external class ContextMenus {
    fun create(createProperties: Json, callback: dynamic)
}

external class ContextMenusOnClicked {
    fun addListener(info: Json, tab: Any)
}

external class Runtime {
    fun getURL(path: String): String
}