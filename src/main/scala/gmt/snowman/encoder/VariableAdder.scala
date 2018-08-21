package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.snowman.encoder.EncoderBase.EncoderOptions

trait VariableAdder {

    def addVariables(encoding: Encoding, encoderOptions: EncoderOptions): Unit
}
