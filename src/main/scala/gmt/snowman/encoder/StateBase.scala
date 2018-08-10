package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation
import gmt.planner.operation.{BooleanVariable, IntegerVariable}
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.StateBase.Ball
import gmt.snowman.game.`object`._
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable
import scala.collection.mutable.ListBuffer

object StateBase {

    abstract class CoordinateVariables(val x: IntegerVariable, val y: IntegerVariable)
    case class Ball(override val x: IntegerVariable, override  val y: IntegerVariable, size: IntegerVariable) extends CoordinateVariables(x, y)

    def apply(level: Level, timeStep: Int): StateBase = {
        val balls = ListBuffer.empty[Ball]
        val occupancy =  SortedMap.empty[Coordinate, BooleanVariable]
        val snow = SortedMap.empty[Coordinate, BooleanVariable]

        for (l <- level.map.values) {
            occupancy.put(l.c, BooleanVariable("O_X" + l.c.x + "Y" + l.c.y + "S" + timeStep))
        }

        for (l <- level.map.values.filter(f => Object.isSnow(f.o))) {
            snow.put(l.c, BooleanVariable("S_X" + l.c.x + "Y" + l.c.y + "_S" + timeStep))
        }

        for(i <- level.balls.indices) {
            balls.append(Ball(IntegerVariable("B" + i + "_X_S" + timeStep), IntegerVariable("B" + i + "Y_S" + timeStep), IntegerVariable("B" + i + "T_S" + timeStep)))
        }

        new StateBase(timeStep, balls.toList, snow, occupancy)
    }
}

class StateBase (val timeStep: Int,
                 val balls: immutable.Seq[Ball],
                 val snow: SortedMap[Coordinate, BooleanVariable],
                 val occupancy: SortedMap[Coordinate, BooleanVariable]) extends VariableAdder {

    override def addVariables(encoding: Encoding): Unit = {
        for (b <- balls) {
            encoding.add(operation.VariableDeclaration(b.x))
            encoding.add(operation.VariableDeclaration(b.y))
            encoding.add(operation.VariableDeclaration(b.size))
        }
        for (v <- occupancy.values) {
            encoding.add(operation.VariableDeclaration(v))
        }
        for (v <- snow.values) {
            encoding.add(operation.VariableDeclaration(v))
        }
    }
}
