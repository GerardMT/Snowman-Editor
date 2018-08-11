package gmt.planner.timestep

import gmt.planner.encoder.Encoder
import gmt.planner.solver.Solver
import gmt.planner.translator.Translator

class TimeStepSolver[A, B](encoder: Encoder[A, B], translator: Translator, solver: Solver) {

    def solve(timeSteps: Int): TimeStepResult[A] = {
        val startTime = System.currentTimeMillis()

        val encodingResult = encoder.encode(timeSteps)

        val solverResult = solver.solve(translator.translate(encodingResult.encoding))

        val time = System.currentTimeMillis() - startTime

        if (!solverResult.sat) {
            TimeStepResult(sat = false, timeSteps, None, time )
        } else {
            TimeStepResult(sat = true, timeSteps, Some(encoder.decode(solverResult.assignments, encodingResult.encodingData)), time )
        }
    }
}