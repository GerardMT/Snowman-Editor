package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation.{BooleanVariable, IntegerVariable}
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder.StateBase.Ball
import gmt.snowman.encoder.StateBase.Character
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable

object StateBasic {

    def apply(level: Level, timeStep: Int): StateBasic = {
        new StateBasic(StateBase(level, timeStep))
    }
}

class StateBasic private (override val timeStep: Int,
                          override val character: Character,
                          override val balls: immutable.Seq[Ball],
                          override val snow: SortedMap[Coordinate, BooleanVariable])
    extends StateBase(timeStep,
        character,
        balls,
        snow) {

    def this(stateBase: StateBase) = {
        this(stateBase.timeStep, stateBase.character, stateBase.balls, stateBase.snow)
    }

    override def addVariables(encoding: Encoding, encoderOptions: EncoderOptions): Unit = {
        super.addVariables(encoding, encoderOptions)
    }
}