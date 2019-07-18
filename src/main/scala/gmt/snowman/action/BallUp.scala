package gmt.snowman.action

import gmt.snowman.level.Coordinate

case class BallUp(override val ball: Int) extends BallAction(ball) {

    override val shift: Coordinate = Coordinate(0, +1)

    override def toString: String = "U"
}
