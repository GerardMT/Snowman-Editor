package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
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
                             override val balls: immutable.Seq[Ball],
                             override val snow: SortedMap[Coordinate, BooleanVariable],
                             override val occupancy: SortedMap[Coordinate, BooleanVariable])
    extends StateBase(timeStep,
        balls,
        snow,
        occupancy) with VariableAdder {

    def this(stateBase: StateBase) = {
        this(stateBase.timeStep, stateBase.balls, stateBase.snow, stateBase.occupancy)
    }

    override def addVariables(encoding: Encoding): Unit = {
        super.addVariables(encoding)
    }
}
