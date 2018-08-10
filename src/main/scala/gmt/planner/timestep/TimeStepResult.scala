package gmt.planner.timestep

case class TimeStepResult[A](sat: Boolean, timeSteps: Int, result: Option[A])
