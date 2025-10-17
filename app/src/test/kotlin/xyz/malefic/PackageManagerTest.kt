package xyz.malefic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PackageManagerTest {
    @Test
    fun testPackageBundleCreation() {
        val bundle = PackageBundle(
            name = "vim",
            source = "official",
            description = "Text editor",
            configFiles = listOf(
                ConfigFile("/home/user/.vimrc", "vim/.vimrc", "Vim config")
            )
        )
        
        assertEquals("vim", bundle.name)
        assertEquals("official", bundle.source)
        assertEquals("Text editor", bundle.description)
        assertEquals(1, bundle.configFiles.size)
    }

    @Test
    fun testPackageBundleAurSource() {
        val bundle = PackageBundle(
            name = "yay",
            source = "aur",
            description = "AUR helper"
        )
        
        assertEquals("aur", bundle.source)
    }

    @Test
    fun testConfigWithPackageBundles() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            packageBundles = listOf(
                PackageBundle("vim", "official", "Text editor"),
                PackageBundle("yay", "aur", "AUR helper")
            )
        )
        
        assertEquals(2, config.packageBundles.size)
        assertEquals("vim", config.packageBundles[0].name)
        assertEquals("yay", config.packageBundles[1].name)
    }

    @Test
    fun testPackageManagerCreation() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            packageBundles = listOf(
                PackageBundle("vim", "official", "Text editor")
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
            packageBundles = emptyList()
        )
        
        val packageManager = PackageManager(config)
        val statuses = packageManager.getPackageStatuses()
        
        assertTrue(statuses.isEmpty())
    }

    @Test
    fun testPackageBundleWithMultipleConfigs() {
        val bundle = PackageBundle(
            name = "i3",
            source = "official",
            description = "Tiling window manager",
            configFiles = listOf(
                ConfigFile("/home/user/.config/i3/config", "i3/config", "Main config"),
                ConfigFile("/home/user/.config/i3/status.conf", "i3/status.conf", "Status bar config")
            )
        )
        
        assertEquals(2, bundle.configFiles.size)
        assertEquals("i3/config", bundle.configFiles[0].destinationPath)
        assertEquals("i3/status.conf", bundle.configFiles[1].destinationPath)
    }

    @Test
    fun testMockModeDefaultValue() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git"
        )
        
        assertEquals(false, config.mockMode)
    }

    @Test
    fun testMockModeEnabled() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            mockMode = true
        )
        
        assertTrue(config.mockMode)
    }

    @Test
    fun testPackageManagerWithMockMode() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            packageBundles = listOf(
                PackageBundle("vim", "official", "Text editor")
            ),
            mockMode = true
        )
        
        val packageManager = PackageManager(config)
        assertNotNull(packageManager)
        
        // In mock mode, paru is always considered installed
        assertTrue(packageManager.isParuInstalled())
    }

    @Test
    fun testInstallPackageInMockMode() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            mockMode = true
        )
        
        val packageManager = PackageManager(config)
        val bundle = PackageBundle("vim", "official", "Text editor")
        
        // In mock mode, installation should succeed without errors
        val result = packageManager.installPackage(bundle)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testInstallParuInMockMode() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            mockMode = true
        )
        
        val packageManager = PackageManager(config)
        
        // In mock mode, paru installation should succeed
        val result = packageManager.installParu()
        assertTrue(result.isSuccess)
    }

    @Test
    fun testExportPackageMetadata() {
        val config = FilameConfig(
            deviceName = "test-device",
            githubRepo = "https://github.com/test/repo.git",
            mockMode = true
        )
        
        val bundle = PackageBundle(
            name = "vim",
            source = "official",
            description = "Text editor",
            configFiles = listOf(
                ConfigFile("/home/user/.vimrc", "vim/.vimrc", "Vim config")
            )
        )
        
        val packageManager = PackageManager(config)
        val result = packageManager.exportPackageMetadata(bundle)
        
        // Metadata export should succeed
        assertTrue(result.isSuccess)
        val metadataPath = result.getOrNull()
        assertNotNull(metadataPath)
        assertTrue(metadataPath!!.contains("package.yaml"))
    }
}
