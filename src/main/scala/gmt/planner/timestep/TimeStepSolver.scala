package gmt.planner.timestep

import gmt.planner.encoder.Encoder
import gmt.planner.solver.Solver
import gmt.planner.translator.Translator
import gmt.snowman.encoder.Encoding


class TimeStepSolver(translator: Translator, encoder: Encoder, solver: Solver, workingDirectoryPath: String) {

    def solve(timeSteps: Int): TimeStepResult = {
        solver.workingDirectoryPath_=(workingDirectoryPath)

        val solverResult = solver.solve(translator.translate(encoder.encode(timeSteps)))

        TimeStepResult(solverResult.sat, timeSteps, solverResult.assignments)
    }

    def encode(timeSteps: Int): Encoding ={
        encoder.encode(timeSteps)
    }
}