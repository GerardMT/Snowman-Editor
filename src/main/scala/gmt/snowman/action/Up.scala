package gmt.snowman.action

import gmt.snowman.level.Coordinate

case object Up extends SnowmanAction {

    override val shift: Coordinate = Coordinate(0, +1)

    override def toString: String = "up"
}
