package com.example.focullpointv2

import com.example.focullpointv2.filemanager.PhotoScanner
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PhotoScannerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scanner = PhotoScanner()

    @Test
    fun `scan groups jpg and arw together`() {
        val root = tempFolder.newFolder()
        File(root, "IMG_001.JPG").createNewFile()
        File(root, "IMG_001.ARW").createNewFile()
        
        val results = scanner.scan(root)
        
        assertEquals(1, results.size)
        val item = results[0]
        assertEquals("IMG_001", item.baseName)
        assertEquals(2, item.files.size)
        assertEquals("IMG_001.JPG", item.previewFile.name)
        assertTrue(item.files.any { it.name == "IMG_001.JPG" })
        assertTrue(item.files.any { it.name == "IMG_001.ARW" })
    }

    @Test
    fun `scan handles raw only`() {
        val root = tempFolder.newFolder()
        File(root, "IMG_002.NEF").createNewFile()
        
        val results = scanner.scan(root)
        
        assertEquals(1, results.size)
        assertEquals("IMG_002.NEF", results[0].previewFile.name)
    }

    @Test
    fun `scan filters out unsupported files`() {
        val root = tempFolder.newFolder()
        File(root, "IMG_003.JPG").createNewFile()
        File(root, "IMG_003.TXT").createNewFile()
        File(root, "random.pdf").createNewFile()
        
        val results = scanner.scan(root)
        
        assertEquals(1, results.size)
        assertEquals(1, results[0].files.size)
        assertEquals("IMG_003.JPG", results[0].files[0].name)
    }

    @Test
    fun `scan is case-insensitive for base name grouping`() {
        val root = tempFolder.newFolder()
        File(root, "img_004.jpg").createNewFile()
        File(root, "IMG_004.ARW").createNewFile()
        
        val results = scanner.scan(root)
        
        assertEquals(1, results.size)
        assertEquals(2, results[0].files.size)
    }

    @Test
    fun `scan results are sorted by base name`() {
        val root = tempFolder.newFolder()
        File(root, "B.jpg").createNewFile()
        File(root, "A.jpg").createNewFile()
        File(root, "c.jpg").createNewFile()
        
        val results = scanner.scan(root)
        
        assertEquals(3, results.size)
        assertEquals("A", results[0].baseName)
        assertEquals("B", results[1].baseName)
        assertEquals("c", results[2].baseName)
    }
}
