package snowman.solver

import snowman.level.objects.Wall
import snowman.level.{Level, Position}
import snowman.solver.encoder.State
import snowman.solver.solver.Assignment
import snowman.solver.solver.value.{ValueBoolean, ValueInteger}

import scala.collection.mutable.ArrayBuffer

// TODO Deprecate / remove
object Report {

    private val CHAR_UNDEFINED = '-'
    private val CHAR_UNKNOWN = '?'

    def generateReport(level: Level, states: Seq[State], assignments: Seq[Assignment]): String = {
        val sb = new StringBuilder

        val assignmentsMap = assignments.map(f => (f.name, f)).toMap

        sb.append("ACTIONS " + (states.size - 1) + "\n\n")

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
            for (p <- level.map.iterator) {
                s.snow(p.c) match {
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

            sb.append("Occupied\n")
            val occupiedLines = ArrayBuffer.fill(level.height, level.width)(CHAR_UNDEFINED)

            for (p <- level.map.iterator) {
                val c = assignmentsMap.get(s.occupied(p.c).get.name) match {
                    case Some(Assignment(_, ValueBoolean(v))) =>
                        if (v) '1' else '0'
                    case None =>
                        CHAR_UNKNOWN
                }
                occupiedLines(p.c.y)(p.c.x) = c
            }

            occupiedLines.reverse.foreach(f => sb.append(f.mkString + "\n"))
            sb.append("\n")

            sb.append("Reachable\n")
            val reachableLines = ArrayBuffer.fill(level.height, level.width)(CHAR_UNDEFINED)

            for (p <- level.map.iterator) {
                s.reachableNodes(p.c) match {
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
        }

        sb.toString()
    }

    private def generateMap(level: Level, state: State, assignmentsMap: Map[String, Assignment]): String =  {
        val mapLines = ArrayBuffer.fill(level.height, level.width)(CHAR_UNDEFINED)

        // Walls
        for (p <- level.map.iterator) {
            p match {
                case Position(c, Wall) =>
                    mapLines(c.y)(c.x) = '#'
                case _ =>
                    mapLines(p.c.y)(p.c.x) = '\''
            }
        }

        for (p <- level.map.iterator) {
            state.snow(p.c) match {
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

        // Player
        val pXA = assignmentsMap.get(state.player.x.name)
        val pYA = assignmentsMap.get(state.player.y.name)

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
            val bTA = assignmentsMap.get(b.size.name)

            if(bXA.isDefined && bYA.isDefined && bTA.isDefined) {
                val bX = bXA.get match { case Assignment(_, ValueInteger(v)) => v }
                val bY = bYA.get match { case Assignment(_, ValueInteger(v)) => v }
                val bT = bTA.get match { case Assignment(_, ValueInteger(v)) => v }

                val c = mapLines(bY)(bX)
                if (c.isDigit) {
                    mapLines(bY)(bX) = (c.asDigit + bT).toString.charAt(0)
                } else {
                    mapLines(bY)(bX) = bT.toString.charAt(0)
                }
            }
        }

        val sb = new StringBuilder
        mapLines.reverse.foreach(f => sb.append(f.mkString + "\n"))

        sb.toString()
    }

    def translateClause(c: Clause): String = {
        c match {
            case Not(c1) =>
                "!" + translateClause(c1)
            case And() | And(_) =>
                throw new IllegalStateException
            case And(c1, c2@_*) =>
                "(" + translateClause(c1) + c2.map(f => " & " + translateClause(f)).mkString + ")"
            case Or() | Or(_) =>
                throw new IllegalStateException
            case Or(c1, c2@_*) =>
                translateClause(c1) + c2.map(f => " | " + translateClause(f)).mkString + ")"
            case Implication(c1, c2) =>
                "(" + translateClause(c1) + " -> " + translateClause(c2) + ")"
            case BooleanVariable(name) =>
                name
            case BooleanConstant(value) =>
                if (value) {
                    "true"
                } else {
                    "false"
                }
            case Equals(c1, c2) =>
                "(" + translateClause(c1) + " = " + translateClause(c2) + ")"
            case Add(c1, c2) =>
                "(" + translateClause(c1) + " + " + translateClause(c2) + ")"
            case Sub(c1, c2) =>
                "( " + translateClause(c1) + " - " + translateClause(c2) + ")"
            case Smaller(c1, c2) =>
                "( " + translateClause(c1) + " < " + translateClause(c2) + ")"
            case Greater(c1, c2) =>
                "( " + translateClause(c1) + " > " + translateClause(c2) + ")"
            case IntegerVariable(name) =>
                name
            case IntegerConstant(value) =>
                value.toString
        }
    }
}

