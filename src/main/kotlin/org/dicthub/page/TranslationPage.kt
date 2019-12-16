package org.dicthub.page

import browserObj
import i18nMessage
import org.dicthub.lang.fromCode
import org.dicthub.model.*
import org.dicthub.plugin.PluginContentAdapter
import org.dicthub.plugin.PluginOptionsAdapter
import org.dicthub.util.getElementById
import org.dicthub.view.content.QueryContainer
import org.dicthub.view.content.QueryListener
import org.dicthub.view.content.ResultContainer
import org.dicthub.view.content.UIMode
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.div
import kotlinx.html.js.iframe
import kotlinx.html.stream.appendHTML
import org.dicthub.lang.Lang
import org.dicthub.lang.LangDetector
import org.dicthub.plugin.PluginUpdateChecker
import org.dicthub.version.VersionDetector
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

class TranslationPage(private val userPreference: UserPreference,
                      private val pluginContentAdapter: PluginContentAdapter,
                      private val pluginOptionsAdapter: PluginOptionsAdapter,
                      private val pluginUpdateChecker: PluginUpdateChecker,
                      private val langDetector: LangDetector,
                      private val queryText: String,
                      private val fromLangStr: String?,
                      private val toLangStr: String?,
                      private val inFrame: Boolean) {

    private val extensionCheckInterval = 24 * 3600 * 1000 // 1 day
    private val pluginCheckInterval = 24 * 3600 * 1000 // 1 day

    private lateinit var sandbox: HTMLIFrameElement
    private lateinit var notificationContainer : HTMLDivElement
    private lateinit var queryContainer: QueryContainer
    private lateinit var resultContainer: ResultContainer

    private var sandBoxReady: Boolean = false
    private var pendingQuery: Query? = null


    private val queryListener: QueryListener = {

        resultContainer.clearResults()
        if (sandBoxReady) {
            sendQueryMessage(it)
        } else {
            pendingQuery = it
        }
    }

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    private val messageListener = { evt: Event ->

        val packet = evt.let { it as? MessageEvent }
                ?.takeIf { it.origin == window.origin }
                ?.let { it.data }
                ?.let { it as? Json }
                ?.let { Packet(it) }

        when (packet?.cmd) {
            Command.SANDBOX_START -> {
                sendSandBoxMessage(Command.USER_PREFERENCE, userPreference.data)
                val pluginIds = userPreference.enabledPlugins.map { it.id }
                pluginOptionsAdapter.loadOptions(pluginIds)
                pluginContentAdapter.load(pluginIds).then { pluginContents ->
                    pluginOptionsAdapter.loadOptions(pluginIds).then { pluginOptions ->
                        val contents = pluginContents.map { Pair(it, pluginOptions[it.id] ?: json()) }
                        sendSandBoxMessage(Command.PLUGINS, PluginContents(contents).data)
                    }.catch {
                        console.warn("No plugin options found")
                        val contents = pluginContents.map { Pair(it, json()) }
                        sendSandBoxMessage(Command.PLUGINS, PluginContents(contents).data)
                    }
                }
            }
            Command.SANDBOX_READY -> {
                sandBoxReady = true
                pendingQuery?.let { sendQueryMessage(it) }
            }
            Command.TRANSLATION_RESULT -> {
                val translationResult = TranslationResult(packet.payload)
                if (!isTranslationResultUnsafe(translationResult)) {
                    resultContainer.appendResult(translationResult.htmlContent)
                    sendAnalysisInfo(translationResult)
                } else {
                    val unsafeWarning = buildSecurityViolationMessage(translationResult.pluginId);
                    resultContainer.appendResult(unsafeWarning)
                    sendAnalysisInfo(TranslationResult(translationResult.pluginId, translationResult.query, false, unsafeWarning))
                }
            }
            else -> {
                console.warn("Unknown message", packet)
            }
        }
        Unit
    }

    fun render() {
        val fromLang = fromLangStr?.let { fromCode(it) }
        val toLang = toLangStr?.let { fromCode(it) } ?: userPreference.primaryLang

        val notificationContainerId = "notificationContainer"
        val queryContainerId = "queryContainer"
        val resultContainerId = "resultContainer"
        val sandboxId = "sandbox"

        document.body?.append {
            iframe {
                attributes["sandbox"] = "allow-same-origin allow-scripts"
                id = sandboxId
                src = "sandbox.html"
                width = "0"
                height = "0"
                hidden = true
            }
            div("container") {
                div {
                    attributes["style"] = "display: none;"
                    id = notificationContainerId
                }
                div {
                    id = queryContainerId
                }
                hr {}
                div {
                    id = resultContainerId
                }
            }
        }

        val effectiveLangDetector: LangDetector = if (userPreference.autoDetectLang) langDetector else NullLangDetector

        sandbox = getElementById(sandboxId)
        notificationContainer = getElementById(notificationContainerId)
        queryContainer = QueryContainer(getElementById(queryContainerId),effectiveLangDetector, queryListener,
                queryText, fromLang, toLang, listOf(userPreference.primaryLang), if (inFrame) UIMode.OVERLAY else UIMode.POPUP)
        resultContainer = ResultContainer(getElementById(resultContainerId))

        resultContainer.render()
        queryContainer.render()

        window.addEventListener("message", messageListener, false)

        checkExtensionStatus()
        checkPluginStatus()
    }

    private fun sendQueryMessage(query: Query) {
        sendSandBoxMessage(Command.QUERY, query)
        sendAnalysisInfo(query)
    }

    private fun sendSandBoxMessage(cmd: Command, payload: Json?) {
        console.log("Popup --> Sandbox", cmd, payload)
        sandbox.contentWindow?.postMessage(Packet(cmd, payload).data, sandbox.contentWindow?.origin
                ?: "", emptyArray())
    }

    private fun sendPluginNotification(title: String, message: String) {
        val notificationOptions = json(
                "type" to "basic",
                "title" to title,
                "message" to message,
                "iconUrl" to "ico/dicthub-48.png"
        )

        browserObj.runtime.sendMessage(json(
                "cmd" to "plugin-notification",
                "payload" to notificationOptions
        ))
    }

    private fun sendAnalysisInfo(query: Query) {
        if (!userPreference.sendAnalysisInfo) {
            return
        }

        ga("send", "pageview", window.location.pathname)
        ga("send", "event", "FromLangToLang", "${query.getFrom().code}_${query.getTo().code}")
    }

    private fun sendAnalysisInfo(translationResult: TranslationResult) {
        if (!userPreference.sendAnalysisInfo) {
            return
        }

        val pluginVersion = userPreference.enabledPlugins.firstOrNull { it.id == translationResult.pluginId }?.version ?: ""
        ga("send", "event", "PluginVersion", translationResult.pluginId, pluginVersion)

        val action = if (translationResult.success) "TranslationSuccess" else "TranslationFailure"
        val fromLangToLang = "${translationResult.query.getFrom().code}_${translationResult.query.getTo().code}"
        ga("send", "event", action, translationResult.pluginId, fromLangToLang, 1)
        if (!translationResult.success) {
            // Send failed query for debugging
            ga("send", "event", "PluginFailure-${translationResult.pluginId}", fromLangToLang, translationResult.query.getText())
        }
    }

    private fun isTranslationResultUnsafe(result: TranslationResult) = result.htmlContent.contains("<script>")

    private fun buildSecurityViolationMessage(pluginId: String): String {
        val htmlContent = StringBuilder()
        htmlContent.appendHTML().div (classes = "translation-failure alert alert-danger") {
            role = "alert"
            +"${i18nMessage("insecure_translation_script_warning")} ID: $pluginId"
        }
        return htmlContent.toString()
    }

    private fun checkExtensionStatus() {
        val lastCheckTime = VersionDetector.getLastCheckTime()
        if (Date().getTime() - lastCheckTime < extensionCheckInterval ) {
            console.info("Ignore check extension version", lastCheckTime)
            return
        }

        VersionDetector.setLastCheckTime(Date().getTime().toLong())

        val currentVersion = VersionDetector.getCurrentVersion()
        VersionDetector.getPublishedVersion().then { publishedVersion ->
            console.info("Extension versions: published $publishedVersion current: $currentVersion")
            if (VersionDetector.hasNewVersion(publishedVersion, currentVersion)) {
                notificationContainer.style.display = "block"
                notificationContainer.append {
                    div (classes = "alert alert-primary") {
                        a (href = VersionDetector.getExtensionUrl(), target = "_blank") {
                            +"DictHub: $publishedVersion. \uD83C\uDD95"
                        }
                    }
                }
            }
        }.catch {
            console.warn(it)
        }
    }

    private fun checkPluginStatus() {

        val lastPluginCheckTime = pluginUpdateChecker.getLastCheckTime()
        if (Date().getTime() - lastPluginCheckTime < pluginCheckInterval) {
            console.info("Ignore check plugin version", lastPluginCheckTime)
            return
        }
        pluginUpdateChecker.check().then { result ->
            if (result.upgradablePlugins.isNotEmpty()) {
                val upgradablePluginNames = result.upgradablePlugins.map { it.name }.joinToString("\n")
                if (userPreference.autoUpdatePlugin) {
                    val pluginUpdates = result.upgradablePlugins.map { pluginContentAdapter.update(it) }.toTypedArray()
                    Promise.all(pluginUpdates).then {
                        // Update enabled plugins
                        val upgradedPluginIds = result.upgradablePlugins.map { it.id }.toSet()
                        val enabledPlugins = userPreference.enabledPlugins.filterNot { upgradedPluginIds.contains(it.id) }.toMutableSet().apply {
                            addAll(result.upgradablePlugins)
                        }
                        userPreference.enabledPlugins = enabledPlugins

                        val upgradedPluginsNames = result.upgradablePlugins.map { it.name }.joinToString("\n")
                        sendPluginNotification(i18nMessage("plugin_updated"), upgradedPluginsNames)
                    }.catch {
                        sendPluginNotification(i18nMessage("plugin_updates_available"), upgradablePluginNames)
                    }
                } else {
                    sendPluginNotification(i18nMessage("plugin_updates_available"), upgradablePluginNames)
                }
            }

            if (result.newPlugins.isNotEmpty()) {
                val newPluginNames = result.newPlugins.map { it.name }.joinToString { "\n" }
                sendPluginNotification(i18nMessage("new_plugins_available"), newPluginNames)
            }
        }.catch {
            console.warn("Failed to check plugin updates", it)
        }
    }
}

private object NullLangDetector : LangDetector {
    override fun detectLang(text: String) = Promise<Lang> {_, reject ->
        reject(UnsupportedOperationException("Lang detection is not supported"))
    }
}

private external fun ga(command: String, pageView: String, location: String)

private external fun ga(command: String, type: String,
                        eventCategory: String, eventAction: String)

private external fun ga(command: String, type: String,
                        eventCategory: String, eventAction: String, eventLabel: String)

private external fun ga(command: String, type: String,
                        eventCategory: String, eventAction: String, eventLabel: String, eventValue: Int)