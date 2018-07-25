package snowman.solver.timeStepSolver

import snowman.solver.solver.Assignment

case class TimeStepResult(sat: Boolean, actions: Int, assignments: Seq[Assignment])
