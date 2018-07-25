package snowman.solver.timeStepSolver

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import snowman.level.Level
import snowman.solver.encoder.{Encoder, Encoding}
import snowman.solver.planningSolver.PlanningSolver
import snowman.solver.solver.yices2.Yices2Solver
import snowman.ui.Settings

object TimeStepSolver {

    private val THREAD_FOLDER = "thread"
}

// TODO Eliminar els threads d'aqui i passar-ho a Child

class TimeStepSolver(threadNumber: Int, threadMaster: PlanningSolver, settings: Settings, level: Level, encoder: Encoder) extends Thread {

    private var _solved = false
    private var _end = false
    private var _actions = new AtomicInteger(0)

    private var _workingPath = settings.workingPath + TimeStepSolver.THREAD_FOLDER + threadNumber + "/"

    private var _result: Option[TimeStepResult] = None

    private var _codification: Option[Encoding] = None

    override def run(): Unit = {
        try {
            _actions.set(threadMaster.getActionsAndIncrement)

            new File(_workingPath).mkdir()

            val solver = new Yices2Solver(settings.solverPath, _workingPath)

            while (!_solved && !_end && _actions.get() <= settings.maxActions) {
                println("Thread " + threadNumber + ": Trying with... " + _actions.get() + " actions")

                val encoding = encoder.encode(level, _actions.get())
                _codification = Some(encoding)

                val solverResult = solver.solve(encoding.problem)
                _result = Some(TimeStepResult(solverResult.sat, _actions.get(), solverResult.assignments))
                _solved = solverResult.sat

                if (_solved) {
                    println("Thread " + threadNumber + ": " + _actions.get() + " actions: Sat")
                    threadMaster.solutionFound(this)
                } else {
                    println("Thread " + threadNumber + ": " + _actions.get() + " actions: Unsat")
                    _actions.set(threadMaster.getActionsAndIncrement)
                }
            }
        } catch {
            case e: InterruptedException =>
        }
    }

    def removeFiles(): Unit = {
        for (f <- new File(_workingPath).listFiles) {
            Files.delete(f.toPath)
        }
        Files.delete(new File(_workingPath).toPath)
    }

    def end(): Unit = _end = true

    def workingPath: String = _workingPath

    def actions: Int = _actions.get

    def result: TimeStepResult = _result.get

    def codification: Encoding = _codification.get
}
