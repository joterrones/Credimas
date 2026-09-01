package com.credimax.app.data

import android.content.Context
import android.net.Uri
import com.credimax.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

fun textPart(value: String): RequestBody =
    value.toRequestBody("text/plain".toMediaTypeOrNull())

fun mediaUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return BuildConfig.BASE_URL.trimEnd('/') + if (path.startsWith("/")) path else "/$path"
}

fun uriToPart(context: Context, uri: Uri, fieldName: String): MultipartBody.Part {
    val cr = context.contentResolver
    val type = cr.getType(uri) ?: "image/jpeg"
    val name = "$fieldName.${if (type.contains("pdf")) "pdf" else "jpg"}"
    val input = cr.openInputStream(uri) ?: error("No se pudo leer el archivo")
    val file = File(context.cacheDir, "${System.currentTimeMillis()}_$name")
    file.outputStream().use { out -> input.use { it.copyTo(out) } }
    val body = file.asRequestBody(type.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, name, body)
}
