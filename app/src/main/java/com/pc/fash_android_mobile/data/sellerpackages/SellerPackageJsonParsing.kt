package com.pc.fash_android_mobile.data.sellerpackages

import org.json.JSONObject

/**
 * Core wire may send booleans as JSON bool, string ("true"/"false"), or 0/1.
 * [JSONObject.optBoolean] only accepts JSON boolean literals.
 */
internal fun parseJsonBoolean(value: Any?): Boolean? = when (value) {
    is Boolean -> value
    is Number -> value.toInt() != 0
    is String -> {
        when (value.trim().lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
    }
    else -> null
}

internal fun JSONObject.wireBoolean(vararg keys: String, default: Boolean = false): Boolean {
    for (key in keys) {
        if (!has(key) || isNull(key)) continue
        parseJsonBoolean(get(key))?.let { return it }
    }
    return default
}

internal fun JSONObject.wireReleasedFlag(): Boolean {
    val topKeys = arrayOf("is_released", "isReleased", "IsReleased")
    if (topKeys.any { has(it) && !isNull(it) }) {
        return wireBoolean(*topKeys, default = false)
    }
    val metaRaw = optString("metadata").trim()
    if (metaRaw.isNotEmpty()) {
        runCatching {
            val meta = JSONObject(metaRaw)
            if (meta.has("is_released") || meta.has("isReleased")) {
                return meta.wireBoolean("is_released", "isReleased", default = false)
            }
        }
    }
    return false
}
