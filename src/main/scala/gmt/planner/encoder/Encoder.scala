package gmt.planner.encoder

import gmt.planner.action.Action
import gmt.planner.solver.Assignment

import scala.collection.immutable


trait Encoder[A <: Action, B <: EncodingData] {

    def encode(timeSteps: Int): EncoderResult[B]

    def decode(assignments: Seq[Assignment], encodingData: B): immutable.Seq[A]
}
