package gmt.planner.planner

import gmt.planner.encoder.Encoder
import gmt.planner.solver.Solver
import gmt.planner.timestep.{TimeStepResult, TimeStepSolver}
import gmt.planner.translator.Translator
import gmt.ui.Settings

import scala.collection.mutable.ListBuffer


class Planner(settings: Settings) {

    private var timeStep = settings.startAction

    private val threads = ListBuffer.empty[Child]

    private var timeStepResult: Option[TimeStepResult] = None

    def solve(encoder: Encoder, tranlator: Translator, solver: Solver): PlannerResult = {
        for (i <- 0 until settings.threads) {
            threads.append(new Child(i, this, settings, new TimeStepSolver(tranlator, encoder, solver, settings.workingPath + i)))
        }

        threads.foreach(f => f.start())
        threads.foreach(f => f.join())

        timeStepResult match {
            case Some(r) =>
                PlannerResult(r.sat, r.timeSteps, r.assignments)
            case None =>
                PlannerResult(sat = false, timeStep, List())
        }
    }

    //noinspection AccessorLikeMethodIsEmptyParen
    // The function has to have parentesis https://docs.scala-lang.org/style/naming-conventions.html
    def getTimeStepAndIncrement(): Int = synchronized {
        val previusTimeStep = timeStep
        timeStep += 1
        previusTimeStep
    }

    def solutionFound(child: Child): Unit = synchronized {
        for (t <- threads) {
            if (t != child) {
                if (t.timeStep > child.timeStep) {
                    t.interrupt()
                } else {
                    t.end()
                }
            }
        }

        timeStepResult match {
            case Some(t) =>
                if (t.timeSteps > child.timeStep) {
                    timeStepResult = Some(child.result)
                }
            case None =>
                timeStepResult = Some(child.result)
        }
    }
}