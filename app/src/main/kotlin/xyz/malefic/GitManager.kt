package xyz.malefic

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Manages Git operations for syncing configuration files
 */
class GitManager(
    private val config: FilameConfig,
) {
    private val repoDir = File(System.getProperty("user.home"), ".config/filame/repo")

    /**
     * Initialize or clone the repository
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
     * Pull latest changes from remote
     */
    fun pull(git: Git): Result<Unit> =
        try {
            git.pull().call()
            Result.success(Unit)
        } catch (e: GitAPIException) {
            Result.failure(e)
        }

    /**
     * Push changes to remote
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
     * Commit changes
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

    fun getRepoDir(): File = repoDir
}
