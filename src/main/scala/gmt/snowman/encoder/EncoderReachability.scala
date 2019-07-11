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
                Equals(state.balls(b).x, IntegerConstant(p.c.x + level.width * p.c.y))
            }): _*)

            encoding.add(ClauseDeclaration(Implies(or, Not(state.reachabilityNodes.get(p.c).get))))
        }

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            val nodeStart = state.reachabilityNodes.get(l.c).get

            val ors = ListBuffer.empty[Clause]

            for (end <- SnowmanAction.ACTIONS.flatMap(f => level.map.get(l.c + f.shift)).filter(f => Object.isPlayableArea(f.o)).map(f => f.c)) {
                val edgeInverse = state.reachabilityEdges.get((end, l.c)).get

                ors.append(edgeInverse)

                encoding.add(ClauseDeclaration(Implies(edgeInverse, state.reachabilityNodes.get(end).get)))
                encoding.add(ClauseDeclaration(Implies(edgeInverse, Smaller(state.reachabilityWeights.get(end).get, state.reachabilityWeights.get(l.c).get))))
            }

            if (ors.nonEmpty) {
                encoding.add(ClauseDeclaration(Implies(And(Not(Equals(state.character.x, IntegerConstant(l.c.x + level.width * l.c.y))), nodeStart), Operations.simplify(Or(ors: _*)))))
            }
        }
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
        println(Report.generateReport(level, encodingData.initialState :: encodingData.statesData.map(f => f.stateNext).toList, assignments)) // TODO Remove println
        decodeTeleport(assignments, encodingData)
    }

    private def reachability(state: StateReachability, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        Or((for ((c, rn) <- flattenTuple(level.map.keys.map(f => (f, state.reachabilityNodes.get(f - shift))))) yield {
            And(Equals(stateActionBall.x, IntegerConstant(c.x + level.width * c.y)), rn)
        }).toSeq: _*)
    }

    private def teleportCharacterBall[T <: StateBase](stateActionBall: StateBase.Ball, stateNext: T): Clause = {
        Equals(stateNext.character.x, stateActionBall.x)
    }

    private def teleportCharacter[T <: StateBase](stateActionBall: StateBase.Ball, stateNext: T, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBall, stateNext.character, -shift)
    }
}