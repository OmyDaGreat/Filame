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
 * Manager responsible for handling Filame's configuration storage.
 *
 * Provides accessors for the configuration directory and file, plus helpers
 * to create the directory and check for the presence of the config file.
 */
object ConfigManager {
    /** Directory under the user's home where Filame stores configuration (for example `~/.config/filame`). */
    private val configDir = File(System.getProperty("user.home"), ".config/filame")

    /** Reference to the configuration file within `configDir`. */
    val configFile = File(configDir, "config.yaml")

    /**
     * Ensure the configuration directory exists on disk.
     *
     * Creates the directory and any missing parent directories when necessary.
     * This is a no-op if the directory already exists.
     */
    fun ensureConfigDir() {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }
}
