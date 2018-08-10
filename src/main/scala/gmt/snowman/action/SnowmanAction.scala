package gmt.snowman.action

import gmt.snowman.level.Coordinate

object SnowmanAction {

    val ACTIONS = List(Right, Left, Up, Down)
}

trait SnowmanAction {

    val shift: Coordinate
}
