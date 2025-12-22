package xyz.malefic.filame.config

import com.charleskorn.kaml.Yaml
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
 * All functions return [Result] types to allow the UI layer to handle errors appropriately.
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
     * @return A [Result] containing the loaded or default configuration, or a failure
     *         with the exception if deserialization fails.
     */
    fun loadOrCreateConfig(): Result<FilameConfig> =
        if (configFile.exists()) {
            try {
                val yaml = configFile.readText()
                val config = Yaml.default.decodeFromString(FilameConfig.serializer(), yaml)
                Result.success(config)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.success(FilameConfig())
        }

    /**
     * Save configuration to file.
     * 
     * Serializes the configuration to YAML format and writes it to the config file.
     *
     * @param config The configuration to save.
     * @return A [Result] indicating success or failure with the exception.
     */
    fun saveConfig(config: FilameConfig): Result<Unit> =
        try {
            val yaml = Yaml.default.encodeToString(FilameConfig.serializer(), config)
            configFile.writeText(yaml)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

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
    ): FilameConfig =
        config.copy(
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
     * @return A [Result] containing the updated configuration with discovered packages,
     *         or a failure if the repository is not configured or scanning fails.
     */
    fun scanRepoForPackages(config: FilameConfig): Result<FilameConfig> {
        if (config.githubRepo.isEmpty()) {
            return Result.failure(IllegalStateException("GitHub repository not configured"))
        }

        val prep = config.prepareGitRepo()
        if (prep.isFailure) {
            return Result.failure(prep.exceptionOrNull() ?: Exception("Failed to prepare git repo"))
        }

        val (_, git) = prep.getOrThrow()

        val packageManager = PackageManager(config)
        val scanResult = packageManager.scanRepoForPackages()

        git.close()

        return if (scanResult.isSuccess) {
            val bundles = scanResult.getOrNull() ?: emptyList()
            Result.success(config.copy(packageBundles = bundles))
        } else {
            Result.failure(scanResult.exceptionOrNull() ?: Exception("Failed to scan repository"))
        }
    }
}
