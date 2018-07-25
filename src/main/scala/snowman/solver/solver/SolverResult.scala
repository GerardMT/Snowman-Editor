package snowman.solver.solver

import Assignment

case class SolverResult (sat: Boolean, assignments: Seq[Assignment])
