package gmt.snowman.solver

import gmt.snowman.action.SnowmanAction

case class SnowmanSolverResult(solved: Boolean, valid: Boolean, actions: Seq[SnowmanAction])

