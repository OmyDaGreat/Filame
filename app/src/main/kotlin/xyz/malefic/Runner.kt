package xyz.malefic

import com.charleskorn.kaml.Yaml
import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.multilineInput
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.foundation.text.yellow

/**
 * Filame - File manager for Arch Linux configurations
 * Manages configuration files across multiple devices with GitHub integration
 *
 * Session lifecycle: The entire application runs within a single top-level session block.
 * This establishes the terminal runtime once and maintains it throughout the app lifecycle.
 */
fun main(vararg args: String) {
    // Single top-level session for the entire application
    session {
        if (args.isNotEmpty() && args[0] == "hello") {
            section {
                textLine("Hello World!")
            }.run()
            return@session
        }

        ConfigManager.ensureConfigDir()
        val config = loadOrCreateConfig()

        showMainMenu(config)
    }
}

/**
 * Common completions for yes/no prompts
 */
private val yesNoCompletions: Completions = Completions("y", "n", "yes", "no")

/**
 * Display a colored header using Kotter
 *
 * Note: This function creates a nested session block. While this works, it's not the ideal pattern.
 * In a future refactor, this should be converted to an extension function on Session scope to
 * avoid the nested session overhead.
 */
private fun displayHeader(text: String) {
    session {
        section {
            cyan { textLine(text) }
            textLine()
        }.run()
    }
}

/**
 * Read a line of input using Kotter
 *
 * Note: This function creates a nested session block for its interactive input loop.
 * While functional, a future refactor could make this an extension function on Session scope.
 */
private fun readInput(prompt: String = "", completions: Completions? = null): String {
    var result = ""
    session {
        section {
            if (prompt.isNotEmpty()) {
                text(prompt)
            }
            if (completions != null) {
                input(completions)
            } else {
                input()
            }
        }.runUntilInputEntered {
            onInputEntered {
                result = input
            }
        }
    }
    return result
}

/**
 * Read multi-line input using Kotter
 *
 * Note: This function creates a nested session block for its interactive input loop.
 * While functional, a future refactor could make this an extension function on Session scope.
 */
private fun readMultiLineInput(prompt: String = ""): String {
    var result = ""
    session {
        section {
            if (prompt.isNotEmpty()) {
                textLine(prompt)
            }
            multilineInput()
        }.runUntilInputEntered {
            onInputEntered {
                result = input
            }
        }
    }
    return result
}

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
            session {
                section {
                    red { textLine("Error loading config: ${e.message}") }
                }.run()
            }
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
        session {
            section {
                green { textLine("Configuration saved successfully!") }
            }.run()
        }
    } catch (e: Exception) {
        session {
            section {
                red { textLine("Error saving config: ${e.message}") }
            }.run()
        }
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
            var exit by liveVarOf(false)
            var choice by liveVarOf(0)
            section {
                cyan { textLine("╔════════════════════════════════════════╗") }
                cyan {
                    text("║  ")
                    white { text("FILAME - Arch Config Manager") }
                    cyan { textLine("          ║") }
                }
                cyan { textLine("╚════════════════════════════════════════╝") }
                textLine()

                if (config.deviceName.isEmpty()) {
                    yellow { textLine("⚠ Configuration not set up. Please configure first.") }
                    textLine()
                }

                textLine("Current device: ${config.deviceName.ifEmpty { "Not set" }}")
                textLine("GitHub repo: ${config.githubRepo.ifEmpty { "Not set" }}")
                textLine("Package bundles: ${config.packageBundles.size}")
                textLine()

                textLine("Select an option: ")

                arrayListOf(
                    "1. Configure settings",
                    "2. Scan repo for packages",
                    "3. List package bundles",
                    "4. Add/Edit package bundle",
                    "5. Install package & apply config",
                    "6. Install all missing packages",
                    "7. Update all packages",
                    "8. Export package configs to repo",
                    "9. Sync with GitHub",
                    "0. Exit",
                ).forEachIndexed { i, line ->
                    val index = if (i == 9) 0 else i + 1
                    if (choice == index) {
                        cyan { textLine(line) }
                    } else {
                        green { textLine(line) }
                    }
                }
                textLine()

                if (exit) {
                    green { textLine("Thanks for using Filame! Goodbye!") }
                    running = false
                }
            }.runUntilKeyPressed(
                Keys.DIGIT_1,
                Keys.DIGIT_2,
                Keys.DIGIT_3,
                Keys.DIGIT_4,
                Keys.DIGIT_5,
                Keys.DIGIT_6,
                Keys.DIGIT_7,
                Keys.DIGIT_8,
                Keys.DIGIT_9,
                Keys.DIGIT_0,
            ) {
                onKeyPressed {
                    when (key) {
                        Keys.DIGIT_1 -> choice = 1
                        Keys.DIGIT_2 -> choice = 2
                        Keys.DIGIT_3 -> choice = 3
                        Keys.DIGIT_4 -> choice = 4
                        Keys.DIGIT_5 -> choice = 5
                        Keys.DIGIT_6 -> choice = 6
                        Keys.DIGIT_7 -> choice = 7
                        Keys.DIGIT_8 -> choice = 8
                        Keys.DIGIT_9 -> choice = 9
                        Keys.DIGIT_0 -> choice = 0

                        Keys.LEFT, Keys.UP ->
                            choice =
                                when (choice) {
                                    1 -> 0
                                    0 -> 9
                                    else -> choice - 1
                                }
                        Keys.RIGHT, Keys.DOWN ->
                            choice =
                                when (choice) {
                                    9 -> 0
                                    0 -> 1
                                    else -> choice + 1
                                }

                        Keys.ENTER -> {
                            when (choice) {
                                1 -> config = configureSettings(config)
                                2 -> config = scanRepoForPackages(config)
                                3 -> listPackageBundles(config)
                                4 -> config = addOrEditPackageBundle(config)
                                5 -> installPackageWithConfig(config)
                                6 -> installAllMissingPackages(config)
                                7 -> updateAllPackages(config)
                                8 -> exportPackageConfigs(config)
                                9 -> config = syncWithGitHub(config)
                                0 -> exit = true
                                else -> { /* no-op */ }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Configure basic settings
 */
fun configureSettings(config: FilameConfig): FilameConfig {
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
fun scanRepoForPackages(config: FilameConfig): FilameConfig {
    session {
        section {
            cyan { textLine("═══ Scan Repository for Packages ═══") }
            textLine()
        }.run()
    }

    if (config.githubRepo.isEmpty()) {
        session {
            section {
                red { textLine("GitHub repository not configured. Please configure first.") }
            }.run()
        }
        return config
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        session {
            section {
                red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
            }.run()
        }
        return config
    }

    session {
        section {
            textLine("Scanning repository for package bundles...")
        }.run()
    }

    val packageManager = PackageManager(config)
    val scanResult = packageManager.scanRepoForPackages()

    return if (scanResult.isSuccess) {
        val bundles = scanResult.getOrNull() ?: emptyList()
        val newConfig = config.copy(packageBundles = bundles)
        saveConfig(newConfig)

        session {
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
        }
        newConfig
    } else {
        session {
            section {
                red { textLine("Error scanning repository: ${scanResult.exceptionOrNull()?.message}") }
            }.run()
        }
        config
    }
}

/**
 * List all package bundles
 */
fun listPackageBundles(config: FilameConfig) {
    val packageManager = PackageManager(config)
    val statuses = packageManager.getPackageStatuses()

    session {
        section {
            cyan { textLine("═══ Package Bundles ═══") }
            textLine()

            if (config.packageBundles.isEmpty()) {
                yellow { textLine("No package bundles tracked yet.") }
                textLine()
                yellow { textLine("Tip: Use 'Scan repo for packages' to discover packages from your GitHub repo") }
            } else {
                config.packageBundles.forEachIndexed { index, bundle ->
                    white { text("${index + 1}. ") }

                    if (statuses[bundle] == true) {
                        green { text("[✓] ") }
                    } else {
                        red { text("[✗] ") }
                    }

                    cyan { text(bundle.name) }
                    text(" (${bundle.source})")
                    textLine()

                    if (bundle.description.isNotEmpty()) {
                        text("   ")
                        textLine(bundle.description)
                    }

                    if (bundle.configFiles.isNotEmpty()) {
                        text("   Config files: ")
                        textLine("${bundle.configFiles.size}")
                        bundle.configFiles.forEach { file ->
                            text("     • ")
                            textLine(file.destinationPath)
                        }
                    }
                    textLine()
                }
            }
        }.run()
    }
}

/**
 * Add or edit a package bundle
 */
fun addOrEditPackageBundle(config: FilameConfig): FilameConfig {
    displayHeader("═══ Add/Edit Package Bundle ═══")

    val name = readInput("Enter package name: ")

    if (name.isEmpty()) {
        session {
            section {
                red { textLine("Package name cannot be empty.") }
            }.run()
        }
        return config
    }

    val source = readInput("Enter source (official/aur) [official]: ", Completions("official", "aur")).ifEmpty { "official" }

    val description = readInput("Enter description (optional): ")

    // Ask if user wants to add config files
    val addConfigs = readInput("Add configuration files? (y/n) [n]: ", yesNoCompletions).lowercase() == "y"

    val configFiles = mutableListOf<ConfigFile>()
    if (addConfigs) {
        var adding = true
        while (adding) {
            val sourcePath = readInput("Enter source path (or press Enter to finish): ")
            if (sourcePath.isEmpty()) {
                adding = false
            } else {
                val destPath = readInput("Enter destination path in repo: ")
                if (destPath.isNotEmpty()) {
                    val fileDesc = readInput("Enter description (optional): ")
                    val expandedPath = sourcePath.replace("~", System.getProperty("user.home"))
                    configFiles.add(ConfigFile(expandedPath, destPath, fileDesc))
                }
            }
        }
    }

    val bundle = PackageBundle(name, source, description, configFiles)

    // Check if bundle already exists and replace it
    val existingIndex = config.packageBundles.indexOfFirst { it.name == name }
    val newBundles =
        if (existingIndex >= 0) {
            config.packageBundles.toMutableList().apply { set(existingIndex, bundle) }
        } else {
            config.packageBundles + bundle
        }

    val newConfig = config.copy(packageBundles = newBundles)
    saveConfig(newConfig)

    // Export package metadata to repo if GitHub repo is configured
    if (newConfig.githubRepo.isNotEmpty()) {
        val gitManager = GitManager(newConfig)
        val gitResult = gitManager.initializeRepo()

        if (gitResult.isSuccess) {
            val packageManager = PackageManager(newConfig)
            val exportResult = packageManager.exportPackageMetadata(bundle)

            if (exportResult.isSuccess) {
                session {
                    section {
                        green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
                        cyan { textLine("✓ Package metadata exported to repo: ${exportResult.getOrNull()}") }
                    }.run()
                }
            } else {
                session {
                    section {
                        green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
                        yellow { textLine("⚠ Could not export metadata to repo: ${exportResult.exceptionOrNull()?.message}") }
                    }.run()
                }
            }
        } else {
            session {
                section {
                    green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
                }.run()
            }
        }
    } else {
        session {
            section {
                green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
            }.run()
        }
    }

    return newConfig
}

/**
 * Install a package and apply its configuration
 */
fun installPackageWithConfig(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Install Package & Apply Config ═══") }
            textLine()
        }.run()
    }

    if (config.packageBundles.isEmpty()) {
        session {
            section {
                yellow { textLine("No package bundles tracked yet.") }
            }.run()
        }
        return
    }

    // Show available packages
    session {
        section {
            config.packageBundles.forEachIndexed { index, bundle ->
                textLine("${index + 1}. ${bundle.name} (${bundle.source})")
            }
        }.run()
    }

    val index = readInput("\nEnter package number to install: ").toIntOrNull()?.minus(1)

    if (index == null || index !in config.packageBundles.indices) {
        session {
            section {
                red { textLine("Invalid package number.") }
            }.run()
        }
        return
    }

    val bundle = config.packageBundles[index]
    val packageManager = PackageManager(config)

    // Check if paru is needed
    if (bundle.source == "aur" && !packageManager.isParuInstalled()) {
        session {
            section {
                yellow { textLine("Paru is required for AUR packages but not installed.") }
            }.run()
        }

        if (readInput("Install paru now? (y/n): ", yesNoCompletions).lowercase() == "y") {
            session {
                section {
                    textLine("Installing paru...")
                }.run()
            }
            val paruResult = packageManager.installParu()
            if (paruResult.isFailure) {
                session {
                    section {
                        red { textLine("Failed to install paru: ${paruResult.exceptionOrNull()?.message}") }
                    }.run()
                }
                return
            }
        } else {
            return
        }
    }

    // Install the package
    session {
        section {
            textLine("Installing ${bundle.name}...")
        }.run()
    }
    val installResult = packageManager.installPackage(bundle)

    if (installResult.isFailure) {
        session {
            section {
                red { textLine("Error installing package: ${installResult.exceptionOrNull()?.message}") }
            }.run()
        }
        return
    }

    // Apply configuration
    if (bundle.configFiles.isNotEmpty()) {
        session {
            section {
                textLine("Applying configuration files...")
            }.run()
        }
        val applyResult = packageManager.applyPackageConfig(bundle)

        if (applyResult.isSuccess) {
            val files = applyResult.getOrNull() ?: emptyList()
            session {
                section {
                    green { textLine("✓ Package '${bundle.name}' installed and configured successfully!") }
                    if (files.isNotEmpty()) {
                        textLine("Applied ${files.size} config file(s):")
                        files.forEach { file ->
                            text("  • ")
                            textLine(file)
                        }
                    }
                }.run()
            }
        } else {
            session {
                section {
                    yellow { textLine("⚠ Package installed but configuration failed: ${applyResult.exceptionOrNull()?.message}") }
                }.run()
            }
        }
    } else {
        session {
            section {
                green { textLine("✓ Package '${bundle.name}' installed successfully!") }
            }.run()
        }
    }
}

/**
 * Install all missing packages
 */
fun installAllMissingPackages(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Install All Missing Packages ═══") }
            textLine()
        }.run()
    }

    val packageManager = PackageManager(config)

    // Check if paru is needed for any AUR packages
    val needsParu = config.packageBundles.any { it.source == "aur" }
    if (needsParu && !packageManager.isParuInstalled()) {
        session {
            section {
                yellow { textLine("Paru is required for AUR packages but not installed.") }
            }.run()
        }

        if (readInput("Install paru now? (y/n): ", yesNoCompletions).lowercase() == "y") {
            session {
                section {
                    textLine("Installing paru...")
                }.run()
            }
            val paruResult = packageManager.installParu()
            if (paruResult.isFailure) {
                session {
                    section {
                        red { textLine("Failed to install paru: ${paruResult.exceptionOrNull()?.message}") }
                    }.run()
                }
                return
            }
        } else {
            return
        }
    }

    session {
        section {
            textLine("Installing missing packages...")
        }.run()
    }
    val result = packageManager.installMissingPackages()

    if (result.isSuccess) {
        val installed = result.getOrNull() ?: emptyList()
        session {
            section {
                if (installed.isEmpty()) {
                    green { textLine("✓ All tracked packages are already installed") }
                } else {
                    green { textLine("✓ Installed ${installed.size} package(s):") }
                    installed.forEach { name ->
                        text("  • ")
                        textLine(name)
                    }
                }
            }.run()
        }
    } else {
        session {
            section {
                red { textLine("Error installing packages: ${result.exceptionOrNull()?.message}") }
            }.run()
        }
    }
}

/**
 * Update all packages
 */
fun updateAllPackages(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Update All Packages ═══") }
            yellow { textLine("This will update all system packages (official + AUR)") }
            textLine()
        }.run()
    }

    val packageManager = PackageManager(config)
    session {
        section {
            textLine("Updating all packages... This may take a while.")
        }.run()
    }

    val result = packageManager.updatePackages()

    if (result.isSuccess) {
        session {
            section {
                green { textLine("✓ All packages updated successfully!") }
            }.run()
        }
    } else {
        session {
            section {
                red { textLine("Error updating packages: ${result.exceptionOrNull()?.message}") }
            }.run()
        }
    }
}

/**
 * Export package configurations to repo
 */
fun exportPackageConfigs(config: FilameConfig) {
    session {
        section {
            cyan { textLine("═══ Export Package Configurations ═══") }
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

    session {
        section {
            textLine("Exporting package configurations...")
        }.run()
    }

    val packageManager = PackageManager(config)
    var totalExported = 0
    var metadataExported = 0

    for (bundle in config.packageBundles) {
        // Export config files
        val exportResult = packageManager.exportPackageConfig(bundle)
        if (exportResult.isSuccess) {
            totalExported += exportResult.getOrNull()?.size ?: 0
        }

        // Export package metadata
        val metadataResult = packageManager.exportPackageMetadata(bundle)
        if (metadataResult.isSuccess) {
            metadataExported++
        }
    }

    session {
        section {
            green { textLine("✓ Exported $totalExported configuration file(s)") }
            cyan { textLine("✓ Exported metadata for $metadataExported package bundle(s)") }
        }.run()
    }
}

/**
 * Sync with GitHub (pull and push)
 */
fun syncWithGitHub(config: FilameConfig): FilameConfig {
    var currentConfig = config
    var syncing = true

    while (syncing) {
        session {
            section {
                cyan { textLine("═══ Sync with GitHub ═══") }
                textLine()
                green { textLine("1. Pull changes from GitHub") }
                green { textLine("2. Push changes to GitHub") }
                cyan { textLine("3. Back to main menu") }
                textLine()
            }.run()
        }

        when (readInput("Select an option: ")) {
            "1" -> currentConfig = syncPull(currentConfig)
            "2" -> syncPush(currentConfig)
            "3" -> syncing = false
        }

        if (syncing) {
            readInput("\nPress Enter to continue...")
        }
    }

    return currentConfig
}

/**
 * Pull changes from GitHub
 */
fun syncPull(config: FilameConfig): FilameConfig {
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
        return config
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        session {
            section {
                red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
            }.run()
        }
        return config
    }

    val git = gitResult.getOrNull()!!

    session {
        section {
            textLine("Pulling latest changes from GitHub...")
        }.run()
    }

    val pullResult = gitManager.pull(git)

    if (pullResult.isSuccess) {
        session {
            section {
                green { textLine("✓ Successfully pulled changes from GitHub") }
                yellow { textLine("Tip: Use 'Scan repo for packages' to update your package list") }
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
    return config
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

    val message = readInput("Enter commit message: ").ifEmpty { "Update configs from ${config.deviceName}" }

    session {
        section {
            textLine("Committing changes...")
        }.run()
    }

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

    session {
        section {
            textLine("Pushing to GitHub...")
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
