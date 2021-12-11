package gmt.snowman.planner

import gmt.snowman.encoder.DecodingData

case class SnowmanSolverResult(solved: Boolean, valid: Boolean, cpuSeconds: Float, result: Option[DecodingData])