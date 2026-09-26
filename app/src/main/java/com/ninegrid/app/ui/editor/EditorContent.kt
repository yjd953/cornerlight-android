package com.ninegrid.app.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ninegrid.app.core.model.AppLanguage
import com.ninegrid.app.core.model.CropMode
import com.ninegrid.app.core.model.GridSpec
import com.ninegrid.app.ui.components.CompositionPreview
import com.ninegrid.app.ui.components.ResultGrid
import com.ninegrid.app.ui.components.SettingsPanel

/** Editing workspace shown only when [EditorUiState.source] is available. */
@Composable
internal fun EditorContent(
    state: EditorUiState,
    onPickPhoto: () -> Unit,
    onGridSelected: (GridSpec) -> Unit,
    onCropModeSelected: (CropMode) -> Unit,
    onPaddingChanged: (Float) -> Unit,
    onBackgroundColorChanged: (Color) -> Unit,
    onQualityChanged: (Int) -> Unit,
    onFocusChanged: (Float, Float) -> Unit,
    onResetSettings: () -> Unit,
    onResetAll: () -> Unit,
    onTileSelected: (Int) -> Unit,
) {
    val source = state.source ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.language.text("构图", "Composition"),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        state.language.text(
                            "拖动照片，决定画面中心",
                            "Drag the photo to position the subject",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    state.language.text("本地处理", "Processed locally"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
                CompositionPreview(source, state.settings, state.language, onFocusChanged)
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.language.text(
                            "${state.settings.gridSpec.tileCount} 张成片",
                            "${state.settings.gridSpec.tileCount} images",
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (state.isGeneratingPreview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    } else {
                        Text(
                            state.language.text("点击可查看单张", "Tap to view one image"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                ResultGrid(
                    previews = state.previews,
                    rows = state.settings.gridSpec.rows,
                    columns = state.settings.gridSpec.columns,
                    language = state.language,
                    onTileSelected = onTileSelected,
                )
            }
        }

        item {
            SettingsPanel(
                settings = state.settings,
                language = state.language,
                onGridSelected = onGridSelected,
                onCropModeSelected = onCropModeSelected,
                onPaddingChanged = onPaddingChanged,
                onBackgroundColorChanged = onBackgroundColorChanged,
                onQualityChanged = onQualityChanged,
                onResetSettings = onResetSettings,
                modifier = Modifier.widthIn(max = 760.dp),
            )
        }

        item {
            SourceInfoCard(
                language = state.language,
                fileName = source.displayName,
                width = source.originalWidth,
                height = source.originalHeight,
                fileSize = source.fileSize,
                onReplace = onPickPhoto,
                onClear = onResetAll,
                modifier = Modifier.widthIn(max = 760.dp),
            )
        }
    }
}

@Composable
internal fun EditorBottomBar(
    count: Int,
    language: AppLanguage,
    enabled: Boolean,
    onSaveAll: () -> Unit,
    onShareAll: () -> Unit,
    onExportZip: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 5.dp, shadowElevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSaveAll,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(language.text("保存全部 $count 张", "Save all $count"))
            }
            OutlinedButton(
                onClick = onShareAll,
                enabled = enabled,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = language.text("分享", "Share"),
                )
            }
            OutlinedButton(
                onClick = onExportZip,
                enabled = enabled,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 15.dp),
            ) { Text("ZIP") }
        }
    }
}

@Composable
private fun SourceInfoCard(
    language: AppLanguage,
    fileName: String,
    width: Int,
    height: Int,
    fileSize: Long?,
    onReplace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(language.text("原图", "Original"), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$width × $height${fileSize?.let { " · ${formatFileSize(it)}" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReplace) {
                    Text(language.text("换一张", "Replace"))
                }
                TextButton(onClick = onClear) {
                    Text(language.text("重新开始", "Start over"))
                }
            }
        }
    }
}

@Composable
internal fun TilePreviewDialog(
    index: Int,
    bitmap: Bitmap,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 620.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            language.text("第 ${index + 1} 张", "Image ${index + 1}"),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            language.text("1080 × 1080 导出", "1080 × 1080 export"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(language.text("关闭", "Close"))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = language.text(
                        "第 ${index + 1} 张切图预览",
                        "Preview of image ${index + 1}",
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text(language.text("保存这张", "Save"))
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Text(language.text("分享这张", "Share"))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExportOverlay(progress: ExportProgress) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 260.dp, max = 320.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp,
        ) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(14.dp))
                Text(progress.label, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = {
                        if (progress.total == 0) 0f else progress.completed.toFloat() / progress.total
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    "${progress.completed} / ${progress.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun formatFileSize(bytes: Long): String {
    val megabytes = bytes / 1024f / 1024f
    return if (megabytes >= 1f) "%.1f MB".format(megabytes) else "%.0f KB".format(bytes / 1024f)
}
