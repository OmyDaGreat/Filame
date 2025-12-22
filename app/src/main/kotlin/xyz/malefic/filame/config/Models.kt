package xyz.malefic.filame.config

import kotlinx.serialization.Serializable

/**
 * Represents the configuration for the Filame application.
 *
 * @property deviceName The name of the device where the configuration is applied. Defaults to an empty string.
 * @property githubRepo The GitHub repository associated with the configuration. Defaults to an empty string.
 * @property packageBundles A list of `PackageBundle` objects representing the packages and their configurations. Defaults to an empty list.
 * @property ignorePatterns A list of file patterns to be ignored during processing. Defaults to "log", "tmp", "cache", and "lock".
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
 * Represents a configuration file to be synced.
 *
 * @property sourcePath The source path of the configuration file.
 * @property destinationPath The destination path where the configuration file will be synced.
 * @property description An optional description of the configuration file. Defaults to an empty string.
 */
@Serializable
data class ConfigFile(
    val sourcePath: String,
    val destinationPath: String,
    val description: String = "",
)

/**
 * Represents a pkg bundle with its configuration files.
 *
 * @property name The name of the pkg bundle.
 * @property source The source of the pkg bundle. Defaults to "official".
 * @property description An optional description of the pkg bundle. Defaults to an empty string.
 * @property configFiles A list of `ConfigFile` objects associated with the pkg bundle. Defaults to an empty list.
 */
@Serializable
data class PackageBundle(
    val name: String,
    val source: String = "official",
    val description: String = "",
    val configFiles: List<ConfigFile> = emptyList(),
)
