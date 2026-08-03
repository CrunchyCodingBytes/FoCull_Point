package com.example.focullpointv2.filemanager

import com.example.focullpointv2.model.PhotoItem
import java.io.File
import java.util.Locale

class PhotoScanner {
    /**
     * Scans [sourceDir] for supported photo files and groups them by base name.
     * Groups are returned as [PhotoItem]s sorted by base name (case-insensitive).
     */
    fun scan(sourceDir: File): List<PhotoItem> {
        val allFiles = sourceDir.listFiles() ?: return emptyList()
        
        val supportedFiles = allFiles.filter { it.isFile && SupportedExtensions.isSupported(it) }
        
        // Group by base name (file name without extension), case-insensitive
        val groups = supportedFiles.groupBy { it.nameWithoutExtension.lowercase(Locale.ROOT) }
        
        return groups.entries
            .mapNotNull { (lowercaseBaseName, files) ->
                if (files.isEmpty()) return@mapNotNull null
                
                // Determine preview file: JPEG (prefer jpg > jpeg) > OTHER_IMAGE > RAW
                val previewFile = files.find { it.extension.lowercase(Locale.ROOT) == "jpg" }
                    ?: files.find { it.extension.lowercase(Locale.ROOT) == "jpeg" }
                    ?: files.find { SupportedExtensions.OTHER_IMAGE.contains(it.extension.lowercase(Locale.ROOT)) }
                    ?: files.first() // Must be a RAW if it reached here and files is not empty
                
                // Use the baseName from the first file in the group for the PhotoItem
                // (or we could use the lowercaseBaseName, but usually original casing is nicer)
                // The requirements say "Sort resulting list by baseName (case-insensitive)".
                // It doesn't explicitly say which casing to use for the baseName field itself.
                // Let's use the most common base name or just the first one.
                val baseName = files.first().nameWithoutExtension
                
                PhotoItem(
                    baseName = baseName,
                    files = files,
                    previewFile = previewFile
                )
            }
            .sortedBy { it.baseName.lowercase(Locale.ROOT) }
    }
}
