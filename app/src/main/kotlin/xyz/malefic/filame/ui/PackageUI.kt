/**
 * Package management user interface components for Filame.
 * 
 * This file contains UI-only functions for package operations including listing,
 * adding/editing bundles, installing packages, and exporting configurations.
 * All business logic is delegated to [xyz.malefic.filame.pkg.PackageManager].
 */
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
import xyz.malefic.filame.git.GitError
import xyz.malefic.filame.git.GitException
import xyz.malefic.filame.pkg.AurHelper
import xyz.malefic.filame.pkg.PackageManager

/**
 * Render a UI listing all package bundles tracked in the provided [FilameConfig].
 *
 * This function is a receiver on [Session] and uses the kotter runtime to display
 * a formatted section with package bundle information:
 *  - Queries package installation statuses via [PackageManager.getPackageStatuses].
 *  - Displays a header and either a message when no bundles are tracked, or a
 *    numbered list of bundles when present.
 *  - For each bundle it shows an installed indicator ([✓] installed, [✗] not installed),
 *    the bundle name, source (e.g. `official` or `aur`), optional description, and
 *    any configuration files including their destination paths.
 *
 * @param config current [FilameConfig] containing tracked `packageBundles`.
 */
fun Session.listPackageBundles(config: FilameConfig) {
    val packageManager = PackageManager(config)
    val statuses = packageManager.getPackageStatuses()

    section {
        cyan { textLine("═══ Package Bundles ═══") }
        textLine()

        if (config.packageBundles.isEmpty()) {
            yellow { textLine("No pkg bundles tracked yet.") }
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
 * Add or edit a package bundle interactively.
 *
 * Prompts the user for package details including name, source, description, and
 * configuration files. Validates input, updates the configuration, and optionally
 * exports the bundle metadata to the GitHub repository.
 *
 * @receiver The Kotter session for UI rendering and user input.
 * @param config The current application configuration.
 * @return The updated configuration, or the original if the operation was aborted.
 */
fun Session.addOrEditPackageBundle(config: FilameConfig): FilameConfig {
    displayHeader("═══ Add/Edit Package Bundle ═══")

    // Prompt for the package name (required)
    val name = readInput("Enter pkg name: ")

    if (name.isEmpty()) {
        showError("Package name cannot be empty.")
        return config
    }

    // Prompt for the source, defaulting to "official"
    val source = readInput("Enter source (official/aur) [official]: ", Completions("official", "aur")).ifEmpty { "official" }

    // Optional description
    val description = readInput("Enter description (optional): ")

    // Ask if the user wants to add configuration files
    val addConfigs = promptYesNo("Add configuration files? (y/n) [n]: ")

    // Collect configuration files interactively
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
                    // Expand tilde to the user's home directory for convenience
                    val expandedPath = sourcePath.replace("~", System.getProperty("user.home"))
                    configFiles.add(ConfigFile(expandedPath, destPath, fileDesc))
                }
            }
        }
    }

    // Build the new package bundle
    val bundle = PackageBundle(name, source, description, configFiles)

    // Check if bundle already exists and replace it, otherwise append
    val existingIndex = config.packageBundles.indexOfFirst { it.name == name }
    val newBundles =
        if (existingIndex >= 0) {
            config.packageBundles.toMutableList().apply { set(existingIndex, bundle) }
        } else {
            config.packageBundles + bundle
        }

    // Create, persist and return the updated config
    val newConfig = config.copy(packageBundles = newBundles)
    saveConfig(newConfig)

    // If a GitHub repo is configured, attempt to export the package metadata and push
    if (newConfig.githubRepo.isNotEmpty()) {
        val packageManager = PackageManager(newConfig)
        val commitMessage = if (existingIndex >= 0) "Update package ${bundle.name}" else "Add package ${bundle.name}"

        val exportResult = packageManager.exportBundleAndPushWithMetadata(bundle, commitMessage)

        if (exportResult.isSuccess) {
            section {
                green {
                    textLine("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!")
                    textLine("✓ Package metadata exported to repo: ${exportResult.getOrNull()}")
                }
            }.run()
        } else {
            // Handle various Git-related errors and surface friendly messages to the user
            val ex = exportResult.exceptionOrNull() as? GitException
            when (val err2 = ex?.error) {
                is GitError.PushFailed -> {
                    showSuccess("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!")
                    showWarning("⚠ Could not push metadata to repo: ${err2.message}")
                }

                is GitError.SaveCredentialsFailed -> {
                    showSuccess("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!")
                    showWarning("⚠ Metadata exported, but saving credentials failed: ${err2.message}")
                }

                else -> {
                    showSuccess("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!")
                    showWarning("⚠ Could not export metadata to repo: ${exportResult.exceptionOrNull()?.message}")
                }
            }
        }
    } else {
        // No Git configured; just show success
        showSuccess("✓ Package bundle ${if (existingIndex >= 0) "updated" else "added"} successfully!")
    }

    return newConfig
}

/**
 * Install a selected package and apply its configuration files.
 * 
 * Displays available packages, prompts the user to select one, installs it
 * (including AUR helper if needed), and applies configuration files from the repository.
 *
 * @receiver The Kotter session for UI rendering and user input.
 * @param config The current application configuration.
 */
fun Session.installPackageWithConfig(config: FilameConfig) {
    displayHeader("═══ Install Package & Apply Config ═══")

    if (config.packageBundles.isEmpty()) {
        showWarning("No package bundles tracked yet.")
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
        showError("Invalid package number.")
        return
    }

    val bundle = config.packageBundles[index]
    val packageManager = PackageManager(config)

    // Check if an AUR helper is needed
    if (bundle.source == "aur" && !packageManager.isAurHelperInstalled()) {
        if (installAURHelper(packageManager)) return
    }

    // Install the pkg
    showInfo("Installing package ${bundle.name}...")
    val installResult = packageManager.installPackage(bundle)

    if (installResult.isFailure) {
        showError("Error installing package: ${installResult.exceptionOrNull()?.message}")
        return
    }

    // Apply configuration
    if (bundle.configFiles.isNotEmpty()) {
        showInfo("Applying configuration files for ${bundle.name}...")
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
            showWarning("⚠ Package installed but configuration failed: ${applyResult.exceptionOrNull()?.message}")
        }
    } else {
        showSuccess("✓ Package '${bundle.name}' installed successfully!")
    }
}

/**
 * Install all missing packages tracked in the configuration.
 * 
 * Checks which tracked packages are not installed and installs them all,
 * automatically handling AUR helper installation if required for AUR packages.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The current application configuration.
 */
fun Session.installAllMissingPackages(config: FilameConfig) {
    displayHeader("═══ Install All Missing Packages ═══")

    val packageManager = PackageManager(config)

    val needsAurHelper = config.packageBundles.any { it.source == "aur" }
    if (needsAurHelper && !packageManager.isAurHelperInstalled()) {
        if (installAURHelper(packageManager)) return
    }

    showInfo("Installing missing packages...")

    val result = packageManager.installMissingPackages()
    if (result.isSuccess) {
        val installed = result.getOrNull() ?: emptyList()
        section {
            if (installed.isEmpty()) {
                green { textLine("✓ All tracked packages are already installed") }
            } else {
                green { textLine("✓ Installed ${installed.size} pkg(s):") }
                installed.forEach { name ->
                    text("  • ")
                    textLine(name)
                }
            }
        }.run()
    } else {
        showError("Error installing packages: ${result.exceptionOrNull()?.message}")
    }
}

/**
 * Update all system packages (official and AUR).
 * 
 * Delegates to the package manager to update all installed packages system-wide.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The current application configuration.
 */
fun Session.updateAllPackages(config: FilameConfig) {
    displayHeader("═══ Update All Packages ═══", "This will update all system packages (official + AUR)")

    val packageManager = PackageManager(config)
    showInfo("Updating all packages... This may take a while.")

    val result = packageManager.updatePackages()
    showConditional(
        result.isSuccess,
        "✓ All packages updated successfully!",
        "Error updating packages: ${result.exceptionOrNull()?.message}",
    )
}

/**
 * Export all package configurations to the GitHub repository.
 * 
 * Exports configuration files and metadata for all tracked packages and pushes
 * them to the configured GitHub repository.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The current application configuration.
 */
fun Session.exportPackageConfigs(config: FilameConfig) {
    section {
        cyan { textLine("═══ Export Package Configurations ═══") }
        textLine()
    }.run()

    if (config.githubRepo.isEmpty()) {
        showError("GitHub repository not configured. Please configure first.")
        return
    }

    val packageManager = PackageManager(config)
    val result = packageManager.exportAllAndPush("Export package configurations")

    if (result.isSuccess) {
        val (totalExported, metadataExported) = result.getOrNull() ?: (0 to 0)
        showSuccess("✓ Exported $totalExported configuration file(s)")
        showSuccess("✓ Exported metadata for $metadataExported pkg bundle(s)")
    } else {
        showError(
            when (val err = (result.exceptionOrNull() as? GitException)?.error) {
                GitError.RepoNotConfigured -> "GitHub repository not configured. Please configure first."
                is GitError.IoError -> "I/O error exporting package configurations: ${err.message}"
                is GitError.GitApi -> "Git error exporting package configurations: ${err.message}"
                else -> "Error exporting package configurations: ${result.exceptionOrNull()?.message}"
            },
        )
    }
}

/**
 * Prompt user to select and install an AUR helper.
 * 
 * Displays a warning, prompts the user to choose between yay and paru,
 * and attempts to install the selected helper.
 *
 * @receiver The Kotter session for UI rendering and user input.
 * @param packageManager The package manager instance to use for installation.
 * @return `true` if the operation was aborted or failed, `false` if installation succeeded.
 */
private fun Session.installAURHelper(packageManager: PackageManager): Boolean {
    showWarning("An AUR helper is required for AUR packages but not installed.")

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

    if (promptYesNo("Install ${aurHelper.command} now? (y/n): ")) {
        showInfo("Installing ${aurHelper.command} now...")
        val installResult = packageManager.installAurHelper(aurHelper)
        if (installResult.isFailure) {
            showError("Failed to install ${aurHelper.command}: ${installResult.exceptionOrNull()?.message}")
            return true
        }
    } else {
        return true
    }
    return false
}
