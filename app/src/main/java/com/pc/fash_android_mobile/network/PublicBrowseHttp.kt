package com.pc.fash_android_mobile.network

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.locale.AppLocale
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * HTTP client for GET /api/v1/public/... — sends app attestation headers (not user JWT).
 * Requires [BuildConfig.PUBLIC_BROWSE_CLIENT_TOKEN] configured per environment.
 *
 * Paths intentionally **omit** `{vi|en}/` (same as common-service editorial `commonServicePath`).
 * Kong public routes match `/api/v1/public` only; locale is sent via headers/query, not path prefix.
 */
object PublicBrowseHttp {

    const val HEADER_CLIENT = "X-Fash-Public-Client"
    const val HEADER_CLIENT_TOKEN = "X-Fash-Public-Client-Token"

    fun isConfigured(): Boolean =
        BuildConfig.PUBLIC_BROWSE_CLIENT_ID.isNotBlank() &&
            BuildConfig.PUBLIC_BROWSE_CLIENT_TOKEN.isNotBlank()

    fun createClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val locale = AppLocale.coreApiPathSegment()
                val req = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("Accept-Language", locale)
                    .header("X-Fash-Lang", locale)
                    .header("User-Agent", "FashAndroid/1.0")
                    .header(HEADER_CLIENT, BuildConfig.PUBLIC_BROWSE_CLIENT_ID)
                    .header(HEADER_CLIENT_TOKEN, BuildConfig.PUBLIC_BROWSE_CLIENT_TOKEN)
                    .build()
                chain.proceed(req)
            }
        return builder.build()
    }

    fun publicApiPath(relativePath: String): String =
        AppEnvironment.apiPathWithoutLocale("api/v1/public/${relativePath.trimStart('/')}")
}
