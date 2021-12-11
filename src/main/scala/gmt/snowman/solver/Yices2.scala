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

    private val processBuilder = new ProcessBuilder("/usr/bin/time", "-p", solverBinaryPath, "--incremental")
    processBuilder.redirectErrorStream(true)
    private val process = processBuilder.start()

    private val output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream, CHARSET))
    private val inputStream = process.getInputStream
    private val input =  new BufferedReader(new InputStreamReader(inputStream, CHARSET))

    private var cpuSeconds: Float = -1f

    def write(input: String): Unit = {
        try {
            output.write(input)
            output.flush()
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

            cpuSeconds = readTime(linesModel.takeRight(3))
            linesModel.remove(linesModel.length - 3, 3)

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

        SolverResult(sat, assignments)
    }

    def finish() : Float = {
        if (process.isAlive) {
            val lines = ListBuffer.empty[String]

            output.write("(get-model)\n")
            output.write("(exit)\n")
            output.flush()

            var line = input.readLine()
            while (line != null) {
                lines.append(line)
                line = input.readLine()
            }

            readTime(lines)
        } else {
            cpuSeconds
        }
    }

    private def readTime(lines: ListBuffer[String]) : Float = {
        val kernelTime = lines.last.substring(4).toFloat
        val userTime = lines(lines.length - 2).substring(5).toFloat

        userTime + kernelTime
    }
}
