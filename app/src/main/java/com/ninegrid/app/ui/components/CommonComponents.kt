package com.ninegrid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ninegrid.app.core.model.AppLanguage

@Composable
fun BrandHeader(
    darkTheme: Boolean,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GridBrandMark(modifier = Modifier.size(30.dp))
            Text(
                text = language.text("隅光", "Cornerlight"),
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .size(40.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = language.text("关于隅光", "About Cornerlight")
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onToggleLanguage,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("language-switch")
                    .semantics {
                        role = Role.Button
                        contentDescription = language.text("切换为英语", "Switch to Chinese")
                    },
            ) {
                Text(
                    text = language.switchLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(40.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (darkTheme) {
                            language.text("切换到浅色模式", "Switch to light mode")
                        } else {
                            language.text("切换到深色模式", "Switch to dark mode")
                        }
                    },
            ) {
                val iconColor = MaterialTheme.colorScheme.onSurface
                val cutoutColor = MaterialTheme.colorScheme.background
                Canvas(modifier = Modifier.size(20.dp)) {
                    if (darkTheme) {
                        drawCircle(
                            color = iconColor,
                            radius = 5.dp.toPx(),
                        )
                        repeat(8) { index ->
                            val angle = Math.toRadians(index * 45.0)
                            val inner = 7.dp.toPx()
                            val outer = 9.dp.toPx()
                            drawLine(
                                color = iconColor,
                                start = Offset(
                                    center.x + kotlin.math.cos(angle).toFloat() * inner,
                                    center.y + kotlin.math.sin(angle).toFloat() * inner,
                                ),
                                end = Offset(
                                    center.x + kotlin.math.cos(angle).toFloat() * outer,
                                    center.y + kotlin.math.sin(angle).toFloat() * outer,
                                ),
                                strokeWidth = 1.4.dp.toPx(),
                            )
                        }
                    } else {
                        drawCircle(
                            color = iconColor,
                            radius = 7.dp.toPx(),
                        )
                        drawCircle(
                            color = cutoutColor,
                            radius = 7.dp.toPx(),
                            center = center + Offset(4.dp.toPx(), (-3).dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GridBrandMark(modifier: Modifier = Modifier) {
    val tileColor = MaterialTheme.colorScheme.onPrimary
    val accentColor = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth(0.56f)) {
            val gap = 1.8.dp.toPx()
            val cell = (size.width - gap * 2) / 3
            repeat(3) { row ->
                repeat(3) { column ->
                    drawRoundRect(
                        color = if (row == 2 && column == 2) {
                            accentColor
                        } else {
                            tileColor
                        },
                        topLeft = Offset(column * (cell + gap), row * (cell + gap)),
                        size = androidx.compose.ui.geometry.Size(cell, cell),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.7.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
