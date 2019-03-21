package gmt.planner.operation

object Greater {
    val GREATER: (Clause, Clause) => Clause = (c1, c2) => Greater(c1, c2)
}

case class Greater(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnInteger(c1) || !Operations.isReturnInteger(c2)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Greater(" + c1 + ", " + c2 + ")"
}
