package gmt.snowman.solver

import gmt.planner.encoder.Encoder
import gmt.planner.planner.Planner
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.planner.solver.Yices2Solver
import gmt.planner.translator.SMTLib2
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder._
import gmt.snowman.level.Level
import gmt.snowman.validator.Validator

object SnowmanSolver {

    case class GenerateOptions(timeSteps: Int)

    def generateBasicEncoding(level: Level, encoderOptions: EncoderOptions, generateOptions: GenerateOptions): String = {
        encodeSMTLIB2(EncoderBasic(level, encoderOptions), generateOptions.timeSteps)
    }

    def generateCheatingEncoding(level: Level, encoderOptions: EncoderOptions, generateOptions: GenerateOptions): String = {
        encodeSMTLIB2(EncoderBasic(level, encoderOptions), generateOptions.timeSteps)
    }

    def generateReachabilityEncoding(level: Level, encoderOptions: EncoderBase.EncoderOptions, generateOptions: GenerateOptions): String = {
        encodeSMTLIB2(EncoderBasic(level, encoderOptions), generateOptions.timeSteps)
    }

    private def encodeSMTLIB2(encoder: Encoder[_, _], timeSteps: Int): String = {
        SMTLib2.translate(encoder.encode(timeSteps).encoding)
    }

    def solveBasicEncoding(solverPath: String, level: Level, encoderOptions: EncoderOptions, plannerOptions: PlannerOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(solverPath, level, EncoderBasic(level, encoderOptions), plannerOptions, updateFunction)
    }

    def solveCheatingEncoding(solverPath: String, level: Level, encoderOptions: EncoderOptions, plannerOptions: PlannerOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(solverPath, level, EncoderCheating(level, encoderOptions), plannerOptions, updateFunction)
    }

    def solveReachabilityEncoding(solverPath: String, level: Level, encoderOptions: EncoderOptions, plannerOptions: PlannerOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(solverPath, level, EncoderReachability(level, encoderOptions), plannerOptions, updateFunction)
    }

    private def solveSMTYics2(solverPath: String, level: Level, encoder: Encoder[DecodingData, EncodingData], plannerOptions: PlannerOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        val result = new Planner(plannerOptions).solve(encoder, SMTLib2, new Yices2Solver(solverPath), updateFunction)

        result.result match {
            case Some(r) =>
                val (valid, _ ) = Validator.validate(level, r.actions)
                SnowmanSolverResult(result.sat, valid, result.milliseconds, Some(r))
            case None =>
                SnowmanSolverResult(result.sat, valid = false, result.milliseconds, None)
        }
    }
}
