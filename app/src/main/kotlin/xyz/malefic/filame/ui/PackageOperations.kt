package xyz.malefic.filame.ui

import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session
import xyz.malefic.filame.config.ConfigFile
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.config.PackageBundle
import xyz.malefic.filame.git.GitManager
import xyz.malefic.filame.`package`.AurHelper
import xyz.malefic.filame.`package`.PackageManager

/**
 * List all package bundles
 */
fun Session.listPackageBundles(config: FilameConfig) {
    val packageManager = PackageManager(config)
    val statuses = packageManager.getPackageStatuses()

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

/**
 * Add or edit a package bundle
 */
fun Session.addOrEditPackageBundle(config: FilameConfig): FilameConfig {
    displayHeader("═══ Add/Edit Package Bundle ═══")

    val name = readInput("Enter package name: ")

    if (name.isEmpty()) {
        section {
            red { textLine("Package name cannot be empty.") }
        }.run()
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
                section {
                    green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
                    cyan { textLine("✓ Package metadata exported to repo: ${exportResult.getOrNull()}") }
                }.run()
            } else {
                section {
                    green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
                    yellow { textLine("⚠ Could not export metadata to repo: ${exportResult.exceptionOrNull()?.message}") }
                }.run()
            }
        } else {
            section {
                green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
            }.run()
        }
    } else {
        section {
            green { textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!") }
        }.run()
    }

    return newConfig
}

/**
 * Install a package and apply its configuration
 */
fun Session.installPackageWithConfig(config: FilameConfig) {
    section {
        cyan { textLine("═══ Install Package & Apply Config ═══") }
        textLine()
    }.run()

    if (config.packageBundles.isEmpty()) {
        section {
            yellow { textLine("No package bundles tracked yet.") }
        }.run()
        return
    }

    // Show available packages
    section {
        config.packageBundles.forEachIndexed { index, bundle ->
            textLine("${index + 1}. ${bundle.name} (${bundle.source})")
        }
    }.run()

    val index = readInput("\nEnter package number to install: ").toIntOrNull()?.minus(1)

    if (index == null || index !in config.packageBundles.indices) {
        section {
            red { textLine("Invalid package number.") }
        }.run()
        return
    }

    val bundle = config.packageBundles[index]
    val packageManager = PackageManager(config)

    // Check if an AUR helper is needed
    if (bundle.source == "aur" && !packageManager.isAurHelperInstalled()) {
        if (installAURHelper(packageManager)) return
    }

    // Install the package
    section {
        textLine("Installing ${bundle.name}...")
    }.run()
    val installResult = packageManager.installPackage(bundle)

    if (installResult.isFailure) {
        section {
            red { textLine("Error installing package: ${installResult.exceptionOrNull()?.message}") }
        }.run()
        return
    }

    // Apply configuration
    if (bundle.configFiles.isNotEmpty()) {
        section {
            textLine("Applying configuration files...")
        }.run()
        val applyResult = packageManager.applyPackageConfig(bundle)

        if (applyResult.isSuccess) {
            val files = applyResult.getOrNull() ?: emptyList()
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
        } else {
            section {
                yellow { textLine("⚠ Package installed but configuration failed: ${applyResult.exceptionOrNull()?.message}") }
            }.run()
        }
    } else {
        section {
            green { textLine("✓ Package '${bundle.name}' installed successfully!") }
        }.run()
    }
}

/**
 * Install all missing packages
 */
fun Session.installAllMissingPackages(config: FilameConfig) {
    section {
        cyan { textLine("═══ Install All Missing Packages ═══") }
        textLine()
    }.run()

    val packageManager = PackageManager(config)

    // Check if an AUR helper is needed for any AUR packages
    val needsAurHelper = config.packageBundles.any { it.source == "aur" }
    if (needsAurHelper && !packageManager.isAurHelperInstalled()) {
        if (installAURHelper(packageManager)) return
    }

    section {
        textLine("Installing missing packages...")
    }.run()
    val result = packageManager.installMissingPackages()

    if (result.isSuccess) {
        val installed = result.getOrNull() ?: emptyList()
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
    } else {
        section {
            red { textLine("Error installing packages: ${result.exceptionOrNull()?.message}") }
        }.run()
    }
}

/**
 * Update all packages
 */
fun Session.updateAllPackages(config: FilameConfig) {
    section {
        cyan { textLine("═══ Update All Packages ═══") }
        yellow { textLine("This will update all system packages (official + AUR)") }
        textLine()
    }.run()

    val packageManager = PackageManager(config)
    section {
        textLine("Updating all packages... This may take a while.")
    }.run()

    val result = packageManager.updatePackages()

    if (result.isSuccess) {
        section {
            green { textLine("✓ All packages updated successfully!") }
        }.run()
    } else {
        section {
            red { textLine("Error updating packages: ${result.exceptionOrNull()?.message}") }
        }.run()
    }
}

/**
 * Export package configurations to repo
 */
fun Session.exportPackageConfigs(config: FilameConfig) {
    section {
        cyan { textLine("═══ Export Package Configurations ═══") }
        textLine()
    }.run()

    if (config.githubRepo.isEmpty()) {
        section {
            red { textLine("GitHub repository not configured. Please configure first.") }
        }.run()
        return
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        section {
            red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
        }.run()
        return
    }

    section {
        textLine("Exporting package configurations...")
    }.run()

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

    section {
        green { textLine("✓ Exported $totalExported configuration file(s)") }
        cyan { textLine("✓ Exported metadata for $metadataExported package bundle(s)") }
    }.run()
}

/**
 * Helper function to install an AUR helper
 */
private fun Session.installAURHelper(packageManager: PackageManager): Boolean {
    section {
        yellow { textLine("An AUR helper is required for AUR packages but not installed.") }
    }.run()

    val chosen =
        readInput(
            "Choose AUR helper to install [yay/paru] (press enter for default ${AurHelper.DEFAULT.command}): ",
            Completions("yay", "paru"),
        ).lowercase()
    val aurHelper =
        when (chosen) {
            "yay" -> AurHelper.YAY
            "paru" -> AurHelper.PARU
            else -> AurHelper.DEFAULT
        }

    if (readInput("Install ${aurHelper.command} now? (y/n): ", yesNoCompletions).lowercase() == "y") {
        section {
            textLine("Installing ${aurHelper.command}...")
        }.run()
        val installResult = packageManager.installAurHelper(aurHelper)
        if (installResult.isFailure) {
            section {
                red { textLine("Failed to install ${aurHelper.command}: ${installResult.exceptionOrNull()?.message}") }
            }.run()
            return true
        }
    } else {
        return true
    }
    return false
}
