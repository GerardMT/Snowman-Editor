package gmt.snowman.encoder

import gmt.planner.encoder.{Encoder, Encoding}
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.planner.solver.value.{Value, ValueBoolean, ValueInteger}
import gmt.snowman.action.{BallAction, Down, Left, Right, SnowmanAction, Up}
import gmt.snowman.encoder.EncoderBase.EncoderOptions
import gmt.snowman.encoder.EncodingDataSnowman.{ActionData, StateData}
import gmt.snowman.encoder.StateBase.CoordinateVariables
import gmt.snowman.game.`object`._
import gmt.snowman.level.{Coordinate, Level}
import gmt.util.AStar

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}


object EncoderBase { // TODO Add rule: Can not unmount a snowman once done

    protected val SMALL_BALL = 1
    protected val MEDIUM_BALL = 2
    protected val LARGE_BALL = 4

    object EncoderEnum extends Enumeration {
        val BASIC, CHEATING, REACHABILITY = Value
    }

    def apply(encoderEnum: EncoderEnum.Value, level: Level, encoderOptions: EncoderOptions): EncoderBase[_ <: VariableAdder] = {
        encoderEnum match {
            case EncoderEnum.BASIC =>
                EncoderBasic(level, encoderOptions)

            case EncoderEnum.CHEATING =>
                EncoderCheating(level, encoderOptions)

            case EncoderEnum.REACHABILITY =>
                EncoderReachability(level, encoderOptions)
        }
    }

    case class EncoderOptions(invariantBallSizes: Boolean, invariantBallLocations: Boolean)
}

abstract class EncoderBase[A <: StateBase](val level: Level, val encoderOptions: EncoderOptions) extends Encoder[A, EncodingDataSnowman, DecodingData] {

    override def createEncodingData(): EncodingDataSnowman = new EncodingDataSnowman(level)

    override def startTimeStep(): Int = {
        level.balls.combinations(2).toList.map(f => f.head.c.manhattanDistance(f(1).c)).min
    }

    override def goal(state: A, encodingData: EncodingDataSnowman): (Clause, immutable.Seq[Clause]) = {
        val ands = ListBuffer.empty[Clause]
        val variables = ListBuffer.empty[Clause]

        if (level.snowmans == 1) {
            ands.append(Equals(state.balls.head.x, state.balls(1).x), Equals(state.balls.head.y, state.balls(1).y))
            ands.append(Equals(state.balls.head.x, state.balls(2).x), Equals(state.balls.head.y, state.balls(2).y))
        } else {
            val snowmansVariables = ListBuffer.empty[(IntegerVariable, IntegerVariable)]
            for (i <- 0 until level.snowmans) {
                val x = IntegerVariable()
                val y = IntegerVariable()
                snowmansVariables.append((x, y))
                variables.append(x)
                variables.append(y)
            }

            for (b <- state.balls) {
                val ors = for ((x, y) <- snowmansVariables) yield {
                    And(Equals(b.x, x), Equals(b.y, y))
                }

                ands.append(Or(ors.toList: _*))
            }
        }

        (And(ands: _*), variables.toList)

        // Jessica & Amelia // 6 min // With combinations 20.8 days
//        val and1 = And(Equals(state.balls(0).x, IntegerConstant(4)),
//            Equals(state.balls(0).y, IntegerConstant(1)),
//            Equals(state.balls(1).x, IntegerConstant(4)),
//            Equals(state.balls(1).y, IntegerConstant(5)),
//            Equals(state.balls(2).x, IntegerConstant(4)),
//            Equals(state.balls(2).y, IntegerConstant(1)),
//            Equals(state.balls(3).x, IntegerConstant(4)),
//            Equals(state.balls(3).y, IntegerConstant(5)),
//            Equals(state.balls(4).x, IntegerConstant(4)),
//            Equals(state.balls(4).y, IntegerConstant(1)),
//            Equals(state.balls(5).x, IntegerConstant(4)),
//            Equals(state.balls(5).y, IntegerConstant(5)))
//
//        val and2 = And(state.balls.zip(level.balls).map(f => Equals(f._1.size, IntegerConstant(getBallSize(f._2.o)))): _*)
//
//        (And(and1, and2), Nil)
    }

    override def initialState(state: A, encoding: Encoding, encodingData: EncodingDataSnowman): Unit = {
        encoding.add(Comment("Initial State"))

        encoding.add(Comment("S0 Character"))
        encodeCharacterState0(state, encoding)

        encoding.add(Comment("S0 Balls"))
        for ((b, levelBall) <- level.balls.indices.map(f => (state.balls(f), level.balls(f)))) {
            encoding.add(ClauseDeclaration(Equals(b.x, IntegerConstant(levelBall.c.x))))
            encoding.add(ClauseDeclaration(Equals(b.y, IntegerConstant(levelBall.c.y))))
            encoding.add(ClauseDeclaration(Equals(b.size, IntegerConstant(getBallSize(levelBall.o)))))
        }

        encoding.add(Comment("S0 Snow"))
        for (s <- state.snow.values) {
            encoding.add(ClauseDeclaration(s))
        }

        encodeReachability(state, encoding)

        encodingData.initialState = state
    }

    override def otherStates(state: A, encoding: Encoding, encodingData: EncodingDataSnowman): Unit = {
        if (encoderOptions.invariantBallLocations) {
            invariantBallLocatoins(state, encoding)
        }
        if (encoderOptions.invariantBallSizes) {
            invariantBallSizes(state, encoding)
        }
    }

    override def actions(state: A, stateNext: A, encoding: Encoding, encodingData: EncodingDataSnowman): Unit = {
        val actionsData = ListBuffer.empty[EncodingDataSnowman.ActionData]

        encodeLocationsRestriction(stateNext, encoding)
        encodeReachability(stateNext, encoding)

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

                actionsData.append(EncodingDataSnowman.ActionData(action, actionVariable, iB))
            }
        }

        encoding.addAll(Operations.getEO(actionsVariables, "EO_A" + stateNext.timeStep))

        encodingData.statesData.append(StateData(stateNext, actionsData.toList))

        actionsData.toList
    }

    override def createState(index: Int, encoding: Encoding, encodingData: EncodingDataSnowman): A

    protected def encodeCharacterState0(state0: A, encoding: Encoding)

    protected def encodeReachability(state: A, encoing: Encoding)

    protected def encodeCharacterAction(actionName: String, state: A, stateNext: A, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsData: mutable.Buffer[EncodingDataSnowman.ActionData])

    protected def createBallAction(actionName: String, state: A, stateActionBall: StateBase.Ball, stateNext: A, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression])

    protected def encodeCharacterState[T <: StateBase](state: T, encoding: Encoding): Unit = {
        encoding.add(ClauseDeclaration(Equals(state.character.x, IntegerConstant(level.character.c.x))))
        encoding.add(ClauseDeclaration(Equals(state.character.y, IntegerConstant(level.character.c.y))))
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

    protected def equalCharacterVariables[T <: StateBase](state: T, stateNext: T): Clause = {
        And(Equals(state.character.x, stateNext.character.x), Equals(state.character.y, stateNext.character.y))
    }

    protected def equalOtherBallsVariables(state: StateBase, stateActionBall: StateBase.Ball, stateNext: StateBase, stateNextActionBall: StateBase.Ball): Clause = {
        And((for ((b, bNext) <- state.balls.filter(f => f != stateActionBall).zip(stateNext.balls.filter(f => f != stateNextActionBall))) yield {
            And(Equals(b.x, bNext.x), Equals(b.y, bNext.y), Equals(b.size, bNext.size))
        }): _*)
    }

    protected def updateSnowVariables(state: StateBase, stateActionBall: StateBase.Ball, stateNext: StateBase, shift: Coordinate): Clause = {
        Operations.simplify(And((for ((c, s) <- flattenTuple(level.map.keys.map(f => (f, state.snow.get(f + shift))))) yield {
            Equivalent(And(s, Or(Not(Equals(stateActionBall.x, IntegerConstant(c.x))), Not(Equals(stateActionBall.y, IntegerConstant(c.y))))), stateNext.snow.get(c + shift).get)
        }).toSeq: _*))
    }

    protected def updateBallSize(actionName: String, state: StateBase, stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Seq[Expression]) = {
        if (level.hasSnow) {
            val expressions = ListBuffer.empty[Expression]

            val theresSnow = Operations.simplify(Or((for ((c, s) <- flattenTuple(level.map.keys.map(f => (f, state.snow.get(f + shift))))) yield {
                    And(Equals(stateActionBall.x, IntegerConstant(c.x)), Equals(stateActionBall.y, IntegerConstant(c.y)), s)
            }).toSeq: _*))

            val theresSnowVar = BooleanVariable(actionName + "_TS")
            expressions.append(VariableDeclaration(theresSnowVar))
            expressions.append(ClauseDeclaration(Equivalent(theresSnow, theresSnowVar)))

            val clause = And(Implies(And(theresSnowVar, Equals(stateActionBall.size, IntegerConstant(EncoderBase.SMALL_BALL))), Equals(stateNextActionBall.size, IntegerConstant(EncoderBase.MEDIUM_BALL))),
                Implies(And(theresSnowVar, Equals(stateActionBall.size, IntegerConstant(EncoderBase.MEDIUM_BALL))), Equals(stateNextActionBall.size, IntegerConstant(EncoderBase.LARGE_BALL))),
                Implies(Or(Not(theresSnowVar), Equals(stateActionBall.size, IntegerConstant(EncoderBase.LARGE_BALL))), Equals(stateNextActionBall.size, stateActionBall.size)))

            (clause, expressions)
        } else {
            (Equals(stateNextActionBall.size, stateActionBall.size), Seq.empty)
        }
    }

    protected def invariantBallSizes(state: StateBase, encoding: Encoding): Unit = {
        val mediumBalls = IntegerVariable()
        encoding.add(VariableDeclaration(mediumBalls))
        val sumMedium = state.balls.map(f => Ite(Equals(f.size, IntegerConstant(EncoderBase.MEDIUM_BALL)), IntegerConstant(1), IntegerConstant(0))).reduce(Add.ADD)
        encoding.add(ClauseDeclaration(Equals(mediumBalls, sumMedium)))
        encoding.add(ClauseDeclaration(Smaller(mediumBalls, IntegerConstant(2 * level.snowmans + 1))))

        val largeBalls = IntegerVariable()
        encoding.add(VariableDeclaration(largeBalls))
        val sumLarge = state.balls.map(f => Ite(Equals(f.size, IntegerConstant(EncoderBase.LARGE_BALL)), IntegerConstant(1), IntegerConstant(0))).reduce(Add.ADD)
        encoding.add(ClauseDeclaration(Equals(largeBalls, sumLarge)))
        encoding.add(ClauseDeclaration(Smaller(largeBalls, IntegerConstant(2 * level.snowmans))))
    }

    protected def invariantBallLocatoins(state: StateBase, encoding: Encoding): Unit = {
        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            val up = level.map.get(l.c + Up.shift).get.o
            val down = level.map.get(l.c + Down.shift).get.o
            val right = level.map.get(l.c + Right.shift).get.o
            val left = level.map.get(l.c + Left.shift).get.o

            val invalid = !Object.isPlayableArea(up) && !Object.isPlayableArea(right) ||
                !Object.isPlayableArea(right) && !Object.isPlayableArea(down) ||
                !Object.isPlayableArea(down) && !Object.isPlayableArea(left) ||
                !Object.isPlayableArea(left) && !Object.isPlayableArea(up)

            if (invalid) {
                for (ball <- state.balls) {
                    encoding.add(ClauseDeclaration(Implies(And(Equals(ball.x, IntegerConstant(l.c.x)), Equals(ball.y, IntegerConstant(l.c.y))), Not(Or(Equals(ball.size, IntegerConstant(EncoderBase.SMALL_BALL)), Equals(ball.size, IntegerConstant(EncoderBase.MEDIUM_BALL)))))))
                }
            }
        }
    }

    protected trait ApplyShiftClauseOperation {
        def apply(c1: Clause, c2: Clause): Clause
    }
    protected object AND extends ApplyShiftClauseOperation {
        override def apply(c1: Clause, c2: Clause): Clause = And.AND(c1, c2)
    }
    protected object OR extends ApplyShiftClauseOperation {
        override def apply(c1: Clause, c2: Clause): Clause = Or(Not(c1), Not(c2))
    }

    protected def applyShiftClause(applyVariables: StateBase.CoordinateVariables, variables: StateBase.CoordinateVariables, shift: Coordinate, operation: ApplyShiftClauseOperation): Clause = {
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
            EncoderBase.SMALL_BALL
        case MediumBall =>
            EncoderBase.MEDIUM_BALL
        case LargeBall =>
            EncoderBase.LARGE_BALL
    }

    protected def decodeTeleport(assignments: Seq[Assignment], encodingData: EncodingDataSnowman): DecodingData = {
        val assignmentsMap = assignments.map(f => (f.name, f.value)).toMap

        val actions = ListBuffer.empty[SnowmanAction]
        val actionsBalls = ListBuffer.empty[BallAction]

        var characterLocation = encodingData.level.character.c

        var state = encodingData.initialState
        for (stateData <- encodingData.statesData) {
            val stateNext = stateData.stateNext
            val actionData = stateData.actionsData.find(f => assignmentsMap(f.actionVariable.name).asInstanceOf[ValueBoolean].v).get

            val ballCoordinate = coordinateFromCoordinateVariables(state.balls(actionData.ballActionIndex), assignmentsMap)

            val preActionCoordinate = ballCoordinate - actionData.action.shift

            val assigmentsReport = assignments.map(f => (f.name, f)).toMap
            println("STATE") // TODO Remove println
            print(Report.generateMap(level, state, assigmentsReport))
            println("STATE NEXT")
            print(Report.generateMap(level, stateNext, assigmentsReport))


            println("Path Start: " +  characterLocation)
            println("Path End: " + preActionCoordinate)

            if (preActionCoordinate - characterLocation != Coordinate(0, 0)) {
                val allNodes = () => level.map.keys.toList
                val neighboursRelaxed  = (c: Coordinate) => SnowmanAction.ACTIONS.map(f => c + f.shift).filter(f => {val l = level.map.get(f); l.isDefined && Object.isPlayableArea(l.get.o)})
                val neighbours = (c: Coordinate) => neighboursRelaxed(c).filter(f => !state.balls.exists(b => assignmentsMap(b.x.name).asInstanceOf[ValueInteger].v == f.x && assignmentsMap(b.y.name).asInstanceOf[ValueInteger].v == f.y))
                val heuristic = (start: Coordinate, goal: Coordinate) => start.euclideanDistance(goal).toFloat

                var path = AStar.aStar(characterLocation, preActionCoordinate, allNodes, neighbours, heuristic)
                if (path.isEmpty) {
                    path = AStar.aStar(characterLocation, preActionCoordinate, allNodes, neighboursRelaxed, heuristic)

                    println("Omitting occpuied locations for balls")
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

    private def encodeLocationsRestriction[T <: StateBase](state: T, encoder: Encoding): Unit = {
        encoder.add(ClauseDeclaration(Or((for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) yield {
            And(Equals(state.character.x, IntegerConstant(l.c.x)), Equals(state.character.y, IntegerConstant(l.c.y)))
        }).toSeq: _*)))
        for (b <- state.balls) {
            encoder.add(ClauseDeclaration(Not(And(Equals(state.character.x, b.x), Equals(state.character.y, b.y)))))
        }
        for (b <- state.balls) {
            encoder.add(ClauseDeclaration(Or((for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) yield {
                And(Equals(b.x, IntegerConstant(l.c.x)), Equals(b.y, IntegerConstant(l.c.y)))
            }).toSeq: _*)))
        }
    }

    protected def flattenTuple[T,U](l: Iterator[(T, Option[U])]): Iterator[(T, U)] = l.filter(f => f._2.isDefined).map(f => (f._1, f._2.get))
}
