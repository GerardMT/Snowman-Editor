package gmt.snowman.solver

import java.io.{BufferedWriter, File, FileWriter}

import gmt.planner.planner.Planner
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.planner.solver.Yices2Solver
import gmt.planner.translator.SMTLib2
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.encoder._
import gmt.snowman.level.{Level, MutableLevel}
import gmt.snowman.util.Files
import gmt.snowman.validator.Validator

object SnowmanSolver {

    abstract class AutoSolveUpdate(val level: String)
    case class AutoSolveUpdateProgress(override val level: String, plannerUpdate: PlannerUpdate[DecodingData]) extends AutoSolveUpdate(level)
    case class AutoSolveUpdateFinal(override val level: String, snowmanSolver: SnowmanSolverResult) extends AutoSolveUpdate(level)

    case class GenerateOptions(timeSteps: Int)

    def encodeSMTLIB2(level: Level, encoderEnum: EncoderEnum.Value, encoderOptions: EncoderBase.EncoderOptions, generateOptions: GenerateOptions): String = {
        SMTLib2.translate(EncoderBase(encoderEnum, level, encoderOptions).encode(generateOptions.timeSteps).encoding)
    }

    def solveSMTYics2(solverPath: String, level: Level, encoderEnum: EncoderEnum.Value, encoderOptions: EncoderOptions, plannerOptions: PlannerOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        val result = new Planner(plannerOptions).solve(EncoderBase(encoderEnum, level, encoderOptions), SMTLib2, new Yices2Solver(solverPath), updateFunction)

        result.result match {
            case Some(r) =>
                val (valid, _ ) = Validator.validate(level, r.actions)
                SnowmanSolverResult(result.sat, valid, result.milliseconds, Some(r))
            case None =>
                SnowmanSolverResult(result.sat, valid = false, result.milliseconds, None)
        }
    }

    def autoSolveSMTYices2(solverPath: String, levelsPath: String, outPath: String, encoderEnum: EncoderEnum.Value, encoderOptions: EncoderOptions, plannerOptions: PlannerOptions, updateFunction: AutoSolveUpdate => Unit): Unit = {
        val levelsDirectory = new File(levelsPath)

        val resultsFile = new File(outPath + "results.cvs")

        val resultsWriter = new BufferedWriter(new FileWriter(resultsFile, true))
        resultsWriter.write("name,solved,timesteps,time,valid,actions,actionsballs\n")
        resultsWriter.close()

        for ((file, level) <- levelsDirectory.listFiles().map(f => (f, MutableLevel.load(Files.openTextFile(f)).toLevel))) {
            val levelDirectory = outPath + file.getName
            new File(levelDirectory).mkdir()
            val levelLogFile = new File(levelDirectory + "/out.log")
            levelLogFile.createNewFile()

            val updateFunction: PlannerUpdate[DecodingData] => Unit = f => {
                val writer = new BufferedWriter(new FileWriter(levelLogFile, true))
                writer.write("update timesteps=" + f.timeStepResult.timeSteps + " sat=" + f.timeStepResult.sat + " time=" + f.timeStepResult.milliseconds + " totaltime=" + f.totalMilliseconds + "\n")
                writer.close()
            }

            val result = new Planner(plannerOptions).solve(EncoderBase(encoderEnum, level, encoderOptions), SMTLib2, new Yices2Solver(solverPath), updateFunction)

            val (writeFinal, resultString) = result.result match {
                case Some(r) =>
                    val (valid, _ ) = Validator.validate(level, r.actions)

                    val union: (String, String) => String = (s1, s2) => s1 + "|" + s2

                    val actionsString = r.actions.map(f => f.toString).fold("")(union)
                    val actionsBallsString = r.actionsBall.map(f => f.toString).fold("")(union)

                    ("final solved=" + result.sat+ "timesteps=" + result.timeSteps + " time=" + result.milliseconds + " valid=" + valid + " actions=" + actionsString + " actionsballs=" + actionsBallsString + "\n",
                        file.getName + "," + result.sat + "," + result.timeSteps + " ," + result.milliseconds + "," + valid + "," + r.actions.size + "," + r.actionsBall.size + "\n")
                case None =>
                    SnowmanSolverResult(result.sat, valid = false, result.milliseconds, None)
                    ("final solved=" + result.sat+ "timesteps=" + result.timeSteps + " time=" + result.milliseconds + "\n",
                        file.getName + "," + result.sat + "," + result.timeSteps + " ," + result.milliseconds + "\n")
            }

            val resultsWriter = new BufferedWriter(new FileWriter(resultsFile, true))
            resultsWriter.write(resultString)
            resultsWriter.close()

            val writer = new BufferedWriter(new FileWriter(levelLogFile, true))
            writer.write(writeFinal)
            writer.close()
        }
    }
}
