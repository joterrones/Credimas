package com.credimax.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.credimax.app.ui.theme.SlateMuted
import java.io.File

class PhotoActions(
    val uri: Uri?,
    val onUri: (Uri?) -> Unit,
    val pickGallery: () -> Unit,
    val takePhoto: () -> Unit,
)

/** Registrar en la pantalla (no dentro de un AlertDialog) para que la cámara no cierre el flujo. */
@Composable
fun rememberPhotoActions(uri: Uri?, onUri: (Uri?) -> Unit): PhotoActions {
    val context = LocalContext.current
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked != null) onUri(picked)
    }
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCamera?.let { onUri(Uri.parse(it)) }
    }
    return PhotoActions(
        uri = uri,
        onUri = onUri,
        pickGallery = { pick.launch("image/*") },
        takePhoto = {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            val u = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCamera = u.toString()
            take.launch(u)
        },
    )
}

@Composable
fun OptionalPhotoPicker(
    uri: Uri?,
    onUri: (Uri?) -> Unit,
    title: String,
    hint: String,
) {
    OptionalPhotoPicker(rememberPhotoActions(uri, onUri), title, hint)
}

@Composable
fun OptionalPhotoPicker(
    actions: PhotoActions,
    title: String,
    hint: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.pickGallery, modifier = Modifier.weight(1f)) {
                Text("Galería")
            }
            OutlinedButton(onClick = actions.takePhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Text("  Cámara")
            }
        }
        if (actions.uri != null) {
            AsyncImage(
                model = actions.uri,
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentScale = ContentScale.Crop,
            )
            TextButton(onClick = { actions.onUri(null) }) { Text("Quitar foto") }
        } else {
            Text(hint, color = SlateMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
