package gmt.planner.operation

case class Add(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnInteger(c1) || !Operations.isReturnInteger(c2)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Add(" + c1 +  ", " + c2 + ")"
}
