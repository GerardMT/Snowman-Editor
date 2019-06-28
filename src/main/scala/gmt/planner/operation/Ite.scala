package gmt.planner.operation

object Ite {
    val ITE: (Clause, Clause, Clause) => Clause = (c1, c2, c3) => Ite(c1, c2, c3)
}

case class Ite(c1: Clause, c2: Clause, c3: Clause) extends Clause {
    if (!Operations.isReturnBoolean(c1) || (Operations.isReturnBoolean(c2) != Operations.isReturnBoolean(c3)) || (Operations.isReturnInteger(c2) != Operations.isReturnInteger(c3))) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Ite(" + c1 + ", " + c2 + ", " + c3 + ")"
}