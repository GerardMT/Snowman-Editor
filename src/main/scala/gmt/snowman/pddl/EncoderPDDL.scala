package gmt.snowman.pddl

import java.io.File

import gmt.snowman.action.SnowmanAction
import gmt.snowman.game.`object`.{Object, _}
import gmt.snowman.level.{Coordinate, Level}
import gmt.util.Files

object EncoderPDDL {

    case class TooManyBallsNotSupported() extends Exception()

    val directions = List("right", "left", "up", "down")

    def encodeBasicAdl(level: Level): (String, String) = {
        val levelName = extractName(level.name)

        val domain = StringBuilder.newBuilder

        domain.append("(define (domain snowman_basic_adl_snowmen_" + level.snowmen + ")\n")
        domain.append("""
          |    (:requirements
          |        :typing
          |        :negative-preconditions
          |        :equality
          |        :disjunctive-preconditions
          |        :conditional-effects
          |        :action-costs
          |    )
          |
          |    (:types
          |        location direction ball - object
          |    )
          |
          |    (:predicates
          |        (snow ?l - location)
          |        (next ?from ?to - location ?dir - direction)
          |        (occupancy ?l - location)
          |        (character_at ?l - location)
          |        (ball_at ?b - ball ?l - location)
          |        (ball_size_small ?b - ball)
          |        (ball_size_medium ?b - ball)
          |        (ball_size_large ?b - ball)
          |        (goal)
          |   )
          |
          |    (:functions
          |        (total-cost) - number
          |    )
          |
          |    (:action move_character
          |
          |     :parameters
          |       (?from ?to - location ?dir - direction)
          |
          |     :precondition
          |        (and
          |            (next ?from ?to ?dir)
          |            (character_at ?from)
          |            (not (occupancy ?to)))
          |
          |     :effect
          |        (and
          |            (not (character_at ?from))
          |            (character_at ?to))
          |    )
          |
          |    (:action move_ball
          |
          |     :parameters
          |        (?b - ball ?ppos ?from ?to - location ?dir - direction)
          |
          |     :precondition
          |        (and
          |            (next ?ppos ?from ?dir)
          |            (next ?from ?to ?dir)
          |            (ball_at ?b ?from)
          |            (character_at ?ppos)
          |            (forall (?o - ball)
          |                (or
          |                    (= ?o ?b)
          |                    (or
          |                        (not (ball_at ?o ?from))
          |                        (or
          |                            (and
          |                                (ball_size_small ?b)
          |                                (ball_size_medium ?o))
          |                            (and
          |                                (ball_size_small ?b)
          |                                (ball_size_large ?o))
          |                            (and
          |                                (ball_size_medium ?b)
          |                                (ball_size_large ?o))))))
          |            (or
          |                (forall (?o - ball)
          |                    (or
          |                        (= ?o ?b)
          |                        (not (ball_at ?o ?from))))
          |                (forall (?o - ball)
          |                        (not (ball_at ?o ?to))))
          |            (forall (?o - ball)
          |                    (or
          |                        (not (ball_at ?o ?to))
          |                        (or
          |                            (and
          |                                (ball_size_small ?b)
          |                                (ball_size_medium ?o))
          |                            (and
          |                                (ball_size_small ?b)
          |                                (ball_size_large ?o))
          |                            (and
          |                                (ball_size_medium ?b)
          |                                (ball_size_large ?o))))))
          |     :effect
          |        (and
          |            (occupancy ?to)
          |            (not (ball_at ?b ?from))
          |            (ball_at ?b ?to)
          |            (when
          |                (forall (?o - ball)
          |                    (or
          |                        (= ?o ?b)
          |                        (not (ball_at ?o ?from))))
          |                (and
          |                    (not (character_at ?ppos))
          |                    (character_at ?from)
          |                    (not (occupancy ?from))))
          |            (not (snow ?to))
          |            (when
          |                (and
          |                    (snow ?to)
          |                    (ball_size_small ?b))
          |                (and
          |                    (not (ball_size_small ?b))
          |                    (ball_size_medium ?b)))
          |            (when
          |                (and
          |                    (snow ?to)
          |                    (ball_size_medium ?b))
          |                (and
          |                    (not (ball_size_medium ?b))
          |                    (ball_size_large ?b)))
          |            (increase (total-cost) 1))
          |    )
          |
          |    (:action goal
          |
          |     :parameters
          |""".stripMargin)

        domain.append("        (" + (0 until level.snowmen).map(f => "?b" + (f * 3) + " ?b" + (f * 3 + 1) + " ?b" + (f * 3 + 2) + " - ball ?p" + f + " - location").mkString(" ") + ")\n")

        domain.append("""
          |     :precondition
          |        (and
          |""".stripMargin)

        for (i <- 0 until level.snowmen * 3) {
            for (j <- i + 1 until level.snowmen * 3) {
                domain.append("            (not (= ?b" + i + " ?b" + j + "))\n")
            }
        }

        for (i <- 0 until level.snowmen) {
            domain.append("            (ball_at ?b" + (3 * i + 0) + " ?p" + i + ")\n")
            domain.append("            (ball_at ?b" + (3 * i + 1) + " ?p" + i + ")\n")
            domain.append("            (ball_at ?b" + (3 * i + 2) + " ?p" + i + ")\n")
        }

        domain.append("""        )
          |
          |     :effect
          |         (and (goal))
          |    )
          |)""".stripMargin)

        val problem = StringBuilder.newBuilder

        problem.append("(define (problem " + levelName + ")\n")
        problem.append("\n")
        problem.append("    (:domain snowman_basic_adl_snowmen_" + level.snowmen + ")\n")
        problem.append("\n")
        problem.append("    (:objects\n")
        for (d <- directions) {
            problem.append("        " + d + " - direction\n")
        }

        problem.append(encodeObjectsBalls(level))
        problem.append(encodeObjectsLocations(level))

        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:init\n")
        problem.append("        (= (total-cost) 0)\n")

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o)); (o, d) <- SnowmanAction.CHARACTER_ACTIONS.map(f => f.shift).zip(directions) ) {
            level.map.get(l.c + o) match {
                case Some(l2) =>
                    if (Object.isPlayableArea(l2.o)) {
                        problem.append("        (next loc_" + l.c.x + "_" + l.c.y + " loc_" + l2.c.x + "_" + l2.c.y + " " + d + ")\n")
                    }
                case None =>
            }
        }

        problem.append("        (character_at loc_" + level.character.c.x + "_" + level.character.c.y + ")\n")

        for ((l, i) <- level.balls.zipWithIndex) {
            problem.append("        (ball_at ball_" + i + " loc_" + l.c.x + "_" + l.c.y + ")\n")
            problem.append(encodeBallSize(l.o, i))
        }

        problem.append(encodeInitSnow(level))
        problem.append(encodeInitOccupancy(level))
        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:goal\n")
        problem.append("        (and (goal))\n")
        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:metric minimize (total-cost))\n")
        problem.append(")")

        (domain.toString(), problem.toString())
    }

    def encodeCheatingAdl(level: Level): (String, String) = {
        val levelName = extractName(level.name)

        val domain = StringBuilder.newBuilder

        domain.append("(define (domain snowman_cheating_adl_snowmen_" + level.snowmen + ")\n")
        domain.append("""
                        |    (:requirements
                        |        :typing
                        |        :negative-preconditions
                        |        :equality
                        |        :disjunctive-preconditions
                        |        :conditional-effects
                        |        :action-costs
                        |    )
                        |
                        |    (:types
                        |        location direction ball - object
                        |    )
                        |
                        |    (:predicates
                        |        (snow ?l - location)
                        |        (next ?from ?to - location ?dir - direction)
                        |        (occupancy ?l - location)
                        |        (ball_at ?b - ball ?l - location)
                        |        (ball_size_small ?b - ball)
                        |        (ball_size_medium ?b - ball)
                        |        (ball_size_large ?b - ball)
                        |        (goal)
                        |   )
                        |
                        |    (:functions
                        |        (total-cost) - number
                        |    )
                        |
                        |    (:action move_ball
                        |
                        |     :parameters
                        |        (?b - ball ?ppos ?from ?to - location ?dir - direction)
                        |
                        |     :precondition
                        |        (and
                        |            (next ?ppos ?from ?dir)
                        |            (next ?from ?to ?dir)
                        |            (ball_at ?b ?from)
                        |            (not (occupancy ?ppos))
                        |            (forall (?o - ball)
                        |                (or
                        |                    (= ?o ?b)
                        |                    (or
                        |                        (not (ball_at ?o ?from))
                        |                        (or
                        |                            (and
                        |                                (ball_size_small ?b)
                        |                                (ball_size_medium ?o))
                        |                            (and
                        |                                (ball_size_small ?b)
                        |                                (ball_size_large ?o))
                        |                            (and
                        |                                (ball_size_medium ?b)
                        |                                (ball_size_large ?o))))))
                        |            (or
                        |                (forall (?o - ball)
                        |                    (or
                        |                        (= ?o ?b)
                        |                        (not (ball_at ?o ?from))))
                        |                (forall (?o - ball)
                        |                        (not (ball_at ?o ?to))))
                        |            (forall (?o - ball)
                        |                    (or
                        |                        (not (ball_at ?o ?to))
                        |                        (or
                        |                            (and
                        |                                (ball_size_small ?b)
                        |                                (ball_size_medium ?o))
                        |                            (and
                        |                                (ball_size_small ?b)
                        |                                (ball_size_large ?o))
                        |                            (and
                        |                                (ball_size_medium ?b)
                        |                                (ball_size_large ?o))))))
                        |     :effect
                        |        (and
                        |            (occupancy ?to)
                        |            (not (ball_at ?b ?from))
                        |            (ball_at ?b ?to)
                        |            (when
                        |                (forall (?o - ball)
                        |                    (or
                        |                        (= ?o ?b)
                        |                        (not (ball_at ?o ?from))))
                        |                (and
                        |                    (not (occupancy ?from))))
                        |            (not (snow ?to))
                        |            (when
                        |                (and
                        |                    (snow ?to)
                        |                    (ball_size_small ?b))
                        |                (and
                        |                    (not (ball_size_small ?b))
                        |                    (ball_size_medium ?b)))
                        |            (when
                        |                (and
                        |                    (snow ?to)
                        |                    (ball_size_medium ?b))
                        |                (and
                        |                    (not (ball_size_medium ?b))
                        |                    (ball_size_large ?b)))
                        |            (increase (total-cost) 1))
                        |    )
                        |
                        |    (:action goal
                        |
                        |     :parameters
                        |""".stripMargin)

        domain.append("        (" + (0 until level.snowmen).map(f => "?b" + (f * 3) + " ?b" + (f * 3 + 1) + " ?b" + (f * 3 + 2) + " - ball ?p" + f + " - location").mkString(" ") + ")\n")

        domain.append("""
                        |     :precondition
                        |        (and
                        |""".stripMargin)

        for (i <- 0 until level.snowmen * 3) {
          for (j <- i + 1 until level.snowmen * 3) {
            domain.append("            (not (= ?b" + i + " ?b" + j + "))\n")
          }
        }

        for (i <- 0 until level.snowmen) {
          domain.append("            (ball_at ?b" + (3 * i + 0) + " ?p" + i + ")\n")
          domain.append("            (ball_at ?b" + (3 * i + 1) + " ?p" + i + ")\n")
          domain.append("            (ball_at ?b" + (3 * i + 2) + " ?p" + i + ")\n")
        }

        domain.append("""        )
                        |
                        |     :effect
                        |         (and (goal))
                        |    )
                        |)""".stripMargin)

        val problem = StringBuilder.newBuilder

        problem.append("(define (problem " + levelName + ")\n")
        problem.append("\n")
        problem.append("    (:domain snowman_cheating_adl_snowmen_" + level.snowmen + ")\n")
        problem.append("\n")
        problem.append("    (:objects\n")
        for (d <- directions) {
          problem.append("        " + d + " - direction\n")
        }

        problem.append(encodeObjectsBalls(level))
        problem.append(encodeObjectsLocations(level))

        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:init\n")
        problem.append("        (= (total-cost) 0)\n")

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o)); (o, d) <- SnowmanAction.CHARACTER_ACTIONS.map(f => f.shift).zip(directions) ) {
          level.map.get(l.c + o) match {
            case Some(l2) =>
              if (Object.isPlayableArea(l2.o)) {
                problem.append("        (next loc_" + l.c.x + "_" + l.c.y + " loc_" + l2.c.x + "_" + l2.c.y + " " + d + ")\n")
              }
            case None =>
          }
        }

        for ((l, i) <- level.balls.zipWithIndex) {
          problem.append("        (ball_at ball_" + i + " loc_" + l.c.x + "_" + l.c.y + ")\n")
          problem.append(encodeBallSize(l.o, i))
        }

        problem.append(encodeInitSnow(level))
        problem.append(encodeInitOccupancy(level))
        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:goal\n")
        problem.append("        (and (goal))\n")
        problem.append("    )\n")
        problem.append("\n")
        problem.append("    (:metric minimize (total-cost))\n")
        problem.append(")")

        (domain.toString(), problem.toString())
    }

    def encodeBasicAdlGrounded(level: Level): (String, String)= {
        val levelName = extractName(level.name)
        val domain = StringBuilder.newBuilder

        domain.append("(define (domain snowman_adl_grounded__" + levelName + ")\n")
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
                                domain.append("            (not (and (ball_at " + ob(0)  + " " + from + ") (ball_at " + ob(1) + " " + from + ")))\n")
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

                            // Once snowmen is build the top ball can not be pop
//                            domain.append("            (or\n")
//                            domain.append("                (not (or")
//
//                            for (o <- otherBalls) {
//                                domain.append("\n")
//                                domain.append("                    (ball_at " + o + " " + from + ")")
//                            }
//
//                            domain.append("))\n")
//                            domain.append("                (and")
//
//                            for (o <- otherBalls) {
//                                domain.append("\n")
//                                domain.append("                    (not (ball_at " + o + " " + to + "))")
//                            }
//                            domain.append("))\n")
                            // ----

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

                            if (level.snowmen == 1) {
                                domain.append("            (when\n")
                                domain.append("                (and\n")
                                domain.append("                    " + level.balls.indices.patch(bI, Nil, 1).map(f => "(ball_at ball_" + f + " " + coordinateToLocation(l2.c) + ")").mkString(" ") + ")\n")
                                domain.append("                (and\n")
                                domain.append("                    (goal)))\n")
                            } else {
                                for (s <- 0 until level.snowmen) {
                                    domain.append("            (when\n")

                                    val padding = if (s != 0) {
                                        "    "
                                    } else {
                                        ""
                                    }

                                    if (s != 0) {
                                        domain.append("                (and\n")
                                        domain.append("                    (goal_" + (s - 1) + ")\n")
                                    }

                                    domain.append(padding + padding + "                        (or\n")
                                    for (snowmenBalls <- level.balls.indices.patch(bI, Nil, 1).combinations(2)) {
                                        domain.append("            " + padding + "        (and " + snowmenBalls.map(f => "(ball_at ball_" + f + " " + coordinateToLocation(l2.c) + ")").mkString(" ") + ")\n")
                                    }
                                    domain.append(padding + padding + "                        )\n")

                                    if (s != 0) {
                                        domain.append("                )\n")
                                    }

                                    domain.append("                (and\n")
                                    domain.append("                    (goal_" + s + ")))\n")
                                }
                            }

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
                            domain.append("                (and\n")
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

        problem.append("(define (problem " + levelName + ")\n")
        problem.append("\n")
        problem.append("    (:domain snowman_adl_grounded__" + levelName + ")\n")
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
        problem.append("    (:goal\n")
        problem.append("        (and\n")

        if (level.snowmen == 1) {
            problem.append("            (goal)\n")
        } else {
            for (s <- 0 until level.snowmen) {
                problem.append("            (goal_" + s + ")\n")
            }
        }
        problem.append("        )\n")
        problem.append("    )\n")
        problem.append("\n")
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

    private def coordinateToLocation(c: Coordinate) = "loc_" + c.x + "_" + c.y

    def getBallSize(o: gmt.snowman.game.`object`.Object): String = o match {
        case SmallBall => "small"
        case MediumBall => "medium"
        case LargeBall => "large"
    }

    def extractName(name: Option[String]): String = name match {
        case Some(s) =>
            s
        case None =>
            "no_name"
    }
}
