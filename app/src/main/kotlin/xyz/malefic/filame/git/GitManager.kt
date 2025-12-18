package xyz.malefic.filame.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import xyz.malefic.filame.config.FilameConfig
import java.io.File

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
     * @return A `Result` containing the initialized or cloned `Git` object, or an exception if the operation fails.
     */
    fun initializeRepo(): Result<Git> {
        return try {
            if (repoDir.exists() && File(repoDir, ".git").exists()) {
                Result.success(Git.open(repoDir))
            } else {
                if (config.githubRepo.isEmpty()) {
                    return Result.failure(Exception("GitHub repository URL not configured"))
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
        } catch (e: Exception) {
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
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
            Result.failure(e)
        }

    /**
     * Retrieves the directory where the repository is stored.
     *
     * @return The `File` object representing the repository directory.
     */
    fun getRepoDir(): File = repoDir
}
