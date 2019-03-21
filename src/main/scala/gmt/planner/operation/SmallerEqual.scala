package gmt.planner.operation

object SmallerEqual {
    val SMALLER_EQUAL: (Clause, Clause) => Clause = (c1, c2) => SmallerEqual(c1, c2)
}

case class SmallerEqual(c1: Clause, c2: Clause) extends Clause {

    if (!Operations.isReturnInteger(c1) || !Operations.isReturnInteger(c2)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "SmallerEqual(" + c1 + ", " + c2 + ")"
}
