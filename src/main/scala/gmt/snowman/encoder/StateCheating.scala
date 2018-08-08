package gmt.snowman.encoder

import gmt.planner.operation.BooleanVariable
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.StateBase.Ball
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable

object StateCheating {

    def apply(level: Level, timeStep: Int): StateCheating = {
        new StateCheating(StateBase(level, timeStep))
    }
}

class StateCheating private (override val timeStep: Int,
                    override val character: StateBase.Character, // TODO REMOVE FOLLOW DOCUMENTATION
                    override val balls: immutable.Seq[Ball],
                    override val snow: SortedMap[Coordinate, BooleanVariable],
                    override val occupancy: SortedMap[Coordinate, BooleanVariable])
    extends StateBase(timeStep,
        character,
        balls,
        snow,
        occupancy) {

    def this(stateBase: StateBase) = {
        this(stateBase.timeStep, stateBase.character, stateBase.balls, stateBase.snow, stateBase.occupancy)
    }
}
