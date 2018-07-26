package gmt.snowman.solver

import java.io.File

import gmt.snowman.encoder.{EncoderBaisc, EncoderCheating, EncoderReachability, EncoderSnowman}
import gmt.snowman.level.Level
import gmt.planner.planner.Planner
import gmt.planner.solver.Yices2Solver
import gmt.planner.translator.SMTLib2
import gmt.snowman.level.`object`._
import gmt.ui.Settings

object SnowmanSolver {

    def solveWithBasicEncoding(l: Level, encoding: EncoderSnowman): SnowmanSolverResult = {
        val solver = new Planner(Settings.read(new File("tmp")))

        solver.solve(new EncoderBaisc(l), SMTLib2, new Yices2Solver("tmp"))

        // Assigments obtinguts
        // Falten states. Callback?

        throw new NotImplementedError()
    }

    def solveWithCheatingEncoding(l: Level, encoding: EncoderSnowman): SnowmanSolverResult = {
        val solver = new Planner(Settings.read(new File("tmp")))

        solver.solve(new EncoderCheating(l), SMTLib2, new Yices2Solver("tmp"))

        throw new NotImplementedError()
    }

    def solveWithReachabilityEncoding(l: Level, encoding: EncoderSnowman): SnowmanSolverResult = {
        val solver = new Planner(Settings.read(new File("tmp")))

        solver.solve(new EncoderReachability(l), SMTLib2, new Yices2Solver("tmp"))

        throw new NotImplementedError()
    }

    def solvePddl(level: Level): SnowmanSolverResult = {
        var encoding = ""

        val directoins = List("dir-right", "dir-left", "dir-up", "dir-down")

        encoding += "(define (problem snowman-problem)\n"
        encoding += "(:domain sowman-domain)\n"
        encoding += "(:objects\n"
        for (d <- directoins) {
            encoding += "    " + d + " - direction\n"
        }

        for (i <- 0 to level.balls.size) {
            encoding += "    ball-" + i + " - ball\n"
        }

        for (l <- level.map.filter(f => f.o != Wall)) {
            encoding += "    loc-" + l.c.x + "-" + l.c.y + " - location\n"
        }

        encoding += ")\n"
        encoding += "(:init\n"

        for (l <- level.map.filter(f => f.o != Wall); t <- Level.cOffsets.zip(directoins) ) {
            level.map.get(l.c + t._1) match {
                case Some(l2) =>
                    encoding += "    (next loc-" + l.c.x + "-" + l.c.y + " loc-" + l2.c.x + "-" + l2.c.y + " " + t._2 + ")\n"
                case None =>
            }
        }

        encoding += "    (character-at loc-" + level.player.c.x + "-" + level.player.c.y + ")\n"

        for (l <-level.balls) {
            encoding += "    (ball-at loc-" + l.c.x + "-" + l.c.y + ")\n"
            encoding += "    (ball-size " + getBallSize(l.o) + ")\n"
        }

        for (l <- level.map.filter(f => f.o == Snow)) {
            encoding += "    (snow loc-" + l.c.x + "-" + l.c.y + ")\n"
        }

        encoding += "(:goal (exist (?pos - position)\n"
        encoding += "    (and\n"
        for (i <- 0 to level.balls.size) {
            encoding += "        (ball-at ball-" + i + ")\n"
        }
        encoding += "    )\n"
        encoding += "))\n"

        encoding += ")\n"

        println(encoding)

        SnowmanSolverResult(false, List()) // TODO
    }

    private def getBallSize(o: gmt.snowman.level.`object`.Object): String = o match {
        case SmallBall =>
            "small"
        case MediumBall =>
            "medium"
        case LargeBall =>
            "large"
    }
}
