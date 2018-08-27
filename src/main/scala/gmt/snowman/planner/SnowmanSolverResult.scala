package gmt.snowman.planner

import gmt.snowman.encoder.DecodingData

case class SnowmanSolverResult(solved: Boolean, valid: Boolean, milliseconds: Long, result: Option[DecodingData])