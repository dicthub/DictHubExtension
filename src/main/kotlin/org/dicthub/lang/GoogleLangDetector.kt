package org.dicthub.lang

import org.dicthub.util.HttpAsyncClient
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.browser.localStorage
import kotlin.js.Json
import kotlin.js.Promise


class GoogleLangDetector(private val httpClient: HttpAsyncClient) : LangDetector {

    override fun detectLang(text: String): Promise<Lang> {
        return Promise { resolve, reject ->
            detectUsingCachedToken(text).then(resolve).catch {
                detectUsingNewToken(text).then(resolve).catch(reject)
            }
        }
    }

    private val tokenStorageKey = "extension-googletranslation-token"
    private fun detectUsingCachedToken(text: String): Promise<Lang> {
        return Promise { resolve, reject ->
            localStorage[tokenStorageKey]?.let { token ->
                console.info("Detect language using cached google token $token")
                translateWithToken(token, text).then { resolve(it) }.catch(reject)
            } ?: run {
                reject(IllegalStateException("No cached google token available"))
            }
        }
    }

    private fun detectUsingNewToken(text: String): Promise<Lang> {
        return Promise { resolve, reject ->
            getGTranslateToken().then { token ->
                console.info("Detect language using new google token $token")
                localStorage[tokenStorageKey] = token
                translateWithToken(token, text).then { resolve(it) }.catch(reject)
            }.catch(reject)
        }
    }


    private fun translateWithToken(token: String, text: String) = Promise<Lang> { resolve, reject ->
        val tk = hash(text, token) as String
        val requestUrl = queryUrl(tk, text)
        httpClient.get(requestUrl).then { resolve(extractSourceLang(it)) }.catch(reject)
    }


    private fun extractSourceLang(rawContent: String): Lang {
        val data = JSON.parse<Array<Json>>(rawContent)
        val langCode = data.getOrNull(2).toString()
        return fromCode(langCode) ?: throw IllegalStateException("Unknown lang code from GoogleTranslation detection")
    }


    private fun getGTranslateToken(): Promise<String> {

        return Promise { resolve, reject ->
            httpClient.get(baseUrl()).then { html ->
                val regex = Regex("TKK='(\\d+\\.\\d+)'|tkk:'(\\d+\\.\\d+)'")
                val match = regex.find(html)
                match?.groups?.lastOrNull()?.value?.let { resolve(it) } ?: run {
                    reject(IllegalStateException("Failed when getting google token"))
                }
            }.catch { reject(it) }
        }
    }

    private fun baseUrl() = DOMAIN_TRANSLATE_GOOGLE_COM

    private fun queryUrl(token: String, text: String) = "${baseUrl()}/translate_a/single?client=webapp" +
            "&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&pc=1&otf=1&ssel=0&tsel=0&kc=1" +
            "&sl=auto&tl=en&hl=en&tk=$token&q=${encodeURIComponent(text)}"
}

private const val DOMAIN_TRANSLATE_GOOGLE_COM = "https://translate.google.com"

private val hash = js("function sM(r,t){var n;if(null!==yr)n=yr;else{n=wr(String.fromCharCode(84));var e=wr(String.fromCharCode(75));(n=[n(),n()])[1]=e(),n=(yr=t||\"\")||\"\"}var a=wr(String.fromCharCode(116));e=wr(String.fromCharCode(107));(a=[a(),a()])[1]=e(),e=\"&\"+a.join(\"\")+\"=\",a=n.split(\".\"),n=Number(a[0])||0;for(var o=[],h=0,f=0;f<r.length;f++){var u=r.charCodeAt(f);128>u?o[h++]=u:(2048>u?o[h++]=u>>6|192:(55296==(64512&u)&&f+1<r.length&&56320==(64512&r.charCodeAt(f+1))?(u=65536+((1023&u)<<10)+(1023&r.charCodeAt(++f)),o[h++]=u>>18|240,o[h++]=u>>12&63|128):o[h++]=u>>12|224,o[h++]=u>>6&63|128),o[h++]=63&u|128)}for(r=n,h=0;h<o.length;h++)r+=o[h],r=xr(r,\"+-a^+6\");return r=xr(r,\"+-3^+b+-f\"),0>(r^=Number(a[1])||0)&&(r=2147483648+(2147483647&r)),e+((r%=1e6).toString()+\".\")+(r^n)}var yr=null,wr=function(r){return function(){return r}},xr=function(r,t){for(var n=0;n<t.length-2;n+=3){var e=\"a\"<=(e=t.charAt(n+2))?e.charCodeAt(0)-87:Number(e);e=\"+\"==t.charAt(n+1)?r>>>e:r<<e;r=\"+\"==t.charAt(n)?r+e&4294967295:r^e}return r};sM")

private external fun encodeURIComponent(str: String): String