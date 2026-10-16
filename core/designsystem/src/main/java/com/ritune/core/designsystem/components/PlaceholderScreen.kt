package com.ritune.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ritune.core.designsystem.theme.Ritune

data class PlaceholderAction(val label: String, val onClick: () -> Unit)

@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    actions: List<PlaceholderAction> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Ritune.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        actions.forEach { SecondaryButton(text = it.label, onClick = it.onClick) }
    }
}