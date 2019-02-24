package org.dicthub.plugin

import StorageArea
import org.dicthub.model.PluginContent
import org.dicthub.model.PluginInfo
import org.dicthub.util.HttpAsyncClient
import org.dicthub.util.convertToJson
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json


private const val PLUGIN_PREFIX = "PLUGIN_CONTENT_"

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
class PluginContentAdapter(private val httpClient: HttpAsyncClient, private val storage: StorageArea) {

    fun load(pluginIds: Collection<String>) = Promise<Array<PluginContent>> { resolve, _ ->
        val storageKeys = pluginIds.map { getPluginContentStorageKey(it) }.toTypedArray()
        storage.get(storageKeys) { data ->
            val cachedPluginContents = storageKeys.map {  storageKey ->
                data[storageKey]?.let { convertCachedJson(it) }
            }.filterNotNull().toTypedArray()
            resolve(cachedPluginContents)
        }
    }

    private fun convertCachedJson(cachedData: Any) =
        cachedData.let { it as? Json }?.let {
            PluginContent(it)
        } ?: run {
            PluginContent(convertToJson(cachedData))
        }

    fun update(pluginInfo: PluginInfo): Promise<Boolean?> {

        return Promise { resolve, reject ->
            val storageKey = getPluginContentStorageKey(pluginInfo.id)
            storage.get(storageKey) {
                if (pluginInfo.version == it["version"]) {
                    resolve(null)
                } else {
                    httpClient.get(pluginInfo.contentUrl).then { html ->
                        val data = PluginContent(pluginInfo.id, pluginInfo.version, html)
                        storage.set(json(storageKey to data.data)) {
                            console.info("Saved plugin: ${pluginInfo.id}, ${pluginInfo.version}")
                            resolve(true)
                        }
                    }.catch(reject)
                }
            }
        }
    }


}

private fun getPluginContentStorageKey(id: String) = "$PLUGIN_PREFIX$id"