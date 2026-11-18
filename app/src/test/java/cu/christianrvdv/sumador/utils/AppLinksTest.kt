package cu.christianrvdv.sumador.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLinksTest {
    @Test
    fun downloadUrl_isTheOfficialLandingPage() {
        assertEquals("https://christianrvdv.github.io/Sumador/", AppLinks.DOWNLOAD_URL)
    }
}
