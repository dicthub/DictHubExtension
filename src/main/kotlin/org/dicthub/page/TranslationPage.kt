package org.dicthub.page

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
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

class TranslationPage(private val userPreference: UserPreference,
                      private val pluginContentAdapter: PluginContentAdapter,
                      private val pluginOptionsAdapter: PluginOptionsAdapter,
                      private val queryText: String,
                      private val fromLangStr: String?,
                      private val toLangStr: String?,
                      private val inFrame: Boolean) {

    private lateinit var sandbox: HTMLIFrameElement
    private lateinit var queryContainer: QueryContainer
    private lateinit var resultContainer: ResultContainer

    private var sandBoxReady: Boolean = false
    private var pendingQuery: Query? = null


    private val queryListener: QueryListener = {

        resultContainer.clearResults()
        if (sandBoxReady) {
            sendSandBoxMessage(Command.QUERY, it)
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
                        val pluginContents = pluginContents.map { Pair(it, pluginOptions[it.id] ?: json()) }
                        sendSandBoxMessage(Command.PLUGINS, PluginContents(pluginContents).data)
                    }.catch {
                        console.warn("No plugin options found")
                        val pluginContents = pluginContents.map { Pair(it, json()) }
                        sendSandBoxMessage(Command.PLUGINS, PluginContents(pluginContents).data)
                    }
                }
            }
            Command.SANDBOX_READY -> {
                sandBoxReady = true
                pendingQuery?.let { sendSandBoxMessage(Command.QUERY, it) }
            }
            Command.TRANSLATION_RESULT -> {
                val translationResult = TranslationResult(packet.payload)
                resultContainer.appendResult(translationResult.htmlContent)
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
                    id = queryContainerId
                }
                hr {}
                div {
                    id = resultContainerId
                }
            }
        }

        sandbox = getElementById(sandboxId)
        queryContainer = QueryContainer(getElementById(queryContainerId), queryListener,
                queryText, fromLang, toLang, if (inFrame) UIMode.OVERLAY else UIMode.POPUP)
        resultContainer = ResultContainer(getElementById(resultContainerId))

        resultContainer.render()
        queryContainer.render()

        window.addEventListener("message", messageListener, false)
    }

    private fun sendSandBoxMessage(cmd: Command, payload: Json?) {
        console.log("Popup --> Sandbox", cmd, payload)
        sandbox.contentWindow?.postMessage(Packet(cmd, payload).data, sandbox.contentWindow?.origin
                ?: "", emptyArray())
    }
}