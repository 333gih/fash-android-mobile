package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pc.fash_android_mobile.data.model.ListingWithMatch
import com.pc.fash_android_mobile.data.model.SizeMatchBadge
import kotlin.math.abs

/**
 * Modal showing detailed measurements for a listing with size match context
 */
@Composable
fun MeasurementPreviewModal(
    item: ListingWithMatch,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            MeasurementPreviewContent(
                item = item,
                onDismiss = onDismiss,
                onAddToCart = onAddToCart
            )
        }
    }
}

@Composable
private fun MeasurementPreviewContent(
    item: ListingWithMatch,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MeasurementTab.GARMENT) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Size & Fit Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
        
        // Size match badge (if available)
        item.sizeMatch?.let { sizeMatch ->
            SizeMatchInfoCard(sizeMatch)
        }
        
        // Tab selector
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            MeasurementTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) }
                )
            }
        }
        
        // Tab content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                MeasurementTab.GARMENT -> GarmentMeasurementsView(
                    measurements = item.measurements ?: emptyMap(),
                    listingSize = item.listing.size
                )
                MeasurementTab.YOUR_PROFILE -> YourProfileMeasurementsView()
                MeasurementTab.COMPARISON -> ComparisonView(
                    garmentMeasurements = item.measurements ?: emptyMap(),
                    sizeMatch = item.sizeMatch
                )
            }
        }
        
        Divider()
        
        // CTA buttons
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddToCart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Cart", fontWeight = FontWeight.SemiBold)
            }
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue Shopping")
            }
        }
    }
}

@Composable
private fun SizeMatchInfoCard(sizeMatch: com.pc.fash_android_mobile.data.model.SizeMatchInfo) {
    val badge = SizeMatchBadge.fromBadge(sizeMatch.badge)
    val (icon, color) = when (badge) {
        SizeMatchBadge.YOUR_SIZE -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        SizeMatchBadge.CLOSE_FIT -> Icons.Default.Info to Color(0xFF2196F3)
        SizeMatchBadge.SIZE_UP -> Icons.Default.KeyboardArrowUp to Color(0xFFFF9800)
        SizeMatchBadge.SIZE_DOWN -> Icons.Default.KeyboardArrowDown to Color(0xFFFF9800)
        null -> Icons.Default.Help to Color.Gray
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = badge?.displayText ?: sizeMatch.badge,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = sizeMatch.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "${(sizeMatch.confidence * 100).toInt()}% match",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GarmentMeasurementsView(
    measurements: Map<String, Double>,
    listingSize: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listingSize?.let { size ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Listed Size:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Divider()
        }
        
        if (measurements.isEmpty()) {
            EmptyMeasurementsPlaceholder()
        } else {
            measurements.entries.sortedBy { it.key }.forEach { (key, value) ->
                MeasurementRow(
                    label = formatMeasurementKey(key),
                    value = "${value.toInt()} cm"
                )
            }
        }
    }
}

@Composable
private fun YourProfileMeasurementsView() {
    var profileMeasurements by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    LaunchedEffect(Unit) {
        // TODO: Load from user profile
        profileMeasurements = emptyMap()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (profileMeasurements.isEmpty()) {
            EmptyProfilePlaceholder()
        } else {
            profileMeasurements.entries.sortedBy { it.key }.forEach { (key, value) ->
                MeasurementRow(
                    label = formatMeasurementKey(key),
                    value = "${value.toInt()} cm"
                )
            }
        }
    }
}

@Composable
private fun ComparisonView(
    garmentMeasurements: Map<String, Double>,
    sizeMatch: com.pc.fash_android_mobile.data.model.SizeMatchInfo?
) {
    var profileMeasurements by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    LaunchedEffect(Unit) {
        // TODO: Load from user profile
        profileMeasurements = emptyMap()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (profileMeasurements.isEmpty()) {
            EmptyComparisonPlaceholder()
        } else {
            garmentMeasurements.entries.sortedBy { it.key }.forEach { (key, garmentValue) ->
                profileMeasurements[key]?.let { profileValue ->
                    ComparisonRow(
                        label = formatMeasurementKey(key),
                        garmentValue = garmentValue,
                        profileValue = profileValue
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    garmentValue: Double,
    profileValue: Double
) {
    val difference = garmentValue - profileValue
    val absDiff = abs(difference)
    val color = when {
        absDiff < 2 -> Color(0xFF4CAF50)
        absDiff < 5 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Garment measurement
            Column {
                Text(
                    text = "Garment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${garmentValue.toInt()} cm",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Difference indicator
            Icon(
                imageVector = when {
                    difference > 0 -> Icons.Default.KeyboardArrowUp
                    difference < 0 -> Icons.Default.KeyboardArrowDown
                    else -> Icons.Default.Check
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            
            // Profile measurement
            Column {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${profileValue.toInt()} cm",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Difference value
            Text(
                text = "${if (difference > 0) "+" else ""}${difference.toInt()} cm",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyMeasurementsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No measurements available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "This seller hasn't provided detailed measurements yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyProfilePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Complete your size profile",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Add your measurements to get accurate size recommendations",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { /* Open profile setup */ },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Set Up Profile")
        }
    }
}

@Composable
private fun EmptyComparisonPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Add your measurements to compare",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMeasurementKey(key: String): String {
    return key.replace("_", " ").split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

enum class MeasurementTab(val title: String) {
    GARMENT("Garment"),
    YOUR_PROFILE("Your Profile"),
    COMPARISON("Comparison")
}
