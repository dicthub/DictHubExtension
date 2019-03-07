package org.dicthub.view.options

import i18nMessage
import org.dicthub.lang.Lang
import org.dicthub.lang.fromCode
import org.dicthub.model.UserPreference
import org.dicthub.util.getElementById
import org.dicthub.view.Component
import org.dicthub.view.TagAppender
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement


private const val ID_PRIMARY_LANG = "primaryLang"
private const val ID_MAX_TRANSLATION_RESULTS = "maxTranslationResults"


class TranslationSettings(private val parent: HTMLElement, private val userPreference: UserPreference) : Component {

    override fun render() {
        parent.append {
            p (classes = CSS_SETTINGS_SECTION_TITLE) {
                +i18nMessage("translation_settings")
            }
            div (classes = CSS_SETTINGS_ROW) {
                renderPrimaryLangSelect(this)
            }
            div (classes = CSS_SETTINGS_ROW) {
                renderAutoLangDetectionCheckbox(this)
            }
            div (classes = CSS_SETTINGS_ROW) {
                renderMaxTranslationResult(this)
            }
        }

        val primarySettingsSelect: HTMLSelectElement = getElementById(ID_PRIMARY_LANG)
        val maxTranslationResultsInput: HTMLInputElement = getElementById(ID_MAX_TRANSLATION_RESULTS)

        primarySettingsSelect.value = userPreference.primaryLang.code
        primarySettingsSelect.onchange = {
            userPreference.primaryLang = fromCode(primarySettingsSelect.value) ?: Lang.EN
            true
        }

        maxTranslationResultsInput.onchange = {
            userPreference.maxTranslationResult = maxTranslationResultsInput.value.toIntOrNull() ?: 3
            true
        }
    }

    private val renderPrimaryLangSelect: TagAppender = {
        label(classes = CSS_SETTINGS_ROW_LABEL) {
            +i18nMessage("preferred_language")
        }
        div(classes = CSS_SETTINGS_ROW_CONTENT) {
            select(classes = CSS_SETTINGS_ROW_INPUT) {
                id = ID_PRIMARY_LANG
                Lang.values().forEach { lang ->
                    option {
                        +lang.getCode()
                    }
                }
            }
        }
    }

    private val renderAutoLangDetectionCheckbox: TagAppender = {
        label(classes = CSS_SETTINGS_ROW_LABEL) {
            +i18nMessage("auto_detect_language")
        }
        div(classes = "$CSS_SETTINGS_ROW_CONTENT align-self-center") {
            var autoDetectLang = userPreference.autoDetectLang
            checkBoxInput {
                checked = autoDetectLang
                onChangeFunction = {
                    autoDetectLang = !autoDetectLang
                    userPreference.autoDetectLang = autoDetectLang
                }
            }
        }
    }

    private val renderMaxTranslationResult: TagAppender = {
        label(classes = CSS_SETTINGS_ROW_LABEL) {
            +i18nMessage("max_translation_result")
        }
        div(classes = CSS_SETTINGS_ROW_CONTENT) {
            input (classes = CSS_SETTINGS_ROW_INPUT) {
                id = ID_MAX_TRANSLATION_RESULTS
                type = InputType.number
                placeholder = "3"
                value = "${userPreference.maxTranslationResult}"
            }
        }
    }

}