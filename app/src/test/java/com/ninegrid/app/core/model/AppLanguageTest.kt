package com.ninegrid.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `toggle labels describe the target language`() {
        assertEquals("EN", AppLanguage.CHINESE.switchLabel)
        assertEquals("中", AppLanguage.ENGLISH.switchLabel)
    }

    @Test
    fun `toggle switches between both supported languages`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.CHINESE.toggled())
        assertEquals(AppLanguage.CHINESE, AppLanguage.ENGLISH.toggled())
    }

    @Test
    fun `text selects the current language`() {
        assertEquals("中文", AppLanguage.CHINESE.text("中文", "English"))
        assertEquals("English", AppLanguage.ENGLISH.text("中文", "English"))
    }
}
