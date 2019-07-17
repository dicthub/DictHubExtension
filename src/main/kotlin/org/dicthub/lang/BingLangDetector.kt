package org.dicthub.lang

import org.dicthub.util.HttpAsyncClient
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.dom.url.URL
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.localStorage
import kotlin.js.Json
import kotlin.js.Promise

class BingLangDetector(private val httpAsyncClient: HttpAsyncClient) : LangDetector {

    override fun detectLang(text: String): Promise<Lang> {

        val detectLangWithNewContext = fun(resolve: (Lang) -> Unit, reject: (Throwable) -> Unit) {
            getBingContext().then { newContext ->
                saveContextToCache(newContext)
                console.info("Detect lang use new context $newContext")
                detectLangWithContext(newContext, text).then(resolve).catch(reject)
            }
        }

        return Promise { resolve, reject ->

            loadCachedContext()?.let { cachedContext ->
                console.info("Detect lang use cached context $cachedContext")
                detectLangWithContext(cachedContext, text).then(resolve).catch {
                    detectLangWithNewContext(resolve, reject)
                }
            } ?: run {
                detectLangWithNewContext(resolve, reject)
            }
        }
    }

    private fun detectLangWithContext(context: BingContext, text: String): Promise<Lang> {
        return Promise { resolve, reject ->
            callBingDetectApi(context, text).then { resolve(parseBingResponse(it)) }.catch(reject)
        }
    }

    private fun callBingDetectApi(context: BingContext, text: String): Promise<String> {
        val url = "https://${context.domain}/ttranslatev3?isVertical=1&IG=${context.token}&IID=translator.5037.1"
        return httpAsyncClient.post(url, mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                "&fromLang=auto-detect&text=${encodeURIComponent(text)}&to=en")
    }

    private fun parseBingResponse(result: String): Lang {
        return JSON.parse<Array<Json>>(result).getOrNull(0)?.get("detectedLanguage")?.let { it as? Json }
                ?.get("language")?.let { convertBingLangCodeToLang(it.toString()) } ?: throw IllegalStateException("No lang detected")
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

    private val domainStorageKey = "extension-bing-domain"
    private val tokenStorageKey = "extension-bing-token"

    private fun loadCachedContext(): BingContext? {
        val domain = localStorage[domainStorageKey] ?: return null
        val token = localStorage[tokenStorageKey] ?: return null
        return BingContext(domain = domain, token = token)
    }

    private fun saveContextToCache(context: BingContext) {
        localStorage[domainStorageKey] = context.domain
        localStorage[tokenStorageKey] = context.token
    }

    private fun getBingContext(): Promise<BingContext> {
        return Promise { resolve, reject ->
            val xhr = XMLHttpRequest()
            xhr.addEventListener("load", { _ ->
                val url = URL(xhr.responseURL)
                val htmlContent = xhr.responseText
                iggRegex.find(htmlContent)?.groupValues?.get(1)?.let { token ->
                    resolve(BingContext(domain = url.hostname, token = token))
                } ?: run {
                    reject(IllegalStateException("No IG found in html content"))
                }
            })
            xhr.addEventListener("error", { event ->
                reject(IllegalStateException("Failed to fetch url, error event: $event"))
            })
            xhr.open("GET", DEFAULT_URL)
            xhr.send()
        }
    }

    private val DEFAULT_URL = "https://www.bing.com/translator"
    private val iggRegex = Regex("IG:\"([\\w\\d]+)\"")

    data class BingContext(
            val domain: String,
            val token: String
    )
}

private external fun encodeURIComponent(text: String): String