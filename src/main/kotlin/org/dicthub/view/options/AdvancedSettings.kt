package org.dicthub.view.options

import i18nMessage
import org.dicthub.model.UserPreference
import org.dicthub.model.loadUserPreference
import org.dicthub.model.saveUserPreference
import org.dicthub.util.getElementById
import org.dicthub.view.Component
import org.dicthub.view.TagAppender
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import kotlin.browser.window
import kotlin.js.Json

private const val ID_JSON_SETTINGS = "jsonSettings"
private const val ID_JSON_SETTINGS_CONTENT = "userPreferenceText"

class AdvancedSettings(private val parent: HTMLElement,
                       private val userPreference: UserPreference) : Component {

    private lateinit var jsonSettingsTextArea: HTMLTextAreaElement

    override fun render() {
        parent.append {
            p(classes = CSS_SETTINGS_SECTION_TITLE) {
                +i18nMessage("advanced_settings")
                small { +"(${i18nMessage("advanced_settings_warn")})" }
            }
            div(classes = CSS_SETTINGS_ROW) {
                renderAdvancedSettings(this)
            }
        }

        jsonSettingsTextArea = getElementById(ID_JSON_SETTINGS_CONTENT)
        jsonSettingsTextArea.textContent = JSON.stringify(userPreference.data, null, 2)
    }

    private val renderAdvancedSettings: TagAppender = {
        label(classes = CSS_SETTINGS_ROW_LABEL) {
            +i18nMessage("edit_settings_directly")
        }
        div(classes = CSS_SETTINGS_ROW_CONTENT) {
            button(classes = "btn btn-primary c") {
                attributes["data-toggle"] = "modal"
                attributes["data-target"] = "#$ID_JSON_SETTINGS"
                +i18nMessage("show_advanced_settings")
                onClickFunction = {
                    refreshSettingsContent()
                }
            }
        }

        div(classes = "modal fade") {
            id = ID_JSON_SETTINGS
            tabIndex = "-1"
            role = "dialog"
            div(classes = "modal-dialog") {
                role = "document"
                div(classes = "modal-content") {
                    div(classes = "modal-header") {
                        h5(classes = "modal-title") {
                            +i18nMessage("raw_json_config_title")
                        }
                        button(classes = "close") {
                            type = ButtonType.button
                            attributes["data-dismiss"] = "modal"
                        }
                    }
                    div(classes = "modal-body") {
                        textArea(classes = "form-control") {
                            id = ID_JSON_SETTINGS_CONTENT
                            rows = "10"
                        }
                    }
                    div(classes = "modal-footer") {
                        button(classes = "btn btn-secondary") {
                            attributes["data-dismiss"] = "modal"
                            +i18nMessage("close_btn")
                        }
                        button(classes = "btn btn-primary") {
                            +i18nMessage("save_changes_btn")
                            onClickFunction = onSaveSettingsClicked
                        }
                    }
                }
            }
        }
    }

    private fun refreshSettingsContent() {
        loadUserPreference().then { latestUserPreference ->
            jsonSettingsTextArea.textContent = JSON.stringify(latestUserPreference.data, null, 2)
        }
    }

    private val onSaveSettingsClicked = { _: Event ->
        val data = JSON.parse<Json>(jsonSettingsTextArea.value ?: "")
        try {
            val newUserPreference = UserPreference(data)
            saveUserPreference(newUserPreference)

            window.location.reload()
        } catch (e: Exception) {
            console.error("Failed to parse input settings")
        }
    }
}