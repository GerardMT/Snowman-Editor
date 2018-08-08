package gmt.snowman.action
import gmt.snowman.level.Coordinate

case object Left extends SnowmanAction {

    override val shift: Coordinate = Coordinate(-1, 0)

    override def toString: String = "left"
}
