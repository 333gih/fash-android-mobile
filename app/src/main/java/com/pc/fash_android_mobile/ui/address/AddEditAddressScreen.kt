package com.pc.fash_android_mobile.ui.address

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddressScreen(
    viewModel: AddressBookViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSaved: (newAddressId: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var line1 by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    fun save() {
        val ok = listOf(name, phone, city, district, ward, line1).all { it.trim().isNotEmpty() }
        if (!ok) {
            validationError = true
            return
        }
        validationError = false
        val id = viewModel.addAddress(
            recipientName = name,
            phone = phone,
            city = city,
            district = district,
            ward = ward,
            line1 = line1,
            isDefault = isDefault,
        )
        if (id != null) onSaved(id)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.address_add_title),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FashTheme.spacing.editorialStart)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.address_section_shipping),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FashColors.Primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.address_form_header),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(20.dp))
            AddressFormField(
                label = stringResource(R.string.address_field_full_name),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.address_field_full_name_hint),
            )
            AddressFormField(
                label = stringResource(R.string.address_field_phone),
                value = phone,
                onValueChange = { phone = it },
                placeholder = stringResource(R.string.address_field_phone_hint),
            )
            AddressFormField(
                label = stringResource(R.string.address_field_city),
                value = city,
                onValueChange = { city = it },
                placeholder = stringResource(R.string.address_field_city_hint),
            )
            AddressFormField(
                label = stringResource(R.string.address_field_district),
                value = district,
                onValueChange = { district = it },
                placeholder = stringResource(R.string.address_field_district_hint),
            )
            AddressFormField(
                label = stringResource(R.string.address_field_ward),
                value = ward,
                onValueChange = { ward = it },
                placeholder = stringResource(R.string.address_field_ward_hint),
            )
            AddressFormField(
                label = stringResource(R.string.address_field_line1),
                value = line1,
                onValueChange = { line1 = it },
                placeholder = stringResource(R.string.address_field_line1_hint),
                multiline = true,
            )
            if (validationError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.address_validation_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.address_default_toggle),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = stringResource(R.string.address_default_toggle_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.address_save),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun AddressFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    multiline: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            singleLine = !multiline,
            minLines = if (multiline) 2 else 1,
            maxLines = if (multiline) 4 else 1,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}
