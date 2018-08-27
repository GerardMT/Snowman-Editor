package gmt.planner.planner

import gmt.planner.encoder.Encoder
import gmt.planner.planner.Planner.{PlannerOptions, PlannerUpdate}
import gmt.planner.solver.Solver
import gmt.planner.timestep.{TimeStepResult, TimeStepSolver}
import gmt.snowman.transaltor.Translator

import scala.collection.mutable.ListBuffer

object Planner {

    case class PlannerUpdate[A](timeStepResult: TimeStepResult[A], totalMilliseconds: Long)
    case class PlannerOptions(startTimeSteps: Option[Int], maxTimeSteps: Int, timeout: Int, threads: Int)
}

class Planner[A, B](val plannerOptions: PlannerOptions) {

    private var timeStep = plannerOptions.threads

    private val threads = ListBuffer.empty[Child[A, B]]

    private var timeStepResult: Option[TimeStepResult[A]] = None

    def solve(encoder: Encoder[A, B], translator: Translator, solver: Solver, updateFunction: PlannerUpdate[A] => Unit): PlannerResult[A] = {
        val startTime = System.currentTimeMillis()

        plannerOptions.startTimeSteps match {
            case Some(n) =>
                timeStep = n
            case None =>
                timeStep = encoder.startTimeStep()
        }

        val updateSincronized = (t: TimeStepResult[A]) => synchronized { updateFunction(PlannerUpdate(t, System.currentTimeMillis() - startTime)) }

        for (i <- 0 until plannerOptions.threads) {
            threads.append(new Child[A, B](i, this, new TimeStepSolver[A, B](encoder, translator, solver), updateSincronized))
        }

        new Timeout(threads, plannerOptions.timeout).start()

        threads.foreach(f => f.start())
        threads.foreach(f => f.join())

        val time = System.currentTimeMillis() - startTime

        timeStepResult match {
            case Some(r) =>
                PlannerResult[A](r.sat, r.timeSteps, time, r.result)
            case None =>
                PlannerResult[A](sat = false, timeStep, time, None)
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