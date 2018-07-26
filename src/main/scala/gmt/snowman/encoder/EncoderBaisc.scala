package gmt.snowman.encoder

import gmt.planner.solver.Assignment
import gmt.snowman.level.Level

import scala.swing.Action

class EncoderBaisc(l: Level) extends EncoderSnowman(l) {

    override def encode(timeSteps: Int): Encoding = ???

    override def decode(assigments: Seq[Assignment]): Seq[Action] = ???
}
