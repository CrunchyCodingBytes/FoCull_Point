package com.example.focullpointv2.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Helpers for All-Files-Access permission and for resolving folder-picker tree
 * URIs back to [File] paths (the app uses direct java.io.File access).
 */
object StorageAccess {

    /** True when the app can freely read/write the shared storage. */
    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /** Intent that routes the user to grant All-Files Access (or legacy runtime request). */
    fun requestAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * Resolves a folder-picker tree [Uri] to an absolute filesystem path.
     * Supports the primary shared volume ("primary") and named secondary volumes.
     * Returns null if it cannot be resolved.
     */
    fun resolveTreeUriToFile(treeUri: Uri): File? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parts = docId.split(":", limit = 2)
            val volume = parts[0]
            val relativePath = parts.getOrElse(1) { "" }
            val base: File = when {
                volume.equals("primary", ignoreCase = true) ->
                    Environment.getExternalStorageDirectory()
                else -> File("/storage/$volume")
            }
            if (relativePath.isEmpty()) base else File(base, relativePath)
        } catch (e: Exception) {
            null
        }
    }
}
