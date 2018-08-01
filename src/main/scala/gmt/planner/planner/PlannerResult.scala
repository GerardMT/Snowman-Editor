package gmt.planner.planner

import gmt.planner.action.Action

case class PlannerResult(sat: Boolean, timeSteps: Int, actions: Seq[Action])