import org.dicthub.model.loadUserPreference
import org.dicthub.page.OptionsPage
import org.dicthub.page.SandboxPage
import org.dicthub.page.TranslationPage
import org.dicthub.plugin.PluginIndex
import org.dicthub.plugin.PluginContentAdapter
import org.dicthub.plugin.PluginOptionsAdapter
import org.dicthub.util.AjaxHttpClient
import org.w3c.dom.COMPLETE
import org.w3c.dom.DocumentReadyState
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

fun main(args: Array<String>) {

    document.onreadystatechange =  {
        if (document.readyState == DocumentReadyState.COMPLETE) {
            when(document.body?.id) {
                "popupBody" ->  {
                    initPopupPage()
                }
                "overlayBody" -> {
                    initOverlayPage()
                }
                "optionsBody" -> {
                    initOptionsPage()
                }
                "sandboxBody" -> {
                    initSandboxPage()
                }
            }
        }
    }
}

private fun initPopupPage() {
    document.documentElement?.setAttribute("style", "font-size: 13px")

    val pluginOptionsAdapter = PluginOptionsAdapter(browserObj.storage.local)

    extractQueryFromTabs().then {
        loadUserPreference().then { userPreference ->
            val translationPage = TranslationPage(userPreference, PluginContentAdapter(AjaxHttpClient, browserObj.storage.local),
                    pluginOptionsAdapter, it.first, it.second, userPreference.primaryLang.code, false)
            translationPage.render()
        }
    }
}

private fun initOverlayPage() {
    document.documentElement?.setAttribute("style", "font-size: 12px")

    val pluginOptionsAdapter = PluginOptionsAdapter(browserObj.storage.local)

    val queryContext = extractQueryFromUrl()
    loadUserPreference().then { userPreference ->
        val translationPage = TranslationPage(userPreference, PluginContentAdapter(AjaxHttpClient, browserObj.storage.local),
                pluginOptionsAdapter, queryContext.first, queryContext.second, userPreference.primaryLang.code, true)
        translationPage.render()
    }
}

private fun initOptionsPage() {

    loadUserPreference().then { userPreference ->
        val pluginIndex = PluginIndex(AjaxHttpClient, userPreference.pluginRepository)
        val pluginUpdater = PluginContentAdapter(AjaxHttpClient, browserObj.storage.local)
        val pluginOptionsAdapter = PluginOptionsAdapter(browserObj.storage.local)

        val optionsPage = OptionsPage(userPreference, pluginIndex, pluginUpdater, pluginOptionsAdapter)
        optionsPage.render()
    }
}

private fun initSandboxPage() {

    val sandboxPageViewController = SandboxPage()

    sandboxPageViewController.init()
}

private fun extractQueryFromUrl(): Triple<String, String?, String?> =
    window.location.hash.takeIf { it.startsWith("#") }?.let {
        val jsonStr = decodeURIComponent(it.substring(1))
        val data = JSON.parse<Json>(jsonStr)
        return Triple(
                data["selection"].toString(),
                data["fromLang"]?.toString() ?: "",
                data["toLang"]?.toString() ?: ""
        )
    } ?: run {
        return Triple("", null, null)
    }

private fun extractQueryFromTabs(): Promise<Pair<String, String?>> = Promise { resolve, _ ->
    val getSelectionCode = "window.getSelection().toString();"
    browserObj.tabs.executeScript(null, json("code" to getSelectionCode)) { selections ->
        browserObj.tabs.detectLanguage(null) { tabLang ->
            val selection = selections?.let { it as Array<*> }?.filterIsInstance<String>()?.firstOrNull() ?: ""
            resolve(Pair(selection, tabLang))
        }
    }
}