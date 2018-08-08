package gmt.snowman.encoder

import gmt.planner.encoder.{Encoding, EncodingData}
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.planner.solver.value.{ValueBoolean, ValueInteger}
import gmt.snowman.action.SnowmanAction
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

case class EncoderBasic(override val level: Level) extends EncoderBase[StateBasic, SnowmanAction](level) {

    override def createState(level: Level, timeStep: Int): StateBasic = StateBasic(level, timeStep)

    override def createBallAction(actionName: String, state: StateBasic, stateActionBall: StateBase.Ball, stateNext: StateBasic, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val otherBallUnderVar = BooleanVariable(actionName + "_OBU")

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall)

        val pre = And(characterNextToBall(state, stateActionBall, shift),
            noWallInFront(state, stateActionBall),
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnderVar)),
            otherBallsInFrontLarger(state, stateActionBall, shift))

        val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            Implies(Not(otherBallUnderVar), moveCharacter(state, stateNext, shift)),
            Implies(otherBallUnderVar, equalCharacterVariables(state, stateNext)),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause)

        if (level.hasSnow) {
            constantEff.append(updateSnowVariables(state, stateNext, shift))
        }

        val eff = And(constantEff.toList: _*)

        val expressions = List(VariableDeclaration(otherBallUnderVar),
            ClauseDeclaration(Equivalent(otherBallUnderVar, otherBallUnder(state, stateActionBall)))) ++
            updateBallSizeExpressions

        (pre, eff, expressions)
    }

    override def codifyReachability(state: StateBasic, encoing: Encoding): Unit = {}

    override protected def codifyCharacterAction(actionName: String, state: StateBasic, stateNext: StateBasic, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsData: mutable.Buffer[SnowmanEncodingData.ActionData]): Unit = {
        val actionVariable = BooleanVariable(actionName + "_S" + state.timeStep + "S" + stateNext.timeStep)
        encoding.add(VariableDeclaration(actionVariable))
        actionVariables.append(actionVariable)

        actionsData.append(SnowmanEncodingData.ActionData(action, actionVariable, 0))

        val pre = characterLocatoinValid(state, action.shift)

        val constantEff = ListBuffer(moveCharacter(state, stateNext, action.shift),
            equalBallsVariables(state, stateNext))

        if (level.hasSnow) {
            constantEff.append(equalSnowVariables(state, stateNext))
        }

        val eff = And(constantEff.toList: _*)

        encoding.add(ClauseDeclaration(Equivalent(eff, actionVariable)))
        encoding.add(ClauseDeclaration(Implies(actionVariable, pre)))
    }

    override def decode(assignments: Seq[Assignment], encodingData: SnowmanEncodingData): immutable.Seq[SnowmanAction] = {
        // TODO DEBUG
        println(Report.generateReport(level, encodingData.state0 :: encodingData.statesData.map(f => f.stateNext).toList, assignments))

        val assignmentsMap = assignments.map(f => (f.name, f.value)).toMap

        val actions = ListBuffer.empty[SnowmanAction]

        for (stateData <- encodingData.statesData) {

            val iterator = stateData.actionsData.iterator
            var found = false

            while (iterator.hasNext && !found) {
                val actionData = iterator.next()

                assignmentsMap.get(actionData.actionVariable.name) match {
                    case Some(a) =>
                        a match {
                            case ValueBoolean(true) =>
                                found = true
                                actions.append(actionData.action)
                            case _ =>
                        }
                    case None =>
                }
            }
        }

        actions.toList
    }

    private def characterNextToBall(state: StateBase, stateActionBll: StateBase.Ball, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBll.x, stateActionBll.y, state.character.x, state.character.y, -shift, and)
    }

    private def moveCharacter(state: StateBase, stateNext: StateBase, shift: Coordinate): Clause = {
        applyShiftClause(state.character.x, state.character.y, stateNext.character.x, stateNext.character.y, shift, and)
    }

    private def equalSnowVariables(state: StateBase, stateNext: StateBase): Clause = {
        Operations.simplify(And((for ((s, sNext) <- state.snow.values.zip(stateNext.snow.values)) yield {
            Equivalent(s, sNext)
        }).toSeq: _*))
    }

    private def equalBallsVariables(state: StateBase, stateNext: StateBase): Clause = {
        And((for ((b, bNext) <- state.balls.zip(stateNext.balls)) yield {
            And(Equals(b.x, bNext.x), Equals(b.y, bNext.y), Equals(b.size, bNext.size))
        }).toSeq: _*)
    }
}
