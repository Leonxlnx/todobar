package dev.todobar.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import dev.todobar.mobile.store.Store

/**
 * Invisible launcher that lets the in-overlay Settings view pick a backdrop
 * image. We use SAF's OpenDocument with a persistable read-URI permission,
 * then write the URI into the shared Store so the overlay re-renders.
 */
class BackdropPickerActivity : ComponentActivity() {

    private val launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val name = uri.lastPathSegment?.substringAfterLast('/')?.take(80).orEmpty()
            val store = Store.get(this)
            store.saveSettings(store.settings().copy(
                backdropImageUri = uri.toString(),
                backdropImageName = name,
            ))
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher.launch(arrayOf("image/*"))
    }
}
