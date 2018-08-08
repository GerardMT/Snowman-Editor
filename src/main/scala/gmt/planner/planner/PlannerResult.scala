package gmt.planner.planner

import gmt.planner.action.Action

case class PlannerResult[A <: Action](sat: Boolean, timeSteps: Int, actions: Seq[A])