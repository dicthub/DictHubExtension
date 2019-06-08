package org.dicthub.view.content

import org.dicthub.lang.Lang
import org.dicthub.lang.fromCode
import org.dicthub.model.Query
import org.dicthub.model.createQury
import org.dicthub.util.getElementById
import org.dicthub.view.Component
import org.dicthub.view.TagAppender
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import org.dicthub.lang.LangDetector
import org.w3c.dom.*
import org.w3c.dom.events.Event

enum class UIMode {
    POPUP,
    OVERLAY
}

typealias QueryListener = (Query) -> Unit


private const val ID_FORM = "queryForm"
private const val ID_QUERY_TEXT = "queryText"
private const val ID_FROM_LANG = "fromLang"
private const val ID_TO_LANG = "toLang"

class QueryContainer(private val parent: HTMLElement,
                     private val langDetector: LangDetector,
                     private val listener: QueryListener,
                     private val initialQueryText: String? = "",
                     private val initialFromLang: Lang? = null,
                     private val initialToLang: Lang? = null,
                     private val preferredLangs: List<Lang> = emptyList(),
                     private val uiMode: UIMode) : Component {

    private lateinit var queryTextInput: HTMLInputElement
    private lateinit var fromLangSelect: HTMLSelectElement
    private lateinit var toLangSelect: HTMLSelectElement

    private val renderQueryInput: TagAppender = {
        input(classes = "form-control form-control-sm") {
            id = ID_QUERY_TEXT
            type = InputType.text
            size = if (uiMode == UIMode.POPUP) "14" else "7"
            placeholder = "Words..."
            value = initialQueryText ?: ""
        }
    }

    private val renderQueryButton: TagAppender = {
        button(classes = "btn btn-light btn-primary btn-sm mb-2") {
            type = ButtonType.submit
            +"\uD83D\uDD0D" // Search emoji
        }
    }

    private fun renderLangSelect(formId: String): TagAppender = {
        select(classes = "form-control form-control-sm mb-2 lang-select-optional") {
            id = formId
            option { +"" }
            langList().forEach { lang ->
                option {
                    +lang.getCode()
                }

            }

            onChangeFunction = onValueChange
        }
    }

    private fun langList(): List<Lang> {
        val langList = mutableListOf<Lang>()
        langList.addAll(preferredLangs.distinct())
        langList.addAll(Lang.values().filterNot { preferredLangs.contains(it) })
        return langList
    }

    override fun render() {
        parent.append {
            form(classes = "form-inline ${if (uiMode == UIMode.OVERLAY && initialFromLang != null) "collapse" else ""}") {
                id = ID_FORM
                onSubmitFunction = { evt ->
                    evt.preventDefault()
                    queryTextInput.value.takeIf { it.isNotBlank() }?.let { text ->
                        langDetector.detectLang(text).then {
                            onValueChange(evt)
                        }.catch {
                            onValueChange(evt)
                        }
                    }
                }
                div(classes = "form-row") {
                    div(classes = "col-auto") {
                        renderQueryInput(this)
                    }
                    div(classes = "col-auto") {
                        renderQueryButton(this)
                    }
                    div(classes = "col-auto") {
                        div(classes = "input-group") {
                            renderLangSelect(ID_FROM_LANG)(this)
                            label {
                                +"⇒"
                            }
                            renderLangSelect(ID_TO_LANG)(this)
                        }
                    }
                    if (uiMode == UIMode.POPUP) {
                        div(classes = "col-auto") {
                            a(classes = "btn btn-light btn-primary btn-sm mb-2") {
                                href = "options.html"
                                target = "_blank"
                                +"⚙"
                            }
                        }
                    }
                }
            }
            if (uiMode == UIMode.OVERLAY) {
                button(classes = "btn btn-light") {
                    role = "button"
                    attributes["data-toggle"] = "collapse"
                    attributes["data-target"] = "#$ID_FORM"
                    id = "showQuery"
                    +"↕️" // Up down emoji
                }
            }
        }

        queryTextInput = getElementById(ID_QUERY_TEXT)
        queryTextInput.required = true // required is not supported in kotlinx.html yet

        fromLangSelect = getElementById(ID_FROM_LANG)
        fromLangSelect.required = true
        fromLangSelect.value = initialFromLang?.code ?: ""

        toLangSelect = getElementById(ID_TO_LANG)
        toLangSelect.required = true
        toLangSelect.value = initialToLang?.code ?: ""

        initialQueryText?.takeIf { it.isNotBlank() }?.let { text ->
            langDetector.detectLang(text).then { lang ->
                fromLangSelect.value = lang.code
                invokeListenerIfReady()
            }.catch {
                invokeListenerIfReady()
            }
        }
    }

    private val onValueChange = { _: Event ->
        invokeListenerIfReady()
    }

    private fun invokeListenerIfReady() {
        val queryText = queryTextInput.value.takeIf { it.isNotBlank() }
        val fromLang = fromCode(fromLangSelect.value)
        val toLang = fromCode(toLangSelect.value)

        if (queryText != null && fromLang != null && toLang != null) {
            listener(createQury(queryText, fromLang, toLang))
        }
    }
}
