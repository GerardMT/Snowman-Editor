package gmt.snowman.action

import gmt.snowman.level.Coordinate

case object Down extends SnowmanAction {

    override val shift: Coordinate = Coordinate(0, -1)

    override def toString: String = "down"
}
