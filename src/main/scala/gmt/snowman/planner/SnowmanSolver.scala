package gmt.snowman.planner

import gmt.planner.planner.Planner
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.snowman.encoder.EncoderBase.{EncoderEnum, EncoderOptions}
import gmt.snowman.encoder._
import gmt.snowman.level.Level
import gmt.snowman.solver.Yices2
import gmt.snowman.translator.SMTLib2
import gmt.snowman.validator.Validator

object SnowmanSolver {

    case class GenerateOptions(timeSteps: Int)

    def generateSMTLIB2(level: Level, encoderEnum: EncoderEnum.Value, encoderOptions: EncoderBase.EncoderOptions, generateOptions: GenerateOptions): String = {
        SMTLib2.translate(Planner.generate(generateOptions.timeSteps, EncoderBase(encoderEnum, level, encoderOptions)))
    }

    def solveSMTYics2(solverPath: String, level: Level, encoderEnum: EncoderEnum.Value, encoderOptions: EncoderOptions, plannerOptions: PlannerOptions, updateFunction: PlannerUpdate => Unit): SnowmanSolverResult = {
        val result = Planner.solve(plannerOptions, EncoderBase(encoderEnum, level, encoderOptions), new Yices2(solverPath), updateFunction)

        result.result match {
            case Some(r) =>
                val (valid, _ ) = Validator.validate(level, r.actions)
                SnowmanSolverResult(result.sat, valid, result.milliseconds, Some(r))
            case None =>
                SnowmanSolverResult(result.sat, valid = false, result.milliseconds, None)
        }
    }
}
