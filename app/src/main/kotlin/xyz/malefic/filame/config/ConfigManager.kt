package xyz.malefic.filame.config

import com.charleskorn.kaml.Yaml
import xyz.malefic.filame.git.prepareGitRepo
import xyz.malefic.filame.pkg.PackageManager
import java.io.File

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

    /**
     * Load existing config or create a new one
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
     * Save configuration to file
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
     * Update device and repository settings
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
     * Scan repository for pkg bundles
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
