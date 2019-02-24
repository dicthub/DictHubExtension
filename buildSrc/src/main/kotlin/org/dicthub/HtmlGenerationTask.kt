package org.dicthub

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.StringBuilder


data class Script(val src: String, val integrity: String? = null)

data class StyleSheet(val href: String, val integrity: String? = null)

open class HtmlGenerationTask() : DefaultTask() {

    lateinit var outputPath: String

    var title: String = ""

    var bodyId: String = ""

    var scripts: Collection<Script> = listOf()

    var styleSheets: Collection<StyleSheet> = listOf()


    @TaskAction
    fun renderHtml() {
        val sb = StringBuilder()
        sb.appendHTML().html {
            head {
                meta {
                    charset = "UTF-8"
                }
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1, shrink-to-fit=no"
                }
                title {
                    +this@HtmlGenerationTask.title
                }
                styleSheets.forEach {
                    link {
                        rel = "stylesheet"
                        href = it.href
                        it.integrity?.let { integrity = it }
                    }
                }
                scripts.forEach {
                    script {
                        src = it.src
                        it.integrity?.let { integrity = it }
                    }
                }
            }

            body {
                id = bodyId
            }
        }

        val f = File(outputPath)
        f.writeText(sb.toString())
    }

}