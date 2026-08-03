package com.example.focullpointv2.filemanager

import com.example.focullpointv2.model.ConflictStrategy
import com.example.focullpointv2.model.PhotoItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

sealed interface MoveResult {
    /** [movedFiles] are the final destination paths, in the same order as the item's files. */
    data class Success(val movedFiles: List<File> = emptyList()) : MoveResult
    data class Conflict(val conflictingFiles: List<File>) : MoveResult
    data class Error(val throwable: Throwable) : MoveResult
}

interface FileMover {
    fun move(item: PhotoItem, destDir: File, strategy: ConflictStrategy?): MoveResult

    /**
     * Moves each `(currentFile -> targetFile)` pair back to its exact target path
     * (copy-verify-delete, overwriting the target). Used to undo a previous move.
     */
    fun moveBack(mappings: List<Pair<File, File>>): MoveResult
}

class DefaultFileMover : FileMover {

    override fun move(item: PhotoItem, destDir: File, strategy: ConflictStrategy?): MoveResult {
        return try {
            if (!destDir.exists() && !destDir.mkdirs()) {
                return MoveResult.Error(IOException("Could not create destination directory: ${destDir.absolutePath}"))
            }

            val finalMappings = when (strategy) {
                null -> {
                    val actualConflicts = item.files.map { it to File(destDir, it.name) }
                        .filter { (src, dest) -> dest.exists() && src.absolutePath != dest.absolutePath }
                    
                    if (actualConflicts.isNotEmpty()) {
                        return MoveResult.Conflict(actualConflicts.map { it.second })
                    }
                    item.files.associateWith { File(destDir, it.name) }
                }
                ConflictStrategy.SKIP -> {
                    // Check if any destination exists and is not the source. 
                    // If we SKIP, we just return Success without moving anything.
                    // The requirement says: "SKIP: do nothing, return Success (no files moved)."
                    return MoveResult.Success(emptyList())
                }
                ConflictStrategy.REPLACE -> {
                    item.files.associateWith { File(destDir, it.name) }
                }
                ConflictStrategy.RENAME -> {
                    computeRenameMappings(item, destDir)
                }
            }

            for ((source, destination) in finalMappings) {
                moveFile(source, destination)
            }

            // Report moved destinations in the same order as the item's files.
            val movedFiles = item.files.map { finalMappings.getValue(it) }
            MoveResult.Success(movedFiles)
        } catch (e: Exception) {
            MoveResult.Error(e)
        }
    }

    override fun moveBack(mappings: List<Pair<File, File>>): MoveResult {
        return try {
            for ((current, target) in mappings) {
                val parent = target.parentFile
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    return MoveResult.Error(IOException("Could not create directory: ${parent.absolutePath}"))
                }
                moveFile(current, target)
            }
            MoveResult.Success(mappings.map { it.second })
        } catch (e: Exception) {
            MoveResult.Error(e)
        }
    }

    private fun computeRenameMappings(item: PhotoItem, destDir: File): Map<File, File> {
        var n = 1
        while (true) {
            val suffix = "_$n"
            val mappings = item.files.associateWith { source ->
                val newName = "${source.nameWithoutExtension}$suffix.${source.extension}"
                File(destDir, newName)
            }
            
            val collision = mappings.values.any { it.exists() && !item.files.any { f -> f.absolutePath == it.absolutePath } }
            if (!collision) return mappings
            n++
        }
    }

    private fun moveFile(source: File, destination: File) {
        if (source.absolutePath == destination.absolutePath) return

        val tempFile = File(destination.parentFile, ".${destination.name}.tmp")
        
        try {
            // Copy source to temp
            copyStream(source, tempFile)
            
            // Verify length
            if (tempFile.length() != source.length()) {
                tempFile.delete()
                throw IOException("Verification failed: Copied file length mismatch for ${source.name}")
            }
            
            // Replace destination if it exists
            if (destination.exists()) {
                if (!destination.delete()) {
                    throw IOException("Could not delete existing destination file: ${destination.absolutePath}")
                }
            }
            
            // Atomically rename temp to final
            if (!tempFile.renameTo(destination)) {
                // Fallback to stream copy
                copyStream(tempFile, destination)
                tempFile.delete()
            }
            
            // Delete source
            source.delete()
            
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    private fun copyStream(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
            }
        }
    }
}

object CullFolders {
    fun ensureCullFolders(sourceDir: File): Pair<File, File> {
        val favoritesDir = File(sourceDir, "Favorites")
        val rejectsDir = File(sourceDir, "Rejects")
        
        if (!favoritesDir.exists()) favoritesDir.mkdirs()
        if (!rejectsDir.exists()) rejectsDir.mkdirs()
        
        return Pair(favoritesDir, rejectsDir)
    }
}
