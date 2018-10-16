package gmt.main

import java.io.{File, FileNotFoundException, FileWriter}

import gmt.main.Settings.{SettingsNoGamePathException, SettingsNoSavePathException, SettingsNoSolverPathException}
import gmt.util.OptionException

import scala.io.Source

object Settings {

    case class SettingsParseException(message: String) extends Exception(message)
    case class SettingsNoGamePathException() extends Exception
    case class SettingsNoSavePathException() extends Exception
    case class SettingsNoSolverPathException() extends Exception

    private val KEY_GAME_PATH = "game_path"
    private val KEY_SAVE_PATH = "save_path"
    private val KEY_SOLVER_PATH = "solver_path"

    private val SEPARATOR = '='

    def default: Settings = Settings(OptionException(None, new SettingsNoGamePathException), OptionException(None, new SettingsNoSavePathException), OptionException(Some("yices-smt2"), new SettingsNoSolverPathException))

    def load(file: File): Settings = {
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

        val settingsSplit = lines.map(f => f.split(SEPARATOR))

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

        new Settings(OptionException(gamePath, new SettingsNoGamePathException), OptionException(savePath, new SettingsNoSavePathException), OptionException(solverPath, new SettingsNoSolverPathException))
    }
}


case class Settings private(gamePath: OptionException[String, SettingsNoGamePathException],
                            savePath: OptionException[String, SettingsNoSavePathException],
                            solverPath: OptionException[String, SettingsNoSolverPathException]) {

    def save(file: File): Unit = {
        val fileWriter = new FileWriter(file)

        val saveGamePath = if (gamePath.isDefined) {
            gamePath.get
        } else {
            ""
        }

        val saveSavePath = if (savePath.isDefined) {
            savePath.get
        } else {
            ""
        }

        val saveSolverPath = if (solverPath.isDefined) {
            solverPath.get
        } else {
            ""
        }

        fileWriter.append(Settings.KEY_GAME_PATH + Settings.SEPARATOR + saveGamePath + "\n")
        fileWriter.append(Settings.KEY_SAVE_PATH + Settings.SEPARATOR + saveSavePath + "\n")
        fileWriter.append(Settings.KEY_SOLVER_PATH + Settings.SEPARATOR + saveSolverPath  + "\n")

        fileWriter.close()
    }
}