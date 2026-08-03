package com.example.focullpointv2.model

import java.io.File

/**
 * A single culling unit. A RAW + JPEG pair sharing a base name is one [PhotoItem];
 * all [files] move together while only [previewFile] is displayed.
 */
data class PhotoItem(
    val baseName: String,
    val files: List<File>,
    val previewFile: File
) {
    /** Stable id for list/queue identity (source path of the preview). */
    val id: String get() = previewFile.absolutePath
}
