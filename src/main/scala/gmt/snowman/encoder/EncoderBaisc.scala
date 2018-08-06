package gmt.snowman.encoder

import gmt.planner.action.Action
import gmt.planner.encoder.{Encoding, EncodingData}
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable.ListBuffer

class EncoderBaisc(level: Level) extends EncoderBase[StateBasic](level) {

    override def createState(level: Level, timeStep: Int): StateBasic = StateBasic(level, timeStep)

    override def createBallAction(actionName: String, state: StateBasic, stateActionBall: StateBase.Ball, stateNext: StateBasic, stateNextActionBall: StateBase.Ball, offset: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val otherBallUnderVar = BooleanVariable(actionName + "_OBU")

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall)

        val pre = And(characterNextToBall(state, stateActionBall, offset),
            noWallInFront(state, stateActionBall),
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, offset), otherBallUnderVar)),
            otherBallsInFrontLarger(state, stateActionBall, offset))

        val eff = And(moveBall(stateActionBall, stateNextActionBall, offset),
            Implies(Not(otherBallUnderVar), moveCharacter(state, stateNext, offset)),
            Implies(otherBallUnderVar, equalCharacterVariables(state, stateNext)),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause,
            updateSnowVariables(state, stateNext, offset))

        val expressions = List(VariableDeclaration(otherBallUnderVar),
            ClauseDeclaration(Equivalent(otherBallUnderVar, otherBallUnder(state, stateActionBall)))) ++
            updateBallSizeExpressions

        (pre, eff, expressions)
    }

    override def codifyReachability(state: StateBasic, encoing: Encoding): Unit = {}

    override def codifyCharacterAction(name: String, state: StateBasic, stateNext: StateBasic, offset: Coordinate, encoding: Encoding, actionVariables: ListBuffer[BooleanVariable]): Unit = {
        val actionVariable = BooleanVariable(name + "_S" + state.timeStep + "S" + stateNext.timeStep)
        actionVariables.append(actionVariable)

        val pre = characterPositionValid(state)

        val eff = moveCharacter(state, stateNext, offset)

        encoding.add(ClauseDeclaration(Equivalent(eff, actionVariable)))
        encoding.add(ClauseDeclaration(Implies(actionVariable, pre)))
    }

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[Action] = ???

    private def characterNextToBall(state: StateBase, stateActionBll: StateBase.Ball, offset: Coordinate): Clause = {
        applyOffsetClause(stateActionBll.x, stateActionBll.x, state.character.x, state.character.y, -offset, and)
    }

    private def moveCharacter(state: StateBase, stateNext: StateBase, offset: Coordinate): Clause = {
        applyOffsetClause(state.character.x, state.character.y, stateNext.character.x, stateNext.character.y, offset, and)
    }
}
