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

        _process = Some((solverBinaryPath #< inputStream #> outputStream).run())
        _process.get.exitValue()

        terminated match {
            case true =>
                SolverResult(false, List()) // TODO Use Option[Assigments] and 3 state variable: sat, unsat, terminated
            case false =>
                val assignments = ListBuffer.empty[Assignment]

                val lines = outputStream.toString.split("\n").toList

                val sat = lines.head match {
                    case "sat" =>
                        true
                    case "unsat" =>
                        false
                    case _ =>
                        lines.foreach(f => System.err.println(f))
                        throw new InvalidParameterException
                }

                for (l <- lines.tail) {
                    if (l.startsWith("(=")) {
                        val s = l.drop(3).dropRight(1).split(' ')
                        val v = s(1) match {
                            case "true" =>
                                ValueBoolean(true)
                            case "false" =>
                                ValueBoolean(false)
                            case _ =>
                                if (s.length == 3) {
                                    ValueInteger(-s(2).dropRight(1).toInt)
                                } else {
                                    ValueInteger(s(1).toInt)
                                }
                        }
                        assignments.append(Assignment(s(0), v))
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