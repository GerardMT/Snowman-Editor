package gmt.snowman.solver

import gmt.snowman.encoder.DecodingData

case class SnowmanSolverResult(solved: Boolean, valid: Boolean, result: Option[DecodingData])