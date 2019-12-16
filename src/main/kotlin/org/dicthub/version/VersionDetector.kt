package org.dicthub.version

import browserObj
import isChrome
import isFirefox
import org.dicthub.util.AjaxHttpClient
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.browser.localStorage
import kotlin.js.Json
import kotlin.js.Promise

/**
 * An interface abstracts the source of plugin version
 */
object VersionDetector {

    private const val LAST_Extension_VERSION_CHECK_TIME = "LastExtensionVersionCheckTime"

    private const val chromeExtensionUrl = "https://chrome.google.com/webstore/detail/dicthub/cibocdpeaeganigafnnofchcliihpchn"
    private const val firefoxExtensionUrl = "https://addons.mozilla.org/en-US/firefox/addon/dicthub/"

    fun getExtensionUrl(): String {
        return if (isFirefox()) firefoxExtensionUrl else chromeExtensionUrl
    }

    fun getCurrentVersion(): String {
        return browserObj.runtime.getManifest()["version"].toString()
    }

    fun getPublishedVersion(): Promise<String> {
        val url = "https://dicthub.org/dicthub.versions.published.json"
        return Promise { resolve, reject ->
            AjaxHttpClient.get(url).then { jsonStr ->
                val json = JSON.parse<Json>(jsonStr)
                val key = if (isFirefox()) "firefox" else "chrome"
                json[key]?.let { resolve(it.toString()) } ?: reject(IllegalStateException("No version found"))
            }.catch(reject)
        }
    }

    fun getLastCheckTime(): Long {
        return localStorage.getItem(LAST_Extension_VERSION_CHECK_TIME)?.toLong() ?: 0
    }

    fun setLastCheckTime(time: Long) {
        localStorage.setItem(LAST_Extension_VERSION_CHECK_TIME, time.toString())
    }

    fun hasNewVersion(newVersion: String, currentVersion: String): Boolean {
        if (newVersion == currentVersion) {
            return false
        }
        return Version(newVersion) > Version(currentVersion)
    }

    private data class Version(
            val major: Int,
            val minor: Int,
            val suffix: Int
    ) : Comparable<Version> {

        constructor(version: String) : this(
                version.split(".")[0].toInt(),
                version.split(".")[1].toInt(),
                version.split(".")[2].toInt()
        )

        override fun compareTo(other: Version): Int {
            return this.major.compareTo(other.major) * 10000 +
                    this.minor.compareTo(other.minor) * 100 +
                    this.suffix.compareTo(other.suffix)
        }
    }
}