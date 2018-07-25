package snowman.solver.encoder

import snowman.level.objects._
import snowman.level.{Coordinate, Level, Position, objects}
import snowman.solver._
import snowman.solver.timeStepSolver.solver._

import scala.collection.mutable.ListBuffer

object EncoderReachability extends Encoder {

    override def encode(l: Level, actions: Int): Encoding = {
        val problem = new Problem

        val states = ListBuffer.empty[State]

        val s0 = new State(l, 0)
        s0.addVariables(problem)
        states.append(s0)

        problem.addAll(codifyState(l, s0))

        problem.addAll(codifyReachability(l, s0)) // TODO Compliation process

        // S0 Occupied
        problem.add(Comment("S0 Occupied"))
        for (p <- l.map.iterator) {
            if (p.o == Wall || snowman.level.objects.Object.isBall(p.o)) {
                // OX_Y_S0
                problem.add(ClauseDeclaration(s0.occupied(p.c).get))
            } else {
                // !OX_Y_S0
                problem.add(ClauseDeclaration(Not(s0.occupied(p.c).get)))
            }
        }

        // S0 Player
        problem.add(Comment("S0 Player"))
        problem.add(ClauseDeclaration(Equals(s0.player.x, IntegerConstant(l.player.c.x))))
        problem.add(ClauseDeclaration(Equals(s0.player.y, IntegerConstant(l.player.c.y))))

        // S0 Balls
        problem.add(Comment("S0 Balls"))
        for (b <- l.balls.indices) {
            problem.add(ClauseDeclaration(Equals(s0.balls(b).x, IntegerConstant(l.balls(b).c.x))))
            problem.add(ClauseDeclaration(Equals(s0.balls(b).y, IntegerConstant(l.balls(b).c.y))))
            problem.add(ClauseDeclaration(Equals(s0.balls(b).size, IntegerConstant(getBallSize(l.balls(b).o)))))
        }

        // S0 Snow
        problem.add(Comment("S0 Snow"))
        for (p <- l.map.iterator) {
            s0.snow(p.c) match {
                case Some(v) =>
                    problem.add(ClauseDeclaration(v))
                case _ =>
            }
        }

        var sAnt = s0

        for (iS <- 1 until actions + 1) {
            val s =  new State(l, iS)
            states.append(s)
            s.addVariables(problem)

            problem.addAll(codifyState(l, s))
            problem.addAll(codifyMiddleState(l, s))

            val actionsVariables = ListBuffer.empty[BooleanVariable]

            // Move Ball
            // Right
            val actionsCodification0 = codifyBallAction("0", l, s, sAnt, Coordinate(+1, 0))
            problem.addAll(actionsCodification0.expressions)
            actionsVariables.appendAll(actionsCodification0.actionsVariables)
            // Left
            val actionsCodification1 = codifyBallAction("1", l, s, sAnt, Coordinate(-1, 0))
            problem.addAll(actionsCodification1.expressions)
            actionsVariables.appendAll(actionsCodification1.actionsVariables)
            // Up
            val actionsCodification2 = codifyBallAction("2", l, s, sAnt, Coordinate(0, +1))
            problem.addAll(actionsCodification2.expressions)
            actionsVariables.appendAll(actionsCodification2.actionsVariables)
            // Down
            val actionsCodification3 = codifyBallAction("3", l, s, sAnt, Coordinate(0, -1))
            problem.addAll(actionsCodification3.expressions)
            actionsVariables.appendAll(actionsCodification3.actionsVariables)

            // EO(A0B0, A1B0...)
            problem.add(Comment("EO Actions"))
            if (actionsVariables.length > 1) {
                problem.addAll(Operations.getEO(actionsVariables, "AMOAS" + s.stateNumber))
            } else {
                problem.add(ClauseDeclaration(actionsVariables.head))
            }

            sAnt = s
        }

        // End
        val combinations = sAnt.balls.combinations(3).toList

        val combinationsVariables = ListBuffer.empty[BooleanVariable]

        if (combinations.size > 1) {
            problem.add(Comment("End clauses"))
            for ((c, i) <- combinations.zipWithIndex) {
                val v = BooleanVariable("C" + i)

                problem.add(Comment("Combination " + i))
                combinationsVariables.append(v)
                problem.add(VariableDeclaration(v))
                problem.add(ClauseDeclaration(DoubleImplication(And(Equals(c(1).x, c.head.x), Equals(c(1).y, c.head.y), Equals(c(2).x, c.head.x), Equals(c(2).y, c.head.y)), v)))
            }

            problem.addAll(Operations.addEK(combinationsVariables, l.balls.size / 3, "EKC"))
        } else {
            val c = combinations.head
            c.tail.foreach(f => problem.add(ClauseDeclaration(And(Equals(f.x, c.head.x), Equals(f.y, c.head.y)))))
        }

        Encoding(problem, states)
    }

    def codifyState(l: Level, s: State): Seq[Expression] = {
        val expressions = ListBuffer.empty[Expression]

        // TODO Per mes boles
        // Restriction 3 balls size 2
        val andsTwo = ListBuffer.empty[Clause]
        for (b <- s.balls) {
            andsTwo.append(Equals(b.size, IntegerConstant(2)))
        }
        expressions.append(ClauseDeclaration(Not(And(andsTwo: _*))))

        for (c <- s.balls.combinations(2)) {
            val andsFour = ListBuffer.empty[Clause]
            for (b <- c) {
                andsFour.append(Equals(b.size, IntegerConstant(4)))
            }
            expressions.append(ClauseDeclaration(Not(And(andsFour : _*))))
        }


        // Static Walls
        expressions.append(Comment("Static walls"))
        for(i <- l.map.iterator) {
            i.o match {
                case Wall =>
                    // OX_Y_
                    expressions.append(ClauseDeclaration(s.occupied(i.c).get))
                case _ =>
            }
        }

        expressions
    }

    def codifyReachability(l: Level, s: State): Seq[Expression] = {
        val expressions = ListBuffer.empty[Expression]

        expressions.append(Comment("Reachability"))
        for (p <- l.map.iterator.filterNot(f => f.o == Wall)) {
            val ors = ListBuffer.empty[Clause]

            for (b <- l.balls.indices) {
                ors.append(And(Equals(s.balls(b).x, IntegerConstant(p.c.x)), Equals(s.balls(b).y, IntegerConstant(p.c.y))))
            }

            expressions.append(ClauseDeclaration(Implication(Or(ors: _*), Not(s.reachableNodes(p.c).get))))
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

                                expressions.append(ClauseDeclaration(Implication(edgeInverse, s.reachableNodes(end).get)))
                                expressions.append(ClauseDeclaration(Implication(edgeInverse, Smaller(s.reachableNodesAcyclicity(end).get, s.reachableNodesAcyclicity(p.c).get))))
                            case None =>
                        }
                    }

                    if (ors.nonEmpty) {
                        expressions.append(ClauseDeclaration(DoubleImplication(Not(And(Equals(s.player.x, IntegerConstant(p.c.x)), Equals(s.player.y, IntegerConstant(p.c.y)))), Implication(nodeStart, Operations.simplify(Or(ors: _*))))))
                    }
            }
        }

        expressions
    }

    def codifyMiddleState(l: Level, s : State): Seq[Expression] = {
        val expressions = ListBuffer.empty[Expression]

        expressions.appendAll(codifyReachability(l, s)) // TODO Inline

        // Occupied
        expressions.append(Comment("Occupied"))
        for (p <- l.map.iterator.filterNot(f => f.o == Wall)) {
            val ors = ListBuffer.empty[Clause]

            for (b <- l.balls.indices) {
                ors.append(And(Equals(s.balls(b).x, IntegerConstant(p.c.x)), Equals(s.balls(b).y, IntegerConstant(p.c.y)))) // TODO Es pot optimitzar? Pq només es mou la bola que fa l'acció, no cal fer un conjunt d'ors
            }

            // (B0XS_ = x & B0YS_ = y) | (B1XS_ = x & B1YS_ = y) | (B2XS_ = x & B2YS_ = y) ... = <-> OXxYyS_
            expressions.append(ClauseDeclaration(DoubleImplication(Or(ors: _*), s.occupied(p.c).get)))
        }

        expressions
    }

    private def clauseSumOffset(cOffset: Coordinate, c: And): Clause = {
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

    private def codifyBallAction(name: String, l: Level, s: State, sAnt: State, cOffset: Coordinate): ActionCodification = {
        val expressions = ListBuffer.empty[Expression]

        val actionsVariables = ListBuffer.empty[BooleanVariable]

        for (((sB, sAntB), iB) <- s.balls zip sAnt.balls zipWithIndex) {
            val vA = BooleanVariable("A" + name +"B" + iB + "S" + sAnt.stateNumber + "S" + s.stateNumber)
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
                        orsInsideMapBall.append(And(Equals(x, sAntB.x), Equals(y, sAntB.y)))
                    }
                }
            }
            val insideMapBall = if (checkWall) Not(Or(orsInsideMapBall: _*)) else Or(orsInsideMapBall: _*)

            // No ball on top
            val orsBallOnTop = ListBuffer.empty[Clause]
            for (kB <- sAnt.balls) {
                if (kB != sAntB) {
                    orsBallOnTop.append(And(Equals(sAntB.x, kB.x), Equals(sAntB.y, kB.y), Greater(sAntB.size, kB.size)))
                }
            }
            val ballOnTop = Operations.simplify(Or(orsBallOnTop: _*))


            val combinations = sAnt.balls.filter(f => f != sAntB).combinations(2).map(f => (f.head, f(1))).toList

            // Two balls in front
            val orsTwoBallsInFront = ListBuffer.empty[Clause]
            for ((kB1, kB2) <- combinations) {
                orsTwoBallsInFront.append(And(clauseSumOffset(cOffset, And(Equals(sAntB.x, kB1.x), Equals(sAntB.y, kB1.y))), clauseSumOffset(cOffset, And(Equals(sAntB.x, kB2.x), Equals(sAntB.y, kB2.y)))))
            }
            val twoBallsInFront = Operations.simplify(Or(orsTwoBallsInFront: _*))

            val orsTwoBallsInFrontWithSmall = ListBuffer.empty[Clause]
            for ((kB1, kB2) <- combinations) {
                orsTwoBallsInFrontWithSmall.append(And(clauseSumOffset(cOffset, And(Equals(sAntB.x, kB1.x), Equals(sAntB.y, kB1.y))), clauseSumOffset(cOffset, And(Equals(sAntB.x, kB2.x), Equals(sAntB.y, kB2.y))), Or(Equals(kB1.size, IntegerConstant(1)), Equals(kB2.size, IntegerConstant(1)))))
            }
            val twoBallsInFrontWithSmall = Operations.simplify(Or(orsTwoBallsInFrontWithSmall: _*))

            // Pre (ballInFront) & !(ballUnder) -> (ballInFrontBigger)
            // Ball in front
            val orsBallInFront = ListBuffer.empty[Clause]
            for (kB <- sAnt.balls) {
                if (kB != sAntB) {
                    orsBallInFront.append(clauseSumOffset(cOffset, And(Equals(sAntB.x, kB.x), Equals(sAntB.y, kB.y))))
                }
            }
            val ballInFront = Operations.simplify(Or(orsBallInFront: _*))

            // No ball under
            val orsBallUnder = ListBuffer.empty[Clause]
            for (kB <- sAnt.balls) {
                if (kB != sAntB) {
                    orsBallUnder.append(And(Equals(sAntB.x, kB.x), Equals(sAntB.y, kB.y)))
                }
            }
            val ballUnder = Operations.simplify(Or(orsBallUnder: _*))

            // Ball in front bigger
            val orsBallInFrontBigger = ListBuffer.empty[Clause]
            for (kB <- sAnt.balls) {
                if (kB != sAntB) {
                    orsBallInFrontBigger.append(And(clauseSumOffset(cOffset, And(Equals(sAntB.x, kB.x), Equals(sAntB.y, kB.y))), Smaller(sAntB.size, kB.size)))
                }
            }
            val ballInFrontBigger = Operations.simplify(Or(orsBallInFrontBigger: _*))

            // Equal other balls variables
            val equalBallsVariables = ListBuffer.empty[Clause]
            for (((kSB, kSantB), kIB) <- s.balls zip sAnt.balls zipWithIndex) {
                if (kIB != iB) {
                    equalBallsVariables.append(Equals(kSB.x, kSantB.x))
                    equalBallsVariables.append(Equals(kSB.y, kSantB.y))
                    equalBallsVariables.append(Equals(kSB.size, kSantB.size))
                }
            }

            // Inside map player
            val orsInsideMapPlayer = ListBuffer.empty[Clause]
            for (p <- l.map.iterator) {
                sAnt.occupied(p.c - cOffset) match {
                    case Some(v) =>
                        orsInsideMapPlayer.append(And(Equals(sAntB.x, IntegerConstant(p.c.x)), Equals(sAntB.y, IntegerConstant(p.c.y)), Not(v)))
                    case None =>
                }
            }
            val insideMapPlayer = Or(orsInsideMapPlayer: _*)

            // Reachable
            val orsReachable = ListBuffer.empty[Clause]
            for (p <- l.map.iterator) {
                sAnt.reachableNodes(p.c - cOffset) match {
                    case Some(v) =>
                        orsReachable.append(And(Equals(sAntB.x, IntegerConstant(p.c.x)), Equals(sAntB.y, IntegerConstant(p.c.y)), v))
                    case None =>
                }
            }
            val reachable = Or(orsReachable: _*)

            // Equal snow
            val equalSnowVariables = ListBuffer.empty[Clause]
            for (p <- l.map.iterator) {
                val c = p.c + cOffset
                sAnt.snow(c) match {
                    case Some(v) =>
                        val sSnow = s.snow(c).get
                        val a = And(v, Not(And(v, Equals(sAntB.x, IntegerConstant(p.c.x)), Equals(sAntB.y, IntegerConstant(p.c.y)))))
                        // Snow & !(Snow & Ball) <-> SnowNextState
                        equalSnowVariables.append(Implication(a, sSnow))
                        equalSnowVariables.append(Implication(sSnow, a))
                    case None =>
                }
            }

            val ballUnderVar = BooleanVariable("TVAR0A" + name + "B" + iB + "S" + s.stateNumber)
            expressions.append(Comment("Ball under"))
            expressions.append(VariableDeclaration(ballUnderVar))
            expressions.append(ClauseDeclaration(Implication(ballUnder, ballUnderVar)))
            expressions.append(ClauseDeclaration(Implication(ballUnderVar, ballUnder)))

            val pre = And(
                insideMapBall, // TODO Millorar implementacio de dins del mapa
                reachable,
                insideMapPlayer,
                Not(ballOnTop),
                Not(And(ballInFront, ballUnderVar)),
                Implication(And(ballInFront, Not(ballUnderVar)), ballInFrontBigger),
                Implication(twoBallsInFront, Equals(sAntB.size, IntegerConstant(1))), // TODO Optimitzar el bug 6 + 2
                Not(twoBallsInFrontWithSmall))

            val andsEf = ListBuffer.empty[Clause]
            andsEf.append(clauseSumOffset(cOffset, And(Equals(sAntB.x, sB.x),Equals(sAntB.y, sB.y))))
            andsEf.append(Implication(Not(ballUnderVar), And(Equals(s.player.x, sAntB.x), Equals(s.player.y, sAntB.y))))
            andsEf.append(Implication(ballUnderVar, clauseSumOffset(-cOffset, And(Equals(sAntB.x, s.player.x), Equals(sAntB.y, s.player.y)))))
            andsEf.appendAll(equalSnowVariables)
            andsEf.appendAll(equalBallsVariables)

            if (l.hasSnow) {
                // There is snow
                val orsTheresSnow = ListBuffer.empty[Clause]
                for (p <- l.map.iterator) {
                    sAnt.snow(p.c + cOffset) match {
                        case Some(v) =>
                            orsTheresSnow.append(And(Equals(sAntB.x, IntegerConstant(p.c.x)), Equals(sAntB.y, IntegerConstant(p.c.y)), v))
                        case None =>
                    }
                }
                val theresSnow = Operations.simplify(Or(orsTheresSnow: _*))


                val theresSnowVar = BooleanVariable("TVAR1A" + name + "B" + iB + "S" + s.stateNumber)
                expressions.append(Comment("There is snow"))
                expressions.append(VariableDeclaration(theresSnowVar))
                expressions.append(ClauseDeclaration(Implication(theresSnow, theresSnowVar)))
                expressions.append(ClauseDeclaration(Implication(theresSnowVar, theresSnow)))

                andsEf.append(Implication(And(theresSnowVar, Equals(sAntB.size, IntegerConstant(1))), Equals(sB.size, IntegerConstant(2))))
                andsEf.append(Implication(And(theresSnowVar, Equals(sAntB.size, IntegerConstant(2))), Equals(sB.size, IntegerConstant(4))))
                andsEf.append(Implication(Or(Not(theresSnowVar), Equals(sAntB.size, IntegerConstant(4))), Equals(sB.size, sAntB.size)))
            } else {
                andsEf.append(Equals(sB.size, sAntB.size))
            }

            val ef = And(andsEf: _*)

            // EF_ <-> A_
            expressions.append(Comment("EF <-> A" + name))
            expressions.append(ClauseDeclaration(Implication(ef, vA)))
            expressions.append(ClauseDeclaration(Implication(vA, ef)))
            // A_ -> PRE_
            expressions.append(Comment("A" + name + " -> PRE"))
            expressions.append(ClauseDeclaration(Implication(vA, pre)))
        }

        ActionCodification(expressions, actionsVariables)
    }

    private def getBallSize(o: objects.Object): Int = o match {
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

    case class ActionCodification(expressions: Seq[Expression], actionsVariables: Seq[BooleanVariable])
}