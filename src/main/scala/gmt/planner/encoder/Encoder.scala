package gmt.planner.encoder

import gmt.snowman.encoder.Encoding

trait Encoder {

    def encode(timeSteps: Int): Encoding
}
