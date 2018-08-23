package gmt.mod

import java.io.{BufferedReader, File, FileReader, FileWriter}
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest

import gmt.mod.Game.RestoreException
import gmt.snowman.level.Level
import gmt.ui.Settings

import scala.collection.mutable.ListBuffer
import scala.sys.process._

object Game {

    case class RestoreException(message: String) extends Exception(message)
}

class Game(settings: Settings) {

    private val HASH_LAYOUT = "93685a29f53bb8f32fb03eee8a6a38d1d430cc9b46e888d82e1c9d34cc95e96"
    private val HASH_LEVELS = "634634d9186803b507887f94b6a81b1d95336daed331d84f62463d5bb4464b79"

    private val BACKUP_DIRECTORY = "./game_backup/"

    private val LAYOUT_FILENAME = "layout.txt"
    private val LEVELS_FILENAME = "levels.txt"
    private val PROGRESS_FILENAME = "progress.json"

    private val STEAMAPPID_FILENAME = "steam_appid.txt"

    private val RESOURCES_PATH = "resources/"

    private val LINUX_EXECUTABLE = "a-good-snowman.sh"
    private val WINDOWS_EXECUTABLE = "Snowman.exe"

    def levelsNames: List[String] =  {
        val names = ListBuffer.empty[String]

        val backupLevelsFile = new File(BACKUP_DIRECTORY  + LEVELS_FILENAME)

        val levelsFile = if (backupLevelsFile.exists()) {
            backupLevelsFile
        } else if (gameHasOriginalFiles) {
            new File(settings.gamePath.get + RESOURCES_PATH + LEVELS_FILENAME)
        } else {
            return List()
        }

        val br = new BufferedReader(new FileReader(levelsFile))
        try {
            var line = br.readLine
            if (line != null) {
                names.append(line)

                var lastEnter = false

                line = br.readLine()
                while (line != null) {
                    if (lastEnter) {
                        names.append(line)
                        lastEnter = false
                    } else if (line == "") {
                        lastEnter = true
                    }
                    line = br.readLine()
                }
            }
        } finally {
            if (br != null) {
                br.close()
            }
        }

        names.toList
    }

    def getLevel(name: String): String= {
        var found = false

        val backupLevelsFile = new File(BACKUP_DIRECTORY + LEVELS_FILENAME)

        val levelsFile = if (backupLevelsFile.exists()) {
            backupLevelsFile
        } else if (gameHasOriginalFiles) {
            new File(settings.gamePath.get + RESOURCES_PATH + LEVELS_FILENAME)
        } else {
            return ""
        }

        val br = new BufferedReader(new FileReader(levelsFile))
        try {
            var line = br.readLine
            if (line != null) {
                var lastEnter = false

                if (line == name) {
                    found = true
                } else {
                    line = br.readLine()
                }

                while (!found && line != null) {
                    if (lastEnter) {
                        if (line == name) {
                            found = true
                        } else {
                            lastEnter = false
                        }
                    } else if (line == "") {
                        lastEnter = true
                    }
                    if (!found) {
                        line = br.readLine()
                    }
                }
            }

            br.readLine()
            line = br.readLine()
            if (line.length == 1) {
                line = br.readLine()
            }

            var lastEnter = false

            val levelString = new StringBuilder()
            while (line != null && !lastEnter) {
                levelString.append(line)

                line = br.readLine()
                lastEnter = line == ""

                if (line != null && !lastEnter) {
                    levelString.append("\n")
                }
            }

            levelString.toString
        } finally {
            if (br != null) {
                br.close()
            }
        }
    }

    def loadAndRun(level: Level): Unit = {
        val steamappiddFile = new File(settings.gamePath.get + STEAMAPPID_FILENAME)
        if (!steamappiddFile.exists()) {
            val fileWriter = new FileWriter(steamappiddFile)
            fileWriter.write("316610")
            fileWriter.close()
        }

        val layoutFile = new File(settings.gamePath.get + RESOURCES_PATH + LAYOUT_FILENAME)
        val levelsFile = new File(settings.gamePath.get + RESOURCES_PATH + LEVELS_FILENAME)
        val progressFile = new File(settings.savePath.get + PROGRESS_FILENAME)

        layoutFile.delete()
        levelsFile.delete()
        progressFile.delete()

        val layoutFileWriter = new FileWriter(layoutFile)
        layoutFileWriter.write("LAYOUT VERSION 1\n")
        layoutFileWriter.write("\n")
        layoutFileWriter.write("0,0\n")
        layoutFileWriter.write("\n")
        layoutFileWriter.close()

        val levelsFileWriter = new FileWriter(levelsFile)
        levelsFileWriter.write("Custom")
        for (i <- 0 until level.snowmans - 1) {
            levelsFileWriter.write(", Custom")
        }
        levelsFileWriter.write("\n")
        levelsFileWriter.write("straw_hat buttons")
        for (i <- 0 until level.snowmans - 1) {
            levelsFileWriter.write(", straw_hat buttons")
        }
        levelsFileWriter.write("\n")
        levelsFileWriter.write("1\n")
        levelsFileWriter.write(level.toString)
        levelsFileWriter.close()

        val y = level.height - level.character.c.y - 1

        val progressFileWriter = new FileWriter(progressFile)
        progressFileWriter.write("{")
        progressFileWriter.write("\t\"level\": 0,\n")
        progressFileWriter.write("\t\"version\": 6,\n")
        progressFileWriter.write("\t\"hugged\": [],\n")
        progressFileWriter.write("\t\"sitDirection\": null,\n")
        progressFileWriter.write("\t\"hasPlayed\": true,")
        progressFileWriter.write("\t\"timestamp\": 1527523913000,\n")
        progressFileWriter.write("\t\"levels\": {\n")
        progressFileWriter.write("\t\t\"0\": {\n")
        progressFileWriter.write("\t\t\t\"hasCompletedWithoutReentering\": false,\n")
        progressFileWriter.write("\t\t\t\"resetX\": " + level.character.c.x + ",\n")
        progressFileWriter.write("\t\t\t\"entities\": {\n")
        progressFileWriter.write("\t\t\t\t\"1\": {\n")
        progressFileWriter.write("\t\t\t\t\t\"x\": " + level.character.c.x + ",\n")
        progressFileWriter.write("\t\t\t\t\t\"type\": \"player\",\n")
        progressFileWriter.write("\t\t\t\t\t\"y\": " + y + "\n")
        progressFileWriter.write("\t\t\t\t}\n")
        progressFileWriter.write("\t\t\t},\n")
        progressFileWriter.write("\t\t\t\"hasReentered\": false,\n")
        progressFileWriter.write("\t\t\t\"resetY\": " + y + ",\n")
        progressFileWriter.write("\t\t\t\"snowmen\": [],\n")
        progressFileWriter.write("\t\t\t\"completed\": false\n")
        progressFileWriter.write("\t\t}\n")
        progressFileWriter.write("\t}\n")
        progressFileWriter.write("}")
        progressFileWriter.close()

        run()
    }

    def run(): Unit = {
        if (System.getProperty("os.name").startsWith("Windows")) {
            Seq(settings.gamePath.get + WINDOWS_EXECUTABLE).run()
        } else {
            Seq(settings.gamePath.get + LINUX_EXECUTABLE).run()
        }
    }

    def restore(): Unit = {
        val backupLayoutFile = new File(BACKUP_DIRECTORY + LAYOUT_FILENAME)
        val backupLevelsFile = new File(BACKUP_DIRECTORY + LEVELS_FILENAME)
        val backupProgressFile = new File(BACKUP_DIRECTORY + PROGRESS_FILENAME)

        if (backupLayoutFile.exists && backupLevelsFile.exists) {
            val customLayoutFile = new File(settings.gamePath.get + RESOURCES_PATH + LAYOUT_FILENAME)
            val customLevelsFile = new File(settings.gamePath.get + RESOURCES_PATH + LEVELS_FILENAME)
            val customProgressFile = new File(settings.savePath.get + PROGRESS_FILENAME)

            // Delete custom files
            customLayoutFile.delete()
            customLevelsFile.delete()
            customProgressFile.delete()

            // Copy files
            Files.copy(backupLayoutFile.toPath, customLayoutFile.toPath)
            Files.copy(backupLevelsFile.toPath, customLevelsFile.toPath)
            Files.copy(backupProgressFile.toPath, customProgressFile.toPath)
        } else {
            throw RestoreException("Original files don't exist")
        }
    }

    def backup(): Unit = {
        val originalLayoutFile = new File(settings.gamePath.get + RESOURCES_PATH + LAYOUT_FILENAME)
        val originalLevelsFile = new File(settings.gamePath.get + RESOURCES_PATH + LEVELS_FILENAME)
        val originalProgressFile = new File(settings.savePath.get + PROGRESS_FILENAME)

        val backupLayoutFile = new File(BACKUP_DIRECTORY + LAYOUT_FILENAME)
        val backupLevelsFile = new File(BACKUP_DIRECTORY + LEVELS_FILENAME)
        val backupProgressFile = new File(BACKUP_DIRECTORY + PROGRESS_FILENAME)

        val backupDirectory = new File(BACKUP_DIRECTORY)

        // Create backup folder
        if (!backupDirectory.exists) {
            backupDirectory.mkdir()
        } else {
            backupLayoutFile.delete()
            backupLevelsFile.delete()
            backupProgressFile.delete()
        }

        // Copy files
        Files.copy(originalLayoutFile.toPath, backupLayoutFile.toPath)
        Files.copy(originalLevelsFile.toPath, backupLevelsFile.toPath)
        Files.copy(originalProgressFile.toPath, backupProgressFile.toPath)
    }

    def gameHasOriginalFiles: Boolean = {
        val originalLayoutFile = new File(settings.gamePath.get + RESOURCES_PATH + LAYOUT_FILENAME)
        val originalLevelsFile = new File(settings.gamePath.get + RESOURCES_PATH + LEVELS_FILENAME)

        val messageDigest = MessageDigest.getInstance("SHA-256")

        val hashLayout = String.format("%032x", new BigInteger(1, messageDigest.digest(Files.readAllBytes(originalLayoutFile.toPath))))
        messageDigest.reset()
        val hashLevels = String.format("%032x", new BigInteger(1, messageDigest.digest(Files.readAllBytes(originalLevelsFile.toPath))))

        hashLayout == HASH_LAYOUT && hashLevels == HASH_LEVELS
    }
}
