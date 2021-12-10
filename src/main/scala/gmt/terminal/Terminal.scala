package gmt.terminal

import java.io.{File, FileNotFoundException, PrintWriter}

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
        println("Timesteps: " + plannerUpdate.timeSteps + " sat: " + plannerUpdate.sat + " Time: " + plannerUpdate.nanoseconds / 1e9 + " TotalTime: " + plannerUpdate.totalNanoseconds / 1e9 + " (s)")
    }

    def showResult(snowmanSolverResult: SnowmanSolverResult): Unit = {
        println("Solved: " + snowmanSolverResult.solved)
        println("Time (s): " + snowmanSolverResult.nanoseconds / 1e9)

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
                System.out.print("""Solve, edit and play A Good Snowman Is Hard To Build levels.
                                   |
                                   |The editor uses Yices 2 as a SMT solver. To play the levels the tool modifies the
                                   |original game files. Both, the Yices 2 and game path, can be set in the
                                   |snowman_editor.config file.
                                   |
                                   |Usage: java -jar snowman_editor.jar -h | --help
                                   |
                                   |              Show this message.
                                   |
                                   |Usage: java -jar snowman_editor.jar init
                                   |
                                   |               First run. Generate a new settings file.
                                   |
                                   |Usage: java -jar snowman_editor.jar <settings_path> <mode>
                                   |
                                   |<settings_path>:
                                   |
                                   |                Path to the generated settings file
                                   |<mode>:
                                   |
                                   |    gui
                                   |
                                   |                Open the user graphical user interface
                                   |
                                   |    smt-basic <level_path> <results_path> <start_time_steps (<int> | auto)>
                                   |        <max_time_steps> <invariant_ball_sizes (true | false)>
                                   |        <invariant_ball_locations (true | false)> <invariant_wall_u (true | false)>
                                   |
                                   |                Solve a given level. Minimizes the character + ball movements.
                                   |
                                   |    smt-cheating <level_path> <results_path> <start_time_steps (<int> | auto)>
                                   |        <max_time_steps> <invariant_ball_sizes (true | false)>
                                   |        <invariant_ball_locations (true | false)> <invariant_wall_u (true | false)>
                                   |
                                   |                Solve a given level without taking into consideration the character
                                   |                movements. It can return invalid plans due to character
                                   |                teletransportation.
                                   |
                                   |    smt-reachability <level_path> <results_path> <start_time_steps (<int> | auto)>
                                   |        <max_time_steps> <invariant_ball_sizes (true | false)>
                                   |        <invariant_ball_locations (true | false)> <invariant_wall_u (true | false)>
                                   |
                                   |               Solve a given level. Minimizes the ball movements guaranteeing the
                                   |               character movements to be valid.
                                   |
                                   |    basic-adl <level_path> <save_directory>
                                   |
                                   |               Generate a PDDL domain and problem files using the adl PDDL subset.
                                   |               Using basic encoding.
                                   |               PDDL requirements:
                                   |               - typing
                                   |               - negative-preconditions
                                   |               - equality
                                   |               - disjunctive-preconditions
                                   |               - conditional-effects
                                   |               - action-costs
                                   |
                                   |    basic-adl-grounded <level_path> <save_directory>
                                   |
                                   |               Generate a PDDL domain and problem files using the adl PDDL subset.
                                   |               Using basic encoding.
                                   |               The forall predicates have been grounded.
                                   |               PDDL requirments:
                                   |               - typing
                                   |               - negative-preconditions
                                   |               - equality
                                   |               - disjunctive-preconditions
                                   |               - conditional-effects
                                   |               - action-costs
                                   |
                                   |    cheating-adl <level_path> <save_directory>
                                   |
                                   |               Generate a PDDL domain and problem files using the adl PDDL subset.
                                   |               Using cheating encoding.
                                   |               PDDL requirements:
                                   |               - typing
                                   |               - negative-preconditions
                                   |               - equality
                                   |               - disjunctive-preconditions
                                   |               - conditional-effects
                                   |               - action-costs
                                   |""".stripMargin)
            case List("init") =>
                val settingsFile = new File("")
                if (settingsFile.exists()) {
                    System.out.println("Settings file already exists.")
                } else {
                    val p = new PrintWriter(settingsFile)
                    p.write("game_path=\n")
                    p.write("save_path=\n")
                    p.write("solver_path=")
                    p.close()

                    System.out.println("Settings file created.")
                }
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
                System.out.println("Settings file not found.")
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

                    case List("basic-adl", levelPath, directoryPath) =>
                        openLevelGeneratePDDL(levelPath, directoryPath, EncoderPDDL.encodeBasicAdl)

                    case List("basic-adl-grounded", levelPath, directoryPath) =>
                        openLevelGeneratePDDL(levelPath, directoryPath, EncoderPDDL.encodeBasicAdlGrounded)

                    case List("cheating-adl", levelPath, directoryPath) =>
                        openLevelGeneratePDDL(levelPath, directoryPath, EncoderPDDL.encodeCheatingAdl)

                    case List(_*) =>
                        wrongArgumentsError()
                }
            } catch {
                case SettingsParseException(message) =>
                    System.out.println("Settings error: " + message)
                    System.exit(0)
                case e: FileNotFoundException =>
                    System.out.println(e.getMessage)
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
                    resultString.append("solvingTime=" + result.nanoseconds + "\n")
                    resultString.append("actions=" + r.actions.head.toString + r.actions.tail.map(f => "|" + f).mkString  + "\n")
                    resultString.append("nActions=" + r.actions.length  + "\n")
                    resultString.append("ballActions=" + r.actionsBall.head.toString + r.actionsBall.tail.map(f => "|" + f).mkString + "\n")
                    resultString.append("nBallActions=" + r.actionsBall.length)

                    Files.saveTextFile(new File(resultsPath), resultString.toString())
                case None =>
            }
        }

        private def openLevelGeneratePDDL(levelPath: String, directoryPath: String, generator: Level => (String, String)): Unit = {
            val file = new File(levelPath)

            var name = file.getName
            name = name.substring(0, name.lastIndexOf('.'))

            val (domain, problem) = generator(MutableLevel.load(Files.openTextFile(file), Some(name)).toLevel)

            val modifiedDirectory = Files.removeSlash(directoryPath)

            Files.saveTextFile(new File(modifiedDirectory + "/domain.pddl"), domain)
            Files.saveTextFile(new File(modifiedDirectory + "/problem.pddl"), problem)
        }
    }
}