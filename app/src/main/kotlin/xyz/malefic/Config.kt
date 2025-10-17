package xyz.malefic

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Configuration for Filame - manages Arch Linux packages and their configurations
 */
@Serializable
data class FilameConfig(
    val deviceName: String = "",
    val githubRepo: String = "",
    val packageBundles: List<PackageBundle> = emptyList(),
    val ignorePatterns: List<String> =
        listOf(
            "*.log",
            "*.tmp",
            ".cache/*",
            "*.lock",
        ),
    val mockMode: Boolean = false, // Enable mock mode for non-Linux environments
)

/**
 * Represents a configuration file to be synced
 */
@Serializable
data class ConfigFile(
    val sourcePath: String,
    val destinationPath: String,
    val description: String = "",
)

/**
 * Represents a package bundle with its configuration files
 * This is the primary entity in Filame - packages with their associated configs
 */
@Serializable
data class PackageBundle(
    val name: String,
    val source: String = "official", // "official" or "aur"
    val description: String = "",
    val configFiles: List<ConfigFile> = emptyList(),
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
