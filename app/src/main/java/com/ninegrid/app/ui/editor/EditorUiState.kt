package com.ninegrid.app.ui.editor

import androidx.compose.runtime.Immutable
import com.ninegrid.app.core.model.AppLanguage
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.SourceImage
import com.ninegrid.app.core.model.TilePreview

/** Complete render state for the single-screen editor. */
@Immutable
data class EditorUiState(
    val source: SourceImage? = null,
    val settings: EditorSettings = EditorSettings.Default,
    val previews: List<TilePreview> = emptyList(),
    val isLoadingImage: Boolean = false,
    val isGeneratingPreview: Boolean = false,
    val exportProgress: ExportProgress? = null,
    val selectedTileIndex: Int? = null,
    val darkTheme: Boolean = false,
    val language: AppLanguage = AppLanguage.CHINESE,
) {
    val hasImage: Boolean get() = source != null
    val hasResults: Boolean get() = previews.size == settings.gridSpec.tileCount
    val isExporting: Boolean get() = exportProgress != null
}

@Immutable
data class ExportProgress(
    val label: String,
    val completed: Int,
    val total: Int,
)

/** One-shot platform actions that must not be replayed as persistent UI state. */
sealed interface EditorEffect {
    data class ShowMessage(val message: String) : EditorEffect
    data class ShareImages(val uris: List<android.net.Uri>) : EditorEffect
    data object RequestLegacyWritePermission : EditorEffect
    data class CreateZipDocument(val suggestedName: String) : EditorEffect
}

sealed interface PendingSaveAction {
    data object All : PendingSaveAction
    data class One(val index: Int) : PendingSaveAction
}
