package org.dicthub.page

import org.dicthub.model.*
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.EventListener
import kotlin.browser.window
import kotlin.js.*

class SandboxPage {

    private var userPreference: UserPreference = UserPreference(json())

    private var providers: MutableList<TranslationProvider> = mutableListOf()

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
    private val evtListener = EventListener { event ->

        val packet = event.let { it as? MessageEvent }
                ?.takeIf { it.origin == window.origin }?.data
                ?.let { it as? Json }
                ?.let { Packet(it) }
                ?: return@EventListener

        when (packet.cmd) {
            Command.USER_PREFERENCE -> handleUserPreference(UserPreference(packet.payload))
            Command.QUERY -> handleQueryEvent(packet.payload)
            Command.PLUGINS -> {
                val promises = PluginContents(packet.payload).plugins.map { handlePluginEvent(it) }.toTypedArray()
                // When plugins are ready, signal parent page to query
                Promise.all(promises).then { sendReadySignal() }
            }
            else -> {
                console.warn("Unknown packet", packet)
            }
        }
    }

    fun init() {
        window.addEventListener("message", evtListener, false)

        sendMessageToPopUp(Command.SANDBOX_START, json())

        // unset unnecessary variables
        js("if(typeof chrome !== 'undefined')chrome=undefined;if(typeof browser !== 'undefined')browser=undefined;")
    }

    private fun handlePluginEvent(plugin: Pair<PluginContent, PluginOptions>): Promise<String> {
        return Promise { resolve, reject ->
            val pluginContent = plugin.first
            val pluginOptions = plugin.second
            val createPlugin = "create_${pluginContent.id}".replace("-", "_")
            console.info("Executing", createPlugin)
            window.setTimeout({
                try {
                    val pluginCode = eval(pluginContent.content)
                    val pluginInstance = pluginCode[createPlugin]()
                    providers.add(pluginInstance)
                    console.info("Created plugin: ${pluginContent.id}")
                    if (pluginInstance.updateOptions) {
                        pluginInstance.updateOptions(pluginOptions)
                        console.info("Updated options for ${pluginContent.id}", pluginOptions)
                    }
                    resolve(pluginContent.id)
                } catch (err: Throwable) {
                    console.warn("Failed to create plugin ${pluginContent.id}")
                    reject(err)
                }
            })
        }
    }

    private fun handleQueryEvent(query: Query) {
        providers.filter { it.canTranslate(query) }.forEach { provider ->
            provider.translate(query).then { htmlContent ->
                sendMessageToPopUp(Command.TRANSLATION_RESULT, TranslationResult(provider.id(), query,
                        !htmlContent.contains("translation-failure"), htmlContent).data)
            }.catch {
                // TODO: Add handling for failure
                console.error(it)
            }
        }
    }

    private fun handleUserPreference(userPreference: UserPreference) {
        this.userPreference = userPreference
    }

    private fun sendReadySignal() = sendMessageToPopUp(Command.SANDBOX_READY, json())

    private fun sendMessageToPopUp(cmd: Command, payload: Json) {
        console.log("SandBox --> Popup", cmd, payload)
        window.parent.postMessage(Packet(cmd, payload).data, window.origin, emptyArray())
    }
}
