package gmt.planner.planner

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import gmt.planner.timestep.{TimeStepResult, TimeStepSolver}
import gmt.ui.Settings

object Child {

    private val THREAD_FOLDER = "thread"
}

class Child(threadNumber: Int, threadMaster: Planner, settings: Settings, timeStepSolver: TimeStepSolver) extends Thread {

    private val _timeStep = new AtomicInteger(0)

    private var _end = false

    private val _workingPath = settings.workingPath + Child.THREAD_FOLDER + threadNumber + "/"

    private var _result: Option[TimeStepResult] = None

    override def run(): Unit = {
        try {
            _timeStep.set(threadMaster.getTimeStepAndIncrement())

            new File(_workingPath).mkdir()

            var solved = false

            while (!solved && !_end && _timeStep.get() <= settings.maxActions) {
                val solverResult = timeStepSolver.solve(_timeStep.get)
                solved = solverResult.sat

                if (solved) {
                    threadMaster.solutionFound(this)
                    _result = Some(solverResult)
                } else {
                    _timeStep.set(threadMaster.getTimeStepAndIncrement())
                }
            }
        } catch {
            case e: InterruptedException =>
        }
    }


    def end(): Unit = _end = true

    def result: TimeStepResult = _result.get

    def timeStep = _timeStep.get()
}