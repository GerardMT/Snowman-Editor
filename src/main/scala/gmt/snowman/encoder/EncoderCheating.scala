package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.action.SnowmanAction
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

case class EncoderCheating(override val level: Level) extends EncoderBase[StateCheating, DecodingData](level) {

    override def createState(level: Level, timeStep: Int): StateCheating = StateCheating(level, timeStep)

    override protected def encodeCharacterState0(state0: StateCheating, encoding: Encoding): Unit = {}

    override def createBallAction(actionName: String, state: StateCheating, stateActionBall: StateBase.Ball, stateNext: StateCheating, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val (updateBallSizeClause, expressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall, shift)

        val pre = And(noWallInFront(state, stateActionBall),
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnder(state, stateActionBall))),
            otherBallsInFrontLarger(state, stateActionBall, shift),
            characterLocatoinTeleportValid(state, stateActionBall, shift))

        val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause)

        if (level.hasSnow) {
            constantEff.append(updateSnowVariables(state, stateActionBall, stateNext, shift))
        }

        val eff = And(constantEff.toList: _*)

        (pre, eff, expressions)
    }

    override def encodeReachability(state: StateCheating, encoing: Encoding): Unit = {}

    override protected def encodeCharacterAction(actionName: String, state: StateCheating, stateNext: StateCheating, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsState: mutable.Buffer[SnowmanEncodingData.ActionData]): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: SnowmanEncodingData): Option[DecodingData] = {
        // TODO DEBUG
        println(Report.generateReport(level, encodingData.state0 :: encodingData.statesData.map(f => f.stateNext).toList, assignments))

        decodeTeleport(assignments, encodingData)
    }

}
