package com.example.focullpointv2.filemanager

import java.io.File
import java.util.Locale

object SupportedExtensions {
    val RAW = setOf(
        "arw", "cr2", "cr3", "nef", "nrw", "orf", "raf", "rw2", "dng", "pef",
        "srw", "srf", "sr2", "raw", "kdc", "dcr", "x3f", "mrw", "3fr", "mef",
        "iiq", "gpr"
    )

    val JPEG = setOf("jpg", "jpeg")

    val OTHER_IMAGE = setOf("png", "heic", "heif", "webp", "bmp", "gif")

    val ALL = RAW + JPEG + OTHER_IMAGE

    fun isSupported(file: File): Boolean {
        return ALL.contains(file.extension.lowercase(Locale.ROOT))
    }

    fun isRaw(file: File): Boolean {
        return RAW.contains(file.extension.lowercase(Locale.ROOT))
    }

    fun isJpeg(file: File): Boolean {
        return JPEG.contains(file.extension.lowercase(Locale.ROOT))
    }
}
