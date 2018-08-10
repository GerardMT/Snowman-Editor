package gmt.snowman.level

import gmt.snowman.game.`object`.{Empty, Object}
import gmt.snowman.collection.SortedMap
import gmt.snowman.validator.{PlayableLevel, TwoDimSeq}

case class Level(width: Int, height: Int, size: Int, hasSnow: Boolean, snowmans: Int, character: Location, balls: List[Location], map: SortedMap[Coordinate, Location], override val toString: String) {

    def toPlayableLevel: PlayableLevel = {
        val tmpMap = Array.ofDim[Object](width, height)

        for (l <- map.values) {
            tmpMap(l.c.x)(l.c.y) = l.o
        }

        for (x <- 0 until width) {
            for (y <- 0 until height) {
                if (tmpMap(x)(y) == null) {
                    tmpMap(x)(y) = Empty
                }
            }
        }

        PlayableLevel(Coordinate(character.c.x, character.c.y), TwoDimSeq(tmpMap.map(f => f.toVector).toVector))
    }
}