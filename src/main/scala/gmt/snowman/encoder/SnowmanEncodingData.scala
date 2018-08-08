package gmt.snowman.encoder

import gmt.planner.encoder.EncodingData
import gmt.planner.operation.BooleanVariable
import gmt.snowman.action.SnowmanAction
import gmt.snowman.encoder.SnowmanEncodingData.StateData
import gmt.snowman.level.Level

import scala.collection.immutable

object SnowmanEncodingData {
    case class StateData(stateNext: StateBase, actionsData: immutable.Seq[ActionData])
    case class ActionData(action: SnowmanAction, actionVariable: BooleanVariable, ballActionIndex: Int)
}

case class SnowmanEncodingData(level: Level, state0: StateBase, statesData: immutable.Seq[StateData]) extends EncodingData
