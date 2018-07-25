package snowman.solver.solver.yices2

import java.io.{BufferedWriter, File, FileWriter}
import java.security.InvalidParameterException

import snowman.solver.Problem
import snowman.solver.translators.SMTLib2
import snowman.solver.solver.{Assignment, Solver, SolverResult}
import snowman.solver.solver.value.{ValueBoolean, ValueInteger}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.sys.process._

object Yices2Solver {

    private val IN_FILE = "in.smt2"
    private val OUT_FILE = "out.smt2"
}

class Yices2Solver(solverBinaryPath: String, workingDirectoryPath: String) extends Solver {

    private var _process: Option[Process] = None

    override def solve(input: String): SolverResult = {
        val s =

        val bwIn = new BufferedWriter(new FileWriter(workingDirectoryPath + Yices2Solver.IN_FILE))
        bwIn.write(s)
        bwIn.close()

        _process = Some((Seq(solverBinaryPath, workingDirectoryPath + Yices2Solver.IN_FILE) #> new File(workingDirectoryPath + Yices2Solver.OUT_FILE)).run())
        _process.get.exitValue()

        val assignments = ListBuffer.empty[Assignment]

        val lines = Source.fromFile(workingDirectoryPath + Yices2Solver.OUT_FILE).getLines().toList

        var sat = false

        if (lines.head == "sat" ) {
            sat = true
        } else if (lines.head == "unsat") {
            sat = false
        } else {
            throw new InvalidParameterException
        }

        lines.tail.foreach(f => {
            if (f.startsWith("(=")) {
                val s = f.drop(3).dropRight(1).split(' ')
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
        })

        SolverResult(sat, assignments)
    }

    def terminate(): Unit = _process match {
        case Some(p) =>
            p.destroy()
        case None =>
    }

}
