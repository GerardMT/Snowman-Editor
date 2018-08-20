package gmt.snowman.encoder

import gmt.planner.encoder.{Encoder, EncoderResult, Encoding}
import gmt.planner.operation
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.planner.solver.value.{Value, ValueBoolean, ValueInteger}
import gmt.snowman.action.{BallAction, SnowmanAction}
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder.StateBase.CoordinateVariables
import gmt.snowman.game.Game
import gmt.snowman.game.`object`._
import gmt.snowman.level.{Coordinate, Level}
import gmt.snowman.util.AStar

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}


object EncoderBase {

    object EncoderEnum extends Enumeration {
        val BASIC, CHEATING, REACHABILITY = Value
    }

    def apply(encoderEnum: EncoderEnum.Value, level: Level, encoderOptions: EncoderOptions): EncoderBase[_] = {
        encoderEnum match {
            case EncoderEnum.BASIC =>
                EncoderBasic(level, encoderOptions)

            case EncoderEnum.CHEATING =>
                EncoderCheating(level, encoderOptions)

            case EncoderEnum.REACHABILITY =>
                EncoderReachability(level, encoderOptions)
        }
    }

    case class EncoderOptions(invariantBallSizes: Boolean, invariantBallLocations: Boolean, invariantBallDistances: Boolean)
}

abstract class EncoderBase[A <: StateBase](val level: Level, val encoderOptions: EncoderOptions) extends Encoder[DecodingData, EncodingData] {

    override def startTimeStep(): Int = {
        level.balls.combinations(2).toList.map(f => f.head.c.manhattanDistance(f(1).c)).sorted.take(level.snowmans * 2).sum
    }

    override def encode(timeSteps: Int): EncoderResult[EncodingData] = {
        val encoding = new Encoding

        val states = ListBuffer.empty[A]

        val statesData = ListBuffer.empty[EncodingData.StateData]

        val state0 = createState(level, 0)
        state0.addVariables(encoding)
        states.append(state0)

        encoding.add(Comment("Initial State"))

        encoding.add(Comment("S0 Character"))
        encodeCharacterState0(state0, encoding)

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
        for ((c, v) <- state0.occupancy) {
            val o = level.map.get(c).get.o
            if (o == Wall || Object.isBall(o)) {
                encoding.add(ClauseDeclaration(v))
            } else {
                encoding.add(ClauseDeclaration(Not(v)))
            }
        }

        encoding.add(Comment("Middle States"))

        var state = state0

        for (timeStep <- 1 until timeSteps + 1) {
            val stateNext =  createState(level, timeStep)
            stateNext.addVariables(encoding)
            states.append(stateNext)

            val actionsData = ListBuffer.empty[EncodingData.ActionData]

            encoding.add(Comment("Occupancy"))
            for (l <- level.map.values.filterNot(f => f.o == Wall)) {
                val ors = for (b <- stateNext.balls) yield {
                    And(Equals(b.x, IntegerConstant(l.c.x)), Equals(b.y, IntegerConstant(l.c.y))) // TODO OPTIMITZAR Es pot optimitzar? Pq només es mou la bola que fa l'acció, no cal fer un conjunt d'ors
                }
                encoding.add(ClauseDeclaration(Equivalent(Or(ors: _*), stateNext.occupancy.get(l.c).get)))
            }

            encoding.add(ClauseDeclaration(occupancyWalls(stateNext)))

            encodeReachability(stateNext, encoding)

            // TODO Invariants

            val actionsVariables = ListBuffer.empty[BooleanVariable]

            val namesCharacterActions = List("AV_MCR", "AV_MCL", "AV_MCU", "AV_MCD")

            for ((action, name) <- SnowmanAction.ACTIONS.zip(namesCharacterActions)) {
                encodeCharacterAction(name, state, stateNext, action, encoding, actionsVariables, actionsData)
            }

            val namesBallActions = List("AV_MBR", "AV_MBL", "AV_MBU", "AV_MBD")

            for ((action, name) <- SnowmanAction.ACTIONS.zip(namesBallActions)) {
                for (((stateActionBall, stateNextActionBall), iB) <- state.balls.zip(stateNext.balls).zipWithIndex) {
                    val actionName = name + "_B" + iB + "_S" + state.timeStep + "S" + stateNext.timeStep

                    val actionVariable = BooleanVariable(actionName)
                    encoding.add(VariableDeclaration(actionVariable))
                    actionsVariables.append(actionVariable)

                    val (pre, eff, returnExpressions) = createBallAction(actionName, state, stateActionBall, stateNext, stateNextActionBall, action.shift)
                    encoding.addAll(returnExpressions)

                    encoding.add(ClauseDeclaration(Equivalent(eff, actionVariable)))
                    encoding.add(ClauseDeclaration(Implies(actionVariable, pre)))

                    actionsData.append(EncodingData.ActionData(action, actionVariable, iB))
                }
            }

            encoding.addAll(Operations.getEO(actionsVariables, "EO_A" + timeStep))

            statesData.append(EncodingData.StateData(stateNext, actionsData.toList))

            state = stateNext
        }

        encoding.add(Comment("Goal")) // TODO New goal
        val combinations = state.balls.combinations(Game.SNOWMAN_BALLS).toList

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

        EncoderResult(encoding, EncodingData(level, state0, statesData.toList))
    }

    protected def createState(level: Level, index: Int): A

    protected def encodeCharacterState0(state0: A, encoding: Encoding)

    protected def encodeReachability(state: A, encoing: Encoding)

    protected def encodeCharacterAction(actionName: String, state: A, stateNext: A, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsData: mutable.Buffer[EncodingData.ActionData])

    protected def createBallAction(actionName: String, state: A, stateActionBall: StateBase.Ball, stateNext: A, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression])

    protected def noWallInFront(state: StateBase, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = { // TODO OPTIMIZATION Can be done with coordinates
        if (level.map.values.count(f => Object.isPlayableArea(f.o)) < level.map.values.count(f => !Object.isPlayableArea(f.o))) {
            Not(And((for (l <- level.map.keys.flatMap(f => level.map.get(f + shift)).filter(f => Object.isPlayableArea(f.o))) yield { // TODO BUG
                Or(Not(Equals(stateActionBall.x, IntegerConstant(l.c.x))), Not(Equals(stateActionBall.y, IntegerConstant(l.c.y))))
            }).toSeq : _*))
        } else {
            And((for (l <- level.map.keys.flatMap(f => level.map.get(f + shift)).filter(f => !Object.isPlayableArea(f.o))) yield {
                Or(Not(Equals(stateActionBall.x, IntegerConstant(l.c.x))), Not(Equals(stateActionBall.y, IntegerConstant(l.c.y))))
            }).toSeq : _*)
        }
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

    protected def otherBallInFront(state: StateBase, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        Or((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            applyShiftClause(stateActionBall, b, shift, AND)
        }): _*)
    }

    protected def otherBallsInFrontLarger(state: StateBase, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        And((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            Or(applyShiftClause(stateActionBall, b, shift, OR), Smaller(stateActionBall.size, b.size))
        }): _*)
    }

    protected def moveBall(stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBall, stateNextActionBall, shift, AND)
    }

    protected def equalCharacterVariables[T <: CharacterInterface](state: T, stateNext: T): Clause = {
        And(Equals(state.character.x, stateNext.character.x), Equals(state.character.y, stateNext.character.y))
    }

    protected def equalOtherBallsVariables(state: StateBase, stateActionBall: StateBase.Ball, stateNext: StateBase, stateNextActionBall: StateBase.Ball): Clause = {
        And((for ((b, bNext) <- state.balls.filter(f => f != stateActionBall).zip(stateNext.balls.filter(f => f != stateNextActionBall))) yield {
            And(Equals(b.x, bNext.x), Equals(b.y, bNext.y), Equals(b.size, bNext.size))
        }): _*)
    }

    protected def updateSnowVariables(state: StateBase, stateActionBall: StateBase.Ball, stateNext: StateBase, shift: Coordinate): Clause = {
        Operations.simplify(And((for ((c, s) <- flattenTuple(level.map.keys.map(f => (f, state.snow.get(f + shift))))) yield {
            Equivalent(And(s, Not(And(Equals(stateActionBall.x, IntegerConstant(c.x)), Equals(stateActionBall.y, IntegerConstant(c.y))))), stateNext.snow.get(c + shift).get)
        }).toSeq: _*))
    }

    protected def characterLocationTeleportValid[T <: StateBase](state: T, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        Or((for ((c, o) <- flattenTuple(level.map.keys.map(f => (f, state.occupancy.get(f - shift))))) yield {
            And(Equals(stateActionBall.x, IntegerConstant(c.x)), Equals(stateActionBall.y, IntegerConstant(c.y)), Not(o))
        }).toSeq: _*)
    }

    protected def updateBallSize(actionName: String, state: StateBase, stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Seq[Expression]) = {
        if (level.hasSnow) {
            val expressions = ListBuffer.empty[Expression]

            val theresSnow = Operations.simplify(Or((for ((c, s) <- flattenTuple(level.map.keys.map(f => (f, state.snow.get(f + shift))))) yield {
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

    protected val AND: (Clause, Clause) => And = (c1, c2) => And(c1, c2)

    protected val OR: (Clause, Clause) => Or = (c1, c2) => Or(Not(c1), Not(c2))

    protected def applyShiftClause(applyVariables: StateBase.CoordinateVariables, variables: StateBase.CoordinateVariables, shift: Coordinate, operation: (Clause, Clause) => Clause): Clause = {
        val newX = if (shift.x > 0) {
            Add(applyVariables.x, IntegerConstant(shift.x))
        } else if (shift.x < 0){
            Sub(applyVariables.x, IntegerConstant(-shift.x))
        } else {
            applyVariables.x
        }
        val newY = if (shift.y > 0) {
            Add(applyVariables.y, IntegerConstant(shift.y))
        } else if (shift.y < 0){
            Sub(applyVariables.y, IntegerConstant(-shift.y))
        } else {
            applyVariables.y
        }

        operation(Equals(newX, variables.x), Equals(newY, variables.y))
    }

    protected def getBallSize(o: gmt.snowman.game.`object`.Object): Int = o match {
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

    private def occupancyWalls(state: StateBase): Clause = {
        And((for (o <- level.map.values.filter(f => f.o == Wall).flatMap(f => state.occupancy.get(f.c))) yield {
            o
        }).toSeq: _*)
    }

    protected def decodeTeleport(assignments: Seq[Assignment], encodingData: EncodingData): DecodingData = {
        val assignmentsMap = assignments.map(f => (f.name, f.value)).toMap


        val actions = ListBuffer.empty[SnowmanAction]
        val actionsBalls = ListBuffer.empty[BallAction]

        var characterLocation = encodingData.level.character.c

        var state = encodingData.state0
        for (stateData <- encodingData.statesData) {
            val stateNext = stateData.stateNext
            val actionData = stateData.actionsData.find(f => assignmentsMap(f.actionVariable.name).asInstanceOf[ValueBoolean].v).get

            val ballCoordinate = coordinateFromCoordinateVariables(state.balls(actionData.ballActionIndex), assignmentsMap)

            val preActionCoordinate = ballCoordinate - actionData.action.shift

            val assigmentsReport = assignments.map(f => (f.name, f)).toMap
            println("STATE")
            print(Report.generateMap(level, state, assigmentsReport))
            println("STATE NEXT")
            print(Report.generateMap(level, stateNext, assigmentsReport))


            println("Path Start: " +  characterLocation)
            println("Path End: " + preActionCoordinate)

            if (preActionCoordinate - characterLocation != actionData.action.shift) {
                val allNodes = () => level.map.keys.toList
                val neighboursRelaxed  = (c: Coordinate) => SnowmanAction.ACTIONS.map(f => c + f.shift).filter(f => {val l = level.map.get(f); l.isDefined && Object.isPlayableArea(l.get.o)})
                val neighbours = (c: Coordinate) => neighboursRelaxed(c).filter(f => !assignmentsMap(state.occupancy.get(f).get.name).asInstanceOf[ValueBoolean].v)
                val heuristic = (start: Coordinate, goal: Coordinate) => start.euclideanDistance(goal).toFloat

                var path = AStar.aStar(characterLocation, preActionCoordinate, allNodes, neighbours, heuristic)
                if (path.isEmpty) {
                    path = AStar.aStar(characterLocation, preActionCoordinate, allNodes, neighboursRelaxed, heuristic)

                    println("Omitting occupancy for balls")
                }

                val pathActions = pathToActions(path)
                pathActions.foreach(f => println("    Path: " + f))

                actions.appendAll(pathActions)
            }

            actions.append(actionData.action)
            actionsBalls.append(BallAction(actionData.action, actionData.ballActionIndex))
            println("Action: " + actionData.action + "\n")

            val existOtherBallUnder = state.balls.patch(actionData.ballActionIndex, Nil, 1).exists(f => coordinateFromCoordinateVariables(f, assignmentsMap) == ballCoordinate)

            characterLocation  = if (existOtherBallUnder) {
                preActionCoordinate
            } else {
                ballCoordinate
            }

            state = stateNext
        }

        DecodingData(actions.toList, level.balls, actionsBalls.toList)
    }

    private def coordinateFromCoordinateVariables(coordinateVariables: CoordinateVariables, assignmentsMap: immutable.Map[String, Value]): Coordinate = {
        Coordinate(assignmentsMap(coordinateVariables.x.name).asInstanceOf[ValueInteger].v, assignmentsMap(coordinateVariables.y.name).asInstanceOf[ValueInteger].v)
    }

    private def pathToActions(path: immutable.Seq[Coordinate]): List[SnowmanAction] = {
        val reverse = path.reverse
        reverse.zip(reverse.tail).map(f => SnowmanAction.ACTIONS.find(f2 => f2.shift == (f._2 - f._1)).get).toList
    }


    protected def flattenTuple[T,U](l: Iterator[(T, Option[U])]): Iterator[(T, U)] = l.filter(f => f._2.isDefined).map(f => (f._1, f._2.get))
}
