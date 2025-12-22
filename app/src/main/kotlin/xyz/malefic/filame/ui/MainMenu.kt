package xyz.malefic.filame.ui

import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session
import xyz.malefic.filame.config.FilameConfig

/**
 * Display and manage the main application menu.
 *
 * Renders an interactive menu with keyboard navigation, allowing users to select
 * various operations including configuration, package management, and Git synchronization.
 * The menu loops until the user chooses to exit.
 *
 * @receiver The Kotter session for UI rendering and input handling.
 * @param initialConfig The initial application configuration to work with.
 */
fun Session.showMainMenu(initialConfig: FilameConfig) {
    var config = initialConfig
    var running = true

    while (running) {
        var exit by liveVarOf(false)
        var choice by liveVarOf('c')
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
                "c. Configure settings",
                "s. Scan repo for packages",
                "l. List pkg bundles",
                "a. Add/Edit pkg bundle",
                "i. Install pkg & apply config",
                "m. Install all missing packages",
                "u. Update all packages",
                "e. Export pkg configs to repo",
                "g. Sync with GitHub",
                "f. Search for packages",
                "r. Remove package",
                "h. Help",
                "q. Exit",
            ).forEach { line ->
                val key = line[0]
                if (choice == key) {
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
            Keys.ENTER,
        ) {
            onKeyPressed {
                when (key) {
                    Keys.C -> choice = 'c'
                    Keys.S -> choice = 's'
                    Keys.L -> choice = 'l'
                    Keys.A -> choice = 'a'
                    Keys.I -> choice = 'i'
                    Keys.M -> choice = 'm'
                    Keys.U -> choice = 'u'
                    Keys.E -> choice = 'e'
                    Keys.G -> choice = 'g'
                    Keys.F -> choice = 'f'
                    Keys.R -> choice = 'r'
                    Keys.H -> choice = 'h'
                    Keys.Q -> choice = 'q'

                    Keys.LEFT, Keys.UP -> {
                        choice =
                            when (choice) {
                                'c' -> 'q'
                                's' -> 'c'
                                'l' -> 's'
                                'a' -> 'l'
                                'i' -> 'a'
                                'm' -> 'i'
                                'u' -> 'm'
                                'e' -> 'u'
                                'g' -> 'e'
                                'f' -> 'g'
                                'r' -> 'f'
                                'h' -> 'r'
                                'q' -> 'h'
                                else -> 'c'
                            }
                    }

                    Keys.RIGHT, Keys.DOWN -> {
                        choice =
                            when (choice) {
                                'c' -> 's'
                                's' -> 'l'
                                'l' -> 'a'
                                'a' -> 'i'
                                'i' -> 'm'
                                'm' -> 'u'
                                'u' -> 'e'
                                'e' -> 'g'
                                'g' -> 'f'
                                'f' -> 'r'
                                'r' -> 'h'
                                'h' -> 'q'
                                'q' -> 'c'
                                else -> 'c'
                            }
                    }

                    Keys.ENTER -> {
                        when (choice) {
                            'c' -> config = configureSettings(config)
                            's' -> config = scanRepoForPackages(config)
                            'l' -> listPackageBundles(config)
                            'a' -> config = addOrEditPackageBundle(config)
                            'i' -> installPackageWithConfig(config)
                            'm' -> installAllMissingPackages(config)
                            'u' -> updateAllPackages(config)
                            'e' -> exportPackageConfigs(config)
                            'g' -> config = syncWithGitHub(config)
                            'f' -> searchForPackages(config)
                            'r' -> config = removePackageInteractive(config)
                            'h' -> showHelp()
                            'q' -> exit = true
                        }
                    }
                }
            }
        }
    }
}
