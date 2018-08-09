package gmt.snowman.solver

import gmt.planner.encoder.Encoder
import gmt.planner.planner.Planner
import gmt.planner.solver.Yices2Solver
import gmt.planner.translator.SMTLib2
import gmt.snowman.action.SnowmanAction
import gmt.snowman.encoder._
import gmt.snowman.level.Level
import gmt.snowman.pddl.EncoderPDDL
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

    def solveBasicEncoding(settings: Settings, level: Level): SnowmanSolverResult = {
        solveSMTYics2(settings, level, EncoderBasic(level))
    }

    def solveCheatingEncoding(settings: Settings, level: Level): SnowmanSolverResult = {
        solveSMTYics2(settings, level, EncoderCheating(level))
    }

    def solveReachabilityEncoding(settings: Settings, level: Level): SnowmanSolverResult = {
        solveSMTYics2(settings: Settings, level, EncoderReachability(level))
    }

    private def solveSMTYics2(settings: Settings, level: Level, encoder: Encoder[SnowmanAction, SnowmanEncodingData]): SnowmanSolverResult = {
        val result = new Planner(1, 100).solve(encoder, SMTLib2, new Yices2Solver(settings.solverPath))

        val (valid, _) = Validator.validate(level, result.actions)

        SnowmanSolverResult(result.sat, valid, result.actions)
    }

    def solvePDDLStrips(level: Level): SnowmanSolverResult = {
        val encoding = EncoderPDDL.encodeStrips(level)

        SnowmanSolverResult(solved = false, valid = false, List()) // TODO PDDL
    }

    def solvePDDLObjectFluents(level: Level): SnowmanSolverResult = {
        val encoding = EncoderPDDL.encodeObjectFluents(level)

        SnowmanSolverResult(solved = false, valid = false, List()) // TODO PDDL
    }

    def solvePDDLNumericFluents(level: Level): SnowmanSolverResult = {
        val encoding = EncoderPDDL.encodeObjectFluents(level)

        SnowmanSolverResult(solved = false, valid = false, List()) // TODO PDDL
    }
}
