package gmt.snowman.solver

import java.io.File

import gmt.planner.encoder.Encoder
import gmt.planner.planner.Planner
import gmt.planner.solver.Yices2Solver
import gmt.planner.translator.SMTLib2
import gmt.snowman.encoder._
import gmt.snowman.level.Level
import gmt.snowman.pddl.EncoderPDDL
import gmt.ui.Settings

object SnowmanSolver {

    def solveWithBasicEncoding(level: Level): SnowmanSolverResult = {
        solveSMTYics2(level, new EncoderBaisc(level))
    }

    def solveWithCheatingEncoding(level: Level): SnowmanSolverResult = {
        solveSMTYics2(level, new EncoderCheating(level))
    }

    def solveWithReachabilityEncoding(level: Level): SnowmanSolverResult = {
        solveSMTYics2(level, new EncoderReachability(level))
    }

    private def solveSMTYics2(level: Level, encoder: Encoder): SnowmanSolverResult = {
        val result = new Planner(Settings.read(new File("tmp"))).solve(encoder, SMTLib2, new Yices2Solver("tmp"))

        SnowmanSolverResult(result.sat, result.actions)
    }

    def solvePDDLStrips(level: Level): SnowmanSolverResult = {
        val encoding = EncoderPDDL.encodeStrips(level)

        SnowmanSolverResult(false, List()) // TODO
    }

    def solvePDDLObjectFluents(level: Level): SnowmanSolverResult = {
        val encoding = EncoderPDDL.encodeObjectFluents(level)

        SnowmanSolverResult(false, List()) // TODO
    }

    def solvePDDLNumericFluents(level: Level): SnowmanSolverResult = {
        val encoding = EncoderPDDL.encodeObjectFluents(level)

        SnowmanSolverResult(false, List()) // TODO
    }
}
