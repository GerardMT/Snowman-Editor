package gmt.snowman.action

import gmt.snowman.level.Coordinate

case class BallLeft(override val ball: Int) extends BallAction(ball) {

    override val shift: Coordinate = Coordinate(-1, 0)

    override def toString: String = "L"
}
