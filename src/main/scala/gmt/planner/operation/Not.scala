package gmt.planner.operation

case class Not(c: Clause) extends Clause {

    if (!Operations.isReturnBoolean(c)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Not(" + c + ")"
}
