package com.pc.fash_android_mobile.data.user

/** Strips leading @ so Explore autocomplete "@username" matches GET /users/search. */
fun normalizePeopleSearchQuery(raw: String): String {
    var q = raw.trim()
    while (q.startsWith("@")) {
        q = q.removePrefix("@").trim()
    }
    return q
}
