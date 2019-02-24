package org.dicthub.model

import org.dicthub.lang.Lang
import org.dicthub.lang.toLang
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

data class PluginInfo(val data: Json) {

    val id
        inline get() = data["id"] as String

    val name
        inline get() = data["name"] as String

    val version
        inline get() = data["version"] as String

    val contentUrl
        inline get() = data["contentUrl"] as String

    val description
        inline get() = data["description"] as String?

    val source
        inline get() = data["source"] as String?

    val sourceUrl
        inline get() = data["sourceUrl"] as String?

    val author
        inline get() = data["author"] as String?

    val authorUrl
        inline get() = data["authorUrl"] as String?

    val options: Map<String, PluginOptionConfig>
        inline get() {
            val options = data["options"]?.let { it as? Json } ?: return emptyMap()
            val keys = js("Object.keys(options)") as Array<String>
            return keys.mapNotNull { key -> options[key]?.let{it as? Json}?.let { PluginOptionConfig(it) }
                    ?.let { value -> key to value} }.toMap()
        }

    override fun equals(other: Any?): Boolean {
        val pluginInfo = other as? PluginInfo
        return id == pluginInfo?.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class PluginOptionConfig(val data: Json) {

    val type
        inline get() = data["type"] as String

    val default
        inline get() = data["default"] as String?

    val description
        inline get() = data["description"] as String?
}


typealias PluginOptions = Json

data class PluginContent(val data: Json) {

    constructor(id: String, version: String, content: String) : this(json(
            "id" to id,
            "version" to version,
            "content" to content
    ))

    val id: String
        inline get() = data["id"] as String

    val version: String
        inline get() = data["version"] as String

    val content: String
        inline get() = data["content"] as String
}

typealias Query = Json

fun Query.getText(): String = attribute("text")
fun Query.getFrom(): Lang = attribute<String>("from").toLang()
fun Query.getTo(): Lang = attribute<String>("to").toLang()

fun createQury(text: String, from: Lang, to: Lang) : Query = json(
        "text" to text,
        "from" to from.code,
        "to" to to.code
)

typealias Meta = Json

internal fun Meta.getName(): String = attribute("name")
internal fun Meta.getDescription(): String? = attribute("description")
internal fun Meta.getSource(): String? = attribute("source")
internal fun Meta.getSourceUrl(): String? = attribute("sourceUrl")
internal fun Meta.getAuthor(): String? = attribute("author")
internal fun Meta.getAuthorUrl(): String? = attribute("authorUrl")


interface TranslationProvider {

    @JsName("id")
    fun id(): String

    @JsName("meta")
    fun meta(): Meta

    @JsName("canTranslate")
    fun canTranslate(query: Query): Boolean

    @JsName("translate")
    fun translate(query: Query): Promise<String>

    @JsName("updateOptions")
    fun updateOptions(options: Json) { console.info("Update options for ${id()}", options) }
}

@Suppress("UNCHECKED_CAST")
private fun <T> Json.attribute(name: String): T {
    return this[name] as T
}