package gmt.snowman.planner

import gmt.snowman.encoder.DecodingData

case class SnowmanSolverResult(solved: Boolean, valid: Boolean, nanoseconds: Long, result: Option[DecodingData])