package com.pc.fash_android_mobile.ui.entitlements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.entitlements.UserEntitlementRepository
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerPackageToolsScreen(
    repository: UserEntitlementRepository,
    onBack: () -> Unit,
    onEntitlementsChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var listingId by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun run(action: () -> Result<Unit>) {
        scope.launch {
            message = action().fold(
                onSuccess = {
                    onEntitlementsChanged()
                    "OK"
                },
                onFailure = { it.message ?: "Error" },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.seller_packages_tools_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = FashColors.Primary)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = listingId,
                onValueChange = { listingId = it },
                label = { Text(stringResource(R.string.seller_packages_tools_listing_id)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text(stringResource(R.string.seller_packages_tools_caption)) },
                modifier = Modifier.fillMaxWidth(),
            )
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = { run { repository.requestAuthenticity(listingId) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = listingId.isNotBlank(),
            ) { Text(stringResource(R.string.seller_packages_tools_verify)) }
            Button(
                onClick = { run { repository.applyExploreBoost(listingId) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = listingId.isNotBlank(),
            ) { Text(stringResource(R.string.seller_packages_tools_boost)) }
            Button(
                onClick = { run { repository.requestFanpage(listingId, caption) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.seller_packages_tools_fanpage)) }
            Button(
                onClick = { run { repository.requestSocialPromo(listingId, caption) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.seller_packages_tools_social)) }
        }
    }
}
