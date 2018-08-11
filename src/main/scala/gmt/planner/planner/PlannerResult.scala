package gmt.planner.planner

case class PlannerResult[A](sat: Boolean, timeSteps: Int, result: Option[A], milliseconds: Long)