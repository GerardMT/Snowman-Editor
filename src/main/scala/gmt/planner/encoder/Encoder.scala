package gmt.planner.encoder

import gmt.planner.operation.Clause
import gmt.planner.solver.Assignment

import scala.collection.immutable

trait Encoder[A, B, C] {

    def createEncodingData(): B

    def initialState(state: A, encoding: Encoding, encodingData: B): Unit

    def goal(state: A, encodingData: B): (Clause, immutable.Seq[Clause])

    def actions(state: A, stateNext: A, encoding: Encoding, encodingData: B): Unit

    def createState(index: Int, encoding: Encoding, encodingData: B): A

    def decode(assignments: Seq[Assignment], encodingData: B): C

    def startTimeStep(): Int
}
