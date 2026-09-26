package com.ninegrid.app.ui.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ninegrid.app.MainActivity
import com.ninegrid.app.core.model.AppLanguage
import com.ninegrid.app.data.preferences.LanguageRepository
import org.junit.Rule
import org.junit.Test

class NineGridLandingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun landingScreenSwitchesBetweenChineseAndEnglish() {
        val initialLanguage = LanguageRepository(composeRule.activity).getLanguage()
        assertLandingLanguage(initialLanguage)

        composeRule.onNodeWithTag("language-switch").performClick()
        assertLandingLanguage(initialLanguage.toggled())

        composeRule.onNodeWithTag("language-switch").performClick()
        assertLandingLanguage(initialLanguage)
    }

    private fun assertLandingLanguage(language: AppLanguage) {
        if (language == AppLanguage.CHINESE) {
            composeRule.onNodeWithText("从一张，\n成为九张。").assertIsDisplayed()
            composeRule.onNodeWithText("选择一张照片").assertIsDisplayed()
            composeRule.onNodeWithText("EN").assertIsDisplayed()
        } else {
            composeRule.onNodeWithText("One photo.\nNine moments.").assertIsDisplayed()
            composeRule.onNodeWithText("Choose a photo").assertIsDisplayed()
            composeRule.onNodeWithText("中").assertIsDisplayed()
        }
    }
}
