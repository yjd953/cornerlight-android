package com.ninegrid.app.core.model

/** Supported publication layouts; ordering is always row-major. */
enum class GridSpec(
    val rows: Int,
    val columns: Int,
    private val chineseName: String,
    private val englishName: String,
    val description: String,
) {
    FOUR(2, 2, "四宫格", "4 tiles", "2 × 2"),
    NINE(3, 3, "九宫格", "9 tiles", "3 × 3"),
    TWELVE(3, 4, "十二宫格", "12 tiles", "4 × 3"),
    ;

    val tileCount: Int = rows * columns

    fun displayName(language: AppLanguage): String = language.text(chineseName, englishName)
}
