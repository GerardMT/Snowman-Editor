package gmt.snowman.encoder

import gmt.planner.encoder.Encoding

trait VariableAdder {

    def addVariables(encoding: Encoding): Unit
}
