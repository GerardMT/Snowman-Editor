package gmt.snowman.encoder

import gmt.planner.solver.Assignment
import gmt.planner.solver.value.{ValueBoolean, ValueInteger}
import gmt.snowman.game.`object`.Wall
import gmt.snowman.level.Level

import scala.collection.mutable.ArrayBuffer

object Report {

    private val CHAR_UNDEFINED = '-'
    private val CHAR_UNKNOWN = '?'

    case class DecodeException() extends Exception()

    def generateReport(level: Level, states: Seq[StateBase], assignments: Seq[Assignment]): String = {
        val sb = new StringBuilder

        val assignmentsMap = assignments.map(f => (f.name, f)).toMap

        sb.append("\nACTIONS " + (states.size - 1) + "\n\n")

        for ((s, i) <- states.zipWithIndex) {
            sb.append("STATE " + i + "\n")
            sb.append(generateMap(level, s, assignmentsMap))
            sb.append("\n")
        }

        sb.append("\nFULL REPORT\n")

        val snowLines = ArrayBuffer.fill(level.height, level.width)(CHAR_UNDEFINED)

        for ((s, i) <- states.zipWithIndex) {
            sb.append("STATE " + i + "\n")
            sb.append(generateMap(level, s, assignmentsMap))

            sb.append("\nSnow\n")
            for (p <- level.map.values) {
                s.snow.get(p.c) match {
                    case Some(v) =>
                        val c = assignmentsMap.get(v.name) match {
                            case Some(Assignment(_, ValueBoolean(value))) =>
                                if (value) '1' else '0'
                            case None =>
                                CHAR_UNKNOWN
                        }
                        snowLines(p.c.y)(p.c.x) = c
                    case None =>
                }
            }

            snowLines.reverse.foreach(f => sb.append(f.mkString + "\n"))
            sb.append("\n")

            s match {
                case sR: StateReachability =>
                    sb.append("Reachable\n")
                    val reachableLines = ArrayBuffer.fill(level.height, level.width)(CHAR_UNDEFINED)

                    for (p <- level.map.values) {
                        sR.reachabilityNodes.get(p.c) match {
                            case Some(r) =>
                                val c = assignmentsMap.get(r.name) match {
                                    case Some(Assignment(_, ValueBoolean(v))) =>
                                        if (v) '1' else '0'
                                    case None =>
                                        CHAR_UNKNOWN
                                }
                                reachableLines(p.c.y)(p.c.x) = c
                            case None =>
                                CHAR_UNKNOWN
                        }
                    }

                    reachableLines.reverse.foreach(f => sb.append(f.mkString + "\n"))
                    sb.append("\n")
                case _ =>
            }
        }

        sb.toString()
    }

    def generateMap(level: Level, state: StateBase, assignmentsMap: Map[String, Assignment]): String =  {
        val mapLines = ArrayBuffer.fill(level.height, level.width)(CHAR_UNDEFINED)

        // Walls
        for (p <- level.map.values) {
            p.o match {
                case Wall =>
                    mapLines(p.c.y)(p.c.x) = '#'
                case _ =>
                    mapLines(p.c.y)(p.c.x) = '\''
            }
        }

        for (p <- level.map.values) {
            state.snow.get(p.c) match {
                case Some(v) =>
                    val c = assignmentsMap.get(v.name) match {
                        case Some(Assignment(_, ValueBoolean(value))) =>
                            if (value) '1' else '0'
                        case None =>
                            CHAR_UNKNOWN
                    }
                    val cMap = if (c == '1') {
                        '.'
                    } else if (c == '0') {
                        '\''
                    } else {
                        c
                    }
                    mapLines(p.c.y)(p.c.x) = cMap
                case None =>
            }
        }

        // Character
        val pXA = assignmentsMap.get(state.character.x.name)
        val pYA = assignmentsMap.get(state.character.y.name)

        if(pXA.isDefined && pYA.isDefined) {
            val pX = pXA.get match { case Assignment(_, ValueInteger(v)) => v }
            val pY = pYA.get match { case Assignment(_, ValueInteger(v)) => v }

            if (mapLines(pY)(pX) == '.') {
                mapLines(pY)(pX) = 'q'
            } else {
                mapLines(pY)(pX) = 'p'
            }
        }

        // Balls
        for (b <- state.balls) {
            val bXA = assignmentsMap.get(b.x.name)
            val bYA = assignmentsMap.get(b.y.name)
            val bSA = assignmentsMap.get(b.sizeA.name)
            val bSB = assignmentsMap.get(b.sizeB.name)

            if(bXA.isDefined && bYA.isDefined && bSA.isDefined && bSB.isDefined) {
                val bX = bXA.get match { case Assignment(_, ValueInteger(v)) => v }
                val bY = bYA.get match { case Assignment(_, ValueInteger(v)) => v }
                val bA = bSA.get match { case Assignment(_, ValueBoolean(v)) => v }
                val bB = bSB.get match { case Assignment(_, ValueBoolean(v)) => v }

                val bS = if (!bA && !bB) {
                    1
                } else if (bA && !bB) {
                    2
                } else if (bA && bB) {
                    4
                } else {
                    throw DecodeException()
                }

                val c = mapLines(bY)(bX)
                if (c.isDigit) {
                    mapLines(bY)(bX) = (c.asDigit + bS).toString.charAt(0)
                } else {
                    mapLines(bY)(bX) = bS.toString.charAt(0)
                }
            }
        }

        val  sb = new StringBuilder
        mapLines.reverse.foreach(f => sb.append(f.mkString + "\n"))

        sb.toString()
    }
}

