package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation
import gmt.planner.operation.{BooleanVariable, IntegerVariable}
import gmt.snowman.action.SnowmanAction
import gmt.snowman.collection.SortedMap
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder.StateBase.Ball
import gmt.snowman.game.`object`.Object
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.immutable

object StateReachability {

    def apply(level: Level, timeStep: Int): StateReachability = {
        val reachabilityNodes = SortedMap.empty[Coordinate, BooleanVariable]
        val reachabilityWeights = SortedMap.empty[Coordinate, IntegerVariable]
        val reachabilityEdges = SortedMap.empty[(Coordinate, Coordinate), BooleanVariable]

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            reachabilityNodes.put(l.c, BooleanVariable("RN_X" + l.c.x + "Y" + l.c.y + "_S" + timeStep))
            reachabilityWeights.put(l.c, IntegerVariable("RW_X" + l.c.x + "Y" + l.c.y + "_S" + timeStep))

            for (shift <- SnowmanAction.ACTIONS.map(f => f.shift)) {
                val end = l.c + shift
                if (level.map.contains(end)) {
                    reachabilityEdges.put((l.c, end), BooleanVariable("RA_X" + l.c.x + "Y" + l.c.y + "TX" + end.x + "Y" + end.y  + "S" + timeStep))
                }
            }
        }

        new StateReachability(StateBase(level, timeStep), reachabilityNodes, reachabilityWeights, reachabilityEdges, CharacterModule(level, timeStep))
    }
}

class StateReachability private (override val timeStep: Int,
                                 override val balls: immutable.Seq[Ball],
                                 override val snow: SortedMap[Coordinate, BooleanVariable],
                                 override val occupancy: SortedMap[Coordinate, BooleanVariable],
                                 override val mediumBalls: IntegerVariable,
                                 override val largeBalls: IntegerVariable,
                                 val reachabilityNodes: SortedMap[Coordinate, BooleanVariable],
                                 val reachabilityWeights: SortedMap[Coordinate, IntegerVariable],
                                 val reachabilityEdges: SortedMap[(Coordinate, Coordinate), BooleanVariable],
                                 private val characterModule: CharacterModule)
    extends StateBase(timeStep,
        balls,
        snow,
        occupancy,
        mediumBalls,
        largeBalls) with CharacterInterface with VariableAdder {

    def this(stateBase: StateBase, reachabilityNodes: SortedMap[Coordinate, BooleanVariable], reachabilityWeights: SortedMap[Coordinate, IntegerVariable], reachabilityEdges: SortedMap[(Coordinate, Coordinate), BooleanVariable] ,characterModule: CharacterModule) {
        this(stateBase.timeStep, stateBase.balls, stateBase.snow, stateBase.occupancy, stateBase.mediumBalls, stateBase.largeBalls, reachabilityNodes, reachabilityWeights, reachabilityEdges, characterModule)
    }

    override val character: CharacterModule.Character = characterModule.character

    override def addVariables(encoding: Encoding, encoderOptions: EncoderOptions): Unit = {
        super.addVariables(encoding, encoderOptions)
        characterModule.addVariables(encoding, encoderOptions)

        for (v <- reachabilityNodes.values) {
            encoding.add(operation.VariableDeclaration(v))
        }
        for (v <- reachabilityWeights.values) {
            encoding.add(operation.VariableDeclaration(v))
        }
        for (v <- reachabilityEdges.values) {
            encoding.add(operation.VariableDeclaration(v))
        }
    }
}
