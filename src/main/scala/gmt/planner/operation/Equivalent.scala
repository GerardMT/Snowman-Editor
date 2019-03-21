package gmt.planner.operation

object Equivalent {
    val EQUIVALENT: (Clause, Clause) => Clause = (c1, c2) => Equivalent(c1, c2)
}

case class Equivalent(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnBoolean(c1) || !Operations.isReturnBoolean(c2)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Equivalent(" + c1 + ", " + c2 + ")"
}
