package gmt.util

import java.io.{BufferedReader, File, FileReader, FileWriter}


object Files {

    def saveTextFile(file: File, string: String): Unit = {
        val fileWriter = new FileWriter(file)
        fileWriter.write(string)
        fileWriter.close()
    }

    def openTextFile(file: File): String = {
        val stringBuilder = new StringBuilder()

        val br = new BufferedReader(new FileReader(file))
        var line = br.readLine()
        while (line != null) {
            stringBuilder.append(line + '\n')

            line = br.readLine()
        }

        stringBuilder.toString()
    }
}
