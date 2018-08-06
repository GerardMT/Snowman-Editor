package gmt.snowman.encoder

import gmt.planner.encoder.{Encoder, EncoderResult, Encoding}
import gmt.planner.operation
import gmt.planner.operation._
import gmt.snowman.level.`object`._
import gmt.snowman.level.{Coordinate, Level, `object`}

import scala.collection.mutable.ListBuffer

abstract class EncoderBase[A <: StateBase](level: Level) extends Encoder {

    override def encode(timeSteps: Int): EncoderResult = {
        val encoding = new Encoding

        val states = ListBuffer.empty[A]

        val state0 = createState(level, 0)
        state0.addVariables(encoding)
        states.append(state0)

        encoding.add(Comment("Initial State"))

        encoding.add(Comment("S0 Character"))
        encoding.add(ClauseDeclaration(Equals(state0.character.x, IntegerConstant(level.player.c.x))))
        encoding.add(ClauseDeclaration(Equals(state0.character.y, IntegerConstant(level.player.c.y))))

        encoding.add(Comment("S0 Balls"))
        for ((b, levelBall) <- level.balls.indices.map(f => (state0.balls(f), level.balls(f)))) {
            encoding.add(ClauseDeclaration(Equals(b.x, IntegerConstant(levelBall.c.x))))
            encoding.add(ClauseDeclaration(Equals(b.y, IntegerConstant(levelBall.c.y))))
            encoding.add(ClauseDeclaration(Equals(b.size, IntegerConstant(getBallSize(levelBall.o)))))
        }

        encoding.add(Comment("S0 Snow"))
        for (s <- state0.snow.values) {
            encoding.add(ClauseDeclaration(s))
        }

        encoding.add(Comment("S0 Occupancy"))
        for (l <- level.map.values) {
            if (l.o == Wall || Object.isBall(l.o)) {
                encoding.add(ClauseDeclaration(state0.occupancy.get(l.c).get))
            } else {
                encoding.add(ClauseDeclaration(Not(state0.occupancy.get(l.c).get)))
            }
        }

        // TODO OCCUPANCY VARIABLES UPDATE WALLS

        encoding.add(Comment("Middle States"))

        var state = state0

        for (timeStep <- 1 until timeSteps + 1) {
            val stateNext =  createState(level, timeStep)
            stateNext.addVariables(encoding)
            states.append(stateNext)

            encoding.add(Comment("Occupancy"))
            for (l <- level.map.values.filterNot(f => f.o == Wall)) {
                val ors = for (b <- stateNext.balls) yield {
                    And(Equals(b.x, IntegerConstant(l.c.x)), Equals(b.y, IntegerConstant(l.c.y))) // TODO OPTIMITZAR Es pot optimitzar? Pq només es mou la bola que fa l'acció, no cal fer un conjunt d'ors
                }
                encoding.add(ClauseDeclaration(Equivalent(Or(ors: _*), stateNext.occupancy.get(l.c).get)))
            }

            // TODO OCCUPANCY VARIABLES UPDATE WALLS

            codifyReachability(stateNext, encoding)

            // TODO Invariants

            val actionsVariables = ListBuffer.empty[BooleanVariable]

            val offsets = List(Coordinate(+1, 0), Coordinate(-1, 0), Coordinate(0, +1), Coordinate(0, -1))

            val namesCharacterActions = List("AVMCR", "AVMCL", "AVMCU", "AVMCD")

            for ((offset, name) <- offsets.zip(namesCharacterActions)) {
                codifyCharacterAction(name, state, stateNext, offset, encoding, actionsVariables)
            }

            val namesBallActions = List("AVMBR", "AVMBL", "AVMBU", "AVMBD")

            for ((offset, name) <- offsets.zip(namesBallActions)) {
                for (((stateActionBall, stateNextActionBall), iB) <- state.balls.zip(stateNext.balls).zipWithIndex) {
                    val actionName = name + "_B" + iB + "_S" + state.timeStep + "S" + stateNext.timeStep

                    val actionVariable = BooleanVariable(actionName)
                    actionsVariables.append(actionVariable)

                    val (eff, pre, returnExpressions) = createBallAction(actionName, state, stateActionBall, stateNext, stateNextActionBall, offset)
                    encoding.addAll(returnExpressions)

                    encoding.add(ClauseDeclaration(Equivalent(eff, actionVariable)))
                    encoding.add(ClauseDeclaration(Implies(actionVariable, pre)))
                }
            }


            encoding.add(Comment("EO Actions"))
            if (actionsVariables.length > 1) {
                encoding.addAll(Operations.getEO(actionsVariables, "EOA" + timeStep))
            } else {
                encoding.add(ClauseDeclaration(actionsVariables.head))
            }

            state = stateNext
        }

        encoding.add(Comment("Goal")) // TODO New goal
        val combinations = state.balls.combinations(3).toList

        val combinationsVariables = ListBuffer.empty[BooleanVariable]

        if (combinations.size > 1) {
            for ((c, i) <- combinations.zipWithIndex) {
                val v = BooleanVariable("C" + i)

                encoding.add(Comment("Combination " + i))
                combinationsVariables.append(v)
                encoding.add(VariableDeclaration(v))
                encoding.add(ClauseDeclaration(Equivalent(And(operation.Equals(c(1).x, c.head.x), operation.Equals(c(1).y, c.head.y), operation.Equals(c(2).x, c.head.x), operation.Equals(c(2).y, c.head.y)), v)))
            }

            encoding.addAll(Operations.addEK(combinationsVariables, level.balls.size / 3, "EKC"))
        } else {
            val c = combinations.head
            c.tail.foreach(f => encoding.add(ClauseDeclaration(And(operation.Equals(f.x, c.head.x), operation.Equals(f.y, c.head.y)))))
        }

        EncoderResult(encoding, null) // TODO
    }

    protected def createState(level: Level, index: Int): A

    protected def codifyReachability(state: A, encoing: Encoding)

    protected def codifyCharacterAction(actionName: String, state: A, stateNext: A, offset: Coordinate, encoding: Encoding, actionVariables: ListBuffer[BooleanVariable])

    protected def createBallAction(actoinName: String, state: A, stateActionBall: StateBase.Ball, stateNext: A, stateNextActionBall: StateBase.Ball, offset: Coordinate): (Clause, Clause, Seq[Expression])

    protected def noWallInFront(state: StateBase, stateActionBall: StateBase.Ball): Clause = { // TODO OPTIMITZACIO Es podria fer per coordenades
        And((for (l <- level.map.values.filter(f => f.o == Wall)) yield {
            Or(Not(Equals(stateActionBall.x, IntegerConstant(l.c.x))), Not(Equals(stateActionBall.y, IntegerConstant(l.c.y))))
        }).toSeq : _*) // TODO Fer alternativa inGrass
    }

    protected def noOtherBallsOver(state: StateBase, stateActionBall: StateBase.Ball): Clause = {
        And((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            Or(Not(Equals(stateActionBall.x, b.x)), Not(Equals(stateActionBall.y, b.y)), Smaller(stateActionBall.size, b.size))
        }): _*)
    }

    protected def otherBallUnder(state: StateBase, stateActionBall: StateBase.Ball): Clause = {
        Or((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            And(Equals(stateActionBall.x, b.x), Equals(stateActionBall.y, b.y), Smaller(stateActionBall.size, b.size))
        }): _*)
    }

    protected def otherBallInFront(state: StateBase, stateActionBall: StateBase.Ball, offset: Coordinate): Clause = {
        Or((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            applyOffsetClause(stateActionBall.x, stateActionBall.y, b.x, b.y, offset, and)
        }) : _*)
    }

    protected def otherBallsInFrontLarger(state: StateBase, stateActionBall: StateBase.Ball, offset: Coordinate): Clause = {
        And((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            Or(applyOffsetClause(stateActionBall.x, stateActionBall.y, b.x, b.y, offset, or), Smaller(stateActionBall.size, b.size))
        }) : _*)
    }

    protected def moveBall(stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball, offset: Coordinate): Clause = {
        applyOffsetClause(stateActionBall.x, stateActionBall.y, stateNextActionBall.x, stateNextActionBall.y, offset, and)
    }

    protected def equalCharacterVariables(state: StateBase, stateNext: StateBase): Clause = {
        And(Equals(state.character.x, stateNext.character.x), Equals(state.character.y, stateNext.character.y))
    }

    protected def equalOtherBallsVariables(state: StateBase, stateActionBall: StateBase.Ball, stateNext: StateBase, stateNextActionBall: StateBase.Ball): Clause = {
        And((for ((stateB, stateNextB) <- state.balls.filter(f => f != stateActionBall).zip(stateNext.balls.filter(f => f != stateNextActionBall))) yield {
            And(Equals(stateB.x, stateNextB.x), Equals(stateB.y, stateNextB.y), Equals(stateB.size, stateNextB.size))
        }): _*)
    }

    protected def updateSnowVariables(state: StateBase, stateNext: StateBase, offset: Coordinate): Clause = {
        And((for ((c, s) <- state.snow) yield {
            Equivalent(And(s, Not(And(Equals(state.character.x, IntegerConstant(c.x)), Equals(state.character.y, IntegerConstant(c.y))))), stateNext.snow.get(c + offset).get)
        }).toSeq: _*)
    }

    protected def characterPositionValid(state: StateBase): Clause = {
        Or((for ((c, o) <- state.occupancy) yield {
            And(Equals(state.character.x, IntegerConstant(c.x)), Equals(state.character.y, IntegerConstant(c.y)), Not(o))
        }).toSeq: _*)
    }

    protected def updateBallSize(actionName: String, state: StateBase, stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball): (Clause, Seq[Expression]) = {
        if (level.hasSnow) {
            val expressions = ListBuffer.empty[Expression]

            val theresSnow = Operations.simplify(Or((for ((c, s) <- state.snow) yield {
                    And(operation.Equals(stateActionBall.x, IntegerConstant(c.x)), operation.Equals(stateActionBall.y, IntegerConstant(c.y)), s)
            }).toSeq: _*))

            val theresSnowVar = BooleanVariable(actionName + "_TS")
            expressions.append(VariableDeclaration(theresSnowVar))
            expressions.append(ClauseDeclaration(Equivalent(theresSnow, theresSnowVar)))

            val clause = And(Implies(And(theresSnowVar, operation.Equals(stateActionBall.size, IntegerConstant(1))), operation.Equals(stateNextActionBall.size, IntegerConstant(2))),
                Implies(And(theresSnowVar, operation.Equals(stateActionBall.size, IntegerConstant(2))), operation.Equals(stateNextActionBall.size, IntegerConstant(4))),
                Implies(Or(Not(theresSnowVar), operation.Equals(stateActionBall.size, IntegerConstant(4))), operation.Equals(stateNextActionBall.size, stateActionBall.size)))

            (clause, expressions)
        } else {
            (Equals(stateNextActionBall.size, stateActionBall.size), Seq.empty)
        }
    }

    protected def and(c1: Clause, c2: Clause): Clause = {
        And(c1, c2)
    }

    protected def or(c1: Clause, c2: Clause): Clause = {
        Or(Not(c1), Not(c2))
    }

    protected def applyOffsetClause(applyX: Clause, applyY: Clause, bX: Clause, bY: Clause, offset: Coordinate, operation: (Clause, Clause) => Clause): Clause = { // TODO Es pot millorar? Herencia player ball
        val newX = if (offset.x > 0) {
            Add(applyX, IntegerConstant(offset.x))
        } else if (offset.x < 0){
            Sub(applyX, IntegerConstant(-offset.x))
        } else {
            applyX
        }
        val newY = if (offset.y > 0) {
            Add(applyY, IntegerConstant(offset.y))
        } else if (offset.y < 0){
            Sub(applyY, IntegerConstant(-offset.y))
        } else {
            applyY
        }

        operation(Equals(newX, bX), Equals(newY, bY))
    }

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
}
