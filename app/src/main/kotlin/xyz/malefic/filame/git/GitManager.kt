package xyz.malefic.filame.git

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
 * Exception wrapper carrying a GitError. Use this when returning a failed kotlin.Result so
 * the failure value is a throwable that still exposes a typed GitError.
 */
class GitException(
    val error: GitError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)

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
     * @return A `Result` containing the initialized or cloned `Git` object, or a GitException if the operation fails.
     */
    fun initializeRepo(): Result<Git> {
        return try {
            if (repoDir.exists() && File(repoDir, ".git").exists()) {
                Result.success(Git.open(repoDir))
            } else {
                if (config.githubRepo.isEmpty()) {
                    return Result.failure(GitException(GitError.RepoNotConfigured))
                }
                repoDir.mkdirs()
                Result.success(
                    Git
                        .cloneRepository()
                        .setURI(config.githubRepo)
                        .setDirectory(repoDir)
                        .call(),
                )
            }
        } catch (e: GitAPIException) {
            Result.failure(GitException(GitError.GitApi(e.message ?: e.toString()), e))
        } catch (e: Exception) {
            Result.failure(GitException(GitError.IoError(e.message ?: e.toString()), e))
        }
    }

    /**
     * Pulls the latest changes from the remote repository.
     *
     * @param git The `Git` object representing the repository.
     * @return A `Result` indicating success or failure of the pull operation.
     */
    fun pull(git: Git): Result<Unit> =
        try {
            git.pull().call()
            Result.success(Unit)
        } catch (e: GitAPIException) {
            Result.failure(GitException(GitError.PullFailed(e.message ?: e.toString()), e))
        }

    /**
     * Pushes local changes to the remote repository.
     *
     * @param git The `Git` object representing the repository.
     * @param username Optional username for authentication.
     * @param token Optional personal access token for authentication.
     * @return A `Result` indicating success or failure of the push operation.
     */
    fun push(
        git: Git,
        username: String? = null,
        token: String? = null,
    ): Result<Unit> =
        try {
            val pushCommand = git.push()
            if (username != null && token != null) {
                pushCommand.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(username, token),
                )
            }
            pushCommand.call()
            Result.success(Unit)
        } catch (e: GitAPIException) {
            Result.failure(GitException(GitError.PushFailed(e.message ?: e.toString()), e))
        }

    /**
     * Commits changes to the repository.
     *
     * @param git The `Git` object representing the repository.
     * @param message The commit message.
     * @return A `Result` indicating success or failure of the commit operation.
     */
    fun commit(
        git: Git,
        message: String,
    ): Result<Unit> =
        try {
            git.add().addFilepattern(".").call()
            git.commit().setMessage(message).call()
            Result.success(Unit)
        } catch (e: GitAPIException) {
            Result.failure(GitException(GitError.CommitFailed(e.message ?: e.toString()), e))
        }

    /**
     * Retrieves the directory where the repository is stored.
     *
     * @return The `File` object representing the repository directory.
     */
    fun getRepoDir(): File = repoDir

    /**
     * Save credentials to ~/.git-credentials and attempt to enable the Git credential store helper.
     * This is non-UI logic and returns a Result so callers can decide how to surface errors.
     */
    fun saveCredentials(
        username: String,
        token: String,
    ): Result<Unit> {
        return try {
            val credFile = File(System.getProperty("user.home"), ".git-credentials")
            val entry = "https://$username:$token@github.com\n"
            try {
                credFile.appendText(entry)
            } catch (e: Exception) {
                return Result.failure(GitException(GitError.IoError(e.message ?: e.toString()), e))
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
                    // Credentials were written but enabling store helper failed
                    return Result.failure(GitException(GitError.SaveCredentialsFailed(output)))
                }
            } catch (e: Exception) {
                return Result.failure(GitException(GitError.SaveCredentialsFailed(e.message ?: e.toString()), e))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(GitException(GitError.SaveCredentialsFailed(e.message ?: e.toString()), e))
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
    ): Result<Unit> {
        val firstAttempt = push(git)
        if (firstAttempt.isSuccess) return firstAttempt

        if (!username.isNullOrEmpty() && !token.isNullOrEmpty()) {
            val secondAttempt = push(git, username, token)
            if (secondAttempt.isSuccess) {
                if (saveCredentialsIfUsed) {
                    val saveResult = saveCredentials(username, token)
                    if (saveResult.isFailure) {
                        // Return failure to allow UI to show message but indicate that push succeeded
                        val msg = "Push succeeded but saving credentials failed: ${saveResult.exceptionOrNull()?.message}"
                        return Result.failure(GitException(GitError.SaveCredentialsFailed(msg)))
                    }
                }
                return Result.success(Unit)
            }
            return secondAttempt
        }

        return firstAttempt
    }

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
    ): Result<Unit> {
        // Commit first
        val commitResult = commit(git, commitMessage)
        if (commitResult.isFailure) return commitResult

        // Try pushing without credentials first
        val firstPush = push(git)
        if (firstPush.isSuccess) return firstPush

        // If no credential provider given, return the original push failure
        if (credentialProvider == null) return firstPush

        // Ask UI for credentials (UI may return null to indicate user declined)
        val creds =
            try {
                credentialProvider()
            } catch (e: Exception) {
                return Result.failure(GitException(GitError.PushFailed("Credential provider failed: ${e.message}"), e))
            }

        if (creds == null) return firstPush

        val (username, token) = creds
        if (username.isEmpty() || token.isEmpty()) {
            return Result.failure(GitException(GitError.PushFailed("Empty username or token provided")))
        }

        return tryPushWithOptionalCredentials(git, username, token, saveCredentialsIfUsed)
    }
}

/**
 * Prepare a GitManager and initialize the Git repository for this config.
 *
 * This function returns a Result containing a Pair of the created GitManager and the initialized
 * Git instance on success, or a failure Result with the underlying GitException. It intentionally
 * does not perform any UI actions so callers (UI layer) can decide how to present errors.
 */
fun FilameConfig.prepareGitRepo(): Result<Pair<GitManager, Git>> {
    if (this.githubRepo.isEmpty()) {
        return Result.failure(GitException(GitError.RepoNotConfigured))
    }

    val gitManager = GitManager(this)
    val gitResult = gitManager.initializeRepo()

    return if (gitResult.isSuccess) {
        Result.success(gitManager to gitResult.getOrThrow())
    } else {
        Result.failure(
            gitResult.exceptionOrNull() as? GitException
                ?: GitException(GitError.GitApi(gitResult.exceptionOrNull()?.message ?: "Unknown error"), gitResult.exceptionOrNull()),
        )
    }
}
