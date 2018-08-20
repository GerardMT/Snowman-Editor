package gmt.ui

import java.io.{File, FileNotFoundException, FileWriter}

import scala.io.Source

object Settings {

    case class SettingsParseException(message: String) extends Exception(message)

    private val KEY_GAME_PATH = "game_path"
    private val KEY_SAVE_PATH = "save_path"
    private val KEY_SOLVER_PATH = "solver_path"

    private val SEPARATOR = '='

    def default: Settings = Settings(None, None, Some("yices-smt2"))

    def read(file: File): Settings = {
        val configFile = try {
            Source.fromFile(file.getPath)
        } catch {
            case _: Exception =>
                throw new FileNotFoundException("File \"" + file.getPath + "\" not found")
        }

        val lines = configFile.getLines.filter(f => f.head != ';').toList
        lines.zipWithIndex.foreach(f => {
            if (!f._1.contains(SEPARATOR)) {
                throw SettingsParseException("No " + SEPARATOR  + " at line " + f._2)

            }
        })

        val settingsSplit = lines.toList.map(f => f.split(SEPARATOR))

        settingsSplit.zipWithIndex.foreach(f => {
            if (f._1.length > 2) {
                throw SettingsParseException("Multiple " + SEPARATOR + " at line " + f._2)
            }
        })

        val settingsMap = settingsSplit.map(f => {
            if (f.length == 2) {
                (f(0), f(1))
            } else {
                (f(0), "")
            }
        }).toMap

        val gamePath = settingsMap.get(KEY_GAME_PATH) match {
            case Some("") =>
                None
            case Some(s) =>
                Some(s)
            case None =>
                throw SettingsParseException("Key \"" + KEY_GAME_PATH + "\" not found")
        }

        val savePath = settingsMap.get(KEY_SAVE_PATH) match {
            case Some("") =>
                None
            case Some(s) =>
                Some(s)
            case None =>
                throw SettingsParseException("Key \"" + KEY_SAVE_PATH + "\" not found")
        }

        val solverPath = settingsMap.get(KEY_SOLVER_PATH) match {
            case Some("") =>
                None
            case Some(s) =>
                Some(s)
            case None =>
                throw SettingsParseException("Key \"" + KEY_SOLVER_PATH + "\" not found")
        }

        new Settings(gamePath, savePath, solverPath)
    }
}


case class Settings private(gamePath: Option[String],
                            savePath: Option[String],
                            solverPath: Option[String]) {

    def save(file: File): Unit = {
        val fileWriter = new FileWriter(file)

        val saveGamePath = gamePath match {
            case Some(s) =>
                s
            case None =>
                ""
        }

        val saveSavePath  = savePath match {
            case Some(s) =>
                s
            case None =>
                ""
        }

        val saveSolverPath = solverPath match {
            case Some(s) =>
                s
            case None =>
                ""
        }

        fileWriter.append(Settings.KEY_GAME_PATH + Settings.SEPARATOR + saveGamePath + "\n")
        fileWriter.append(Settings.KEY_SAVE_PATH + Settings.SEPARATOR + saveSavePath + "\n")
        fileWriter.append(Settings.KEY_SOLVER_PATH + Settings.SEPARATOR + saveSolverPath  + "\n")

        fileWriter.close()
    }
}