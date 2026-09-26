package com.ninegrid.app

import android.content.Context
import com.ninegrid.app.core.image.GridRenderer
import com.ninegrid.app.data.export.ExportRepository
import com.ninegrid.app.data.image.AndroidImageLoader
import com.ninegrid.app.data.preferences.LanguageRepository
import com.ninegrid.app.data.preferences.ThemeRepository

/**
 * Explicit application-scoped dependency graph.
 *
 * The project is small enough that a DI framework would add more lifecycle and generated-code
 * surface than value; dependencies stay visible and replaceable from this single composition root.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val renderer = GridRenderer()
    val imageLoader = AndroidImageLoader(appContext)
    val exportRepository = ExportRepository(appContext, renderer)
    val themeRepository = ThemeRepository(appContext)
    val languageRepository = LanguageRepository(appContext)
}
