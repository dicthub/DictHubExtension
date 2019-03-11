package org.dicthub.page

import browserObj
import i18nMessage
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import org.dicthub.lang.Lang
import org.dicthub.lang.fromCode
import org.dicthub.model.UserPreference
import org.dicthub.plugin.PluginContentAdapter
import org.dicthub.plugin.PluginIndex
import org.dicthub.view.TagAppender
import kotlin.browser.document
import kotlin.browser.window

class WelcomePage(private val userPreference: UserPreference,
                  private val pluginIndex: PluginIndex,
                  private val pluginUpdater: PluginContentAdapter) {

    private val guideUrl = "https://dicthub.org/docs/getting-started/quick-start/"

    fun render() {
        document.body?.append {
            div (classes = "alert alert-info mt-1 mb-0 py-0") {
                +i18nMessage("welcome_translation_notice")
                a (classes = "btn", href = "https://translate.google.com/translate?sl=en&u=${encodeURIComponent(guideUrl)}&tl=${browserObj.i18n.getUILanguage()}") {
                    target = "_blank"
                    +"\uD83C\uDF0F"
                }
            }
            div {
                style = "width:100%;height:100%;margin-bottom:4rem;"
                appendHelpIframe(this)
            }

            div(classes = "fixed-bottom") {
                appendExpressSetup(this)
            }
        }
    }

    private val appendHelpIframe: TagAppender = {
        iframe {
            style = "border:0; padding-bottom: 4rem"
            width = "100%"
            height = "100%"
            src = guideUrl
        }
    }

    private val appendExpressSetup: TagAppender = {
        val expressSetupDetailsId = "expressSetUpDetails"
        div(classes = "d-flex justify-content-center align-self-center py-3") {
            button(classes = "btn btn-primary btn-lg mx-3") {
                type = ButtonType.submit
                +i18nMessage("express_setup_btn")
                onClickFunction = {
                    userPreference.primaryLang = fromCode(browserObj.i18n.getUILanguage()) ?: Lang.EN
                    userPreference.sendAnalysisInfo = true
                    pluginIndex.load().then { plugins ->
                        plugins.firstOrNull { it.id == "plugin-com-google-translate" }?.let { googleTranslationPlugin ->
                            pluginUpdater.update(googleTranslationPlugin).then {
                                val enabledPlugins = userPreference.enabledPlugins.toMutableSet().apply {
                                    add(googleTranslationPlugin)
                                }
                                userPreference.enabledPlugins = enabledPlugins
                                window.location.href = "/options.html"
                            }
                        }
                    }
                }
            }

            button(classes = "btn btn-light btn-sm") {
                type = ButtonType.button
                attributes["data-toggle"] = "collapse"
                attributes["data-target"] = "#$expressSetupDetailsId"
                +i18nMessage("show_details_btn")
            }
        }
        div(classes = "collapse") {
            id = expressSetupDetailsId
            ul(classes = "list-group") {
                li(classes = "list-group-item") {
                    +"✅ ${i18nMessage("express_setup_details_language")}"
                }
                li(classes = "list-group-item") {
                    +"✅ ${i18nMessage("express_setup_details_google_plugin")}"
                }
                li(classes = "list-group-item") {
                    +"✅ ${i18nMessage("express_setup_details_google_analytics")}"
                }
            }
        }
    }
}

private external fun encodeURIComponent(urlComponent: String): String