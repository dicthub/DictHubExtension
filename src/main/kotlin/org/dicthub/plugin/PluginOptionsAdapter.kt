package org.dicthub.plugin

import StorageArea
import org.dicthub.model.PluginOptions
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json


private const val PLUGIN_PREFIX = "PLUGIN_OPTIONS_"

class PluginOptionsAdapter(private val storage: StorageArea) {

    fun loadOptions(pluginIds: Collection<String>) = Promise<Map<String, PluginOptions?>>  { resolve, _ ->
        val storageKeys = pluginIds.map { getPluginOptionsStorageKey(it) }.toTypedArray()
        storage.get(storageKeys) { data ->
            val storedOptions = pluginIds.mapNotNull { key ->
                data[getPluginOptionsStorageKey(key)]?.let { it as? Json }?.let { value -> key to value } }.toMap()
            console.info("Loaded plugin options", storedOptions)
            resolve(storedOptions)
        }
    }

    fun saveOptionValue(pluginId: String, optionName: String, optionValue: String) = Promise<String> { resolve, _ ->
        storage.get(pluginId) {
            val currentData = it[pluginId]?.let { it as? Json } ?: json()
            currentData[optionName] = optionValue
            val newData = json( getPluginOptionsStorageKey(pluginId) to currentData )
            console.info("Store new option value for $pluginId, $optionName=$optionValue")
            storage.set(newData) {
                console.info("Store new data", newData)
                resolve(pluginId)
            }
        }
    }
}

fun getPluginOptionsStorageKey(id: String) = "$PLUGIN_PREFIX$id"
