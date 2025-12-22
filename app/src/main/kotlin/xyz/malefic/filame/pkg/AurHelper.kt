package xyz.malefic.filame.pkg

/**
 * Enumerates supported AUR helpers.
 *
 * Each enum entry provides the executable command name that can be used to invoke
 * the corresponding helper on a system that has it installed.
 *
 * @property command the executable name used to invoke the helper.
 */
enum class AurHelper(
    val command: String,
) {
    /** The `yay` AUR helper. Commonly used as a default helper on many systems. */
    YAY("yay"),

    /** The `paru` AUR helper. An alternative AUR helper offering similar functionality. */
    PARU("paru"),
    ;

    companion object {
        /**
         * Default AUR helper to install or prefer if none is detected.
         *
         * This value is used as a fallback when selection or detection logic
         * does not find an installed helper.
         */
        val DEFAULT = YAY
    }
}
