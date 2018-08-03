package gmt.snowman.encoder

import gmt.planner.action.Action
import gmt.planner.encoder.EncodingData
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.level.Level


class EncoderBaisc(level: Level) extends EncoderSnowman(level) {

    override def createBallAction(state: StateSnowman, stateNext: StateSnowman, stateActionBall: StateSnowman.Ball, stateNextActionBall: StateSnowman.Ball): (Clause, Clause, Seq[Expression]) = {
        val otherBallUnderVar = otherBallUnder

        val pre = And(characterNextToBall,
            noWallInFront,
            noOtherBallsOver(state, stateActionBall),
            Not(And(otherBallInFront, otherBallUnderVar)),
            otherBallsInFrontLarger)

        val eff = And(moveBall,
            Implies(Not(otherBallUnderVar), moveCharacter),
            Implies(otherBallUnderVar, equalCharacterVariables),
            equalOtherBallsVariables,
            updateSnowVariables)

        val expressions = List(VariableDeclaration(otherBallUnderVar))

        (pre, eff, expressions)
    }


    private def characterNextToBall: Clause = ???

    private def moveCharacter: Clause = ???

    override def moveCharacterActions: Seq[Expression] = ???

    override def createState(level: Level, index: Int): StateSnowman = ???

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[Action] = ???
}
