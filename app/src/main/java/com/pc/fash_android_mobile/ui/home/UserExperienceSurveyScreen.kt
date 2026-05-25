package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserExperienceSurveyScreen(
    surveyKey: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: UserExperienceSurveyViewModel = viewModel(),
) {
    LaunchedEffect(surveyKey) { viewModel.load(surveyKey) }
    val loading by viewModel.loading.collectAsState()
    val submitting by viewModel.submitting.collectAsState()
    val error by viewModel.error.collectAsState()
    val survey by viewModel.survey.collectAsState()
    val submitted by viewModel.submitted.collectAsState()
    val spacing = FashTheme.spacing
    val ratings = remember { mutableStateMapOf<String, Int>() }
    val texts = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        survey?.title?.take(48).orEmpty()
                            .ifBlank { stringResource(R.string.ux_survey_title) },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when {
            loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            }
            error != null || survey == null -> {
                FashEmptyState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.feed_load_error),
                    subtitle = error.orEmpty(),
                )
            }
            submitted -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(spacing.editorialStart),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.ux_survey_thanks_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.height(spacing.spacing2))
                    Text(
                        text = stringResource(R.string.ux_survey_thanks_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(spacing.spacing4))
                    Button(onClick = onBack) { Text(stringResource(R.string.cd_back)) }
                }
            }
            else -> {
                val s = survey!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.editorialStart, vertical = spacing.spacing3),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
                ) {
                    if (s.description.isNotBlank()) {
                        Text(
                            text = s.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    for (q in s.questions) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = q.prompt,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            when (q.questionType) {
                                "rating" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (star in 1..5) {
                                            val selected = (ratings[q.id] ?: 0) >= star
                                            IconButton(onClick = {
                                                ratings[q.id] = star
                                                viewModel.setRating(q.id, star)
                                            }) {
                                                Icon(
                                                    imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                                    contentDescription = null,
                                                    tint = if (selected) FashColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                                "text" -> {
                                    OutlinedTextField(
                                        value = texts[q.id].orEmpty(),
                                        onValueChange = {
                                            texts[q.id] = it
                                            viewModel.setText(q.id, it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        placeholder = { Text(stringResource(R.string.ux_survey_text_hint)) },
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.submit(
                                onSuccess = onSubmitted,
                                onFailure = { },
                            )
                        },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text(stringResource(R.string.ux_survey_submit))
                        }
                    }
                }
            }
        }
    }
}
