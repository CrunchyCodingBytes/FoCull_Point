package com.example.focullpointv2

import com.example.focullpointv2.filemanager.DefaultFileMover
import com.example.focullpointv2.filemanager.MoveResult
import com.example.focullpointv2.model.ConflictStrategy
import com.example.focullpointv2.model.PhotoItem
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileMoverTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mover = DefaultFileMover()

    @Test
    fun `move succeeds and removes sources`() {
        val sourceDir = tempFolder.newFolder("source")
        val destDir = tempFolder.newFolder("dest")
        
        val f1 = File(sourceDir, "test.jpg").apply { writeText("content1") }
        val f2 = File(sourceDir, "test.arw").apply { writeText("content2") }
        
        val item = PhotoItem("test", listOf(f1, f2), f1)
        
        val result = mover.move(item, destDir, null)
        
        assertTrue(result is MoveResult.Success)
        assertFalse(f1.exists())
        assertFalse(f2.exists())
        assertTrue(File(destDir, "test.jpg").exists())
        assertTrue(File(destDir, "test.arw").exists())
        assertEquals("content1", File(destDir, "test.jpg").readText())
    }

    @Test
    fun `move returns conflict when dest exists and strategy null`() {
        val sourceDir = tempFolder.newFolder("source")
        val destDir = tempFolder.newFolder("dest")
        
        val f1 = File(sourceDir, "test.jpg").apply { writeText("new") }
        File(destDir, "test.jpg").apply { writeText("old") }
        
        val item = PhotoItem("test", listOf(f1), f1)
        
        val result = mover.move(item, destDir, null)
        
        assertTrue(result is MoveResult.Conflict)
        val conflict = result as MoveResult.Conflict
        assertEquals(1, conflict.conflictingFiles.size)
        assertEquals("test.jpg", conflict.conflictingFiles[0].name)
        assertTrue(f1.exists())
    }

    @Test
    fun `move with REPLACE overwrites existing`() {
        val sourceDir = tempFolder.newFolder("source")
        val destDir = tempFolder.newFolder("dest")
        
        val f1 = File(sourceDir, "test.jpg").apply { writeText("new") }
        File(destDir, "test.jpg").apply { writeText("old") }
        
        val item = PhotoItem("test", listOf(f1), f1)
        
        val result = mover.move(item, destDir, ConflictStrategy.REPLACE)
        
        assertTrue(result is MoveResult.Success)
        assertEquals("new", File(destDir, "test.jpg").readText())
    }

    @Test
    fun `move with SKIP does nothing`() {
        val sourceDir = tempFolder.newFolder("source")
        val destDir = tempFolder.newFolder("dest")
        
        val f1 = File(sourceDir, "test.jpg").apply { writeText("new") }
        File(destDir, "test.jpg").apply { writeText("old") }
        
        val item = PhotoItem("test", listOf(f1), f1)
        
        val result = mover.move(item, destDir, ConflictStrategy.SKIP)
        
        assertTrue(result is MoveResult.Success)
        assertTrue(f1.exists())
        assertEquals("old", File(destDir, "test.jpg").readText())
    }

    @Test
    fun `move with RENAME increments suffix for pair`() {
        val sourceDir = tempFolder.newFolder("source")
        val destDir = tempFolder.newFolder("dest")
        
        val f1 = File(sourceDir, "test.jpg").apply { writeText("c1") }
        val f2 = File(sourceDir, "test.arw").apply { writeText("c2") }
        
        // Create conflict for test_1.jpg but not test_1.arw
        File(destDir, "test_1.jpg").apply { writeText("blocked") }
        
        val item = PhotoItem("test", listOf(f1, f2), f1)
        
        val result = mover.move(item, destDir, ConflictStrategy.RENAME)
        
        assertTrue(result is MoveResult.Success)
        
        // Should have skipped _1 because test_1.jpg exists, so should use _2
        assertTrue(File(destDir, "test_2.jpg").exists())
        assertTrue(File(destDir, "test_2.arw").exists())
        assertEquals("c1", File(destDir, "test_2.jpg").readText())
        assertEquals("c2", File(destDir, "test_2.arw").readText())
    }
}
