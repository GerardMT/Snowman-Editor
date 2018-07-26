package gmt.planner.solver


abstract class Solver {

    def solve(input: String): SolverResult

    def workingDirectoryPath_=(path: String) // TODO Solucionar aixo
}