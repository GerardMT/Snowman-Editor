package gmt.terminal

import java.io.File

import gmt.core.Core

object Main {

    def main(args: Array[String]): Unit = {
        val argsList = args.toList

        Terminal(Core.create(List(argsList.head))).parseArguments(argsList.tail)
    }
}