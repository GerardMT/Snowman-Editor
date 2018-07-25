package snowman.solver.planningSolver

import snowman.solver.solver.Assignment

case class PlanningSolverResult(sat: Boolean, assignment: Seq[Assignment])