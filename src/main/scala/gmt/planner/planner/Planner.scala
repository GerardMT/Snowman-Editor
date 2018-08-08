package gmt.planner.planner

import gmt.planner.action.Action
import gmt.planner.encoder.{Encoder, EncodingData}
import gmt.planner.solver.Solver
import gmt.planner.timestep.{TimeStepResult, TimeStepSolver}
import gmt.planner.translator.Translator

import scala.collection.mutable.ListBuffer


class Planner[A <: Action, B <: EncodingData](val nThreads: Int, val maxActions: Int) {

    private var timeStep = 1

    private val threads = ListBuffer.empty[Child[A, B]]

    private var timeStepResult: Option[TimeStepResult[A]] = None

    def solve(encoder: Encoder[A, B], tranlator: Translator, solver: Solver): PlannerResult[A] = {
        for (i <- 0 until nThreads) {
            threads.append(new Child[A, B](i, this, new TimeStepSolver[A, B](encoder, tranlator, solver)))
        }

        threads.foreach(f => f.start())
        threads.foreach(f => f.join())

        timeStepResult match {
            case Some(r) =>
                PlannerResult[A](r.sat, r.timeSteps, r.actions)
            case None =>
                PlannerResult[A](sat = false, timeStep, List())
        }
    }

    //noinspection AccessorLikeMethodIsEmptyParen
    // The function has to have parentesis https://docs.scala-lang.org/style/naming-conventions.html
    def getTimeStepAndIncrement(): Int = synchronized {
        val previusTimeStep = timeStep
        timeStep += 1
        previusTimeStep
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