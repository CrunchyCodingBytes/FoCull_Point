package com.example.focullpointv2.filemanager

import androidx.exifinterface.media.ExifInterface
import java.io.File

class RawThumbnailExtractor {
    /**
     * Extracts the embedded JPEG thumbnail from a RAW [file] using ExifInterface.
     * Returns null if no thumbnail is found or on exception.
     */
    fun extractThumbnail(file: File): ByteArray? {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.thumbnailBytes
        } catch (e: Exception) {
            null
        }
    }
}
