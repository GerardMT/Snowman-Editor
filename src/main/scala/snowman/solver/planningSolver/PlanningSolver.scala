package snowman.solver.planningSolver

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, StandardCopyOption}

import snowman.level.Level
import snowman.solver.Report
import snowman.solver.encoder.Encoder
import snowman.solver.timeStepSolver.TimeStepSolver
import snowman.ui.Settings

import scala.collection.mutable.ListBuffer


class PlanningSolver(settings: Settings) {

    private var _actions = settings.startAction

    private val _threads = ListBuffer.empty[TimeStepSolver]

    private var _threadMinimumSolution: Option[TimeStepSolver] = None

    def solve(level: Level, encoder: Encoder, tranlator: Translator): PlanningSolverResult = {
        for (i <- 0 until settings.threads) {
            _threads.append(new TimeStepSolver(i, this, settings, level, encoder))
        }

        _threads.foreach(_.start())
        _threads.foreach(_.join())

        _threadMinimumSolution match {
            case Some(t) =>
                for (f <- new File(t.workingPath).listFiles) {
                    Files.move(f.toPath, new File(settings.workingPath + f.getName).toPath, StandardCopyOption.REPLACE_EXISTING)
                }

                val bwIn = new BufferedWriter(new FileWriter(settings.workingPath + "report.txt"))
                bwIn.write(Report.generateReport(level, t.codification.states, t.result.assignments))
                bwIn.close()

                _threads.foreach(f => f.removeFiles())

                PlanningSolverResult(sat = true, t.result.assignments)
            case None =>
                PlanningSolverResult(sat = false, List())
        }
    }

    def getActionsAndIncrement: Int = synchronized {
        val a = _actions
        _actions += 1
        a
    }

    def solutionFound(thread: TimeStepSolver): Unit = synchronized {
        println("Solution found. " + thread.result.actions + " actions")

        for (t <- _threads) {
            if (t != thread) {
                if (t.actions > thread.actions) {
                    t.interrupt()
                } else {
                    t.end()
                }
            }
        }

        _threadMinimumSolution match {
            case Some(t) =>
                if (t.result.actions > thread.result.actions) {
                    _threadMinimumSolution = Some(thread)
                }
            case None =>
                _threadMinimumSolution = Some(thread)
        }
    }
}