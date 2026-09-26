package com.ninegrid.app.ui.editor

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ninegrid.app.BuildConfig
import com.ninegrid.app.core.model.AppLanguage
import com.ninegrid.app.core.model.CropMode
import com.ninegrid.app.core.model.GridSpec
import com.ninegrid.app.ui.components.BrandHeader

/** 开源仓库地址，供“关于隅光”中的开源入口使用。 */
private const val GITHUB_REPOSITORY_URL = "https://github.com/yjd953/cornerlight-android"

/**
 * Stateless editor route.
 *
 * This composable owns only page-level composition. Business state and side effects stay in
 * [NineGridViewModel], while landing/editor details live in focused sibling files.
 */
@Composable
fun NineGridScreen(
    state: EditorUiState,
    snackbarHostState: SnackbarHostState,
    onPickPhoto: () -> Unit,
    onPastePhoto: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onGridSelected: (GridSpec) -> Unit,
    onCropModeSelected: (CropMode) -> Unit,
    onPaddingChanged: (Float) -> Unit,
    onBackgroundColorChanged: (Color) -> Unit,
    onQualityChanged: (Int) -> Unit,
    onFocusChanged: (Float, Float) -> Unit,
    onResetSettings: () -> Unit,
    onResetAll: () -> Unit,
    onTileSelected: (Int) -> Unit,
    onCloseTile: () -> Unit,
    onSaveAll: () -> Unit,
    onSaveOne: (Int) -> Unit,
    onShareAll: () -> Unit,
    onShareOne: (Int) -> Unit,
    onExportZip: () -> Unit,
) {
    var aboutVisible by rememberSaveable { mutableStateOf(false) }
    var privacyVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BrandHeader(
                darkTheme = state.darkTheme,
                language = state.language,
                onToggleLanguage = onToggleLanguage,
                onToggleTheme = onToggleTheme,
                onOpenAbout = { aboutVisible = true },
                modifier = Modifier.statusBarsPadding(),
            )
        },
        bottomBar = {
            if (state.hasImage) {
                EditorBottomBar(
                    count = state.settings.gridSpec.tileCount,
                    language = state.language,
                    enabled = state.hasResults && !state.isExporting,
                    onSaveAll = onSaveAll,
                    onShareAll = onShareAll,
                    onExportZip = onExportZip,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (state.hasImage) {
                EditorContent(
                    state = state,
                    onPickPhoto = onPickPhoto,
                    onGridSelected = onGridSelected,
                    onCropModeSelected = onCropModeSelected,
                    onPaddingChanged = onPaddingChanged,
                    onBackgroundColorChanged = onBackgroundColorChanged,
                    onQualityChanged = onQualityChanged,
                    onFocusChanged = onFocusChanged,
                    onResetSettings = onResetSettings,
                    onResetAll = onResetAll,
                    onTileSelected = onTileSelected,
                )
            } else {
                LandingContent(
                    loading = state.isLoadingImage,
                    language = state.language,
                    onPickPhoto = onPickPhoto,
                    onPastePhoto = onPastePhoto,
                )
            }

            state.exportProgress?.let { progress -> ExportOverlay(progress) }
        }
    }

    val selectedIndex = state.selectedTileIndex
    val selectedTile = selectedIndex?.let(state.previews::getOrNull)
    if (selectedIndex != null && selectedTile != null) {
        TilePreviewDialog(
            index = selectedIndex,
            bitmap = selectedTile.bitmap,
            language = state.language,
            onDismiss = onCloseTile,
            onSave = {
                onCloseTile()
                onSaveOne(selectedIndex)
            },
            onShare = {
                onCloseTile()
                onShareOne(selectedIndex)
            },
        )
    }

    if (aboutVisible) {
        AboutDialog(
            language = state.language,
            onDismiss = { aboutVisible = false },
            onOpenPrivacy = {
                aboutVisible = false
                privacyVisible = true
            },
        )
    }

    if (privacyVisible) {
        PrivacyDialog(
            language = state.language,
            onBack = {
                privacyVisible = false
                aboutVisible = true
            },
            onDismiss = { privacyVisible = false },
        )
    }
}

@Composable
private fun AboutDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    val openRepository = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPOSITORY_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast
                .makeText(
                    context,
                    language.text("未找到可打开链接的应用", "No app available to open the link"),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 560.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            language.text("关于隅光", "About Cornerlight"),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            language.text(
                                "离线、纯净、尊重隐私",
                                "Offline, clean, and private",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = language.text("关闭", "Close"),
                        )
                    }
                }
                Text(
                    text = language.text(
                        "隅光不收集个人数据，也不会自行上传照片。只有你主动分享时，所选应用才会获得对应切图。",
                        "Cornerlight collects no personal data and does not upload photos. A receiving app gets an image only when you choose to share it.",
                    ),
                    modifier = Modifier.padding(top = 22.dp, bottom = 20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                AboutAction(
                    title = language.text("隐私政策", "Privacy policy"),
                    subtitle = language.text(
                        "查看照片、权限与数据处理说明",
                        "How photos, permissions, and data are handled",
                    ),
                    actionLabel = language.text("打开", "Open"),
                    onClick = onOpenPrivacy,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                AboutAction(
                    title = language.text("开源代码", "Open source"),
                    subtitle = language.text(
                        "源代码采用 MIT 许可证，品牌名称与图标保留权利",
                        "MIT-licensed source code; brand name and icons are reserved",
                    ),
                    actionLabel = language.text("GitHub", "GitHub"),
                    onClick = openRepository,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        language.text("当前版本", "Current version"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutAction(
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun PrivacyDialog(
    language: AppLanguage,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 620.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = language.text("返回", "Back"),
                        )
                    }
                    Text(
                        language.text("隐私政策", "Privacy policy"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = language.text("关闭", "Close"),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 22.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    language.text("你的照片，只属于你", "Your photos belong to you"),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                PrivacySection(
                    title = language.text("我们不收集个人数据", "We collect no personal data"),
                    body = language.text(
                        "隅光没有账号系统、广告 SDK、统计 SDK 或行为追踪，不收集姓名、手机号、设备标识、位置或使用记录。",
                        "Cornerlight has no accounts, advertising SDKs, analytics SDKs, or behavior tracking. It does not collect names, phone numbers, device identifiers, locations, or usage history.",
                    ),
                )
                PrivacySection(
                    title = language.text("照片仅在本机处理", "Photos stay on your device"),
                    body = language.text(
                        "应用只读取你通过系统选择器选择或从剪贴板主动导入的照片，不能浏览完整相册。读取、裁剪、预览和导出均在当前设备中完成。",
                        "The app reads only photos you select with the system picker or explicitly import from the clipboard. It cannot browse your full library. Reading, cropping, previewing, and exporting happen on this device.",
                    ),
                )
                PrivacySection(
                    title = language.text("权限使用", "Permissions"),
                    body = language.text(
                        "Android 10 及以上使用系统接口保存图片，无需存储权限。Android 9 及以下仅在你主动保存图片时申请写入存储权限。",
                        "Android 10 and later save images through system APIs without storage permission. Android 9 and earlier request write permission only when you choose to save.",
                    ),
                )
                PrivacySection(
                    title = language.text("本地缓存与分享", "Local cache and sharing"),
                    body = language.text(
                        "应用不包含网络权限。导入副本通常会在解码后删除；分享缓存会由系统清理，应用也会在下次分享时清理超过 24 小时的旧缓存。主动分享后，接收应用将按其自身隐私政策处理图片。",
                        "The app has no network permission. Import copies are normally deleted after decoding. Android may clear sharing files, and the app removes files older than 24 hours before the next share. Receiving apps handle shared images under their own privacy policies.",
                    ),
                )
                Text(
                    language.text(
                        "更新日期：2026 年 9 月 27 日",
                        "Updated: September 27, 2026",
                    ),
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            body,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
