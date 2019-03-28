package gmt.snowman.encoder

import gmt.planner.operation.BooleanVariable
import gmt.snowman.action.SnowmanAction
import gmt.snowman.encoder.EncodingDataSnowman.StateData
import gmt.snowman.level.Level

import scala.collection.immutable
import scala.collection.mutable.ListBuffer

object EncodingDataSnowman {
    case class StateData(stateNext: StateBase, actionsData: immutable.Seq[ActionData])

    object ActionData {
        val NO_BALL: Int = -1
    }

    case class ActionData(action: SnowmanAction, actionVariable: BooleanVariable, ballActionIndex: Int)
}

class EncodingDataSnowman(val level: Level) {

    var initialState: StateBase = _
    val statesData: ListBuffer[StateData] = ListBuffer.empty[StateData]
}
