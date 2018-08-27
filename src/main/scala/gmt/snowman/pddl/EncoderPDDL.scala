package gmt.snowman.pddl

import gmt.snowman.action.SnowmanAction
import gmt.snowman.game.`object`._
import gmt.snowman.level.Level

object EncoderPDDL {

    def encodeStrips(level: Level): String = {
        var encoding = ""

        val directions = List("dir-right", "dir-left", "dir-up", "dir-down")

        encoding += "(define (problem snowman-problem)\n"
        encoding += "    (:domain snowman-adl)\n"
        encoding += "    (:objects\n"
        for (d <- directions) {
            encoding += "        " + d + " - direction\n"
        }

        encoding += encodeObjectsBalls(level)
        encoding += encodeObjectsLocations(level)

        encoding += "    )\n"
        encoding += "    (:init\n"

        for (l <- level.map.values.filter(f => f.o != Wall); (o, d) <- SnowmanAction.ACTIONS.map(f => f.shift).zip(directions) ) {
            level.map.get(l.c + o) match {
                case Some(l2) =>
                    if (l2.o != Wall) {
                        encoding += "        (next loc-" + l.c.x + "-" + l.c.y + " loc-" + l2.c.x + "-" + l2.c.y + " " + d + ")\n"
                    }
                case None =>
            }
        }

        encoding += "        (character-at loc-" + level.character.c.x + "-" + level.character.c.y + ")\n"

        for ((l, i) <- level.balls.zipWithIndex) {
            encoding += "        (ball-at ball-" + i + " loc-" + l.c.x + "-" + l.c.y + ")\n"
            encoding += encodeBallSize(l.o, i)
        }

        encoding += encodeInitSnow(level)
        encoding += encodeInitOccupancy(level)

        encoding += "    )\n"

        encoding += encodeGoal()

        encoding += ")"

        encoding
    }

    def encodeObjectFluents(level: Level): String = {
        var encoding = ""

        val directions = List("right", "left", "up", "down")

        encoding += "(define (problem snowman-problem)\n"
        encoding += "    (:domain snowman-object-fluents)\n"
        encoding += "    (:objects\n"

        encoding += encodeObjectsBalls(level)
        encoding += encodeObjectsLocations(level)

        encoding += "    )\n"
        encoding += "    (:init\n"

        for (l <- level.map.values.filter(f => f.o != Wall); (o, d) <- SnowmanAction.ACTIONS.map(f => f.shift).zip(directions) ) {
            level.map.get(l.c + o) match {
                case Some(l2) =>
                    if (l2.o != Wall) {
                        encoding += "        (= (next-" + d + " loc-" + l.c.x + "-" + l.c.y + ") loc-" + l2.c.x + "-" + l2.c.y + ")\n"
                    }
                case None =>
            }
        }

        encoding += "        (= (character-at) loc-" + level.character.c.x + "-" + level.character.c.y + ")\n"

        for ((l, i) <- level.balls.zipWithIndex) {
            encoding += "        (= (ball-at) ball-" + i + " loc-" + l.c.x + "-" + l.c.y + ")\n"
            encoding += encodeBallSize(l.o, i)
        }

        encoding += encodeInitSnow(level)
        encoding += encodeInitOccupancy(level)

        encoding += "    )\n"

        encoding += encodeGoal()

        encoding += ")"

        encoding
    }

    //def encodeNumericFluents(toLevel: Level): String = {
    //    throw new NotImplementedError() // TODO PDDL
    //}

    private def encodeObjectsBalls(level: Level): String ={
        var encoding = ""

        for (i <- level.balls.indices) {
            encoding += "        ball-" + i + " - ball\n"
        }

        encoding
    }

    private def encodeObjectsLocations(level: Level): String ={
        var encoding = ""

        for (l <- level.map.values.filter(f => f.o != Wall)) {
            encoding += "        loc-" + l.c.x + "-" + l.c.y + " - location\n"
        }

        encoding
    }

    private def encodeInitOccupancy(level: Level): String = {
        var encoding = ""

        for (l <- level.map.values.filter(f => Object.isBall(f.o))) {
            encoding += "        (occupancy loc-" + l.c.x + "-" + l.c.y + ")\n"
        }

        encoding
    }

    private def encodeInitSnow(level: Level): String = {
        var encoding = ""

        for (l <- level.map.values.filter(f => f.o == Snow)) {
            encoding += "        (snow loc-" + l.c.x + "-" + l.c.y + ")\n"
        }

        encoding
    }

    private def encodeBallSize(o: Object, index: Int): String = {
        "        (ball-size-" + getBallSize(o) + " ball-" + index + ")\n"
    }

    private def encodeGoal(): String = {
        """|    (:goal
           |        (goal)
           |    )
           |""".stripMargin
    }

    def getBallSize(o: gmt.snowman.game.`object`.Object): String = o match {
        case SmallBall =>
            "small"
        case MediumBall =>
            "medium"
        case LargeBall =>
            "large"
    }
}
