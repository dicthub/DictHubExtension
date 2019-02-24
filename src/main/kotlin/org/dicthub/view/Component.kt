package org.dicthub.view

import kotlinx.html.FlowContent


interface Component {

    fun render()
}


typealias TagAppender = (FlowContent.() -> Unit)

