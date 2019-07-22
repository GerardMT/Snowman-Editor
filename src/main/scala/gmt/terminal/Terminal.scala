package gmt.terminal

import java.io.{File, FileNotFoundException}

import gmt.main.Settings
import gmt.main.Settings.SettingsParseException
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.level.{Level, MutableLevel}
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.planner.{SnowmanSolver, SnowmanSolverResult}
import gmt.util.Files

object Terminal {

    def main(args: Array[String]): Unit = {
        new Terminal().parseArguments(args.toList)
    }

    def showSolverUpdate(plannerUpdate: PlannerUpdate): Unit = {
        println("Timesteps: " + plannerUpdate.timeSteps + " sat: " + plannerUpdate.sat + " Time: " + plannerUpdate.milliseconds / 1000.0 + " TotalTime: " + plannerUpdate.totalMilliseconds / 1000.0 + " (s)")
    }

    def showResult(snowmanSolverResult: SnowmanSolverResult): Unit = {
        println("Solved: " + snowmanSolverResult.solved)
        println("Time (s): " + snowmanSolverResult.milliseconds / 1000.0)

        snowmanSolverResult.result match {
            case Some(r) =>
                println("Valid: " + snowmanSolverResult.valid)
                println("Actions: " + r.actions.size)
                println("    " + r.actions.mkString)
                println("Ball references from initial state:")
                r.balls.zipWithIndex.foreach(f => println("    Ball (" + f._2 + "): " + f._1))
                println("Ball Actions: " + r.actionsBall.size)
                println("    " + r.actionsBall.map(f => f.toStringRef).mkString)

            case None =>
        }
    }
}

class Terminal {

    def parseArguments(arguments: List[String]): Unit = {
        arguments match {
            case List("--help") | List("-h") =>
                System.out.print("""<snowman_editor> <settings_path> <option>
                                   |
                                   |<option>:
                                   |    gui
                                   |
                                   |    smt-basic <level_path> <results_path> <start_time_steps (<int> | auto)>
                                   |        <max_time_steps> <threads> <invariant_ball_sizes (true | false)>
                                   |        <invariant_ball_locations (true | false)> <invariant_wall_u (true | false)>
                                   |
                                   |    smt-cheating <level_path> <results_path> <start_time_steps (<int> | auto)>
                                   |        <max_time_steps> <threads> <invariant_ball_sizes (true | false)>
                                   |        <invariant_ball_locations (true | false)> <invariant_wall_u (true | false)>
                                   |
                                   |    smt-reachability <level_path> <results_path> <start_time_steps (<int> | auto)>
                                   |        <max_time_steps> <threads> <invariant_ball_sizes (true | false)>
                                   |        <invariant_ball_locations (true | false)> <invariant_wall_u (true | false)>
                                   |
                                   |    adl <level_path> <save_directory>
                                   |
                                   |    adl-grounded <level_path> <save_directory>
                                   |
                                   |    object-fluents <level_path> <save_directory>""".stripMargin)
            case List(settingsPath, _*) =>
                new TerminalSettings(new File(settingsPath)).parseArguments(arguments.tail)

            case _ =>
                wrongArgumentsError()
        }
    }

    private def wrongArgumentsError(): Unit = {
        System.out.println("Error parsing the arguments. -h for help.")
    }

    class TerminalSettings(private val settingsFile: File) {

        private val settings: Settings = try {
            Settings.load(settingsFile)
        } catch {
            case SettingsParseException(message) =>
                System.out.println("Settings error: " + message)
                sys.exit()
            case _: FileNotFoundException =>
                System.out.println("Settings file not found. File created at: " + settingsFile)
                sys.exit()
        }

        def parseArguments(arguments: List[String]): Unit = {
            try {
                arguments match {
                    case List("gui") =>
                        new gmt.gui.GUI(settingsFile)

                    case List("smt-basic", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, invaraintBallSizes, invariantBallLocations, invariantWallU) =>
                        openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, invaraintBallSizes, invariantBallLocations, invariantWallU, EncoderEnum.BASIC)

                    case List("smt-cheating", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, invaraintBallSizes, invariantBallLocations, invariantWallU) =>
                        openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, invaraintBallSizes, invariantBallLocations, invariantWallU, EncoderEnum.CHEATING)

                    case List("smt-reachability", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, invaraintBallSizes, invariantBallLocations, invariantWallU) =>
                        openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, invaraintBallSizes, invariantBallLocations, invariantWallU, EncoderEnum.REACHABILITY)

                    case List("adl-grounded", levelPath, directoryPath) =>
                        openLevelGeneratePDDL(levelPath, directoryPath, EncoderPDDL.encodeAdlGrounded)

                    case List("object-fluents", levelPath, directoryPath) =>
                        System.out.println("Warning: Experimental")
                        openLevelGeneratePDDL(levelPath, directoryPath, EncoderPDDL.encodeObjectFluents)

                    case List(_*) =>
                        wrongArgumentsError()
                }
            } catch {
                case SettingsParseException(message) =>
                    System.out.println("Settings error: " + message)
                    System.exit(0)
                case _: FileNotFoundException =>
                    System.out.println()
                    System.exit(0)
            }
        }

        private def openLevelSolveSMT(levelPath: String, resultsPath: String, startTimeStepsStr: String, maxTimeSteps: String, invaraintBallSizes: String, invariantBallLocations: String, invariantWallU: String, encoderEnum: EncoderEnum.Value): Unit = {
            val startTimeSteps = startTimeStepsStr match {
                case "auto" =>
                    None
                case _@other =>
                    Some(other.toInt)
            }

            val level = MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel

            val plannerOptions = PlannerOptions(startTimeSteps, maxTimeSteps.toInt)
            val encoderOptions = EncoderOptions(invaraintBallSizes.toBoolean, invariantBallLocations.toBoolean, invariantWallU.toBoolean)
            val result = SnowmanSolver.solveSMTYics2(settings.solverPath.get, level, encoderEnum, encoderOptions, plannerOptions, Terminal.showSolverUpdate)

            Terminal.showResult(result)

            result.result match {
                case Some(r) =>
                    val resultString = StringBuilder.newBuilder
                    resultString.append("solved=" + result.solved + "\n")
                    resultString.append("solvingTime=" + result.milliseconds + "\n")
                    resultString.append("actions=" + r.actions.head.toString + r.actions.tail.map(f => "|" + f).mkString  + "\n")
                    resultString.append("nActions=" + r.actions.length  + "\n")
                    resultString.append("ballActions=" + r.actionsBall.head.toString + r.actionsBall.tail.map(f => "|" + f).mkString + "\n")
                    resultString.append("nBallActions=" + r.actionsBall.length)

                    Files.saveTextFile(new File(resultsPath), resultString.toString())
                case None =>
            }
        }

        private def openLevelGeneratePDDL(levelPath: String, directoryPath: String, generator: Level => (String, String)): Unit = {
            val (domain, problem) = generator(MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel)

            val modifiedDirectory = Files.removeSlash(directoryPath)

            Files.saveTextFile(new File(modifiedDirectory + "/domain.pddl"), domain)
            Files.saveTextFile(new File(modifiedDirectory + "/problem.pddl"), problem)
        }
    }
}