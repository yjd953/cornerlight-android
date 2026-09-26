package com.ninegrid.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ninegrid.app.core.model.AppLanguage
import com.ninegrid.app.core.model.CropMode
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.GridSpec
import kotlin.math.roundToInt

@Composable
fun SettingsPanel(
    settings: EditorSettings,
    language: AppLanguage,
    onGridSelected: (GridSpec) -> Unit,
    onCropModeSelected: (CropMode) -> Unit,
    onPaddingChanged: (Float) -> Unit,
    onBackgroundColorChanged: (Color) -> Unit,
    onQualityChanged: (Int) -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var advancedVisible by rememberSaveable { mutableStateOf(false) }
    SectionCard(
        title = language.text("调整效果", "Adjustments"),
        subtitle = language.text("修改后自动更新", "Preview updates automatically"),
        modifier = modifier,
        trailing = {
            Text(
                text = language.text("恢复默认", "Reset"),
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onResetSettings)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        SettingLabel(number = "1", title = language.text("选择版式", "Choose layout"))
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GridSpec.entries.forEach { spec ->
                GridSpecOption(
                    spec = spec,
                    language = language,
                    selected = settings.gridSpec == spec,
                    onClick = { onGridSelected(spec) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SettingLabel(number = "2", title = language.text("图片适配", "Image fit"))
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropMode.entries.forEach { mode ->
                TextOption(
                    title = mode.displayName(language),
                    subtitle = mode.description(language),
                    selected = settings.cropMode == mode,
                    onClick = { onCropModeSelected(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SettingLabel(number = "3", title = language.text("添加白边", "Add border"))
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                language.text("无", "None") to 0f,
                language.text("细", "Thin") to 1.5f,
                language.text("宽", "Wide") to 3f,
            ).forEach { (label, value) ->
                SimpleOption(
                    label = label,
                    selected = kotlin.math.abs(settings.paddingPercent - value) < 0.1f,
                    onClick = { onPaddingChanged(value) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Surface(
            onClick = { advancedVisible = !advancedVisible },
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.text("更多设置", "More settings"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = language.text(
                        "白边、颜色与清晰度  ${if (advancedVisible) "⌃" else "⌄"}",
                        "Border, color, quality  ${if (advancedVisible) "⌃" else "⌄"}",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(visible = advancedVisible) {
            Column {
                Text(
                    text = language.text(
                        "白边宽度  ${settings.paddingPercent.roundToInt()}%",
                        "Border width  ${settings.paddingPercent.roundToInt()}%",
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = settings.paddingPercent,
                    onValueChange = onPaddingChanged,
                    valueRange = 0f..12f,
                    steps = 23,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = language.text("背景颜色", "Background color"),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    backgroundPalette.forEach { color ->
                        ColorOption(
                            color = color,
                            selected = settings.backgroundColor == color,
                            onClick = { onBackgroundColorChanged(color) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = language.text(
                        "导出清晰度  ${settings.quality}%",
                        "Export quality  ${settings.quality}%",
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = settings.quality.toFloat(),
                    onValueChange = { value ->
                        onQualityChanged((value / 5).roundToInt() * 5)
                    },
                    valueRange = 60f..100f,
                    steps = 7,
                )
                Text(
                    text = language.text(
                        "清晰度越高，图片文件越大",
                        "Higher quality creates larger files",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingLabel(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun GridSpecOption(
    spec: GridSpec,
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "grid option",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(6.dp),
        color = color,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MiniGrid(spec.rows, spec.columns, selected)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = spec.displayName(language), style = MaterialTheme.typography.labelLarge)
            Text(
                text = spec.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MiniGrid(rows: Int, columns: Int, selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(width = 37.dp, height = 29.dp)) {
        val gap = 2.dp.toPx()
        val width = (size.width - gap * (columns - 1)) / columns
        val height = (size.height - gap * (rows - 1)) / rows
        repeat(rows) { row ->
            repeat(columns) { column ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(column * (width + gap), row * (height + gap)),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun TextOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SimpleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Box(modifier = Modifier.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ColorOption(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                shape = CircleShape,
            )
            .padding(4.dp)
            .background(color, CircleShape)
            .then(
                if (color == Color.White) Modifier.background(
                    Color.Transparent,
                    CircleShape,
                ) else Modifier,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
    )
}

private val backgroundPalette = listOf(
    Color.White,
    Color(0xFFF4F0E8),
    Color(0xFFFFE5EF),
    Color(0xFFE9E5FF),
    Color(0xFFE4F1FF),
    Color(0xFF18171F),
)
