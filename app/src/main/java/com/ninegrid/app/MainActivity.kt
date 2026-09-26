package com.ninegrid.app

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ninegrid.app.data.export.ShareLauncher
import com.ninegrid.app.ui.editor.EditorEffect
import com.ninegrid.app.ui.editor.NineGridScreen
import com.ninegrid.app.ui.editor.NineGridViewModel
import com.ninegrid.app.ui.theme.NineGridTheme

class MainActivity : ComponentActivity() {
    private val viewModel: NineGridViewModel by viewModels {
        NineGridViewModel.factory((application as NineGridApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current
            val shareLauncher = remember(context) { ShareLauncher(context) }
            val currentLanguage by rememberUpdatedState(state.language)

            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = viewModel::onPhotoSelected,
            )
            val legacyPermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = viewModel::onLegacyWritePermissionResult,
            )
            val zipDestination = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/zip"),
                onResult = viewModel::exportZip,
            )

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is EditorEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                        is EditorEffect.ShareImages -> runCatching {
                            shareLauncher.shareImages(effect.uris, currentLanguage)
                        }.onFailure {
                            snackbarHostState.showSnackbar(
                                currentLanguage.text(
                                    "没有找到可分享图片的应用",
                                    "No app is available to share images",
                                ),
                            )
                        }
                        is EditorEffect.RequestLegacyWritePermission -> {
                            legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        is EditorEffect.CreateZipDocument -> zipDestination.launch(effect.suggestedName)
                    }
                }
            }

            BackHandler(enabled = state.hasImage && !state.isExporting) {
                if (state.selectedTileIndex != null) viewModel.closeTile() else viewModel.resetAll()
            }

            NineGridTheme(darkTheme = state.darkTheme) {
                NineGridScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onPickPhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onPastePhoto = {
                        viewModel.onClipboardImageSelected(clipboardImageUri())
                    },
                    onToggleLanguage = viewModel::toggleLanguage,
                    onToggleTheme = viewModel::toggleTheme,
                    onGridSelected = viewModel::selectGrid,
                    onCropModeSelected = viewModel::selectCropMode,
                    onPaddingChanged = viewModel::setPadding,
                    onBackgroundColorChanged = viewModel::setBackgroundColor,
                    onQualityChanged = viewModel::setQuality,
                    onFocusChanged = viewModel::setFocus,
                    onResetSettings = viewModel::resetSettings,
                    onResetAll = viewModel::resetAll,
                    onTileSelected = viewModel::openTile,
                    onCloseTile = viewModel::closeTile,
                    onSaveAll = viewModel::saveAll,
                    onSaveOne = viewModel::saveOne,
                    onShareAll = viewModel::shareAll,
                    onShareOne = viewModel::shareOne,
                    onExportZip = viewModel::requestZipExport,
                )
            }
        }
    }

    private fun clipboardImageUri(): Uri? {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return null
        val uri = clip.getItemAt(0).uri ?: clip.getItemAt(0).intent?.data ?: return null
        // Some apps expose a URI as text/uri-list instead of image/*. Trust the provider's MIME
        // type as a fallback, but never pass a known non-image URI into the decoder.
        val isImage = clip.description.hasMimeType("image/*") ||
            contentResolver.getType(uri)?.startsWith("image/") == true
        return uri.takeIf { isImage }
    }
}
