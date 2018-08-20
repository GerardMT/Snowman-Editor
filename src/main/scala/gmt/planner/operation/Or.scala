package gmt.planner.operation

object Or {
    val OR: (Clause, Clause) => Clause = (c1, c2) => Or(c1, c2)
}

case class Or(c: Clause*) extends Clause {

    if (!c.forall(f => Operations.isReturnBoolean(f)) || c.isEmpty) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "Or(" + c.head + c.tail.map(f => ", " + f.toString).mkString + ")"
}
