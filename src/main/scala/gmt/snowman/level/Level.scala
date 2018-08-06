package gmt.snowman.level

import gmt.snowman.collection.SortedMap

object Level {

    val OFFSETS = List(Coordinate(+1, 0), Coordinate(-1, 0), Coordinate(0, +1), Coordinate(0, -1))
}

case class Level(width: Int, height: Int, size: Int, hasSnow: Boolean, player: Location, balls: List[Location], map: SortedMap[Coordinate, Location], override val toString: String)