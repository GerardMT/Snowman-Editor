package gmt.snowman.encoder

import gmt.planner.encoder.Encoding
import gmt.planner.operation
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.action.SnowmanAction
import gmt.snowman.game.`object`._
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

protected case class EncoderReachability(override val level: Level, override val encoderOptions: EncoderBase.EncoderOptions) extends EncoderBase[StateReachability](level, encoderOptions) {

    override def createState(index: Int, encoding: Encoding, encodingData: EncodingDataSnowman): StateReachability = StateReachability(level, index)

    override protected def encodeCharacterState0(state0: StateReachability, encoding: Encoding): Unit = encodeCharacterState(state0, encoding)

    override protected def encodeReachability(state: StateReachability, encoding: Encoding): Unit = {
        encoding.add(Comment("Reachability"))
        for (p <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            val or = Or((for (b <- level.balls.indices) yield {
                And(Equals(state.balls(b).x, IntegerConstant(p.c.x)), Equals(state.balls(b).y, IntegerConstant(p.c.y)))
            }): _*)

            encoding.add(ClauseDeclaration(Implies(or, Not(state.reachabilityNodes(p.c)))))

            encoding.add(ClauseDeclaration(Implies(And(Equals(state.target.x, IntegerConstant(p.c.x)), Equals(state.target.y, IntegerConstant(p.c.y))), state.reachabilityNodes(p.c))))
            encoding.add(ClauseDeclaration(Implies(And(Equals(state.character.x, IntegerConstant(p.c.x)), Equals(state.character.y, IntegerConstant(p.c.y))), state.reachabilityNodes(p.c))))
        }

        for (p <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            val node = state.reachabilityNodes(p.c)
            val neighbours = SnowmanAction.CHARACTER_ACTIONS.flatMap(f => level.map.get(p.c + f.shift)).filter(f => Object.isPlayableArea(f.o)).map(f => f.c)

            val sourceOrTarget = Or(And(Equals(state.character.x, IntegerConstant(p.c.x)), Equals(state.character.y, IntegerConstant(p.c.y))), And(Equals(state.target.x, IntegerConstant(p.c.x)), Equals(state.target.y, IntegerConstant(p.c.y))))
            val sourceDifferentTarget = Or(Not(Equals(state.character.x, state.target.x)), Not(Equals(state.character.y, state.target.y)))

            if (neighbours.size == 1) {
                encoding.add(ClauseDeclaration(Implies(And(sourceOrTarget, sourceDifferentTarget), And(node, state.reachabilityNodes(neighbours.head)))))
            } else if (neighbours.size == 2) {
                val n1 = state.reachabilityNodes(neighbours(0))
                val n2 = state.reachabilityNodes(neighbours(1))

                encoding.add(ClauseDeclaration(Implies(And(sourceOrTarget, sourceDifferentTarget), And(node, Or(And(n1, Not(n2)), And(Not(n1), n2))))))
                encoding.add(ClauseDeclaration(Implies(And(Not(sourceOrTarget), node), Or(And(n1, n2), And(Not(n1), Not(n2))))))
            } else {
                val neighboursVars = neighbours.map(f => state.reachabilityNodes(f))
                val (c, e) = Operations.getEOLog(neighboursVars)
                encoding.addAll(e)

                val none = And(neighboursVars.map(f => Not(f)): _*)
                val two = neighboursVars.combinations(2).map(f => { val others = neighboursVars.diff(f); And(List(f(0), f(1)) ++ others.map(n => Not(n)): _*) }).toSeq

                encoding.add(ClauseDeclaration(Implies(And(sourceOrTarget, sourceDifferentTarget), c)))
                encoding.add(ClauseDeclaration(Implies(And(Not(sourceOrTarget), node), Or(two :+ none: _*))))
            }
        }

        encoding.add(ClauseDeclaration(Or((for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) yield {
            And(Equals(state.target.x, IntegerConstant(l.c.x)), Equals(state.target.y, IntegerConstant(l.c.y)))
        }).toSeq: _*)))
    }

    override def createBallAction(actionName: String, state: StateReachability, stateActionBall: StateBase.Ball, stateNext: StateReachability, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val expressions = ListBuffer.empty[Expression]

        val otherBallUnderVar = BooleanVariable(actionName + "_S" + state.timeStep + "OBU")
        expressions.append(VariableDeclaration(otherBallUnderVar))
        expressions.append(ClauseDeclaration(Equivalent(otherBallUnderVar, otherBallUnder(state, stateActionBall))))

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall, shift)
        expressions.appendAll(updateBallSizeExpressions)

        val pre = And(noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnderVar)),
            otherBallsInFrontLarger(state, stateActionBall, shift),
            reachability(state, stateActionBall, shift))

      val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            Implies(Not(otherBallUnderVar), teleportCharacterBall(stateActionBall, stateNext)),
            Implies(otherBallUnderVar, teleportCharacter(stateActionBall, stateNext, shift)),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause)

        if (level.hasSnow) {
            constantEff.append(updateSnowVariables(state, stateActionBall, stateNext, shift))
        }

        val eff = And(constantEff.toList: _*)

        (pre, eff, expressions)
    }

    override protected def encodeCharacterAction(actionName: String, state: StateReachability, stateNext: StateReachability, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsState: mutable.Buffer[EncodingDataSnowman.ActionData]): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: EncodingDataSnowman): DecodingData = {
        print(Report.generateReport(level, encodingData.initialState :: encodingData.statesData.map(f => f.stateNext).toList, assignments)) // TODO Remove println
        decodeTeleport(assignments, encodingData)
    }

    private def reachability(state: StateReachability, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBall, state.target, -shift, AND)
    }

    private def teleportCharacterBall[T <: StateBase](stateActionBall: StateBase.Ball, stateNext: T): Clause = {
        And(Equals(stateNext.character.x, stateActionBall.x), Equals(stateNext.character.y, stateActionBall.y))
    }

    private def teleportCharacter[T <: StateBase](stateActionBall: StateBase.Ball, stateNext: T, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBall, stateNext.character, -shift, AND)
    }
}