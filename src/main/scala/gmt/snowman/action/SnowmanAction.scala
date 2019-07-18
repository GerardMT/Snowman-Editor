package gmt.snowman.action

import gmt.snowman.level.Coordinate

object SnowmanAction {

    val CHARACTER_ACTIONS = List(CharacterRight, CharacterLeft, CharacterUp, CharacterDown)

    val BALL_ACTIONS = List(BallRight, BallLeft, BallUp, BallDown)
}

trait SnowmanAction {

    val shift: Coordinate
}
