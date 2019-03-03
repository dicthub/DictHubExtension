package org.dicthub.page

import org.dicthub.model.PluginInfo
import org.dicthub.model.UserPreference
import org.dicthub.plugin.PluginIndex
import org.dicthub.plugin.PluginContentAdapter
import org.dicthub.plugin.PluginOptionsAdapter
import org.dicthub.util.getElementById
import org.dicthub.view.options.AdvancedSettings
import org.dicthub.view.options.CSS_SETTINGS_SECTION
import org.dicthub.view.options.PluginSettings
import org.dicthub.view.options.TranslationSettings
import kotlinx.html.*
import kotlinx.html.dom.append
import org.dicthub.model.TranslationResult
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Promise

const val ID_TRANSLATION_SETTINGS = "translationSettings"
const val ID_PLUGIN_SETTINGS = "pluginSettings"
const val ID_ADVANCED_SETTINGS = "advancedSettings"

class OptionsPage(private val userPreference: UserPreference,
                  private val pluginIndex: PluginIndex,
                  private val pluginContentAdapter: PluginContentAdapter,
                  private val pluginOptionsAdapter: PluginOptionsAdapter) {

    fun render() {
        document.body?.append {
            div(classes = "container card") {
                div(classes = CSS_SETTINGS_SECTION) {
                    id = ID_TRANSLATION_SETTINGS
                }
                div(classes = CSS_SETTINGS_SECTION) {
                    id = ID_PLUGIN_SETTINGS
                }
                div(classes = CSS_SETTINGS_SECTION) {
                    id = ID_ADVANCED_SETTINGS
                }
            }
        }

        val translationSettings = TranslationSettings(getElementById(ID_TRANSLATION_SETTINGS), userPreference)
        translationSettings.render()

        getPluginInfoList().then { pluginStatus ->
            pluginOptionsAdapter.loadOptions(pluginStatus.keys.map { it.id }).then { pluginOptions ->
                val pluginSettings = PluginSettings(getElementById(ID_PLUGIN_SETTINGS), userPreference, pluginStatus, pluginOptions,
                        { pluginContentAdapter.update(it) },
                        { pluginId, optionName, optionValue ->  pluginOptionsAdapter.saveOptionValue(pluginId, optionName, optionValue)})
                pluginSettings.render()
            }.catch {
                val pluginSettings = PluginSettings(getElementById(ID_PLUGIN_SETTINGS), userPreference, pluginStatus, emptyMap(),
                        { pluginContentAdapter.update(it) },
                        { pluginId, optionName, optionValue ->  pluginOptionsAdapter.saveOptionValue(pluginId, optionName, optionValue)})
                pluginSettings.render()
            }
        }

        val advancedSettings = AdvancedSettings(getElementById(ID_ADVANCED_SETTINGS), userPreference)
        advancedSettings.render()

        sendAnalysisInfo()
    }

    private fun getPluginInfoList() =  Promise<Map<PluginInfo, Boolean>> { resolve, _ ->
        pluginIndex.load().then { pluginList ->
            val pluginIds = pluginList.map { it.id }
            pluginContentAdapter.load(pluginIds).then { localPlugins ->
                val pluginVersions = localPlugins.map { it.id to it.version }.toMap()
                val pluginStatus = pluginList.map { it to (it.version == pluginVersions[it.id]) }.toMap()
                resolve(pluginStatus)
            }
        }
    }

    private fun sendAnalysisInfo() {
        if (!userPreference.sendAnalysisInfo) {
            return
        }

        ga("send", "pageview", window.location.pathname)
    }
}

private external fun ga(command: String, pageView: String, location: String)