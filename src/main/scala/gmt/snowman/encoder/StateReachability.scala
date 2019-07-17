package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation.{BooleanVariable, IntegerVariable, VariableDeclaration}
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.StateBase.{Ball, Character, CoordinateVariables}
import gmt.snowman.game.`object`.Object
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable

object StateReachability {

    def apply(level: Level, timeStep: Int): StateReachability = {
        val reachabilityNodes = SortedMap.empty[Coordinate, BooleanVariable]

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            reachabilityNodes.put(l.c, BooleanVariable("RN_X" + l.c.x + "Y" + l.c.y + "_S" + timeStep))
        }

        new StateReachability(StateBase(level, timeStep), reachabilityNodes, new CoordinateVariables(IntegerVariable("RT_X_S" + timeStep), IntegerVariable("RT_Y_S" + timeStep)))
    }
}

class StateReachability private (override val timeStep: Int,
                                 override val character: Character,
                                 override val balls: immutable.Seq[Ball],
                                 override val snow: SortedMap[Coordinate, BooleanVariable],
                                 val reachabilityNodes: SortedMap[Coordinate, BooleanVariable],
                                 val target: CoordinateVariables)
    extends StateBase(timeStep,
        character,
        balls,
        snow) with VariableAdder {

    def this(stateBase: StateBase, reachabilityNodes: SortedMap[Coordinate, BooleanVariable], target: CoordinateVariables) {
        this(stateBase.timeStep, stateBase.character, stateBase.balls, stateBase.snow, reachabilityNodes, target)
    }


    override def addVariables(encoding: Encoding): Unit = {
        super.addVariables(encoding)

        for (v <- reachabilityNodes.values) {
            encoding.add(VariableDeclaration(v))
        }

        encoding.add(VariableDeclaration(target.x))
        encoding.add(VariableDeclaration(target.y))
    }
}
