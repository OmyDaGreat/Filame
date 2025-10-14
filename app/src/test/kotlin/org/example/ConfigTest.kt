package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString

class ConfigTest {
    
    @Test
    fun testFilameConfigSerialization() {
        val config = FilameConfig(
            deviceName = "arch-laptop",
            githubRepo = "https://github.com/user/configs.git",
            configFiles = listOf(
                ConfigFile(
                    sourcePath = "/home/user/.config/i3/config",
                    destinationPath = "i3/config",
                    description = "i3 window manager config"
                )
            ),
            ignorePatterns = listOf("*.log", "*.tmp")
        )
        
        val yaml = Yaml.default.encodeToString(FilameConfig.serializer(), config)
        assertNotNull(yaml)
        assertTrue(yaml.contains("arch-laptop"))
        assertTrue(yaml.contains("i3/config"))
    }
    
    @Test
    fun testFilameConfigDeserialization() {
        val yaml = """
            deviceName: arch-desktop
            githubRepo: https://github.com/user/dotfiles.git
            configFiles:
              - sourcePath: /home/user/.bashrc
                destinationPath: bashrc
                description: Bash configuration
            ignorePatterns:
              - "*.log"
              - "*.cache"
        """.trimIndent()
        
        val config = Yaml.default.decodeFromString(FilameConfig.serializer(), yaml)
        assertEquals("arch-desktop", config.deviceName)
        assertEquals("https://github.com/user/dotfiles.git", config.githubRepo)
        assertEquals(1, config.configFiles.size)
        assertEquals("/home/user/.bashrc", config.configFiles[0].sourcePath)
        assertEquals(2, config.ignorePatterns.size)
    }
    
    @Test
    fun testConfigFileCreation() {
        val configFile = ConfigFile(
            sourcePath = "/etc/pacman.conf",
            destinationPath = "pacman.conf",
            description = "Pacman package manager config"
        )
        
        assertEquals("/etc/pacman.conf", configFile.sourcePath)
        assertEquals("pacman.conf", configFile.destinationPath)
        assertEquals("Pacman package manager config", configFile.description)
    }
    
    @Test
    fun testDefaultIgnorePatterns() {
        val config = FilameConfig()
        assertTrue(config.ignorePatterns.isNotEmpty())
        assertTrue(config.ignorePatterns.contains("*.log"))
        assertTrue(config.ignorePatterns.contains("*.tmp"))
    }
    
    @Test
    fun testConfigManagerPaths() {
        ConfigManager.ensureConfigDir()
        val configFile = ConfigManager.getConfigFile()
        assertNotNull(configFile)
        assertTrue(configFile.path.contains(".config/filame"))
    }
}
