package gmt.snowman.encoder

import gmt.planner.encoder.{Encoder, Encoding}
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.planner.solver.value.{Value, ValueBoolean, ValueInteger}
import gmt.snowman.action.{BallAction, CharacterDown, CharacterLeft, CharacterRight, CharacterUp, SnowmanAction}
import gmt.snowman.encoder.EncoderBase.{EncoderOptions, InvalidCoordinateFixedDimensionException}
import gmt.snowman.encoder.EncodingDataSnowman.StateData
import gmt.snowman.encoder.StateBase.{Ball, CoordinateVariables}
import gmt.snowman.game.`object`._
import gmt.snowman.level.{Coordinate, CoordinateMask, Level, Location}
import gmt.util.AStar

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}


object EncoderBase {

    case class InvalidCoordinateFixedDimensionException() extends Exception

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

    case class EncoderOptions(invariantBallSizes: Boolean, invariantBallLocations: Boolean, invariantWallsU: Boolean)
}

abstract class EncoderBase[A <: StateBase](val level: Level, val encoderOptions: EncoderOptions) extends Encoder[A, EncodingDataSnowman, DecodingData] {

    override def createEncodingData(): EncodingDataSnowman = new EncodingDataSnowman(level)

    override def startTimeStep(): Int = {
        level.balls.combinations(2).toList.map(f => f.head.c.manhattanDistance(f(1).c)).min
    }

    override def goal(state: A, encodingData: EncodingDataSnowman): (Clause, immutable.Seq[Clause]) = {
        val ands = ListBuffer.empty[Clause]
        val variables = ListBuffer.empty[Clause]

        if (level.snowmen == 1) {
            ands.append(Equals(state.balls.head.x, state.balls(1).x), Equals(state.balls.head.y, state.balls(1).y))
            ands.append(Equals(state.balls.head.x, state.balls(2).x), Equals(state.balls.head.y, state.balls(2).y))
        } else {
            val snowmansVariables = ListBuffer.empty[(IntegerVariable, IntegerVariable)]
            for (i <- 0 until level.snowmen) {
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
    }

    override def initialState(state: A, encoding: Encoding, encodingData: EncodingDataSnowman): Unit = {
        encoding.add(Comment("Initial State"))

        encoding.add(Comment("S0 Character"))
        encodeCharacterState0(state, encoding)

        encoding.add(Comment("S0 Balls"))
        for ((b, levelBall) <- level.balls.indices.map(f => (state.balls(f), level.balls(f)))) {
            encoding.add(ClauseDeclaration(Equals(b.x, IntegerConstant(levelBall.c.x))))
            encoding.add(ClauseDeclaration(Equals(b.y, IntegerConstant(levelBall.c.y))))
            encoding.add(ClauseDeclaration(encodeBallSizeEquals(b, levelBall.o)))
        }

        encoding.add(Comment("S0 Snow"))
        for (s <- state.snow.values) {
            encoding.add(ClauseDeclaration(s))
        }

        encodingData.initialState = state
    }

    override def otherStates(state: A, encoding: Encoding, encodingData: EncodingDataSnowman): Unit = {
        if (encoderOptions.invariantBallLocations) {
            invariantBallLocatoins(state, encoding)
        }
        if (encoderOptions.invariantBallSizes) {
            invariantBallSizes(state, encoding)
        }

        for (b <- state.balls) {
            encoding.add(ClauseDeclaration(Implies(b.sizeB, b.sizeA)))
        }
    }

    override def actions(state: A, stateNext: A, encoding: Encoding, encodingData: EncodingDataSnowman): Unit = {
        val actionsData = ListBuffer.empty[EncodingDataSnowman.ActionData]

        encodeLocationsRestriction(stateNext, encoding)
        encodeReachability(state, encoding)

        val actionsVariables = ListBuffer.empty[BooleanVariable]

        val namesCharacterActions = List("AV_MCR", "AV_MCL", "AV_MCU", "AV_MCD")

        for ((action, name) <- SnowmanAction.CHARACTER_ACTIONS.zip(namesCharacterActions)) {
            encodeCharacterAction(name, state, stateNext, action, encoding, actionsVariables, actionsData)
        }

        val namesBallActions = List("AV_MBR", "AV_MBL", "AV_MBU", "AV_MBD")

        for ((action, name) <- SnowmanAction.BALL_ACTIONS.zip(namesBallActions)) {
            for (((stateActionBall, stateNextActionBall), iB) <- state.balls.zip(stateNext.balls).zipWithIndex) {
                val actionName = name + "_B" + iB + "_S" + state.timeStep + "S" + stateNext.timeStep

                val actionVariable = BooleanVariable(actionName)
                encoding.add(VariableDeclaration(actionVariable))
                actionsVariables.append(actionVariable)

                val (pre, eff, returnExpressions) = createBallAction(actionName, state, stateActionBall, stateNext, stateNextActionBall, action(iB).shift)
                encoding.addAll(returnExpressions)

                encoding.add(ClauseDeclaration(Equivalent(eff, actionVariable)))
                encoding.add(ClauseDeclaration(Implies(actionVariable, pre)))

                actionsData.append(EncodingDataSnowman.ActionData(action(iB), actionVariable, iB))
            }
        }

        val (c, e) = Operations.getEOLog(actionsVariables)
        encoding.addAll(e)
        encoding.add(ClauseDeclaration(c))

        encodingData.statesData.append(StateData(stateNext, actionsData.toList))

        if (encoderOptions.invariantWallsU) {
            invariantWallU(state, stateNext, encoding)
        }

        if (level.hasSnow) {
            encoding.add(ClauseDeclaration(updateSnowVariables(state, stateNext)))
        }

        encoding.add(ClauseDeclaration(updateBallSize(state, stateNext)))
    }

    override def createState(index: Int, encoding: Encoding, encodingData: EncodingDataSnowman): A

    protected def encodeCharacterState0(state0: A, encoding: Encoding)

    protected def encodeReachability(state: A, encoing: Encoding): Unit = {}

    protected def encodeCharacterAction(actionName: String, state: A, stateNext: A, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsData: mutable.Buffer[EncodingDataSnowman.ActionData])

    protected def createBallAction(actionName: String, state: A, stateActionBall: StateBase.Ball, stateNext: A, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression])

    protected def actions(state: A, stateNext: A, encoding: Encoding): Unit = {}

    protected def encodeCharacterState[T <: StateBase](state: T, encoding: Encoding): Unit = {
        encoding.add(ClauseDeclaration(Equals(state.character.x, IntegerConstant(level.character.c.x))))
        encoding.add(ClauseDeclaration(Equals(state.character.y, IntegerConstant(level.character.c.y))))
    }

    protected def noOtherTwoBallsUnder(state: StateBase, stateActionBall: StateBase.Ball): Clause = {
        And((for (b <- state.balls.filter(f => f != stateActionBall).combinations(2)) yield {
            Not(And(Equals(stateActionBall.x, b(0).x), Equals(stateActionBall.y, b(0).y), And(Equals(stateActionBall.x, b(1).x), Equals(stateActionBall.y, b(1).y))))
        }).toSeq: _*)
    }

    protected def noOtherBallsOver(state: StateBase, stateActionBall: StateBase.Ball): Clause = {
        And((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            Or(Not(Equals(stateActionBall.x, b.x)), Not(Equals(stateActionBall.y, b.y)), encodeBallSizeSmaller(stateActionBall, b))
        }): _*)
    }

    protected def otherBallUnder(state: StateBase, stateActionBall: StateBase.Ball): Clause = {
        Or((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            And(Equals(stateActionBall.x, b.x), Equals(stateActionBall.y, b.y), encodeBallSizeSmaller(stateActionBall, b))
        }): _*)
    }

    protected def otherBallInFront(state: StateBase, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        Or((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            applyShiftClause(stateActionBall, b, shift, AND)
        }): _*)
    }

    protected def otherBallsInFrontLarger(state: StateBase, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        And((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            Or(applyShiftClause(stateActionBall, b, shift, OR), encodeBallSizeSmaller(stateActionBall, b))
        }): _*)
    }

    protected def moveBall(stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBall, stateNextActionBall, shift, AND)
    }

    protected def equalCharacterVariables[T <: StateBase](state: T, stateNext: T): Clause = {
        And(Equals(state.character.x, stateNext.character.x), Equals(state.character.y, stateNext.character.y))
    }

    protected def equalOtherBallsPositions(state: StateBase, stateActionBall: StateBase.Ball, stateNext: StateBase, stateNextActionBall: StateBase.Ball): Clause = {
        And((for ((b, bNext) <- state.balls.filter(f => f != stateActionBall).zip(stateNext.balls.filter(f => f != stateNextActionBall))) yield {
            And(Equals(b.x, bNext.x), Equals(b.y, bNext.y))
        }): _*)
    }

    protected def updateSnowVariables(state: StateBase, stateNext: StateBase): Clause = {
        val ands = ListBuffer.empty[Clause]

        for (c <- level.map.values.filter(f => Object.isSnow(f.o)).map(f => f.c)) {
            val noBall = And(stateNext.balls.map(f => Or(Not(Equals(f.x, IntegerConstant(c.x))), Not(Equals(f.y, IntegerConstant(c.y))))): _*)

            ands.append(Equivalent(stateNext.snow(c), And(state.snow(c), noBall)))
        }

        Operations.simplify(And(ands: _*))
    }

    protected def updateBallSize(state: StateBase, stateNext: StateBase): Clause = {
        val ands = ListBuffer.empty[Clause]

        if (level.hasSnow) {
            for (b <-level.balls.indices) {
                for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
                    if (Object.isSnow(l.o)) {
                        ands.append(Implies(And(state.snow(l.c), Equals(stateNext.balls(b).x, IntegerConstant(l.c.x)), Equals(stateNext.balls(b).y, IntegerConstant(l.c.y)), Not(state.balls(b).sizeA), Not(state.balls(b).sizeB)),
                            And(stateNext.balls(b).sizeA, Not(stateNext.balls(b).sizeB))))
                        ands.append(Implies(And(state.snow(l.c), Equals(stateNext.balls(b).x, IntegerConstant(l.c.x)), Equals(stateNext.balls(b).y, IntegerConstant(l.c.y)), state.balls(b).sizeA, Not(state.balls(b).sizeB)),
                            stateNext.balls(b).sizeB))
                        ands.append(Implies(And(Not(state.snow(l.c)), Equals(stateNext.balls(b).x, IntegerConstant(l.c.x)), Equals(stateNext.balls(b).y, IntegerConstant(l.c.y))),
                            And(Equivalent(state.balls(b).sizeA, stateNext.balls(b).sizeA), Equivalent(state.balls(b).sizeB, stateNext.balls(b).sizeB))))
                        ands.append(Implies(state.balls(b).sizeB,
                            stateNext.balls(b).sizeB))
                    } else {
                        ands.append(Implies(And(Equals(stateNext.balls(b).x, IntegerConstant(l.c.x)), Equals(stateNext.balls(b).y, IntegerConstant(l.c.y))),
                            And(Equivalent(state.balls(b).sizeA, stateNext.balls(b).sizeA), Equivalent(state.balls(b).sizeB, stateNext.balls(b).sizeB))))
                    }
                }
            }
        } else {
            for (b <- level.balls.indices) {
                ands.append(Equivalent(state.balls(b).sizeA, stateNext.balls(b).sizeA))
                ands.append(Equivalent(state.balls(b).sizeB, stateNext.balls(b).sizeB))
            }
        }

        And(ands: _*)
    }

    private  def invariantBallSizes(state: StateBase, encoding: Encoding): Unit = {
        encoding.addAll(Operations.getAMKTotalizer(state.balls.map(f => List(f.sizeA, f.sizeB)).reduce((a, b) => a ++ b), 3 * level.snowmen))

        val mediumBallsCounter = ListBuffer.empty[BooleanVariable]
        for (b <- state.balls) {
            val v = BooleanVariable()
            encoding.add(VariableDeclaration(v))
            encoding.add(ClauseDeclaration(Equivalent(And(b.sizeA, Not(b.sizeB)), v)))
            mediumBallsCounter.append(v)
        }

        encoding.addAll(Operations.getAMKTotalizer(mediumBallsCounter, level.snowmen * 2))
    }

    private lazy val invalidLocations: Iterable[Coordinate] = {
        val invalidLocationsMutable = ListBuffer.empty[Coordinate]

        for (l <- level.map.values.filter(f => Object.isPlayableArea(f.o))) {
            val up = level.map(l.c + CharacterUp.shift).o
            val down = level.map(l.c + CharacterDown.shift).o
            val right = level.map(l.c + CharacterRight.shift).o
            val left = level.map(l.c + CharacterLeft.shift).o

            val invalid = !Object.isPlayableArea(up) && !Object.isPlayableArea(right) ||
                !Object.isPlayableArea(right) && !Object.isPlayableArea(down) ||
                !Object.isPlayableArea(down) && !Object.isPlayableArea(left) ||
                !Object.isPlayableArea(left) && !Object.isPlayableArea(up)

            if (invalid) {
                invalidLocationsMutable.append(l.c)
            }
        }

        invalidLocationsMutable.to[immutable.Seq]
    }

    private  def invariantBallLocatoins(state: StateBase, encoding: Encoding): Unit = {
        for (c <- invalidLocations) {
            for ((ball, otherBalls )<- state.balls.zipWithIndex.map(f => (f._1, state.balls.patch(f._2, Nil, 1)))) {
                encoding.add(ClauseDeclaration(
                    Implies(
                        And(
                            Equals(ball.x, IntegerConstant(c.x)),
                            Equals(ball.y, IntegerConstant(c.y)),
                            encodeBallSizeEquals(ball, SmallBall)),
                        Or(otherBalls.map(f => And(
                            Equals(f.x, IntegerConstant(c.x)),
                            Equals(f.y, IntegerConstant(c.y)),
                            encodeBallSizeEquals(f, MediumBall))): _*))))
                encoding.add(ClauseDeclaration(
                    Implies(
                        And(
                            Equals(ball.x, IntegerConstant(c.x)),
                            Equals(ball.y, IntegerConstant(c.y)),
                            encodeBallSizeEquals(ball, MediumBall)),
                        Or(otherBalls.map(f => And(
                            Equals(f.x, IntegerConstant(c.x)),
                            Equals(f.y, IntegerConstant(c.y)),
                            encodeBallSizeEquals(f, LargeBall))): _*))))
            }
        }
    }

    def tupleSort[A](t: (A, A))(implicit ordering: A => Ordered[A]): (A, A) = if (t._1 < t._2) {
        (t._1, t._2)
    } else {
        (t._2, t._1)
    }

    private case class CoordinateFixedDimension(value: Int, fiexedCoordinate: CoordinateMask, rangeFrom: Int, rangeTo: Int)

    private case class LocationVisited(location: Location) {
        var visited = CoordinateMask(x = false, y = false)
    }

    private def checkForU(startCoordinate: Coordinate, direction: Coordinate, wall: Coordinate, dimension: CoordinateMask, visitedMap: immutable.Map[Coordinate, LocationVisited]): Option[CoordinateFixedDimension] = {
        var currentCoordinate = startCoordinate
        var search = true
        var nextPlayable = true

        visitedMap(currentCoordinate).visited |= dimension

        while (search) {
            currentCoordinate += direction

            val currentVisitedLocation = visitedMap(currentCoordinate)
            nextPlayable = Object.isPlayableArea(currentVisitedLocation.location.o)
            search = (currentVisitedLocation.visited & dimension) == CoordinateMask(x = false, y = false) &&  nextPlayable  && !Object.isPlayableArea(visitedMap(currentCoordinate + wall).location.o)

            currentVisitedLocation.visited |= dimension
        }

        if (!nextPlayable) {
            val (from, to) = tupleSort((startCoordinate.getDimensionValue(!dimension), (currentCoordinate - direction).getDimensionValue(!dimension)))
            Some(CoordinateFixedDimension(startCoordinate.getDimensionValue(dimension), dimension, from, to))
        } else {
            None
        }
    }

    private lazy val findU: Iterable[CoordinateFixedDimension] = {
        val mutableWallU = ListBuffer.empty[CoordinateFixedDimension]

        val visitedMap = level.map.map(f => (f._1, LocationVisited(f._2))).toMap

        val x = CoordinateMask(x = true, y = false)
        val y = CoordinateMask(x = false, y = true)

        for ((_, v) <- visitedMap.filter(f => Object.isPlayableArea(f._2.location.o))) {
            if (v.visited.isAnyEmpty) {
                val up = level.map(v.location.c + CharacterUp.shift).o
                val down = level.map(v.location.c + CharacterDown.shift).o
                val right = level.map(v.location.c + CharacterRight.shift).o
                val left = level.map(v.location.c + CharacterLeft.shift).o

                val addToMutableWallU = (f: CoordinateFixedDimension) => {
                    mutableWallU.append(f)
                    true
                }

                if (!Object.isPlayableArea(up) && !Object.isPlayableArea(right)) {
                    checkForU(v.location.c, CharacterDown.shift, CharacterRight.shift, x, visitedMap).exists(addToMutableWallU)
                    checkForU(v.location.c, CharacterLeft.shift, CharacterUp.shift, y, visitedMap).exists(addToMutableWallU)
                } else if (!Object.isPlayableArea(right) && !Object.isPlayableArea(down)) {
                    checkForU(v.location.c, CharacterUp.shift, CharacterRight.shift, x, visitedMap).exists(addToMutableWallU)
                    checkForU(v.location.c, CharacterLeft.shift, CharacterDown.shift, y, visitedMap).exists(addToMutableWallU)
                } else if (!Object.isPlayableArea(down) && !Object.isPlayableArea(left)) {
                    checkForU(v.location.c, CharacterUp.shift, CharacterLeft.shift, x, visitedMap).exists(addToMutableWallU)
                    checkForU(v.location.c, CharacterRight.shift, CharacterDown.shift, y, visitedMap).exists(addToMutableWallU)
                } else if (!Object.isPlayableArea(left) && !Object.isPlayableArea(up)) {
                    checkForU(v.location.c, CharacterDown.shift, CharacterLeft.shift, x, visitedMap).exists(addToMutableWallU)
                    checkForU(v.location.c, CharacterRight.shift, CharacterUp.shift, y, visitedMap).exists(addToMutableWallU)
                }
            }
        }

        mutableWallU.to[immutable.Seq]
    }

    private def applyCoordinateFixedDimension(state: StateBase, coordinateFixedDimension: CoordinateFixedDimension, coordinateVariables: CoordinateVariables): Clause = {
        if (coordinateFixedDimension.fiexedCoordinate.x) {
            And(Equals(coordinateVariables.x, IntegerConstant(coordinateFixedDimension.value)),
                GreaterEqual(coordinateVariables.y, IntegerConstant(coordinateFixedDimension.rangeFrom)),
                SmallerEqual(coordinateVariables.y, IntegerConstant(coordinateFixedDimension.rangeTo)))
        } else if (coordinateFixedDimension.fiexedCoordinate.y) {
            And(Equals(coordinateVariables.y, IntegerConstant(coordinateFixedDimension.value)),
                GreaterEqual(coordinateVariables.x, IntegerConstant(coordinateFixedDimension.rangeFrom)),
                SmallerEqual(coordinateVariables.x, IntegerConstant(coordinateFixedDimension.rangeTo)))
        } else {
            throw InvalidCoordinateFixedDimensionException()
        }
    }

    private def invariantWallU(state: StateBase, stateNext: StateBase, encoding: Encoding): Unit = {
        for (f <- findU) {
            for (b <- state.balls) {
                encoding.add(ClauseDeclaration(Implies(applyCoordinateFixedDimension(state, f, b), applyCoordinateFixedDimension(stateNext, f, b))))
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

            if (preActionCoordinate - characterLocation != Coordinate(0, 0)) {
                val allNodes = () => level.map.keys.toList
                val neighboursRelaxed  = (c: Coordinate) => SnowmanAction.CHARACTER_ACTIONS.map(f => c + f.shift).filter(f => {val l = level.map.get(f); l.isDefined && Object.isPlayableArea(l.get.o)})
                val neighbours = (c: Coordinate) => neighboursRelaxed(c).filter(f => !state.balls.exists(b => assignmentsMap(b.x.name).asInstanceOf[ValueInteger].v == f.x && assignmentsMap(b.y.name).asInstanceOf[ValueInteger].v == f.y))
                val heuristic = (start: Coordinate, goal: Coordinate) => start.euclideanDistance(goal).toFloat

                var path = AStar.aStar(characterLocation, preActionCoordinate, allNodes, neighbours, heuristic)
                if (path.isEmpty) {
                    path = AStar.aStar(characterLocation, preActionCoordinate, allNodes, neighboursRelaxed, heuristic)
                }

                val pathActions = pathToActions(path)

                actions.appendAll(pathActions)
            }

            actions.append(actionData.action)
            actionData.action match {
                case f: BallAction => actionsBalls.append(f)
            }

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
        reverse.zip(reverse.tail).map(f => SnowmanAction.CHARACTER_ACTIONS.find(f2 => f2.shift == (f._2 - f._1)).get).toList
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

    protected def encodeBallSizeEquals(ball: Ball, o: Object): Clause = o match {
        case SmallBall =>
            And(Not(ball.sizeA), Not(ball.sizeB))
        case MediumBall =>
            And(ball.sizeA, Not(ball.sizeB))
        case LargeBall =>
            And(ball.sizeA, ball.sizeB)
    }

    protected def encodeBallSizeSmaller(a: Ball, b: Ball): Clause = {
        And(Not(a.sizeB), b.sizeA, Implies(a.sizeA, b.sizeB))
    }

    protected def encodeBallSizeEquals(a: Ball, b: Ball): Clause = {
        And(Equivalent(a.sizeA, b.sizeA), Equivalent(a.sizeB, b.sizeB))
    }
}
