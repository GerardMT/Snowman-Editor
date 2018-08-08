package gmt.snowman.solver

import java.io.File

import gmt.planner.encoder.{Encoder, EncodingData}
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

    def solveWithBasicEncoding(settings: Settings, level: Level): SnowmanSolverResult = {
        solveSMTYics2(settings, level, EncoderBasic(level))
    }

    def solveWithCheatingEncoding(settings: Settings, level: Level): SnowmanSolverResult = {
        solveSMTYics2(settings, level, EncoderCheating(level))
    }

    def solveWithReachabilityEncoding(settings: Settings, level: Level): SnowmanSolverResult = {
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
