package xyz.malefic.filame.`package`

/**
 * Supported AUR helpers
 */
enum class AurHelper(
    val command: String,
) {
    YAY("yay"),
    PARU("paru"),
    ;

    companion object {
        /**
         * Default AUR helper to install if none is present
         */
        val DEFAULT = YAY
    }
}
