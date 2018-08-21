package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation.{BooleanVariable, IntegerVariable}
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder.StateBase.Ball
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable

object StateBasic {

    def apply(level: Level, timeStep: Int): StateBasic = {
        new StateBasic(StateBase(level, timeStep), CharacterModule(level, timeStep))
    }
}

class StateBasic private (override val timeStep: Int,
                          override val balls: immutable.Seq[Ball],
                          override val snow: SortedMap[Coordinate, BooleanVariable],
                          override val occupancy: SortedMap[Coordinate, BooleanVariable],
                          override val mediumBalls: IntegerVariable,
                          override val largeBalls: IntegerVariable,
                          private val characterModule: CharacterModule)
    extends StateBase(timeStep,
        balls,
        snow,
        occupancy,
        mediumBalls,
        largeBalls) with CharacterInterface {


    override val character: CharacterModule.Character = characterModule.character

    def this(stateBase: StateBase, characterModule: CharacterModule) = {
        this(stateBase.timeStep, stateBase.balls, stateBase.snow, stateBase.occupancy, stateBase.mediumBalls, stateBase.largeBalls, characterModule)
    }

    override def addVariables(encoding: Encoding, encoderOptions: EncoderOptions): Unit = {
        super.addVariables(encoding, encoderOptions)
        characterModule.addVariables(encoding, encoderOptions)
    }
}