package xyz.malefic.filame.pkg

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import java.io.File

/**
 * Utility object to detect and install AUR helpers on an Arch Linux system.
 *
 * This manager provides functions to:
 * - Check whether a specific AUR helper is available on the system.
 * - Check whether any supported AUR helper is installed.
 * - Determine which supported AUR helper is currently installed.
 * - Install an AUR helper by ensuring build dependencies, cloning the AUR repository,
 *   and building/installing the package using `makepkg`.
 *
 * Note: Installation performs external commands (pacman, git, makepkg) and may invoke
 * `sudo` for package installation. Callers should ensure the environment is appropriate
 * and that the process has necessary privileges.
 */
object AurHelperManager {
    /**
     * Check if a specific AUR helper is installed.
     *
     * The check uses the `which` executable to determine whether the helper's command
     * is present in `PATH`.
     *
     * @param helper The AUR helper to check.
     * @return `true` if the helper's executable is found in `PATH`, otherwise `false`.
     */
    fun isInstalled(helper: AurHelper): Boolean =
        try {
            val process = ProcessBuilder("which", helper.command).start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }

    /**
     * Check if any supported AUR helper is installed.
     *
     * Iterates through the known `AurHelper.entries` and returns `true` as soon as
     * a helper is detected on the system.
     *
     * @return `true` if at least one supported AUR helper is installed, otherwise `false`.
     */
    fun isAnyInstalled(): Boolean = AurHelper.entries.any { isInstalled(it) }

    /**
     * Get the first installed AUR helper from the supported entries.
     *
     * The order of `AurHelper.entries` determines priority (for example, `yay` can be
     * prioritized over `paru` by ordering). Returns `null` if none are installed.
     *
     * @return The detected `AurHelper` instance or `null` if none are installed.
     */
    fun getInstalled(): AurHelper? = AurHelper.entries.firstOrNull { isInstalled(it) }

    /**
     * Install a given AUR helper from the official AUR repository.
     *
     * Installation steps:
     * 1. Ensure required dependencies (`git`, `base-devel`) are installed using `pacman`.
     * 2. Clone the AUR repository for the helper into a temporary directory under `/tmp`.
     * 3. Run `makepkg -si --noconfirm` inside the cloned package directory to build and install.
     * 4. Clean up the temporary directory after the build.
     *
     * This function executes external processes and redirects their output to the current
     * process' stdout/stderr. It will attempt to use `sudo` for `pacman -S` if dependencies
     * need installation.
     *
     * @param helper The AUR helper to install.
     * @return `Either.Right(Unit)` on successful installation, or `Either.Left` with
     *         an error message describing the failure.
     *
     * @throws SecurityException If the runtime environment prevents executing the required
     *         external commands. Exceptions thrown by process execution are captured and
     *         returned as `Either.Left`.
     */
    fun install(helper: AurHelper): Either<String, Unit> =
        either<Throwable, Unit> {
            if (isInstalled(helper)) {
                return@either
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
                    ensure(installProcess.waitFor() == 0) {
                        raise(Exception("Failed to install $dep"))
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
                raise(Exception("Failed to clone ${helper.command} repository"))
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

            ensure(exitCode == 0) {
                raise(Exception("Failed to build and install ${helper.command}"))
            }
        }.mapLeft { it.message ?: "Unknown error" }
}
