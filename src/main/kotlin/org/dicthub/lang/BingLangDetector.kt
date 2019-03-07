package org.dicthub.lang

import browserObj
import org.dicthub.util.HttpAsyncClient
import kotlin.js.Promise
import kotlin.js.json

class BingLangDetector(private val httpAsyncClient: HttpAsyncClient) : LangDetector {

    override fun detectLang(text: String): Promise<Lang> {
        return Promise { resolve, reject ->
            detectLangUsingCachedToken(text).then(resolve).catch {
                detectLangUsingNewToken(text).then(resolve).catch(reject)
            }
        }
    }

    private fun detectLangUsingCachedToken(text: String): Promise<Lang> {
        return Promise { resolve, reject ->
            cachedToken().then { igg ->
                callBingDetectApi(igg, text).then { code ->
                    convertBingLangCodeToLang(code)
                            ?.let(resolve)
                            ?:reject(IllegalArgumentException("Failed to convert bing lang code $code"))
                }.catch(reject)
            }.catch(reject)
        }
    }

    private fun detectLangUsingNewToken(text: String): Promise<Lang> {
        return Promise { resolve, reject ->
            newToken().then { igg ->
                callBingDetectApi(igg, text).then { code ->
                    convertBingLangCodeToLang(code)
                            ?.let(resolve)
                            ?:reject(IllegalArgumentException("Failed to convert bing lang code $code"))
                }.catch(reject)
            }.catch(reject)
        }
    }

    private fun callBingDetectApi(igg: String, text: String): Promise<String> {
        val url = "https://www.bing.com/tdetect?&IG=$igg&IID=translator.5037.1"
        return httpAsyncClient.post(url, mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                "&text=${encodeURIComponent(text)}")
    }

    private val tokenStorageKey = "bing_token_igg";
    private fun cachedToken(): Promise<String> = Promise { resolve, reject ->
        browserObj.storage.local.get(tokenStorageKey) { data ->
            data[tokenStorageKey]?.let { it as? String }
                    ?.let { igg ->
                        console.info("Loaded bing lang detection token $igg")
                        resolve(igg)
                    }
                    ?: reject(IllegalStateException("No cached token"))
        }
    }

    private val iggRegex = Regex("IG:\"([\\w\\d]+)\"")

    private fun newToken(): Promise<String> = Promise { resolve, reject ->
            httpAsyncClient.get("https://www.bing.com/translator").then { htmlContent ->
                iggRegex.find(htmlContent)?.groupValues?.get(1)
                        ?.let { igg ->
                            browserObj.storage.local.set(json( tokenStorageKey to igg )) {
                                console.info("Cached bing lang detection token $igg")
                            }
                            resolve(igg)
                        }
                        ?: run {
                            reject(IllegalStateException("No IG found in html content"))
                        }
            }.catch(reject)
    }

    private val knownBingLangMap = mapOf(
            "zh-CHS" to Lang.ZH_CN,
            "zh-CHT" to Lang.ZH_TW
    )
    private fun convertBingLangCodeToLang(code: String): Lang? {
        // Most cases direct match
        return fromCode(code)
                ?: code.match("(.*)-(.*)")?.firstOrNull()?.let { fromCode(it) }
                ?: knownBingLangMap[code]
    }

}

private external fun encodeURIComponent(text: String): String