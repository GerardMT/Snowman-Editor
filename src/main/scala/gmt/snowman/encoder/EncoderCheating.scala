package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.action.SnowmanAction
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

protected case class EncoderCheating(override val level: Level, override val encoderOptions: EncoderBase.EncoderOptions) extends EncoderBase[StateCheating](level, encoderOptions) {

    override def createState(index: Int, encoding: Encoding, encodingData: EncodingDataSnowman): StateCheating = StateCheating(level, index)

    override protected def encodeCharacterState0(state0: StateCheating, encoding: Encoding): Unit = {}

    override def createBallAction(actionName: String, state: StateCheating, stateActionBall: StateBase.Ball, stateNext: StateCheating, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val expressions = ListBuffer.empty[Expression]

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall, shift)
        expressions.appendAll(updateBallSizeExpressions)

        val pre = And(noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnder(state, stateActionBall))),
            otherBallsInFrontLarger(state, stateActionBall, shift))

        val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause)

        if (level.hasSnow) {
            constantEff.append(updateSnowVariables(state, stateActionBall, stateNext, shift))
        }

        val eff = And(constantEff.toList: _*)

        (pre, eff, expressions)
    }

    override protected def encodeCharacterAction(actionName: String, state: StateCheating, stateNext: StateCheating, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsState: mutable.Buffer[EncodingDataSnowman.ActionData]): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: EncodingDataSnowman): DecodingData = {
        println(Report.generateReport(level, encodingData.initialState :: encodingData.statesData.map(f => f.stateNext).toList, assignments)) // TODO Remove println
        decodeTeleport(assignments, encodingData)
    }

}
