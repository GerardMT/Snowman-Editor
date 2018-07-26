package gmt.planner.encoder

import gmt.planner.solver.Assignment
import gmt.snowman.encoder.Encoding

import scala.swing.Action

trait Encoder {

    def encode(timeSteps: Int): Encoding

    def decode(assigments: Seq[Assignment]): Seq[Action]
}
