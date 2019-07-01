package gmt.snowman.pddl

import gmt.snowman.action.SnowmanAction
import gmt.snowman.game.Game
import gmt.snowman.game.`object`.{Object, _}
import gmt.snowman.level.{Coordinate, Level}

object EncoderPDDL {

    case class TooManyBallsNotSupported() extends Exception()

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

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o)); (o, d) <- SnowmanAction.ACTIONS.map(f => f.shift).zip(directions) ) {
            level.map.get(l.c + o) match {
                case Some(l2) =>
                    if (Object.isPlayableArea(l2.o)) {
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

        encoding += encodeGoal(level)

        encoding += "\n"

        encoding += "    (:metric minimize (total-cost))\n"

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

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o)); (o, d) <- SnowmanAction.ACTIONS.map(f => f.shift).zip(directions) ) {
            level.map.get(l.c + o) match {
                case Some(l2) =>
                    if (Object.isPlayableArea(l2.o)) {
                        encoding += "        (= (next_" + d + " loc_" + l.c.x + "_" + l.c.y + ") loc_" + l2.c.x + "_" + l2.c.y + ")\n"
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

        encoding += encodeGoal(level)

        encoding += "\n"

        encoding += "    (:metric minimize (total-cost))\n"

        encoding += ")"

        encoding
    }

    def encodeAdlGrounded(level: Level): (String, String)= {
        val domain = StringBuilder.newBuilder

        domain.append("(define (domain snowman_adl_grounded)\n")
        domain.append("\n")
        domain.append("    (:requirements\n")
        domain.append("        :typing\n")
        domain.append("        :negative-preconditions\n")
        domain.append("        :equality\n")
        domain.append("        :disjunctive-preconditions\n")
        domain.append("        :conditional-effects\n")
        domain.append("        :action-costs\n")
        domain.append("    )\n")
        domain.append("\n")
        domain.append("    (:types\n")
        domain.append("        location ball - object\n")
        domain.append("    )\n")
        domain.append("\n")
        domain.append("    (:constants\n")

        for (i <- level.balls.indices) {
                domain.append("        ball_" + i + " - ball\n")
        }

        domain.append(encodeObjectsLocations(level))
        domain.append("    )\n")
        domain.append("\n")
        domain.append("    (:predicates\n")
        domain.append("        (snow ?l - location)\n")
        domain.append("        (occupancy ?l - location)\n")
        domain.append("        (character_at ?l - location)\n")
        domain.append("        (ball_at ?b - ball ?l - location)\n")
        domain.append("        (ball_size_small ?b - ball)\n")
        domain.append("        (ball_size_medium ?b - ball)\n")
        domain.append("        (ball_size_large ?b - ball)\n")
        domain.append("        (goal)\n")
        domain.append("    )\n")

        domain.append("    (:functions\n")
        domain.append("        (total-cost) - number\n")
        domain.append("    )\n")

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o)); o <- SnowmanAction.ACTIONS.map(f => f.shift)) {
            level.map.get(l.c + o) match {
                case Some(l2) =>
                    if (Object.isPlayableArea(l2.o)) {
                        val from = "loc_" + l.c.x + "_" + l.c.y
                        val to = "loc_" + l2.c.x + "_" + l2.c.y

                        domain.append("\n")
                        domain.append("    (:action move_character__" + from + "__" + to + "\n")
                        domain.append("\n")
                        domain.append("     :parameters\n")
                        domain.append("        ()\n")
                        domain.append("\n")
                        domain.append("     :precondition\n")
                        domain.append("        (and\n")
                        domain.append("            (character_at " + from + ")\n")
                        domain.append("            (not (occupancy " + to + ")))\n")
                        domain.append("\n")
                        domain.append("     :effect\n")
                        domain.append("       (and\n")
                        domain.append("           (not (character_at " + from + "))\n")
                        domain.append("           (character_at " + to + "))\n")
                        domain.append("    )\n")
                    }
                case None =>
            }
        }

        for (l0 <- level.map.values.filter(f => Object.isPlayableArea(f.o)); shift <- SnowmanAction.ACTIONS.map(f => f.shift)) {
            (level.map.get(l0.c + shift), level.map.get(l0.c + shift + shift)) match {
                case (Some(l1), Some(l2)) =>
                    if (Object.isPlayableArea(l1.o) && Object.isPlayableArea(l2.o)) {
                        val ppos = "loc_" + l0.c.x + "_" + l0.c.y
                        val from = "loc_" + l1.c.x + "_" + l1.c.y
                        val to = "loc_" + l2.c.x + "_" + l2.c.y

                        val allBalls = level.balls.indices.map(f => "ball_" + f)

                        for ((b, otherBalls) <- level.balls.indices.map(f => ("ball_" + f, allBalls.patch(f, Nil, 1)))) {
                            domain.append("\n")
                            domain.append("    (:action move_ball__" + b + "__" + ppos + "__" + from + "__" + to)

                            for (o <- otherBalls) {
                                domain.append("__" + o)
                            }

                            domain.append("\n")
                            domain.append("\n")
                            domain.append("     :parameters\n")
                            domain.append("        ()\n")
                            domain.append("\n")
                            domain.append("     :precondition\n")
                            domain.append("        (and\n")
                            domain.append("            (ball_at " + b + " " + from + ")\n")
                            domain.append("            (character_at " + ppos + ")\n")
                            domain.append("            (and")

                            for (o <- otherBalls) {
                                domain.append("\n")
                                domain.append("                (or\n")
                                domain.append("                    (not (ball_at " +  o  + " " + from + "))\n")
                                domain.append("                    (or\n")
                                domain.append("                        (and\n")
                                domain.append("                            (ball_size_small " + b + ")\n")
                                domain.append("                            (ball_size_medium " + o + "))\n")
                                domain.append("                        (and\n")
                                domain.append("                            (ball_size_small " + b + ")\n")
                                domain.append("                            (ball_size_large " + o + "))\n")
                                domain.append("                        (and\n")
                                domain.append("                            (ball_size_medium " + b + ")\n")
                                domain.append("                            (ball_size_large " + o + "))))")
                            }

                            domain.append(")\n")
                            domain.append("            (or\n")
                            domain.append("                (not (or")

                            for (o <- otherBalls) {
                                domain.append("\n")
                                domain.append("                    (ball_at " + o + " " + from + ")")
                            }

                            domain.append("))\n")
                            domain.append("                (and\n")

                            for (o <- otherBalls) {
                                domain.append("\n")
                                domain.append("                    (not (ball_at " + o + " " + to + "))")
                            }

                            domain.append("))\n")
                            domain.append("            (and")

                            for (o <- otherBalls) {
                                domain.append("\n")
                                domain.append("                (or\n")
                                domain.append("                    (not (ball_at " + o + " " + to + "))\n")
                                domain.append("                    (or\n")
                                domain.append("                        (and\n")
                                domain.append("                            (ball_size_small " + b + ")\n")
                                domain.append("                            (ball_size_medium " + o + "))\n")
                                domain.append("                        (and\n")
                                domain.append("                            (ball_size_small " + b + ")\n")
                                domain.append("                            (ball_size_large " + o + "))\n")
                                domain.append("                        (and\n")
                                domain.append("                            (ball_size_medium " + b + ")\n")
                                domain.append("                            (ball_size_large " + o + "))))")
                            }

                            domain.append("))\n")
                            domain.append("\n")
                            domain.append("     :effect\n")
                            domain.append("        (and\n")
                            domain.append("            (when\n")
                            domain.append("                (and")

                            for (o <- otherBalls) {
                                domain.append("\n")
                                domain.append("                    (ball_at " + o + " " + to + ")")
                            }

                            domain.append(")\n")
                            domain.append("                (goal))\n")
                            domain.append("            (occupancy " + to + ")\n")
                            domain.append("            (not (ball_at " + b + " " + from + "))\n")
                            domain.append("            (ball_at " + b + " " + to + ")\n")
                            domain.append("            (when\n")
                            domain.append("                (and")

                            for (o <- otherBalls) {
                                domain.append("\n")
                                domain.append("                    (not (ball_at " + o + " " + from + "))")
                            }
                            domain.append(")\n")

                            domain.append("                (and\n")
                            domain.append("                    (not (character_at " + ppos + "))\n")
                            domain.append("                    (character_at " + from + ")\n")
                            domain.append("                    (not (occupancy " + from + "))))\n")
                            domain.append("            (not (snow " + to + "))\n")
                            domain.append("            (when\n")
                            domain.append("                (and\n")
                            domain.append("                    (snow " + to + ")\n")
                            domain.append("                    (ball_size_small " + b + "))\n")
                            domain.append("                (and\n")
                            domain.append("                    (not (ball_size_small " + b + "))\n")
                            domain.append("                    (ball_size_medium " + b + ")))\n")
                            domain.append("            (when\n")
                            domain.append("                (and\n")
                            domain.append("                    (snow " + to + ")\n")
                            domain.append("                    (ball_size_medium " + b + "))\n")
                            domain.append("                (and")
                            domain.append("                    (not (ball_size_medium " + b + "))\n")
                            domain.append("                    (ball_size_large " + b + ")))\n")
                            domain.append("            (increase (total-cost) 1))\n")
                            domain.append("    )\n")
                        }
                    }
                case _ =>
            }
        }

        domain.append(")")

        val problem = StringBuilder.newBuilder

        val levelName = level.name match {
            case Some(s) =>
                s
            case None =>
                "snowman_problem"
        }

        problem.append("(define (problem " + levelName + ")\n")
        problem.append("\n")
        problem.append("    (:domain snowman_adl_grounded)\n")
        problem.append("\n")
        problem.append("    (:init\n")
        problem.append("        (character_at loc_" + level.character.c.x + "_" + level.character.c.y + ")\n")

        for ((l, i) <- level.balls.zipWithIndex) {
            problem.append("        (ball_at ball_" + i + " loc_" + l.c.x + "_" + l.c.y + ")\n")
            problem.append(encodeBallSize(l.o, i))
        }

        problem.append(encodeInitSnow(level))
        problem.append(encodeInitOccupancy(level))
        problem.append("    )\n")
        problem.append("\n")
        problem.append(encodeGoal(level))
        problem.append("\n")
        problem.append("    (:metric minimize (total-cost))\n")

        problem.append(")")

        (domain.toString(), problem.toString())
    }

    private def encodeObjectsBalls(level: Level): String = {
        var encoding = ""

        for (i <- level.balls.indices) {
            encoding += "        ball_" + i + " - ball\n"
        }

        encoding
    }

    private def encodeObjectsLocations(level: Level): String = {
        var encoding = ""

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
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

        for (l <- level.map.values.filter(f => Object.isSnow(f.o))) {
            encoding += "        (snow loc_" + l.c.x + "_" + l.c.y + ")\n"
        }

        encoding
    }

    private def encodeBallSize(o: Object, index: Int): String = {
        "        (ball_size_" + getBallSize(o) + " ball_" + index + ")\n"
    }

    private def encodeSnowman(snowman: Seq[Int]): Int = 0x0000 | 0x0001 << snowman(0) | 0x0001 << snowman(1) | 0x0001 << snowman(2)

    private def decodeSnowman(snowman: Int): Seq[Int] = inmersiveDecodeSnowman(snowman, 0)

    private def inmersiveDecodeSnowman(snowman: Int, bit: Int): Seq[Int] = {
        if (snowman == 0x0000) {
            Nil
        } else {
            if ((snowman & 0x0001) == 0x0001) {
                bit +: inmersiveDecodeSnowman(snowman >>> 1, bit + 1)
            } else {
                inmersiveDecodeSnowman(snowman >>> 1, bit + 1)
            }
        }
    }

    private def encodeGoal(level: Level): String = {
        val goal = new StringBuilder
        goal.append("    (:goal\n")
        if (level.snowmen == 1) {
            goal.append("        (or\n")
            goal.append(level.map.values.filter(f => Object.isPlayableArea(f.o)).map(f => "            " + equalBallPositions(f.c, level.balls.indices.toList)).mkString("\n") + "\n")
            goal.append("        )\n")
        } else {
            goal.append("        (or\n")
            for (snowmenBalls <- setPartitionBalls(level)) {
                goal.append("            (or \n")
                for (c <- level.map.values.filter(f => Object.isPlayableArea(f.o)).toSeq.combinations(level.snowmen)) {
                    for ((location, iSnowman) <- c.zipWithIndex) {
                        goal.append("                (and ")
                        goal.append(snowmenBalls(iSnowman).map(ballIndex => "(ball_at ball_" + ballIndex + " " + coordinateToLocation(location.c) + ")").mkString(" "))
                        goal.append(")\n")
                    }
                }
                goal.append("            )\n")
            }
            goal.append("        )\n")
        }
        goal.append("    )\n")

        goal.mkString
    }

    private def setPartitionBalls(level: Level): Seq[Seq[Seq[Int]]] = {
        if (level.balls.size > 32) {
            throw TooManyBallsNotSupported()
        }

        inmersiveSetPartitionBalls(level.snowmen, level.balls.indices.combinations(Game.SNOWMAN_BALLS).map(f => encodeSnowman(f)).toSeq, 0, Nil).map(f => f.map(k => decodeSnowman(k)))
    }

    private def inmersiveSetPartitionBalls(nSnomwen: Int, combinations: Seq[Int], mask: Int, partial: Seq[Int]): Seq[Seq[Int]] = {
        if (combinations.isEmpty) {
            Nil
        } else {
            inmersiveSetPartitionBalls(nSnomwen, combinations.tail, mask, partial) ++ (if ((combinations.head & mask) != 0) {
                Nil
            } else if (partial.size == nSnomwen - 1) {
                List(combinations.head +: partial)
            } else {
                inmersiveSetPartitionBalls(nSnomwen, combinations.tail, combinations.head | mask, combinations.head +: partial)
            })
        }
    }

    private def equalBallPositions(c: Coordinate , balls: List[Int]): String = "(and (ball_at ball_" + balls(0) + " " + coordinateToLocation(c) + ")" +
        " (ball_at ball_" + balls(1) + " " + coordinateToLocation(c) + ")" +
        " (ball_at ball_" + balls(2) + " " + coordinateToLocation(c) + "))"

    private def coordinateToLocation(c: Coordinate) = "loc_" + c.x + "_" + c.y

    def getBallSize(o: gmt.snowman.game.`object`.Object): String = o match {
        case SmallBall =>
            "small"
        case MediumBall =>
            "medium"
        case LargeBall =>
            "large"
    }
}
