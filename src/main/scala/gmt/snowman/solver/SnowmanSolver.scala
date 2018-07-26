package gmt.snowman.solver

import java.io.File

import gmt.snowman.encoder.{EncoderBaisc, EncoderCheating, EncoderReachability, EncoderSnowman}
import gmt.snowman.level.Level
import gmt.planner.planner.Planner
import gmt.planner.solver.Yices2Solver
import gmt.planner.translator.SMTLib2
import gmt.ui.Settings

class SnowmanSolver {

    def solveWithBasicEncoding(l: Level, encoding: EncoderSnowman): SnowmanSolverResult = {
        val solver = new Planner(Settings.read(new File("tmp")))

        solver.solve(new EncoderBaisc(l), SMTLib2, new Yices2Solver("tmp"))

        // Assigments obtinguts
        // Falten states. Callback?

        throw new NotImplementedError()
    }

    def solveWithCheatingEncoding(l: Level, encoding: EncoderSnowman): SnowmanSolverResult = {
        val solver = new Planner(Settings.read(new File("tmp")))

        solver.solve(new EncoderCheating(l), SMTLib2, new Yices2Solver("tmp"))

        throw new NotImplementedError()
    }

    def solveWithReachabilityEncoding(l: Level, encoding: EncoderSnowman): SnowmanSolverResult = {
        val solver = new Planner(Settings.read(new File("tmp")))

        solver.solve(new EncoderReachability(l), SMTLib2, new Yices2Solver("tmp"))

        throw new NotImplementedError()
    }

    def solvePddl(l: Level): SnowmanSolverResult = {
        throw new NotImplementedError() // TODO
    }
}
