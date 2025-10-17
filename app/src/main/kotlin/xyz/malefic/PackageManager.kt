package xyz.malefic

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Manages package operations including installation, removal, and tracking
 */
class PackageManager(
    private val config: FilameConfig,
) {
    /**
     * Check if paru is installed
     */
    fun isParuInstalled(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("which paru")
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Install paru AUR helper
     */
    fun installParu(): Result<Unit> {
        return try {
            if (isParuInstalled()) {
                return Result.success(Unit)
            }

            // Install dependencies
            val deps = arrayOf("git", "base-devel")
            for (dep in deps) {
                val checkProcess = Runtime.getRuntime().exec("pacman -Qq $dep")
                if (checkProcess.waitFor() != 0) {
                    val installProcess = ProcessBuilder("sudo", "pacman", "-S", "--noconfirm", dep)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()
                    if (installProcess.waitFor() != 0) {
                        return Result.failure(Exception("Failed to install $dep"))
                    }
                }
            }

            // Clone and build paru
            val tmpDir = File("/tmp/paru-install-${System.currentTimeMillis()}")
            tmpDir.mkdirs()

            val cloneProcess = ProcessBuilder("git", "clone", "https://aur.archlinux.org/paru.git")
                .directory(tmpDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            
            if (cloneProcess.waitFor() != 0) {
                tmpDir.deleteRecursively()
                return Result.failure(Exception("Failed to clone paru repository"))
            }

            val paruDir = File(tmpDir, "paru")
            val buildProcess = ProcessBuilder("makepkg", "-si", "--noconfirm")
                .directory(paruDir)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            
            val exitCode = buildProcess.waitFor()
            tmpDir.deleteRecursively()

            if (exitCode != 0) {
                return Result.failure(Exception("Failed to build and install paru"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search for packages in official repos and AUR
     */
    fun searchPackages(query: String): Result<List<PackageSearchResult>> {
        return try {
            val results = mutableListOf<PackageSearchResult>()

            // Search official repos with pacman
            val pacmanProcess = ProcessBuilder("pacman", "-Ss", query)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            
            val pacmanReader = BufferedReader(InputStreamReader(pacmanProcess.inputStream))
            var line: String?
            var currentPackage: String? = null
            
            while (pacmanReader.readLine().also { line = it } != null) {
                if (line!!.startsWith(" ")) {
                    // This is a description line
                    if (currentPackage != null) {
                        results.add(PackageSearchResult(currentPackage, "official", line!!.trim()))
                        currentPackage = null
                    }
                } else {
                    // This is a package line
                    val parts = line!!.split("/")
                    if (parts.size >= 2) {
                        val packageInfo = parts[1].split(" ")
                        currentPackage = packageInfo[0]
                    }
                }
            }
            pacmanProcess.waitFor()

            // Search AUR with paru if available
            if (isParuInstalled()) {
                val paruProcess = ProcessBuilder("paru", "-Ssa", query)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                
                val paruReader = BufferedReader(InputStreamReader(paruProcess.inputStream))
                currentPackage = null
                
                while (paruReader.readLine().also { line = it } != null) {
                    if (line!!.startsWith(" ")) {
                        // This is a description line
                        if (currentPackage != null) {
                            // Only add if not already in results from pacman
                            if (!results.any { it.name == currentPackage }) {
                                results.add(PackageSearchResult(currentPackage!!, "aur", line!!.trim()))
                            }
                            currentPackage = null
                        }
                    } else {
                        // This is a package line
                        val parts = line!!.split("/")
                        if (parts.size >= 2) {
                            val packageInfo = parts[1].split(" ")
                            currentPackage = packageInfo[0]
                        }
                    }
                }
                paruProcess.waitFor()
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a package is installed
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("pacman -Qq $packageName")
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get installation status of all tracked packages
     */
    fun getPackageStatuses(): Map<Package, Boolean> {
        return config.packages.associateWith { pkg ->
            isPackageInstalled(pkg.name)
        }
    }

    /**
     * Install a package
     */
    fun installPackage(pkg: Package): Result<Unit> {
        return try {
            val command = if (pkg.source == "aur") {
                if (!isParuInstalled()) {
                    return Result.failure(Exception("Paru is required for AUR packages but not installed"))
                }
                arrayOf("paru", "-S", "--noconfirm", pkg.name)
            } else {
                arrayOf("sudo", "pacman", "-S", "--noconfirm", pkg.name)
            }

            val process = ProcessBuilder(*command)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                Result.failure(Exception("Failed to install package ${pkg.name}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Install all tracked packages that are not yet installed
     */
    fun installMissingPackages(): Result<List<String>> {
        val installed = mutableListOf<String>()
        val statuses = getPackageStatuses()
        
        for ((pkg, isInstalled) in statuses) {
            if (!isInstalled) {
                val result = installPackage(pkg)
                if (result.isSuccess) {
                    installed.add(pkg.name)
                } else {
                    return Result.failure(result.exceptionOrNull()!!)
                }
            }
        }
        
        return Result.success(installed)
    }

    /**
     * Update all installed packages
     */
    fun updatePackages(): Result<Unit> {
        return try {
            // Update official packages
            val pacmanProcess = ProcessBuilder("sudo", "pacman", "-Syu", "--noconfirm")
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            
            if (pacmanProcess.waitFor() != 0) {
                return Result.failure(Exception("Failed to update official packages"))
            }

            // Update AUR packages if paru is installed
            if (isParuInstalled()) {
                val paruProcess = ProcessBuilder("paru", "-Sua", "--noconfirm")
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                
                if (paruProcess.waitFor() != 0) {
                    return Result.failure(Exception("Failed to update AUR packages"))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove a package
     */
    fun removePackage(packageName: String): Result<Unit> {
        return try {
            val process = ProcessBuilder("sudo", "pacman", "-R", "--noconfirm", packageName)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                Result.failure(Exception("Failed to remove package $packageName"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Search result for a package
 */
data class PackageSearchResult(
    val name: String,
    val source: String,
    val description: String,
)
