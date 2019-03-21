package gmt.planner.operation

object GreaterEqual {
    val GREATER_EQUAL: (Clause, Clause) => Clause = (c1, c2) => GreaterEqual(c1, c2)
}

case class GreaterEqual(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnInteger(c1) || !Operations.isReturnInteger(c2)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "GreaterEqual(" + c1 + ", " + c2 + ")"
}
