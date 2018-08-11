package gmt.planner.planner

import gmt.planner.encoder.Encoder
import gmt.planner.planner.Planner.PlannerUpdate
import gmt.planner.solver.Solver
import gmt.planner.timestep.{TimeStepResult, TimeStepSolver}
import gmt.planner.translator.Translator

import scala.collection.mutable.ListBuffer

object Planner {

    case class PlannerUpdate[A](timeStepResult: TimeStepResult[A], totalMilliseconds: Long)
}

class Planner[A, B](val nThreads: Int, val maxActions: Int) {

    private var timeStep = 1

    private val threads = ListBuffer.empty[Child[A, B]]

    private var timeStepResult: Option[TimeStepResult[A]] = None

    def solve(encoder: Encoder[A, B], translator: Translator, solver: Solver, updateFunction: PlannerUpdate[A] => Unit): PlannerResult[A] = {
        val startTime = System.currentTimeMillis()

        val updateSincronized = (t: TimeStepResult[A]) => synchronized { updateFunction(PlannerUpdate(t, startTime)) }

        for (i <- 0 until nThreads) {
            threads.append(new Child[A, B](i, this, new TimeStepSolver[A, B](encoder, translator, solver), updateSincronized))
        }

        threads.foreach(f => f.start())
        threads.foreach(f => f.join())

        val time = System.currentTimeMillis() - startTime

        timeStepResult match {
            case Some(r) =>
                PlannerResult[A](r.sat, r.timeSteps, r.result, time)
            case None =>
                PlannerResult[A](sat = false, timeStep, None, time)
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