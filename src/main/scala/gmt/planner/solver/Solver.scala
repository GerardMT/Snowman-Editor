package gmt.planner.solver


abstract class Solver {

    def solve(input: String): SolverResult

    def terminate(): Unit
}