package gmt.planner.operation

object Implies {
    val IMPLIES: (Clause, Clause) => Clause = (c1, c2) => Implies(c1, c2)
}

case class Implies(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnBoolean(c1) || !Operations.isReturnBoolean(c1)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Implies(" + c1 + ", " + c2 + ")"
}
