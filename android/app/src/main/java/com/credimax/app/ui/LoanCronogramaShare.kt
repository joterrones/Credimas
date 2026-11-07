package com.credimax.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.credimax.app.data.LoanDto
import java.io.File
import java.io.FileOutputStream

/** Único texto al compartir: saludo (el cronograma va como imagen). */
fun buildLoanCronogramaGreeting(loan: LoanDto): String {
    val nombre = loan.client?.nombre?.ifBlank { null } ?: "cliente"
    return "Estimado(a) $nombre, le enviamos su cronograma de pagos"
}

fun formatPct(value: Double): String {
    val t = value.toString().trimEnd('0').trimEnd('.')
    return t.ifBlank { "0" }
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String = "cronograma"): Uri {
    val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Abre WhatsApp con imagen + caption de texto. false si no está instalado. */
fun shareImageAndTextViaWhatsApp(context: Context, imageUri: Uri, text: String): Boolean {
    val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
    for (pkg in packages) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            setPackage(pkg)
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newUri(context.contentResolver, "cronograma", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // probar siguiente
        }
    }
    return false
}
