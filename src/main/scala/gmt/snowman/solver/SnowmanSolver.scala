package gmt.snowman.solver

import gmt.planner.encoder.Encoder
import gmt.planner.planner.Planner
import gmt.planner.solver.Yices2Solver
import gmt.planner.timestep.TimeStepResult
import gmt.planner.translator.SMTLib2
import gmt.snowman.encoder._
import gmt.snowman.level.Level
import gmt.snowman.validator.Validator
import gmt.ui.Settings

object SnowmanSolver {

    def encodeBasicEncoding(level: Level, timeSteps: Int): String = {
        encodeSMTLIB2(EncoderBasic(level), timeSteps)
    }

    def encodeCheatingEncoding(level: Level, timeSteps: Int): String = {
        encodeSMTLIB2(EncoderBasic(level), timeSteps)
    }

    def encodeReachabilityEncoding(level: Level, timeSteps: Int): String = {
        encodeSMTLIB2(EncoderBasic(level), timeSteps)
    }

    private def encodeSMTLIB2(encoder: Encoder[_, _], timeSteps: Int): String = {
        SMTLib2.translate(encoder.encode(timeSteps).encoding)
    }

    def solveBasicEncoding(settings: Settings, level: Level, updateFunction: TimeStepResult[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(settings, level, EncoderBasic(level), updateFunction)
    }

    def solveCheatingEncoding(settings: Settings, level: Level, updateFunction: TimeStepResult[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(settings, level, EncoderCheating(level), updateFunction)
    }

    def solveReachabilityEncoding(settings: Settings, level: Level, updateFunction: TimeStepResult[DecodingData] => Unit): SnowmanSolverResult = {
        solveSMTYics2(settings: Settings, level, EncoderReachability(level), updateFunction)
    }

    private def solveSMTYics2(settings: Settings, level: Level, encoder: Encoder[DecodingData, EncodingData], updateFunction: TimeStepResult[DecodingData] => Unit): SnowmanSolverResult = {
        val result = new Planner(1, 100).solve(encoder, SMTLib2, new Yices2Solver(settings.solverPath), updateFunction)

        result.result match {
            case Some(r) =>
                val (valid, _ ) = Validator.validate(level, r.actions)
                SnowmanSolverResult(result.sat, valid, Some(r))
            case None =>
                SnowmanSolverResult(result.sat, valid = false, None)
        }
    }

    def solvePDDLStrips(level: Level): SnowmanSolverResult = {
        //val encoding = EncoderPDDL.encodeStrips(level)

        SnowmanSolverResult(solved = false, valid = false, None) // TODO PDDL
    }

    def solvePDDLObjectFluents(level: Level): SnowmanSolverResult = {
        //val encoding = EncoderPDDL.encodeObjectFluents(level)

        SnowmanSolverResult(solved = false, valid = false, None) // TODO PDDL
    }

    def solvePDDLNumericFluents(level: Level): SnowmanSolverResult = {
        //val encoding = EncoderPDDL.encodeObjectFluents(level)

        SnowmanSolverResult(solved = false, valid = false, None) // TODO PDDL
    }
}
