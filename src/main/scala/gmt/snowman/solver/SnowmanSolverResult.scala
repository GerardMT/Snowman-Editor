package gmt.snowman.solver

import gmt.planner.action.Action

case class SnowmanSolverResult(solved: Boolean, actions: Seq[Action])

