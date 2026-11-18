package cu.christianrvdv.sumador.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsAboutNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutItem_dismissesSheetBeforeNavigating() {
        val sheetVisible = mutableStateOf(true)
        var navigations = 0
        var sheetVisibleAtNavigation: Boolean? = null

        composeTestRule.setContent {
            MaterialTheme {
                if (sheetVisible.value) {
                    SettingsBottomSheet(
                        settingsState = SettingsState(),
                        onDismiss = { sheetVisible.value = false },
                        onThemeChange = {},
                        onCurrencyChange = {},
                        onSortChange = {},
                        onAutoSaveChange = {},
                        onConfirmClearChange = {},
                        onLanguageChange = {},
                        onKeepScreenOnChange = {},
                        onUseCoinsChange = {},
                        onAboutClick = {
                            navigations++
                            sheetVisibleAtNavigation = sheetVisible.value
                        },
                        onCheckForUpdates = {},
                        onManageDenominations = {},
                        onExportRequest = {},
                        onImportRequest = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("settings_about_item").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, navigations)
        assertFalse(sheetVisibleAtNavigation!!)
        assertFalse(sheetVisible.value)
    }
}
