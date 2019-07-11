package gmt.planner.planner

import gmt.planner.encoder.{Encoder, Encoding}
import gmt.planner.operation._
import gmt.planner.solver.SolverResult
import gmt.snowman.encoder.VariableAdder
import gmt.snowman.solver.Yices2
import gmt.snowman.translator.SMTLib2

import scala.collection.immutable
import scala.collection.mutable.ListBuffer

object Planner {

    case class Assuming(booleanVariable: BooleanVariable, value: Boolean)

    case class PlannerUpdate(sat: Boolean, timeSteps: Int, milliseconds: Long, totalMilliseconds: Long)

    case class PlannerOptions(startTimeSteps: Option[Int], maxTimeSteps: Int)

    case class PlannerResult[A](sat: Boolean, timeSteps: Int, milliseconds: Long, result: Option[A])

    def solve[A <: VariableAdder, B, C](plannerOptions: PlannerOptions, encoder: Encoder[A, B, C], solver: Yices2, updateFunction: PlannerUpdate => Unit): PlannerResult[C] = {
        val startTime = System.currentTimeMillis()
        var totalTime = 0l

        var solved = false

        val encoding = new Encoding
        val encodingData = encoder.createEncodingData()

        encoding.add(Custom("(set-option :produce-models true)"))
        encoding.add(Custom("(set-logic QF_LIA)"))

        solver.write(SMTLib2.translate(encoding))

        var iState = Math.max(1, plannerOptions.startTimeSteps.getOrElse(encoder.startTimeStep()))
        var state = encodeInitialTimeSteps(plannerOptions, encoder, solver, encoding, encodingData, iState - 1)

        var solverResult: SolverResult = null

        val goalsVariables = ListBuffer.empty[Assuming]

        while (!solved && iState <= plannerOptions.maxTimeSteps) {
            val startStepTime = System.currentTimeMillis()

            if (iState - 1 != 0) {
                val (goalClause, variables) = encoder.goal(state, encodingData)
                declareVariables(variables, encoding)
                encoding.add(ClauseDeclaration(Not(goalClause)))
            }

            val stateNext = encoder.createState(iState, encoding, encodingData)
            stateNext.addVariables(encoding)
            encoder.otherStates(stateNext, encoding, encodingData)


            encoder.actions(state, stateNext, encoding, encodingData)

            val goalVariable = BooleanVariable()
            encoding.add(VariableDeclaration(goalVariable))
            val (goalClause, variables) = encoder.goal(stateNext, encodingData)
            declareVariables(variables, encoding)
            encoding.add(ClauseDeclaration(Equivalent(goalVariable, goalClause)))

            goalsVariables.append(Assuming(goalVariable, value = true))

            encoding.add(Custom("(check-sat-assuming " + assumingListToString(goalsVariables.toList) + ")"))

            solver.write(SMTLib2.translate(encoding))
            solverResult = solver.solve()
            solved = solverResult.sat

            val time = System.currentTimeMillis()
            val stepsTime = time - startStepTime
            totalTime = time - startTime

            updateFunction(PlannerUpdate(solverResult.sat, iState, stepsTime, totalTime))

            if (!solved) {
                goalsVariables.remove(goalsVariables.size - 1)
                goalsVariables.append(Assuming(goalVariable, value = false))

                iState += 1
                state = stateNext
            }
        }


        if (solved) {
            PlannerResult(sat = true, iState, totalTime, Some(encoder.decode(solverResult.assignments, encodingData)))
        } else {
            PlannerResult(sat = false, iState, totalTime, None)
        }
    }

    def encodeInitialTimeSteps[A <: VariableAdder, B, C](plannerOptions: PlannerOptions, encoder: Encoder[A, B, C], solver: Yices2, encoding: Encoding, encodingData: B, timeSteps: Int): A = {
        var iState = 0

        var state = encoder.createState(iState, encoding, encodingData)
        state.addVariables(encoding)
        encoder.initialState(state, encoding, encodingData)

        iState += 1

        while (iState <= timeSteps) {
            if (iState - 1 != 0) {
                val (goalClause, variables) = encoder.goal(state, encodingData)
                declareVariables(variables, encoding)
                encoding.add(ClauseDeclaration(Not(goalClause)))
            }

            val stateNext = encoder.createState(iState, encoding, encodingData)
            stateNext.addVariables(encoding)
            encoder.otherStates(stateNext, encoding, encodingData)

            encoder.actions(state, stateNext, encoding, encodingData)

            iState += 1
            state = stateNext
        }

        state
    }

    def generate[A <: VariableAdder, B, C](timeSteps: Int, encoder: Encoder[A, B, C]): Encoding = {
        val encoding = new Encoding
        val encodingData = encoder.createEncodingData()

        encoding.add(Custom("(set-option :produce-models true)"))
        encoding.add(Custom("(set-logic QF_LIA)"))

        var state = encoder.createState(0, encoding, encodingData)
        state.addVariables(encoding)
        encoder.initialState(state, encoding, encodingData)

        val goalsVariables = ListBuffer.empty[Assuming]

        for (i <- 1 to timeSteps) {
            val stateNext = encoder.createState(i, encoding, encodingData)
            stateNext.addVariables(encoding)
            encoder.otherStates(stateNext, encoding, encodingData)

            encoder.actions(state, stateNext, encoding, encodingData)

            val goalVariable = BooleanVariable()
            encoding.add(VariableDeclaration(goalVariable))
            val (goalClause, variables) = encoder.goal(stateNext, encodingData)
            declareVariables(variables, encoding)
            encoding.add(ClauseDeclaration(Equivalent(goalVariable, goalClause)))

            goalsVariables.append(Assuming(goalVariable, value = i == timeSteps))

            state = stateNext
        }

        encoding.add(Custom("(check-sat-assuming " + assumingListToString(goalsVariables.toList) + ")"))
        encoding.add(Custom("(get-model)"))
        encoding.add(Custom("(exit)"))

        encoding
    }

    private def assumingListToString(assuming: immutable.Seq[Assuming]): String = {
        "(" + assmingToString(assuming.head) + assuming.tail.map(f => " " + assmingToString(f)).mkString + ")"
    }

    private def assmingToString(assuming: Assuming): String = if (assuming.value) {
        assuming.booleanVariable.name
    } else {
        "(not " + assuming.booleanVariable.name + ")"
    }

    private def declareVariables(variables: immutable.Seq[Clause], encoding: Encoding): Unit = {
        for (variable <- variables) {
            encoding.add(VariableDeclaration(variable))
        }
    }
}