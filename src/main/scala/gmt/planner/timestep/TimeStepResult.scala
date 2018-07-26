package gmt.planner.timestep

import gmt.planner.solver.Assignment

case class TimeStepResult(sat: Boolean, timeSteps: Int, assignments: Seq[Assignment])
