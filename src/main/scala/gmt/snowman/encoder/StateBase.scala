package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation.{BooleanVariable, IntegerVariable, VariableDeclaration}
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder.StateBase.Ball
import gmt.snowman.encoder.StateBase.Character
import gmt.snowman.game.`object`._
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable
import scala.collection.mutable.ListBuffer

object StateBase {

    abstract class CoordinateVariables(val x: IntegerVariable, val y: IntegerVariable)
    case class Ball(override val x: IntegerVariable, override  val y: IntegerVariable, size: IntegerVariable) extends CoordinateVariables(x, y)
    case class Character(override val x: IntegerVariable, override  val y: IntegerVariable) extends CoordinateVariables(x, y)


    def apply(level: Level, timeStep: Int): StateBase = {
        val balls = ListBuffer.empty[Ball]
        val snow = SortedMap.empty[Coordinate, BooleanVariable]

        for (l <- level.map.values.filter(f => Object.isSnow(f.o))) {
            snow.put(l.c, BooleanVariable("S_X" + l.c.x + "Y" + l.c.y + "_S" + timeStep))
        }

        for(i <- level.balls.indices) {
            balls.append(Ball(IntegerVariable("B" + i + "_X_S" + timeStep), IntegerVariable("B" + i + "Y_S" + timeStep), IntegerVariable("B" + i + "T_S" + timeStep)))
        }

        new StateBase(timeStep, Character(IntegerVariable("C_X_S" + timeStep), IntegerVariable("C_Y_S" + timeStep)), balls.toList, snow, IntegerVariable("IMB_I" + timeStep), IntegerVariable("ILB_I" + timeStep))
    }
}

class StateBase (val timeStep: Int,
                 val character: Character,
                 val balls: immutable.Seq[Ball],
                 val snow: SortedMap[Coordinate, BooleanVariable],
                 val mediumBalls: IntegerVariable,
                 val largeBalls: IntegerVariable) extends VariableAdder {

    override def addVariables(encoding: Encoding, encoderOptions: EncoderOptions): Unit = {
        encoding.add(VariableDeclaration(character.x))
        encoding.add(VariableDeclaration(character.y))

        for (b <- balls) {
            encoding.add(VariableDeclaration(b.x))
            encoding.add(VariableDeclaration(b.y))
            encoding.add(VariableDeclaration(b.size))
        }

        for (v <- snow.values) {
            encoding.add(VariableDeclaration(v))
        }

        if (encoderOptions.invariantBallSizes) {
            encoding.add(VariableDeclaration(mediumBalls))
            encoding.add(VariableDeclaration(largeBalls))
        }
    }
}
