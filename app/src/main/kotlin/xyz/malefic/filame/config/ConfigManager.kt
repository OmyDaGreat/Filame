package xyz.malefic.filame.config

import arrow.core.Either
import arrow.core.raise.either
import com.charleskorn.kaml.Yaml
import xyz.malefic.filame.git.GitError
import xyz.malefic.filame.git.prepareGitRepo
import xyz.malefic.filame.pkg.PackageManager
import java.io.File

/**
 * Manager responsible for handling Filame's configuration storage and operations.
 *
 * This object provides all non-UI configuration management functionality including:
 * - Loading and saving configuration files
 * - Updating configuration settings
 * - Scanning Git repositories for package bundles
 *
 * All functions return [Either] types to allow the UI layer to handle errors appropriately.
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

    /**
     * Load existing configuration from file or create a new default configuration.
     *
     * Reads and deserializes the YAML configuration file if it exists, otherwise
     * returns a new default [FilameConfig] instance.
     *
     * @return An [Either] containing the loaded or default configuration on the right, or a failure
     *         message on the left if deserialization fails.
     */
    fun loadOrCreateConfig(): Either<String, FilameConfig> =
        either<Throwable, FilameConfig> {
            if (configFile.exists()) {
                val yaml = configFile.readText()
                Yaml.default.decodeFromString(FilameConfig.serializer(), yaml)
            } else {
                FilameConfig()
            }
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Save configuration to file.
     *
     * Serializes the configuration to YAML format and writes it to the config file.
     *
     * @param config The configuration to save.
     * @return An [Either] indicating success or failure with error message.
     */
    fun saveConfig(config: FilameConfig): Either<String, Unit> =
        either<Throwable, Unit> {
            val yaml = Yaml.default.encodeToString(FilameConfig.serializer(), config)
            configFile.writeText(yaml)
        }.mapLeft { it.message ?: "Unknown error" }

    /**
     * Update device and repository settings in the configuration.
     *
     * Creates a new configuration instance with updated device name and GitHub repository URL.
     *
     * @param config The current configuration.
     * @param deviceName The new device name.
     * @param githubRepo The new GitHub repository URL.
     * @return A new [FilameConfig] with the updated settings.
     */
    fun updateSettings(
        config: FilameConfig,
        deviceName: String,
        githubRepo: String,
    ) = config.copy(
        deviceName = deviceName,
        githubRepo = githubRepo,
    )

    /**
     * Scan the Git repository for package bundles.
     *
     * Clones or opens the configured GitHub repository, scans it for package bundles,
     * and returns an updated configuration with the discovered packages. The Git
     * repository is automatically closed after the operation.
     *
     * @param config The current configuration containing the GitHub repository URL.
     * @return An [Either] containing the updated configuration with discovered packages on the right,
     *         or a [GitError] on the left if the repository is not configured or scanning fails.
     */
    fun scanRepoForPackages(config: FilameConfig): Either<GitError, FilameConfig> =
        either {
            val (_, git) = config.prepareGitRepo().bind()

            val packageManager = PackageManager(config)
            val bundles =
                git.use { _ ->
                    packageManager.scanRepoForPackages().mapLeft { GitError.IoError(it) }.bind()
                }

            config.copy(packageBundles = bundles)
        }
}
