package gmt.planner.planner

import gmt.planner.encoder.Encoder
import gmt.planner.solver.Solver
import gmt.planner.timestep.{TimeStepResult, TimeStepSolver}
import gmt.planner.translator.Translator

import scala.collection.mutable.ListBuffer


class Planner[A, B](val nThreads: Int, val maxActions: Int) {

    private var timeStep = 1

    private val threads = ListBuffer.empty[Child[A, B]]

    private var timeStepResult: Option[TimeStepResult[A]] = None

    def solve(encoder: Encoder[A, B], translator: Translator, solver: Solver, updateFunction: TimeStepResult[A] => Unit): PlannerResult[A] = {
        val updateSincronized = (t: TimeStepResult[A]) => synchronized { updateFunction(t) }

        for (i <- 0 until nThreads) {
            threads.append(new Child[A, B](i, this, new TimeStepSolver[A, B](encoder, translator, solver), updateSincronized))
        }

        threads.foreach(f => f.start())
        threads.foreach(f => f.join())

        timeStepResult match {
            case Some(r) =>
                PlannerResult[A](r.sat, r.timeSteps, r.result)
            case None =>
                PlannerResult[A](sat = false, timeStep, None)
        }
    }

    //noinspection AccessorLikeMethodIsEmptyParen
    // The function has to have parentesis https://docs.scala-lang.org/style/naming-conventions.html
    def getTimeStepAndIncrement(): Int = synchronized {
        val previousTimeStep = timeStep
        timeStep += 1
        previousTimeStep
    }

    def solutionFound(child: Child[A, B]): Unit = synchronized {
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