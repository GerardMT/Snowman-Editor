package gmt.terminal

import java.io.{File, FileNotFoundException}

import gmt.main.Settings
import gmt.main.Settings.SettingsParseException
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.snowman.encoder.DecodingData
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.level.{Level, MutableLevel}
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.planner.{SnowmanSolver, SnowmanSolverResult}
import gmt.util.Files

object Terminal {

    def main(args: Array[String]): Unit = {
        new Terminal().parseArguments(args.toList)
    }

    def showSolverUpdate(plannerUpdate: PlannerUpdate[DecodingData]): Unit = {
        println("Timesteps: " + plannerUpdate.timeStepResult.timeSteps + " sat: " + plannerUpdate.timeStepResult.sat + " Time: " + plannerUpdate.timeStepResult.milliseconds + " TotalTime: " + plannerUpdate.totalMilliseconds + " (ms)")
    }

    def showResult(snowmanSolverResult: SnowmanSolverResult): Unit = {
        println("Solved: " + snowmanSolverResult.solved)

        snowmanSolverResult.result match {
            case Some(r) =>
                println("Valid: " + snowmanSolverResult.valid)
                println("Actions: " + r.actions.size)
                r.actions.foreach(f => println("    " + f.toString))
                println("Ball references from initial state:")
                r.balls.zipWithIndex.foreach(f => println("    Ball (" + f._2 + "): " + f._1))
                println("Ball Actions: " + r.actionsBall.size)
                r.actionsBall   .foreach(f => println("    " + f.toString))

            case None =>
        }
    }
}

class Terminal {

    def parseArguments(arguments: List[String]): Unit = {
        arguments match {
            case List("--help") | List("-h") =>
                System.out.print("""<snowman editor> <settings path> <option>
                                   |
                                   |<option>:
                                   |
                                   |    smt-basic <level path> <result path> <start time steps (<int> | auto)> <max time steps> <threads> <invariant ball sizes (true | false)> <invariant ball locations (true | false)> <invariant distances (true | false)>
                                   |    smt-cheating <level path> <result path> <start time steps (<int> | auto)> <max time steps> <threads> <invariant ball sizes (true | false)> <invariant ball locations (true | false)> <invariant distances (true | false)>
                                   |    smt-reachability <level path> <result path> <start time steps (<int> | auto)> <max time steps> <threads> <invariant ball sizes (true | false)> <invariant ball locations (true | false)> <invariant distances (true | false)>
                                   |    adl <level path> <save problem path>
                                   |    adl-grounded <level path> <save domain path> <save problem path>
                                   |    object-fluents <level path> <save problem path>""".stripMargin)
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

                    case List("smt-basic", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, invariantDistances) =>
                        openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, invariantDistances, EncoderEnum.BASIC)

                    case List("smt-cheating", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, invariantDistances) =>
                        openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, invariantDistances, EncoderEnum.CHEATING)

                    case List("smt-reachability", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, invariantDistances) =>
                        openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, invariantDistances, EncoderEnum.REACHABILITY)

                    case List("adl", levelPath, problemPath) =>
                        openLevelGeneratePDDLProblem(levelPath, problemPath, EncoderPDDL.encodeAdl)

                    case List("adl-grounded", levelPath, domainPath, problemPath) =>
                        openLevelGeneratePDDLDomainProblem(levelPath, domainPath, problemPath, EncoderPDDL.encodeAdlGrounded)

                    case List("object-fluents", levelPath, problemPath) =>
                        openLevelGeneratePDDLProblem(levelPath, problemPath, EncoderPDDL.encodeObjectFluents)

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

        private def openLevelSolveSMT(levelPath: String, resultsPath: String, startTimeStepsStr: String, maxTimeSteps: String, threads: String, invaraintBallSizes: String, invariantBallLocations: String, invariantDistances: String, encoderEnum: EncoderEnum.Value): Unit = {
            val startTimeSteps = startTimeStepsStr match {
                case "auto" =>
                    None
                case _@other =>
                    Some(other.toInt)
            }

            val level = MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel

            val plannerOptions = PlannerOptions(startTimeSteps, maxTimeSteps.toInt, threads.toInt)
            val encoderOptions = EncoderOptions(invaraintBallSizes.toBoolean, invariantBallLocations.toBoolean, invariantDistances.toBoolean)
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

        private def openLevelGeneratePDDLProblem(levelPath: String, problemPath: String, generator: Level => String): Unit = {
            Files.saveTextFile(new File(problemPath), generator(MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel))
        }

        private def openLevelGeneratePDDLDomainProblem(levelPath: String, domainPath: String, problemPath: String, generator: Level => (String, String)): Unit = {
            val (domain, problem) = generator(MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel)

            Files.saveTextFile(new File(domainPath), domain)
            Files.saveTextFile(new File(problemPath), problem)
        }
    }

}