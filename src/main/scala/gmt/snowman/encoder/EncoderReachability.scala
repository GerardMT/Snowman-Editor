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

case class EncoderReachability(override val level: Level, override val encoderOptions: EncoderBase.EncoderOptions) extends EncoderBase[StateReachability, DecodingData](level, encoderOptions) {

    override def createState(level: Level, timeStep: Int): StateReachability = StateReachability(level, timeStep)

    override protected def encodeCharacterState0(state0: StateReachability, encoding: Encoding): Unit = {}

    override protected def encodeReachability(state: StateReachability, encoding: Encoding): Unit = {
        encoding.add(Comment("Reachability"))
        for (p <- level.map.values.filterNot(f => f.o == Wall)) {
            val or = Or((for (b <- level.balls.indices) yield {
                And(operation.Equals(state.balls(b).x, IntegerConstant(p.c.x)), operation.Equals(state.balls(b).y, IntegerConstant(p.c.y)))
            }): _*)

            encoding.add(ClauseDeclaration(Implies(or, operation.Not(state.reachabilityNodes.get(p.c).get))))
        }

        for (l <- level.map.values.filter(f => f.o != Wall)) {
            val nodeStart = state.reachabilityNodes.get(l.c).get

            val ors = ListBuffer.empty[Clause]

            for (end <- SnowmanAction.ACTIONS.flatMap(f => level.map.get(l.c + f.shift)).filter(f => Object.isPlayableArea(f.o)).map(f => f.c)) {
                val edgeInverse = state.reachabilityEdges.get((end, l.c)).get

                ors.append(edgeInverse)

                encoding.add(ClauseDeclaration(operation.Implies(edgeInverse, state.reachabilityNodes.get(end).get)))
                encoding.add(ClauseDeclaration(operation.Implies(edgeInverse, operation.Smaller(state.reachabilityWeights.get(end).get, state.reachabilityWeights.get(l.c).get))))
            }

            if (ors.nonEmpty) {
                encoding.add(ClauseDeclaration(Equivalent(Not(And(operation.Equals(state.character.x, IntegerConstant(l.c.x)), operation.Equals(state.character.y, IntegerConstant(l.c.y)))), operation.Implies(nodeStart, Operations.simplify(Or(ors: _*))))))
            }
        }
    }

    override def createBallAction(actionName: String, state: StateReachability, stateActionBall: StateBase.Ball, stateNext: StateReachability, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val otherBallUnderVar = BooleanVariable(actionName + "_S" + state.timeStep + "OBU")

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall, shift)

        val pre = And(noWallInFront(state, stateActionBall, shift),
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, shift), otherBallUnderVar)),
            otherBallsInFrontLarger(state, stateActionBall, shift),
            characterLocationTeleportValid(state, stateActionBall, shift),
            reachability(state, stateActionBall, shift))

      val constantEff = ListBuffer(moveBall(stateActionBall, stateNextActionBall, shift),
            Implies(Not(otherBallUnderVar), teleportCharacter(stateActionBall, stateNext)),
            Implies(otherBallUnderVar, equalCharacterVariables(state, stateNext)),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause)

        if (level.hasSnow) {
            constantEff.append(updateSnowVariables(state, stateActionBall, stateNext, shift))
        }

        val eff = And(constantEff.toList: _*)

        val expressions = List(VariableDeclaration(otherBallUnderVar),
            ClauseDeclaration(Equivalent(otherBallUnderVar, otherBallUnder(state, stateActionBall)))) ++
            updateBallSizeExpressions

        (pre, eff, expressions)
    }

    override protected def encodeCharacterAction(actionName: String, state: StateReachability, stateNext: StateReachability, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsState: mutable.Buffer[EncodingData.ActionData]): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): DecodingData = decodeTeleport(assignments, encodingData)

    private def reachability(state: StateReachability, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        Or((for ((c, rn) <- flattenTuple(level.map.keys.map(f => (f, state.reachabilityNodes.get(f - shift))))) yield {
            And(Equals(stateActionBall.x, IntegerConstant(c.x)), Equals(stateActionBall.y, IntegerConstant(c.y)), rn)
        }).toSeq: _*)
    }

    private def teleportCharacter[T <: StateBase with CharacterInterface](stateActionBall: StateBase.Ball, stateNext: T): Clause = {
        And(Equals(stateNext.character.x, stateActionBall.x), Equals(stateNext.character.y, stateActionBall.y))
    }
}