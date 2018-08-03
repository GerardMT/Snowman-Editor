package gmt.snowman.encoder

import gmt.planner.encoder.{Encoder, EncoderResult, Encoding}
import gmt.planner.operation._
import gmt.snowman.level.`object`._
import gmt.snowman.level.{Coordinate, Level, `object`}

import scala.collection.mutable.ListBuffer


abstract class EncoderSnowman(level: Level) extends Encoder {

    override def encode(timeSteps: Int): EncoderResult = {
        val encoding = new Encoding

        val states = ListBuffer.empty[StateSnowman]

        val state0 = createState(level, 0)
        state0.addVariables(encoding)
        states.append(state0)

        encoding.add(Comment("Initial State"))

        encoding.add(Comment("S0 Character"))
        encoding.add(ClauseDeclaration(Equals(state0.player.x, IntegerConstant(level.player.c.x))))
        encoding.add(ClauseDeclaration(Equals(state0.player.y, IntegerConstant(level.player.c.y))))

        encoding.add(Comment("S0 Balls"))
        for ((b, levelBall) <- level.balls.indices.map(f => (state0.balls(f), level.balls(f)))) {
            encoding.add(ClauseDeclaration(Equals(b.x, IntegerConstant(levelBall.c.x))))
            encoding.add(ClauseDeclaration(Equals(b.y, IntegerConstant(levelBall.c.y))))
            encoding.add(ClauseDeclaration(Equals(b.size, IntegerConstant(getBallSize(levelBall.o)))))
        }

        encoding.add(Comment("S0 Snow"))
        for (s <- level.map.filter(f => f.o == Snow || f.o == PlayerSnow).map(f => state0.snow(f.c))) {
            encoding.add(ClauseDeclaration(s.get))
        }

        encoding.add(Comment("S0 Occupancy"))
        for (l <- level.map) {
            if (l.o == Wall || Object.isBall(l.o)) {
                encoding.add(ClauseDeclaration(state0.occupancy(l.c).get))
            } else {
                encoding.add(ClauseDeclaration(Not(state0.occupancy(l.c).get)))
            }
        }

        encoding.add(Comment("Middle States"))

        var stateAnt = state0

        for (timeStep <- 1 until timeSteps + 1) {
            val stateI =  new StateSnowman(level, timeStep)
            stateI.addVariables(encoding)
            states.append(stateI)

            encoding.add(Comment("Occupancy"))
            for (l <- level.map.iterator.filterNot(f => f.o == Wall)) {
                val ors = for (b <- stateI.balls) yield {
                    And(Equals(b.x, IntegerConstant(l.c.x)), Equals(b.y, IntegerConstant(l.c.y))) // TODO OPTIMITZAR Es pot optimitzar? Pq només es mou la bola que fa l'acció, no cal fer un conjunt d'ors
                }
                encoding.add(ClauseDeclaration(Equivalent(Or(ors: _*), stateI.occupancy(l.c).get)))
            }

            val actionsVariables = ListBuffer.empty[BooleanVariable]

            val namesCharacterActions = List("AVMCR", "AVMCL", "AVMCU", "AVMCD")
            val offsets = List(Coordinate(+1, 0), Coordinate(-1, 0), Coordinate(0, +1), Coordinate(0, -1))

            for ((offset, name) <- offsets.zip(namesCharacterActions)) {
                val (variables, expressions) = codifyCharacterAction(name, stateI, stateAnt, offset)
                encoding.addAll(expressions)
                actionsVariables.appendAll(variables)
            }

            val namesBallActions = List("AVMBR", "AVMBL", "AVMBU", "AVMBD")
            for ((offset, name) <- offsets.zip(namesBallActions)) {
                val (variables, expressions) = codifyBallActions(name, stateI, stateAnt, offset)
                encoding.addAll(expressions)
                actionsVariables.appendAll(variables)
            }


            encoding.add(Comment("EO Actions"))
            if (actionsVariables.length > 1) {
                encoding.addAll(Operations.getEO(actionsVariables, "EOA" + timeStep))
            } else {
                encoding.add(ClauseDeclaration(actionsVariables.head))
            }

            stateAnt = stateI
        }

        encoding.add(Comment("Goal"))

        EncoderResult(encoding, null) // TODO
    }

    private def codifyCharacterAction(name: String, state: StateSnowman, stateNext: StateSnowman, offset: Coordinate): (Seq[BooleanVariable], Seq[Expression]) = { // TODO Inline

    }

    private def codifyBallActions(name: String, state: StateSnowman, stateNext: StateSnowman, offset: Coordinate): (Seq[BooleanVariable], Seq[Expression]) = { // TODO Inline
        val expressions = ListBuffer.empty[Expression]

        val actionsVariables = ListBuffer.empty[BooleanVariable]

        for (((sAntB, sB), iB) <- state.balls.zip(stateNext.balls).zipWithIndex) {

            val actionVariable = BooleanVariable(name + "B" + iB + "S" + state.stateNumber + "S" + stateNext.stateNumber)

            val (eff, pre, returnExpressions) = createBallAction()
            expressions.appendAll(returnExpressions)

            expressions.append(ClauseDeclaration(Implies(eff, actionVariable)))
            expressions.append(ClauseDeclaration(Implies(actionVariable, pre)))
        }

        (actionsVariables, expressions)
    }

    def createBallAction(): (Clause, Clause, Seq[Expression])

    def moveCharacterActions: Seq[Expression]

    def createState(level: Level, index: Int): StateSnowman

    protected def getBallSize(o: `object`.Object): Int = o match {
        case SmallBall =>
            1
        case MediumBall =>
            2
        case LargeBall =>
            4
        case LargeMediumBall =>
            6
        case MediumSmallBall =>
            3
        case LargeMediumSmallBall =>
            7
    }

    protected def noWallInFront(state: StateSnowman, actionBallIndex: Int): Clause = { // TODO OPTIMITZACIO Es podria fer per coordenades

    }

    protected def noOtherBallsOver(state: StateSnowman, stateActionBall: StateSnowman.Ball): Clause = {
        val ors = for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            And(Equals(stateActionBall.x, b.x), Equals(stateActionBall.y, b.y), Greater(stateActionBall.size, b.size))
        }
        Or(ors: _*)
    }

    protected def otherBallUnder: Clause = ???

    protected def otherBallInFront: Clause = ???

    protected def otherBallsInFrontLarger: Clause = ???

    protected def moveBall: Clause = ???

    protected def equalCharacterVariables: Clause = ???

    protected def equalOtherBallsVariables: Clause = ???

    protected def updateSnowVariables: Clause = ???

    protected def characterPositionValid: Clause = ???
}
