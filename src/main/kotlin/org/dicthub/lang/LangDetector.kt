package org.dicthub.lang

import kotlin.js.Promise

/**
 * Detect the language of the text
 */
interface LangDetector {

    fun detectLang(text: String): Promise<Lang>
}