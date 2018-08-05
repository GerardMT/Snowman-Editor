package gmt.snowman.encoder

import gmt.planner.action.Action
import gmt.planner.encoder.{Encoding, EncodingData}
import gmt.planner.operation
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.level.`object`._
import gmt.snowman.level.{Coordinate, Level}

import scala.collection.mutable.ListBuffer

class EncoderReachability(level: Level) extends EncoderSnowman(level) {

    override def createState(level: Level, timeStep: Int): StateSnowman = new StateSnowman(level, timeStep) // TODO

    override protected def codifyReachability(state: StateSnowman, encoing: Encoding): Unit = {
        encoing.add(Comment("Reachability"))
        for (p <- level.map.filterNot(f => f.o == Wall)) {
            val or = Or((for (b <- level.balls.indices) yield {
                And(operation.Equals(state.balls(b).x, IntegerConstant(p.c.x)), operation.Equals(state.balls(b).y, IntegerConstant(p.c.y)))
            }).toSeq: _*)

            encoing.add(ClauseDeclaration(Implies(or, operation.Not(state.reachableNodes(p.c).get))))
        }

        for (l <- level.map.filter(f => f.o != Wall)) {
            val nodeStart = state.reachableNodes(l.c).get

            val ors = ListBuffer.empty[Clause]

            for (end <- Level.OFFSETS.flatMap(f => level.map.get(l.c + f)).map(f => f.c)) {
                val edgeInverse = state.reachableEdges(end, l.c).get

                ors.append(edgeInverse)

                encoing.add(ClauseDeclaration(operation.Implies(edgeInverse, state.reachableNodes(end).get)))
                encoing.add(ClauseDeclaration(operation.Implies(edgeInverse, operation.Smaller(state.reachableNodesAcyclicity(end).get, state.reachableNodesAcyclicity(l.c).get))))
            }

            if (ors.nonEmpty) {
                encoing.add(ClauseDeclaration(Equivalent(Not(And(operation.Equals(state.character.x, IntegerConstant(l.c.x)), operation.Equals(state.character.y, IntegerConstant(l.c.y)))), operation.Implies(nodeStart, Operations.simplify(Or(ors: _*))))))
            }
        }
    }

    override def createBallAction(actionName: String, state: StateSnowman, stateActionBall: StateSnowman.Ball, stateNext: StateSnowman, stateNextActionBall: StateSnowman.Ball, offset: Coordinate): (Clause, Clause, Seq[Expression]) = {
        val otherBallUnderVar = BooleanVariable("S" + state.timeStep + "OBU")

        val (updateBallSizeClause, updateBallSizeExpressions) = updateBallSize(actionName, state, stateActionBall, stateNextActionBall)

        val pre = And(noWallInFront(state, stateActionBall),
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront(state, stateActionBall, offset), otherBallUnderVar)),
            otherBallsInFrontLarger(state, stateActionBall, offset),
            characterPositionValid(state),
            reachability)

        val eff = And(moveBall(stateActionBall, stateNextActionBall, offset),
            Implies(Not(otherBallUnderVar), teleportCharacter),
            Implies(otherBallUnderVar, equalCharacterVariables(state, stateNext)),
            equalOtherBallsVariables(state, stateActionBall, stateNext, stateNextActionBall),
            updateBallSizeClause,
            updateSnowVariables(state, stateNext))

        val expressions = List(VariableDeclaration(otherBallUnderVar),
            ClauseDeclaration(Equivalent(otherBallUnderVar, otherBallUnder(state, stateActionBall)))) ++
            updateBallSizeExpressions

        (pre, eff, expressions)
    }

    override def codifyCharacterAction(name: String, state: StateSnowman, stateNext: StateSnowman, offset: Coordinate, encoding: Encoding, actionVariables: ListBuffer[BooleanVariable]): Unit = {}

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[Action] = ???

    private def reachability(): Clause = ???

    private def teleportCharacter(): Clause = ???
}