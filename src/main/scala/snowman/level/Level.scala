package snowman.level

import snowman.collection.SortedMap

object Level {

    val cOffsets = List(Coordinate(+1, 0), Coordinate(-1, 0), Coordinate(0, +1), Coordinate(0, -1))
}

class Level(val width: Int, val height: Int, val size: Int, val hasSnow: Boolean, val player: Position, val balls: List[Position], val map: SortedMap[Coordinate, Position], string: String) {
    override def toString: String = string
}