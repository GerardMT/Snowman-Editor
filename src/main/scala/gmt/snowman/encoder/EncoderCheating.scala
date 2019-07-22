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

        val constantPre = ListBuffer(noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnder(state, stateActionBall))),
            otherBallsInFrontLarger(state, stateActionBall, shift))

        if (level.snowmen > 1) {
            constantPre.append(noOtherTwoBallsUnder(state, stateActionBall))
        }

        val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            equalOtherBallsPositions(state, stateActionBall, stateNext, stateNextActionBall))

        val pre = And(constantPre.toList: _*)
        val eff = And(constantEff.toList: _*)

        (pre, eff, expressions)
    }

    override protected def encodeCharacterAction(actionName: String, state: StateCheating, stateNext: StateCheating, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsState: mutable.Buffer[EncodingDataSnowman.ActionData]): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: EncodingDataSnowman): DecodingData = {
        print(Report.generateReport(level, encodingData.initialState :: encodingData.statesData.map(f => f.stateNext).toList, assignments)) // TODO Remove println
        decodeTeleport(assignments, encodingData)
    }
}
