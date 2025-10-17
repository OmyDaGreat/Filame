package xyz.malefic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PackageManagerTest {
    @Test
    fun testPackageCreation() {
        val pkg = Package(
            name = "vim",
            source = "official",
            description = "Text editor"
        )
        
        assertEquals("vim", pkg.name)
        assertEquals("official", pkg.source)
        assertEquals("Text editor", pkg.description)
    }

    @Test
    fun testPackageAurSource() {
        val pkg = Package(
            name = "yay",
            source = "aur",
            description = "AUR helper"
        )
        
        assertEquals("aur", pkg.source)
    }

    @Test
    fun testConfigWithPackages() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            packages = listOf(
                Package("vim", "official", "Text editor"),
                Package("yay", "aur", "AUR helper")
            )
        )
        
        assertEquals(2, config.packages.size)
        assertEquals("vim", config.packages[0].name)
        assertEquals("yay", config.packages[1].name)
    }

    @Test
    fun testPackageManagerCreation() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            packages = listOf(
                Package("vim", "official", "Text editor")
            )
        )
        
        val packageManager = PackageManager(config)
        assertNotNull(packageManager)
    }

    @Test
    fun testPackageSearchResultCreation() {
        val result = PackageSearchResult(
            name = "firefox",
            source = "official",
            description = "Web browser"
        )
        
        assertEquals("firefox", result.name)
        assertEquals("official", result.source)
        assertEquals("Web browser", result.description)
    }

    @Test
    fun testGetPackageStatusesWithEmptyList() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            packages = emptyList()
        )
        
        val packageManager = PackageManager(config)
        val statuses = packageManager.getPackageStatuses()
        
        assertTrue(statuses.isEmpty())
    }
}
