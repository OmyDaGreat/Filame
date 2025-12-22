/**
 * Git synchronization user interface components for Filame.
 * 
 * This file contains UI-only functions for Git operations including pull and push.
 * All business logic is delegated to [xyz.malefic.filame.git.GitManager].
 */
package xyz.malefic.filame.ui

import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.git.GitError
import xyz.malefic.filame.git.GitException
import xyz.malefic.filame.git.GitManager

/**
 * Display a submenu for Git synchronization operations.
 * 
 * Presents options to pull from or push to GitHub, handling user input
 * and delegating to the appropriate sync functions.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The current application configuration.
 * @return The potentially updated configuration (unchanged in this case).
 */
fun Session.syncWithGitHub(config: FilameConfig): FilameConfig {
    var currentConfig = config
    var syncing = true

    while (syncing) {
        section {
            cyan { textLine("═══ Sync with GitHub ═══") }
            textLine()
            green { textLine("1. Pull changes from GitHub") }
            green { textLine("2. Push changes to GitHub") }
            cyan { textLine("3. Back to main menu") }
            textLine()
        }.run()

        when (readInput("Select an option: ")) {
            "1" -> currentConfig = syncPull(currentConfig)
            "2" -> currentConfig = syncPush(currentConfig)
            "3" -> syncing = false
        }

        if (syncing) {
            readInput("\nPress Enter to continue...")
        }
    }

    return currentConfig
}

/**
 * Pull latest changes from GitHub and display the result.
 * 
 * Delegates to [GitManager.syncPull] for the actual operation and renders
 * appropriate success or error messages based on the result.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The current application configuration.
 * @return The configuration (unchanged).
 */
fun Session.syncPull(config: FilameConfig): FilameConfig {
    section {
        cyan { textLine("═══ Sync from GitHub (Pull) ═══") }
        textLine()
    }.run()

    section {
        textLine("Pulling latest changes from GitHub...")
    }.run()

    val gitManager = GitManager(config)
    val pullResult = gitManager.syncPull()

    if (pullResult.isSuccess) {
        section {
            green { textLine("✓ Successfully pulled changes from GitHub") }
            yellow { textLine("Tip: Use 'Scan repo for packages' to update your pkg list") }
        }.run()
    } else {
        val ex = pullResult.exceptionOrNull() as? GitException
        when (val err = ex?.error) {
            GitError.RepoNotConfigured -> showError("GitHub repository not configured. Please configure it in settings.")
            is GitError.PullFailed -> showError("Error pulling from GitHub: ${err.message}")
            else -> showError("Error pulling from GitHub: ${err ?: pullResult.exceptionOrNull()?.message}")
        }
    }

    return config
}

/**
 * Push local changes to GitHub and display the result.
 * 
 * Prompts for a commit message, delegates to [GitManager.syncPush] for the actual
 * operation (including credential prompting if needed), and renders appropriate
 * success or error messages based on the result.
 *
 * @receiver The Kotter session for UI rendering.
 * @param config The current application configuration.
 * @return The configuration (unchanged).
 */
fun Session.syncPush(config: FilameConfig): FilameConfig {
    section {
        cyan { textLine("═══ Sync to GitHub (Push) ═══") }
        textLine()
    }.run()

    val message = readInput("Enter commit message: ").ifEmpty { "Update configs from ${config.deviceName}" }

    section { textLine("Committing and pushing changes...") }.run()

    val gitManager = GitManager(config)
    val result = gitManager.syncPush(
        commitMessage = message,
        credentialProvider = { promptCredentials() },
        saveCredentialsIfUsed = true,
    )

    if (result.isSuccess) {
        showSuccess("✓ Successfully pushed changes to GitHub")
        return config
    }

    val ex = result.exceptionOrNull() as? GitException
    when (val err = ex?.error) {
        GitError.RepoNotConfigured -> {
            showError("GitHub repository not configured. Please configure it in settings.")
        }
        is GitError.CommitFailed -> {
            showError("Error committing: ${err.message}")
        }
        is GitError.SaveCredentialsFailed -> {
            showSuccess("✓ Successfully pushed changes to GitHub with provided credentials")
            showError("Warning: ${err.message}")
        }
        is GitError.PushFailed -> {
            showError("Error pushing to GitHub: ${err.message}")
            showError("Ensure the token has repo permissions and the username/token are correct")
        }
        else -> {
            showError("Error during push: ${err?.toString() ?: result.exceptionOrNull()?.message}")
        }
    }

    return config
}
