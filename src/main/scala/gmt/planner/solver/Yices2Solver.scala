package gmt.planner.solver

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Files
import java.security.InvalidParameterException

import gmt.planner.solver.value.{ValueBoolean, ValueInteger}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.sys.process.Process
import scala.sys.process._

object Yices2Solver {

    private val IN_FILE = "in.smtlib2"
    private val OUT_FILE = "out.smtlib2"
}

class Yices2Solver(solverBinaryPath: String) extends Solver {

    private var _process: Option[Process] = None

    private var workingDirectoryPath: Option[String] = None

    override def solve(input: String): SolverResult = {
        val workingPath = workingDirectoryPath match {
            case Some(s)=>
                s
            case None =>
                throw new IllegalStateException("workingDirectoryPath must be initialized")
        }

        val bwIn = new BufferedWriter(new FileWriter(workingPath + Yices2Solver.IN_FILE))
        bwIn.write(input)
        bwIn.close()

        _process = Some((Seq(solverBinaryPath, workingPath + Yices2Solver.IN_FILE) #> new File(workingPath + Yices2Solver.OUT_FILE)).run())
        _process.get.exitValue()

        val assignments = ListBuffer.empty[Assignment]

        val lines = Source.fromFile(workingPath + Yices2Solver.OUT_FILE).getLines().toList

        val sat = lines.head match {
            case "sat" =>
                true
            case "unsat" =>
                false
            case _ =>
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

        removeFiles()

        SolverResult(sat, assignments)
    }


    override def workingDirectoryPath_=(path: String): Unit = workingDirectoryPath = Some(path)

    def terminate(): Unit = _process match {
        case Some(p) =>
            p.destroy()
            removeFiles()
        case None =>
    }

    private def removeFiles(): Unit = {
        val workingPath = workingDirectoryPath match {
            case Some(s)=>
                s
            case None =>
                throw new IllegalStateException("workingDirectoryPath must be initialized")
        }

        for (f <- new File(workingPath).listFiles) {
            Files.delete(f.toPath)
        }
        Files.delete(new File(workingPath).toPath)
    }
}
