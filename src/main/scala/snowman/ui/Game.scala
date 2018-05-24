package snowman.ui

import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest

class Game(gamePath: String, backupPath: String) {

    private val HASH_LAYOUT = "093685a29f53bb8f32fb03eee8a6a38d1d430cc9b46e888d82e1c9d34cc95e96"
    private val HASH_LEVELS = "634634d9186803b507887f94b6a81b1d95336daed331d84f62463d5bb4464b79"

    private val LAYOUT_FILENAME = "layout.txt"
    private val LEVELS_FILENAME = "layout.txt"

    private val RESOURCES_PATH = "resources/"

    def play(map: UILevel): Unit = {
        if (gameHasOriginalFiles) {
            backup()
        }


    }

    def restore(): Unit = {
        val backupLayoutFile = new File(backupPath + LAYOUT_FILENAME)
        val backupLevelsFile = new File(backupPath + LEVELS_FILENAME)

        if (backupLayoutFile.exists && backupLevelsFile.exists) {
            val customLayoutFile = new File(gamePath + RESOURCES_PATH + LAYOUT_FILENAME)
            val customLevelsFile = new File(gamePath + RESOURCES_PATH + LEVELS_FILENAME)

            // Delete custom files
            customLayoutFile.delete
            customLevelsFile.delete

            // Copy files
            Files.copy(backupLayoutFile.toPath, customLayoutFile.toPath)
            Files.copy(backupLevelsFile.toPath, customLevelsFile.toPath)
        } else {
            // TODO throw new
        }
    }

    private def backup(): Unit = {
        if (gameHasOriginalFiles) {
            val originalLayoutFile = new File(gamePath + RESOURCES_PATH + LAYOUT_FILENAME)
            val originalLevelsFile = new File(gamePath + RESOURCES_PATH + LEVELS_FILENAME)

            val backupLayoutFile = new File(backupPath + LAYOUT_FILENAME)
            val backupLevelsFile = new File(backupPath + LEVELS_FILENAME)

            val backupDirecotry = new File(backupPath)

            // Create backup folder
            if (!backupDirecotry.exists) {
                backupDirecotry.mkdir()
            }

            // Copy files
            Files.copy(originalLayoutFile.toPath, backupLayoutFile.toPath)
            Files.copy(originalLevelsFile.toPath, backupLevelsFile.toPath)
        } else {
            // TODO throw new
        }
    }

    private def gameHasOriginalFiles: Boolean = {
        val originalLayoutFile = new File(gamePath + RESOURCES_PATH + LAYOUT_FILENAME)
        val originalLevelsFile = new File(gamePath + RESOURCES_PATH + LEVELS_FILENAME)

        val messageDigest = MessageDigest.getInstance("SHA-256")

        String.format("%032x", new BigInteger(1, messageDigest.digest(Files.readAllBytes(originalLayoutFile.toPath)))) == HASH_LAYOUT &&
            String.format("%032x", new BigInteger(1, messageDigest.digest(Files.readAllBytes(originalLevelsFile.toPath)))) == HASH_LEVELS
    }
}
