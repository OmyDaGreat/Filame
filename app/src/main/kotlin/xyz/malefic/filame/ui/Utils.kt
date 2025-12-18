package xyz.malefic.filame.ui

import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Session

/**
 * Common completions for yes/no prompts
 */
val yesNoCompletions = Completions("y", "n", "yes", "no")

/**
 * Display a colored header using cyan text and a trailing blank line.
 *
 * Renders the provided text as a cyan colored header within the current [Session].
 *
 * @receiver The session used to render the header.
 * @param text The header text to display.
 */
fun Session.displayHeader(text: String) =
    section {
        cyan { textLine(text) }
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
