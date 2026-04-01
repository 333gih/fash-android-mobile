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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.pc.fash_android_mobile.data.common.CommonAddressDto
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
    /** When false, hides the Vietnam catalog hint (e.g. post-listing overlay). */
    showCatalogHint: Boolean = true,
) {
    val provinces by viewModel.provinces.collectAsState()
    val districts by viewModel.districts.collectAsState()
    val wards by viewModel.wards.collectAsState()

    var label by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var line1 by remember { mutableStateOf("") }
    var line2 by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    var selectedProvince by remember { mutableStateOf<CommonAddressDto?>(null) }
    var selectedDistrict by remember { mutableStateOf<CommonAddressDto?>(null) }
    var selectedWard by remember { mutableStateOf<CommonAddressDto?>(null) }

    var validationError by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProvincesIfNeeded()
        viewModel.resetAdministrativeDropdowns()
    }

    LaunchedEffect(selectedProvince?.id) {
        viewModel.onProvinceSelected(selectedProvince?.id)
        if (selectedProvince == null) {
            selectedDistrict = null
            selectedWard = null
        }
    }

    LaunchedEffect(selectedDistrict?.id) {
        viewModel.onDistrictSelected(selectedDistrict?.id)
        if (selectedDistrict == null) {
            selectedWard = null
        }
    }

    BackHandler(onBack = onBack)

    fun save() {
        val hasArea = selectedProvince != null && selectedDistrict != null && selectedWard != null
        val ok = name.trim().isNotEmpty() && phone.trim().isNotEmpty() &&
            line1.trim().isNotEmpty() && hasArea
        if (!ok) {
            validationError = true
            saveError = null
            return
        }
        validationError = false
        saveError = null
        saving = true
        val prov = selectedProvince!!
        val dist = selectedDistrict!!
        val w = selectedWard!!
        viewModel.createShippingAddress(
            label = label,
            recipientName = name,
            phone = phone,
            line1 = line1,
            line2 = line2,
            city = prov.name,
            region = dist.name,
            postalCode = postalCode,
            countryCode = "VN",
            provinceId = prov.id,
            provinceName = prov.name,
            districtId = dist.id,
            districtName = dist.name,
            wardId = w.id,
            wardName = w.name,
            isDefault = isDefault,
            onResult = { result ->
                saving = false
                result.fold(
                    onSuccess = { id -> onSaved(id) },
                    onFailure = {
                        saveError = it.message ?: ""
                        validationError = false
                    },
                )
            },
        )
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
                    IconButton(onClick = onBack, enabled = !saving) {
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
            if (showCatalogHint) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.address_vn_catalog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AddressFormField(
                label = stringResource(R.string.address_field_label_optional),
                value = label,
                onValueChange = { label = it },
                placeholder = stringResource(R.string.address_field_label_hint),
            )
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
            Text(
                text = stringResource(R.string.address_country_fixed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = stringResource(R.string.address_country_vn),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.address_field_province),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            VnAddressDropdown(
                label = stringResource(R.string.address_field_province),
                options = provinces,
                selected = selectedProvince,
                onSelect = { selected ->
                    selectedProvince = selected
                    selectedDistrict = null
                    selectedWard = null
                },
                placeholder = stringResource(R.string.address_select_province),
            )
            Spacer(modifier = Modifier.height(8.dp))
            VnAddressDropdown(
                label = stringResource(R.string.address_field_district),
                options = districts,
                selected = selectedDistrict,
                onSelect = { selected ->
                    selectedDistrict = selected
                    selectedWard = null
                },
                enabled = selectedProvince != null,
                placeholder = stringResource(R.string.address_select_district),
            )
            Spacer(modifier = Modifier.height(8.dp))
            VnAddressDropdown(
                label = stringResource(R.string.address_field_ward),
                options = wards,
                selected = selectedWard,
                onSelect = { selectedWard = it },
                enabled = selectedDistrict != null,
                placeholder = stringResource(R.string.address_select_ward),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AddressFormField(
                label = stringResource(R.string.address_field_line1),
                value = line1,
                onValueChange = { line1 = it },
                placeholder = stringResource(R.string.address_field_line1_hint),
                multiline = true,
            )
            AddressFormField(
                label = stringResource(R.string.address_field_line2_optional),
                value = line2,
                onValueChange = { line2 = it },
                placeholder = stringResource(R.string.address_field_line2_hint),
            )
            AddressFormField(
                label = stringResource(R.string.address_field_postal_optional),
                value = postalCode,
                onValueChange = { postalCode = it.filter { ch -> ch.isDigit() }.take(12) },
                placeholder = stringResource(R.string.address_field_postal_hint),
            )
            if (validationError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.address_validation_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            saveError?.takeIf { it.isNotBlank() }?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = err,
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
                    enabled = !saving,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary.fashReadableOn(),
                    )
                } else {
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
