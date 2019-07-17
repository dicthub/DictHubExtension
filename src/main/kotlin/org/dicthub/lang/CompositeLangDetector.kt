package org.dicthub.lang

import kotlin.js.Promise


fun compositeLangDetector(vararg detectors: LangDetector): LangDetector {
    return object : LangDetector {
        override fun detectLang(text: String): Promise<Lang> {
            val rejections = mutableListOf<Throwable>()
            return Promise { resolve, reject ->
                detectors.forEach { detector ->
                    detector.detectLang(text).then(resolve).catch {
                        console.warn("Failure in lang detection: ", it)
                        rejections.add(it)
                        if (rejections.size == detectors.size) {
                            reject(it)
                        }
                    }
                }
            }
        }
    }
}