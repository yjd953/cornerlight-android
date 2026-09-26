package com.ninegrid.app.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.ninegrid.app.core.model.AppLanguage

class LanguageRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("nine_grid_preferences", Context.MODE_PRIVATE)

    fun getLanguage(): AppLanguage {
        val stored = preferences.getString(KEY_LANGUAGE, null)
        return AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.CHINESE
    }

    fun setLanguage(language: AppLanguage) {
        preferences.edit { putString(KEY_LANGUAGE, language.name) }
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
    }
}
