package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation
import gmt.planner.operation.{BooleanVariable, VariableDeclaration}
import gmt.snowman.collection.SortedMap
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
                          private val characterModule: CharacterModule)
    extends StateBase(timeStep,
        balls,
        snow,
        occupancy) with CharacterInterface {


    override val character: CharacterModule.Character = characterModule.character

    def this(stateBase: StateBase, characterModule: CharacterModule) = {
        this(stateBase.timeStep, stateBase.balls, stateBase.snow, stateBase.occupancy, characterModule)
    }

    override def addVariables(encoding: Encoding): Unit = {
        super.addVariables(encoding)
        characterModule.addVariables(encoding)
    }
}