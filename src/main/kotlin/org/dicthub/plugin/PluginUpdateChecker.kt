package org.dicthub.plugin

import org.dicthub.model.PluginInfo
import org.dicthub.model.UserPreference
import org.w3c.dom.get
import kotlin.browser.localStorage
import kotlin.js.Date
import kotlin.js.Promise


data class PluginUpdateResult(
        val newPlugins: List<PluginInfo>,
        val upgradablePlugins: List<PluginInfo>
)


class PluginUpdateChecker(private val pluginIndex: PluginIndex, private val userPreference: UserPreference) {

    private val LAST_CHECK_TIME = "lastCheckTime"
    private val LAST_AVAILABLE_PLUGINS = "lastAvailablePlugins"

    fun getLastCheckTime(): Long {
        return localStorage[LAST_CHECK_TIME]?.toLong() ?: 0
    }

    fun check(): Promise<PluginUpdateResult> {

        return Promise { resolve, reject ->
            pluginIndex.load().then { latestPluginInfo ->

                val lastPluginIds = localStorage.getItem(LAST_AVAILABLE_PLUGINS)?.split(",")?.toSet()

                val newPlugins = latestPluginInfo.filterNot { lastPluginIds?.contains(it.id) ?: true }

                val enabledPluginVersions = userPreference.enabledPlugins.map { it.id to it.version }.toMap()
                val upgradablePlugins = latestPluginInfo.filter { enabledPluginVersions.containsKey(it.id) && enabledPluginVersions.get(it.id) != it.version}

                val latestPluginIdStr = latestPluginInfo.map { it.id }.joinToString(",")
                localStorage.setItem(LAST_AVAILABLE_PLUGINS, latestPluginIdStr)
                localStorage.setItem(LAST_CHECK_TIME, Date().getTime().toString())

                resolve(PluginUpdateResult(newPlugins = newPlugins, upgradablePlugins = upgradablePlugins))
            }.catch(reject)
        }
    }
}