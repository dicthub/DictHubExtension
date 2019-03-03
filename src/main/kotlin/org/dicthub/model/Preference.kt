package org.dicthub.model

import browserObj
import org.dicthub.lang.Lang
import org.dicthub.lang.fromCode
import org.dicthub.util.convertToJson
import kotlin.js.*

const val KEY_USER_PREFERENCE = "userPreference"

const val DEFAULT_MAX_TRANSLATION_RESULTS = 3
const val DEFAULT_PLUGIN_INDEX_URL = "https://raw.githubusercontent.com/dicthub/DictHubPluginRepository/prod/index.json"

const val CURRENT_USER_PREFERENCE_VERSION = "1.0"

@Suppress("UNCHECKED_CAST")
class UserPreference(val data: Json) {

    val version = data["version"]?.let { it as? String } ?: CURRENT_USER_PREFERENCE_VERSION

    var primaryLang: Lang
        inline get() = data["primaryLang"]?.let { it as? String }?.let { fromCode(it) } ?: Lang.EN
        inline set(value) {
            data["primaryLang"] = value.code
            saveUserPreference(this)
        }

    var maxTranslationResult: Int
        inline get() = data["maxTranslationResult"]?.let { it as? Int } ?: DEFAULT_MAX_TRANSLATION_RESULTS
        inline set(value) {
            data["maxTranslationResult"] = value
            saveUserPreference(this)
        }

    var enabledPlugins: Set<PluginInfo>
        inline get() = data["enabledPlugins"]?.let { it as? Array<Json> }?.map { PluginInfo(it) }?.toSet() ?: setOf()
        inline set(value) {
            data["enabledPlugins"] = value.map { it.data }.toTypedArray()
            saveUserPreference(this)
        }

    var pluginRepository: List<String>
        inline get() {
            val storedList = data["pluginRepository"]?.let { it as? Array<*> }?.filterIsInstance<String>()
            return if (storedList?.isNotEmpty() == true) storedList else listOf(DEFAULT_PLUGIN_INDEX_URL)

        }
        inline set(value) {
            data["pluginRepository"] = value.toTypedArray()
            saveUserPreference(this)
        }

    var pluginPriority: List<String>
        inline get() = data["pluginPriority"]?.let { it as? Array<*> }?.filterIsInstance<String>() ?: emptyList()
        inline set(value) {
            data["pluginPriority"] = value.toTypedArray()
            saveUserPreference(this)
        }

    var sendAnalysisInfo: Boolean
        inline get() = data["sendAnalysisInfo"]?.let { it as? Boolean } ?: false
        inline set(value) {
            data["sendAnalysisInfo"] = value
            saveUserPreference(this)
        }
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun loadUserPreference(): Promise<UserPreference> {

    return Promise { resolve, _ ->

        val storage = browserObj.storage.local
        storage.get(KEY_USER_PREFERENCE) { storedData ->
            console.info("Loaded user preference", storedData)
            storedData[KEY_USER_PREFERENCE]?.let { prefData ->
                prefData.let { it as? Json }?.let {
                   resolve(UserPreference(it))
                }?:run {
                    val json = convertToJson(prefData);
                    console.info("Converted preference data to json", json)
                    resolve(UserPreference(json))
                }
            } ?: run {
                console.info("No user preference found.")
                resolve(UserPreference(json(
                        "version" to CURRENT_USER_PREFERENCE_VERSION
                )))
            }
        }
    }
}

fun saveUserPreference(pref: UserPreference) {

    val storage = browserObj.storage.local
    console.info("Storing model:", pref.data)

    storage.set(json(
            KEY_USER_PREFERENCE to pref.data
    )) {}
}
