package xyz.malefic.filame.ui

import arrow.core.getOrElse
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Session
import xyz.malefic.filame.config.ConfigManager
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.git.GitError

/**
 * Load existing configuration from disk or create a new default one.
 *
 * Delegates to [ConfigManager.loadOrCreateConfig] and displays error messages
 * if loading fails. Returns a default configuration as fallback.
 *
 * @receiver The Kotter session for UI rendering.
 * @return The loaded or default configuration.
 */
fun Session.loadOrCreateConfig(): FilameConfig {
    val result = ConfigManager.loadOrCreateConfig()
    return result.getOrElse { err ->
        showError("Error loading configuration: $err")
        FilameConfig()
    }
}

/**
 * Save configuration to disk and display the result.
 *
 * Delegates to [ConfigManager.saveConfig] and shows a success or error message
 * based on the result.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The configuration to save.
 */
fun Session.saveConfig(config: FilameConfig) {
    ConfigManager.saveConfig(config).fold(
        ifLeft = { err -> showError("Error saving configuration: $err") },
        ifRight = { showSuccess("Configuration saved successfully!") },
    )
}

/**
 * Interactively configure device and repository settings.
 *
 * Prompts the user for device name and GitHub repository URL, updates the
 * configuration with [ConfigManager.updateSettings], and saves it to disk.
 *
 * @receiver The Kotter session for UI rendering and user input.
 * @param config The current configuration.
 * @return The updated configuration.
 */
fun Session.configureSettings(config: FilameConfig): FilameConfig {
    displayHeader("═══ Configure Settings ═══")

    val deviceName = readInput("Enter device name (current: ${config.deviceName}): ").ifEmpty { config.deviceName }

    val githubRepo = readInput("Enter GitHub repository URL (current: ${config.githubRepo}): ").ifEmpty { config.githubRepo }

    val newConfig = ConfigManager.updateSettings(config, deviceName, githubRepo)

    saveConfig(newConfig)

    return newConfig
}

/**
 * Scan the GitHub repository for package bundles and update configuration.
 *
 * Displays progress, delegates to [ConfigManager.scanRepoForPackages] for the
 * actual scanning, and renders the discovered packages or error messages.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The current configuration.
 * @return The updated configuration with discovered packages, or the original if scanning failed.
 */
fun Session.scanRepoForPackages(config: FilameConfig): FilameConfig {
    displayHeader("═══ Scan Repository for Packages ═══")

    showInfo("Scanning repository for package bundles...")

    val result = ConfigManager.scanRepoForPackages(config)

    return result.fold(
        ifLeft = { err ->
            showError(
                when (err) {
                    GitError.RepoNotConfigured -> "GitHub repository not configured. Please configure first."
                    is GitError.IoError -> "I/O error preparing repository: ${err.message}"
                    is GitError.GitApi -> "Git error preparing repository: ${err.message}"
                    else -> "Error scanning repository: $err"
                },
            )
            config
        },
        ifRight = { newConfig ->
            saveConfig(newConfig)

            section {
                green { textLine("✓ Found ${newConfig.packageBundles.size} package bundle(s)") }
                newConfig.packageBundles.forEach { bundle ->
                    text("  • ")
                    cyan { text(bundle.name) }
                    text(" (${bundle.source})")
                    if (bundle.configFiles.isNotEmpty()) {
                        text(" - ${bundle.configFiles.size} config file(s)")
                    }
                    textLine()
                }
            }.run()
            newConfig
        },
    )
}
