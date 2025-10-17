package xyz.malefic

import com.charleskorn.kaml.Yaml
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
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
import java.util.Scanner

/**
 * Filame - File manager for Arch Linux configurations
 * Manages configuration files across multiple devices with GitHub integration
 */
fun main(vararg args: String) {
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
                textLine("Config files: ${config.configFiles.size}")
                textLine("Packages: ${config.packages.size}")
                textLine()

                arrayListOf(
                    "1. Configure settings",
                    "2. Add configuration file",
                    "3. List configuration files",
                    "4. Export configs to repo",
                    "5. Import configs from repo",
                    "6. Sync with GitHub (pull)",
                    "7. Sync with GitHub (push)",
                    "8. Manage ignore patterns",
                    "9. Manage packages",
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

                text("Select an option: ")
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

                        Keys.LEFT -> choice = if (choice == 1) 0 else if (choice == 0) 9 else choice - 1
                        Keys.DOWN -> choice = if (choice == 1) 0 else if (choice == 0) 9 else choice - 1
                        Keys.RIGHT -> choice = if (choice == 9) 0 else if (choice == 0) 1 else choice + 1
                        Keys.UP -> choice = if (choice == 9) 0 else if (choice == 0) 1 else choice + 1

                        Keys.ENTER -> {
                            when (choice) {
                                1 -> config = configureSettings(config)
                                2 -> config = addConfigFile(config)
                                3 -> listConfigFiles(config)
                                4 -> exportConfigs(config)
                                5 -> importConfigs(config)
                                6 -> syncPull(config)
                                7 -> syncPush(config)
                                8 -> config = manageIgnorePatterns(config)
                                9 -> config = managePackages(config)
                                0 -> {
                                    session {
                                        section {
                                            green { textLine("Thanks for using Filame! Goodbye!") }
                                        }.run()
                                    }
                                    running = false
                                }
                                else -> {
                                    section {
                                        red { textLine("Invalid option. Please try again.") }
                                    }.run()
                                }
                            }
                        }
                    }
                    rerender()
                    println("Choice is $choice")
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

/**
 * Manage packages
 */
fun managePackages(config: FilameConfig): FilameConfig {
    var currentConfig = config
    var managing = true
    val packageManager = PackageManager(currentConfig)

    while (managing) {
        session {
            section {
                cyan { textLine("═══ Manage Packages ═══") }
                textLine()

                // Show paru status
                if (packageManager.isParuInstalled()) {
                    green { textLine("✓ Paru is installed") }
                } else {
                    yellow { textLine("⚠ Paru is not installed (required for AUR packages)") }
                }
                textLine()

                // Show tracked packages
                if (currentConfig.packages.isEmpty()) {
                    yellow { textLine("No packages tracked yet.") }
                } else {
                    textLine("Tracked packages:")
                    val statuses = packageManager.getPackageStatuses()
                    currentConfig.packages.forEachIndexed { index, pkg ->
                        white { text("${index + 1}. ") }
                        if (statuses[pkg] == true) {
                            green { text("[✓] ") }
                        } else {
                            red { text("[✗] ") }
                        }
                        cyan { text(pkg.name) }
                        text(" (${pkg.source})")
                        if (pkg.description.isNotEmpty()) {
                            text(" - ${pkg.description}")
                        }
                        textLine()
                    }
                }

                textLine()
                green { textLine("1. Install paru (if not installed)") }
                green { textLine("2. Add package") }
                green { textLine("3. Search packages") }
                green { textLine("4. Install missing packages") }
                green { textLine("5. Update all packages") }
                green { textLine("6. Remove package from tracking") }
                green { textLine("7. Uninstall package from system") }
                cyan { textLine("8. Back to main menu") }
                textLine()
                text("Select an option: ")
            }.run()
        }

        when (scanner.nextLine()) {
            "1" -> {
                if (packageManager.isParuInstalled()) {
                    session {
                        section {
                            green { textLine("✓ Paru is already installed") }
                        }.run()
                    }
                } else {
                    session {
                        section {
                            yellow { textLine("Installing paru... This may take a few minutes.") }
                        }.run()
                    }
                    val result = packageManager.installParu()
                    if (result.isSuccess) {
                        session {
                            section {
                                green { textLine("✓ Paru installed successfully!") }
                            }.run()
                        }
                    } else {
                        session {
                            section {
                                red { textLine("Error installing paru: ${result.exceptionOrNull()?.message}") }
                            }.run()
                        }
                    }
                }
            }
            "2" -> {
                print("Enter package name: ")
                val name = scanner.nextLine()
                if (name.isEmpty()) {
                    session {
                        section {
                            red { textLine("Package name cannot be empty.") }
                        }.run()
                    }
                } else {
                    print("Enter source (official/aur) [official]: ")
                    val source = scanner.nextLine().ifEmpty { "official" }
                    print("Enter description (optional): ")
                    val description = scanner.nextLine()
                    
                    val pkg = Package(name, source, description)
                    currentConfig = currentConfig.copy(packages = currentConfig.packages + pkg)
                    saveConfig(currentConfig)
                    session {
                        section {
                            green { textLine("✓ Package added to tracking") }
                        }.run()
                    }
                }
            }
            "3" -> {
                print("Enter search query: ")
                val query = scanner.nextLine()
                if (query.isNotEmpty()) {
                    session {
                        section {
                            yellow { textLine("Searching for '$query'...") }
                        }.run()
                    }
                    val result = packageManager.searchPackages(query)
                    if (result.isSuccess) {
                        val results = result.getOrNull() ?: emptyList()
                        session {
                            section {
                                if (results.isEmpty()) {
                                    yellow { textLine("No packages found.") }
                                } else {
                                    cyan { textLine("Search results:") }
                                    textLine()
                                    results.take(20).forEach { pkg ->
                                        white { text("• ") }
                                        cyan { text(pkg.name) }
                                        text(" (${pkg.source})")
                                        if (pkg.description.isNotEmpty()) {
                                            textLine()
                                            text("  ${pkg.description}")
                                        }
                                        textLine()
                                    }
                                    if (results.size > 20) {
                                        yellow { textLine("\n... and ${results.size - 20} more results") }
                                    }
                                }
                            }.run()
                        }
                    } else {
                        session {
                            section {
                                red { textLine("Error searching packages: ${result.exceptionOrNull()?.message}") }
                            }.run()
                        }
                    }
                }
            }
            "4" -> {
                session {
                    section {
                        yellow { textLine("Installing missing packages...") }
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
            "5" -> {
                session {
                    section {
                        yellow { textLine("Updating all packages... This may take a while.") }
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
            "6" -> {
                if (currentConfig.packages.isNotEmpty()) {
                    print("Enter package number to remove from tracking: ")
                    val index = scanner.nextLine().toIntOrNull()?.minus(1)
                    if (index != null && index in currentConfig.packages.indices) {
                        val pkg = currentConfig.packages[index]
                        currentConfig = currentConfig.copy(
                            packages = currentConfig.packages.filterIndexed { i, _ -> i != index }
                        )
                        saveConfig(currentConfig)
                        session {
                            section {
                                green { textLine("✓ Package '${pkg.name}' removed from tracking") }
                            }.run()
                        }
                    }
                }
            }
            "7" -> {
                print("Enter package name to uninstall: ")
                val packageName = scanner.nextLine()
                if (packageName.isNotEmpty()) {
                    session {
                        section {
                            yellow { textLine("Uninstalling $packageName...") }
                        }.run()
                    }
                    val result = packageManager.removePackage(packageName)
                    if (result.isSuccess) {
                        session {
                            section {
                                green { textLine("✓ Package '$packageName' uninstalled successfully!") }
                            }.run()
                        }
                    } else {
                        session {
                            section {
                                red { textLine("Error uninstalling package: ${result.exceptionOrNull()?.message}") }
                            }.run()
                        }
                    }
                }
            }
            "8" -> managing = false
        }

        if (managing) {
            println("\nPress Enter to continue...")
            scanner.nextLine()
        }
    }

    return currentConfig
}
