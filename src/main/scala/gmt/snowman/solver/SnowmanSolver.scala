package gmt.snowman.solver

import gmt.planner.encoder.Encoder
import gmt.planner.planner.Planner
import gmt.planner.planner.Planner.PlannerUpdate
import gmt.planner.solver.Yices2Solver
import gmt.planner.translator.SMTLib2
import gmt.snowman.encoder._
import gmt.snowman.level.Level
import gmt.snowman.validator.Validator

object SnowmanSolver {

    def encodeBasicEncoding(level: Level, timeSteps: Int, encoderOptions: EncoderBase.EncoderOptions): String = {
        encodeSMTLIB2(EncoderBasic(level, encoderOptions), timeSteps)
    }

    def encodeCheatingEncoding(level: Level, timeSteps: Int, encoderOptions: EncoderBase.EncoderOptions): String = {
        encodeSMTLIB2(EncoderBasic(level, encoderOptions), timeSteps)
    }

    def encodeReachabilityEncoding(level: Level, timeSteps: Int, encoderOptions: EncoderBase.EncoderOptions): String = {
        encodeSMTLIB2(EncoderBasic(level, encoderOptions), timeSteps)
    }

    private def encodeSMTLIB2(encoder: Encoder[_, _], timeSteps: Int): String = {
        SMTLib2.translate(encoder.encode(timeSteps).encoding)
    }

    def solveBasicEncoding(solverPath: String, level: Level, encoderOptions: EncoderBase.EncoderOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(solverPath, level, EncoderBasic(level, encoderOptions), updateFunction)
    }

    def solveCheatingEncoding(solverPath: String, level: Level, encoderOptions: EncoderBase.EncoderOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(solverPath, level, EncoderCheating(level, encoderOptions), updateFunction)
    }

    def solveReachabilityEncoding(solverPath: String, level: Level, encoderOptions: EncoderBase.EncoderOptions, updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(solverPath, level, EncoderReachability(level, encoderOptions), updateFunction)
    }

    private def solveSMTYics2(solverPath: String, level: Level, encoder: Encoder[DecodingData, EncodingData], updateFunction: PlannerUpdate[DecodingData] => Unit): SnowmanSolverResult = {
        val result = new Planner(1, 100).solve(encoder, SMTLib2, new Yices2Solver(solverPath), updateFunction)

        result.result match {
            case Some(r) =>
                val (valid, _ ) = Validator.validate(level, r.actions)
                SnowmanSolverResult(result.sat, valid, Some(r))
            case None =>
                SnowmanSolverResult(result.sat, valid = false, None)
        }
    }
}
