package gmt.planner.operation

object Equals {
    val EQUALS: (Clause, Clause) => Clause = (c1, c2) => Equals(c1, c2)
}

case class Equals(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnInteger(c1) || !Operations.isReturnInteger(c1)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Equals(" + c1 + ", " + c2 + ")"
}
