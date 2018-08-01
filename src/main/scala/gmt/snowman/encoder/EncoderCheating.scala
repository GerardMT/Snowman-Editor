package gmt.snowman.encoder

import gmt.planner.action
import gmt.planner.encoder.{EncoderResult, EncodingData}
import gmt.planner.solver.Assignment
import gmt.snowman.level.Level


class EncoderCheating(l: Level) extends EncoderSnowman(l) {
    override def encode(timeSteps: Int): EncoderResult = ???

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[action.Action] = ???
}
