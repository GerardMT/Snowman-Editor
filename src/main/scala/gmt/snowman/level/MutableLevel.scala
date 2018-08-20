package gmt.snowman.level

import gmt.snowman.collection.SortedMap
import gmt.snowman.game.Game
import gmt.snowman.level.MutableLevel.Info
import gmt.snowman.game.`object`._

import scala.collection.{immutable, mutable}
import scala.collection.mutable.ListBuffer

object MutableLevel {

    case class Info(size: Int, width: Int, height: Int, playableArea: Int, balls: Int, objects: immutable.Map[Object, Int])

    def default(width: Int, height: Int): MutableLevel = {
        val mutableLevel = new MutableLevel(width, height)

        for (x <- 0 until width) {
            for (y <- 0 until height) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    mutableLevel(Coordinate(x, y)) = Wall
                } else {
                    mutableLevel(Coordinate(x, y)) = Grass
                }
            }
        }

        mutableLevel
    }

    def load(s: String): MutableLevel = {
        val lines = ListBuffer.empty[String]

        for (l <- s.split("\n")) {
            lines.append(l)
        }

        val length = lines.head.length
        lines.tail.foreach(f => {
            if (f.length != length) {
                throw LevelParserException("Level has to be rectangular")
            }
        })

        val mutableLevel = new MutableLevel(length, lines.length)

        var inverseY = lines.length - 1
        for (y <- lines.indices) {
            for (x <- 0 until length) {
                val c = lines(y).charAt(x)

                val o = try {
                    Object.createObject(c)
                } catch {
                    case _: NoSuchElementException =>
                        throw LevelParserException("Illegal character \"" + c + "\" at (X,Y) = (" + x + "," + y + ")")
                }

                mutableLevel(Coordinate(x, inverseY)) = o
            }
            inverseY -= 1
        }

        mutableLevel
    }
}

class MutableLevel private (val width: Int, val height: Int){

    private val map = Array.ofDim[gmt.snowman.game.`object`.Object](width, height)

    def update(c: Coordinate, o: gmt.snowman.game.`object`.Object): Unit = {
        map(c.x)(c.y) = o
    }

    def apply(c: Coordinate): gmt.snowman.game.`object`.Object = {
        map(c.x)(c.y)
    }

    def toLevel: Level = {
        var size = 0
        var hasSnow = false

        var someCharacterLocation: Option[Location] = None
        val balls = ListBuffer.empty[Location]

        val sortedMap = SortedMap.empty[Coordinate, Location]

        for (x <- 0 until width) {
            for (y <- 0 until height) {
                val c = Coordinate(x, y)

                val o = map(x)(y)
                val p = Location(c, o)

                sortedMap.put(c, p)

                o match {
                    case Empty =>
                    case _ =>
                        size += 1
                }

                o match {
                    case Snow =>
                        hasSnow = true
                    case Character =>
                        someCharacterLocation = Some(p)
                    case CharacterSnow =>
                        hasSnow = true
                        someCharacterLocation = Some(p)
                    case SmallBall | MediumBall | LargeBall =>
                        balls.append(p)
                    case MediumSmallBall | LargeSmallBall | LargeMediumBall | LargeMediumSmallBall =>
                        Object.unpackBalls(o).foreach(f => balls.append(Location(c, f)))
                    case _ =>
                }
            }
        }

        val toRemove = ListBuffer.empty[Coordinate]

        for (p <- sortedMap.values) {
            val right= sortedMap.get(p.c + Coordinate(+1, 0))
            val left = sortedMap.get(p.c + Coordinate(-1, 0))
            val up = sortedMap.get(p.c + Coordinate(0, +1))
            val down = sortedMap.get(p.c + Coordinate(0, -1))

            if ((right.isEmpty || right.get.o == Wall) && (left.isEmpty || left.get.o == Wall) && (up.isEmpty || up.get.o == Wall) && (down.isEmpty || down.get.o == Wall)) {
                toRemove.append(p.c)
            }
        }

        toRemove.foreach(f => {
            sortedMap.remove(f)
        })

        Level(width, height, size, hasSnow, balls.size / Game.SNOWMAN_BALLS, someCharacterLocation.get, balls.toList, sortedMap, toString)
    }

    def validate: (Boolean, Boolean) = {
        var balls = 0
        var character = 0

        for (x <- 0 until width) {
            for (y <- 0 until height) {
                map(x)(y) match {
                    case Character | CharacterSnow =>
                        character += 1
                    case o @ (SmallBall | MediumBall | LargeBall | MediumSmallBall | LargeSmallBall | LargeMediumBall | LargeMediumSmallBall) =>
                        balls += Object.unpackBalls(o).size
                    case _ =>
                }
            }
        }

        (character == 1, balls % Game.SNOWMAN_BALLS == 0 && balls != 0)
    }

    def save: String = {
        toString
    }

    override def toString: String = {
        val stringBuilder = new StringBuilder

        for (y <- height - 1 to 0 by -1) {
            for (x <- 0 until width ) {
                stringBuilder.append(map(x)(y).char)
            }
            stringBuilder.append("\n")
        }

        stringBuilder.toString()
    }

    def info: Info = {
        var playableArea = 0
        var balls = 0
        val objects = mutable.Map.empty[Object, Int]

        for (x <- 0 until width) {
            for (y <- 0 until height) {
                val o = map(x)(y)

                if (objects.get(o).isEmpty) {
                    objects(o) = 1
                } else {
                    objects(o) += 1
                }

                if (Object.isPlayableArea(o)) {
                    playableArea += 1
                }

                if(Object.isBall(o)) {
                    balls += Object.unpackBalls(o).size
                }
            }
        }

        Info(width * height, width, height, playableArea, balls, objects.toMap)
    }
}