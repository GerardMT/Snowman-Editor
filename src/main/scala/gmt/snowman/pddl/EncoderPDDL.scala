package gmt.snowman.pddl

import java.io.{BufferedWriter, File, FileWriter}

import gmt.snowman.action.SnowmanAction
import gmt.snowman.game.`object`._
import gmt.snowman.level.{Level, MutableLevel}
import gmt.snowman.util.Files

object EncoderPDDL {

    def encodeAdl(level: Level): String = {
        var encoding = ""

        val directions = List("dir_right", "dir_left", "dir_up", "dir_down")

        val levelName = level.name match {
            case Some(s) =>
                s
            case None =>
                "snowman_problem"
        }

        encoding += "(define (problem " + levelName + ")\n"
        encoding += "    (:domain snowman_adl)\n"
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
                        encoding += "        (next loc_" + l.c.x + "_" + l.c.y + " loc_" + l2.c.x + "_" + l2.c.y + " " + d + ")\n"
                    }
                case None =>
            }
        }

        encoding += "        (character_at loc_" + level.character.c.x + "_" + level.character.c.y + ")\n"

        for ((l, i) <- level.balls.zipWithIndex) {
            encoding += "        (ball_at ball_" + i + " loc_" + l.c.x + "_" + l.c.y + ")\n"
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

        val levelName = level.name match {
            case Some(s) =>
                s
            case None =>
                "snowman_problem"
        }

        encoding += "(define (problem " + levelName + ")\n"
        encoding += "    (:domain snowman_object_fluents)\n"
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

        encoding += "        (= (character_at) loc_" + level.character.c.x + "_" + level.character.c.y + ")\n"

        for ((l, i) <- level.balls.zipWithIndex) {
            encoding += "        (= (ball_at) ball_" + i + " loc_" + l.c.x + "_" + l.c.y + ")\n"
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
            encoding += "        ball_" + i + " - ball\n"
        }

        encoding
    }

    private def encodeObjectsLocations(level: Level): String ={
        var encoding = ""

        for (l <- level.map.values.filter(f => f.o != Wall)) {
            encoding += "        loc_" + l.c.x + "_" + l.c.y + " - location\n"
        }

        encoding
    }

    private def encodeInitOccupancy(level: Level): String = {
        var encoding = ""

        for (l <- level.map.values.filter(f => Object.isBall(f.o))) {
            encoding += "        (occupancy loc_" + l.c.x + "_" + l.c.y + ")\n"
        }

        encoding
    }

    private def encodeInitSnow(level: Level): String = {
        var encoding = ""

        for (l <- level.map.values.filter(f => f.o == Snow)) {
            encoding += "        (snow loc_" + l.c.x + "_" + l.c.y + ")\n"
        }

        encoding
    }

    private def encodeBallSize(o: Object, index: Int): String = {
        "        (ball_size_" + getBallSize(o) + " ball_" + index + ")\n"
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

    def autoEncoder(encoder: Level => String, levelsPath: String, outPath: String): Unit = {
        val levelsDirectory = new File(levelsPath)

        for (file <- levelsDirectory.listFiles().sorted) {
            try {
                val level = MutableLevel.load(Files.openTextFile(file)).toLevel

                val outFolder = outPath + file.getName
                new File(outFolder).mkdir()
                Files.saveTextFile(new File(outFolder + "/problem.pddl"), encoder(level))
            }
        }
    }
}
