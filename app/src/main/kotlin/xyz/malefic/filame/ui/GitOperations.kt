package xyz.malefic.filame.ui

import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.git.GitError
import xyz.malefic.filame.git.GitException
import xyz.malefic.filame.git.prepareGitRepo

/**
 * Sync with GitHub (pull and push)
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
 * Perform a Git pull against the configured GitHub repository and report status to the user.
 */
fun Session.syncPull(config: FilameConfig): FilameConfig {
    section {
        cyan { textLine("═══ Sync from GitHub (Pull) ═══") }
        textLine()
    }.run()

    val prepResult = config.prepareGitRepo()

    if (prepResult.isFailure) {
        val ex = prepResult.exceptionOrNull() as? GitException
        when (ex?.error) {
            GitError.RepoNotConfigured -> showError("GitHub repository not configured. Please configure it in settings.")
            else -> showError("Git repository not ready: ${ex?.error ?: prepResult.exceptionOrNull()?.message}")
        }
        return config
    }

    val (gitManager, git) = prepResult.getOrThrow()

    section {
        textLine("Pulling latest changes from GitHub...")
    }.run()

    val pullResult = gitManager.pull(git)

    if (pullResult.isSuccess) {
        section {
            green { textLine("✓ Successfully pulled changes from GitHub") }
            yellow { textLine("Tip: Use 'Scan repo for packages' to update your pkg list") }
        }.run()
    } else {
        val ex = pullResult.exceptionOrNull() as? GitException
        val err = ex?.error
        when (err) {
            is GitError.PullFailed -> showError("Error pulling from GitHub: ${err.message}")
            else -> showError("Error pulling from GitHub: ${err ?: pullResult.exceptionOrNull()?.message}")
        }
    }

    git.close()
    return config
}

/**
 * Pushes local changes to the configured GitHub repository.
 */
fun Session.syncPush(config: FilameConfig): FilameConfig {
    section {
        cyan { textLine("═══ Sync to GitHub (Push) ═══") }
        textLine()
    }.run()

    val prepResult = config.prepareGitRepo()
    if (prepResult.isFailure) {
        val ex = prepResult.exceptionOrNull() as? GitException
        when (ex?.error) {
            GitError.RepoNotConfigured -> showError("GitHub repository not configured. Please configure it in settings.")
            else -> showError("Git repository not ready: ${ex?.error ?: prepResult.exceptionOrNull()?.message}")
        }
        return config
    }

    val (gitManager, git) = prepResult.getOrThrow()

    val message = readInput("Enter commit message: ").ifEmpty { "Update configs from ${config.deviceName}" }

    section { textLine("Committing and pushing changes...") }.run()

    // Provide credentials when needed via a callback that prompts the user
    val result =
        gitManager.pushWithCommitAndRetry(
            git,
            message,
            credentialProvider = { promptCredentials() },
            saveCredentialsIfUsed = true,
        )

    if (result.isSuccess) {
        showSuccess("✓ Successfully pushed changes to GitHub")
        git.close()
        return config
    }

    val ex = result.exceptionOrNull() as? GitException

    when (val err = ex?.error) {
        is GitError.CommitFailed -> {
            showError("Error committing: ${err.message}")
        }

        is GitError.SaveCredentialsFailed -> {
            // Special case: push succeeded but saving credentials failed (we represent this as a SaveCredentialsFailed).
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

    git.close()
    return config
}
