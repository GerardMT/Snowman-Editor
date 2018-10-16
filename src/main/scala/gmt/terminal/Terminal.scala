package gmt.terminal

import java.io.{File, FileNotFoundException}

import gmt.main.Settings.SettingsParseException
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.snowman.encoder.DecodingData
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.level.{Level, MutableLevel}
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.planner.{SnowmanSolver, SnowmanSolverResult}
import gmt.util.Files

object Main {

    def main(args: Array[String]): Unit = {

    }

}

class Terminal ls{

    def main(args: Array[String]): Unit = {
        try {
            args.toList match {
                case List(settingsPath, "gui") =>
                    gmt.gui.Main(new File(settingsPath))

                case List("smt-basic", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations) =>
                    openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, EncoderEnum.BASIC)

                case List("smt-cheating", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations) =>
                    openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, EncoderEnum.CHEATING)

                case List("smt-reachability", levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations) =>
                    openLevelSolveSMT(levelPath, resultsPath, startTimeStepsStr, maxTimeSteps, threads, invaraintBallSizes, invariantBallLocations, EncoderEnum.REACHABILITY)

                case List("adl", levelPath, problemPath) =>
                    openLevelGeneratePDDLProblem(levelPath, problemPath, EncoderPDDL.encodeAdl)

                case List("adl-grounded", levelPath, problemPath) =>
                    openLevelGeneratePDDLProblem(levelPath, problemPath, EncoderPDDL.encodeObjectFluents)

                case List("object-fluents", levelPath, domainPath, problemPath) =>
                    openLevelGeneratePDDLDomainProblem(levelPath, domainPath, problemPath, EncoderPDDL.encodeAdlGrounded)

                case List("--help") | List("-h") =>
                    System.out.println("<snomwna editor> <settings path> <option>")
                    System.out.println("")
                    System.out.println("<option>:")
                    System.out.println("")
                    System.out.println("    smt-basic <level path> <result path> <start time steps (<int> | auto)> <max time steps> <threads> <invariant ball sizes (true | false)> <invariant ball locations (true | false)>")
                    System.out.println("    smt-cheating <level path> <result path> <start time steps (<int> | auto)> <max time steps> <threads> <invariant ball sizes (true | false)> <invariant ball locations (true | false)>")
                    System.out.println("    smt-reachability <level path> <result path> <start time steps (<int> | auto)> <max time steps> <threads> <invariant ball sizes (true | false)> <invariant ball locations (true | false)>")
                    System.out.println("    adl <level path> <save problem path>")
                    System.out.println("    adl-grounded <level path> <save domain path> <save problem path>")
                    System.out.println("    object-fluents <level path> <save problem path>")

                case List(_*) =>
                    System.out.println("Error parsing the arguments. -h for help.")
            }
        } catch {
            case SettingsParseException(message) =>
                System.out.println("Settings error: " + message)
                System.exit(0)
            case e : FileNotFoundException =>
                System.out.println()
                System.exit(0)
        }
    }

    private def openLevelSolveSMT(levelPath: String, resultsPath: String, startTimeStepsStr: String, maxTimeSteps: String, threads: String, invaraintBallSizes: String, invariantBallLocations: String, encoderEnum: EncoderEnum.Value): Unit = {
        val startTimeSteps = startTimeStepsStr match {
            case "auto" =>
                None
            case _ @ other =>
                Some(other.toInt)
        }

        val level = MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel

        val plannerOptions = PlannerOptions(startTimeSteps, maxTimeSteps.toInt, threads.toInt)
        val encoderOptions = EncoderOptions(invaraintBallSizes.toBoolean, invariantBallLocations.toBoolean)
        val result = SnowmanSolver.solveSMTYics2(settings.solverPath.get, level, encoderEnum, encoderOptions, plannerOptions, Terminal.showSolverUpdate)

        Terminal.showResult(result)

        result.result match {
            case Some(r) =>
                val resultString = StringBuilder.newBuilder
                resultString.append("solved=" + result.solved + "\n")
                resultString.append("solvingTime=" + result.milliseconds + "\n")
                resultString.append("actions=" + r.actions.head.toString + r.actions.map(f => "|" + f).mkString)

                Files.saveTextFile(new File(resultsPath), resultString.toString())
            case None =>
        }
    }

    private def openLevelGeneratePDDLProblem(levelPath: String, problemPath: String, generator : Level => String): Unit = {
        Files.saveTextFile(new File(problemPath), generator(MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel))
    }

    private def openLevelGeneratePDDLDomainProblem(levelPath: String, domainPath: String, problemPath: String, generator : Level => (String, String)): Unit = {
        val (domain, problem) = generator(MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel)

        Files.saveTextFile(new File(domainPath), domain)
        Files.saveTextFile(new File(problemPath), problem)
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

case class Terminal {

}