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
