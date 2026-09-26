package com.ninegrid.app.core.model

enum class CropMode(
    private val chineseName: String,
    private val englishName: String,
    private val chineseDescription: String,
    private val englishDescription: String,
) {
    COVER("裁满画面", "Fill frame", "方格更饱满", "Crop to fill every tile"),
    CONTAIN("保留全图", "Fit image", "不裁掉内容", "Keep the entire image"),
    ;

    fun displayName(language: AppLanguage): String = language.text(chineseName, englishName)

    fun description(language: AppLanguage): String =
        language.text(chineseDescription, englishDescription)
}
