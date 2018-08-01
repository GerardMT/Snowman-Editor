package gmt.planner.encoder

import gmt.planner.action.Action
import gmt.planner.solver.Assignment


trait Encoder {

    def encode(timeSteps: Int): EncoderResult

    def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[Action]
}
