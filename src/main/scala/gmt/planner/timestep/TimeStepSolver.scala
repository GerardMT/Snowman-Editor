package gmt.planner.timestep


import gmt.planner.encoder.Encoder
import gmt.planner.solver.Solver
import gmt.planner.translator.Translator


class TimeStepSolver[A, B](encoder: Encoder[A, B], translator: Translator, solver: Solver) {

    def solve(timeSteps: Int): TimeStepResult[A] = {

        val encodingResult = encoder.encode(timeSteps)

        val solverResult = solver.solve(translator.translate(encodingResult.encoding))

        if (!solverResult.sat) {
            TimeStepResult(sat = false, timeSteps, None)
        } else {
            TimeStepResult(sat = true, timeSteps, encoder.decode(solverResult.assignments, encodingResult.encodingData))
        }
    }
}