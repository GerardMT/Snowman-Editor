package gmt.planner.operation

case class Smaller(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnInteger(c1) || !Operations.isReturnInteger(c1)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Smaller(" + c1 + ", " + c2 + ")"
}
