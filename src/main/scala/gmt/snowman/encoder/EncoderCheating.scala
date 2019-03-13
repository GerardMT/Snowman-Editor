package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.action.SnowmanAction
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

protected case class EncoderCheating(override val level: Level, override val encoderOptions: EncoderBase.EncoderOptions) extends EncoderBase[StateCheating](level, encoderOptions) {

    override def createState(level: Level, timeStep: Int): StateCheating = StateCheating(level, timeStep)

    override protected def encodeCharacterState0(state0: StateCheating, encoding: Encoding): Unit = {}

    override protected def encodeEmptyAction(state: StateCheating, stateNext: StateCheating, actionVariable: BooleanVariable, encoing: Encoding): (Clause, Clause) = {
        var eff = And(Equivalent(state.emptyAction, BooleanConstant(true)),
            equalBallsVariables(state, stateNext),
            equalSnowVariables(state, stateNext),
            equalCharacterVariables(state, stateNext))

        if (state.snow.nonEmpty) {
            eff = And(eff, equalSnowVariables(state, stateNext))
        }

        (BooleanConstant(true), eff)
    }

    override def createBallAction(actionName: String, state: StateCheating, stateActionBall: StateBase.Ball, stateNext: StateCheating, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val expressions = ListBuffer.empty[Expression]

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall, shift)
        expressions.appendAll(updateBallSizeExpressions)

        val pre = And(noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnder(state, stateActionBall))),
            otherBallsInFrontLarger(state, stateActionBall, shift),
            Not(state.emptyAction))

        val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause)

        if (level.hasSnow) {
            constantEff.append(updateSnowVariables(state, stateActionBall, stateNext, shift))
        }

        if (encoderOptions.invariantBallSizes && level.hasSnow) {
            constantEff.append(invariantBallSizes(state, stateActionBall, stateNext, stateNextActionBall))
        }

        if (encoderOptions.invariantBallLocations) {
            constantEff.append(invariantBallLocatoins(stateNext, stateNextActionBall))
        }

        val eff = And(constantEff.toList: _*)

        (pre, eff, expressions)
    }

    override def encodeReachability(state: StateCheating, encoding: Encoding): Unit = {}

    override protected def encodeCharacterAction(actionName: String, state: StateCheating, stateNext: StateCheating, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsState: mutable.Buffer[EncodingData.ActionData], declareActionVariable: Boolean): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): DecodingData = {
        println(Report.generateReport(level, encodingData.state0 :: encodingData.statesData.map(f => f.stateNext).toList, assignments)) // TODO Remove println
        decodeTeleport(assignments, encodingData)
    }

}
