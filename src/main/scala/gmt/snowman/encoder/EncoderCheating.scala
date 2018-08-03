package gmt.snowman.encoder

import gmt.planner.action
import gmt.planner.encoder.{EncoderResult, EncodingData}
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.snowman.level.Level


class EncoderCheating(l: Level) extends EncoderSnowman(l) {

    override def encode(timeSteps: Int): EncoderResult = ???

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[action.Action] = ???

    override def createBallAction(): (Clause, Clause, Seq[Expression]) = {
        val otherBallUnderVar = otherBallUnder

        val pre = And(noWallInFront,
            noOtherBallsOver,
            Not(And(otherBallInFront, otherBallUnder)),
            otherBallsInFrontLarger,
            characterPositionValid)

        val eff = And(moveBall,
            equalOtherBallsVariables,
            updateSnowVariables)

        val expressions = List(VariableDeclaration(otherBallUnderVar))

        (pre, eff, expressions)
    }

    override def moveCharacterActions: Seq[Expression] = ???

    override def createState(level: Level, index: Int): StateSnowman = ???
}
