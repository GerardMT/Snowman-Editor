package gmt.snowman.encoder

import gmt.planner.action.Action
import gmt.planner.encoder.{EncoderResult, Encoding, EncodingData}
import gmt.snowman.level.`object`._
import gmt.snowman.level.{Coordinate, Level, Position}
import gmt.planner.{operation, _}
import gmt.planner.operation._
import gmt.planner.solver.Assignment

import scala.collection.mutable.ListBuffer

class EncoderReachability(level: Level) extends EncoderSnowman(level) {

    override def decode(assignments: Seq[Assignment], encodingData: EncodingData): Seq[Action] = ???

    override def encode(timeSteps: Int): EncoderResult = {
        val encoding = new Encoding

        val states = ListBuffer.empty[StateSnowman]

        val s0 = new StateSnowman(level, 0)
        s0.addVariables(encoding)
        states.append(s0)

        encoding.addAll(codifyState(level, s0))

        encoding.addAll(codifyReachability(level, s0)) // TODO Compliation process

        // S0 Occupied
        encoding.add(Comment("S0 Occupied"))
        for (p <- level.map.iterator) {
            if (p.o == Wall || gmt.snowman.level.`object`.Object.isBall(p.o)) {
                // OX_Y_S0
                encoding.add(ClauseDeclaration(s0.occupancy(p.c).get))
            } else {
                // !OX_Y_S0
                encoding.add(ClauseDeclaration(operation.Not(s0.occupancy(p.c).get)))
            }
        }

        // S0 Player
        encoding.add(Comment("S0 Player"))
        encoding.add(ClauseDeclaration(operation.Equals(s0.player.x, IntegerConstant(level.player.c.x))))
        encoding.add(ClauseDeclaration(operation.Equals(s0.player.y, IntegerConstant(level.player.c.y))))

        // S0 Balls
        encoding.add(Comment("S0 Balls"))
        for (b <- level.balls.indices) {
            encoding.add(ClauseDeclaration(operation.Equals(s0.balls(b).x, IntegerConstant(level.balls(b).c.x))))
            encoding.add(ClauseDeclaration(operation.Equals(s0.balls(b).y, IntegerConstant(level.balls(b).c.y))))
            encoding.add(ClauseDeclaration(operation.Equals(s0.balls(b).size, IntegerConstant(getBallSize(level.balls(b).o)))))
        }

        // S0 Snow
        encoding.add(Comment("S0 Snow"))
        for (p <- level.map.iterator) {
            s0.snow(p.c) match {
                case Some(v) =>
                    encoding.add(operation.ClauseDeclaration(v))
                case _ =>
            }
        }

        var sAnt = s0

        for (iS <- 1 until timeSteps + 1) {
            val s =  new StateSnowman(level, iS)
            states.append(s)
            s.addVariables(encoding)

            encoding.addAll(codifyState(level, s))
            encoding.addAll(codifyMiddleState(level, s))

            val actionsVariables = ListBuffer.empty[BooleanVariable]

            // Move Ball
            // Right
            val actionsCodification0 = codifyBallAction("0", level, s, sAnt, Coordinate(+1, 0))
            encoding.addAll(actionsCodification0.expressions)
            actionsVariables.appendAll(actionsCodification0.actionsVariables)
            // Left
            val actionsCodification1 = codifyBallAction("1", level, s, sAnt, Coordinate(-1, 0))
            encoding.addAll(actionsCodification1.expressions)
            actionsVariables.appendAll(actionsCodification1.actionsVariables)
            // Up
            val actionsCodification2 = codifyBallAction("2", level, s, sAnt, Coordinate(0, +1))
            encoding.addAll(actionsCodification2.expressions)
            actionsVariables.appendAll(actionsCodification2.actionsVariables)
            // Down
            val actionsCodification3 = codifyBallAction("3", level, s, sAnt, Coordinate(0, -1))
            encoding.addAll(actionsCodification3.expressions)
            actionsVariables.appendAll(actionsCodification3.actionsVariables)

            // EO(A0B0, A1B0...)
            encoding.add(Comment("EO Actions"))
            if (actionsVariables.length > 1) {
                encoding.addAll(Operations.getEO(actionsVariables, "AMOAS" + s.stateNumber))
            } else {
                encoding.add(operation.ClauseDeclaration(actionsVariables.head))
            }

            sAnt = s
        }

        // End
        val combinations = sAnt.balls.combinations(3).toList

        val combinationsVariables = ListBuffer.empty[BooleanVariable]

        if (combinations.size > 1) {
            encoding.add(Comment("End clauses"))
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

    def codifyState(l: Level, s: StateSnowman): Seq[Expression] = {
        val expressions = ListBuffer.empty[Expression]

        // TODO Per mes boles
        // Restriction 3 balls size 2
        val andsTwo = ListBuffer.empty[Clause]
        for (b <- s.balls) {
            andsTwo.append(operation.Equals(b.size, IntegerConstant(2)))
        }
        expressions.append(ClauseDeclaration(Not(And(andsTwo: _*))))

        for (c <- s.balls.combinations(2)) {
            val andsFour = ListBuffer.empty[Clause]
            for (b <- c) {
                andsFour.append(operation.Equals(b.size, IntegerConstant(4)))
            }
            expressions.append(ClauseDeclaration(Not(And(andsFour : _*))))
        }


        // Static Walls
        expressions.append(Comment("Static walls"))
        for(i <- l.map.iterator) {
            i.o match {
                case Wall =>
                    // OX_Y_
                    expressions.append(operation.ClauseDeclaration(s.occupancy(i.c).get))
                case _ =>
            }
        }

        expressions
    }

    def codifyReachability(l: Level, s: StateSnowman): Seq[Expression] = {
        val expressions = ListBuffer.empty[Expression]

        expressions.append(Comment("Reachability"))
        for (p <- l.map.iterator.filterNot(f => f.o == Wall)) {
            val ors = ListBuffer.empty[Clause]

            for (b <- l.balls.indices) {
                ors.append(And(operation.Equals(s.balls(b).x, IntegerConstant(p.c.x)), operation.Equals(s.balls(b).y, IntegerConstant(p.c.y))))
            }

            expressions.append(ClauseDeclaration(Implies(Or(ors: _*), operation.Not(s.reachableNodes(p.c).get))))
        }

        for (p <- l.map.iterator) {
            p match {
                case Position(_, Wall) =>
                case _ =>
                    val nodeStart = s.reachableNodes(p.c).get

                    val ors = ListBuffer.empty[Clause]

                    for (cOffset <- Level.cOffsets) {
                        val end = p.c + cOffset

                        l.map.get(end) match {
                            case Some(Position(_, Wall)) =>
                            case Some(_) =>
                                val edgeInverse = s.reachableEdges(end, p.c).get

                                ors.append(edgeInverse)

                                expressions.append(ClauseDeclaration(operation.Implies(edgeInverse, s.reachableNodes(end).get)))
                                expressions.append(ClauseDeclaration(operation.Implies(edgeInverse, operation.Smaller(s.reachableNodesAcyclicity(end).get, s.reachableNodesAcyclicity(p.c).get))))
                            case None =>
                        }
                    }

                    if (ors.nonEmpty) {
                        expressions.append(ClauseDeclaration(Equivalent(Not(And(operation.Equals(s.player.x, IntegerConstant(p.c.x)), operation.Equals(s.player.y, IntegerConstant(p.c.y)))), operation.Implies(nodeStart, Operations.simplify(Or(ors: _*))))))
                    }
            }
        }

        expressions
    }

    def codifyMiddleState(l: Level, s : StateSnowman): Seq[Expression] = {
        val expressions = ListBuffer.empty[Expression]

        expressions.appendAll(codifyReachability(l, s)) // TODO Inline // TODO de ser l'estat enterior

        // Occupied
        expressions.append(Comment("Occupied"))
        for (p <- l.map.iterator.filterNot(f => f.o == Wall)) {
            val ors = ListBuffer.empty[Clause]

            for (b <- l.balls.indices) {
                ors.append(And(operation.Equals(s.balls(b).x, IntegerConstant(p.c.x)), operation.Equals(s.balls(b).y, IntegerConstant(p.c.y)))) // TODO Es pot optimitzar? Pq només es mou la bola que fa l'acció, no cal fer un conjunt d'ors
            }

            // (B0XS_ = x & B0YS_ = y) | (B1XS_ = x & B1YS_ = y) | (B2XS_ = x & B2YS_ = y) ... = <-> OXxYyS_
            expressions.append(ClauseDeclaration(operation.Equivalent(Or(ors: _*), s.occupancy(p.c).get)))
        }

        expressions
    }

    private def clauseSumOffset(cOffset: Coordinate, c: And): Clause = { // TODO Nou metode passr dos parametres
        c match {
            case And(Equals(x, vA), Equals(y, vB)) =>
                val x1 = if (cOffset.x > 0) {
                    Add(x, IntegerConstant(cOffset.x))
                } else if (cOffset.x < 0){
                    Sub(x, IntegerConstant(-cOffset.x))
                } else {
                    x
                }
                val y1 = if (cOffset.y > 0) {
                    Add(y, IntegerConstant(cOffset.y))
                } else if (cOffset.y < 0){
                    Sub(y, IntegerConstant(-cOffset.y))
                } else {
                    y
                }

                And(Equals(x1, vA), Equals(y1, vB))
        }
    }

    private def codifyBallAction(name: String, l: Level, stateNext: StateSnowman, state: StateSnowman, cOffset: Coordinate): ActionCodification = {
        val expressions = ListBuffer.empty[Expression]

        val actionsVariables = ListBuffer.empty[BooleanVariable]

        for (((stateNextBall, stateBall), iB) <- stateNext.balls zip state.balls zipWithIndex) {
            val vA = BooleanVariable("A" + name +"B" + iB + "S" + state.stateNumber + "S" + stateNext.stateNumber)
            actionsVariables.append(vA)
            expressions.append(VariableDeclaration(vA))

            // Ball inside map. Checks for walls or !walls depending which clause is shorter
            val checkWall = l.map.iterator.filter(f => f.o ==Wall).toList.length < l.map.iterator.filterNot(f => f.o ==Wall).toList.length

            val orsInsideMapBall = ListBuffer.empty[Clause]
            for (p <- l.map.iterator) {
                if (checkWall && p.o == Wall || !checkWall && p.o != Wall) {
                    val nC = p.c - cOffset
                    if (l.map.contains(nC)) {
                        val x = if (cOffset.x != 0) {
                            IntegerConstant(nC.x)
                        } else {
                            IntegerConstant(p.c.x)
                        }

                        val y = if (cOffset.y != 0) {
                            IntegerConstant(nC.y)
                        } else {
                            IntegerConstant(p.c.y)
                        }
                        orsInsideMapBall.append(And(operation.Equals(x, stateBall.x), operation.Equals(y, stateBall.y)))
                    }
                }
            }
            val insideMapBall = if (checkWall) Not(Or(orsInsideMapBall: _*)) else Or(orsInsideMapBall: _*)

            // No ball on top
            val orsBallOnTop = ListBuffer.empty[Clause]
            for (kB <- state.balls) {
                if (kB != stateBall) {
                    orsBallOnTop.append(And(operation.Equals(stateBall.x, kB.x), operation.Equals(stateBall.y, kB.y), operation.Greater(stateBall.size, kB.size)))
                }
            }
            val ballOnTop = Operations.simplify(Or(orsBallOnTop: _*))


            val combinations = state.balls.filter(f => f != stateBall).combinations(2).map(f => (f.head, f(1))).toList

            // Two balls in front
            val orsTwoBallsInFront = ListBuffer.empty[Clause]
            for ((kB1, kB2) <- combinations) {
                orsTwoBallsInFront.append(And(clauseSumOffset(cOffset, And(operation.Equals(stateBall.x, kB1.x), operation.Equals(stateBall.y, kB1.y))), clauseSumOffset(cOffset, And(operation.Equals(stateBall.x, kB2.x), operation.Equals(stateBall.y, kB2.y)))))
            }
            val twoBallsInFront = Operations.simplify(Or(orsTwoBallsInFront: _*))

            val orsTwoBallsInFrontWithSmall = ListBuffer.empty[Clause]
            for ((kB1, kB2) <- combinations) {
                orsTwoBallsInFrontWithSmall.append(And(clauseSumOffset(cOffset, And(operation.Equals(stateBall.x, kB1.x), operation.Equals(stateBall.y, kB1.y))), clauseSumOffset(cOffset, And(operation.Equals(stateBall.x, kB2.x), operation.Equals(stateBall.y, kB2.y))), Or(operation.Equals(kB1.size, IntegerConstant(1)), operation.Equals(kB2.size, IntegerConstant(1)))))
            }
            val twoBallsInFrontWithSmall = Operations.simplify(Or(orsTwoBallsInFrontWithSmall: _*))

            // Pre (ballInFront) & !(ballUnder) -> (ballInFrontBigger)
            // Ball in front
            val orsBallInFront = ListBuffer.empty[Clause]
            for (kB <- state.balls) {
                if (kB != stateBall) {
                    orsBallInFront.append(clauseSumOffset(cOffset, And(operation.Equals(stateBall.x, kB.x), operation.Equals(stateBall.y, kB.y))))
                }
            }
            val ballInFront = Operations.simplify(Or(orsBallInFront: _*))

            // No ball under
            val orsBallUnder = ListBuffer.empty[Clause]
            for (kB <- state.balls) {
                if (kB != stateBall) {
                    orsBallUnder.append(And(operation.Equals(stateBall.x, kB.x), operation.Equals(stateBall.y, kB.y)))
                }
            }
            val ballUnder = Operations.simplify(Or(orsBallUnder: _*))

            // Ball in front bigger
            val orsBallInFrontBigger = ListBuffer.empty[Clause]
            for (kB <- state.balls) {
                if (kB != stateBall) {
                    orsBallInFrontBigger.append(And(clauseSumOffset(cOffset, And(operation.Equals(stateBall.x, kB.x), operation.Equals(stateBall.y, kB.y))), operation.Smaller(stateBall.size, kB.size)))
                }
            }
            val ballInFrontBigger = Operations.simplify(Or(orsBallInFrontBigger: _*))

            // Equal other balls variables
            val equalBallsVariables = ListBuffer.empty[Clause]
            for (((kSB, kSantB), kIB) <- stateNext.balls zip state.balls zipWithIndex) {
                if (kIB != iB) {
                    equalBallsVariables.append(operation.Equals(kSB.x, kSantB.x))
                    equalBallsVariables.append(operation.Equals(kSB.y, kSantB.y))
                    equalBallsVariables.append(operation.Equals(kSB.size, kSantB.size))
                }
            }

            // Inside map player
            val orsInsideMapPlayer = ListBuffer.empty[Clause]
            for (p <- l.map.iterator) {
                state.occupancy(p.c - cOffset) match {
                    case Some(v) =>
                        orsInsideMapPlayer.append(And(operation.Equals(stateBall.x, IntegerConstant(p.c.x)), operation.Equals(stateBall.y, IntegerConstant(p.c.y)), operation.Not(v)))
                    case None =>
                }
            }
            val insideMapPlayer = Or(orsInsideMapPlayer: _*)

            // Reachable
            val orsReachable = ListBuffer.empty[Clause]
            for (p <- l.map.iterator) {
                state.reachableNodes(p.c - cOffset) match {
                    case Some(v) =>
                        orsReachable.append(operation.And(operation.Equals(stateBall.x, IntegerConstant(p.c.x)), operation.Equals(stateBall.y, IntegerConstant(p.c.y)), v))
                    case None =>
                }
            }
            val reachable = Or(orsReachable: _*)

            // Equal snow
            val equalSnowVariables = ListBuffer.empty[Clause]
            for (p <- l.map.iterator) {
                val c = p.c + cOffset
                state.snow(c) match {
                    case Some(v) =>
                        val sSnow = stateNext.snow(c).get
                        val a = operation.And(v, Not(operation.And(v, operation.Equals(stateBall.x, IntegerConstant(p.c.x)), operation.Equals(stateBall.y, IntegerConstant(p.c.y)))))
                        // Snow & !(Snow & Ball) <-> SnowNextState
                        equalSnowVariables.append(operation.Implies(a, sSnow))
                        equalSnowVariables.append(operation.Implies(sSnow, a))
                    case None =>
                }
            }

            val ballUnderVar = BooleanVariable("TVAR0A" + name + "B" + iB + "S" + stateNext.stateNumber)
            expressions.append(Comment("Ball under"))
            expressions.append(VariableDeclaration(ballUnderVar))
            expressions.append(ClauseDeclaration(Implies(ballUnder, ballUnderVar)))
            expressions.append(ClauseDeclaration(Implies(ballUnderVar, ballUnder)))

            val pre = And(
                insideMapBall, // TODO Millorar implementacio de dins del mapa
                reachable,
                insideMapPlayer,
                Not(ballOnTop),
                Not(And(ballInFront, ballUnderVar)),
                Implies(And(ballInFront, Not(ballUnderVar)), ballInFrontBigger),
                Implies(twoBallsInFront, operation.Equals(stateBall.size, IntegerConstant(1))), // TODO Optimitzar el bug 6 + 2. Fet a la documentacio, falta implementar
                Not(twoBallsInFrontWithSmall))

            val andsEf = ListBuffer.empty[Clause]
            andsEf.append(clauseSumOffset(cOffset, And(operation.Equals(stateBall.x, stateNextBall.x),operation.Equals(stateBall.y, stateNextBall.y))))
            andsEf.append(Implies(Not(ballUnderVar), And(operation.Equals(stateNext.player.x, stateBall.x), operation.Equals(stateNext.player.y, stateBall.y))))
            andsEf.append(Implies(ballUnderVar, clauseSumOffset(-cOffset, And(operation.Equals(stateBall.x, stateNext.player.x), operation.Equals(stateBall.y, stateNext.player.y)))))
            andsEf.appendAll(equalSnowVariables)
            andsEf.appendAll(equalBallsVariables)

            if (l.hasSnow) {
                // There is snow
                val orsTheresSnow = ListBuffer.empty[Clause]
                for (p <- l.map.iterator) {
                    state.snow(p.c + cOffset) match {
                        case Some(v) =>
                            orsTheresSnow.append(operation.And(operation.Equals(stateBall.x, IntegerConstant(p.c.x)), operation.Equals(stateBall.y, IntegerConstant(p.c.y)), v))
                        case None =>
                    }
                }
                val theresSnow = Operations.simplify(Or(orsTheresSnow: _*))


                val theresSnowVar = BooleanVariable("TVAR1A" + name + "B" + iB + "S" + stateNext.stateNumber)
                expressions.append(Comment("There is snow"))
                expressions.append(VariableDeclaration(theresSnowVar))
                expressions.append(ClauseDeclaration(Implies(theresSnow, theresSnowVar)))
                expressions.append(ClauseDeclaration(Implies(theresSnowVar, theresSnow)))

                andsEf.append(Implies(And(theresSnowVar, operation.Equals(stateBall.size, IntegerConstant(1))), operation.Equals(stateNextBall.size, IntegerConstant(2))))
                andsEf.append(Implies(And(theresSnowVar, operation.Equals(stateBall.size, IntegerConstant(2))), operation.Equals(stateNextBall.size, IntegerConstant(4))))
                andsEf.append(Implies(Or(Not(theresSnowVar), operation.Equals(stateBall.size, IntegerConstant(4))), operation.Equals(stateNextBall.size, stateBall.size)))
            } else {
                andsEf.append(operation.Equals(stateNextBall.size, stateBall.size))
            }

            val ef = And(andsEf: _*)

            // EF_ <-> A_
            expressions.append(Comment("EF <-> A" + name))
            expressions.append(ClauseDeclaration(Implies(ef, vA)))
            expressions.append(ClauseDeclaration(Implies(vA, ef)))
            // A_ -> PRE_
            expressions.append(Comment("A" + name + " -> PRE"))
            expressions.append(ClauseDeclaration(Implies(vA, pre)))
        }

        ActionCodification(expressions, actionsVariables)
    }

    case class ActionCodification(expressions: Seq[Expression], actionsVariables: Seq[BooleanVariable])

    override def moveCharacterActions: Seq[Expression] = ???

    override def createState(level: Level, index: Int): StateSnowman = ???

    override def createBallAction(): (Clause, Clause, Seq[Expression]) = {
        val otherBallUnderVar = otherBallUnder

        val pre = And(noWallInFront,
            noOtherBallsOver,
            Not(And(otherBallInFront, otherBallUnderVar)),
            otherBallsInFrontLarger,
            characterPositionValid,
            reachability)

        val eff = And(moveBall,
            Implies(Not(otherBallUnderVar), teleportCharacter),
            Implies(otherBallUnderVar, equalCharacterVariables),
            equalOtherBallsVariables,
            updateSnowVariables)

        val expressions = List(VariableDeclaration(otherBallUnderVar))

        (pre, eff, expressions)
    }

    private def reachability(): Clause = ???

    private def teleportCharacter(): Clause = ???
}