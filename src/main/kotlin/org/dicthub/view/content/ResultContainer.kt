package org.dicthub.view.content

import org.dicthub.view.Component
import kotlinx.html.dom.create
import kotlinx.html.js.div
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window

class ResultContainer(private val parent: HTMLElement) : Component {

    private lateinit var rootContainer: HTMLElement;

    override fun render() {
        rootContainer = document.create.div() { }
        parent.append(rootContainer)
    }

    fun clearResults() {
        rootContainer.innerHTML = ""
    }

    fun appendResult(htmlContent: String) {
        val translationResult = document.create.div("clearfix translation-result") { }
        translationResult.innerHTML = htmlContent

        val collection = translationResult.getElementsByClassName("translation-voice")
        for (i in 0..collection.length) {
            collection[i]?.let { span ->
                span.let { it as? HTMLSpanElement }?.title = if (isInFrame()) "Click inside frame to enable voice play" else "";
                span.addEventListener("mouseover", { _ ->
                    val audio = span.firstElementChild as? HTMLAudioElement
                    audio?.play()?.then {
                    }?.catch {
                        console.info("Audio not playable")
                    }
                })
            }
        }

        rootContainer.append(translationResult)
    }

    private fun isInFrame() = window.location != window.parent.location
}