package xyz.malefic.filame.ui

import com.charleskorn.kaml.Yaml
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Session
import xyz.malefic.filame.config.ConfigManager.configFile
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.git.GitManager
import xyz.malefic.filame.`package`.PackageManager

/**
 * Load existing config or create a new one
 */
fun Session.loadOrCreateConfig(): FilameConfig =
    if (configFile.exists()) {
        try {
            val yaml = configFile.readText()
            Yaml.default.decodeFromString(FilameConfig.serializer(), yaml)
        } catch (e: Exception) {
            section {
                red { textLine("Error loading config: ${e.message}") }
            }.run()
            FilameConfig()
        }
    } else {
        FilameConfig()
    }

/**
 * Save configuration to file
 */
fun Session.saveConfig(config: FilameConfig) =
    try {
        val yaml = Yaml.default.encodeToString(FilameConfig.serializer(), config)
        configFile.writeText(yaml)
        section {
            green { textLine("Configuration saved successfully!") }
        }.run()
    } catch (e: Exception) {
        section {
            red { textLine("Error saving config: ${e.message}") }
        }.run()
    }

/**
 * Configure device and repository settings
 */
fun Session.configureSettings(config: FilameConfig): FilameConfig {
    displayHeader("═══ Configure Settings ═══")

    val deviceName = readInput("Enter device name (current: ${config.deviceName}): ").ifEmpty { config.deviceName }

    val githubRepo = readInput("Enter GitHub repository URL (current: ${config.githubRepo}): ").ifEmpty { config.githubRepo }

    val newConfig =
        config.copy(
            deviceName = deviceName,
            githubRepo = githubRepo,
        )

    saveConfig(newConfig)

    return newConfig
}

/**
 * Scan repository for package bundles
 */
fun Session.scanRepoForPackages(config: FilameConfig): FilameConfig {
    displayHeader("═══ Scan Repository for Packages ═══")

    if (config.githubRepo.isEmpty()) {
        section {
            red { textLine("GitHub repository not configured. Please configure first.") }
        }.run()
        return config
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        section {
            red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
        }.run()
        return config
    }

    section {
        textLine("Scanning repository for package bundles...")
    }.run()

    val packageManager = PackageManager(config)
    val scanResult = packageManager.scanRepoForPackages()

    return if (scanResult.isSuccess) {
        val bundles = scanResult.getOrNull() ?: emptyList()
        val newConfig = config.copy(packageBundles = bundles)
        saveConfig(newConfig)

        section {
            green { textLine("✓ Found ${bundles.size} package bundle(s)") }
            bundles.forEach { bundle ->
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
    } else {
        section {
            red { textLine("Error scanning repository: ${scanResult.exceptionOrNull()?.message}") }
        }.run()
        config
    }
}
