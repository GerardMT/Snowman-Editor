package gmt.planner.operation

case class And(c: Clause*) extends Clause {

    if (!c.forall(f => Operations.isReturnBoolean(f)) || c.isEmpty) {
        throw InvalidClauseException(toString)
    }

    override def toString: String = "And(" + c.head + c.tail.map(f => ", " + f.toString).mkString + ")"
}
