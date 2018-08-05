package gmt.snowman.level

import gmt.snowman.collection.SortedMap

object Level {

    val OFFSETS = List(Coordinate(+1, 0), Coordinate(-1, 0), Coordinate(0, +1), Coordinate(0, -1))
}

class Level(val width: Int, val height: Int, val size: Int, val hasSnow: Boolean, val player: Location, val balls: List[Location], val map: SortedMap[Coordinate, Location], string: String) {
    override def toString: String = string
}