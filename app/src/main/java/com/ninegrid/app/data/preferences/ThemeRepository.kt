package com.ninegrid.app.data.preferences

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit

class ThemeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("nine_grid_preferences", Context.MODE_PRIVATE)

    fun isDarkTheme(): Boolean = if (preferences.contains(KEY_DARK_THEME)) {
        preferences.getBoolean(KEY_DARK_THEME, false)
    } else {
        val nightMode = appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightMode == Configuration.UI_MODE_NIGHT_YES
    }

    fun setDarkTheme(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_DARK_THEME, enabled) }
    }

    private companion object {
        const val KEY_DARK_THEME = "dark_theme"
    }
}
