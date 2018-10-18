package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation.{BooleanVariable, IntegerVariable}
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder.StateBase.Ball
import gmt.snowman.encoder.StateBase.Character
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable

object StateCheating {

    def apply(level: Level, timeStep: Int): StateCheating = {
        new StateCheating(StateBase(level, timeStep))
    }
}

class StateCheating private (override val timeStep: Int,
                             override val character: Character,
                             override val balls: immutable.Seq[Ball],
                             override val snow: SortedMap[Coordinate, BooleanVariable],
                             override val mediumBalls: IntegerVariable,
                             override val largeBalls: IntegerVariable)
    extends StateBase(timeStep,
        character,
        balls,
        snow,
        mediumBalls,
        largeBalls) with VariableAdder {

    def this(stateBase: StateBase) = {
        this(stateBase.timeStep, stateBase.character, stateBase.balls, stateBase.snow, stateBase.mediumBalls, stateBase.largeBalls)
    }

    override def addVariables(encoding: Encoding, encoderOptions: EncoderOptions): Unit = {
        super.addVariables(encoding, encoderOptions)
    }
}
