package com.ninegrid.app.ui.editor

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ninegrid.app.AppContainer
import com.ninegrid.app.core.model.CropMode
import com.ninegrid.app.core.model.EditorSettings
import com.ninegrid.app.core.model.GridSpec
import com.ninegrid.app.core.model.messageFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Owns the editor's unidirectional state and serialises bitmap-intensive work.
 *
 * Preview jobs are cancellable and replace older work. Exports snapshot their source/settings and
 * temporarily lock editing so no bitmap can be recycled while it is being encoded.
 */
class NineGridViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EditorUiState(
            darkTheme = container.themeRepository.isDarkTheme(),
            language = container.languageRepository.getLanguage(),
        ),
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<EditorEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<EditorEffect> = _effects.asSharedFlow()

    private var previewJob: Job? = null
    private var exportJob: Job? = null
    private var pendingSaveAction: PendingSaveAction? = null

    fun onPhotoSelected(uri: Uri?) {
        if (uri == null || _uiState.value.isExporting) return
        // Joining the cancelled job prevents two decoders from peaking in memory at the same time.
        val previousPreviewJob = previewJob
        previousPreviewJob?.cancel()
        previewJob = viewModelScope.launch {
            previousPreviewJob?.join()
            _uiState.update { it.copy(isLoadingImage = true, isGeneratingPreview = false) }
            try {
                val newSource = container.imageLoader.load(uri)
                val oldState = _uiState.value
                _uiState.update {
                    it.copy(
                        source = newSource,
                        settings = EditorSettings.Default,
                        previews = emptyList(),
                        isLoadingImage = false,
                        isGeneratingPreview = true,
                        selectedTileIndex = null,
                    )
                }
                disposeBitmapsLater(oldState.source, oldState.previews)
                generatePreviews(newSource.bitmap, EditorSettings.Default)
                _effects.emit(EditorEffect.ShowMessage(text("照片已在本机完成切图", "Image split locally")))
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (error: Throwable) {
                _uiState.update { it.copy(isLoadingImage = false, isGeneratingPreview = false) }
                _effects.emit(
                    EditorEffect.ShowMessage(
                        error.messageFor(
                            _uiState.value.language,
                            fallbackChinese = "图片读取失败",
                            fallbackEnglish = "Unable to load the image",
                        ),
                    ),
                )
            }
        }
    }

    fun onClipboardImageSelected(uri: Uri?) {
        if (uri == null) {
            _effects.tryEmit(
                EditorEffect.ShowMessage(
                    text("剪贴板里没有可读取的图片", "No readable image found on the clipboard"),
                ),
            )
        } else {
            onPhotoSelected(uri)
        }
    }

    fun selectGrid(spec: GridSpec) = updateSettings { copy(gridSpec = spec) }

    fun selectCropMode(mode: CropMode) = updateSettings { copy(cropMode = mode) }

    fun setPadding(percent: Float) = updateSettings {
        copy(paddingPercent = percent.coerceIn(0f, 12f))
    }

    fun setBackgroundColor(color: Color) = updateSettings { copy(backgroundColor = color) }

    fun setQuality(quality: Int) {
        if (_uiState.value.source == null || _uiState.value.isExporting) return
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(quality = quality.coerceIn(60, 100)))
        }
    }

    fun setFocus(x: Float, y: Float) = updateSettings(debounce = true) {
        copy(focusX = x.coerceIn(0f, 1f), focusY = y.coerceIn(0f, 1f))
    }

    fun resetSettings() {
        val grid = _uiState.value.settings.gridSpec
        updateSettings(debounce = false) { EditorSettings.Default.copy(gridSpec = grid) }
    }

    fun toggleTheme() {
        val enabled = !_uiState.value.darkTheme
        container.themeRepository.setDarkTheme(enabled)
        _uiState.update { it.copy(darkTheme = enabled) }
    }

    fun toggleLanguage() {
        val language = _uiState.value.language.toggled()
        container.languageRepository.setLanguage(language)
        _uiState.update { it.copy(language = language) }
    }

    fun openTile(index: Int) {
        if (index in _uiState.value.previews.indices) {
            _uiState.update { it.copy(selectedTileIndex = index) }
        }
    }

    fun closeTile() {
        _uiState.update { it.copy(selectedTileIndex = null) }
    }

    fun resetAll() {
        if (_uiState.value.isExporting) return
        previewJob?.cancel()
        val old = _uiState.value
        _uiState.value = EditorUiState(darkTheme = old.darkTheme, language = old.language)
        disposeBitmapsLater(old.source, old.previews)
    }

    fun saveAll() = requestSave(PendingSaveAction.All)

    fun saveOne(index: Int) = requestSave(PendingSaveAction.One(index))

    private fun requestSave(action: PendingSaveAction) {
        if (!_uiState.value.hasResults || _uiState.value.isExporting) return
        if (container.exportRepository.requiresLegacyWritePermission) {
            pendingSaveAction = action
            _effects.tryEmit(EditorEffect.RequestLegacyWritePermission)
        } else {
            executeSave(action)
        }
    }

    fun onLegacyWritePermissionResult(granted: Boolean) {
        val action = pendingSaveAction
        pendingSaveAction = null
        if (granted && action != null) {
            executeSave(action)
        } else if (!granted) {
            _effects.tryEmit(
                EditorEffect.ShowMessage(
                    text(
                        "未获得存储权限，无法保存到相册",
                        "Storage permission was denied, so the images cannot be saved",
                    ),
                ),
            )
        }
    }

    private fun executeSave(action: PendingSaveAction) =
        runExport(text("正在保存", "Saving")) { source, settings ->
        when (action) {
            PendingSaveAction.All -> {
                container.exportRepository.saveAllToGallery(source, settings, ::updateProgress)
                _effects.emit(
                    EditorEffect.ShowMessage(
                        text("已保存到相册“隅光”", "Saved to the Cornerlight album"),
                    ),
                )
            }
            is PendingSaveAction.One -> {
                container.exportRepository.saveOneToGallery(source, settings, action.index)
                _effects.emit(EditorEffect.ShowMessage(text("已保存 1 张图片", "1 image saved")))
            }
        }
    }

    fun shareAll() = prepareShare(null)

    fun shareOne(index: Int) = prepareShare(index)

    private fun prepareShare(index: Int?) =
        runExport(text("正在准备分享", "Preparing to share")) { source, settings ->
        val uris = container.exportRepository.prepareShareImages(
            source = source,
            settings = settings,
            onlyIndex = index,
            onProgress = ::updateProgress,
        )
        _effects.emit(EditorEffect.ShareImages(uris))
    }

    fun requestZipExport() {
        if (!_uiState.value.hasResults || _uiState.value.isExporting) return
        _effects.tryEmit(EditorEffect.CreateZipDocument(container.exportRepository.zipFileName()))
    }

    fun exportZip(destination: Uri?) {
        if (destination == null) return
        runExport(text("正在导出 ZIP", "Exporting ZIP")) { source, settings ->
            container.exportRepository.writeZip(destination, source, settings, ::updateProgress)
            _effects.emit(EditorEffect.ShowMessage(text("ZIP 已导出", "ZIP exported")))
        }
    }

    /** Applies settings immediately while coalescing expensive preview regeneration. */
    private fun updateSettings(
        debounce: Boolean = true,
        transform: EditorSettings.() -> EditorSettings,
    ) {
        if (_uiState.value.source == null || _uiState.value.isExporting) return
        val updated = _uiState.value.settings.transform()
        if (updated == _uiState.value.settings) return
        _uiState.update { it.copy(settings = updated, isGeneratingPreview = true) }
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            if (debounce) delay(PREVIEW_DEBOUNCE_MS)
            val source = _uiState.value.source ?: return@launch
            generatePreviews(source.bitmap, updated)
        }
    }

    private suspend fun generatePreviews(
        source: android.graphics.Bitmap,
        settings: EditorSettings,
    ) {
        val previews = withContext(Dispatchers.Default) {
            val generated = container.renderer.renderPreviews(source, settings)
            try {
                coroutineContext.ensureActive()
                generated
            } catch (error: Throwable) {
                generated.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                throw error
            }
        }
        val current = _uiState.value
        if (current.source?.bitmap !== source || current.settings != settings) {
            previews.forEach { it.bitmap.recycle() }
            return
        }
        _uiState.update {
            it.copy(previews = previews, isGeneratingPreview = false)
        }
        disposePreviewsLater(current.previews)
    }

    private fun runExport(
        label: String,
        block: suspend (android.graphics.Bitmap, EditorSettings) -> Unit,
    ) {
        val state = _uiState.value
        val source = state.source?.bitmap ?: return
        if (!state.hasResults || state.isExporting) return
        val settings = state.settings
        exportJob = viewModelScope.launch {
            _uiState.update {
                it.copy(exportProgress = ExportProgress(label, 0, settings.gridSpec.tileCount))
            }
            try {
                block(source, settings)
            } catch (_: CancellationException) {
                throw CancellationException()
            } catch (error: Throwable) {
                _effects.emit(
                    EditorEffect.ShowMessage(
                        error.messageFor(
                            _uiState.value.language,
                            fallbackChinese = "操作失败，请重试",
                            fallbackEnglish = "Something went wrong. Please try again",
                        ),
                    ),
                )
            } finally {
                _uiState.update { it.copy(exportProgress = null) }
            }
        }
    }

    private fun updateProgress(completed: Int, total: Int) {
        _uiState.update { state ->
            state.copy(exportProgress = state.exportProgress?.copy(completed = completed, total = total))
        }
    }

    private fun text(chinese: String, english: String): String =
        _uiState.value.language.text(chinese, english)

    // Compose may still draw the previous state for one frame after StateFlow changes. A short
    // grace period avoids recycling a bitmap while that frame is being submitted to the GPU.
    private fun disposeBitmapsLater(
        source: com.ninegrid.app.core.model.SourceImage?,
        previews: List<com.ninegrid.app.core.model.TilePreview>,
    ) {
        viewModelScope.launch {
            delay(BITMAP_DISPOSAL_DELAY_MS)
            previews.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            source?.bitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun disposePreviewsLater(previews: List<com.ninegrid.app.core.model.TilePreview>) {
        viewModelScope.launch {
            delay(BITMAP_DISPOSAL_DELAY_MS)
            previews.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        }
    }

    override fun onCleared() {
        previewJob?.cancel()
        exportJob?.cancel()
        if (previewJob?.isCompleted != false && exportJob?.isCompleted != false) {
            _uiState.value.previews.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            _uiState.value.source?.bitmap?.let { if (!it.isRecycled) it.recycle() }
        }
        super.onCleared()
    }

    companion object {
        private const val PREVIEW_DEBOUNCE_MS = 140L
        private const val BITMAP_DISPOSAL_DELAY_MS = 120L

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(NineGridViewModel::class.java))
                    return NineGridViewModel(container) as T
                }
            }
    }
}
