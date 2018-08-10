package gmt.planner.timestep


import gmt.planner.encoder.{Encoder, EncodingData}
import gmt.planner.solver.Solver
import gmt.planner.translator.Translator


class TimeStepSolver[A, B <: EncodingData](encoder: Encoder[A, B], translator: Translator, solver: Solver) {

    def solve(timeSteps: Int): TimeStepResult[A] = {

        val encodingResult = encoder.encode(timeSteps)

        val solverResult = solver.solve(translator.translate(encodingResult.encoding))

        if (!solverResult.sat) {
            TimeStepResult(false, timeSteps, None)
        } else {
            TimeStepResult(true, timeSteps, encoder.decode(solverResult.assignments, encodingResult.encodingData))
        }
    }
}