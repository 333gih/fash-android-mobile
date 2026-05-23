package com.pc.fash_android_mobile.ui.onboarding

import java.util.Locale
import kotlin.math.round

/**
 * Typical body measurement ranges per letter size (cm).
 * Used as onboarding guidelines — users rarely measure themselves; we show reference bands instead.
 */
data class SizingMeasurementGuide(
    val chestCm: ClosedFloatingPointRange<Double>,
    val waistCm: ClosedFloatingPointRange<Double>,
    val lengthCm: ClosedFloatingPointRange<Double>,
    val shouldersCm: ClosedFloatingPointRange<Double>,
    val sleeveCm: ClosedFloatingPointRange<Double>,
)

private val womenGuides: Map<String, SizingMeasurementGuide> = mapOf(
    "XXS" to SizingMeasurementGuide(74.0..78.0, 56.0..60.0, 58.0..62.0, 34.0..36.0, 56.0..58.0),
    "XS" to SizingMeasurementGuide(78.0..82.0, 60.0..64.0, 60.0..64.0, 36.0..38.0, 57.0..59.0),
    "S" to SizingMeasurementGuide(82.0..86.0, 64.0..68.0, 62.0..66.0, 38.0..40.0, 58.0..60.0),
    "M" to SizingMeasurementGuide(86.0..90.0, 68.0..72.0, 64.0..68.0, 40.0..42.0, 59.0..61.0),
    "L" to SizingMeasurementGuide(90.0..96.0, 72.0..78.0, 66.0..70.0, 42.0..44.0, 60.0..62.0),
    "XL" to SizingMeasurementGuide(96.0..102.0, 78.0..84.0, 68.0..72.0, 44.0..46.0, 61.0..63.0),
    "XXL" to SizingMeasurementGuide(102.0..108.0, 84.0..90.0, 70.0..74.0, 46.0..48.0, 62.0..64.0),
)

private val menGuides: Map<String, SizingMeasurementGuide> = mapOf(
    "XS" to SizingMeasurementGuide(84.0..88.0, 70.0..74.0, 66.0..70.0, 40.0..42.0, 59.0..61.0),
    "S" to SizingMeasurementGuide(88.0..92.0, 74.0..78.0, 68.0..72.0, 42.0..44.0, 60.0..62.0),
    "M" to SizingMeasurementGuide(92.0..96.0, 78.0..82.0, 70.0..74.0, 44.0..46.0, 61.0..63.0),
    "L" to SizingMeasurementGuide(96.0..100.0, 82.0..86.0, 72.0..76.0, 46.0..48.0, 62.0..64.0),
    "XL" to SizingMeasurementGuide(100.0..106.0, 86.0..92.0, 74.0..78.0, 48.0..50.0, 63.0..65.0),
    "XXL" to SizingMeasurementGuide(106.0..112.0, 92.0..98.0, 76.0..80.0, 50.0..52.0, 64.0..66.0),
    "XXXL" to SizingMeasurementGuide(112.0..118.0, 98.0..104.0, 78.0..82.0, 52.0..54.0, 65.0..67.0),
)

fun sizingGuideForGender(genderPreference: String): Map<String, SizingMeasurementGuide> =
    if (genderPreference.equals("men", ignoreCase = true)) menGuides else womenGuides

fun standardReferenceSizes(genderPreference: String): List<String> =
    sizingGuideForGender(genderPreference).keys.toList()

fun lookupSizingGuide(referenceSize: String, genderPreference: String): SizingMeasurementGuide? {
    val key = referenceSize.trim().uppercase(Locale.ROOT)
    return sizingGuideForGender(genderPreference)[key]
}

fun isStandardReferenceSize(referenceSize: String, genderPreference: String): Boolean =
    lookupSizingGuide(referenceSize, genderPreference) != null

private fun cmToIn(cm: Double): Double = cm / 2.54

fun formatMeasurementRangeCm(range: ClosedFloatingPointRange<Double>, unit: String): String {
    val useIn = unit.equals("in", ignoreCase = true)
    val start = if (useIn) cmToIn(range.start) else range.start
    val end = if (useIn) cmToIn(range.endInclusive) else range.endInclusive
    val unitLabel = if (useIn) "in" else "cm"
    return if (useIn) {
        String.format(Locale.getDefault(), "%.0f–%.0f %s", start, end, unitLabel)
    } else {
        String.format(Locale.getDefault(), "%.0f–%.0f %s", start, end, unitLabel)
    }
}

fun midpointMeasurementValue(range: ClosedFloatingPointRange<Double>, unit: String): String {
    val midCm = (range.start + range.endInclusive) / 2.0
    if (unit.equals("in", ignoreCase = true)) {
        val inches = cmToIn(midCm)
        return String.format(Locale.US, "%.1f", inches)
    }
    return round(midCm).toInt().toString()
}

fun typicalMeasurementsForSize(referenceSize: String, genderPreference: String, unit: String): TypicalMeasurements? {
    val guide = lookupSizingGuide(referenceSize, genderPreference) ?: return null
    return TypicalMeasurements(
        chest = midpointMeasurementValue(guide.chestCm, unit),
        waist = midpointMeasurementValue(guide.waistCm, unit),
        length = midpointMeasurementValue(guide.lengthCm, unit),
        shoulders = midpointMeasurementValue(guide.shouldersCm, unit),
        sleeve = midpointMeasurementValue(guide.sleeveCm, unit),
    )
}

data class TypicalMeasurements(
    val chest: String,
    val waist: String,
    val length: String,
    val shoulders: String,
    val sleeve: String,
)

/** Ideal height/weight band per letter size — used to infer size before the user measures garments. */
private data class SizeBodyProfile(
    val heightMidCm: Int,
    val heightHalfRangeCm: Int,
    val weightMidKg: Double,
    val weightHalfRangeKg: Double,
)

enum class SizeRecommendationConfidence {
    High, Medium, Low,
}

data class SizeRecommendation(
    val primarySize: String,
    val alternateSize: String? = null,
    val confidence: SizeRecommendationConfidence,
    val usedHeight: Boolean,
    val usedWeight: Boolean,
)

private val womenBodyProfiles: Map<String, SizeBodyProfile> = mapOf(
    "XXS" to SizeBodyProfile(152, 5, 43.0, 4.0),
    "XS" to SizeBodyProfile(156, 4, 47.0, 4.5),
    "S" to SizeBodyProfile(160, 4, 52.0, 5.0),
    "M" to SizeBodyProfile(165, 4, 57.0, 5.5),
    "L" to SizeBodyProfile(170, 4, 63.0, 6.0),
    "XL" to SizeBodyProfile(175, 5, 70.0, 6.5),
    "XXL" to SizeBodyProfile(180, 5, 78.0, 7.0),
)

private val menBodyProfiles: Map<String, SizeBodyProfile> = mapOf(
    "XS" to SizeBodyProfile(165, 5, 58.0, 5.0),
    "S" to SizeBodyProfile(170, 4, 65.0, 6.0),
    "M" to SizeBodyProfile(175, 4, 72.0, 7.0),
    "L" to SizeBodyProfile(180, 4, 80.0, 8.0),
    "XL" to SizeBodyProfile(185, 5, 88.0, 9.0),
    "XXL" to SizeBodyProfile(190, 5, 98.0, 10.0),
    "XXXL" to SizeBodyProfile(195, 5, 108.0, 12.0),
)

private fun bodyProfilesForGender(genderPreference: String): Map<String, SizeBodyProfile> =
    if (genderPreference.equals("men", ignoreCase = true)) menBodyProfiles else womenBodyProfiles

private fun normalizedDistance(value: Double, mid: Double, halfRange: Double): Double =
    kotlin.math.abs(value - mid) / halfRange.coerceAtLeast(1.0)

private fun scoreSizeForBody(
    size: String,
    profile: SizeBodyProfile,
    heightCm: Int?,
    weightKg: Double?,
): Double? {
    val heightScore = heightCm?.let {
        normalizedDistance(it.toDouble(), profile.heightMidCm.toDouble(), profile.heightHalfRangeCm.toDouble())
    }
    val weightScore = weightKg?.let {
        normalizedDistance(it, profile.weightMidKg, profile.weightHalfRangeKg)
    }
    return when {
        heightScore != null && weightScore != null -> {
            val bmi = weightKg!! / ((heightCm!! / 100.0) * (heightCm / 100.0))
            val idealBmi = profile.weightMidKg /
                ((profile.heightMidCm / 100.0) * (profile.heightMidCm / 100.0))
            val bmiScore = normalizedDistance(bmi, idealBmi, 3.5)
            heightScore * 0.38 + weightScore * 0.42 + bmiScore * 0.20
        }
        heightScore != null -> heightScore
        weightScore != null -> weightScore
        else -> null
    }
}

/**
 * Suggest a letter size from body metrics. Works best with both height and weight;
 * single-metric input still returns a hint with lower confidence.
 */
fun recommendSizeFromBodyMetrics(
    heightCm: Int?,
    weightKg: Double?,
    genderPreference: String,
): SizeRecommendation? {
    val validHeight = heightCm?.takeIf { it in 100..250 }
    val validWeight = weightKg?.takeIf { it in 20.0..300.0 }
    if (validHeight == null && validWeight == null) return null

    val profiles = bodyProfilesForGender(genderPreference)
    val availableSizes = standardReferenceSizes(genderPreference)
    val scored = availableSizes.mapNotNull { size ->
        val profile = profiles[size] ?: return@mapNotNull null
        val score = scoreSizeForBody(size, profile, validHeight, validWeight) ?: return@mapNotNull null
        size to score
    }.sortedBy { it.second }
    if (scored.isEmpty()) return null

    val best = scored.first()
    val runnerUp = scored.getOrNull(1)
    val alternate = runnerUp?.takeIf { (size, score) ->
        score - best.second <= 0.35 && size != best.first
    }?.first

    val confidence = when {
        validHeight != null && validWeight != null && best.second <= 0.55 -> SizeRecommendationConfidence.High
        validHeight != null && validWeight != null && best.second <= 1.15 -> SizeRecommendationConfidence.Medium
        validHeight != null && validWeight != null -> SizeRecommendationConfidence.Low
        best.second <= 0.75 -> SizeRecommendationConfidence.Medium
        else -> SizeRecommendationConfidence.Low
    }

    return SizeRecommendation(
        primarySize = best.first,
        alternateSize = alternate,
        confidence = confidence,
        usedHeight = validHeight != null,
        usedWeight = validWeight != null,
    )
}

fun parseHeightCmInput(raw: String): Int? =
    raw.trim().toIntOrNull()?.takeIf { it in 100..250 }

fun parseWeightKgInput(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..300.0 }

fun measurementsAreBlank(
    chest: String,
    waist: String,
    length: String,
    shoulders: String,
    sleeve: String,
): Boolean = chest.isBlank() && waist.isBlank() && length.isBlank() &&
    shoulders.isBlank() && sleeve.isBlank()

fun applyTypicalMeasurementsForSize(
    referenceSize: String,
    genderPreference: String,
    measurementUnit: String,
    onChestChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onShouldersChange: (String) -> Unit,
    onSleeveChange: (String) -> Unit,
) {
    typicalMeasurementsForSize(referenceSize, genderPreference, measurementUnit)?.let { typical ->
        onChestChange(typical.chest)
        onWaistChange(typical.waist)
        onLengthChange(typical.length)
        onShouldersChange(typical.shoulders)
        onSleeveChange(typical.sleeve)
    }
}
