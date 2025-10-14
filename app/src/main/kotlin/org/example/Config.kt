package org.example

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Configuration for Filame - manages Arch Linux configuration files across devices
 */
@Serializable
data class FilameConfig(
    val deviceName: String = "",
    val githubRepo: String = "",
    val configFiles: List<ConfigFile> = emptyList(),
    val ignorePatterns: List<String> = listOf(
        "*.log",
        "*.tmp",
        ".cache/*",
        "*.lock"
    )
)

/**
 * Represents a configuration file to be synced
 */
@Serializable
data class ConfigFile(
    val sourcePath: String,
    val destinationPath: String,
    val description: String = ""
)

/**
 * Configuration manager for loading and saving settings
 */
object ConfigManager {
    private val configDir = File(System.getProperty("user.home"), ".config/filame")
    private val configFile = File(configDir, "config.yaml")
    
    fun ensureConfigDir() {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }
    
    fun getConfigFile(): File = configFile
    
    fun configExists(): Boolean = configFile.exists()
}
