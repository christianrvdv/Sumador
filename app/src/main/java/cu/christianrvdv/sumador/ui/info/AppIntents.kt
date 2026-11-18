package cu.christianrvdv.sumador.ui.info

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import cu.christianrvdv.sumador.R

interface AppIntents {
    fun openUrl(url: String): Boolean
    fun share(subject: String, text: String): Boolean
}

class AndroidAppIntents(private val context: Context) : AppIntents {
    override fun openUrl(url: String): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    override fun share(subject: String, text: String): Boolean = try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.info_share_chooser_title))
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
