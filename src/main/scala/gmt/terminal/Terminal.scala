package gmt.terminal

import java.io.File

import gmt.snowman.level.{Level, MutableLevel}
import gmt.snowman.pddl.EncoderPDDL
import gmt.snowman.util.Files

class Terminal {

    def main(args: Array[String]): Unit = {

        args.toList match {

            case List(_, "adl", levelPath, savePath) =>
                openLevelGeneratePDDLProblem(levelPath, savePath, EncoderPDDL.encodeAdl)

            case List(_, "adl-grounded", levelPath, savePath) =>
                openLevelGeneratePDDLProblem(levelPath, savePath, EncoderPDDL.encodeObjectFluents)

            case List(_, "object-fluents", levelPath, savePath) =>
                openLevelGeneratePDDLProblem(levelPath, savePath, EncoderPDDL.encodeAdlGrounded)

            case List(_, "smt-basic", levelPath, savePath) =>


            case List(_, "smt-cheating", levelPath, savePath) =>


            case List(_, "smt-reachability", levelPath, savePath) =>


            case List(_, "--help") | List(_, "-h") =>
                System.out.println("<snomwna editor> <option>")
                System.out.println("")
                System.out.println("<option>:")
                System.out.println("    adl <level path> <save path>")
                System.out.println("    adl-grounded <level path> <save path>")
                System.out.println("    object-fluents <level path> <save path>")

            case List(_) =>
                System.out.println("Error parsing the arguments. -h for help.")
        }
    }

    private def openLevelGeneratePDDLProblem(levelPath: String, problemPath: String, generator : Level => String): Unit = {
        Files.saveTextFile(new File(problemPath), generator(MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel))
    }

    private def openLevelGeneratePDDLDomainProblem(levelPath: String, domainPath: String, problemPath: String, generator : Level => (String, String)): Unit = {
        val (domain, problem) = generator(MutableLevel.load(Files.openTextFile(new File(levelPath))).toLevel)

        Files.saveTextFile(new File(domainPath), domain)
        Files.saveTextFile(new File(problemPath), problem)
    }
}
