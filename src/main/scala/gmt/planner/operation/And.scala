package gmt.planner.operation

object And {
    val AND: (Clause, Clause) => Clause = (c1, c2) => And(c1, c2)
}

case class And(c: Clause*) extends Clause {

    if (!c.forall(f => Operations.isReturnBoolean(f)) || c.isEmpty) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "And(" + c.head + c.tail.map(f => ", " + f.toString).mkString + ")"
}
