package gmt.planner.planner

case class PlannerResult[A](sat: Boolean, timeSteps: Int, milliseconds: Long, result: Option[A])