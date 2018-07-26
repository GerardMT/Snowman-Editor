package gmt.planner.operation

case class Sub(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnLogical(c1) || !Operations.isReturnLogical(c2)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Sub(" + c1 +  ", " + c2 + ")"
}
