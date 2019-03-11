package org.dicthub.view.options

import i18nMessage
import org.dicthub.model.PluginInfo
import org.dicthub.model.PluginOptions
import org.dicthub.model.UserPreference
import org.dicthub.plugin.PluginIndex
import org.dicthub.util.AjaxHttpClient
import org.dicthub.util.getElementById
import org.dicthub.view.Component
import org.dicthub.view.TagAppender
import jSelect
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import kotlin.browser.window
import kotlin.js.Promise
import kotlin.js.json

private const val ID_NEW_REPOSITORY_INPUT = "newRepositoryInput"
private const val ID_PLUGIN_SETTINGS_LIST = "pluginSettingsList"
private const val ID_PLUGIN_SETTINGS_INFO = "pluginSettingsInfo"

typealias PluginUpdater = (PluginInfo) -> Promise<Boolean?>

typealias PluginOptionUpdater = (pluginId: String, optionName: String, optionValue: String) -> Promise<String>

class PluginSettings(private val parent: HTMLElement,
                     private val userPreference: UserPreference,
                     private val pluginStatus: Map<PluginInfo, Boolean>,
                     private val pluginOptions: Map<String, PluginOptions?>,
                     private val updater: PluginUpdater,
                     private val optionUpdater: PluginOptionUpdater
) : Component {

    private lateinit var newPluginRepositoryInput: HTMLInputElement
    private lateinit var pluginSettings: HTMLUListElement
    private lateinit var pluginSettingsMessage: HTMLDivElement

    override fun render() {
        parent.append {
            p(classes = CSS_SETTINGS_SECTION_TITLE) {
                +i18nMessage("plugin_repository_list")
            }
            div(classes = "") {
                renderPluginRepositorySettings(this)
            }
            p(classes = CSS_SETTINGS_SECTION_TITLE) {
                +i18nMessage("plugin_settings")
                small { +"(${i18nMessage("drag_to_change_order")})" }
            }
            div(classes = "alert alert-danger") {
                id = ID_PLUGIN_SETTINGS_INFO
                role = "alert"
                hidden = true
            }
            div(classes = "") {
                renderPluginSettings(this)
            }
        }

        enablePluginSort()

        newPluginRepositoryInput = getElementById(ID_NEW_REPOSITORY_INPUT)
        pluginSettings = getElementById(ID_PLUGIN_SETTINGS_LIST)
        pluginSettingsMessage = getElementById(ID_PLUGIN_SETTINGS_INFO)
    }

    private val renderPluginRepositorySettings: TagAppender = {
        ul(classes = "list-group") {
            userPreference.pluginRepository.forEach { repositoryUrl ->
                li(classes = "list-group-item") {
                    div(classes = "form-inline form-row") {
                        div(classes = "form-group col-md-11 mb-2") {
                            a(href = repositoryUrl, classes = "plugin-repository-url") {
                                +repositoryUrl
                            }
                        }

                        button(type = ButtonType.button, classes = "btn btn-light mb-2 col-auto") {
                            i(classes = "fa fa-minus-square text-danger") { }
                            onClickFunction = {
                                removeRepositoryUrl(repositoryUrl)
                            }
                        }
                    }
                }
            }

            li(classes = "list-group-item") {
                div(classes = "form-inline form-row") {
                    div(classes = "form-group col-md-11 mb-2") {
                        input(type = InputType.url, classes = "form-control") {
                            id = ID_NEW_REPOSITORY_INPUT
                            placeholder = "https://raw.githubusercontent.com/willings/DictHubPluginIndex/beta/index.json"
                        }
                    }

                    button(type = ButtonType.button, classes = "btn btn-light mb-2 col-auto") {
                        i(classes = "fa fa-plus-square text-success") { }
                        onClickFunction = {
                            addRepositoryUrl()
                        }
                    }
                }
            }
        }
    }

    private val renderPluginSettings: TagAppender = {
        ul(classes = "list-group list-group-sortable") {
            id = ID_PLUGIN_SETTINGS_LIST

            val enabledPlugins = pluginStatus.keys.filter { plugin ->
                userPreference.enabledPlugins.any { it.id == plugin.id }
            }.sortedBy { userPreference.pluginPriority.indexOf(it.id) }

            val disabledPlugins = pluginStatus.keys.filterNot { plugin ->
                userPreference.enabledPlugins.any { it.id == plugin.id }
            }.sortedBy { userPreference.pluginPriority.indexOf(it.id) }

            val storedPluginVersions = userPreference.enabledPlugins.associateBy({ it.id }, { it.version }).toMap()

            enabledPlugins.forEach {
                renderPluginEntry(it, true, it.version != storedPluginVersions[it.id])(this)
            }

            disabledPlugins.forEach {
                renderPluginEntry(it, false, it.version != storedPluginVersions[it.id])(this)
            }
        }
    }

    private fun renderPluginEntry(pluginInfo: PluginInfo, enabled: Boolean, hasUpdate: Boolean): (UL.() -> Unit) = {
        li(classes = "list-group-item") {

            val optionElementId = "pluginOptions${pluginInfo.id}"

            div(classes = "col-md-1 align-middle") {
                style = "display: inline-block"
                span {
                    style = "font-size: 1.5rem; margin-right: 10px;"
                    i(classes = "fas fa-arrows-alt plugin-item-control") { }
                }
            }

            div(classes = "col-md-7 align-middle") {
                style = "display: inline-block"

                if (enabled && hasUpdate) {
                    button(classes = "btn btn-info btn-sm plugin-update") {
                        type = ButtonType.button
                        onClickFunction = {
                            updatePlugin(pluginInfo)
                        }
                        +i18nMessage("update_btn")
                    }
                }

                strong {
                    +pluginInfo.name
                }

                if (pluginInfo.options.isNotEmpty()) {
                    button(classes = "btn btn-light btn-sm plugin-options-btn") {
                        type = ButtonType.button
                        attributes["data-toggle"] = "collapse"
                        attributes["data-target"] = "#$optionElementId"
                        +i18nMessage("options_btn")
                    }
                }

                br { }

                pluginInfo.description?.let {
                    small {
                        +"$it, "
                    }
                }
                pluginInfo.source?.let { source ->
                    small {
                        +"source from: "
                        a(href = pluginInfo.sourceUrl) {
                            +source
                        }
                        +", "
                    }
                }
                pluginInfo.author?.let { author ->
                    small {
                        +"by: "
                        a(href = pluginInfo.authorUrl) {
                            +author
                        }
                    }
                }
            }

            div(classes = "col-md-4 align-middle") {
                style = "display: inline-block"
                label(classes = "switch") {
                    input(type = InputType.checkBox) {
                        this.id = pluginInfo.id
                        this.checked = enabled
                        this.onClickFunction = onPluginSwitchClicked
                    }
                    span(classes = "slider round") { }
                }
            }

            div(classes = "collapse plugin-options") {
                id = optionElementId
                pluginInfo.options.forEach { (name, config) ->
                    val optionId = "$optionElementId$name"
                    val value = pluginOptions[pluginInfo.id]?.get(name) as? String ?: config.default
                    b(classes = "plugin-option-name") {
                        +name
                    }
                    when (config.type) {
                        "checkbox" -> {
                            var checked = (value == "true")
                            checkBoxInput (classes = "plugin-option-input") {
                                this.id = optionId
                                this.name = name
                                this.checked = checked
                                this.onChangeFunction = {
                                    checked = !checked
                                    optionUpdater(pluginInfo.id, name, checked.toString())
                                }
                            }
                        }
                        else -> {
                            input(classes = "plugin-option-input") {
                                this.id = optionId
                                this.type = InputType.values().firstOrNull { it.realValue == config.type }
                                        ?: InputType.text
                                this.name = name
                                this.value = value ?: ""
                                onChangeFunction = {
                                    optionUpdater(pluginInfo.id, name, this.value)
                                }
                            }
                        }
                    }
                    config.description?.let {
                        label(classes = "plugin-option-label") {
                            htmlFor = optionId
                            +it
                        }
                    }
                }
            }

        }
    }

    private fun enablePluginSort() {
        jSelect(".list-group-sortable").asDynamic().sortable(json(
                "placeholderClass" to "list-group-item",
                "handle" to "i"
        )).bind("sortupdate") {
            updatePluginPriority()
        }
    }

    private fun updatePluginPriority() {

        val ids = mutableListOf<String>()
        for (i in 0..pluginSettings.childElementCount) {
            pluginSettings.children[i]?.let {
                it.getElementsByTagName("input")[0] as? HTMLInputElement
            }?.id?.let {
                ids.add(it)
            }
        }
        userPreference.pluginPriority = ids
    }

    private val onPluginSwitchClicked = { evt: Event ->
        evt.currentTarget?.let { it as? HTMLInputElement }?.let { input ->
            val plugins = userPreference.enabledPlugins.toMutableSet()
            val pluginId = input.id
            if (input.checked) {
                val newPlugin = pluginStatus.keys.filter { it.id == pluginId }.single()
                plugins.add(newPlugin)
                updater(newPlugin).then {
                    reload()
                }
            } else {
                plugins.removeAll { it.id == input.id }
            }
            userPreference.enabledPlugins = plugins
        }
        Unit
    }

    private fun updatePlugin(pluginInfo: PluginInfo) {
        showMessage("Updating ${pluginInfo.name}... ")
        updater(pluginInfo).then {
            val enabledPlugins = userPreference.enabledPlugins.toMutableSet()
            enabledPlugins.removeAll { it.id == pluginInfo.id }
            enabledPlugins.add(pluginInfo)
            userPreference.enabledPlugins = enabledPlugins

            reload()
        }
    }

    private fun removeRepositoryUrl(url: String) {
        userPreference.pluginRepository = userPreference.pluginRepository.toMutableList().apply {
            remove(url)
        }

        reload()
    }

    private fun addRepositoryUrl() {
        val urlStr = newPluginRepositoryInput.value
        try {
            URL(urlStr)
        } catch (e: Throwable) {
            console.warn(e)
            return
        }
        PluginIndex(AjaxHttpClient, listOf(urlStr)).load().then {
            userPreference.pluginRepository = userPreference.pluginRepository.toMutableList().apply {
                add(0, urlStr)
            }

            reload()
        }.catch {
            console.error(it)
        }
    }

    private fun reload(timeout: Int = 1000) {
        window.setTimeout({
            window.location.reload()
        }, timeout)
    }

    private fun showMessage(message: String, cssClass: String = "alert-info", autoHidden: Boolean = true) =
            console.log(message)

}