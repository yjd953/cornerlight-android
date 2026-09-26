package com.ninegrid.app.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ninegrid.app.core.model.AppLanguage

@Composable
internal fun LandingContent(
    loading: Boolean,
    language: AppLanguage,
    onPickPhoto: () -> Unit,
    onPastePhoto: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                EditorialGrid(modifier = Modifier.size(210.dp))
                Spacer(modifier = Modifier.height(34.dp))
                Text(
                    text = language.text("从一张，\n成为九张。", "One photo.\nNine moments."),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = language.text(
                        "选好照片，剩下的交给隅光。",
                        "Choose a photo. Cornerlight handles the rest.",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(30.dp))
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(42.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        language.text("正在打开照片", "Opening photo"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Button(
                        onClick = onPickPhoto,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(
                            text = language.text("选择一张照片", "Choose a photo"),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(onClick = onPastePhoto) {
                        Text(language.text("从剪贴板导入", "Import from clipboard"))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialGrid(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.secondary
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gap = 7.dp.toPx()
            val cell = (size.width - gap * 2) / 3f
            repeat(3) { row ->
                repeat(3) { column ->
                    val index = row * 3 + column
                    val color = when (index) {
                        0, 4, 8 -> ink
                        6 -> accent
                        else -> muted
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(column * (cell + gap), row * (cell + gap)),
                        size = Size(cell, cell),
                    )
                }
            }
            drawLine(
                color = Color.White.copy(alpha = 0.75f),
                start = Offset(gap + cell * 0.25f, size.height - cell * 0.32f),
                end = Offset(size.width - cell * 0.18f, cell * 0.22f),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}
