package gmt.snowman.solver

import java.io._
import java.nio.charset.StandardCharsets
import java.security.InvalidParameterException
import java.util.stream.Collectors

import gmt.planner.solver.value.{ValueBoolean, ValueInteger}
import gmt.planner.solver.{Assignment, SolverResult}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class Yices2(solverBinaryPath: String) {

    private val CHARSET = StandardCharsets.UTF_8.name

    private val processBuilder = new ProcessBuilder(solverBinaryPath, "--incremental")
    processBuilder.redirectErrorStream(true)

    def solve(input: String): SolverResult = {
        val process = processBuilder.start()

        val processOutput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream, CHARSET))
        val inputStream = process.getInputStream
        val processInput =  new BufferedReader(new InputStreamReader(inputStream, CHARSET))

        try {
            processOutput.write(input)
            processOutput.flush()
        } catch {
            case _: Throwable =>
                val lines = input.lines().collect(Collectors.toList()).asScala
                lines.foreach(f => System.err.println(f))
        }

        val assignments = ListBuffer.empty[Assignment]

        val sat = processInput.readLine() match {
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

            processOutput.write("(get-model)\n")
            processOutput.write("(exit)\n")
            processOutput.flush()

            var line = processInput.readLine()
            while (line != null) {
                linesModel.append(line)
                line = processInput.readLine()
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

        SolverResult(sat, assignments)
    }
}
