package gmt.planner.timestep

import gmt.planner.encoder.Encoder
import gmt.planner.solver.Solver
import gmt.planner.translator.Translator


class TimeStepSolver(translator: Translator, encoder: Encoder, solver: Solver, workingDirectoryPath: String) {

    def solve(timeSteps: Int): TimeStepResult = {

        val solverResult = solver.solve(translator.translate(encoder.encode(timeSteps)))
        TimeStepResult(solverResult.sat, timeSteps, solverResult.assignments)
    }
}