package gmt.snowman.solver

import java.io._
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException

import gmt.planner.solver.value.{ValueBoolean, ValueInteger}
import gmt.planner.solver.{Assignment, Solver, SolverResult}

import scala.collection.mutable.ListBuffer
import scala.sys.process.{Process, _}


class Yices2Solver(solverBinaryPath: String) extends Solver {

    private var _process: Option[Process] = None
    private var terminated = false

    override def solve(input: String): SolverResult = {

        val inputStream: InputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8.name))
        val outputStream = new ByteArrayOutputStream()

        _process = Some(("/home/gerard/software/cvc4-2018-09-18/install/bin/cvc4 --lang smt --produce-models" #< inputStream #> outputStream).run())
        _process.get.exitValue()

        if (terminated) {
            SolverResult(sat = false, List())
        } else {
            val assignments = ListBuffer.empty[Assignment]

            val lines = outputStream.toString.split("\n").toList

            val sat = lines.contains("sat")

            for (l <- lines) {
                if (l.startsWith("(define-fun ")) {
                    val s = l.split(' ')(1)
                    val v = l.dropRight(1).split(' ').zipWithIndex.filter(f => f._2 > 3).map(f => f._1).mkString(" ") match {
                        case "true" =>
                            ValueBoolean(true)
                        case "false" =>
                            ValueBoolean(false)
                        case i @ _ =>
                            if (i(0) == '(') {
                                ValueInteger(- i.drop(3).dropRight(1).toInt)
                            } else {
                                ValueInteger(i.toInt)
                            }
                    }
                    assignments.append(Assignment(s, v))
                } else {
                    System.err.println(l)
                }
            }

            SolverResult(sat, assignments)
        }

    }

    def terminate(): Unit = _process match {
        case Some(p) =>
            terminated = true
            p.destroy()
        case None =>
    }
}
