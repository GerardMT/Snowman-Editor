package gmt.planner.planner

import gmt.planner.encoder.{Encoder, Encoding}
import gmt.planner.operation._
import gmt.planner.solver.SolverResult
import gmt.snowman.encoder.VariableAdder
import gmt.snowman.solver.Yices2
import gmt.snowman.translator.SMTLib2

import java.lang.management.ManagementFactory
import scala.collection.immutable
import scala.collection.mutable.ListBuffer

object Planner {

    case class Assuming(booleanVariable: BooleanVariable, value: Boolean)

    case class PlannerUpdate(sat: Boolean, timeSteps: Int, nanoseconds: Long, totalNanoseconds: Long)

    case class PlannerOptions(startTimeSteps: Option[Int], maxTimeSteps: Int)

    case class PlannerResult[A](sat: Boolean, timeSteps: Int, cpuSeconds: Float, result: Option[A])

    def solve[A <: VariableAdder, B, C](plannerOptions: PlannerOptions, encoder: Encoder[A, B, C], solver: Yices2, updateFunction: PlannerUpdate => Unit): PlannerResult[C] = {
        val ThreadMXBean = ManagementFactory.getThreadMXBean
        val plannerCpuStartTime = ThreadMXBean.getThreadCpuTime(Thread.currentThread.getId)

        val startTime = System.nanoTime()

        var solverTotalCpuSeconds = 0f

        var solved = false

        val startTimeSteps = Math.max(1, plannerOptions.startTimeSteps.getOrElse(encoder.startTimeStep()))
        var iState = startTimeSteps

        var solverResult: SolverResult = null
        var encodingData: B = null.asInstanceOf[B]

        while (!solved && iState < plannerOptions.maxTimeSteps) {
            val startStepTime = System.nanoTime()

            val encoding = new Encoding
            encodingData = encoder.createEncodingData()

            encoding.add(Custom("(set-option :produce-models true)"))
            encoding.add(Custom("(set-logic QF_LIA)"))

            var state = encodeInitialTimeSteps(encoder, encoding, encodingData, startTimeSteps - 1)

            for (iStateEncoding <- startTimeSteps to iState) {
                if (iStateEncoding - 1 != 0) {
                    val (goalClause, variables) = encoder.goal(state, encodingData)
                    declareVariables(variables, encoding)
                    encoding.add(ClauseDeclaration(Not(goalClause)))
                }

                val stateNext = encoder.createState(iStateEncoding, encoding, encodingData)
                stateNext.addVariables(encoding)
                encoder.otherStates(stateNext, encoding, encodingData)

                encoder.actions(state, stateNext, encoding, encodingData)

                state = stateNext
            }

            val (goalClause, variables) = encoder.goal(state, encodingData)
            declareVariables(variables, encoding)
            encoding.add(ClauseDeclaration(goalClause))

            encoding.add(Custom("(check-sat)"))

            solverResult = solver.solve(SMTLib2.translate(encoding))
            solved = solverResult.sat

            val time = System.nanoTime()
            val stepsTime = time - startStepTime
            val totalTime = time - startTime

            solverTotalCpuSeconds += solverResult.cpuSeconds

            updateFunction(PlannerUpdate(solverResult.sat, iState, stepsTime, totalTime))

            if (!solved) {
                iState += 1
            }
        }

        val plannerCpuSeconds = (ThreadMXBean.getThreadCpuTime(Thread.currentThread.getId) - plannerCpuStartTime) / 1e9f
        val totalCpuSeconds = solverTotalCpuSeconds + plannerCpuSeconds

        if (solved) {
            PlannerResult(sat = true, iState, totalCpuSeconds, Some(encoder.decode(solverResult.assignments, encodingData)))
        } else {
            PlannerResult(sat = false, iState, totalCpuSeconds, None)
        }
    }

    def encodeInitialTimeSteps[A <: VariableAdder, B, C](encoder: Encoder[A, B, C], encoding: Encoding, encodingData: B, timeSteps: Int): A = {
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

        val state = encodeInitialTimeSteps(encoder, encoding, encodingData, timeSteps - 1)

        val stateNext = encoder.createState(timeSteps, encoding, encodingData)
        stateNext.addVariables(encoding)
        encoder.otherStates(stateNext, encoding, encodingData)

        encoder.actions(state, stateNext, encoding, encodingData)

        val (goalClause, variables) = encoder.goal(stateNext, encodingData)
        declareVariables(variables, encoding)
        encoding.add(ClauseDeclaration(goalClause))

        encoding.add(Custom("(check-sat)"))
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