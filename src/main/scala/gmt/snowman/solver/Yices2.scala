package gmt.snowman.solver

import java.io._
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException
import java.util.stream.Collectors
import gmt.planner.solver.value.{ValueBoolean, ValueInteger}
import gmt.planner.solver.{Assignment, SolverResult}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.sys.process.Process

class Yices2(solverBinaryPath: String) {

    private val CHARSET = StandardCharsets.UTF_8.name

    private val processBuilder = new ProcessBuilder(solverBinaryPath, "--incremental")
    processBuilder.redirectErrorStream(true)
    private val process = processBuilder.start()

    private val output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream, CHARSET))
    private val inputStream = process.getInputStream
    private val input =  new BufferedReader(new InputStreamReader(inputStream, CHARSET))

    private var startTime : Long = -1

    def write(input: String): Unit = {
        try {
            output.write(input)
            output.flush()

            startTime = System.nanoTime()
        } catch {
            case _: Throwable =>
                val lines = input.lines().collect(Collectors.toList()).asScala
                lines.foreach(f => System.err.println(f))
        }
    }

    def solve(): SolverResult = {
        val assignments = ListBuffer.empty[Assignment]

        val sat = input.readLine() match {
            case "sat" =>
                true
            case "unsat" =>
                false
            case null =>
                false
            case l @ _ =>
                System.err.print(l)
                System.err.print("\n")
                throw new InvalidParameterException
        }

        val cpuUsage = Process("ps -p " + process.pid() + " -o %cpu").!!.split("\n")(1).toFloat / 100.0f
        val cpuTime = ((System.nanoTime() - startTime) * cpuUsage).toLong

        if (sat) {
            val linesModel = ListBuffer.empty[String]

            output.write("(get-model)\n")
            output.write("(exit)\n")
            output.flush()

            var line = input.readLine()
            while (line != null) {
                linesModel.append(line)
                line = input.readLine()
            }

            for (l <- linesModel) {
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
        }

        SolverResult(sat, assignments, cpuTime)
    }
}
