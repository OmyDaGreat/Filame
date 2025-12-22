package xyz.malefic.filame

import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.textLine
import xyz.malefic.filame.config.ConfigManager
import xyz.malefic.filame.ui.addOrEditPackageBundle
import xyz.malefic.filame.ui.configureSettings
import xyz.malefic.filame.ui.exportPackageConfigs
import xyz.malefic.filame.ui.installAllMissingPackages
import xyz.malefic.filame.ui.installPackageWithConfig
import xyz.malefic.filame.ui.listPackageBundles
import xyz.malefic.filame.ui.loadOrCreateConfig
import xyz.malefic.filame.ui.removePackageInteractive
import xyz.malefic.filame.ui.scanRepoForPackages
import xyz.malefic.filame.ui.searchForPackages
import xyz.malefic.filame.ui.showHelp
import xyz.malefic.filame.ui.showMainMenu
import xyz.malefic.filame.ui.syncWithGitHub
import xyz.malefic.filame.ui.updateAllPackages

/**
 * Filame - File manager for Arch Linux configurations
 * Manages configuration files across multiple devices with GitHub integration
 *
 * Main entry point for the application.
 */
fun main(vararg args: String) =
    session {
        ConfigManager.ensureConfigDir()
        val config = loadOrCreateConfig()

        // If no arguments provided, show interactive menu
        if (args.isEmpty()) {
            showMainMenu(config)
            return@session
        }

        // Handle command-line arguments
        when (args[0].lowercase()) {
            "c", "configure", "config" -> {
                configureSettings(config)
            }

            "s", "scan" -> {
                scanRepoForPackages(config)
            }

            "l", "list" -> {
                listPackageBundles(config)
            }

            "a", "add", "edit" -> {
                addOrEditPackageBundle(config)
            }

            "i", "install" -> {
                installPackageWithConfig(config)
            }

            "m", "missing" -> {
                installAllMissingPackages(config)
            }

            "u", "update", "upgrade" -> {
                updateAllPackages(config)
            }

            "e", "export" -> {
                exportPackageConfigs(config)
            }

            "g", "git", "sync" -> {
                syncWithGitHub(config)
            }

            "f", "find", "search" -> {
                searchForPackages(config)
            }

            "r", "remove", "uninstall" -> {
                removePackageInteractive(config)
            }

            "h", "help", "--help", "-h" -> {
                showHelp()
            }

            else -> {
                showHelp()
                section {
                    textLine()
                    textLine("Error: Unknown command '${args[0]}'")
                }
            }
        }
    }
