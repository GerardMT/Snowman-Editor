package gmt.snowman.action

import gmt.planner.action.Action
import gmt.snowman.level.Coordinate

object SnowmanAction {

    val ACTIONS = List(Right, Left, Up, Down)
}

trait SnowmanAction extends Action {

    val shift: Coordinate
}
