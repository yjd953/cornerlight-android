package com.ninegrid.app.core.model

enum class AppLanguage {
    CHINESE,
    ENGLISH,
    ;

    val switchLabel: String
        get() = if (this == CHINESE) "EN" else "中"

    fun toggled(): AppLanguage = if (this == CHINESE) ENGLISH else CHINESE

    fun text(chinese: String, english: String): String =
        if (this == CHINESE) chinese else english
}

interface LocalizedMessage {
    val chineseMessage: String
    val englishMessage: String
}

fun Throwable.messageFor(
    language: AppLanguage,
    fallbackChinese: String,
    fallbackEnglish: String,
): String {
    val localized = this as? LocalizedMessage
    return if (localized != null) {
        language.text(localized.chineseMessage, localized.englishMessage)
    } else {
        message ?: language.text(fallbackChinese, fallbackEnglish)
    }
}
