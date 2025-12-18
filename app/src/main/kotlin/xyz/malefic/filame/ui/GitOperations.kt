package xyz.malefic.filame.ui

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session
import org.eclipse.jgit.api.Git
import xyz.malefic.filame.config.FilameConfig
import xyz.malefic.filame.git.GitManager

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
            "2" -> syncPush(currentConfig)
            "3" -> syncing = false
        }

        if (syncing) {
            readInput("\nPress Enter to continue...")
        }
    }

    return currentConfig
}

/**
 * Check repository status and return GitManager and Git instance or config
 */
fun Session.checkRepoStatus(config: FilameConfig): Either<Pair<GitManager, Result<Git>>, FilameConfig> {
    if (config.githubRepo.isEmpty()) {
        section {
            red { textLine("GitHub repository not configured. Please configure first.") }
        }.run()
        return config.right()
    }

    val gitManager = GitManager(config)
    val gitResult = gitManager.initializeRepo()

    if (gitResult.isFailure) {
        section {
            red { textLine("Error initializing repository: ${gitResult.exceptionOrNull()?.message}") }
        }.run()
        return config.right()
    }

    return (gitManager to gitResult).left()
}

/**
 * Pull changes from GitHub
 */
fun Session.syncPull(config: FilameConfig): FilameConfig {
    section {
        cyan { textLine("═══ Sync from GitHub (Pull) ═══") }
        textLine()
    }.run()

    val result = checkRepoStatus(config)

    val (gitManager, git) =
        when (result) {
            is Either.Right -> {
                return result.getOrNull()!!
            }

            is Either.Left -> {
                val (gm, gitResult) = result.value
                val actualGit = gitResult.getOrNull()!!
                gm to actualGit
            }
        }

    section {
        textLine("Pulling latest changes from GitHub...")
    }.run()

    val pullResult = gitManager.pull(git)

    if (pullResult.isSuccess) {
        section {
            green { textLine("✓ Successfully pulled changes from GitHub") }
            yellow { textLine("Tip: Use 'Scan repo for packages' to update your package list") }
        }.run()
    } else {
        section {
            red { textLine("Error pulling from GitHub: ${pullResult.exceptionOrNull()?.message}") }
        }.run()
    }

    git.close()
    return config
}

/**
 * Push changes to GitHub
 */
fun Session.syncPush(config: FilameConfig) {
    section {
        cyan { textLine("═══ Sync to GitHub (Push) ═══") }
        textLine()
    }.run()

    val result = checkRepoStatus(config)

    val (gitManager, git) =
        when (result) {
            is Either.Right -> {
                return
            }

            is Either.Left -> {
                val (gm, gitResult) = result.value
                val actualGit = gitResult.getOrNull()!!
                gm to actualGit
            }
        }

    val message = readInput("Enter commit message: ").ifEmpty { "Update configs from ${config.deviceName}" }

    section {
        textLine("Committing changes...")
    }.run()

    val commitResult = gitManager.commit(git, message)

    if (commitResult.isFailure) {
        section {
            red { textLine("Error committing: ${commitResult.exceptionOrNull()?.message}") }
        }.run()
        git.close()
        return
    }

    section {
        textLine("Pushing to GitHub...")
        yellow { textLine("Note: You may need to configure Git credentials for push") }
    }.run()

    // First attempt without explicit credentials (use system-configured credentials/ssh)
    val pushResult = gitManager.push(git)

    if (pushResult.isSuccess) {
        section {
            green { textLine("✓ Successfully pushed changes to GitHub") }
        }.run()
        git.close()
        return
    }

    // Initial push failed - show error and offer to retry with username/token
    section {
        red { textLine("Error pushing to GitHub: ${pushResult.exceptionOrNull()?.message}") }
        yellow { textLine("Make sure you have configured Git credentials (SSH key or token)") }
    }.run()

    val tryCredentials = readInput("Would you like to provide a username and token to retry push? (y/N): ").trim().lowercase()

    if (tryCredentials == "y" || tryCredentials == "yes") {
        // Prompt for username and token and retry once
        val username = readInput("Enter Git username: ").trim()
        val token = readInput("Enter personal access token (will be visible): ").trim()

        if (username.isNotEmpty() && token.isNotEmpty()) {
            val pushWithCredResult = gitManager.push(git, username, token)
            if (pushWithCredResult.isSuccess) {
                section {
                    green { textLine("✓ Successfully pushed changes to GitHub with provided credentials") }
                }.run()
                        } catch (e: Exception) {
                            section {
                                yellow {
                                    textLine(
                                        "Saved credentials to ~/.git-credentials but could not run git to enable the store helper: ${e.message}",
                                    )
                                }
                            }.run()
                        }
                    } catch (e: Exception) {
                        section {
                            red { textLine("Failed to save credentials: ${e.message}") }
                        }.run()
                    }
                }
            } else {
                section {
                    red { textLine("Error pushing with provided credentials: ${pushWithCredResult.exceptionOrNull()?.message}") }
                    yellow { textLine("Ensure the token has repo permissions and the username/token are correct") }
                }.run()
            }
        } else {
            section {
                red { textLine("Username or token was empty. Aborting push retry.") }
            }.run()
        }
    } else {
        section {
            yellow { textLine("Skipping credential prompt. Configure SSH or token-based auth to enable pushing.") }
        }.run()
    }

    git.close()
}
