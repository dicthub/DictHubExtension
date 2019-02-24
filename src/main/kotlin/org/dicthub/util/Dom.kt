package org.dicthub.util

import org.w3c.dom.Element
import kotlin.browser.document

fun <T : Element> getElementById(id: String): T =
        document.getElementById(id) as? T ?: throw IllegalStateException("Can not find element $id")
