package xyz.malefic

import com.charleskorn.kaml.Yaml
import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.text.*
import java.util.Scanner

/**
 * Filame - File manager for Arch Linux configurations
 * Manages configuration files across multiple devices with GitHub integration
 */
fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "hello") {
        println("Hello World!")
        return
    }

    ConfigManager.ensureConfigDir()
    val config = loadOrCreateConfig()

    showMainMenu(config)
}

private val scanner = Scanner(System.`in`)

/**
 * Load existing config or create a new one
 */
fun loadOrCreateConfig(): FilameConfig {
    val configFile = ConfigManager.getConfigFile()

    return if (configFile.exists()) {
        try {
            val yaml = configFile.readText()
            Yaml.default.decodeFromString(FilameConfig.serializer(), yaml)
        } catch (e: Exception) {
            println("Error loading config: ${e.message}")
            FilameConfig()
        }
    } else {
        FilameConfig()
    }
}

/**
 * Save configuration to file
 */
fun saveConfig(config: FilameConfig) {
    try {
        val yaml = Yaml.default.encodeToString(FilameConfig.serializer(), config)
        ConfigManager.getConfigFile().writeText(yaml)
        println("Configuration saved successfully!")
    } catch (e: Exception) {
        println("Error saving config: ${e.message}")
    }
}

/**
 * Main menu for the application
 */
fun showMainMenu(initialConfig: FilameConfig) {
    var config = initialConfig
    var running = true

    while (running) {
        session {
            section {
                cyan { textLine("╔════════════════════════════════════════╗") }
                cyan {
                    text("║  ")
                    white { text("FILAME - Arch Config Manager") }
                    cyan { textLine("      ║") }
                }
                cyan { textLine("╚════════════════════════════════════════╝") }
                textLine()

                if (config.deviceName.isEmpty()) {
                    yellow { textLine("⚠ Configuration not set up. Please configure first.") }
                    textLine()
                }

                textLine("Current device: ${config.deviceName.ifEmpty { "Not set" }}")
                textLine("GitHub repo: ${config.githubRepo.ifEmpty { "Not set" }}")
                textLine("Config files: ${config.configFiles.size}")
                textLine()

                green { textLine("1. Configure settings") }
                green { textLine("2. Add configuration file") }
                green { textLine("3. List configuration files") }
                green { textLine("4. Export configs to repo") }
                green { textLine("5. Import configs from repo") }
                green { textLine("6. Sync with GitHub (pull)") }
                green { textLine("7. Sync with GitHub (push)") }
                green { textLine("8. Manage ignore patterns") }
                cyan { textLine("9. Exit") }
                textLine()

                text("Select an option: ")
            }.run()
        }

        val choice = scanner.nextLine()

        when (choice) {
            "1" -> config = configureSettings(config)
            "2" -> config = addConfigFile(config)
            "3" -> listConfigFiles(config)
            "4" -> exportConfigs(config)
            "5" -> importConfigs(config)
            "6" -> syncPull(config)
            "7" -> syncPush(config)
            "8" -> config = manageIgnorePatterns(config)
            "9" -> {
                session {
                    section {
                        green { textLine("Thanks for using Filame! Goodbye!") }
                    }.run()
                }
                running = false
            }
            else -> {
                session {
                    section {
                        red { textLine("Invalid option. Please try again.") }
                    }.run()
                }
            }
        }

        if (running) {
            println("\nPress Enter to continue...")
            scanner.nextLine()
        }
    }
}

/**
 * Configure basic settings
 */
fun configureSettings(config: FilameConfig): FilameConfig {
    session {
        section {
            cyan { textLine("═══ Configure Settings ═══") }
            textLine()
        }.run()
    }

    print("Enter device name (current: ${config.deviceName}): ")
    val deviceName = scanner.nextLine().ifEmpty { config.deviceName }

    print("Enter GitHub repository URL (current: ${config.githubRepo}): ")
    val githubRepo = scanner.nextLine().ifEmpty { config.githubRepo }

    val newConfig =
        config.copy(
            deviceName = deviceName,
            githubRepo = githubRepo,
        )

    saveConfig(newConfig)

    session {
        section {
            green { textLine("✓ Settings saved successfully!") }
        }.run()
    }

    return newConfig
}

/**
 * Add a new configuration file to track
 */
fun addConfigFile(config: FilameConfig): FilameConfig {
    session {
        section {
            cyan { textLine("═══ Add Configuration File ═══") }
            textLine()
        }.run()
    }

    print("Enter source path (e.g., ~/.config/i3/config): ")
    val sourcePath = scanner.nextLine()

    if (sourcePath.isEmpty()) {
        session {
            section {
                red { textLine("Source path cannot be empty.") }
            }.run()
        }
        return config
    }

    print("Enter destination path in repo (e.g., i3/config): ")
    val destPath = scanner.nextLine()

    if (destPath.isEmpty()) {
        session {
            section {
                red { textLine("Destination path cannot be empty.") }
            }.run()
        }
        return config
    }

    print("Enter description (optional): ")
    val description = scanner.nextLine()

    val expandedPath = sourcePath.replace("~", System.getProperty("user.home"))
    val configFile = ConfigFile(expandedPath, destPath, description)
    val newConfig = config.copy(configFiles = config.configFiles + configFile)

    saveConfig(newConfig)

    session {
        section {
            green { textLine("✓ Configuration file added successfully!") }
        }.run()
    }

    return newConfig
}

/**
 * List all tracked configuration files
 */
fun listConfigFiles(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Configuration Files ═══") }
            textLine()

            if (config.configFiles.isEmpty()) {
                yellow { textLine("No configuration files tracked yet.") }
            } else {
                config.configFiles.forEachIndexed { index, file ->
                    white { text("${index + 1}. ") }
                    green { text(file.sourcePath) }
                    text(" → ")
                    cyan { textLine(file.destinationPath) }
                    if (file.description.isNotEmpty()) {
                        text("   ")
                        textLine(file.description)
                    }
                    textLine()
                }
            }
        }.run()
    }
}

/**
 * Export configuration files to repository
 */
fun exportConfigs(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Export Configurations ═══") }
            textLine()
        }.run()
    }

    if (config.githubRepo.isEmpty()) {
        session {
            section {
                red { textLine("GitHub repository not configured. Please configure first.") }
            }.run()
        }
        return
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        session {
            section {
                red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
            }.run()
        }
        return
    }

    println("Exporting configuration files...")

    val exportResult = gitManager.exportConfigs()

    if (exportResult.isSuccess) {
        val files = exportResult.getOrNull() ?: emptyList()
        session {
            section {
                green { textLine("✓ Exported ${files.size} configuration file(s)") }
                files.forEach { file ->
                    text("  • ")
                    textLine(file)
                }
            }.run()
        }
    } else {
        session {
            section {
                red { textLine("Error exporting configs: ${exportResult.exceptionOrNull()?.message}") }
            }.run()
        }
    }
}

/**
 * Import configuration files from repository
 */
fun importConfigs(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Import Configurations ═══") }
            textLine()
        }.run()
    }

    if (config.githubRepo.isEmpty()) {
        session {
            section {
                red { textLine("GitHub repository not configured. Please configure first.") }
            }.run()
        }
        return
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        session {
            section {
                red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
            }.run()
        }
        return
    }

    println("Importing configuration files...")

    val importResult = gitManager.importConfigs()

    if (importResult.isSuccess) {
        val files = importResult.getOrNull() ?: emptyList()
        session {
            section {
                green { textLine("✓ Imported ${files.size} configuration file(s)") }
                files.forEach { file ->
                    text("  • ")
                    textLine(file)
                }
            }.run()
        }
    } else {
        session {
            section {
                red { textLine("Error importing configs: ${importResult.exceptionOrNull()?.message}") }
            }.run()
        }
    }
}

/**
 * Pull changes from GitHub
 */
fun syncPull(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Sync from GitHub (Pull) ═══") }
            textLine()
        }.run()
    }

    if (config.githubRepo.isEmpty()) {
        session {
            section {
                red { textLine("GitHub repository not configured. Please configure first.") }
            }.run()
        }
        return
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        session {
            section {
                red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
            }.run()
        }
        return
    }

    val git = gitResult.getOrNull()!!

    println("Pulling latest changes from GitHub...")

    val pullResult = gitManager.pull(git)

    if (pullResult.isSuccess) {
        session {
            section {
                green { textLine("✓ Successfully pulled changes from GitHub") }
            }.run()
        }
    } else {
        session {
            section {
                red { textLine("Error pulling from GitHub: ${pullResult.exceptionOrNull()?.message}") }
            }.run()
        }
    }

    git.close()
}

/**
 * Push changes to GitHub
 */
fun syncPush(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Sync to GitHub (Push) ═══") }
            textLine()
        }.run()
    }

    if (config.githubRepo.isEmpty()) {
        session {
            section {
                red { textLine("GitHub repository not configured. Please configure first.") }
            }.run()
        }
        return
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        session {
            section {
                red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
            }.run()
        }
        return
    }

    val git = gitResult.getOrNull()!!

    print("Enter commit message: ")
    val message = scanner.nextLine().ifEmpty { "Update configs from ${config.deviceName}" }

    println("Committing changes...")

    val commitResult = gitManager.commit(git, message)

    if (commitResult.isFailure) {
        session {
            section {
                red { textLine("Error committing: ${commitResult.exceptionOrNull()?.message}") }
            }.run()
        }
        git.close()
        return
    }

    println("Pushing to GitHub...")

    session {
        section {
            yellow { textLine("Note: You may need to configure Git credentials for push") }
        }.run()
    }

    val pushResult = gitManager.push(git)

    if (pushResult.isSuccess) {
        session {
            section {
                green { textLine("✓ Successfully pushed changes to GitHub") }
            }.run()
        }
    } else {
        session {
            section {
                red { textLine("Error pushing to GitHub: ${pushResult.exceptionOrNull()?.message}") }
                yellow { textLine("Make sure you have configured Git credentials (SSH key or token)") }
            }.run()
        }
    }

    git.close()
}

/**
 * Manage ignore patterns
 */
fun manageIgnorePatterns(config: FilameConfig): FilameConfig {
    var currentConfig = config
    var managing = true

    while (managing) {
        session {
            section {
                cyan { textLine("═══ Manage Ignore Patterns ═══") }
                textLine()

                if (currentConfig.ignorePatterns.isEmpty()) {
                    yellow { textLine("No ignore patterns configured.") }
                } else {
                    textLine("Current ignore patterns:")
                    currentConfig.ignorePatterns.forEachIndexed { index, pattern ->
                        white { text("${index + 1}. ") }
                        textLine(pattern)
                    }
                }

                textLine()
                green { textLine("1. Add pattern") }
                green { textLine("2. Remove pattern") }
                cyan { textLine("3. Back to main menu") }
                textLine()
                text("Select an option: ")
            }.run()
        }

        when (scanner.nextLine()) {
            "1" -> {
                print("Enter ignore pattern (e.g., *.log): ")
                val pattern = scanner.nextLine()
                if (pattern.isNotEmpty()) {
                    currentConfig =
                        currentConfig.copy(
                            ignorePatterns = currentConfig.ignorePatterns + pattern,
                        )
                    saveConfig(currentConfig)
                    session {
                        section {
                            green { textLine("✓ Pattern added") }
                        }.run()
                    }
                }
            }
            "2" -> {
                if (currentConfig.ignorePatterns.isNotEmpty()) {
                    print("Enter pattern number to remove: ")
                    val index = scanner.nextLine().toIntOrNull()?.minus(1)
                    if (index != null && index in currentConfig.ignorePatterns.indices) {
                        currentConfig =
                            currentConfig.copy(
                                ignorePatterns = currentConfig.ignorePatterns.filterIndexed { i, _ -> i != index },
                            )
                        saveConfig(currentConfig)
                        session {
                            section {
                                green { textLine("✓ Pattern removed") }
                            }.run()
                        }
                    }
                }
            }
            "3" -> managing = false
        }

        if (managing) {
            println("\nPress Enter to continue...")
            scanner.nextLine()
        }
    }

    return currentConfig
}
