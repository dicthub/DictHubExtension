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
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
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
                     private val listener: QueryListener,
                     private val initialQueryText: String? = "",
                     private val initialFromLang: Lang? = null,
                     private val initialToLang: Lang? = null,
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

    private val renderFromLangSelect: TagAppender = {
        select(classes = "form-control form-control-sm mb-2 lang-select-optional") {
            id = ID_FROM_LANG
            option { +"" }
            Lang.values().forEach { lang ->
                option {
                    +lang.getCode()
                }
            }

            onChangeFunction = onValueChange
        }
    }

    private val renderToLangSelect: TagAppender = {
        select(classes = "form-control form-control-sm mb-2 lang-select-optional") {
            id = ID_TO_LANG
            option { +"" }
            Lang.values().forEach { lang ->
                option {
                    +lang.getCode()
                }
            }

            onChangeFunction = onValueChange
        }
    }

    override fun render() {
        parent.append {
            form(classes = "form-inline ${if (uiMode == UIMode.OVERLAY && initialFromLang != null) "collapse" else ""}") {
                id = ID_FORM
                onSubmitFunction = { it.preventDefault(); onValueChange(it) }
                div(classes = "form-row") {
                    div(classes = "col-auto") {
                        renderQueryInput(this)
                    }
                    div(classes = "col-auto") {
                        renderQueryButton(this)
                    }
                    div(classes = "col-auto") {
                        div(classes = "input-group") {
                            renderFromLangSelect(this)
                            label {
                                +"⇒"
                            }
                            renderToLangSelect(this)
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

        invokeListenerIfReady()
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