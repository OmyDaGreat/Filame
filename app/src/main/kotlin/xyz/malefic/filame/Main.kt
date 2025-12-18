package xyz.malefic.filame

import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.textLine
import xyz.malefic.filame.config.ConfigManager
import xyz.malefic.filame.ui.loadOrCreateConfig
import xyz.malefic.filame.ui.showMainMenu

/**
 * Filame - File manager for Arch Linux configurations
 * Manages configuration files across multiple devices with GitHub integration
 *
 * Main entry point for the application.
 */
fun main(vararg args: String) =
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
