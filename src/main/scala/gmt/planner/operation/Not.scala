package gmt.planner.operation

case class Not(c: Clause) extends Clause {

    if (!Operations.isReturnLogical(c)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Add(" + c + ")"
}
