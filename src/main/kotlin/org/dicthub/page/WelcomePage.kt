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
import org.dicthub.plugin.PluginOptionsAdapter
import org.dicthub.view.TagAppender
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Promise

class WelcomePage(private val userPreference: UserPreference,
                  private val pluginIndex: PluginIndex,
                  private val pluginUpdater: PluginContentAdapter,
                  private val pluginOptionsAdapter: PluginOptionsAdapter) {

    private val guideUrl = "https://dicthub.org/docs/getting-started/quick-start/"
    private val googleTranslateId = "plugin-com-google-translate"

    fun render() {
        document.body?.append {
            div(classes = "alert alert-info mt-1 mb-0 py-0") {
                +i18nMessage("welcome_translation_notice")
                a(classes = "btn", href = "https://translate.google.com/translate?sl=en&u=${encodeURIComponent(guideUrl)}&tl=${browserObj.i18n.getUILanguage()}") {
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
                        plugins.firstOrNull { it.id == googleTranslateId }?.let { googleTranslationPlugin ->
                            pluginUpdater.update(googleTranslationPlugin).then {
                                val enabledPlugins = userPreference.enabledPlugins.toMutableSet().apply {
                                    add(googleTranslationPlugin)
                                }
                                userPreference.enabledPlugins = enabledPlugins
                                setGoogleTranslateOptions().then {
                                    window.location.href = "/options.html"
                                }
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

    private fun setGoogleTranslateOptions(): Promise<Boolean> {
        val checkErrorWithTimeout = { url: String, timeout: Int, onSuccess: () -> Unit, onError: () -> Unit ->
            val xhr = XMLHttpRequest()
            xhr.timeout = timeout
            xhr.open("GET", url)
            xhr.onloadend = {
                onSuccess()
            }
            xhr.onerror = {
                onError()
            }
            xhr.send()
        }

        return Promise { resolve, reject ->
            checkErrorWithTimeout("https://translate.google.com/robots.txt", 1000, {
                console.info("Access to https://translate.google.com/robots.txt succeeded")
                resolve(false)
            }, {
                checkErrorWithTimeout("https://translate.google.cn/robots.txt", 1000, {
                    console.info("Set useGoogleCn to true for cn ip address. (avoid GFW).")
                    pluginOptionsAdapter.saveOptionValue(googleTranslateId, "useGoogleCn", "true")
                    resolve(true)
                }, {
                    console.warn("Access to https://translate.google.cn/robots.txt failed")
                    resolve(false)
                })
            })
        }
    }
}

private external fun encodeURIComponent(urlComponent: String): String