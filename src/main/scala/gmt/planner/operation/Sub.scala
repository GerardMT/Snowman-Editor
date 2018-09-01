package gmt.planner.operation

object Sub {
    val SUB: (Clause, Clause) => Clause = (c1, c2) => Sub(c1, c2)
}

case class Sub(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnInteger(c1) || !Operations.isReturnInteger(c2)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Sub(" + c1 +  ", " + c2 + ")"
}
