package kz.qorgau.scamguardian.ui.check

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.ui.components.PrivacyBadge
import kz.qorgau.scamguardian.ui.components.RiskBadge

@Composable
fun CheckScreen(
    viewModel: CheckViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var whyExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.check_title),
                style = MaterialTheme.typography.displaySmall,
            )
            PrivacyBadge()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            placeholder = { Text(stringResource(R.string.check_hint)) },
            enabled = !state.isAnalyzing,
            isError = state.errorMessageRes != null,
            supportingText = {
                state.errorMessageRes?.let { res ->
                    Text(stringResource(res))
                }
            },
        )

        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = viewModel::analyze,
                enabled = !state.isAnalyzing,
            ) {
                Text(stringResource(R.string.check_action))
            }
            OutlinedButton(
                onClick = {
                    whyExpanded = true
                    viewModel.clear()
                },
                enabled = !state.isAnalyzing && (state.input.isNotEmpty() || state.result != null),
            ) {
                Text(stringResource(R.string.check_clear))
            }
            if (state.isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        state.result?.let { result ->
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.check_result_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    RiskBadge(riskLevel = result.riskLevel)
                    PrivacyBadge()
                    Text(
                        text = result.explanation,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    OutlinedButton(onClick = { whyExpanded = !whyExpanded }) {
                        Text(stringResource(R.string.why_scam))
                    }
                    AnimatedVisibility(visible = whyExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (result.matchedRules.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.history_matched_rules),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = result.matchedRules.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Text(
                                text = result.messageText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
