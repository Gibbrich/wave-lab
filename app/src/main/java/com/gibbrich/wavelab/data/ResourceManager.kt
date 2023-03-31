package com.gibbrich.wavelab.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream
import java.io.OutputStream

class ResourceManagerImpl(private val context: Context) : ResourceManager {
    override fun openOutputStream(uri: Uri, mode: String): OutputStream? =
        context.contentResolver.openOutputStream(uri, "wt")

    override fun openInputStream(uri: Uri): InputStream? =
        context.contentResolver.openInputStream(uri)

    override fun getFileName(uri: Uri): String {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    return it.getString(columnIndex)
                }
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    }

}

interface ResourceManager {
    fun openOutputStream(uri: Uri, mode: String): OutputStream?
    fun openInputStream(uri: Uri): InputStream?
    fun getFileName(uri: Uri): String
}