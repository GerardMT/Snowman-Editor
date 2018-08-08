package gmt.planner.operation

case class Implies(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnBoolean(c1) || !Operations.isReturnBoolean(c1)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Implies(" + c1 + ", " + c2 + ")"
}
