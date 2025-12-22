package xyz.malefic.filame.ui

import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session

/**
 * Display a colored header using cyan text and a trailing blank line.
 *
 * Renders the provided text as a cyan colored header within the current [Session].
 *
 * @receiver The session used to render the header.
 * @param text The header text to display.
 * @param subheader An optional subheader text to display in yellow below the main header.
 */
fun Session.displayHeader(
    text: String,
    subheader: String = "",
) = section {
    cyan { textLine(text) }
    if (subheader.isNotBlank()) {
        yellow { textLine(subheader) }
    }
    textLine()
}.run()

/**
 * Read a line of input from the user within the current [Session].
 *
 * This function optionally displays a prompt and configures completions for the input.
 * It blocks until the user enters input and returns the entered string.
 *
 * @receiver The session used to read input.
 * @param prompt Optional prompt text to display before the input field. Defaults to an empty string.
 * @param completions Optional completions to provide possible input values; pass `null` to disable completions.
 * @return The string entered by the user.
 */
fun Session.readInput(
    prompt: String = "",
    completions: Completions? = null,
): String {
    var result = ""
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
    return result
}

/**
 * Show an informational message.
 *
 * Renders the provided message within the current [Session].
 *
 * @receiver The session used to render the message.
 * @param message The informational message to display.
 */
fun Session.showInfo(message: String) = section { textLine(message) }.run()

/**
 * Show a warning message in yellow.
 *
 * Renders the provided message using yellow text within the current [Session].
 *
 * @receiver The session used to render the message.
 * @param message The warning message to display.
 */
fun Session.showWarning(message: String) = section { yellow { textLine("Warning: $message") } }.run()

/**
 * Show an error message in red.
 *
 * Renders the provided message using red text within the current [Session].
 *
 * @receiver The session used to render the message.
 * @param message The error message to display.
 */
fun Session.showError(message: String) = section { red { textLine(message) } }.run()

/**
 * Show a success message in green.
 *
 * Renders the provided message using green text within the current [Session].
 *
 * @receiver The session used to render the message.
 * @param message The success message to display.
 */
fun Session.showSuccess(message: String) = section { green { textLine(message) } }.run()

/**
 * Common completions for yes/no prompts
 */
val yesNoCompletions = Completions("y", "n", "yes", "no")

/**
 * Prompt a yes/no question and return true for yes (accepts y/yes), false otherwise.
 */
fun Session.promptYesNo(prompt: String): Boolean {
    val answer = readInput(prompt, yesNoCompletions).trim().lowercase()
    return answer == "y" || answer == "yes"
}

/**
 * Prompt for username and token. Returns Pair(username, token) where either may be empty if not provided.
 */
fun Session.promptCredentials(): Pair<String, String> {
    val username = readInput("Enter Git username: ").trim()
    val token = readInput("Enter personal access token: ").trim()
    return username to token
}

/**
 * Display help information showing all available commands and their descriptions.
 */
fun Session.showHelp() {
    section {
        cyan { textLine("═══ FILAME - Help ═══") }
        textLine()
        green { textLine("Available Commands:") }
        textLine()

        white { text("  c, configure") }
        textLine(" - Configure device name and GitHub repository")

        white { text("  s, scan") }
        textLine("      - Scan repository for package bundles")

        white { text("  l, list") }
        textLine("      - List all tracked package bundles")

        white { text("  a, add") }
        textLine("       - Add or edit a package bundle")

        white { text("  i, install") }
        textLine("   - Install a package and apply its configuration")

        white { text("  m, missing") }
        textLine("   - Install all missing tracked packages")

        white { text("  u, update") }
        textLine("    - Update all system packages (official + AUR)")

        white { text("  e, export") }
        textLine("    - Export package configurations to repository")

        white { text("  g, git, sync") }
        textLine(" - Sync with GitHub (pull/push)")

        white { text("  f, find, search") }
        textLine(" - Search for packages in repos and AUR")

        white { text("  r, remove") }
        textLine("    - Remove an installed package")

        white { text("  h, help") }
        textLine("     - Show this help message")

        white { text("  q, quit, exit") }
        textLine(" - Exit the application")

        textLine()
        yellow { textLine("Usage:") }
        text("  ")
        cyan { text("filame") }
        textLine("              - Start interactive menu")

        text("  ")
        cyan { text("filame <command>") }
        textLine("   - Run a specific command directly")

        textLine()
        yellow { textLine("Examples:") }
        text("  ")
        cyan { text("filame search") }
        textLine("       - Search for packages")

        text("  ")
        cyan { text("filame sync") }
        textLine("         - Sync with GitHub")

        text("  ")
        cyan { text("filame install") }
        textLine("      - Install a package")
    }.run()
}
