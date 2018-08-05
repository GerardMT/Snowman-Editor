package gmt.snowman.encoder

import gmt.planner.action
import gmt.planner.encoder.{Encoding, EncodingData}
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable.ListBuffer


class EncoderCheating(l: Level) extends EncoderSnowman(l) {

    override def createState(level: Level, timeStep: Int): StateSnowman = new StateSnowman(level, timeStep) // TODO

    override def createBallAction(actionName: String, state: StateSnowman, stateActionBall: StateSnowman.Ball, stateNext: StateSnowman, stateNextActionBall: StateSnowman.Ball, offset: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val (updateBallSizeClause, expressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall)

        val pre = And(noWallInFront(state, stateActionBall),
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, offset), otherBallUnder(state, stateActionBall))),
            otherBallsInFrontLarger(state, stateActionBall, offset),
            characterPositionValid(state))

        val eff = And(moveBall(stateActionBall, stateNextActionBall, offset),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause,
            updateSnowVariables(state, stateNext))

        (pre, eff, expressions)
    }

    override def codifyReachability(state: StateSnowman, encoing: Encoding): Unit = {}

    override protected def codifyCharacterAction(name: String, state: StateSnowman, stateNext: StateSnowman, offset: Coordinate, encoding: Encoding, actionVariables: ListBuffer[BooleanVariable]): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[action.Action] = ???
}
