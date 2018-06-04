package snowman.ui

import java.io.{File, FileWriter}

import scala.io.Source

object Settings {

    private val KEY_GAME_PATH = "game_path"
    private val KEY_SAVE_PATH = "save_path"
    private val KEY_BACKUP_PATH = "backup_path"
    private val KEY_FIRST_RUN = "first_run"

    private val SEPARATOR = '='

    def read(file: File): Settings = {
        val configFile = try {
            Source.fromFile(file.getPath)
        } catch {
            case e: Exception =>
                throw SettingsParseException("File \"" + file.getPath + "\" not found")
        }

        val settingsSplit = configFile.getLines.toList.map(f => f.split(SEPARATOR))

        settingsSplit.zipWithIndex.foreach(f => if (f._1.length != 2) throw SettingsParseException("Multiple " + SEPARATOR + " at line " + f._2))

        val settingsMap = settingsSplit.map(f => (f(0), f(1))).toMap

        val gamePath = settingsMap.get(KEY_GAME_PATH) match {
            case Some(s) =>
                s
            case None =>
                throw SettingsParseException("Key \"" + KEY_GAME_PATH + "\" not found")
        }

        val savePath = settingsMap.get(KEY_SAVE_PATH) match {
            case Some(s) =>
                s
            case None =>
                throw SettingsParseException("Key \"" + KEY_SAVE_PATH + "\" not found")
        }

        val backupPath = settingsMap.get(KEY_BACKUP_PATH) match {
            case Some(s) =>
                s
            case None =>
                throw SettingsParseException("Key \"" + KEY_BACKUP_PATH + "\" not found")
        }

        val firstRun = settingsMap.get(KEY_FIRST_RUN) match {
            case Some(s) =>
                s.toBoolean
            case None =>
                throw SettingsParseException("Key \"" + KEY_FIRST_RUN + "\" not found")
        }

        new Settings(gamePath, savePath, backupPath, firstRun)
    }
}

class Settings private (val gamePath: String, val savePath: String, val backupPath: String, var firstRun: Boolean) {

    def save(file: File): Unit = {
        val fileWriter = new FileWriter(file)
        fileWriter.append(Settings.KEY_GAME_PATH + Settings.SEPARATOR + gamePath + "\n")
        fileWriter.append(Settings.KEY_SAVE_PATH + Settings.SEPARATOR + savePath + "\n")
        fileWriter.append(Settings.KEY_BACKUP_PATH + Settings.SEPARATOR + backupPath + "\n")
        fileWriter.append(Settings.KEY_FIRST_RUN + Settings.SEPARATOR + firstRun)
        fileWriter.close()
    }
}