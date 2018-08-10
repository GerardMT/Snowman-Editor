package gmt.planner.planner

import java.util.concurrent.atomic.AtomicInteger

import gmt.planner.encoder.EncodingData
import gmt.planner.timestep.{TimeStepResult, TimeStepSolver}

object Child {

    private val THREAD_FOLDER = "thread"
}

class Child[A, B <: EncodingData](threadNumber: Int, threadMaster: Planner[A, B], timeStepSolver: TimeStepSolver[A, B]) extends Thread {

    private val _timeStep = new AtomicInteger(0)

    private var _end = false

    private var _result: Option[TimeStepResult[A]] = None

    override def run(): Unit = {
        try {
            _timeStep.set(threadMaster.getTimeStepAndIncrement())

            var solved = false

            while (!solved && !_end && _timeStep.get() <= threadMaster.maxActions) {
                val solverResult = timeStepSolver.solve(_timeStep.get)
                solved = solverResult.sat

                if (solved) {
                    _result = Some(solverResult)
                    threadMaster.solutionFound(this)
                } else {
                    _timeStep.set(threadMaster.getTimeStepAndIncrement())
                }
            }
        } catch {
            case e: InterruptedException =>
        }
    }


    def end(): Unit = _end = true

    def result: TimeStepResult[A] = _result.get

    def timeStep = _timeStep.get()
}