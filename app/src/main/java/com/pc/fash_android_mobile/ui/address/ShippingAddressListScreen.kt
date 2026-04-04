package com.pc.fash_android_mobile.ui.address

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * @param orderId When non-null, confirming links this address to the order ([AddressBookViewModel.setOrderShipping]).
 * When null (manage mode), only updates selection / default on device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShippingAddressListScreen(
    orderId: String?,
    viewModel: AddressBookViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val addresses by viewModel.addresses.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(addresses, orderId) {
        if (addresses.isNotEmpty()) {
            selectedId = when (orderId) {
                null -> addresses.firstOrNull { it.isDefault }?.id ?: addresses.first().id
                else -> viewModel.initialSelectionIdForOrder(orderId) ?: addresses.first().id
            }
        }
    }

    BackHandler(onBack = onBack)

    val titleRes = if (orderId != null) R.string.address_list_title else R.string.profile_shipping_addresses
    val subtitleRes = if (orderId != null) R.string.address_list_subtitle else R.string.address_list_subtitle_manage
    val primaryButtonRes = if (orderId != null) R.string.address_confirm else R.string.address_done

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = FashColors.Primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            Text(
                text = stringResource(R.string.address_list_header),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (loading && addresses.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = FashColors.Primary,
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(addresses, key = { it.id }) { addr ->
                            ShippingAddressSelectableCard(
                                address = addr,
                                selected = addr.id == selectedId,
                                onClick = { selectedId = addr.id },
                                onSetDefault = if (!addr.isDefault) {
                                    { viewModel.setDefaultAddress(addr.id) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAddNew,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = FashColors.Primary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.address_add_new),
                    color = FashColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val id = selectedId ?: return@Button
                    val addr = addresses.find { it.id == id } ?: return@Button
                    orderId?.let { oid -> viewModel.setOrderShipping(oid, addr) }
                    onConfirmed()
                },
                enabled = selectedId != null && addresses.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(primaryButtonRes),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
