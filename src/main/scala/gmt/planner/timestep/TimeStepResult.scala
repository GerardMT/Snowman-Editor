package gmt.planner.timestep

import gmt.planner.action.Action
import gmt.planner.solver.Assignment

case class TimeStepResult(sat: Boolean, timeSteps: Int, actions: Seq[Action])
