package xyz.malefic.filame.git

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import xyz.malefic.filame.config.FilameConfig
import java.io.File

/**
 * Small typed error hierarchy for Git-related failures. Callers can inspect the specific
 * GitError variant to decide how to present errors in the UI layer.
 */
sealed class GitError {
    object RepoNotConfigured : GitError()

    data class GitApi(
        val message: String,
    ) : GitError()

    data class PullFailed(
        val message: String,
    ) : GitError()

    data class PushFailed(
        val message: String,
    ) : GitError()

    data class CommitFailed(
        val message: String,
    ) : GitError()

    data class SaveCredentialsFailed(
        val message: String,
    ) : GitError()

    data class IoError(
        val message: String,
    ) : GitError()

    override fun toString(): String =
        when (this) {
            RepoNotConfigured -> "Repository not configured"
            is GitApi -> "Git API error: $message"
            is PullFailed -> "Pull failed: $message"
            is PushFailed -> "Push failed: $message"
            is CommitFailed -> "Commit failed: $message"
            is SaveCredentialsFailed -> "Save credentials failed: $message"
            is IoError -> "I/O error: $message"
        }
}

/**
 * Manages Git operations for syncing configuration files.
 *
 * @property config The configuration object containing details such as the GitHub repository URL.
 */
class GitManager(
    private val config: FilameConfig,
) {
    private val repoDir = File(System.getProperty("user.home"), ".config/filame/repo")

    /**
     * Initializes or clones the repository.
     *
     * @return An `Either` containing the initialized or cloned `Git` object on the right, or a GitError on the left.
     */
    fun initializeRepo(): Either<GitError, Git> =
        either {
            try {
                if (repoDir.exists() && File(repoDir, ".git").exists()) {
                    Git.open(repoDir)
                } else {
                    repoDir.mkdirs()
                    Git
                        .cloneRepository()
                        .setURI(config.githubRepo)
                        .setDirectory(repoDir)
                        .call()
                }
            } catch (e: GitAPIException) {
                raise(GitError.GitApi(e.message ?: e.toString()))
            } catch (e: Exception) {
                raise(GitError.IoError(e.message ?: e.toString()))
            }
        }

    /**
     * Pulls the latest changes from the remote repository.
     *
     * @param git The `Git` object representing the repository.
     * @return An `Either` indicating success or failure of the pull operation.
     */
    fun pull(git: Git): Either<GitError, Unit> =
        either {
            try {
                git.pull().call()
            } catch (e: GitAPIException) {
                raise(GitError.PullFailed(e.message ?: e.toString()))
            } catch (e: Exception) {
                raise(GitError.IoError(e.message ?: e.toString()))
            }
        }

    /**
     * Pushes local changes to the remote repository.
     *
     * @param git The `Git` object representing the repository.
     * @param username Optional username for authentication.
     * @param token Optional personal access token for authentication.
     * @return An `Either` indicating success or failure of the push operation.
     */
    fun push(
        git: Git,
        username: String? = null,
        token: String? = null,
    ): Either<GitError, Unit> =
        either {
            try {
                val pushCommand = git.push()
                if (username != null && token != null) {
                    pushCommand.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(username, token),
                    )
                }
                pushCommand.call()
            } catch (e: GitAPIException) {
                raise(GitError.PushFailed(e.message ?: e.toString()))
            }
        }

    /**
     * Commits changes to the repository.
     *
     * @param git The `Git` object representing the repository.
     * @param message The commit message.
     * @return An `Either` indicating success or failure of the commit operation.
     */
    fun commit(
        git: Git,
        message: String,
    ): Either<GitError, Unit> =
        either {
            try {
                git.add().addFilepattern(".").call()
                git.commit().setMessage(message).call()
            } catch (e: GitAPIException) {
                raise(GitError.CommitFailed(e.message ?: e.toString()))
            }
        }

    /**
     * Save credentials to ~/.git-credentials and attempt to enable the Git credential store helper.
     * This is non-UI logic and returns an [Either] so callers can decide how to surface errors.
     */
    fun saveCredentials(
        username: String,
        token: String,
    ): Either<GitError, Unit> =
        either {
            val credFile = File(System.getProperty("user.home"), ".git-credentials")
            val entry = "https://$username:$token@github.com\n"

            try {
                credFile.appendText(entry)
            } catch (e: Exception) {
                raise(GitError.IoError(e.message ?: e.toString()))
            }

            try {
                val process =
                    ProcessBuilder("git", "config", "--global", "credential.helper", "store")
                        .redirectErrorStream(true)
                        .start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val output =
                        process.inputStream
                            .bufferedReader()
                            .use { it.readText() }
                            .trim()
                    raise(GitError.SaveCredentialsFailed(output))
                }
            } catch (e: Exception) {
                raise(GitError.SaveCredentialsFailed(e.message ?: e.toString()))
            }
        }

    /**
     * Attempt to push, first without credentials then (optionally) with provided credentials.
     * If `saveCredentialsIfUsed` is true and the push with credentials succeeds, this will attempt
     * to persist credentials via `saveCredentials`.
     */
    fun tryPushWithOptionalCredentials(
        git: Git,
        username: String? = null,
        token: String? = null,
        saveCredentialsIfUsed: Boolean = false,
    ): Either<GitError, Unit> =
        push(git).fold(
            ifLeft = { _ ->
                either {
                    ensure(!username.isNullOrEmpty() && !token.isNullOrEmpty()) {
                        raise(GitError.PushFailed("Credentials required"))
                    }

                    push(git, username, token).bind()

                    if (saveCredentialsIfUsed) {
                        saveCredentials(username, token)
                            .onLeft { error ->
                                raise(GitError.SaveCredentialsFailed("Push succeeded but saving credentials failed: $error"))
                            }.bind()
                    }
                }
            },
            ifRight = { Either.Right(Unit) },
        )

    /**
     * High-level orchestration: commit changes and attempt to push.
     * If the initial push fails and a `credentialProvider` is supplied, the provider will be
     * invoked to obtain username/token which will be used to retry the push. The provider
     * is a callback supplied by the UI layer and may perform prompts — GitManager itself
     * remains UI-agnostic.
     *
     * @param git The repository `Git` handle.
     * @param commitMessage The commit message to use.
     * @param credentialProvider Optional callback invoked only when a credential-based retry is needed.
     *                           Return a Pair(username, token) or `null` to skip retry.
     * @param saveCredentialsIfUsed Whether to attempt to persist credentials when a retry succeeds.
     */
    fun pushWithCommitAndRetry(
        git: Git,
        commitMessage: String,
        credentialProvider: (() -> Pair<String, String>?)? = null,
        saveCredentialsIfUsed: Boolean = true,
    ): Either<GitError, Unit> =
        either {
            commit(git, commitMessage).bind()
            push(git).bind()
        }.fold(
            ifLeft = { firstError ->
                either {
                    val provider = credentialProvider ?: raise(firstError)

                    val creds =
                        try {
                            provider()
                        } catch (e: Exception) {
                            raise(GitError.PushFailed("Credential provider failed: ${e.message}"))
                        }

                    ensure(creds != null) { raise(firstError) }

                    val (username, token) = creds
                    ensure(username.isNotEmpty() && token.isNotEmpty()) {
                        raise(GitError.PushFailed("Empty username or token provided"))
                    }

                    tryPushWithOptionalCredentials(git, username, token, saveCredentialsIfUsed).bind()
                }
            },
            ifRight = { Either.Right(Unit) },
        )

    /**
     * Perform a Git pull operation.
     *
     * Prepares the git repository and pulls the latest changes from the remote.
     * Automatically closes the Git instance after the operation completes.
     *
     * @return An `Either` indicating success or failure with appropriate [GitError].
     */
    fun syncPull(): Either<GitError, Unit> =
        either {
            val (gitManager, git) = config.prepareGitRepo().bind()
            git.use { git ->
                gitManager.pull(git).bind()
            }
        }

    /**
     * Perform a Git push operation with commit.
     *
     * Prepares the git repository, commits changes with the provided message, and pushes to remote.
     * Supports credential prompting via callback and credential persistence.
     * Automatically closes the Git instance after the operation completes.
     *
     * @param commitMessage The commit message to use.
     * @param credentialProvider Optional callback invoked when credentials are needed for push.
     * @param saveCredentialsIfUsed Whether to persist credentials when push succeeds with them.
     * @return An `Either` indicating success or failure with appropriate [GitError].
     */
    fun syncPush(
        commitMessage: String,
        credentialProvider: (() -> Pair<String, String>?)? = null,
        saveCredentialsIfUsed: Boolean = true,
    ): Either<GitError, Unit> =
        either {
            val (gitManager, git) = config.prepareGitRepo().bind()
            git.use { git ->
                gitManager.pushWithCommitAndRetry(git, commitMessage, credentialProvider, saveCredentialsIfUsed).bind()
            }
        }
}

/**
 * Prepare a GitManager and initialize the Git repository for this config.
 *
 * This function returns an Either containing a Pair of the created GitManager and the initialized
 * Git instance on success, or a GitError on the left.
 */
fun FilameConfig.prepareGitRepo(): Either<GitError, Pair<GitManager, Git>> =
    either {
        ensure(githubRepo.isNotEmpty()) { raise(GitError.RepoNotConfigured) }

        val gitManager = GitManager(this@prepareGitRepo)
        val git = gitManager.initializeRepo().bind()

        gitManager to git
    }
