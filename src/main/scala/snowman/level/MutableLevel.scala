package snowman.level

import java.io.{BufferedReader, File, FileReader, FileWriter}

import snowman.collection.SortedMap
import snowman.level.`object`._

import scala.collection.mutable.ListBuffer

object MutableLevel {

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
                    snowman.level.`object`.Object.createObject(c)
                } catch {
                    case e: NoSuchElementException =>
                        throw LevelParserException("Illegal character \"" + c + "\" at (X,Y) = (" + x + "," + y + ")")
                }

                mutableLevel(Coordinate(x, inverseY)) = o
            }
            inverseY -= 1
        }

        mutableLevel
    }

    def load(f: File): MutableLevel = {
        val levelString = new StringBuilder()

        val br = new BufferedReader(new FileReader(f))
        var line = br.readLine()
        while (line != null) {
            levelString.append(line)

            line = br.readLine()

            if (line == null) {
                levelString.append("\n")
            }
        }

        load(levelString.toString)
    }
}

class MutableLevel private (val width: Int, val height: Int){

    private val map = Array.ofDim[snowman.level.`object`.Object](width, height)

    def update(c: Coordinate, o: snowman.level.`object`.Object): Unit = {
        map(c.x)(c.y) = o
    }

    def apply(c: Coordinate): snowman.level.`object`.Object = {
        map(c.x)(c.y)
    }

    def toLevel: Level = {
        var size = 0
        var hasSnow = false

        var somePlayerPosition: Option[Position] = None
        val balls = ListBuffer.empty[Position]

        val sortedMap = SortedMap.empty[Coordinate, Position]

        for (x <- 0 until width) {
            for (y <- 0 until height) {
                val c = Coordinate(x, y)

                val o = map(x)(y)
                val p = Position(c, o)

                sortedMap.put(c, p)

                o match {
                    case snowman.level.`object`.Empty =>
                    case _ =>
                        size += 1
                }

                o match {
                    case Snow =>
                        hasSnow = true
                    case Player =>
                        somePlayerPosition = Some(p)
                    case PlayerSnow =>
                        hasSnow = true
                        somePlayerPosition = Some(p)
                    case SmallBall | MediumBall | LargeBall =>
                        balls.append(p)
                    case MediumSmallBall =>
                        balls.append(Position(c, MediumBall))
                        balls.append(Position(c, SmallBall))
                    case LargeSmallBall =>
                        balls.append(Position(c, LargeBall))
                        balls.append(Position(c, SmallBall))
                    case LargeMediumBall =>
                        balls.append(Position(c, MediumBall))
                        balls.append(Position(c, SmallBall))
                    case LargeMediumSmallBall =>
                        balls.append(Position(c, LargeBall))
                        balls.append(Position(c, MediumBall))
                        balls.append(Position(c, SmallBall))
                    case _ =>
                }
            }
        }

        val toRemove = ListBuffer.empty[Coordinate]

        for (p <- sortedMap) {
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

        val playerPosition = somePlayerPosition match {
            case Some(p) =>
                p
            case None =>
                throw LevelParserException("Level must have a player")
        }

        if (balls.size % 3 != 0) {
            throw LevelParserException("Level has only " + balls.size + " balls. Has to be multiple of 3")
        }

        new Level(width, height, size, hasSnow, playerPosition, balls.toList, sortedMap, toString)
    }

    def save(file: File): Unit = {
        val fileWriter = new FileWriter(file)
        fileWriter.write(toString)
        fileWriter.close()
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
}
