package xyz.malefic.filame.config

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
}
