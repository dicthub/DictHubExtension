package org.dicthub.util

import kotlin.js.Json
import kotlin.js.json

fun convertToJson(obj: Any): Json {
    // This is to fix the mismatch when get local stored data.
    val json = json()
    for (entry in js("Object.entries(obj)")) {
        json[entry[0]] = entry[1]
    }
    return json
}