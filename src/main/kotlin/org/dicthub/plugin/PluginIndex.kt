package org.dicthub.plugin

import org.dicthub.model.PluginInfo
import org.dicthub.util.HttpAsyncClient
import kotlin.js.Json
import kotlin.js.Promise



class PluginIndex(private val httpClient: HttpAsyncClient, private val repositories: List<String>) {

    fun load(): Promise<List<PluginInfo>> {

        val promises = repositories.map { url ->
            Promise<List<PluginInfo>> { resolve, reject ->
                httpClient.get(url).then {
                    val plugins = JSON.parse<Array<Json>>(it)
                    val result = plugins.map { PluginInfo(it) }
                    resolve(result)
                }.catch(reject)
            }
        }.toTypedArray()

        return Promise { resolve, reject ->
            Promise.all(promises).then { plugins ->
                resolve(plugins.flatMap { it }.reversed().associateBy { it.id }.values.toList())
            }.catch(reject)
        }
    }
}
