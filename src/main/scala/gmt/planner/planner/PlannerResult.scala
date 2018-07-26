package gmt.planner.planner

import gmt.planner.solver.Assignment

case class PlannerResult(sat: Boolean, timeSteps: Int, assignment: Seq[Assignment])