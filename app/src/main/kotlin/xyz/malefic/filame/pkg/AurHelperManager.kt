package xyz.malefic.filame.pkg

import java.io.File

/**
 * Manages AUR helper detection and installation
 */
object AurHelperManager {
    /**
     * Check if a specific AUR helper is installed
     */
    fun isInstalled(helper: AurHelper): Boolean =
        try {
            val process = ProcessBuilder("which", helper.command).start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }

    /**
     * Check if any AUR helper is installed
     */
    fun isAnyInstalled(): Boolean = AurHelper.entries.any { isInstalled(it) }

    /**
     * Get the installed AUR helper (yay takes priority over paru), or null if neither is installed
     */
    fun getInstalled(): AurHelper? = AurHelper.entries.firstOrNull { isInstalled(it) }

    /**
     * Install an AUR helper
     */
    fun install(helper: AurHelper): Result<Unit> {
        return try {
            if (isInstalled(helper)) {
                return Result.success(Unit)
            }

            // Install dependencies
            val deps = arrayOf("git", "base-devel")
            for (dep in deps) {
                val checkProcess = ProcessBuilder("pacman", "-Qq", dep).start()
                if (checkProcess.waitFor() != 0) {
                    val installProcess =
                        ProcessBuilder("sudo", "pacman", "-S", "--noconfirm", dep)
                            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                            .redirectError(ProcessBuilder.Redirect.INHERIT)
                            .start()
                    if (installProcess.waitFor() != 0) {
                        return Result.failure(Exception("Failed to install $dep"))
                    }
                }
            }

            // Clone and build the AUR helper
            val tmpDir = File("/tmp/${helper.command}-install-${System.currentTimeMillis()}")
            tmpDir.mkdirs()

            val cloneProcess =
                ProcessBuilder("git", "clone", "https://aur.archlinux.org/${helper.command}.git")
                    .directory(tmpDir)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            if (cloneProcess.waitFor() != 0) {
                tmpDir.deleteRecursively()
                return Result.failure(Exception("Failed to clone ${helper.command} repository"))
            }

            val helperDir = File(tmpDir, helper.command)
            val buildProcess =
                ProcessBuilder("makepkg", "-si", "--noconfirm")
                    .directory(helperDir)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            val exitCode = buildProcess.waitFor()
            tmpDir.deleteRecursively()

            if (exitCode != 0) {
                return Result.failure(Exception("Failed to build and install ${helper.command}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
