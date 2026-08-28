package com.pc.fash_android_mobile.data.entitlements

data class UserEntitlementSummary(
    val packageId: String = "",
    val packageCode: String = "",
    val packageName: String = "",
    val features: Map<String, FeatureUsageSummary> = emptyMap(),
)

data class FeatureUsageSummary(
    val enabled: Boolean = false,
    val featureGroup: String = "",
    val executionKind: String = "",
    val name: String = "",
    val description: String = "",
    val requiresListing: Boolean = false,
    val used: Long = 0,
    val remaining: Long? = null,
    val unlimited: Boolean = false,
    val durationDays: Int = 0,
    val priority: Boolean = false,
)

data class PackageActivationResult(
    val packageCode: String = "",
    val packageName: String = "",
    val entitlements: UserEntitlementSummary? = null,
)
