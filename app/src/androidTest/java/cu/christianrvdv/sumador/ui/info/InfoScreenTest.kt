package cu.christianrvdv.sumador.ui.info

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.utils.AppLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InfoScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var appContext: Context

    @Test
    fun rendersIdentityAndMigratedContent() {
        render()

        composeTestRule.onNodeWithTag("info_screen").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(appContext.getString(R.string.about_title))[0]
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("info_version").assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.about_version, "1.8.3")
        )[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.info_share_button)
        )[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.about_features_title)
        )[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.about_github_button)
        )[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText(
            appContext.getString(R.string.about_portfolio_button)
        )[0].assertIsDisplayed()
    }

    @Test
    fun shareButton_usesCentralUrlInSubjectAndText() {
        var clicks = 0
        var subject = ""
        var text = ""

        render(
            onShareApp = {
                clicks++
                subject = appContext.getString(R.string.info_share_subject)
                text = appContext.getString(R.string.info_share_text, AppLinks.DOWNLOAD_URL)
            }
        )

        composeTestRule.onNodeWithTag("info_share_button").performClick()

        assertEquals(1, clicks)
        assertEquals(appContext.getString(R.string.info_share_subject), subject)
        assertTrue(text.contains(AppLinks.DOWNLOAD_URL))
    }

    @Test
    fun officialPageButton_opensCentralUrl() {
        var openedUrl = ""

        render(onOpenOfficialPage = { openedUrl = AppLinks.DOWNLOAD_URL })

        composeTestRule.onNodeWithTag("info_official_page_button").performClick()

        assertEquals(AppLinks.DOWNLOAD_URL, openedUrl)
    }

    @Test
    fun backButton_invokesOnBack() {
        var backs = 0

        render(onBack = { backs++ })

        composeTestRule.onNodeWithContentDescription(
            appContext.getString(R.string.info_back)
        ).performClick()

        assertEquals(1, backs)
    }

    private fun render(
        onBack: () -> Unit = {},
        onOpenOfficialPage: () -> Unit = {},
        onShareApp: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            appContext = LocalContext.current
            MaterialTheme {
                InfoScreen(
                    onBack = onBack,
                    versionName = "1.8.3",
                    onOpenOfficialPage = onOpenOfficialPage,
                    onShareApp = onShareApp
                )
            }
        }
    }
}
