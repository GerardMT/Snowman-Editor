package gmt.planner.operation

case class Equivalent(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnLogical(c1) || !Operations.isReturnLogical(c1)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Equivalent(" + c1 + ", " + c2 + ")"
}
