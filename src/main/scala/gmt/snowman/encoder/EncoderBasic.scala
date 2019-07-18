package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.planner.solver.value.ValueBoolean
import gmt.snowman.action.{BallAction, SnowmanAction}
import gmt.snowman.encoder.EncodingDataSnowman.ActionData
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

protected case class EncoderBasic(override val level: Level, override val encoderOptions: EncoderBase.EncoderOptions) extends EncoderBase[StateBasic](level, encoderOptions) {

    override def createState(index: Int, encoding: Encoding, encodingData: EncodingDataSnowman): StateBasic = StateBasic(level, index)

    override protected def encodeCharacterState0(state0: StateBasic, encoding: Encoding): Unit = encodeCharacterState(state0, encoding)

    override def createBallAction(actionName: String, state: StateBasic, stateActionBall: StateBase.Ball, stateNext: StateBasic, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val expressions = ListBuffer.empty[Expression]

        val otherBallUnderVar = BooleanVariable(actionName + "_OBU")
        expressions.append(VariableDeclaration(otherBallUnderVar))
        expressions.append(ClauseDeclaration(Equivalent(otherBallUnderVar, otherBallUnder(state, stateActionBall))))

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall, shift)
        expressions.appendAll(updateBallSizeExpressions)

        val constantPre = ListBuffer(characterNextToBall(state, stateActionBall, shift),
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnderVar)),
            otherBallsInFrontLarger(state, stateActionBall, shift))

        if (level.snowmen > 1) {
            constantPre.append(noOtherTwoBallsUnder(state, stateActionBall))
        }

        val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            Implies(Not(otherBallUnderVar), moveCharacter(state, stateNext, shift)),
            Implies(otherBallUnderVar, equalCharacterVariables(state, stateNext)),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause)

        if (level.hasSnow) {
            constantEff.append(updateSnowVariables(state, stateActionBall, stateNext, shift))
        }

        val pre = And(constantPre: _*)
        val eff = And(constantEff: _*)

        (pre, eff, expressions)
    }

    override protected def encodeCharacterAction(actionName: String, state: StateBasic, stateNext: StateBasic, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsData: mutable.Buffer[EncodingDataSnowman.ActionData]): Unit = {
        val actionVariable = BooleanVariable(actionName + "_S" + state.timeStep + "S" + stateNext.timeStep)
        encoding.add(VariableDeclaration(actionVariable))
        actionVariables.append(actionVariable)

        actionsData.append(EncodingDataSnowman.ActionData(action, actionVariable, ActionData.NO_BALL))

        val pre = BooleanConstant(true)

        val constantEff = ListBuffer(moveCharacter(state, stateNext, action.shift),
            equalBallsVariables(state, stateNext))

        if (level.hasSnow) {
            constantEff.append(equalSnowVariables(state, stateNext))
        }

        val eff = And(constantEff.toList: _*)

        encoding.add(ClauseDeclaration(Equivalent(eff, actionVariable)))
        encoding.add(ClauseDeclaration(Implies(actionVariable, pre)))
    }

    override def decode(assignments: Seq[Assignment], encodingData: EncodingDataSnowman): DecodingData = {
        println(Report.generateReport(level, encodingData.initialState :: encodingData.statesData.map(f => f.stateNext).toList, assignments)) // TODO Remove println

        val assignmentsMap = assignments.map(f => (f.name, f.value)).toMap

        val actions = ListBuffer.empty[SnowmanAction]
        val actionsBalls = ListBuffer.empty[BallAction]

        for (stateData <- encodingData.statesData) {
            val actionData = stateData.actionsData.find(f => assignmentsMap(f.actionVariable.name).asInstanceOf[ValueBoolean].v).get

            actions.append(actionData.action)

            if (actionData.ballActionIndex != ActionData.NO_BALL) {
                actionsBalls.append(BallAction(actionData.action, actionData.ballActionIndex))
            }
        }

        DecodingData(actions.toList, level.balls, actionsBalls.toList)
    }

    private def characterNextToBall[A <: StateBase](state: A, stateActionBll: StateBase.Ball, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBll, state.character, -shift, AND)
    }

    private def moveCharacter[A <: StateBase](state: A, stateNext: A, shift: Coordinate): Clause = {
        applyShiftClause(state.character, stateNext.character, shift, AND)
    }

    private def equalSnowVariables(state: StateBase, stateNext: StateBase): Clause = {
        Operations.simplify(And((for ((s, sNext) <- state.snow.values.zip(stateNext.snow.values)) yield {
            Equivalent(s, sNext)
        }).toSeq: _*))
    }

    private def equalBallsVariables(state: StateBase, stateNext: StateBase): Clause = {
        And((for ((b, bNext) <- state.balls.zip(stateNext.balls)) yield {
            And(Equals(b.x, bNext.x), Equals(b.y, bNext.y), encodeBallEquals(b, bNext))
        }): _*)
    }
}
