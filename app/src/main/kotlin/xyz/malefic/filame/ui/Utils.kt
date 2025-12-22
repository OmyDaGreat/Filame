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
 * Show either a success or an error message based on a condition.
 *
 * If [condition] is true, displays [success] via [showSuccess], otherwise
 * displays [failure] via [showError].
 *
 * @receiver The session used to render the message.
 * @param condition Condition determining which message to show.
 * @param success Message shown when [condition] is true.
 * @param failure Message shown when [condition] is false.
 */
fun Session.showConditional(
    condition: Boolean,
    success: String,
    failure: String,
) = if (condition) {
    showSuccess(success)
} else {
    showError(failure)
}

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
