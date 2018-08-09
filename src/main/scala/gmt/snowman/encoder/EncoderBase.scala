package gmt.snowman.encoder

import gmt.planner.encoder.{Encoder, EncoderResult, Encoding}
import gmt.planner.operation
import gmt.planner.operation._
import gmt.planner.solver.Assignment
import gmt.planner.solver.value.{ValueBoolean, ValueInteger}
import gmt.snowman.action.SnowmanAction
import gmt.snowman.level.`object`._
import gmt.snowman.level.{Coordinate, Level, `object`}

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

abstract class EncoderBase[A <: StateBase, B <: SnowmanAction](val level: Level) extends Encoder[B, SnowmanEncodingData] {

    override def encode(timeSteps: Int): EncoderResult[SnowmanEncodingData] = {
        val encoding = new Encoding

        val states = ListBuffer.empty[A]

        val statesData = ListBuffer.empty[SnowmanEncodingData.StateData]

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

            val actionsData = ListBuffer.empty[SnowmanEncodingData.ActionData]

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

                    actionsData.append(SnowmanEncodingData.ActionData(action, actionVariable, iB))
                }
            }

            encoding.add(Comment("EO Actions")) // TODO DEBUG
            if (actionsVariables.length > 1) {
                encoding.addAll(Operations.getEO(actionsVariables, "EO_A" + timeStep))
            } else {
                encoding.add(ClauseDeclaration(actionsVariables.head))
            }

            statesData.append(SnowmanEncodingData.StateData(stateNext, actionsData.toList))

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

        EncoderResult(encoding, SnowmanEncodingData(level, state0, statesData.toList))
    }

    protected def createState(level: Level, index: Int): A

    protected def encodeCharacterState0(state0: A, encoding: Encoding)

    protected def encodeReachability(state: A, encoing: Encoding)

    protected def encodeCharacterAction(actionName: String, state: A, stateNext: A, action: SnowmanAction, encoding: Encoding, actionVariables: mutable.Buffer[BooleanVariable], actionsData: mutable.Buffer[SnowmanEncodingData.ActionData])

    protected def createBallAction(actoinName: String, state: A, stateActionBall: StateBase.Ball, stateNext: A, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Clause, Seq[Expression])

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

    protected def otherBallInFront(state: StateBase, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        Or((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            applyShiftClause(stateActionBall.x, stateActionBall.y, b.x, b.y, shift, and)
        }): _*)
    }

    protected def otherBallsInFrontLarger(state: StateBase, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        And((for (b <- state.balls.filter(f => f != stateActionBall)) yield {
            Or(applyShiftClause(stateActionBall.x, stateActionBall.y, b.x, b.y, shift, or), Smaller(stateActionBall.size, b.size))
        }): _*)
    }

    protected def moveBall(stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        applyShiftClause(stateActionBall.x, stateActionBall.y, stateNextActionBall.x, stateNextActionBall.y, shift, and)
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
        Operations.simplify(And((for ((c, s) <- falttenTuple(level.map.values.map(f => (f.c, state.snow.get(f.c + shift))))) yield {
            Equivalent(And(s, Not(And(Equals(stateActionBall.x, IntegerConstant(c.x)), Equals(stateActionBall.y, IntegerConstant(c.y))))), stateNext.snow.get(c + shift).get)
        }).toSeq: _*))
    }

    protected def characterLocatoinTeleportValid[T <: StateBase](state: T, stateActionBall: StateBase.Ball, shift: Coordinate): Clause = {
        Or((for ((c, o) <- falttenTuple(level.map.values.map(f => (f.c, state.occupancy.get(f.c - shift))))) yield {
            And(Equals(stateActionBall.x, IntegerConstant(c.x)), Equals(stateActionBall.y, IntegerConstant(c.y)), Not(o))
        }).toSeq: _*)
    }

    protected def updateBallSize(actionName: String, state: StateBase, stateActionBall: StateBase.Ball, stateNextActionBall: StateBase.Ball, shift: Coordinate): (Clause, Seq[Expression]) = {
        if (level.hasSnow) {
            val expressions = ListBuffer.empty[Expression]

            val theresSnow = Operations.simplify(Or((for ((c, s) <- falttenTuple(level.map.values.map(f => (f.c, state.snow.get(f.c + shift))))) yield {
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

    protected def applyShiftClause(applyX: Clause, applyY: Clause, bX: Clause, bY: Clause, shift: Coordinate, operation: (Clause, Clause) => Clause): Clause = { // TODO Es pot millorar? Herencia player ball
        val newX = if (shift.x > 0) {
            Add(applyX, IntegerConstant(shift.x))
        } else if (shift.x < 0){
            Sub(applyX, IntegerConstant(-shift.x))
        } else {
            applyX
        }
        val newY = if (shift.y > 0) {
            Add(applyY, IntegerConstant(shift.y))
        } else if (shift.y < 0){
            Sub(applyY, IntegerConstant(-shift.y))
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

    private def occupancyWalls(state: StateBase): Clause = {
        And((for (o <- level.map.values.filter(f => f.o == Wall).flatMap(f => state.occupancy.get(f.c))) yield {
            o
        }).toSeq: _*)
    }

    def decodeTeleport(assignments: Seq[Assignment], encodingData: SnowmanEncodingData): immutable.Seq[SnowmanAction] = {
        val assignmentsMap = assignments.map(f => (f.name, f.value)).toMap

        val actions = ListBuffer.empty[SnowmanAction]

        var startCoordinate = encodingData.level.character.c
        var state = encodingData.state0
        for (stateData <- encodingData.statesData) {

            val iterator = stateData.actionsData.iterator
            var found = false

            while (iterator.hasNext && !found) {
                val actionData = iterator.next()

                if (assignmentsMap(actionData.actionVariable.name).asInstanceOf[ValueBoolean].v) {
                    found = true

                    val ball = stateData.stateNext.balls(actionData.ballActionIndex)

                    val ballCoordinateNext = Coordinate(assignmentsMap(ball.x.name).asInstanceOf[ValueInteger].v, assignmentsMap(ball.y.name).asInstanceOf[ValueInteger].v)
                    val ballCoordinate = ballCoordinateNext - actionData.action.shift
                    val characterCoordinate =  ballCoordinate - actionData.action.shift

                    val ballsIterator = state.balls.patch(actionData.ballActionIndex, Nil, 1).iterator

                    var ballFound = false
                    while (!ballFound && ballsIterator.hasNext) {
                        val findBall = ballsIterator.next
                        val findBallCoordinate = Coordinate(assignmentsMap(findBall.x.name).asInstanceOf[ValueInteger].v, assignmentsMap(findBall.y.name).asInstanceOf[ValueInteger].v)

                        ballFound = findBallCoordinate == ballCoordinate
                    }

                    val newCharacterCoordinate = if (ballFound) {
                        characterCoordinate
                    } else {
                        ballCoordinate
                    }

                    val isOccupied = (c: Coordinate) => assignmentsMap(state.occupancy.get(c).get.name).asInstanceOf[ValueBoolean].v

                    if (startCoordinate.manhattanDistance(characterCoordinate) > 1) {
                        val a = aStar(startCoordinate, characterCoordinate, level, isOccupied)
                        a.foreach(f => println(f.getClass.getCanonicalName))
                        actions.appendAll(a)
                    }

                    actions.append(actionData.action)

                    startCoordinate = newCharacterCoordinate
                }
            }

            state = stateData.stateNext
        }

        actions.toList
    }

    protected def aStar(start: Coordinate, goal: Coordinate, level: Level, isOccupied: Coordinate => Boolean): immutable.Seq[SnowmanAction] = {
        val closedSet = mutable.Set.empty[Coordinate]

        val openSet = mutable.Set.empty[Coordinate]
        openSet += start

        val cameFrom = mutable.Map.empty[Coordinate, Coordinate]

        val gScore = mutable.Map.empty[Coordinate, Float]
        for (l <- level.map.values) {
            gScore(l.c) = Float.PositiveInfinity
        }
        gScore(start) = 0.0f

        val fScore = mutable.Map.empty[Coordinate, Float]
        for (l <- level.map.values) {
            fScore(l.c) = Float.PositiveInfinity
        }
        fScore(start) = heuristic(start, goal)

        while (openSet.nonEmpty) {
            var current = fScore.filter(f => openSet(f._1)).toList.minBy(_._2)._1
            if (current == goal) {
                return reconstrucPath(cameFrom, current)
            }

            openSet -= current
            closedSet += current

            for (neighbor <- SnowmanAction.ACTIONS.map(f => f.shift + current).flatMap(f => level.map.get(f)).filter(f => Object.isPlayableArea(f.o) && !isOccupied(f.c)).map(f => f.c)) {
                if (!closedSet(neighbor)) {
                    val tentative_gScore = gScore(current) + 1

                    if (!openSet(neighbor)) {
                        openSet += neighbor
                    }

                    if (tentative_gScore < gScore(neighbor)) {
                        cameFrom(neighbor) = current
                        gScore(neighbor) = tentative_gScore
                        fScore(neighbor) = gScore(neighbor) + heuristic(neighbor, goal)
                    }
                }
            }
        }

        reconstrucPath(cameFrom, goal)
    }

    private def heuristic(start: Coordinate, goal: Coordinate): Float = start.euclidianDistance(goal).toFloat

    private def reconstrucPath(cameFrom: mutable.Map[Coordinate, Coordinate], start: Coordinate): immutable.Seq[SnowmanAction] = {
        val totalPath = ListBuffer(start)

        var current = start
        while (cameFrom.keys.exists(f => f == current)) {
            current = cameFrom(current)
            totalPath.append(current)
        }

        val reverse = totalPath.reverse
        reverse.zip(reverse.tail).map(f => SnowmanAction.ACTIONS.find(f2 => f2.shift == (f._2 - f._1)).get).toList
    }

    def falttenTuple[T,U](l: Iterator[(T, Option[U])]): Iterator[(T, U)] = l.filter(f => f._2.isDefined).map(f => (f._1, f._2.get))
}
