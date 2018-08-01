package gmt.planner.timestep


import gmt.planner.encoder.Encoder
import gmt.planner.solver.Solver
import gmt.planner.translator.Translator


class TimeStepSolver(encoder: Encoder, translator: Translator, solver: Solver) {

    def solve(timeSteps: Int): TimeStepResult = {

        val encodingResult = encoder.encode(timeSteps)
        val solverResult = solver.solve(translator.translate(encodingResult.encoding))
        val actions = encoder.decode(solverResult.assignments, encodingResult.encodingData)

        TimeStepResult(solverResult.sat, timeSteps, actions)
    }
}