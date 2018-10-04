package gmt.planner.operation

object Not {
    val NOT: Clause => Clause = c => Not(c)
}

case class Not(c: Clause) extends Clause {

    if (!Operations.isReturnBoolean(c)) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Not(" + c + ")"
}
