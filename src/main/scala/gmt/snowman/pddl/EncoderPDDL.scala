package gmt.snowman.pddl

import java.io.File

import gmt.snowman.action.SnowmanAction
import gmt.snowman.game.`object`.{Object, _}
import gmt.snowman.level.{Coordinate, Level}
import gmt.util.Files

object EncoderPDDL {

    case class TooManyBallsNotSupported() extends Exception()

    def encodeObjectFluents(level: Level): (String, String) = {
        val problem = StringBuilder.newBuilder

        val directions = List("right", "left", "up", "down")

        val levelName = level.name match {
            case Some(s) =>
                s
            case None =>
                "snowman_problem"
        }

        problem.append("(define (problem " + levelName + ")\n")
        problem.append("    (:domain snowman_object_fluents)\n")
        problem.append("    (:objects\n")

        problem.append(encodeObjectsBalls(level))
        problem.append(encodeObjectsLocations(level))

        problem.append("    )\n")
        problem.append("    (:init\n")

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o)); (o, d) <- SnowmanAction.CHARACTER_ACTIONS.map(f => f.shift).zip(directions) ) {
            level.map.get(l.c + o) match {
                case Some(l2) =>
                    if (Object.isPlayableArea(l2.o)) {
                        problem.append("        (= (next_" + d + " loc_" + l.c.x + "_" + l.c.y + ") loc_" + l2.c.x + "_" + l2.c.y + ")\n")
                    }
                case None =>
            }
        }

        problem.append("        (= (character_at) loc_" + level.character.c.x + "_" + level.character.c.y + ")\n")

        for ((l, i) <- level.balls.zipWithIndex) {
            problem.append("        (= (ball_at) ball_" + i + " loc_" + l.c.x + "_" + l.c.y + ")\n")
            problem.append(encodeBallSize(l.o, i))
        }

        problem.append(encodeInitSnow(level))
        problem.append(encodeInitOccupancy(level))

        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:goal\n")
        problem.append("        (and\n")
        problem.append("            (goal)\n")
        problem.append("        )\n")
        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:metric minimize (total-cost))\n")
        problem.append(")")

        val domain = Files.openTextFile(new File(getClass.getResource("/pddl/object-fluents/domain.pddl").toExternalForm))

        (domain, problem.toString())
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
        if (level.snowmen == 1) {
            domain.append("        (goal)\n")
        } else {
            for (s <- 0 until level.snowmen) {
                domain.append("        (goal_" + s + ")\n")
            }
        }
        domain.append("    )\n")
        domain.append("\n")
        domain.append("    (:functions\n")
        domain.append("        (total-cost) - number\n")
        domain.append("    )\n")

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o)); o <- SnowmanAction.CHARACTER_ACTIONS.map(f => f.shift)) {
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

        for (l0 <- level.map.values.filter(f => Object.isPlayableArea(f.o)); shift <- SnowmanAction.CHARACTER_ACTIONS.map(f => f.shift)) {
            (level.map.get(l0.c + shift), level.map.get(l0.c + shift + shift)) match {
                case (Some(l1), Some(l2)) =>
                    if (Object.isPlayableArea(l1.o) && Object.isPlayableArea(l2.o)) {
                        val ppos = "loc_" + l0.c.x + "_" + l0.c.y
                        val from = "loc_" + l1.c.x + "_" + l1.c.y
                        val to = "loc_" + l2.c.x + "_" + l2.c.y

                        for ((bI, otherBalls) <- level.balls.indices.map(f => (f, level.balls.indices.patch(f, Nil, 1).map(f => "ball_" + f)))) {
                            val b = "ball_" + bI

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
                            for (ob <- otherBalls.combinations(2)) {
                                domain.append("            (not (and (ball_at " + ob(0)  + " " + from + ") (ball_at " + ob(1) + " " + from + ")))")
                            }
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
                            domain.append(encodeEffectGoal("            ", level, bI, coordinateToLocation(l2.c)))
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
        problem.append("        (= (total-cost) 0)\n")
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
        problem.append("    (:metric minimize (total-cost))\n")

        problem.append(")")

        (domain.toString(), problem.toString())
    }

    private def encodeObjectsBalls(level: Level): String = {
        val encoding = StringBuilder.newBuilder

        for (i <- level.balls.indices) {
            encoding.append("        ball_" + i + " - ball\n")
        }

        encoding.toString()
    }

    private def encodeObjectsLocations(level: Level): String = {
        val encoding = StringBuilder.newBuilder

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            encoding.append("        loc_" + l.c.x + "_" + l.c.y + " - location\n")
        }

        encoding.toString()
    }

    private def encodeInitOccupancy(level: Level): String = {
        val encoding = StringBuilder.newBuilder

        for (l <- level.map.values.filter(f => Object.isBall(f.o))) {
            encoding.append("        (occupancy loc_" + l.c.x + "_" + l.c.y + ")\n")
        }

        encoding.toString()
    }

    private def encodeInitSnow(level: Level): String = {
        val encoding = StringBuilder.newBuilder

        for (l <- level.map.values.filter(f => Object.isSnow(f.o))) {
            encoding.append("        (snow loc_" + l.c.x + "_" + l.c.y + ")\n")
        }

        encoding.toString()
    }

    private def encodeBallSize(o: Object, index: Int): String = {
        "        (ball_size_" + getBallSize(o) + " ball_" + index + ")\n"
    }

    private def encodeGoal(level: Level): String = {
        val encoding = StringBuilder.newBuilder

        encoding.append("    (:goal\n")
        encoding.append("        (and\n")

        if (level.snowmen == 1) {
            encoding.append("            (goal)\n")
        } else {
            for (s <- 0 until level.snowmen) {
                encoding.append("            (goal_" + s + ")\n")
            }
        }
        encoding.append("        )\n")
        encoding.append("    )\n")
        encoding.append("\n")

        encoding.toString()
    }

    private def encodeEffectGoal(padding: String, level: Level, actionBall: Int, toLocation: String): String = {
        val goal = new StringBuilder

        if (level.snowmen == 1) {
            goal.append(padding + "(when\n")
            goal.append(padding + "    (and\n")
            goal.append(padding + "        " + level.balls.indices.patch(actionBall, Nil, 1).map(f => "(ball_at ball_" + f + " " + toLocation + ")").mkString(" ") + ")\n")
            goal.append(padding + "    (and\n")
            goal.append(padding + "        (goal)))\n")
        } else {
            for (s <- 0 until level.snowmen) {
                goal.append(padding + "(when\n")

                val padding2 = if (s != 0) {
                    "    "
                } else {
                    ""
                }

                if (s != 0) {
                    goal.append(padding + "    (and\n")
                    goal.append(padding + "        (goal_" + (s - 1) + ")\n")
                }

                goal.append(padding + padding2 + "    (or\n")
                for (snowmenBalls <- level.balls.indices.patch(actionBall, Nil, 1).combinations(2)) {
                    goal.append(padding + padding2 + "        (and " + snowmenBalls.map(f => "(ball_at ball_" + f + " " + toLocation + ")").mkString(" ") + ")\n")
                }
                goal.append(padding + padding2 + "    )\n")

                if (s != 0) {
                    goal.append(padding + "    )\n")
                }

                goal.append(padding + "    (and\n")
                goal.append(padding + "        (goal_" + s + ")))\n")
            }
        }

        goal.mkString
    }

    private def coordinateToLocation(c: Coordinate) = "loc_" + c.x + "_" + c.y

    def getBallSize(o: gmt.snowman.game.`object`.Object): String = o match {
        case SmallBall => "small"
        case MediumBall => "medium"
        case LargeBall => "large"
    }
}
