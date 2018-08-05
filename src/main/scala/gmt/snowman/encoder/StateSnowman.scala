package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation.{BooleanVariable, IntegerVariable, VariableDeclaration}
import gmt.snowman.collection.SortedMap
import gmt.snowman.level.`object`.{Snow, Wall}
import gmt.snowman.level.{Coordinate, Level}
import gmt.planner.operation
import gmt.snowman.encoder.StateSnowman.{Ball, Character}

import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

object StateSnowman {
    case class Ball(x: IntegerVariable, y: IntegerVariable, size: IntegerVariable)
    case class Character(x: IntegerVariable, y: IntegerVariable)
}

class StateSnowman(l: Level, val timeStep: Int) {

    private val _reachableNodes = SortedMap.empty[Coordinate, BooleanVariable] // RN
    private val _reachableNodesAcyclicity = SortedMap.empty[Coordinate, IntegerVariable] // RA
    private val _reachableEdges = SortedMap.empty[(Coordinate, Coordinate), BooleanVariable] // RE

    private val _occupied = SortedMap.empty[Coordinate, BooleanVariable] // O // TODO Merge occupied and reachableNodes

    private var _balls = Vector.empty[Ball]

    private val _character = Character(IntegerVariable("PXS" + timeStep), IntegerVariable("PYS" + timeStep))

    private val _snow = SortedMap.empty[Coordinate, BooleanVariable] // S

    // Constructor
    // Variable creation
    for (p <- l.map.iterator) {
        p.o match {
            case Wall =>
            case _ =>
                _reachableNodes.put(p.c, BooleanVariable("RNX" + p.c.x + "Y" + p.c.y + "S" + timeStep))
                _reachableNodesAcyclicity.put(p.c, IntegerVariable("RAX" + p.c.x + "Y" + p.c.y + "S" + timeStep))

                for (cOffset <- Level.OFFSETS) {
                    val end = p.c + cOffset
                    if (l.map.contains(end)) {
                        _reachableEdges.put((p.c, end), BooleanVariable("RAX" + p.c.x + "Y" + p.c.y + "TX" + end.x + "Y" + end.y  + "S" + timeStep))
                    }
                }
        }
        p.o match {
            case Snow =>
                _snow.put(p.c, BooleanVariable("SX" + p.c.x + "Y" + p.c.y + "S" + timeStep))
            case _ =>
        }
        _occupied.put(p.c, BooleanVariable("OX" + p.c.x + "Y" + p.c.y + "S" + timeStep))
    }

    private val tmpBalls: ArrayBuffer[Ball] = ArrayBuffer.empty[Ball]

    for(i <- l.balls.indices) {
        val x = IntegerVariable("B" + i + "XS" + timeStep)
        val y = IntegerVariable("B" + i + "YS" + timeStep)
        val t = IntegerVariable("B" + i + "TS" + timeStep)
        tmpBalls.append(Ball(x, y, t))
    }

    _balls = tmpBalls.toVector

    def character: Character = _character

    def balls: immutable.Seq[Ball] = _balls

    def reachableNodes(c: Coordinate): Option[BooleanVariable] = _reachableNodes.get(c)

    def reachableNodesAcyclicity(c: Coordinate): Option[IntegerVariable] = _reachableNodesAcyclicity.get(c)

    def reachableEdges(start: Coordinate, end: Coordinate): Option[BooleanVariable] = _reachableEdges.get(start, end)

    def occupancy(c: Coordinate): Option[BooleanVariable] = _occupied.get(c)

    def snow(c: Coordinate): Option[BooleanVariable] = _snow.get(c)

    def addVariables(p: Encoding): Unit = {
        p.add(VariableDeclaration(character.x))
        p.add(operation.VariableDeclaration(character.y))

        for (b <- _balls) {
            p.add(operation.VariableDeclaration(b.x))
            p.add(operation.VariableDeclaration(b.y))
            p.add(operation.VariableDeclaration(b.size))
        }
        for (v <- _reachableNodes) {
            p.add(operation.VariableDeclaration(v))
        }
        for (v <- _reachableNodesAcyclicity) {
            p.add(operation.VariableDeclaration(v))
        }
        for (v <- _reachableEdges) {
            p.add(operation.VariableDeclaration(v))
        }
        for (v <- _occupied) {
            p.add(operation.VariableDeclaration(v))
        }
        for (v <- _snow) {
            p.add(operation.VariableDeclaration(v))
        }
    }
}
