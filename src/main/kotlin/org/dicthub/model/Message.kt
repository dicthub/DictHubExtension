package org.dicthub.model

import kotlin.js.Json
import kotlin.js.json


enum class Command {
    USER_PREFERENCE,
    PLUGINS,
    QUERY,

    SANDBOX_START,
    SANDBOX_READY,
    TRANSLATION_RESULT
}


@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
data class Packet(val data: Json) {

    constructor(cmd: Command, payload: Json?) : this(json(
            "cmd" to cmd.name,
            "payload" to payload
    ))

    val cmd: Command = Command.valueOf(data["cmd"] as String)

    val payload: Json = data["payload"] as Json
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UNCHECKED_CAST")
class PluginContents(val data: Json) {

    constructor(plugins: Collection<Pair<PluginContent, PluginOptions>>) : this(json(
            "plugins" to plugins.map { arrayOf(it.first.data, it.second) }.toTypedArray()
    ))

    val plugins: Collection<Pair<PluginContent, PluginOptions>>
        inline get() = data["plugins"]?.let { it as? Array<Array<Json>> }?.map {
            Pair(PluginContent(it[0]), it[1])
        } ?: emptyList()
}


class TranslationResult(val data: Json) {

    constructor(pluginId: String, query: Query, success: Boolean, htmlContent: String) : this(json(
            "pluginId" to pluginId,
            "query" to query,
            "success" to success,
            "htmlContent" to htmlContent
    ))

    val pluginId
        inline get() = data["pluginId"] as String

    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    val query = data["query"] as Query

    val success = data["success"] as? Boolean ?: true

    val htmlContent
        inline get() = data["htmlContent"] as String
}