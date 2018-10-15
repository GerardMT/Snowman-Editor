package gmt.snowman.planner

import gmt.planner.planner.Planner
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.encoder._
import gmt.snowman.level.Level
import gmt.snowman.solver.Yices2Solver
import gmt.snowman.translator.SMTLib2
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
}
