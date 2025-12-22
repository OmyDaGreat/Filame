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
 * Main menu for the application
 */
fun Session.showMainMenu(initialConfig: FilameConfig) {
    var config = initialConfig
    var running = true

    while (running) {
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
                "3. List pkg bundles",
                "4. Add/Edit pkg bundle",
                "5. Install pkg & apply config",
                "6. Install all missing packages",
                "7. Update all packages",
                "8. Export pkg configs to repo",
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
                    Keys.DIGIT_1 -> {
                        choice = 1
                    }

                    Keys.DIGIT_2 -> {
                        choice = 2
                    }

                    Keys.DIGIT_3 -> {
                        choice = 3
                    }

                    Keys.DIGIT_4 -> {
                        choice = 4
                    }

                    Keys.DIGIT_5 -> {
                        choice = 5
                    }

                    Keys.DIGIT_6 -> {
                        choice = 6
                    }

                    Keys.DIGIT_7 -> {
                        choice = 7
                    }

                    Keys.DIGIT_8 -> {
                        choice = 8
                    }

                    Keys.DIGIT_9 -> {
                        choice = 9
                    }

                    Keys.DIGIT_0 -> {
                        choice = 0
                    }

                    Keys.LEFT, Keys.UP -> {
                        choice =
                            when (choice) {
                                1 -> 0
                                0 -> 9
                                else -> choice - 1
                            }
                    }

                    Keys.RIGHT, Keys.DOWN -> {
                        choice =
                            when (choice) {
                                9 -> 0
                                0 -> 1
                                else -> choice + 1
                            }
                    }

                    Keys.ENTER -> {
                        when (choice) {
                            1 -> {
                                config = configureSettings(config)
                            }

                            2 -> {
                                config = scanRepoForPackages(config)
                            }

                            3 -> {
                                listPackageBundles(config)
                            }

                            4 -> {
                                config = addOrEditPackageBundle(config)
                            }

                            5 -> {
                                installPackageWithConfig(config)
                            }

                            6 -> {
                                installAllMissingPackages(config)
                            }

                            7 -> {
                                updateAllPackages(config)
                            }

                            8 -> {
                                exportPackageConfigs(config)
                            }

                            9 -> {
                                config = syncWithGitHub(config)
                            }

                            0 -> {
                                exit = true
                            }
                        }
                    }
                }
            }
        }
    }
}
