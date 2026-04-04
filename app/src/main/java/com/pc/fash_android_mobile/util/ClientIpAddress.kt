package com.pc.fash_android_mobile.util

import java.net.NetworkInterface
import java.util.Collections

/**
 * Best-effort local IPv4 for auth refresh body (not the public/WAN address).
 * Empty string when unavailable — server may still accept the request.
 */
object ClientIpAddress {
    fun localIpv4OrEmpty(): String = runCatching {
        Collections.list(NetworkInterface.getNetworkInterfaces()).asSequence()
            .flatMap { ni -> Collections.list(ni.inetAddresses).asSequence() }
            .filter { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
            .firstOrNull()?.hostAddress
    }.getOrNull().orEmpty()
}
