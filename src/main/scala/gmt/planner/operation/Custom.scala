package gmt.planner.operation

case class Custom(f: () => String) extends Expression